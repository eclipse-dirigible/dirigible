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
 * A decimal is checked and calculated as the value its column stores - dirigible #8107.
 *
 * <p>
 * The checks and the calculated expressions read the payload's value, and the column then stored it
 * rounded to its scale: 0.0049 hours passed {@code hours > 0}, was stored as 0.00 and still billed
 * 0.49. The generated repository now rounds every decimal to its scale, half up, before anything
 * reads it - on the controllers' pre-write reads and on every write path - so a value that rounds
 * to zero is refused by the check, and the amount is priced on the stored hours.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentDecimalScaleIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "decimalscale";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String ALLOCATIONS = API + "/allocation/AllocationController";
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
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_decimal_is_checked_and_calculated_as_the_value_its_column_stores() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

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
