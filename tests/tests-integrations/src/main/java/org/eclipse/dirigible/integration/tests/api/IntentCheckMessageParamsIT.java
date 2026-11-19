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

import io.restassured.response.ValidatableResponse;

/**
 * A refusal names the record's values (intent {@code {field}} / {@code {Relation.field}}
 * placeholders in a {@code checks:} message, dirigible #8665).
 *
 * <p>
 * The case: an asset's tax depreciation rate is capped by its category, and the refusal has to say
 * which limit applies. Asserted on the running, generated code: the 400 names the record's own cap
 * and the code of the category one hop away, in the default text and - filled in after translation
 * - in a translator's Bulgarian, and the body carries both in {@code messageParams}.
 */
class IntentCheckMessageParamsIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "chkparams";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String ASSETS = API + "/asset/AssetController";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String INTENT_YAML = """
            name: chkparams
            description: an asset's tax rate is capped by its category, and the refusal says which cap
            languages: [en, bg]

            entities:
              - name: Category
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: code, type: string, length: 20 }

              - name: Asset
                fields:
                  - { name: id,         type: integer, primaryKey: true, generated: true }
                  - { name: name,       type: string, length: 100 }
                  - { name: taxRate,    type: decimal }
                  - { name: taxRateCap, type: decimal }
                  - { name: units,      type: integer }
                relations:
                  - { name: Category, kind: manyToOne, to: Category }
                checks:
                  - { id: taxRateCap, kind: compare, field: taxRate, op: le, than: taxRateCap,
                      message: "The tax depreciation rate exceeds the statutory maximum of {taxRateCap}% for category {Category.code}" }
                  - { id: units, kind: compare, field: units, op: le, value: 10,
                      message: "{name}: at most 10 units, not {units}" }

            seeds:
              - name: categories
                entity: Category
                rows:
                  - { id: 1, code: "2.1" }
            """;

    private static final String BG_OVERLAY = """
            {
              "chkparams-model": {
                "checks": {
                  "Asset_taxRateCap": "Данъчната норма надвишава максимума от {taxRateCap}% за категория {Category.code}"
                }
              }
            }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_refusal_names_the_records_own_value_and_one_a_hop_away_in_the_readers_language() {
        generateProject();
        writeResource(PROJECT_PATH + "/i18n/bg-BG/" + PROJECT + ".model.json", BG_OVERLAY);
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        String over = "{\"Name\":\"Truck\",\"TaxRate\":30,\"TaxRateCap\":20,\"Category\":1}";
        post(over, null).statusCode(400)
                        .body("message", equalTo("The tax depreciation rate exceeds the statutory maximum of 20% for category 2.1"))
                        .body("messageParams.'Category.code'", equalTo("2.1"));
        post(over, "bg").statusCode(400)
                        .body("message", equalTo("Данъчната норма надвишава максимума от 20% за категория 2.1"));
        // The record's own fields, beside a literal comparison.
        post("{\"Name\":\"Pallets\",\"Units\":12}", null).statusCode(400)
                                                         .body("message", equalTo("Pallets: at most 10 units, not 12"));
        // Within the cap it saves.
        post("{\"Name\":\"Truck\",\"TaxRate\":15,\"TaxRateCap\":20,\"Category\":1}", null).statusCode(200);
    }

    private ValidatableResponse post(String body, String language) {
        AtomicReference<ValidatableResponse> response = new AtomicReference<>();
        restAssuredExecutor.execute(() -> {
            var request = given().contentType("application/json");
            if (language != null) {
                request = request.header("Accept-Language", language);
            }
            response.set(request.body(body)
                                .when()
                                .post(ASSETS)
                                .then());
        }, TIMEOUT_SECONDS);
        return response.get();
    }

    private void generateProject() {
        writeResource(PROJECT_PATH + "/app.intent", INTENT_YAML);
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

    private void writeResource(String path, String content) {
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(content.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, content.getBytes(StandardCharsets.UTF_8));
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
