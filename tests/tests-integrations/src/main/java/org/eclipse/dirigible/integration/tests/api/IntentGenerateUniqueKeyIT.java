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
import static org.hamcrest.Matchers.hasSize;
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
 * A {@code generates:} action run a second time against a target's business key is refused with the
 * key's authored message and 409 - the answer an ordinary duplicate create gets - not a 500
 * carrying the driver's text, which the page shows as "Something went wrong" (#8661).
 *
 * <p>
 * The mapping of a unique-key violation to its message used to live only in the generated REST
 * controllers; the create-from saves its target through the target's repository and never passed
 * through one. It now lives in the repository, so this runs the published application: the header's
 * composite {@code unique:} key on the second Depreciate of the same month, and a generated LINE's
 * {@code unique: true} field on the second run of an {@code items:} create-from - each refused, and
 * each leaving nothing of the refused run behind.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentGenerateUniqueKeyIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "genunique";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String EVENTS = "/services/java/" + PROJECT + "/gen/events/" + PROJECT;
    private static final String DEPRECIATE = EVENTS + "/DepreciationFromAssetGenerate/run";
    private static final String LABEL = EVENTS + "/LabelFromAssetGenerate/run";
    private static final String DUPLICATE_PERIOD = "This asset already has a depreciation document for this month";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String INTENT_YAML = """
            name: genunique
            description: create-from whose target carries a business key - a repeated run is refused with the key's message

            entities:
              - name: Asset
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: name,   type: string, length: 100 }
                  - { name: period, type: string, length: 7 }

              - name: AssetPart
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: code, type: string, length: 50 }
                relations:
                  - { name: Asset, kind: manyToOne, to: Asset, composition: true, required: true }

              - name: Depreciation
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: period, type: string, length: 7 }
                relations:
                  - { name: Asset, kind: manyToOne, to: Asset }
                unique:
                  - { fields: [Asset, period], message: "This asset already has a depreciation document for this month" }

              - name: Label
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 100 }
                relations:
                  - { name: Asset, kind: manyToOne, to: Asset }

              - name: LabelLine
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: code, type: string, length: 50, unique: true }
                relations:
                  - { name: Label, kind: manyToOne, to: Label, composition: true, required: true }

            generates:
              - name: depreciation-from-asset
                from: Asset
                to: Depreciation
                forEntity: Asset
                map:
                  Asset: id
                  period: period

              - name: label-from-asset
                from: Asset
                to: Label
                forEntity: Asset
                map:
                  name: name
                  Asset: id
                items:
                  from: AssetPart
                  to: LabelLine
                  map:
                    code: code
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_second_run_against_the_targets_business_key_is_refused_with_its_message() {
        generateAndPublish();
        int asset = create("/asset/AssetController", "{\"Name\":\"Press\",\"Period\":\"2026-11\"}");

        run(DEPRECIATE, asset, 200);
        // The second Depreciate of the month: the key's own sentence, as a conflict - never the driver's
        // "Unique index or primary key violation" as a 500.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + asset + "}")
                                                 .when()
                                                 .post(DEPRECIATE)
                                                 .then()
                                                 .statusCode(409)
                                                 .body("message", equalTo(DUPLICATE_PERIOD)),
                TIMEOUT_SECONDS);
        assertCount("/depreciation/DepreciationController?Asset=" + asset, 1);

        // The control: an ordinary create colliding with the same key is answered the same way.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Asset\":" + asset + ",\"Period\":\"2026-11\"}")
                                                 .when()
                                                 .post(API + "/depreciation/DepreciationController")
                                                 .then()
                                                 .statusCode(409)
                                                 .body("message", equalTo(DUPLICATE_PERIOD)),
                TIMEOUT_SECONDS);
    }

    /**
     * A generated line that collides with its entity's unique field refuses the whole run as a conflict
     * too, naming the source line it was mapped from - and the header the run had already saved goes
     * with it.
     */
    @Test
    void a_generated_line_colliding_with_a_unique_field_refuses_the_run_with_its_message() {
        generateAndPublish();
        int asset = create("/asset/AssetController", "{\"Name\":\"Lathe\",\"Period\":\"2026-11\"}");
        int part = create("/asset/AssetPartController", "{\"Asset\":" + asset + ",\"Code\":\"P-1\"}");

        run(LABEL, asset, 200);
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + asset + "}")
                                                 .when()
                                                 .post(LABEL)
                                                 .then()
                                                 .statusCode(409)
                                                 .body("message", containsString("'Code' already exists"))
                                                 .body("message", containsString("(from AssetPart [" + part + "])"))
                                                 .body("field", equalTo("Code")),
                TIMEOUT_SECONDS);
        assertCount("/label/LabelController?Asset=" + asset, 1);
    }

    private void run(String endpoint, int source, int status) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + source + "}")
                                                 .when()
                                                 .post(endpoint)
                                                 .then()
                                                 .statusCode(status),
                TIMEOUT_SECONDS);
    }

    private void assertCount(String path, int count) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + path)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("", hasSize(count)));
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

    private void generateAndPublish() {
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
