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
 * A roll-up {@code where:} tests a column for null (dirigible #7984): {@code value: null} with
 * {@code eq} keeps the rows whose to-one is empty, with {@code ne} the rows where it is set.
 *
 * <p>
 * The case is a posted-entry count onto a source document that must not count a reversal or a
 * reversed entry. Both sums also render the child-side fit guard, which tests the row being written
 * in Java - so the generated repository compiling, and a write going through it, is part of what is
 * proved.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentRollupNullFilterIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "rollupnullfilter";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String PAYMENTS = API + "/payment/PaymentController";
    private static final String ENTRIES = API + "/entry/EntryController";
    private static final long TIMEOUT_SECONDS = 90;
    /** The roll-up runs off the child's topics, after the commit. */
    private static final long ROLLUP_TIMEOUT_SECONDS = 30;

    private static final String INTENT_YAML = """
            name: rollupnullfilter
            description: roll-ups that count only the child rows whose to-one is empty, or set

            entities:
              - name: Payment
                fields:
                  - { name: id,            type: integer, primaryKey: true, generated: true }
                  - { name: postedEntries, type: integer }
                  - { name: postedTotal,   type: decimal, precision: 18, scale: 2 }
                  - { name: stornoTotal,   type: decimal, precision: 18, scale: 2 }
              - name: Entry
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Payment,    kind: manyToOne, to: Payment }
                  - { name: Storno,     kind: manyToOne, to: Entry }
                  - { name: ReversedBy, kind: manyToOne, to: Entry }

            rollups:
              - name: postedEntries
                entity: Entry
                via: Payment
                field: postedEntries
                where:
                  - { field: Storno,     op: eq, value: null }
                  - { field: ReversedBy, op: eq, value: null }
              - name: postedTotal
                entity: Entry
                via: Payment
                field: postedTotal
                op: sum
                of: amount
                where:
                  - { field: Storno,     op: eq, value: null }
                  - { field: ReversedBy, op: eq, value: null }
              - name: stornoTotal
                entity: Entry
                via: Payment
                field: stornoTotal
                op: sum
                of: amount
                where:
                  - { field: Storno, op: ne, value: null }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_reversed_entry_and_its_reversal_leave_the_count_of_live_entries() {
        writeIntent();
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int payment = create(PAYMENTS, "{}");
        int original = create(ENTRIES, "{\"Amount\":100,\"Payment\":" + payment + "}");
        awaitRollups(payment, 1, 100.0f, 0.0f);

        // The reversal names the original: it is not a live entry, and it is the only storno...
        int reversal = create(ENTRIES, "{\"Amount\":-100,\"Payment\":" + payment + ",\"Storno\":" + original + "}");
        awaitRollups(payment, 1, 100.0f, -100.0f);

        // ...and once the original names its reversal, it is not a live entry either.
        update(ENTRIES, original, "{\"Id\":" + original + ",\"Amount\":100,\"Payment\":" + payment + ",\"ReversedBy\":" + reversal + "}");
        awaitRollups(payment, 0, 0.0f, -100.0f);
    }

    private void awaitRollups(int payment, int postedEntries, float postedTotal, float stornoTotal) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(PAYMENTS + "/" + payment)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("PostedEntries", equalTo(postedEntries))
                                                 .body("PostedTotal", equalTo(postedTotal))
                                                 .body("StornoTotal", equalTo(stornoTotal)),
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

    private void update(String controller, int id, String body) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(body)
                                                 .when()
                                                 .put(controller + "/" + id)
                                                 .then()
                                                 .statusCode(200),
                TIMEOUT_SECONDS);
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
