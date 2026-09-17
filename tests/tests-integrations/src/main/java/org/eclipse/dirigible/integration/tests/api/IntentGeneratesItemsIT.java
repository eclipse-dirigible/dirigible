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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * A {@code generates:} create-from with {@code items:} produces a document whose header totals
 * equal the sum of its lines - at runtime, through the published application, not in the generated
 * text.
 *
 * <p>
 * Since #7081 the create-from writes the header, its lines and the source's status flip inside one
 * {@code UnitOfWork}. Every line save re-sums the header synchronously (the generated
 * {@code recalculate}: reload the header, query the lines by foreign key, persist the sums through
 * the targeted {@code updateProperties} mutation), and inside the unit that recompute came out at
 * 0.00 while the very same line POSTed through REST recomputed correctly (issue #7096). The
 * text-level assertions on the create-from live in {@code IntentEngineIT}; this is the runtime half
 * the pipeline's silent degradation demands.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentGeneratesItemsIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "genitems";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String GENERATE_RUN = "/services/java/" + PROJECT + "/gen/events/" + PROJECT + "/InvoiceFromProformaGenerate/run";
    private static final long TIMEOUT_SECONDS = 90;

    /**
     * The document pair of every billing create-from: a header whose {@code aggregate} fields are
     * summed from its {@code *Item} composition child, on both sides of the copy. {@code payable} is
     * the header's own expression over a summed total - the chain #7097 saw read 0 and store 0.
     */
    private static final String INTENT_YAML =
            """
                    name: genitems
                    description: create-from with items fixture - the header sums its lines inside the unit of work

                    entities:
                      - name: ProformaStatus
                        kind: setting
                        fields:
                          - { name: id,   type: integer, primaryKey: true, generated: true }
                          - { name: name, type: string,  required: true, length: 100 }

                      - name: Proforma
                        fields:
                          - { name: id,     type: integer, primaryKey: true, generated: true }
                          - { name: number, type: string, length: 100 }
                          - { name: net,    type: decimal, precision: 18, scale: 2, aggregate: true }
                        relations:
                          - { name: Status, kind: manyToOne, to: ProformaStatus, function: EntityStatus, init: 1 }

                      - name: ProformaItem
                        fields:
                          - { name: id,       type: integer, primaryKey: true, generated: true }
                          - { name: name,     type: string, length: 200 }
                          - { name: quantity, type: decimal, precision: 18, scale: 2, required: true }
                          - { name: price,    type: decimal, precision: 18, scale: 2, required: true }
                          - { name: net,      type: decimal, precision: 18, scale: 2, calculatedOnCreate: "round(Quantity * Price, 2)", calculatedOnUpdate: "round(Quantity * Price, 2)" }
                        relations:
                          - { name: Proforma, kind: manyToOne, to: Proforma, composition: true, required: true }

                      - name: Invoice
                        fields:
                          - { name: id,      type: integer, primaryKey: true, generated: true }
                          - { name: number,  type: string, length: 100 }
                          - { name: net,     type: decimal, precision: 18, scale: 2, aggregate: true }
                          - { name: payable, type: decimal, precision: 18, scale: 2, aggregate: true, calculatedOnCreate: "Net", calculatedOnUpdate: "Net" }

                      - name: InvoiceItem
                        fields:
                          - { name: id,       type: integer, primaryKey: true, generated: true }
                          - { name: name,     type: string, length: 200 }
                          - { name: quantity, type: decimal, precision: 18, scale: 2, required: true }
                          - { name: price,    type: decimal, precision: 18, scale: 2, required: true }
                          - { name: net,      type: decimal, precision: 18, scale: 2, calculatedOnCreate: "round(Quantity * Price, 2)", calculatedOnUpdate: "round(Quantity * Price, 2)" }
                        relations:
                          - { name: Invoice, kind: manyToOne, to: Invoice, composition: true, required: true }

                    seeds:
                      - name: proforma-statuses
                        entity: ProformaStatus
                        rows:
                          - { id: 1, name: Draft }
                          - { id: 2, name: Invoiced }

                    generates:
                      - name: invoice-from-proforma
                        from: Proforma
                        to: Invoice
                        forEntity: Proforma
                        map:
                          number: number
                        items:
                          from: ProformaItem
                          to: InvoiceItem
                          map:
                            name: name
                            quantity: quantity
                            price: price
                        sourceStatus: 2
                    """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_create_from_with_items_produces_a_header_whose_totals_are_the_sum_of_its_lines() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        // The source document: a proforma with two lines, 2 x 10.00 and 3 x 5.00.
        int proforma = create("/proforma/ProformaController", "{\"Number\":\"PF-1\"}");
        create("/proforma/ProformaItemController", "{\"Proforma\":" + proforma + ",\"Name\":\"first\",\"Quantity\":2,\"Price\":10}");
        create("/proforma/ProformaItemController", "{\"Proforma\":" + proforma + ",\"Name\":\"second\",\"Quantity\":3,\"Price\":5}");
        // The control: the same recompute outside any unit of work - a line POSTed through REST - sums
        // the source header, so the recompute machinery itself is fine.
        assertHeader("/proforma/ProformaController/" + proforma, "Net", 35.0f);

        // The create-from: header + two lines + the source's status flip, one unit of work.
        // (The run endpoint answers the created header as a JSON string.)
        AtomicReference<String> generated = new AtomicReference<>();
        restAssuredExecutor.execute(() -> generated.set(given().contentType("application/json")
                                                               .body("{\"id\":" + proforma + "}")
                                                               .when()
                                                               .post(GENERATE_RUN)
                                                               .then()
                                                               .statusCode(200)
                                                               .extract()
                                                               .asString()),
                TIMEOUT_SECONDS);
        Matcher id = Pattern.compile("\"Id\"\\s*:\\s*(\\d+)")
                            .matcher(generated.get());
        assertTrue(id.find(), "the create-from should answer the created invoice, got: " + generated.get());
        int invoice = Integer.parseInt(id.group(1));

        // The lines were copied correctly...
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/invoice/InvoiceItemController?Invoice=" + invoice)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("", hasSize(2))
                                                 .body("Net", equalTo(List.of(20.0f, 15.0f))));
        // ...and the header they belong to carries their sum - not the zero it was inserted with before
        // any line existed (issue #7096), through the summed field and the expression derived from it.
        assertHeader("/invoice/InvoiceController/" + invoice, "Net", 35.0f);
        assertHeader("/invoice/InvoiceController/" + invoice, "Payable", 35.0f);
        // The completion hook committed with the same unit.
        assertHeader("/proforma/ProformaController/" + proforma, "Status", 2);
    }

    private void assertHeader(String path, String property, Object expected) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + path)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(property, equalTo(expected)));
    }

    private int create(String controller, String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(API + controller)
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
