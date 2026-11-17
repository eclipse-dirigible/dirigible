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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
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
 * each leaving nothing of the refused run behind. A case-insensitive key, which the repository
 * checks before the write, refuses a generated line and a scheduled child the same way (#8669).
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
    private static final String BADGE = EVENTS + "/BadgeFromAssetGenerate/run";
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

              - name: Badge
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 100 }
                relations:
                  - { name: Asset, kind: manyToOne, to: Asset }

              - name: BadgeLine
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: code, type: string, length: 50, unique: { ignoreCase: true } }
                relations:
                  - { name: Badge, kind: manyToOne, to: Badge, composition: true, required: true }

              - name: Batch
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: Asset, kind: manyToOne, to: Asset }

              - name: Tag
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: code, type: string, length: 50, unique: { ignoreCase: true } }
                relations:
                  - { name: Batch, kind: manyToOne, to: Batch }

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

              - name: badge-from-asset
                from: Asset
                to: Badge
                forEntity: Asset
                map:
                  name: name
                  Asset: id
                items:
                  from: AssetPart
                  to: BadgeLine
                  map:
                    code: code

            schedules:
              - name: monthly-kiln
                cron: "0 0 3 1 1 ?"
                entity: Asset
                where: [ { field: name, op: eq, value: Kiln } ]
                generate:
                  to: Batch
                  unique: [Asset]
                  map: { Asset: id }
                  children:
                    - to: Tag
                      parent: Batch
                      forEach: { entity: AssetPart, match: { Asset: id } }
                      map: { code: code }
                      onRefused: skip
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
                                                 .body("message", containsString("(from Asset Part [" + part + "])"))
                                                 .body("field", equalTo("Code")),
                TIMEOUT_SECONDS);
        assertCount("/label/LabelController?Asset=" + asset, 1);
    }

    /**
     * A case-insensitive key ({@code unique: { ignoreCase: true }}) is checked by the repository before
     * the write, as no database constraint can enforce it. Its refusal is the one the column's own
     * UNIQUE gets (#8669): a generated line that differs from a stored one only in letter case refuses
     * the run naming its source line, as an exact repeat does - not with the bare HTTP answer only a
     * controller understands.
     */
    @Test
    void a_generated_line_colliding_with_a_case_insensitive_key_refuses_the_run_with_its_message() {
        generateAndPublish();
        int seed = create("/badge/BadgeController", "{\"Name\":\"seed\"}");
        create("/badge/BadgeLineController", "{\"Badge\":" + seed + ",\"Code\":\"b-1\"}");
        int asset = create("/asset/AssetController", "{\"Name\":\"Mill\",\"Period\":\"2026-11\"}");
        int part = create("/asset/AssetPartController", "{\"Asset\":" + asset + ",\"Code\":\"B-1\"}");

        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + asset + "}")
                                                 .when()
                                                 .post(BADGE)
                                                 .then()
                                                 .statusCode(409)
                                                 .body("message", containsString("'Code' already exists"))
                                                 .body("message", containsString("(from Asset Part [" + part + "])"))
                                                 .body("field", equalTo("Code")),
                TIMEOUT_SECONDS);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/badge/BadgeController")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("findAll { it.Asset == " + asset + " }", hasSize(0)),
                TIMEOUT_SECONDS);
    }

    /**
     * A scheduled generate's child declared {@code onRefused: skip} skips a child that collides with a
     * case-insensitive key, as it skips any other refusal (#8669), and keeps the parent and its other
     * children - the refusal used to be an HTTP exception the job did not recognise, which rolled the
     * whole source row back.
     */
    @Test
    void a_scheduled_child_colliding_with_a_case_insensitive_key_is_skipped() {
        generateAndPublish();
        create("/tag/TagController", "{\"Code\":\"k-1\"}");
        int asset = create("/asset/AssetController", "{\"Name\":\"Kiln\",\"Period\":\"2026-11\"}");
        create("/asset/AssetPartController", "{\"Asset\":" + asset + ",\"Code\":\"K-1\"}");
        create("/asset/AssetPartController", "{\"Asset\":" + asset + ",\"Code\":\"K-2\"}");

        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("[]")
                                                 .when()
                                                 .post("/services/jobs/trigger/gen.events." + PROJECT + ".MonthlyKilnJob")
                                                 .then()
                                                 .statusCode(200),
                TIMEOUT_SECONDS);

        AtomicInteger batch = new AtomicInteger();
        restAssuredExecutor.execute(() -> batch.set(given().when()
                                                           .get(API + "/batch/BatchController")
                                                           .then()
                                                           .statusCode(200)
                                                           .body("findAll { it.Asset == " + asset + " }", hasSize(1))
                                                           .extract()
                                                           .<Integer>path("find { it.Asset == " + asset + " }.Id")),
                TIMEOUT_SECONDS);
        // The refused child is left out; its sibling and the seed row stand.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/tag/TagController")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("findAll { it.Batch == " + batch.get() + " }.Code", contains("K-2"))
                                                 .body("Code", containsInAnyOrder("k-1", "K-2")),
                TIMEOUT_SECONDS);
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
