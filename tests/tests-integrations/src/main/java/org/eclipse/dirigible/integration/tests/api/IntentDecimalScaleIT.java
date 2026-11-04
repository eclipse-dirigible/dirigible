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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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

/**
 * A decimal is checked and calculated as the value its column stores - dirigible #8107, #8193.
 *
 * <p>
 * The checks and the calculated expressions read the payload's value, and the column then stored it
 * rounded to its scale: 0.0049 hours passed {@code hours > 0}, was stored as 0.00 and still billed
 * 0.49. The generated repository now rounds every decimal to its scale, half up, before anything
 * reads it - on the controllers' pre-write reads and on every write path - so a value that rounds
 * to zero is refused by the check, and the amount is priced on the stored hours.
 *
 * <p>
 * A total is one too (#8096): a line whose header total, or a child whose roll-up total, the parent
 * column cannot hold is refused with the module's translatable message and nothing is stored. The
 * document header used to fail its resum AFTER the line was stored (a 500, the line kept, the
 * footer short of it); the asynchronous roll-up answered 200 and then died on the parent column for
 * good.
 *
 * <p>
 * A whole number is stored as sent or refused (#8178): a fraction for an integer field used to be
 * cut, 2.9 stored as 2 with a 200.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentDecimalScaleIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "decimalscale";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String ALLOCATIONS = API + "/allocation/AllocationController";
    private static final String PROTOCOLS = API + "/protocol/ProtocolController";
    private static final String ORDERS = API + "/salesorder/SalesOrderController";
    private static final String ORDER_ITEMS = API + "/salesorder/SalesOrderItemController";
    private static final String ENTRIES = API + "/journalentry/JournalEntryController";
    private static final String ENTRY_ITEMS = API + "/journalentryitem/JournalEntryItemController";
    /** The roll-up runs off the child's topics, after the commit. */
    private static final long ROLLUP_TIMEOUT_SECONDS = 30;
    private static final long TIMEOUT_SECONDS = 90;

    private static final String INTENT_YAML = """
            name: decimalscale
            description: an allocation is billed on the hours its row stores

            entities:
              - name: Allocation
                checks:
                  - { kind: compare, field: hours, op: gt, value: 0, message: "Hours must be positive" }
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: hours,  type: decimal, precision: 18, scale: 2 }
                  - { name: rate,   type: decimal, precision: 18, scale: 2 }
                  - { name: amount, type: decimal, precision: 18, scale: 2,
                      calculatedOnCreate: "Hours * Rate", calculatedOnUpdate: "Hours * Rate" }
                  # #8178: a whole number, never cut from a fraction
                  - { name: days,   type: integer }
              # #8193: an `ne` bound, and a calculation reading an input of another scale than its own.
              - name: Protocol
                checks:
                  - { kind: compare, field: taxBase, op: ne, value: 0, message: "A protocol carries a tax base" }
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: taxBase, type: decimal, precision: 18, scale: 2 }
                  - { name: vatRate, type: decimal, precision: 9, scale: 4 }
                  - { name: vat,     type: decimal, precision: 18, scale: 2, calculatedOnCreate: "TaxBase * VatRate / 100" }
              # #8096: a document total resummed with every line, in a column narrower than two lines.
              - name: SalesOrder
                fields:
                  - { name: id,  type: integer, primaryKey: true, generated: true }
                  - { name: net, type: decimal, precision: 6, scale: 2, aggregate: true }
              - name: SalesOrderItem
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: quantity, type: decimal, precision: 18, scale: 2 }
                  - { name: price,    type: decimal, precision: 18, scale: 2 }
                  - { name: net,      type: decimal, precision: 6, scale: 2,
                      calculatedOnCreate: "Quantity * Price", calculatedOnUpdate: "Quantity * Price" }
                relations:
                  - { name: SalesOrder, kind: manyToOne, to: SalesOrder, composition: true, required: true }
              # #8096: the same, maintained by an asynchronous roll-up.
              - name: JournalEntry
                fields:
                  - { name: id,         type: integer, primaryKey: true, generated: true }
                  - { name: debitTotal, type: decimal, precision: 6, scale: 2 }
              - name: JournalEntryItem
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: debit, type: decimal, precision: 6, scale: 2 }
                relations:
                  - { name: JournalEntry, kind: manyToOne, to: JournalEntry }

            rollups:
              - { name: entryDebitTotal, entity: JournalEntryItem, via: JournalEntry, field: debitTotal, op: sum, of: debit }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_decimal_is_checked_and_calculated_as_the_value_its_column_stores() {
        deployProject();

        // Rounds to 0.00: the check refuses it instead of storing a zero row that bills 0.49.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Hours\":\"0.0049\",\"Rate\":\"100\"}")
                                                 .when()
                                                 .post(ALLOCATIONS)
                                                 .then()
                                                 .statusCode(400),
                TIMEOUT_SECONDS);

        // Rounds to 7.99: the amount is priced on the stored hours, 799.00 - not 799.49.
        AtomicInteger allocation = new AtomicInteger();
        restAssuredExecutor.execute(() -> allocation.set(given().contentType("application/json")
                                                                .body("{\"Hours\":\"7.9949\",\"Rate\":\"100\"}")
                                                                .when()
                                                                .post(ALLOCATIONS)
                                                                .then()
                                                                .statusCode(200)
                                                                .body("Hours", equalTo(7.99f))
                                                                .body("Amount", equalTo(799.0f))
                                                                .extract()
                                                                .path("Id")),
                TIMEOUT_SECONDS);

        // The update path holds the same rule: refused when it rounds to zero, priced on 2.01 (half up).
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Hours\":\"0.004\",\"Rate\":\"100\"}")
                                                 .when()
                                                 .put(ALLOCATIONS + "/" + allocation.get())
                                                 .then()
                                                 .statusCode(400));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Hours\":\"2.005\",\"Rate\":\"100\"}")
                                                 .when()
                                                 .put(ALLOCATIONS + "/" + allocation.get())
                                                 .then()
                                                 .statusCode(200));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ALLOCATIONS + "/" + allocation.get())
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Hours", equalTo(2.01f))
                                                 .body("Amount", equalTo(201.0f)));
    }

    @Test
    void a_fraction_for_an_integer_field_is_refused_on_create_and_update_never_cut() {
        deployProject();

        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Hours\":1,\"Rate\":1,\"Days\":2.9}")
                                                 .when()
                                                 .post(ALLOCATIONS)
                                                 .then()
                                                 .statusCode(400)
                                                 .body("message", equalTo("The 'Days' must be a whole number")),
                TIMEOUT_SECONDS);
        int allocation = create(ALLOCATIONS, "{\"Hours\":1,\"Rate\":1,\"Days\":2}");
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Hours\":1,\"Rate\":1,\"Days\":1.5}")
                                                 .when()
                                                 .put(ALLOCATIONS + "/" + allocation)
                                                 .then()
                                                 .statusCode(400)
                                                 .body("message", equalTo("The 'Days' must be a whole number")));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ALLOCATIONS + "/" + allocation)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Days", equalTo(2)));
    }

    @Test
    void a_not_equal_bound_and_a_calculation_from_an_input_of_another_scale_read_the_stored_value() {
        deployProject();

        // #8193: 0.001 rounds to 0.00, which `taxBase ne 0` refuses - no empty protocol is stored.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"TaxBase\":\"0.001\",\"VatRate\":\"20\"}")
                                                 .when()
                                                 .post(PROTOCOLS)
                                                 .then()
                                                 .statusCode(400),
                TIMEOUT_SECONDS);
        // A rate of 20.00004 is stored as 20.0000, so the VAT is 20 % of the base - 200000.00, not
        // the 200000.40 the payload's rate would compute.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"TaxBase\":\"1000000\",\"VatRate\":\"20.00004\"}")
                                                 .when()
                                                 .post(PROTOCOLS)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("VatRate", equalTo(20.0f))
                                                 .body("Vat", equalTo(200000.0f)));
    }

    @Test
    void a_line_whose_document_total_its_column_cannot_hold_is_refused_and_not_stored() {
        deployProject();
        int order = create(ORDERS, "{}");
        create(ORDER_ITEMS, "{\"SalesOrder\":" + order + ",\"Quantity\":1,\"Price\":6000}");

        // 12000.00 does not fit DECIMAL(6, 2): the header resum refuses it, and the line goes with it -
        // it used to be a 500 from the header's UPDATE with the line already stored.
        refused(ORDER_ITEMS, "{\"SalesOrder\":" + order + ",\"Quantity\":1,\"Price\":6000}", "checks.SalesOrder_overflow_Net");

        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ORDER_ITEMS)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("size()", equalTo(1)));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ORDERS + "/" + order)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Net", equalTo(6000.0f)));
    }

    @Test
    void a_child_whose_rollup_total_its_parent_column_cannot_hold_is_refused_and_not_stored() {
        deployProject();
        int entry = create(ENTRIES, "{}");
        create(ENTRY_ITEMS, "{\"JournalEntry\":" + entry + ",\"Debit\":6000}");
        awaitDebitTotal(entry);

        // Each debit fits its own column, the sum does not fit the entry's: refused up front - it used
        // to answer 200 and leave the roll-up dying on the entry's column, its total stale for good.
        refused(ENTRY_ITEMS, "{\"JournalEntry\":" + entry + ",\"Debit\":6000}", "checks.JournalEntry_overflow_DebitTotal");

        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ENTRY_ITEMS)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("size()", equalTo(1)));
        awaitDebitTotal(entry);
    }

    private void awaitDebitTotal(int entry) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ENTRIES + "/" + entry)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("DebitTotal", equalTo(6000.0f)),
                ROLLUP_TIMEOUT_SECONDS);
    }

    /** One attempt, no retry: a retried refusal that is not refused would store another row. */
    private void refused(String controller, String body, String messageKey) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(body)
                                                 .when()
                                                 .post(controller)
                                                 .then()
                                                 .statusCode(400)
                                                 .body("message", containsString("12000.00"))
                                                 .body("messageKey", endsWith(messageKey)));
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

    private void deployProject() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();
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
