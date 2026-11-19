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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
 * A schedule selects by a period start (intent {@code schedules[].where} {@code START_OF_MONTH},
 * dirigible #8664).
 *
 * <p>
 * The case: a month-end depreciation run must skip the assets commissioned in the current month -
 * the depreciation starts the month after. No offset from today says "before this month" on every
 * month length, so the run used to select them and a module guard refused each one as a failure.
 * Asserted on the running, generated job: triggered, it depreciates the asset commissioned last
 * month and leaves the one commissioned this month alone.
 */
class IntentSchedulePeriodStartIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "periodstart";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String INTENT_YAML = """
            name: periodstart
            description: a month-end run depreciates the assets commissioned before this month

            entities:
              - name: Asset
                fields:
                  - { name: id,                type: integer, primaryKey: true, generated: true }
                  - { name: name,              type: string, length: 100 }
                  - { name: commissioningDate, type: date }

              - name: Depreciation
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 100 }
                relations:
                  - { name: Asset, kind: manyToOne, to: Asset }

            schedules:
              - name: monthly-depreciation
                cron: "0 0 22 L * ?"
                entity: Asset
                where:
                  - { field: commissioningDate, op: lt, value: START_OF_MONTH }
                generate:
                  to: Depreciation
                  map: { Asset: id, note: name }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_month_end_run_skips_the_rows_dated_in_the_current_month() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        LocalDate firstOfMonth = LocalDate.now()
                                          .withDayOfMonth(1);
        int lastMonth = create("{\"Name\":\"Truck\",\"CommissioningDate\":\"" + firstOfMonth.minusDays(1) + "\"}");
        create("{\"Name\":\"Press\",\"CommissioningDate\":\"" + firstOfMonth + "\"}");

        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("[]")
                                                 .when()
                                                 .post("/services/jobs/trigger/gen.events." + PROJECT + ".MonthlyDepreciationJob")
                                                 .then()
                                                 .statusCode(200),
                TIMEOUT_SECONDS);

        // Only the asset commissioned before this month is depreciated - the first of the month is not
        // "before" it.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/depreciation/DepreciationController")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Asset", contains(lastMonth)),
                TIMEOUT_SECONDS);
    }

    private int create(String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(API + "/asset/AssetController")
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                TIMEOUT_SECONDS);
        return id.get();
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
