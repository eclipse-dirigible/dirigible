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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import io.restassured.path.json.JsonPath;
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
 * A {@code resolves:} lookup that copies a scalar prices the line on the write itself, and the form
 * asks it before showing the line (#7911).
 *
 * <p>
 * The copy ran in an asynchronous listener AFTER the line was stored, and only into an empty field.
 * The line form always fills the price first - its {@code dependsOn} default from the product - so
 * a line entered in the form was never priced from the list; a REST create that left the price
 * empty was refused when the price is required, before the listener could fill it; and where it is
 * optional the listener's targeted write set the price while the line's calculated amount and the
 * document total stayed computed from no price at all.
 */
class IntentResolveCopyOnWriteIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "resolvecopy";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api/";
    private static final String PRICE_LISTS = API + "pricelist/PriceListController";
    private static final String PRODUCTS = API + "product/ProductController";
    private static final String CUSTOMERS = API + "customer/CustomerController";
    private static final String PRICE_LIST_ITEMS = API + "pricelistitem/PriceListItemController";
    private static final String INVOICES = API + "salesinvoice/SalesInvoiceController";
    private static final String LINES = API + "salesinvoice/SalesInvoiceItemController";

    private static final String INTENT_YAML = """
            name: resolvecopy
            description: a line priced from the price list in force on its invoice's date

            entities:
              - name: PriceList
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 50 }
              - name: Product
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: name,  type: string, length: 50 }
                  - { name: price, type: decimal, precision: 18, scale: 2 }
              - name: Customer
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 50 }
                relations:
                  - { name: PriceList, kind: manyToOne, to: PriceList }
              - name: PriceListItem
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: price,     type: decimal, precision: 18, scale: 2 }
                  - { name: validFrom, type: date }
                  - { name: validTo,   type: date }
                relations:
                  - { name: PriceList, kind: manyToOne, to: PriceList }
                  - { name: Product,   kind: manyToOne, to: Product }
              - name: SalesInvoice
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: issuedOn, type: date }
                  - { name: net,      type: decimal, precision: 18, scale: 2, aggregate: true }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }
              - name: SalesInvoiceItem
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: quantity, type: decimal, precision: 18, scale: 2, required: true }
                  # the form's default is the product's own price - what kept the list price out (#7911)
                  - { name: price,    type: decimal, precision: 18, scale: 2, required: true,
                      dependsOn: { relation: Product, valueFrom: price } }
                  - { name: net,      type: decimal, precision: 18, scale: 2,
                      calculatedOnCreate: "round(Quantity * Price, 2)", calculatedOnUpdate: "round(Quantity * Price, 2)" }
                relations:
                  - { name: SalesInvoice,  kind: manyToOne, to: SalesInvoice, composition: true, required: true }
                  - { name: Product,       kind: manyToOne, to: Product }
                  - { name: PriceListItem, kind: manyToOne, to: PriceListItem }

            resolves:
              - name: priceFromList
                event: { onCreate: SalesInvoiceItem }
                set: PriceListItem
                from: PriceListItem
                match: { Product: Product, PriceList: SalesInvoice.Customer.PriceList }
                between: { start: validFrom, end: validTo, value: SalesInvoice.issuedOn }
                copy: { price: price }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_line_is_priced_from_the_list_in_the_form_and_on_its_create() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int wholesale = id(post(PRICE_LISTS, "{\"Name\":\"Wholesale\"}"));
        int widget = id(post(PRODUCTS, "{\"Name\":\"Widget\",\"Price\":100}"));
        int customer = id(post(CUSTOMERS, "{\"Name\":\"Acme\",\"PriceList\":" + wholesale + "}"));
        // Last year's row no longer covers, this year's open-ended one does.
        post(PRICE_LIST_ITEMS, "{\"PriceList\":" + wholesale + ",\"Product\":" + widget
                + ",\"Price\":70,\"ValidFrom\":\"2025-01-01\",\"ValidTo\":\"2025-12-31\"}");
        post(PRICE_LIST_ITEMS, "{\"PriceList\":" + wholesale + ",\"Product\":" + widget + ",\"Price\":80,\"ValidFrom\":\"2026-01-01\"}");
        int invoice = id(post(INVOICES, "{\"Customer\":" + customer + ",\"IssuedOn\":\"2026-06-01\"}"));

        // The form asks before it shows the line: the draft as the dialog holds it once the product is
        // picked, the price emptied so the product's own price does not stand in for the list's.
        JsonPath asked = post(LINES + "/resolve", "{\"SalesInvoice\":" + invoice + ",\"Product\":" + widget + ",\"Quantity\":2}");
        assertDecimal("80", asked.get("Price"), "the form must be answered the list price");

        // The same line saved without a typed price - the form's own POST, and every REST, import and
        // integration create - is priced on the write, so its calculated amount reads the list price.
        JsonPath line = post(LINES, "{\"SalesInvoice\":" + invoice + ",\"Product\":" + widget + ",\"Quantity\":2}");
        assertDecimal("80", line.get("Price"), "the create must copy the list price");
        assertDecimal("160", line.get("Net"), "the line amount must be computed from the copied price");

        // A price the line carries is never replaced - the user's override stands.
        JsonPath typed = post(LINES, "{\"SalesInvoice\":" + invoice + ",\"Product\":" + widget + ",\"Quantity\":1,\"Price\":95}");
        assertDecimal("95", typed.get("Price"), "a typed price must stand");

        // The document total includes the copied price: it moved with the line's own write.
        assertDecimal("255", get(INVOICES + "/" + invoice).get("Net"), "the document total must sum the priced lines");

        // Nothing covers an invoice dated before the list: the form keeps its own default, and a create
        // without a price is refused by name, as before.
        int old = id(post(INVOICES, "{\"Customer\":" + customer + ",\"IssuedOn\":\"2024-06-01\"}"));
        assertNull(post(LINES + "/resolve", "{\"SalesInvoice\":" + old + ",\"Product\":" + widget + ",\"Quantity\":1}").get("Price"),
                "no covering row must answer no price");
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"SalesInvoice\":" + old + ",\"Product\":" + widget + ",\"Quantity\":1}")
                                                 .when()
                                                 .post(LINES)
                                                 .then()
                                                 .statusCode(400));
    }

    private static int id(JsonPath body) {
        return body.getInt("Id");
    }

    private static void assertDecimal(String expected, Object actual, String message) {
        assertEquals(0, new BigDecimal(expected).compareTo(new BigDecimal(String.valueOf(actual))), message + ", got: " + actual);
    }

    private JsonPath post(String url, String body) {
        AtomicReference<JsonPath> answer = new AtomicReference<>();
        restAssuredExecutor.execute(() -> answer.set(given().contentType("application/json")
                                                            .body(body)
                                                            .when()
                                                            .post(url)
                                                            .then()
                                                            .statusCode(200)
                                                            .extract()
                                                            .jsonPath()));
        return answer.get();
    }

    private JsonPath get(String url) {
        AtomicReference<JsonPath> answer = new AtomicReference<>();
        restAssuredExecutor.execute(() -> answer.set(given().when()
                                                            .get(url)
                                                            .then()
                                                            .statusCode(200)
                                                            .extract()
                                                            .jsonPath()));
        return answer.get();
    }

    private void generateProject() {
        String path = PROJECT_PATH + "/app.intent";
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        }
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

    @AfterEach
    void cleanup() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT)
                                                 .then()
                                                 .statusCode(greaterThanOrEqualTo(200)));
        if (repository.hasCollection(PROJECT_PATH)) {
            repository.removeCollection(PROJECT_PATH);
        }
    }
}
