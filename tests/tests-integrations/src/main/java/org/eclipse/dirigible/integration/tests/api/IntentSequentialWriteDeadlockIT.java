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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

import io.restassured.response.Response;

/**
 * One client writing one call after another is never told that its record was changed by another
 * write (dirigible #8341).
 *
 * <p>
 * A payment names its invoice and then its customer, and the reference check share-locks both for
 * the length of the write (#8252). The platform posts every payment asynchronously, and the posting
 * names the same two records in the other order: its entry the customer, then - after a slow step -
 * its lines the invoice. On H2 a share lock is the exclusive {@code FOR UPDATE}, so the next
 * payment of the same customer, arriving while the previous one is still being posted, and that
 * posting each held one row and waited for the other: a deadlock, and the database rolled the
 * client's write back - answered 409 "changed by another write" although nothing had changed it.
 * Every call must save, every payment must be posted, and the roll-ups over the payments must add
 * up. On PostgreSQL the two share locks do not conflict, and the same walk must pass there too.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentSequentialWriteDeadlockIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "sequentialwrite";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String CUSTOMERS = API + "/customer/CustomerController";
    private static final String INVOICES = API + "/invoice/InvoiceController";
    private static final String PAYMENTS = API + "/payment/PaymentController";
    private static final String ENTRIES = API + "/entry/EntryController";
    private static final long TIMEOUT_SECONDS = 90;
    private static final long DERIVED_WRITES_TIMEOUT_SECONDS = 60;
    private static final int ROUNDS = 6;
    /**
     * Long enough for the previous payment's posting to have locked the customer and reached its slow
     * step.
     */
    private static final long PAUSE_MILLIS = 200;

    private static final String INTENT_YAML = """
            name: sequentialwrite
            description: sequential writes naming the records the previous write's derived writes still hold

            entities:
              - name: Customer
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: name,    type: string, length: 50 }
                  - { name: balance, type: decimal, precision: 18, scale: 2, readOnly: true }
              - name: Invoice
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 50 }
                  - { name: paid, type: decimal, precision: 18, scale: 2, readOnly: true }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }
              - name: Payment
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Invoice,  kind: manyToOne, to: Invoice }
                  - { name: Customer, kind: manyToOne, to: Customer }
              - name: Entry
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }
                  - { name: Payment,  kind: manyToOne, to: Payment }
              - name: EntryLine
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: debit,  type: decimal, precision: 18, scale: 2 }
                  - { name: credit, type: decimal, precision: 18, scale: 2 }
                  - { name: memo,   type: string, length: 20, calculatedActionOnCreate: custom.SlowLineAction }
                relations:
                  - { name: Entry,   kind: manyToOne, to: Entry, composition: true, required: true }
                  - { name: Invoice, kind: manyToOne, to: Invoice }

            rollups:
              - { name: invoicePaid,     entity: Payment, via: Invoice,  field: paid,    op: sum, of: amount }
              - { name: customerBalance, entity: Payment, via: Customer, field: balance, op: sum, of: amount }

            postings:
              - name: paymentPosting
                event: { onCreate: Payment }
                creates: Entry
                backReference: Payment
                map: { Customer: Customer }
                items:
                  - { debit: "Amount", Invoice: "Invoice" }
                  - { credit: "Amount", Invoice: "Invoice" }
            """;

    /**
     * The posting's slow step: it runs inside the posting's transaction, after the entry has locked the
     * customer and before the line locks the invoice.
     */
    private static final String SLOW_LINE_ACTION = """
            package custom;

            import org.eclipse.dirigible.sdk.component.Component;
            import org.eclipse.dirigible.sdk.db.CalculatedField;

            @Component
            public class SlowLineAction implements CalculatedField<Object, String> {

                @Override
                public String calculate(Object line) {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                    }
                    return "posted";
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
    void a_write_naming_the_records_the_previous_writes_posting_holds_saves() throws InterruptedException {
        write(PROJECT_PATH + "/custom/SlowLineAction.java", SLOW_LINE_ACTION);
        write(PROJECT_PATH + "/app.intent", INTENT_YAML);
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int customer = create(CUSTOMERS, "{\"Name\":\"ACME\"}");
        int invoice = create(INVOICES, "{\"Note\":\"first\",\"Customer\":" + customer + "}");

        List<String> refused = new ArrayList<>();
        for (int round = 1; round <= ROUNDS; round++) {
            String payment = "{\"Amount\":1,\"Invoice\":" + invoice + ",\"Customer\":" + customer + "}";
            String edit = "{\"Id\":" + invoice + ",\"Note\":\"round " + round + "\",\"Customer\":" + customer + "}";
            expectSaved(refused, round, "POST payment", restAssuredExecutor.executeWithResult(() -> given().contentType("application/json")
                                                                                                           .body(payment)
                                                                                                           .post(PAYMENTS)));
            Thread.sleep(PAUSE_MILLIS);
            expectSaved(refused, round, "PUT invoice", restAssuredExecutor.executeWithResult(() -> given().contentType("application/json")
                                                                                                          .body(edit)
                                                                                                          .put(INVOICES + "/" + invoice)));
        }
        assertEquals(List.of(), refused, "a sequential write was refused");

        float total = ROUNDS;
        awaitBody(INVOICES + "/" + invoice, "Paid", total);
        awaitBody(CUSTOMERS + "/" + customer, "Balance", total);
        awaitBody(ENTRIES + "/count", "count", ROUNDS);
    }

    /** Called once, never retried: a retry would turn the refusal this test looks for into a pass. */
    private static void expectSaved(List<String> refused, int round, String call, Response response) {
        if (response.statusCode() != 200) {
            refused.add("round " + round + ": " + call + " answered " + response.statusCode() + " " + response.body()
                                                                                                              .asString());
        }
    }

    private void awaitBody(String path, String property, Object expected) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(path)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(property, equalTo(expected)),
                DERIVED_WRITES_TIMEOUT_SECONDS);
    }

    private int create(String controller, String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(controller)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                TIMEOUT_SECONDS);
        return id.get();
    }

    private void generateProject() {
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

    private void write(String path, String content) {
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
