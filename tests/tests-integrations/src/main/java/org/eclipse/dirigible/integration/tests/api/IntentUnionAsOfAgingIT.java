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
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
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

/**
 * dirigible #8619: an aged receivables report AS OF a past date, per customer and currency, over a
 * {@code union:} whose payment member is the allocation row - its customer and currency one hop
 * away through its invoice, its date the date of a payment another model owns - end to end on two
 * generated, published projects. A payment dated after the as-of date must not reduce what was open
 * on it.
 */
class IntentUnionAsOfAgingIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    /** Owns the customer payments. */
    private static final String OWNER = "asof-payments";
    /** Owns the invoices, their allocations and the aging report. */
    private static final String CONSUMER = "asof-sales";

    private static final String OWNER_MODEL = "payments";
    private static final String CONSUMER_MODEL = "sales";

    private static final String OWNER_INTENT = """
            name: payments
            description: as-of aging fixture - the payments, dated when the money came in

            entities:
              - name: CustomerPayment
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: date,   type: date, required: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2 }
            """;

    private static final String CONSUMER_INTENT = """
            name: sales
            description: as-of aging fixture - invoices settled by allocation rows

            uses:
              - { model: payments, project: asof-payments }

            entities:
              - name: Customer
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }
              - name: Currency
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: code, type: string, required: true, length: 3 }
              - name: SalesInvoice
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: date,    type: date }
                  - { name: due,     type: date }
                  - { name: payable, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer, required: true }
                  - { name: Currency, kind: manyToOne, to: Currency, required: true }
              - name: SalesInvoiceCustomerPayment
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: SalesInvoice,    kind: manyToOne, to: SalesInvoice, composition: true, required: true }
                  - { name: CustomerPayment, kind: manyToOne, to: CustomerPayment, model: payments, required: true }

            reports:
              - name: AgedReceivables
                union:
                  - { source: SalesInvoice, counterparty: Customer, columns: { currency: Currency },
                      date: date, due: due, amount: payable }
                  - { source: SalesInvoiceCustomerPayment, counterparty: SalesInvoice.Customer,
                      columns: { currency: SalesInvoice.Currency }, date: CustomerPayment.date, due: SalesInvoice.due,
                      amount: "-amount", filter: "SalesInvoice.payable > 0" }
                dimensions: [counterparty.name, currency.code]
                aging: { buckets: [30, 60, 90] }
            """;

    @Autowired
    private IRepository repository;
    @Autowired
    private RestAssuredExecutor restAssuredExecutor;
    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_payment_dated_after_the_as_of_date_leaves_the_invoice_open_on_it() {
        writeResource(OWNER, "app.intent", OWNER_INTENT);
        generateProject(OWNER);
        writeResource(CONSUMER, "app.intent", CONSUMER_INTENT);
        generateProject(CONSUMER);

        publishProject(OWNER);
        publishProject(CONSUMER);
        synchronizationProcessor.forceProcessSynchronizers();

        String ownerApi = "/services/java/" + OWNER + "/gen/" + OWNER_MODEL + "/api";
        String consumerApi = "/services/java/" + CONSUMER + "/gen/" + CONSUMER_MODEL + "/api";
        String payments = ownerApi + "/customerpayment/CustomerPaymentController";
        String customers = consumerApi + "/customer/CustomerController";
        String currencies = consumerApi + "/currency/CurrencyController";
        String invoices = consumerApi + "/salesinvoice/SalesInvoiceController";
        String allocations = consumerApi + "/salesinvoice/SalesInvoiceCustomerPaymentController";

        int acme = create(customers, "{\"Name\":\"Acme\"}");
        int eur = create(currencies, "{\"Code\":\"EUR\"}");
        int usd = create(currencies, "{\"Code\":\"USD\"}");
        // The issue's acceptance case: 120.00 dated 10-01, due 10-31, fully paid by a payment of 10-20.
        int invoice = create(invoices,
                "{\"Customer\":" + acme + ",\"Currency\":" + eur + ",\"Date\":\"2026-10-01\",\"Due\":\"2026-10-31\",\"Payable\":120}");
        int payment = create(payments, "{\"Date\":\"2026-10-20\",\"Amount\":120}");
        create(allocations, "{\"SalesInvoice\":" + invoice + ",\"CustomerPayment\":" + payment + ",\"Amount\":120}");
        // Another currency is its own row.
        create(invoices,
                "{\"Customer\":" + acme + ",\"Currency\":" + usd + ",\"Date\":\"2026-09-01\",\"Due\":\"2026-09-30\",\"Payable\":50}");

        Map<String, Map<String, Object>> asOf15 = aging("2026-10-15");
        assertAmount("120", asOf15.get("Acme EUR"), "Not Yet Due");
        assertAmount("120", asOf15.get("Acme EUR"), "Open Amount");
        assertAmount("50", asOf15.get("Acme USD"), "Overdue 0-30");
        assertAmount("50", asOf15.get("Acme USD"), "Open Amount");

        Map<String, Map<String, Object>> asOf25 = aging("2026-10-25");
        // Paid in full by then: the invoice and its payment net to nothing.
        if (asOf25.containsKey("Acme EUR")) {
            assertAmount("0", asOf25.get("Acme EUR"), "Open Amount");
        }
        assertAmount("50", asOf25.get("Acme USD"), "Open Amount");
    }

    /** The aging report as of the date, by "customer currency". */
    private Map<String, Map<String, Object>> aging(String asOf) {
        AtomicReference<List<Map<String, Object>>> rows = new AtomicReference<>();
        restAssuredExecutor.execute(() -> rows.set(given().queryParam("asOf", asOf)
                                                          .when()
                                                          .get("/services/java/" + CONSUMER
                                                                  + "/gen/agedreceivables/api/reports/AgedReceivablesController")
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .jsonPath()
                                                          .getList("$")),
                60);
        Map<String, Map<String, Object>> byKey = new LinkedHashMap<>();
        for (Map<String, Object> row : rows.get()) {
            byKey.put(row.get("Counterparty Name") + " " + row.get("Currency Code"), row);
        }
        return byKey;
    }

    private static void assertAmount(String expected, Map<String, Object> row, String column) {
        assertEquals(0, new BigDecimal(expected).compareTo(new BigDecimal(String.valueOf(row.get(column)))), column + " in " + row);
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
                60);
        return id.get();
    }

    /**
     * Generate the model files from the intent and drive model-to-code from the generate response's own
     * plan.
     */
    private void generateProject(String project) {
        AtomicReference<List<Map<String, Object>>> plan = new AtomicReference<>();
        restAssuredExecutor.execute(() -> plan.set(given().when()
                                                          .post("/services/ide/intent/generate?workspace=" + WORKSPACE + "&project="
                                                                  + project + "&path=app.intent")
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

    private void publishProject(String project) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + project + "/")
                                                 .then()
                                                 .statusCode(200));
    }

    private void writeResource(String project, String file, String content) {
        String path = projectPath(project) + "/" + file;
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(content.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String projectPath(String project) {
        return IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + project;
    }

    @AfterEach
    void cleanup() {
        for (String project : List.of(CONSUMER, OWNER)) {
            restAssuredExecutor.execute(() -> given().when()
                                                     .delete("/services/ide/publisher/" + WORKSPACE + "/" + project)
                                                     .then()
                                                     .statusCode(both(greaterThanOrEqualTo(200)).and(lessThan(300))));
            if (repository.hasCollection(projectPath(project))) {
                repository.removeCollection(projectPath(project));
            }
        }
    }
}
