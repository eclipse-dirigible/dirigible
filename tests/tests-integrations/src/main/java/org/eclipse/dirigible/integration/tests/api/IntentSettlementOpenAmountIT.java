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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
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

/**
 * A settlement sizes an allocation from the invoice's allocation ROWS, not from its paid column
 * (#7559), through the published application.
 *
 * <p>
 * The paid column is a roll-up, maintained asynchronously, and it lags the rows. A payment event
 * arriving inside that lag sized an allocation from the stale figure; the junction's capacity guard
 * - which re-sums the rows - refused it, the listener failed with an ERROR, and the part of the
 * payment the same pass would have spent on the LATER invoices stayed unallocated. The race is a
 * matter of timing, so the fixture makes the lag permanent instead: the capacity-bearing roll-up
 * keeps another column, and the settlement's {@code paid} column is never written. Sized from the
 * column, the correction below asks the first invoice for its whole total again and stops at the
 * guard; sized from the rows, it takes only what is left on it and carries the rest to the second.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentSettlementOpenAmountIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "settleopen";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String CUSTOMERS = API + "/customer/CustomerController";
    private static final String INVOICES = API + "/invoice/InvoiceController";
    private static final String ALLOCATIONS = API + "/invoice/InvoicePaymentController";
    private static final String PAYMENTS = API + "/payment/PaymentController";
    private static final long TIMEOUT_SECONDS = 90;
    /** Settlement runs off the payment's events, dispatched after the commit. */
    private static final long EVENT_TIMEOUT_SECONDS = 30;

    private static final String INTENT_YAML = """
            name: settleopen
            description: settlement fixture - an allocation is sized from the invoice's rows, not its lagging paid column

            entities:
              - name: Customer
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: Invoice
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: date,    type: date }
                  - { name: total,   type: decimal, precision: 18, scale: 2 }
                  # The settlement's paid column, deliberately kept by no roll-up: permanently stale.
                  - { name: paid,    type: decimal, precision: 18, scale: 2 }
                  - { name: settled, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }

              - name: Payment
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: date,   type: date }
                  - { name: amount, type: decimal, precision: 18, scale: 2, required: true }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }

              - name: InvoicePayment
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2, required: true }
                relations:
                  - { name: Invoice, kind: manyToOne, to: Invoice, composition: true, required: true }
                  - { name: Payment, kind: manyToOne, to: Payment, required: true }

            rollups:
              # Capacity-bearing, so the junction carries the guard that re-sums the rows.
              - { name: invoiceSettled, entity: InvoicePayment, via: Invoice, field: settled, op: sum, of: amount, capacity: total }

            settlements:
              - { name: autoSettle, junction: InvoicePayment, invoice: Invoice, payment: Payment,
                  amount: amount, total: total, paid: paid, pot: amount, order: date,
                  match: [Customer] }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_correction_takes_only_what_is_left_on_the_older_invoice_and_carries_the_rest() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int customer = create(CUSTOMERS, "{\"Name\":\"acme\"}");
        int older = create(INVOICES, "{\"Date\":\"2026-09-01\",\"Total\":120,\"Customer\":" + customer + "}");
        int newer = create(INVOICES, "{\"Date\":\"2026-09-15\",\"Total\":200,\"Customer\":" + customer + "}");
        int payment = create(PAYMENTS, "{\"Date\":\"2026-09-20\",\"Amount\":100,\"Customer\":" + customer + "}");

        awaitAllocated(older, "100");
        awaitAllocated(newer, "0");

        // The payment is corrected upwards: 200 more to spread. The older invoice can take 20 of it.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + payment + ",\"Date\":\"2026-09-20\",\"Amount\":300,\"Customer\":"
                                                         + customer + "}")
                                                 .when()
                                                 .put(PAYMENTS + "/" + payment)
                                                 .then()
                                                 .statusCode(200));

        awaitAllocated(older, "120");
        awaitAllocated(newer, "180");
    }

    /** The sum of the invoice's allocation rows - what the capacity guard counts. */
    private void awaitAllocated(int invoice, String expected) {
        restAssuredExecutor.execute(() -> {
            List<Map<String, Object>> rows = given().when()
                                                    .get(ALLOCATIONS + "?Invoice=" + invoice)
                                                    .then()
                                                    .statusCode(200)
                                                    .extract()
                                                    .jsonPath()
                                                    .getList("");
            BigDecimal allocated = BigDecimal.ZERO;
            for (Map<String, Object> row : rows) {
                allocated = allocated.add(new BigDecimal(String.valueOf(row.get("Amount"))));
            }
            assertEquals(0, allocated.compareTo(new BigDecimal(expected)),
                    "invoice " + invoice + " should carry " + expected + " in allocations: " + rows);
        }, EVENT_TIMEOUT_SECONDS);
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
