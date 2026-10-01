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
import static org.hamcrest.Matchers.hasSize;
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
 * A hand-deleted settlement allocation STAYS deleted (#7557), through the published application.
 *
 * <p>
 * Deleting a junction row moves the payment's allocated roll-up, and that derived write publishes
 * the payment's {@code "-updated"}. The settlement's correction listener re-settled on it, picked
 * the same oldest open invoice and wrote the allocation straight back - so a person could neither
 * move the money to another document nor leave it on account. The claim is a runtime one (two
 * asynchronous listeners racing a REST delete), so it is asserted at the outermost layer: the
 * delete, the roll-ups settling at zero, and no allocation reappearing. The control half proves the
 * correction listener was not simply switched off: an authored amount correction of the same
 * payment still re-settles it.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentSettlementAllocationDeleteIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "settledelete";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String CUSTOMERS = API + "/customer/CustomerController";
    private static final String INVOICES = API + "/invoice/InvoiceController";
    private static final String ALLOCATIONS = API + "/invoice/InvoicePaymentController";
    private static final String PAYMENTS = API + "/payment/PaymentController";
    private static final long TIMEOUT_SECONDS = 90;
    /** Settlement and roll-ups run off events dispatched after the commit. */
    private static final long EVENT_TIMEOUT_SECONDS = 30;
    /**
     * How long a deleted allocation is watched for coming back. The re-settlement it guards against ran
     * within a second or two of the delete (the issue's repro saw the rows back "seconds later").
     */
    private static final long QUIET_MILLIS = 8000;

    private static final String INTENT_YAML = """
            name: settledelete
            description: settlement fixture - a hand-deleted allocation is not re-settled onto the same invoice

            entities:
              - name: Customer
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: Invoice
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: date,  type: date }
                  - { name: total, type: decimal, precision: 18, scale: 2 }
                  - { name: paid,  type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }

              - name: Payment
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: date,      type: date }
                  - { name: amount,    type: decimal, precision: 18, scale: 2, required: true }
                  - { name: allocated, type: decimal, precision: 18, scale: 2 }
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
              - { name: invoicePaid,      entity: InvoicePayment, via: Invoice, field: paid,      op: sum, of: amount }
              # The roll-up whose derived write used to trigger the re-settlement.
              - { name: paymentAllocated, entity: InvoicePayment, via: Payment, field: allocated, op: sum, of: amount }

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
    void a_hand_deleted_allocation_is_not_re_settled_but_an_amount_correction_still_is() throws InterruptedException {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int customer = create(CUSTOMERS, "{\"Name\":\"acme\"}");
        int invoice = create(INVOICES, "{\"Date\":\"2026-01-10\",\"Total\":100,\"Customer\":" + customer + "}");
        int payment = create(PAYMENTS, "{\"Date\":\"2026-01-20\",\"Amount\":48,\"Customer\":" + customer + "}");

        // The payment settles itself onto the open invoice.
        int allocation = awaitSingleAllocation(invoice, payment, "48");
        awaitDecimal(INVOICES + "/" + invoice, "Paid", "48");
        awaitDecimal(PAYMENTS + "/" + payment, "Allocated", "48");

        // A person takes the allocation off the invoice.
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete(ALLOCATIONS + "/" + allocation)
                                                 .then()
                                                 .statusCode(200));

        // Both roll-ups relinquish - the payment's is the derived "-updated" that used to re-settle.
        awaitDecimal(INVOICES + "/" + invoice, "Paid", "0");
        awaitDecimal(PAYMENTS + "/" + payment, "Allocated", "0");

        // ...and the allocation does not come back.
        Thread.sleep(QUIET_MILLIS);
        assertAllocationsOfInvoice(invoice, 0);
        awaitDecimal(INVOICES + "/" + invoice, "Paid", "0");

        // Control: an AUTHORED correction of the amount is still a reason to re-settle, so the fix did
        // not simply unbind the payment's correction listener.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + payment + ",\"Date\":\"2026-01-20\",\"Amount\":60,\"Customer\":"
                                                         + customer + "}")
                                                 .when()
                                                 .put(PAYMENTS + "/" + payment)
                                                 .then()
                                                 .statusCode(200));
        awaitSingleAllocation(invoice, payment, "60");
        awaitDecimal(INVOICES + "/" + invoice, "Paid", "60");
    }

    private int awaitSingleAllocation(int invoice, int payment, String amount) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> {
            List<Map<String, Object>> rows = given().when()
                                                    .get(ALLOCATIONS + "?Invoice=" + invoice)
                                                    .then()
                                                    .statusCode(200)
                                                    .body("", hasSize(1))
                                                    .extract()
                                                    .jsonPath()
                                                    .getList("");
            Map<String, Object> row = rows.get(0);
            assertEquals(payment, ((Number) row.get("Payment")).intValue(), "the allocation should come from the payment: " + row);
            assertEquals(0, new BigDecimal(String.valueOf(row.get("Amount"))).compareTo(new BigDecimal(amount)),
                    "the allocation should be " + amount + ": " + row);
            id.set(((Number) row.get("Id")).intValue());
        }, EVENT_TIMEOUT_SECONDS);
        return id.get();
    }

    private void assertAllocationsOfInvoice(int invoice, int expected) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ALLOCATIONS + "?Invoice=" + invoice)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("", hasSize(expected)));
    }

    private void awaitDecimal(String path, String field, String expected) {
        restAssuredExecutor.execute(() -> {
            Object value = given().when()
                                  .get(path)
                                  .then()
                                  .statusCode(200)
                                  .extract()
                                  .path(field);
            BigDecimal actual = value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
            assertEquals(0, actual.compareTo(new BigDecimal(expected)),
                    path + " " + field + " should be " + expected + " but was " + value);
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
