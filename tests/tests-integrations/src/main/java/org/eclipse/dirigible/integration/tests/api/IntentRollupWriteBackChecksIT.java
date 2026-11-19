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
import org.springframework.test.annotation.DirtiesContext;

/**
 * A roll-up's write-back is not refused by the parent's own {@code checks:} (dirigible #8443).
 *
 * <p>
 * The shape of the BusinessIntents fixed-assets hunt: an asset's depreciated total is summed from
 * its depreciation documents, and the roll-up owns the asset's status while that sum is above zero.
 * A check gates the asset's rate against its cap at status Active. The rate is raised while the
 * asset is Depreciating (the gate does not apply there), then the only depreciation document is
 * deleted: the recomputed row goes back to Active with Depreciated 0, and that row fails the gate.
 * Before the fix the roll-up handler's write was refused and only logged - the delete had already
 * committed, so the asset kept Depreciated 100 against documents that total 0. The refusal is
 * permanent, so the stale total is not a timing artefact: the await below never succeeds on a build
 * without the fix.
 *
 * <p>
 * The gate still applies to what a person writes: the same row edited at Active is refused.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentRollupWriteBackChecksIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "rollupchecks";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String ASSETS = API + "/asset/AssetController";
    private static final String DEPRECIATIONS = API + "/depreciation/DepreciationController";
    private static final String REFUSAL = "The rate of an active asset cannot exceed its cap";
    private static final long TIMEOUT_SECONDS = 90;
    /** The roll-up runs off the child's topics, after the commit. */
    private static final long ROLLUP_TIMEOUT_SECONDS = 30;

    private static final String INTENT_YAML = """
            name: rollupchecks
            description: a roll-up write-back is not refused by the parent's checks

            entities:
              - name: AssetStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: Asset
                checks:
                  - { kind: compare, field: rate, op: le, than: rateCap, status: 1,
                      message: "The rate of an active asset cannot exceed its cap" }
                fields:
                  - { name: id,          type: integer, primaryKey: true, generated: true }
                  - { name: name,        type: string, length: 40 }
                  - { name: cost,        type: decimal, precision: 18, scale: 2 }
                  - { name: rate,        type: decimal, precision: 18, scale: 2 }
                  - { name: rateCap,     type: decimal, precision: 18, scale: 2 }
                  - { name: depreciated, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Status, kind: manyToOne, to: AssetStatus, function: EntityStatus, init: 1 }

              - name: Depreciation
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Asset, kind: manyToOne, to: Asset, required: true }

            rollups:
              - { name: assetDepreciated, entity: Depreciation, via: Asset, field: depreciated, op: sum, of: amount,
                  capacity: cost, status: Status, statusWhenFull: 3, statusWhenPartial: 2 }

            seeds:
              - name: asset-statuses
                entity: AssetStatus
                rows:
                  - { id: 1, name: Active }
                  - { id: 2, name: Depreciating }
                  - { id: 3, name: Depreciated }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_roll_up_write_back_the_parent_checks_would_refuse_still_lands() {
        writeIntent();
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int asset = create(ASSETS, "{\"Name\":\"Press\",\"Cost\":1000,\"Rate\":5,\"RateCap\":10}");
        int depreciation = create(DEPRECIATIONS, "{\"Amount\":100,\"Asset\":" + asset + "}");
        awaitAsset(asset, 100.0f, 2);

        // Depreciating: the gate is not at its status, so the rate may pass the cap.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Name\":\"Press\",\"Cost\":1000,\"Rate\":20,\"RateCap\":10,\"Status\":2}")
                                                 .when()
                                                 .put(ASSETS + "/" + asset)
                                                 .then()
                                                 .statusCode(200),
                TIMEOUT_SECONDS);

        // The only document goes: the recomputed row is Active with Depreciated 0 - a row the gate
        // refuses. The delete has committed, so the total must follow it.
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete(DEPRECIATIONS + "/" + depreciation)
                                                 .then()
                                                 .statusCode(greaterThanOrEqualTo(200)),
                TIMEOUT_SECONDS);
        awaitAsset(asset, 0.0f, 1);

        // The gate still applies to what a person writes at Active.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Name\":\"Lathe\",\"Cost\":1000,\"Rate\":20,\"RateCap\":10,\"Status\":1}")
                                                 .when()
                                                 .put(ASSETS + "/" + asset)
                                                 .then()
                                                 .statusCode(400)
                                                 .body(containsString(REFUSAL)),
                TIMEOUT_SECONDS);
    }

    private void awaitAsset(int asset, float depreciated, int status) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ASSETS + "/" + asset)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Depreciated", equalTo(depreciated))
                                                 .body("Status", equalTo(status)),
                ROLLUP_TIMEOUT_SECONDS);
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
