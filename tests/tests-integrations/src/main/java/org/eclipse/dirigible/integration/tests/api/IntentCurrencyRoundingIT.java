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

import io.restassured.path.json.JsonPath;
import io.restassured.path.json.config.JsonPathConfig;

/**
 * A money field is rounded to the minor units of the currency it is stated in - dirigible #8307,
 * #8327.
 *
 * <p>
 * A document's amounts were rounded to their column's scale whatever their currency, so a yen
 * invoice line of 3 x 333.33 stored Net 999.99 and billed fractions of a yen. {@code roundTo:
 * Invoice.Currency.rounding} on a decimal now rounds it, half up, to that currency's
 * {@code rounding} digits: a typed value, a calculated one (evaluated straight to those digits) and
 * a document total alike. A record that reaches no currency keeps its column's scale.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentCurrencyRoundingIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "currencyrounding";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String CURRENCIES = API + "/currency/CurrencyController";
    private static final String INVOICES = API + "/invoice/InvoiceController";
    private static final String INVOICE_ITEMS = API + "/invoice/InvoiceItemController";
    private static final long TIMEOUT_SECONDS = 90;

    /** Every money column holds four decimals, so a currency's fewer digits are visible in the row. */
    private static final String INTENT_YAML = """
            name: currencyrounding
            description: an invoice's amounts are rounded to its currency's minor units

            entities:
              - name: Currency
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: code,     type: string, length: 3 }
                  - { name: rounding, type: integer }
              - name: Invoice
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: net,     type: decimal, precision: 18, scale: 4, aggregate: true, roundTo: Currency.rounding }
                  - { name: total,   type: decimal, precision: 18, scale: 4, aggregate: true, roundTo: Currency.rounding }
                  - { name: fee,     type: decimal, precision: 18, scale: 4, roundTo: Currency.rounding }
                  - { name: payable, type: decimal, precision: 18, scale: 4, roundTo: Currency.rounding,
                      calculatedOnCreate: "Total + Fee", calculatedOnUpdate: "Total + Fee" }
                relations:
                  - { name: Currency, kind: manyToOne, to: Currency }
              - name: InvoiceItem
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: quantity, type: decimal, precision: 18, scale: 4 }
                  - { name: price,    type: decimal, precision: 18, scale: 4 }
                  - { name: net,      type: decimal, precision: 18, scale: 4, roundTo: Invoice.Currency.rounding,
                      calculatedOnCreate: "Quantity * Price", calculatedOnUpdate: "Quantity * Price" }
                  - { name: vat,      type: decimal, precision: 18, scale: 4, roundTo: Invoice.Currency.rounding,
                      calculatedOnCreate: "Net * 20 / 100", calculatedOnUpdate: "Net * 20 / 100" }
                  - { name: total,    type: decimal, precision: 18, scale: 4, roundTo: Invoice.Currency.rounding,
                      calculatedOnCreate: "Net + Vat", calculatedOnUpdate: "Net + Vat" }
                relations:
                  - { name: Invoice, kind: manyToOne, to: Invoice, composition: true, required: true }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void amounts_are_rounded_to_the_minor_units_of_the_invoice_currency() {
        deployProject();
        int yen = create(CURRENCIES, "{\"Code\":\"JPY\",\"Rounding\":0}");
        int dinar = create(CURRENCIES, "{\"Code\":\"KWD\",\"Rounding\":3}");

        // No minor units: 3 x 333.33 is 1000, not 999.99, and its 20 % is 200; 0.6 rounds up to 1 and
        // its 0.2 VAT down to 0.
        int yenInvoice = create(INVOICES, "{\"Currency\":" + yen + "}");
        int yenLine = create(INVOICE_ITEMS, "{\"Invoice\":" + yenInvoice + ",\"Quantity\":\"3\",\"Price\":\"333.33\"}");
        int yenSmallLine = create(INVOICE_ITEMS, "{\"Invoice\":" + yenInvoice + ",\"Quantity\":\"1\",\"Price\":\"0.6\"}");
        assertAmounts(INVOICE_ITEMS + "/" + yenLine, Map.of("Net", "1000", "Vat", "200", "Total", "1200"));
        assertAmounts(INVOICE_ITEMS + "/" + yenSmallLine, Map.of("Net", "1", "Vat", "0", "Total", "1"));
        assertAmounts(INVOICES + "/" + yenInvoice, Map.of("Net", "1001", "Total", "1201"));
        // A typed amount rounds too, and the header's calculated payable reads it rounded.
        update(INVOICES + "/" + yenInvoice, "{\"Id\":" + yenInvoice + ",\"Currency\":" + yen + ",\"Fee\":\"10.5\"}");
        assertAmounts(INVOICES + "/" + yenInvoice, Map.of("Net", "1001", "Total", "1201", "Fee", "11", "Payable", "1212"));

        // Three minor units: 0.1235 rounds to 0.124 (not the column's 0.1235), its VAT 0.0248 to 0.025.
        int dinarInvoice = create(INVOICES, "{\"Currency\":" + dinar + "}");
        int dinarLine = create(INVOICE_ITEMS, "{\"Invoice\":" + dinarInvoice + ",\"Quantity\":\"3\",\"Price\":\"333.3333\"}");
        int dinarSmallLine = create(INVOICE_ITEMS, "{\"Invoice\":" + dinarInvoice + ",\"Quantity\":\"1\",\"Price\":\"0.1235\"}");
        assertAmounts(INVOICE_ITEMS + "/" + dinarLine, Map.of("Net", "1000.000", "Vat", "200.000", "Total", "1200.000"));
        assertAmounts(INVOICE_ITEMS + "/" + dinarSmallLine, Map.of("Net", "0.124", "Vat", "0.025", "Total", "0.149"));
        assertAmounts(INVOICES + "/" + dinarInvoice, Map.of("Net", "1000.124", "Total", "1200.149"));
        update(INVOICES + "/" + dinarInvoice, "{\"Id\":" + dinarInvoice + ",\"Currency\":" + dinar + ",\"Fee\":\"0.0005\"}");
        assertAmounts(INVOICES + "/" + dinarInvoice, Map.of("Fee", "0.001", "Payable", "1200.150"));

        // A line update recomputes on the same digits.
        update(INVOICE_ITEMS + "/" + yenSmallLine,
                "{\"Id\":" + yenSmallLine + ",\"Invoice\":" + yenInvoice + ",\"Quantity\":\"1\",\"Price\":\"2.4\"}");
        assertAmounts(INVOICE_ITEMS + "/" + yenSmallLine, Map.of("Net", "2", "Vat", "0", "Total", "2"));
        assertAmounts(INVOICES + "/" + yenInvoice, Map.of("Net", "1002", "Total", "1202"));
    }

    @Test
    void an_invoice_without_a_currency_keeps_the_column_scale() {
        deployProject();
        int invoice = create(INVOICES, "{}");
        int line = create(INVOICE_ITEMS, "{\"Invoice\":" + invoice + ",\"Quantity\":\"3\",\"Price\":\"333.3333\"}");
        int smallLine = create(INVOICE_ITEMS, "{\"Invoice\":" + invoice + ",\"Quantity\":\"1\",\"Price\":\"0.1235\"}");
        assertAmounts(INVOICE_ITEMS + "/" + line, Map.of("Net", "999.9999", "Vat", "200.0000", "Total", "1199.9999"));
        assertAmounts(INVOICE_ITEMS + "/" + smallLine, Map.of("Net", "0.1235", "Vat", "0.0247", "Total", "0.1482"));
        assertAmounts(INVOICES + "/" + invoice, Map.of("Net", "1000.1234", "Total", "1200.1481"));
    }

    private int create(String url, String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(url)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                TIMEOUT_SECONDS);
        return id.get();
    }

    private void update(String url, String body) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(body)
                                                 .when()
                                                 .put(url)
                                                 .then()
                                                 .statusCode(200),
                TIMEOUT_SECONDS);
    }

    /**
     * Compares the amounts as decimals, read exactly - a float cannot tell 1200.1481 from 1200.1482.
     */
    private void assertAmounts(String url, Map<String, String> expected) {
        restAssuredExecutor.execute(() -> {
            String body = given().when()
                                 .get(url)
                                 .then()
                                 .statusCode(200)
                                 .extract()
                                 .asString();
            JsonPath json = JsonPath.from(body)
                                    .using(new JsonPathConfig(JsonPathConfig.NumberReturnType.BIG_DECIMAL));
            for (Map.Entry<String, String> amount : expected.entrySet()) {
                Object value = json.get(amount.getKey());
                assertEquals(0, new BigDecimal(amount.getValue()).compareTo(new BigDecimal(String.valueOf(value))),
                        url + " " + amount.getKey() + ": expected " + amount.getValue() + ", got " + value + " in " + body);
            }
        }, TIMEOUT_SECONDS);
    }

    private void deployProject() {
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
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT + "/")
                                                 .then()
                                                 .statusCode(200));
        synchronizationProcessor.forceProcessSynchronizers();
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
