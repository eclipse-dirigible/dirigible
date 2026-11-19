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

/**
 * The aggregate guard ({@code checks: kind: guard}) over a CALCULATED summed field (dirigible
 * #8743).
 *
 * <p>
 * Stock must never go negative: receipts and issues go into one signed calculated column
 * ({@code signed = Quantity * (3 - 2 * Kind)}, a receipt positive, an issue negative), summed per
 * item by an aggregate whose guard keeps the sum at or above 0. Before the fix the generated
 * repository ran the guard before the calculated fields were assigned, so it read {@code signed} as
 * null, counted 0 and accepted every issue - on create and on update alike.
 */
class IntentAggregateGuardCalculatedIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "aggguardcalc";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String ITEMS = API + "/stockitem/StockItemController";
    private static final String MOVES = API + "/stockmove/StockMoveController";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String INTENT_YAML = """
            name: aggguardcalc
            description: an aggregate guard over a calculated summed field

            entities:
              - name: StockItem
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 40 }

              - name: StockMove
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: quantity, type: decimal, precision: 18, scale: 2 }
                  # 1 = receipt, 2 = issue
                  - { name: kind,     type: integer }
                  - { name: signed,   type: decimal, precision: 18, scale: 2,
                      calculatedOnCreate: "Quantity * (3 - 2 * Kind)", calculatedOnUpdate: "Quantity * (3 - 2 * Kind)" }
                relations:
                  - { name: StockItem, kind: manyToOne, to: StockItem, required: true }
                checks:
                  - kind: guard
                    aggregate: onHand
                    minimum: 0
                    message: Not enough stock

              - name: StockLevel
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: quantity, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: StockItem, kind: manyToOne, to: StockItem, required: true }

            aggregates:
              - { name: onHand, of: StockMove, op: sum, sum: signed, by: [StockItem], into: StockLevel, field: quantity }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_calculated_summed_field_is_guarded_on_create_and_on_update() {
        writeIntent();
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int item = create(ITEMS, "{\"Name\":\"Bolt\"}");
        // A receipt of 10, then an issue of 4: 6 on hand.
        create(MOVES, "{\"Quantity\":10,\"Kind\":1,\"StockItem\":" + item + "}");
        int issue = createOnce(MOVES, "{\"Quantity\":4,\"Kind\":2,\"StockItem\":" + item + "}");

        // A further issue of 8 would leave -2: refused, its signed quantity is -8, not null. The writes
        // from here on are sent once - retried, an accepted write would hide a refusal.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Quantity\":8,\"Kind\":2,\"StockItem\":" + item + "}")
                                                 .when()
                                                 .post(MOVES)
                                                 .then()
                                                 .statusCode(400)
                                                 .body(containsString("Not enough stock")));

        // Raising the stored issue to 20 would leave -10: refused on the update path too.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + issue + ",\"Quantity\":20,\"Kind\":2,\"StockItem\":" + item + "}")
                                                 .when()
                                                 .put(MOVES + "/" + issue)
                                                 .then()
                                                 .statusCode(400)
                                                 .body(containsString("Not enough stock")));

        // An issue that fits is still accepted, and the refused writes changed nothing.
        createOnce(MOVES, "{\"Quantity\":5,\"Kind\":2,\"StockItem\":" + item + "}");
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(MOVES + "/" + issue)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Quantity", equalTo(4.0f))
                                                 .body("Signed", equalTo(-4.0f)),
                TIMEOUT_SECONDS);
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

    private int createOnce(String controller, String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(controller)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")));
        return id.get();
    }

    private void generateProject() {
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
