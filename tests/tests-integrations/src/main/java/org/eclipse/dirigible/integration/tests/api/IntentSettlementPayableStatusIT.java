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
 * The on-invoice settlement delegate settles only an invoice that is still payable (#7668), through
 * the published application.
 *
 * <p>
 * The delegate is wired as a {@code delegate:} step after the invoice becomes payable, and the step
 * is async, so it runs some time after the flow moved the invoice to ISSUED. An invoice voided in
 * that window was still settled: the delegate never looked at the status, while the payment-side
 * handlers skip every invoice outside {@code payableStatuses}. Here the window is made certain -
 * the invoice is created already VOIDED - and a {@code setField} step after the delegate marks when
 * it has run, so the absence of an allocation is asserted after the delegate had its chance, not
 * before. An ISSUED invoice of the same customer is the control.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentSettlementPayableStatusIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "settlepayable";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String CUSTOMERS = API + "/customer/CustomerController";
    private static final String INVOICES = API + "/invoice/InvoiceController";
    private static final String ALLOCATIONS = API + "/invoice/InvoicePaymentController";
    private static final String PAYMENTS = API + "/payment/PaymentController";
    private static final long TIMEOUT_SECONDS = 90;
    /** The delegate runs once the invoice's create event has started the instance. */
    private static final long PROCESS_TIMEOUT_SECONDS = 60;
    private static final int ISSUED = 1;
    private static final int VOIDED = 9;
    private static final String SETTLED = "settled";

    private static final String INTENT_YAML = """
            name: settlepayable
            description: settlement fixture - the on-invoice delegate leaves a retired invoice alone

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
                  - { name: note,  type: string, length: 20 }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }
                  - { name: Status,   kind: manyToOne, to: InvoiceStatus }

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

            settlements:
              - { name: autoSettle, junction: InvoicePayment, invoice: Invoice, payment: Payment,
                  amount: amount, total: total, paid: paid, pot: amount, order: date,
                  match: [Customer], status: Status, payableStatuses: [1] }

            processes:
              - name: InvoiceSettlement
                trigger: { onCreate: Invoice }
                steps:
                  - { name: allocatePayments, kind: serviceTask, args: { delegate: gen.events.AutoSettleOnInvoice, next: markSettled } }
                  - { name: markSettled,      kind: serviceTask, args: { setField: note, value: "settled", next: done } }
                  - { name: done,             kind: end }

            seeds:
              - name: invoice-statuses
                entity: InvoiceStatus
                rows:
                  - { id: 1, name: Issued }
                  - { id: 9, name: Voided }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void the_on_invoice_delegate_settles_a_payable_invoice_and_leaves_a_voided_one_open() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int customer = create(CUSTOMERS, "{\"Name\":\"payable\"}");
        int payment = create(PAYMENTS, "{\"Date\":\"2026-10-01\",\"Amount\":100,\"Customer\":" + customer + "}");

        int voided = create(INVOICES, invoiceBody(customer, VOIDED));
        awaitDelegateRan(voided);
        assertAllocations(voided, 0);

        int issued = create(INVOICES, invoiceBody(customer, ISSUED));
        awaitDelegateRan(issued);
        List<Map<String, Object>> rows = assertAllocations(issued, 1);
        assertEquals(payment, ((Number) rows.get(0)
                                            .get("Payment")).intValue(),
                "the issued invoice should be settled from the payment: " + rows);
        assertEquals(0, new BigDecimal(String.valueOf(rows.get(0)
                                                          .get("Amount"))).compareTo(new BigDecimal("40")),
                "the issued invoice should be settled in full: " + rows);

        // The voided invoice is still untouched once everything has run.
        assertAllocations(voided, 0);
    }

    private static String invoiceBody(int customer, int status) {
        return "{\"Date\":\"2026-10-02\",\"Total\":40,\"Customer\":" + customer + ",\"Status\":" + status + "}";
    }

    /** The step after the delegate stamps the note, so the delegate has run once it is there. */
    private void awaitDelegateRan(int invoice) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(INVOICES + "/" + invoice)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Note", equalTo(SETTLED)),
                PROCESS_TIMEOUT_SECONDS);
    }

    private List<Map<String, Object>> assertAllocations(int invoice, int count) {
        AtomicReference<List<Map<String, Object>>> rows = new AtomicReference<>();
        restAssuredExecutor.execute(() -> rows.set(given().when()
                                                          .get(ALLOCATIONS + "?Invoice=" + invoice)
                                                          .then()
                                                          .statusCode(200)
                                                          .body("", hasSize(count))
                                                          .extract()
                                                          .jsonPath()
                                                          .getList("")));
        return rows.get();
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
