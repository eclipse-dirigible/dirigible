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
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.restassured.specification.RequestSpecification;

/**
 * Check messages take part in the module's i18n - dirigible #7611.
 *
 * <p>
 * A check's message used to be one literal, so a Bulgarian user read an English refusal (or an
 * English user a Bulgarian one). This generates a module whose checks author their message once, in
 * the default language, and translates them the way a label is translated - a hand-written bg-BG
 * catalog overlay under the keys the generated en-US catalog writes - then asserts on the running,
 * generated code: the en-US catalog carries every message under its stable key, a refusal (400) and
 * a warning (428) are answered in the request's {@code Accept-Language} with the placeholders
 * interpolated after translation, the body carries the key and the parameters next to the text, an
 * untranslated key and an untranslated language fall back to the default-language text.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentCheckMessageI18nIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "checkmsg";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String CUSTOMERS = API + "/customer/CustomerController";
    private static final String INVOICES = API + "/invoice/InvoiceController";
    private static final String ITEMS = API + "/invoice/InvoiceItemController";
    private static final String CONFIRM = "X-Confirm-Warnings";
    private static final String CATALOG = PROJECT + ":" + PROJECT + "-model.checks.";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String INTENT_YAML = """
            name: checkmsg
            description: check messages per language
            languages: [en, bg]

            entities:
              - name: Customer
                checks:
                  - kind: duplicate
                    fields: [name]
                    message: "A customer named {match} already exists"
                  - kind: compare
                    field: discount
                    op: le
                    value: 50
                    message: "The discount is at most 50%"
                  - { kind: compare, field: credit, op: ge, value: 0, message: "The credit is never negative" }
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: name,     type: string, length: 100 }
                  - { name: discount, type: decimal, precision: 18, scale: 2 }
                  - { name: credit,   type: decimal, precision: 18, scale: 2 }

              - name: Invoice
                checks:
                  - id: zeroLines
                    kind: itemsCompare
                    field: price
                    op: gt
                    value: 0
                    message: "{count} line(s) at price zero"
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

    /**
     * A translator's overlay: the bg-BG catalog translates three of the four messages by their keys and
     * leaves {@code Customer_compare_2} untranslated, so that one falls back to the default text. The
     * {@code zeroLines} translation moves {@code {count}}, the i18next spelling included.
     */
    private static final String BG_OVERLAY = """
            {
              "checkmsg-model": {
                "checks": {
                  "Customer_duplicate_0": "Вече има клиент с име {match}",
                  "Customer_compare_1": "Отстъпката е най-много 50%",
                  "Invoice_zeroLines": "Редове с нулева цена: {{count}}"
                }
              }
            }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void check_messages_are_answered_in_the_request_language() {
        generateProject();
        assertGeneratedCatalog();
        writeResource(PROJECT_PATH + "/i18n/bg-BG/checkmsg.model.json", BG_OVERLAY);
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        // A refusal translated by the language's catalog: each reader gets their own, with the key next to
        // it.
        refused("{\"Name\":\"Acme\",\"Discount\":60}", "bg").body("message", equalTo("Отстъпката е най-много 50%"))
                                                            .body("messageKey", equalTo(CATALOG + "Customer_compare_1"));
        refused("{\"Name\":\"Acme\",\"Discount\":60}", "en").body("message", equalTo("The discount is at most 50%"));
        refused("{\"Name\":\"Acme\",\"Discount\":60}", "bg-BG").body("message", equalTo("Отстъпката е най-много 50%"));
        // A language the module does not translate falls back to the default-language text.
        refused("{\"Name\":\"Acme\",\"Discount\":60}", "de").body("message", equalTo("The discount is at most 50%"));
        refused("{\"Name\":\"Acme\",\"Discount\":60}", null).body("message", equalTo("The discount is at most 50%"));

        // A key the language's catalog does not translate falls back to the default-language text.
        refused("{\"Name\":\"Acme\",\"Credit\":-1}", "bg").body("message", equalTo("The credit is never negative"))
                                                          .body("messageKey", equalTo(CATALOG + "Customer_compare_2"));
        refused("{\"Name\":\"Acme\",\"Credit\":-1}", "en").body("message", equalTo("The credit is never negative"));

        // A warning: translated, {match} interpolated AFTER translation, key and params in the body.
        create(CUSTOMERS, "{\"Name\":\"Acme\"}", null, "en");
        warned(CUSTOMERS, "{\"Name\":\"Acme\"}", "bg").body("warnings", hasSize(1))
                                                      .body("warnings[0].code", equalTo("Customer.duplicate.0"))
                                                      .body("warnings[0].message", equalTo("Вече има клиент с име Acme"))
                                                      .body("warnings[0].messageKey", equalTo(CATALOG + "Customer_duplicate_0"))
                                                      .body("warnings[0].messageParams.match", equalTo("Acme"));
        warned(CUSTOMERS, "{\"Name\":\"Acme\"}", "en").body("warnings[0].message", equalTo("A customer named Acme already exists"));

        // An explicit id names the key; {count} lands where the translation put it.
        int invoice = create(INVOICES, "{\"Note\":\"October\"}", null, "en");
        create(ITEMS, "{\"Invoice\":" + invoice + ",\"Price\":0}", null, "en");
        create(ITEMS, "{\"Invoice\":" + invoice + ",\"Price\":0}", null, "en");
        AtomicReference<io.restassured.response.ValidatableResponse> response = new AtomicReference<>();
        restAssuredExecutor.execute(() -> response.set(request(null, "bg").body("{\"Note\":\"October, final\"}")
                                                                          .when()
                                                                          .put(INVOICES + "/" + invoice)
                                                                          .then()
                                                                          .statusCode(428)),
                TIMEOUT_SECONDS);
        response.get()
                .body("warnings[0].code", equalTo("Invoice.itemsCompare.0"))
                .body("warnings[0].message", equalTo("Редове с нулева цена: 2"))
                .body("warnings[0].messageKey", equalTo(CATALOG + "Invoice_zeroLines"))
                .body("warnings[0].messageParams.count", equalTo(2));
    }

    /**
     * The generated en-US catalog carries every check message in the default language under the key the
     * generated code resolves - the entry a translator copies into another language's catalog.
     */
    private void assertGeneratedCatalog() {
        IResource catalog = repository.getResource(PROJECT_PATH + "/i18n/en-US/checkmsg.model.json");
        assertTrue(catalog.exists(), "the en-US catalog of the model was not generated");
        JsonObject checks = JsonParser.parseString(new String(catalog.getContent(), StandardCharsets.UTF_8))
                                      .getAsJsonObject()
                                      .getAsJsonObject("checkmsg-model")
                                      .getAsJsonObject("checks");
        assertEquals("A customer named {match} already exists", checks.get("Customer_duplicate_0")
                                                                      .getAsString());
        assertEquals("The discount is at most 50%", checks.get("Customer_compare_1")
                                                          .getAsString());
        assertEquals("The credit is never negative", checks.get("Customer_compare_2")
                                                           .getAsString());
        assertEquals("{count} line(s) at price zero", checks.get("Invoice_zeroLines")
                                                            .getAsString());
    }

    private io.restassured.response.ValidatableResponse refused(String body, String language) {
        AtomicReference<io.restassured.response.ValidatableResponse> response = new AtomicReference<>();
        restAssuredExecutor.execute(() -> response.set(request(null, language).body(body)
                                                                              .when()
                                                                              .post(CUSTOMERS)
                                                                              .then()
                                                                              .statusCode(400)),
                TIMEOUT_SECONDS);
        return response.get();
    }

    private int create(String controller, String body, String confirm, String language) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(request(confirm, language).body(body)
                                                                           .when()
                                                                           .post(controller)
                                                                           .then()
                                                                           .statusCode(200)
                                                                           .extract()
                                                                           .path("Id")),
                TIMEOUT_SECONDS);
        return id.get();
    }

    private io.restassured.response.ValidatableResponse warned(String controller, String body, String language) {
        AtomicReference<io.restassured.response.ValidatableResponse> response = new AtomicReference<>();
        restAssuredExecutor.execute(() -> response.set(request(null, language).body(body)
                                                                              .when()
                                                                              .post(controller)
                                                                              .then()
                                                                              .statusCode(428)
                                                                              .body("errorType", equalTo("ConfirmationRequired"))),
                TIMEOUT_SECONDS);
        return response.get();
    }

    private static RequestSpecification request(String confirm, String language) {
        RequestSpecification request = given().contentType("application/json");
        if (language != null) {
            request = request.header("Accept-Language", language);
        }
        return confirm == null ? request : request.header(CONFIRM, confirm);
    }

    private void generateProject() {
        writeResource(PROJECT_PATH + "/app.intent", INTENT_YAML);
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

    private void writeResource(String path, String content) {
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(content.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, content.getBytes(StandardCharsets.UTF_8));
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
