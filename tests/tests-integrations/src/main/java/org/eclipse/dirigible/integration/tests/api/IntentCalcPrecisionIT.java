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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
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

/**
 * A calculated money field keeps every digit its column holds (#7578).
 *
 * <p>
 * {@code calculatedOnCreate} / {@code calculatedOnUpdate} expressions are evaluated by
 * {@code sdk.utils.Calc}, which used to compute in {@code double}: about 15 significant digits
 * against a {@code decimal(18, 2)} column's 18. The catalog's precision-ceiling probe - the largest
 * value the column holds - came back from {@code Amount - Allocated} as {@code 1.0E16}, one digit
 * longer than the column, and the insert failed with a 500 ("Value too long for column"), while the
 * same POST without the expression was accepted. The body is read as text: a JSON number this long
 * parsed into a double would hide exactly the digits under test.
 */
class IntentCalcPrecisionIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "calcprecision";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String PAYMENTS =
            "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api/customerpayment/CustomerPaymentController";

    private static final String INTENT_YAML = """
            name: calcprecision
            description: calculated money at the column's precision ceiling

            entities:
              - name: CustomerPayment
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: amount,    type: decimal, precision: 18, scale: 2 }
                  - { name: allocated, type: decimal, precision: 18, scale: 2, defaultValue: 0 }
                  - { name: unapplied, type: decimal, precision: 18, scale: 2,
                      calculatedOnCreate: "Amount - Allocated", calculatedOnUpdate: "Amount - Allocated" }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_calculated_money_field_at_the_columns_ceiling_is_persisted_exactly() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        // The issue's probe: the largest value a decimal(18, 2) column holds, minus the default 0.
        assertUnapplied(post("{\"Amount\":9999999999999999.99}"), "9999999999999999.99");
        // ...and below the ceiling the low cents are exact, not the double's nearest neighbour.
        assertUnapplied(post("{\"Amount\":1234567890123456.78,\"Allocated\":0.01}"), "1234567890123456.77");
    }

    private String post(String body) {
        AtomicReference<String> created = new AtomicReference<>();
        restAssuredExecutor.execute(() -> created.set(given().contentType("application/json")
                                                             .body(body)
                                                             .when()
                                                             .post(PAYMENTS)
                                                             .then()
                                                             .statusCode(200)
                                                             .extract()
                                                             .asString()),
                90);
        return created.get();
    }

    private static void assertUnapplied(String json, String expected) {
        assertTrue(Pattern.compile("\"Unapplied\"\\s*:\\s*" + Pattern.quote(expected) + "(?![0-9])")
                          .matcher(json)
                          .find(),
                "Unapplied must be exactly " + expected + ", got: " + json);
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
