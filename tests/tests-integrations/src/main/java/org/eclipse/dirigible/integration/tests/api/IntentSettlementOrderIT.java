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
 * A settlement pays the OLDEST open document first, and oldest is a total order (#7556), through
 * the published application.
 *
 * <p>
 * The generated allocation query ordered by the authored {@code order:} field alone, so documents
 * sharing it - every invoice issued on the same day - came back in whatever order the database
 * returned them, and a payment paid the newer invoice in full while leaving the older one partly
 * open. Two halves: an {@code order:} LIST is honoured field by field (the numbers here run against
 * creation order, so no database returns that order by accident), and documents agreeing on every
 * authored field fall back to creation order. The second half is the issue's own shape - the older
 * invoice was written again after the newer one was created, as issuing it does - which is what
 * moves its row behind the newer one in a PostgreSQL heap scan.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentSettlementOrderIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "settleorder";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String CUSTOMERS = API + "/customer/CustomerController";
    private static final String INVOICES = API + "/invoice/InvoiceController";
    private static final String ALLOCATIONS = API + "/invoice/InvoicePaymentController";
    private static final String PAYMENTS = API + "/payment/PaymentController";
    private static final long TIMEOUT_SECONDS = 90;
    /** Settlement runs off the payment's create event, dispatched after the commit. */
    private static final long EVENT_TIMEOUT_SECONDS = 30;
    private static final String SAME_DAY = "2026-09-30";

    private static final String INTENT_YAML = """
            name: settleorder
            description: settlement fixture - same-day documents settle in a total, oldest-first order

            entities:
              - name: Customer
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: Invoice
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: date,   type: date }
                  - { name: number, type: string, length: 20 }
                  - { name: total,  type: decimal, precision: 18, scale: 2 }
                  - { name: paid,   type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }

              - name: Payment
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: date,   type: date }
                  - { name: number, type: string, length: 20 }
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

            settlements:
              - { name: autoSettle, junction: InvoicePayment, invoice: Invoice, payment: Payment,
                  amount: amount, total: total, paid: paid, pot: amount, order: [date, number],
                  match: [Customer] }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void same_day_invoices_settle_by_the_order_list_and_then_in_creation_order() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        // The order list: S-2 was created first, but S-1 is the lower number on the same day.
        int listCustomer = create(CUSTOMERS, "{\"Name\":\"list\"}");
        int createdFirst = create(INVOICES, invoiceBody(null, "S-2", listCustomer));
        int lowerNumber = create(INVOICES, invoiceBody(null, "S-1", listCustomer));
        int listPayment = create(PAYMENTS, paymentBody(listCustomer));

        awaitAllocation(lowerNumber, listPayment, "144");
        awaitAllocation(createdFirst, listPayment, "36");

        // The tie-break: same day, same number - the older invoice is paid first even though it was
        // written again after the newer one was created.
        int tieCustomer = create(CUSTOMERS, "{\"Name\":\"tie\"}");
        int older = create(INVOICES, invoiceBody(null, "T-1", tieCustomer));
        int newer = create(INVOICES, invoiceBody(null, "T-1", tieCustomer));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(invoiceBody(older, "T-1", tieCustomer))
                                                 .when()
                                                 .put(INVOICES + "/" + older)
                                                 .then()
                                                 .statusCode(200));
        int tiePayment = create(PAYMENTS, paymentBody(tieCustomer));

        awaitAllocation(older, tiePayment, "144");
        awaitAllocation(newer, tiePayment, "36");
    }

    private static String invoiceBody(Integer id, String number, int customer) {
        return "{" + (id == null ? "" : "\"Id\":" + id + ",") + "\"Date\":\"" + SAME_DAY + "\",\"Number\":\"" + number
                + "\",\"Total\":144,\"Customer\":" + customer + "}";
    }

    private static String paymentBody(int customer) {
        return "{\"Date\":\"" + SAME_DAY + "\",\"Number\":\"P-1\",\"Amount\":180,\"Customer\":" + customer + "}";
    }

    private void awaitAllocation(int invoice, int payment, String amount) {
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
                    "invoice " + invoice + " should be allocated " + amount + ": " + row);
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
