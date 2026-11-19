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
 * A record locked by the period register of ANOTHER model (intent {@code immutableInPeriod: {
 * period, model, date }}, dirigible #8687).
 *
 * <p>
 * The ledger keeps the one period register, and the source documents posting into it live in models
 * the ledger's model uses - so they cannot use it back, and before the fix could not name its
 * register at all: a cash order dated in a CLOSED month was still created, edited and deleted. Here
 * the cash model names the journal's register by model and name only, with no {@code uses:}. It is
 * generated and published FIRST, alone, and runs with no lock; once the journal is deployed,
 * closing its period refuses a create, an edit and a delete dated inside it with the period's 409,
 * an order dated outside it stays writable, and reopening the period lifts the lock.
 */
class IntentCrossModelPeriodLockIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String JOURNAL = "cpljournal";
    private static final String CASH = "cplcash";
    private static final String PERIODS = api(JOURNAL) + "/fiscalperiod/FiscalPeriodController";
    private static final String TRANSITIONS = "/services/java/" + JOURNAL + "/gen/events/" + JOURNAL + "/";
    private static final String ORDERS = api(CASH) + "/cashorder/CashOrderController";
    private static final String CLOSED = "The FiscalPeriod covering this date is closed";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String JOURNAL_YAML = """
            name: cpljournal
            description: the ledger's period register

            entities:
              - name: PeriodStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: FiscalPeriod
                period: { start: startDate, end: endDate, closedWhen: "Status == CLOSED" }
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: startDate, type: date, required: true }
                  - { name: endDate,   type: date, required: true }
                relations:
                  - { name: Status, kind: manyToOne, to: PeriodStatus, function: EntityStatus, init: 1 }

            transitions:
              - { name: ClosePeriod,  forEntity: FiscalPeriod, from: [1], setStatus: 2, label: Close }
              - { name: ReopenPeriod, forEntity: FiscalPeriod, from: [2], setStatus: 1, label: Reopen }

            seeds:
              - name: period-statuses
                entity: PeriodStatus
                rows:
                  - { id: 1, name: OPEN }
                  - { id: 2, name: CLOSED }
            """;

    private static final String CASH_YAML = """
            name: cplcash
            description: a source document locked by the ledger's period, without using the ledger

            entities:
              - name: CashOrder
                immutableInPeriod: { period: FiscalPeriod, model: cpljournal, date: date }
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: date,   type: date }
                  - { name: amount, type: decimal, precision: 18, scale: 2 }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void another_models_closed_period_locks_the_record_and_its_absence_locks_nothing() {
        // The cash model alone: it compiles and runs, and with no register deployed nothing is locked.
        deploy(CASH, CASH_YAML);
        int march = create(ORDERS, "{\"Date\":\"2026-03-10\",\"Amount\":10}");

        deploy(JOURNAL, JOURNAL_YAML);
        int period = create(PERIODS, "{\"StartDate\":\"2026-03-01\",\"EndDate\":\"2026-03-31\",\"Status\":1}");
        // Open: the March order is still writable.
        update(march, "2026-03-10", 11, 200);

        transition("ClosePeriod", period);
        refused(() -> given().contentType("application/json")
                             .body("{\"Date\":\"2026-03-15\",\"Amount\":5}")
                             .when()
                             .post(ORDERS));
        update(march, "2026-03-10", 12, 409);
        refused(() -> given().when()
                             .delete(ORDERS + "/" + march));
        // A date the closed period does not cover stays writable.
        create(ORDERS, "{\"Date\":\"2026-04-02\",\"Amount\":7}");

        transition("ReopenPeriod", period);
        update(march, "2026-03-10", 13, 200);
    }

    private void refused(java.util.function.Supplier<io.restassured.response.Response> call) {
        restAssuredExecutor.execute(() -> call.get()
                                              .then()
                                              .statusCode(409)
                                              .body(containsString(CLOSED)));
    }

    private void update(int id, String date, int amount, int status) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + id + ",\"Date\":\"" + date + "\",\"Amount\":" + amount + "}")
                                                 .when()
                                                 .put(ORDERS + "/" + id)
                                                 .then()
                                                 .statusCode(status));
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

    private void deploy(String project, String yaml) {
        String path = projectPath(project) + "/app.intent";
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(yaml.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, yaml.getBytes(StandardCharsets.UTF_8));
        }
        AtomicReference<List<Map<String, Object>>> plan = new AtomicReference<>();
        restAssuredExecutor.execute(() -> plan.set(given().when()
                                                          .post("/services/ide/intent/generate?workspace=" + WORKSPACE + "&project="
                                                                  + project + "&path=app.intent")
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
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + project + "/")
                                                 .then()
                                                 .statusCode(200));
        synchronizationProcessor.forceProcessSynchronizers();
    }

    private static String api(String project) {
        return "/services/java/" + project + "/gen/" + project + "/api";
    }

    private static String projectPath(String project) {
        return IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + project;
    }

    @AfterEach
    void cleanup() {
        for (String project : List.of(CASH, JOURNAL)) {
            restAssuredExecutor.execute(() -> given().when()
                                                     .delete("/services/ide/publisher/" + WORKSPACE + "/" + project)
                                                     .then()
                                                     .statusCode(greaterThanOrEqualTo(200)));
            if (repository.hasCollection(projectPath(project))) {
                repository.removeCollection(projectPath(project));
            }
        }
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
