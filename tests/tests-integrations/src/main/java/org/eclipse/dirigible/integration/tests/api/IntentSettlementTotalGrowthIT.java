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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
 * An invoice whose total grows after it became payable is settled from the money its payer already
 * has on account (#8489), through the published application.
 *
 * <p>
 * A settlement used to run on a payment write and on the {@code <Name>OnInvoice} step only, so a
 * total raised later - a line added to an issued invoice, a debit note rolled up onto a bill - left
 * the invoice PARTIAL while its payer's unallocated money sat on account. Here the invoice's total
 * is a roll-up over its charges: the payment settles the first 100 of it as before, and every
 * charge added afterwards must draw on the 50 left on account - up to the open amount, never beyond
 * what the payment carries. A DRAFT invoice of the same payer, older and so first in line, is the
 * control for the payable test; an allocation deleted by hand is the control for "only a growth of
 * the total": the paid roll-up's own write must not put it back.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentSettlementTotalGrowthIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "settlegrow";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String CUSTOMERS = API + "/customer/CustomerController";
    private static final String INVOICES = API + "/invoice/InvoiceController";
    private static final String CHARGES = API + "/invoice/InvoiceChargeController";
    private static final String ALLOCATIONS = API + "/invoice/InvoicePaymentController";
    private static final String PAYMENTS = API + "/payment/PaymentController";
    private static final long TIMEOUT_SECONDS = 90;
    /** Settlement and roll-ups run off events dispatched after the commit. */
    private static final long EVENT_TIMEOUT_SECONDS = 30;
    /** How long a figure that must NOT move is watched. */
    private static final long QUIET_SECONDS = 6;
    private static final int DRAFT = 1;
    private static final int ISSUED = 2;

    private static final String INTENT_YAML = """
            name: settlegrow
            description: settlement fixture - a total that grows after the invoice became payable draws on the money on account

            entities:
              - name: Customer
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: InvoiceStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 50 }

              - name: Invoice
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: date,  type: date }
                  - { name: total, type: decimal, precision: 18, scale: 2 }
                  - { name: paid,  type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }
                  - { name: Status,   kind: manyToOne, to: InvoiceStatus }

              - name: InvoiceCharge
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2, required: true }
                relations:
                  - { name: Invoice, kind: manyToOne, to: Invoice, composition: true, required: true }

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
              # The invoice's total is a derived write: it grows with every charge added.
              - { name: invoiceTotal, entity: InvoiceCharge, via: Invoice, field: total, op: sum, of: amount }
              # The paid roll-up, capacity-bearing, so the junction carries the guard.
              - { name: invoicePaid,  entity: InvoicePayment, via: Invoice, field: paid, op: sum, of: amount, capacity: total }

            settlements:
              - { name: autoSettle, junction: InvoicePayment, invoice: Invoice, payment: Payment,
                  amount: amount, total: total, paid: paid, pot: amount, order: date,
                  match: [Customer], status: Status, payableStatuses: [2] }

            seeds:
              - name: invoice-statuses
                entity: InvoiceStatus
                rows:
                  - { id: 1, name: Draft }
                  - { id: 2, name: Issued }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_total_raised_after_the_invoice_became_payable_draws_on_the_money_on_account() throws InterruptedException {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int customer = create(CUSTOMERS, "{\"Name\":\"acme\"}");
        int draft = create(INVOICES, invoiceBody(customer, "2026-09-01", DRAFT));
        create(CHARGES, chargeBody(draft, "70"));
        awaitInvoice(draft, "70", "0");
        int issued = create(INVOICES, invoiceBody(customer, "2026-09-10", ISSUED));
        create(CHARGES, chargeBody(issued, "100"));
        awaitInvoice(issued, "100", "0");

        // The payment settles the issued invoice as it always did: 100 of 150, 50 left on account.
        int payment = create(PAYMENTS, "{\"Date\":\"2026-09-20\",\"Amount\":150,\"Customer\":" + customer + "}");
        awaitAllocated("?Invoice=" + issued, "100");

        // The total grows by 10 after the invoice became payable: 10 of the 50 on account is applied.
        create(CHARGES, chargeBody(issued, "10"));
        awaitAllocated("?Invoice=" + issued, "110");

        // It grows by 100 more: only the 40 still on account is applied, never more than the payment.
        create(CHARGES, chargeBody(issued, "100"));
        awaitAllocated("?Invoice=" + issued, "150");
        awaitAllocated("?Payment=" + payment, "150");

        // Another growth with nothing left on account allocates nothing.
        create(CHARGES, chargeBody(issued, "5"));
        awaitInvoice(issued, "215", "150");
        assertStays("?Payment=" + payment, "150");
        assertStays("?Invoice=" + draft, "0");

        // An allocation deleted by hand lowers the paid roll-up, not the total: it stays deleted.
        Map<String, Object> newest = allocations("?Invoice=" + issued).stream()
                                                                      .max(Comparator.comparingInt(
                                                                              row -> ((Number) row.get("Id")).intValue()))
                                                                      .orElseThrow();
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete(ALLOCATIONS + "/" + newest.get("Id"))
                                                 .then()
                                                 .statusCode(greaterThanOrEqualTo(200)));
        String remaining = new BigDecimal("150").subtract(new BigDecimal(String.valueOf(newest.get("Amount"))))
                                                .toPlainString();
        awaitInvoice(issued, "215", remaining);
        assertStays("?Invoice=" + issued, remaining);
    }

    private static String invoiceBody(int customer, String date, int status) {
        return "{\"Date\":\"" + date + "\",\"Customer\":" + customer + ",\"Status\":" + status + "}";
    }

    private static String chargeBody(int invoice, String amount) {
        return "{\"Invoice\":" + invoice + ",\"Amount\":" + amount + "}";
    }

    /** The invoice's total and paid roll-ups, once both have caught up. */
    private void awaitInvoice(int invoice, String total, String paid) {
        restAssuredExecutor.execute(() -> {
            Map<String, Object> row = given().when()
                                             .get(INVOICES + "/" + invoice)
                                             .then()
                                             .statusCode(200)
                                             .extract()
                                             .jsonPath()
                                             .getMap("");
            assertEquals(0, decimal(row.get("Total")).compareTo(new BigDecimal(total)), "invoice " + invoice + " total: " + row);
            assertEquals(0, decimal(row.get("Paid")).compareTo(new BigDecimal(paid)), "invoice " + invoice + " paid: " + row);
        }, EVENT_TIMEOUT_SECONDS);
    }

    private void awaitAllocated(String filter, String expected) {
        restAssuredExecutor.execute(() -> assertEquals(0, sum(allocations(filter)).compareTo(new BigDecimal(expected)),
                "allocations " + filter + " should sum to " + expected + ": " + allocations(filter)), EVENT_TIMEOUT_SECONDS);
    }

    /** A figure that must not move: watched for a while after everything that could move it ran. */
    private void assertStays(String filter, String expected) throws InterruptedException {
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(QUIET_SECONDS);
        while (System.nanoTime() < until) {
            List<Map<String, Object>> rows = allocations(filter);
            assertEquals(0, sum(rows).compareTo(new BigDecimal(expected)),
                    "allocations " + filter + " should stay " + expected + ": " + rows);
            TimeUnit.MILLISECONDS.sleep(500);
        }
    }

    /** Read through the executor, which authenticates the request - also outside an await. */
    private List<Map<String, Object>> allocations(String filter) {
        AtomicReference<List<Map<String, Object>>> rows = new AtomicReference<>();
        restAssuredExecutor.execute(() -> rows.set(given().when()
                                                          .get(ALLOCATIONS + filter)
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .jsonPath()
                                                          .getList("")));
        return rows.get();
    }

    private static BigDecimal sum(List<Map<String, Object>> rows) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Map<String, Object> row : rows) {
            sum = sum.add(decimal(row.get("Amount")));
        }
        return sum;
    }

    private static BigDecimal decimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(String.valueOf(value));
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
