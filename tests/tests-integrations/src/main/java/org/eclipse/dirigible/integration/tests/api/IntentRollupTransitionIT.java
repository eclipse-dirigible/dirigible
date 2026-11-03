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
 * A roll-up filtered by status follows a status a {@code transitions:} button moves (dirigible
 * #7942).
 *
 * <p>
 * The button writes the status with a targeted write that publishes {@code -transitioned} and
 * nothing else, while the roll-up recomputed on the child's create, {@code -updated},
 * {@code -deleted} and {@code -rekeyed} only - so a cancelled call-off went on counting against its
 * contract until some unrelated write of a sibling fired. Both directions are walked: the cancelled
 * row leaves the sum, the re-issued one comes back.
 *
 * <p>
 * A second roll-up over the same child and relation counts the CANCELLED call-offs under a filter
 * of its own (dirigible #8330). Both share one generated handler per event, and that handler used
 * to read the rows once with the first roll-up's filter - so the count counted the issued rows
 * instead. The two values moving in opposite directions is what proves each reads its own rows.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentRollupTransitionIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "rolluptransition";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String CONTRACTS = API + "/contract/ContractController";
    private static final String CALL_OFFS = API + "/calloff/CallOffController";
    private static final String TRANSITIONS = "/services/java/" + PROJECT + "/gen/events/" + PROJECT + "/";
    private static final long TIMEOUT_SECONDS = 90;
    /** The roll-up runs off the child's topics, after the commit. */
    private static final long ROLLUP_TIMEOUT_SECONDS = 30;

    private static final String INTENT_YAML = """
            name: rolluptransition
            description: a status-filtered roll-up recomputed when a transitions button moves the status

            entities:
              - name: CallOffStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 50 }
              - name: Contract
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: committed, type: decimal, precision: 18, scale: 2 }
                  - { name: cancelledCallOffs, type: integer }
              - name: CallOff
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: total, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Contract, kind: manyToOne, to: Contract }
                  - { name: Status, kind: manyToOne, to: CallOffStatus, function: EntityStatus, init: ISSUED }

            transitions:
              - { name: CancelCallOff,  forEntity: CallOff, from: [ISSUED],    setStatus: CANCELLED, label: Cancel }
              - { name: ReissueCallOff, forEntity: CallOff, from: [CANCELLED], setStatus: ISSUED,    label: Reissue }

            rollups:
              - name: contractCommitted
                entity: CallOff
                via: Contract
                field: committed
                op: sum
                of: total
                where:
                  - { field: Status, op: ne, value: CANCELLED }
              - name: contractCancelledCallOffs
                entity: CallOff
                via: Contract
                field: cancelledCallOffs
                where:
                  - { field: Status, op: ne, value: ISSUED }

            seeds:
              - name: call-off-statuses
                entity: CallOffStatus
                rows:
                  - { id: 1, name: ISSUED }
                  - { id: 2, name: CANCELLED, stage: cancelled }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_transition_moves_a_row_out_of_and_back_into_a_status_filtered_sum() {
        writeIntent();
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int contract = create(CONTRACTS, "{}");
        int cancelled = create(CALL_OFFS, "{\"Total\":100,\"Contract\":" + contract + "}");
        create(CALL_OFFS, "{\"Total\":50,\"Contract\":" + contract + "}");
        awaitRollups(contract, 150.0f, 0);

        // The button publishes -transitioned only: the cancelled row leaves the filtered sum and
        // enters the count of cancelled rows, whose filter is its own...
        transition("CancelCallOff", cancelled);
        awaitRollups(contract, 50.0f, 1);

        // ...and both move back once it is issued again.
        transition("ReissueCallOff", cancelled);
        awaitRollups(contract, 150.0f, 0);
    }

    private void awaitRollups(int contract, float committed, int cancelledCallOffs) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(CONTRACTS + "/" + contract)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Committed", equalTo(committed))
                                                 .body("CancelledCallOffs", equalTo(cancelledCallOffs)),
                ROLLUP_TIMEOUT_SECONDS);
    }

    private void transition(String name, int id) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + id + "}")
                                                 .when()
                                                 .post(TRANSITIONS + name + "Transition/run")
                                                 .then()
                                                 .statusCode(200),
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
