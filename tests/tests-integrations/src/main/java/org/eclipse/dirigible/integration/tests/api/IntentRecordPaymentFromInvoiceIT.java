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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import io.restassured.path.json.JsonPath;

/**
 * Record a payment FROM the invoice it pays (#7748), through the published application.
 *
 * <p>
 * With an auto-settlement in place, a payment created on its own is spread over the customer's open
 * invoices oldest first - so a clerk who created one from the allocation row of the SECOND invoice
 * (the picker's inline "New") saw it land on the first, and the allocation they were entering was
 * then refused. The create-from's {@code link:} writes the allocation in the same unit of work as
 * the payment, so by the time the settlement listener sees the payment's create event the payment
 * is already fully allocated, and it touches nothing. The prompt's amount defaults to the invoice's
 * balance; a link row the invoice's capacity guard refuses takes the payment back with it; and the
 * allocation's payment picker offers no inline create ({@code inlineCreate: false}).
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentRecordPaymentFromInvoiceIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "recordpay";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String CUSTOMERS = API + "/customer/CustomerController";
    private static final String INVOICES = API + "/invoice/InvoiceController";
    private static final String ALLOCATIONS = API + "/invoice/InvoicePaymentController";
    private static final String PAYMENTS = API + "/payment/PaymentController";
    private static final String RECORD_PAYMENT = "/services/java/" + PROJECT + "/gen/events/" + PROJECT + "/RecordPaymentGenerate/run";
    private static final long TIMEOUT_SECONDS = 90;
    /** Settlement runs off the payment's create event, dispatched after the commit. */
    private static final long EVENT_TIMEOUT_SECONDS = 30;
    private static final int ITERATIONS = 5;

    private static final String INTENT_YAML = """
            name: recordpay
            description: record a payment from the invoice it pays - the allocation rides the payment's unit of work

            entities:
              - name: Customer
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: Invoice
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: date,    type: date }
                  - { name: number,  type: string, length: 20 }
                  - { name: total,   type: decimal, precision: 18, scale: 2 }
                  - { name: paid,    type: decimal, precision: 18, scale: 2 }
                  - { name: balance, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }

              - name: Payment
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: date,      type: date }
                  - { name: number,    type: string, length: 20 }
                  - { name: reference, type: string, length: 40 }
                  - { name: amount,    type: decimal, precision: 18, scale: 2, required: true }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }

              - name: InvoicePayment
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2, required: true }
                relations:
                  - { name: Invoice, kind: manyToOne, to: Invoice, composition: true, required: true }
                  - { name: Payment, kind: manyToOne, to: Payment, required: true, inlineCreate: false }

            rollups:
              # Capacity-bearing: the allocation's repository refuses rows summing above the invoice total.
              - { name: invoicePaid, entity: InvoicePayment, via: Invoice, field: paid, op: sum, of: amount, capacity: total }

            settlements:
              - { name: autoSettle, junction: InvoicePayment, invoice: Invoice, payment: Payment,
                  amount: amount, total: total, paid: paid, pot: amount, order: [date, number],
                  match: [Customer] }

            generates:
              - name: record-payment
                from: Invoice
                to: Payment
                label: Record payment
                map: { Customer: Customer }
                defaults: { date: now }
                prompt:
                  - { field: amount, required: true, default: balance }
                  - { field: reference }
                link:
                  entity: InvoicePayment
                  map: { amount: amount }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_payment_recorded_from_the_second_invoice_is_allocated_to_it_and_the_first_is_untouched() {
        generateProject();
        assertGeneratedUi();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        for (int i = 0; i < ITERATIONS; i++) {
            int customer = create(CUSTOMERS, "{\"Name\":\"c" + i + "\"}");
            int first = create(INVOICES, invoiceBody("2026-10-01", "A-" + i, 100, customer));
            int second = create(INVOICES, invoiceBody("2026-10-02", "B-" + i, 70, customer));

            // No amount posted: the prompt's default - the second invoice's balance - applies.
            int recorded = recordPayment(second, "{}");
            assertAllocations(second, List.of(Map.of(recorded, "70")));
            assertEquals(0, new BigDecimal(String.valueOf(fetch(PAYMENTS, recorded).get("Amount"))).compareTo(new BigDecimal("70")));

            // A payment created on its own afterwards is settled by the same listener, oldest first; once
            // it has landed on the first invoice, the recorded payment's event - published earlier - has
            // been handled too, and it must not have moved anything.
            int control = create(PAYMENTS, "{\"Date\":\"2026-10-03\",\"Amount\":10,\"Customer\":" + customer + "}");
            awaitAllocations(first, List.of(Map.of(control, "10")));
            assertAllocations(second, List.of(Map.of(recorded, "70")));
        }
    }

    @Test
    void an_explicit_amount_wins_and_a_refused_link_row_leaves_no_payment_behind() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int customer = create(CUSTOMERS, "{\"Name\":\"explicit\"}");
        int invoice = create(INVOICES, invoiceBody("2026-10-01", "E-1", 70, customer));
        int partial = recordPayment(invoice, "{\"values\":{\"Amount\":30,\"Reference\":\"wire 7\"}}");
        assertAllocations(invoice, List.of(Map.of(partial, "30")));
        assertEquals("wire 7", fetch(PAYMENTS, partial).get("Reference"));

        // More than the invoice can take: its capacity guard refuses the allocation, and the whole unit -
        // the payment included - is rolled back, the caller told why.
        int over = create(INVOICES, invoiceBody("2026-10-01", "O-1", 70, customer));
        int paymentsBefore = count(PAYMENTS);
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + over + ",\"values\":{\"Amount\":100}}")
                                                 .when()
                                                 .post(RECORD_PAYMENT)
                                                 .then()
                                                 .statusCode(400),
                TIMEOUT_SECONDS);
        assertEquals(paymentsBefore, count(PAYMENTS), "a refused allocation must take the payment back with it");
        assertAllocations(over, List.of());
    }

    /**
     * {@code inlineCreate: false}: no generated page offers "New Payment" beside the allocation's
     * payment picker - neither a form nor the item dialog's metadata - while a picker without the key
     * (the invoice's customer) keeps it.
     */
    private void assertGeneratedUi() {
        List<String> pages = new ArrayList<>();
        collect(repository.getCollection(PROJECT_PATH + "/gen"), pages);
        String all = String.join("\n", pages);
        assertFalse(all.contains("addRelated('Payment',"), "the payment picker must offer no inline create");
        assertTrue(all.contains("addRelated('Customer',"), "a picker without inlineCreate keeps its inline create");
        String register = new String(
                repository.getResource(PROJECT_PATH + "/gen/" + PROJECT + "/js/components/pages/Invoice/InvoicePayment.detail.js")
                          .getContent(),
                StandardCharsets.UTF_8);
        assertTrue(register.contains("entity: 'Payment', creatable: false"), register);
        String descriptor = new String(repository.getResource(PROJECT_PATH + "/record-payment-generate-action.js")
                                                 .getContent(),
                StandardCharsets.UTF_8);
        assertTrue(descriptor.contains("\"default\": \"Balance\"") && descriptor.contains("\"promptColumns\""), descriptor);
    }

    private static void collect(ICollection collection, List<String> pages) {
        for (IResource resource : collection.getResources()) {
            if (resource.getName()
                        .endsWith(".html")) {
                pages.add(new String(resource.getContent(), StandardCharsets.UTF_8));
            }
        }
        for (ICollection child : collection.getCollections()) {
            collect(child, pages);
        }
    }

    private static String invoiceBody(String date, String number, int total, int customer) {
        return "{\"Date\":\"" + date + "\",\"Number\":\"" + number + "\",\"Total\":" + total + ",\"Balance\":" + total + ",\"Customer\":"
                + customer + "}";
    }

    private int recordPayment(int invoice, String body) {
        String request = body.equals("{}") ? "{\"id\":" + invoice + "}" : "{\"id\":" + invoice + "," + body.substring(1);
        AtomicInteger id = new AtomicInteger();
        // The create-from answers its JSON as text, so the body is parsed rather than path-extracted.
        restAssuredExecutor.execute(() -> id.set(JsonPath.from(given().contentType("application/json")
                                                                      .body(request)
                                                                      .when()
                                                                      .post(RECORD_PAYMENT)
                                                                      .then()
                                                                      .statusCode(200)
                                                                      .extract()
                                                                      .asString())
                                                         .getInt("Id")),
                TIMEOUT_SECONDS);
        return id.get();
    }

    /** The invoice's allocations right now, as payment id -> amount, compared exactly. */
    private void assertAllocations(int invoice, List<Map<Integer, String>> expected) {
        restAssuredExecutor.execute(() -> assertAllocationsAs(invoice, expected));
    }

    private void awaitAllocations(int invoice, List<Map<Integer, String>> expected) {
        restAssuredExecutor.execute(() -> assertAllocationsAs(invoice, expected), EVENT_TIMEOUT_SECONDS);
    }

    /** Must run inside the executor, which authenticates the request. */
    private void assertAllocationsAs(int invoice, List<Map<Integer, String>> expected) {
        assertEquals(expected.stream()
                             .map(IntentRecordPaymentFromInvoiceIT::normalise)
                             .toList(),
                allocations(invoice), "allocations of invoice " + invoice);
    }

    private List<Map<Integer, BigDecimal>> allocations(int invoice) {
        List<Map<String, Object>> rows = given().when()
                                                .get(ALLOCATIONS + "?Invoice=" + invoice)
                                                .then()
                                                .statusCode(200)
                                                .extract()
                                                .jsonPath()
                                                .getList("");
        return rows.stream()
                   .map(row -> Map.of(((Number) row.get("Payment")).intValue(),
                           new BigDecimal(String.valueOf(row.get("Amount"))).stripTrailingZeros()))
                   .toList();
    }

    private static Map<Integer, BigDecimal> normalise(Map<Integer, String> allocation) {
        Map.Entry<Integer, String> entry = allocation.entrySet()
                                                     .iterator()
                                                     .next();
        return Map.of(entry.getKey(), new BigDecimal(entry.getValue()).stripTrailingZeros());
    }

    private Map<String, Object> fetch(String controller, int id) {
        AtomicReference<Map<String, Object>> record = new AtomicReference<>();
        restAssuredExecutor.execute(() -> record.set(given().when()
                                                            .get(controller + "/" + id)
                                                            .then()
                                                            .statusCode(200)
                                                            .extract()
                                                            .jsonPath()
                                                            .getMap("")));
        return record.get();
    }

    private int count(String controller) {
        AtomicInteger count = new AtomicInteger();
        restAssuredExecutor.execute(() -> count.set(given().when()
                                                           .get(controller + "/count")
                                                           .then()
                                                           .statusCode(200)
                                                           .extract()
                                                           .jsonPath()
                                                           .getInt("count")));
        return count.get();
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
