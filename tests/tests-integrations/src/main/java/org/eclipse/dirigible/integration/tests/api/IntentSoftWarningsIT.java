/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import io.restassured.response.ValidatableResponse;

/**
 * The soft "warn and confirm" tier - dirigible #7466.
 *
 * <p>
 * The billing review asked for three things the DSL could only refuse or ignore: warn before a
 * second customer with the same name, warn before a customer is saved outside the usual range, and
 * warn ONCE per document save when lines carry a zero price. This asserts the generated code end to
 * end: the repository's {@code warnings()} and the controllers' confirmation really compile, a
 * write that raises a warning is answered 428 with the codes and is NOT persisted, the same request
 * repeated with the codes in {@code X-Confirm-Warnings} goes through, and a confirmation covers
 * only the codes it names.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentSoftWarningsIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "softwarn";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String CUSTOMERS = API + "/customer/CustomerController";
    private static final String INVOICES = API + "/invoice/InvoiceController";
    private static final String ITEMS = API + "/invoice/InvoiceItemController";
    private static final String CONFIRM = "X-Confirm-Warnings";
    private static final String DUPLICATE = "Customer.duplicate.0";
    private static final String DISCOUNT = "Customer.compare.1";
    private static final String ZERO_LINES = "Invoice.itemsCompare.0";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String INTENT_YAML = """
            name: softwarn
            description: soft-warning fixture - the write is legitimate, the person saving confirms it

            entities:
              - name: Customer
                checks:
                  - { kind: duplicate, fields: [name], message: "A customer with this name already exists: {match}" }
                  - { kind: compare, field: discount, op: le, value: 50, severity: warn, message: "A discount above 50%" }
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: name,     type: string, length: 100 }
                  - { name: discount, type: decimal, precision: 18, scale: 2 }

              - name: Invoice
                checks:
                  - { kind: itemsCompare, field: price, op: gt, value: 0, message: "{count} line(s) at price zero" }
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 200 }

              - name: InvoiceItem
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: price, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Invoice, kind: manyToOne, to: Invoice, composition: true, required: true }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_warning_is_asked_answered_and_only_then_written() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        // A duplicate: the first record is nobody's duplicate...
        create(CUSTOMERS, "{\"Name\":\"Acme\"}", null);
        // ...the second is asked about, and nothing is written until it is confirmed.
        warned(CUSTOMERS, "{\"Name\":\"Acme\"}", null).body("warnings", hasSize(1))
                                                      .body("warnings[0].code", equalTo(DUPLICATE))
                                                      .body("warnings[0].message",
                                                              equalTo("A customer with this name already exists: Acme"));
        assertCount(CUSTOMERS, 1);
        create(CUSTOMERS, "{\"Name\":\"Acme\"}", DUPLICATE);
        assertCount(CUSTOMERS, 2);
        // The same name typed again rarely matches byte for byte (#7524): a different case and stray
        // spaces are still asked about, and {match} names the stored record in its own spelling.
        warned(CUSTOMERS, "{\"Name\":\"  ACME \"}", null).body("warnings", hasSize(1))
                                                         .body("warnings[0].code", equalTo(DUPLICATE))
                                                         .body("warnings[0].message",
                                                                 equalTo("A customer with this name already exists: Acme"));
        assertCount(CUSTOMERS, 2);

        // A record never duplicates itself: saving a unique one again asks nothing.
        int solo = create(CUSTOMERS, "{\"Name\":\"Solo\"}", null);
        update(CUSTOMERS, solo, "{\"Name\":\"Solo\",\"Discount\":10}", null);
        // ...while an update INTO a duplicate is asked like a create.
        warnedUpdate(CUSTOMERS, solo, "{\"Name\":\"Acme\"}", null).body("warnings[0].code", equalTo(DUPLICATE));

        // A soft compare, and a confirmation covers only the codes it names: both warnings are listed,
        // and confirming one of them still asks.
        warned(CUSTOMERS, "{\"Name\":\"Acme\",\"Discount\":60}", DUPLICATE).body("warnings", hasSize(2))
                                                                           .body("warnings[1].code", equalTo(DISCOUNT));
        create(CUSTOMERS, "{\"Name\":\"Acme\",\"Discount\":60}", DUPLICATE + "," + DISCOUNT);
        assertCount(CUSTOMERS, 4);

        // Zero-price lines: never asked per line - a line is written as it is added...
        int invoice = create(INVOICES, "{\"Note\":\"September\"}", null);
        create(ITEMS, "{\"Invoice\":" + invoice + ",\"Price\":0}", null);
        create(ITEMS, "{\"Invoice\":" + invoice + ",\"Price\":12.50}", null);
        create(ITEMS, "{\"Invoice\":" + invoice + ",\"Price\":0}", null);
        // ...and asked ONCE when the document is saved, counting the lines that break it.
        warnedUpdate(INVOICES, invoice, "{\"Note\":\"September, final\"}", null).body("warnings", hasSize(1))
                                                                                .body("warnings[0].code", equalTo(ZERO_LINES))
                                                                                .body("warnings[0].message",
                                                                                        equalTo("2 line(s) at price zero"));
        update(INVOICES, invoice, "{\"Note\":\"September, final\"}", ZERO_LINES);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(INVOICES + "/" + invoice)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Note", equalTo("September, final")),
                TIMEOUT_SECONDS);
    }

    private int create(String controller, String body, String confirm) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(request(confirm).body(body)
                                                                 .when()
                                                                 .post(controller)
                                                                 .then()
                                                                 .statusCode(200)
                                                                 .extract()
                                                                 .path("Id")),
                TIMEOUT_SECONDS);
        return id.get();
    }

    private void update(String controller, int id, String body, String confirm) {
        restAssuredExecutor.execute(() -> request(confirm).body(body)
                                                          .when()
                                                          .put(controller + "/" + id)
                                                          .then()
                                                          .statusCode(200),
                TIMEOUT_SECONDS);
    }

    private ValidatableResponse warned(String controller, String body, String confirm) {
        AtomicReference<ValidatableResponse> response = new AtomicReference<>();
        restAssuredExecutor.execute(() -> response.set(request(confirm).body(body)
                                                                       .when()
                                                                       .post(controller)
                                                                       .then()
                                                                       .statusCode(428)
                                                                       .body("errorType", equalTo("ConfirmationRequired"))),
                TIMEOUT_SECONDS);
        return response.get();
    }

    private ValidatableResponse warnedUpdate(String controller, int id, String body, String confirm) {
        AtomicReference<ValidatableResponse> response = new AtomicReference<>();
        restAssuredExecutor.execute(() -> response.set(request(confirm).body(body)
                                                                       .when()
                                                                       .put(controller + "/" + id)
                                                                       .then()
                                                                       .statusCode(428)
                                                                       .body("errorType", equalTo("ConfirmationRequired"))),
                TIMEOUT_SECONDS);
        return response.get();
    }

    private static io.restassured.specification.RequestSpecification request(String confirm) {
        io.restassured.specification.RequestSpecification request = given().contentType("application/json");
        return confirm == null ? request : request.header(CONFIRM, confirm);
    }

    private void assertCount(String controller, int expected) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(controller)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("size()", equalTo(expected)),
                TIMEOUT_SECONDS);
    }

    private void generateProject() {
        writeIntent();
        AtomicReference<List<Map<String, Object>>> plan = new AtomicReference<>();
        restAssuredExecutor.execute(() -> plan.set(given().when()
                                                          .post("/services/ide/intent/generate?workspace=" + WORKSPACE + "&project="
                                                                  + PROJECT + "&path=app.intent")
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .jsonPath()
                                                          .getList("codeGenerations")));
        for (Map<String, Object> codeGeneration : plan.get()) {
            assertEquals(Boolean.TRUE, codeGeneration.get("generated"),
                    "generating code from " + codeGeneration.get("path") + " failed: " + codeGeneration.get("error"));
        }
    }

    private void publishProject() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT + "/")
                                                 .then()
                                                 .statusCode(200));
    }

    private void writeIntent() {
        String path = PROJECT_PATH + "/app.intent";
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        }
    }

    @AfterEach
    void cleanup() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT)
                                                 .then()
                                                 .statusCode(greaterThanOrEqualTo(200)));
        if (repository.hasCollection(PROJECT_PATH)) {
            repository.removeCollection(PROJECT_PATH);
        }
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
