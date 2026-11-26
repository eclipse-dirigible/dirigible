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
 * Roll-ups coalesced into one handler per event each aggregate the rows their OWN {@code where:}
 * selects (dirigible #7905, #8490).
 *
 * <p>
 * Roll-ups sharing a child and a relation share a generated {@code <Child><Fk>RollupOn*} handler
 * per event. That handler used to read the child rows once, with the criteria of the group's first
 * member, so an unfiltered total declared first turned every filtered sibling into the same total -
 * and a filtered member declared first would have narrowed the unfiltered one. Here the unfiltered
 * sum comes first, followed by a sum and a count under two different filters, and every handler the
 * group generates is walked: create, the {@code -transitioned} channel, a full-row update, the
 * re-parenting {@code -rekeyed} event (both parents) and delete. The three values moving
 * independently is what proves each member reads its own rows.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentRollupMemberFilterIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "rollupmemberfilter";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String PROJECTS = API + "/project/ProjectController";
    private static final String TIMESHEETS = API + "/timesheet/TimesheetController";
    private static final String TRANSITIONS = "/services/java/" + PROJECT + "/gen/events/" + PROJECT + "/";
    private static final long TIMEOUT_SECONDS = 90;
    /** The roll-ups run off the child's topics, after the commit. */
    private static final long ROLLUP_TIMEOUT_SECONDS = 30;
    private static final int APPROVED = 2;

    private static final String INTENT_YAML = """
            name: rollupmemberfilter
            description: coalesced roll-ups on one child and relation, each under its own where

            entities:
              - name: TimesheetStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 50 }
              - name: Project
                fields:
                  - { name: id,            type: integer, primaryKey: true, generated: true }
                  - { name: totalHours,    type: decimal, precision: 18, scale: 2 }
                  - { name: approvedHours, type: decimal, precision: 18, scale: 2 }
                  - { name: draftSheets,   type: integer }
              - name: Timesheet
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: hours, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Project, kind: manyToOne, to: Project }
                  - { name: Status, kind: manyToOne, to: TimesheetStatus, function: EntityStatus, init: DRAFT }

            transitions:
              - { name: ApproveTimesheet, forEntity: Timesheet, from: [DRAFT], setStatus: APPROVED, label: Approve }

            rollups:
              - { name: projectHours, entity: Timesheet, via: Project, field: totalHours, op: sum, of: hours }
              - name: projectApprovedHours
                entity: Timesheet
                via: Project
                field: approvedHours
                op: sum
                of: hours
                where:
                  - { field: Status, op: eq, value: APPROVED }
              - name: projectDraftSheets
                entity: Timesheet
                via: Project
                field: draftSheets
                where:
                  - { field: Status, op: eq, value: DRAFT }

            seeds:
              - name: timesheet-statuses
                entity: TimesheetStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: APPROVED }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void every_member_of_a_coalesced_rollup_group_aggregates_the_rows_its_own_filter_selects() {
        writeIntent();
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int first = create(PROJECTS, "{}");
        int second = create(PROJECTS, "{}");

        // create: both rows are drafts - in the total and the draft count, not in the approved sum.
        int draft = create(TIMESHEETS, "{\"Hours\":8,\"Project\":" + first + "}");
        int approved = create(TIMESHEETS, "{\"Hours\":5,\"Project\":" + first + "}");
        awaitProject(first, 13.0f, 0.0f, 2);

        // -transitioned: the approved row moves from the draft count into the approved sum.
        transition("ApproveTimesheet", approved);
        awaitProject(first, 13.0f, 5.0f, 1);

        // -updated: an edit of the draft moves the total and nothing the approved sum selects.
        put(TIMESHEETS, draft, "{\"Id\":" + draft + ",\"Hours\":10,\"Project\":" + first + ",\"Status\":1}");
        awaitProject(first, 15.0f, 5.0f, 1);

        // -rekeyed and -updated: the approved row leaves one parent's filtered sum for the other's.
        put(TIMESHEETS, approved, "{\"Id\":" + approved + ",\"Hours\":5,\"Project\":" + second + ",\"Status\":" + APPROVED + "}");
        awaitProject(first, 10.0f, 0.0f, 1);
        awaitProject(second, 5.0f, 5.0f, 0);

        // -deleted: the last row of the second parent takes every one of its values back to zero.
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete(TIMESHEETS + "/" + approved)
                                                 .then()
                                                 .statusCode(greaterThanOrEqualTo(200)),
                TIMEOUT_SECONDS);
        awaitProject(second, 0.0f, 0.0f, 0);
        awaitProject(first, 10.0f, 0.0f, 1);
    }

    private void awaitProject(int project, float totalHours, float approvedHours, int draftSheets) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(PROJECTS + "/" + project)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("TotalHours", equalTo(totalHours))
                                                 .body("ApprovedHours", equalTo(approvedHours))
                                                 .body("DraftSheets", equalTo(draftSheets)),
                ROLLUP_TIMEOUT_SECONDS);
    }

    private void put(String controller, int id, String body) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(body)
                                                 .when()
                                                 .put(controller + "/" + id)
                                                 .then()
                                                 .statusCode(200),
                TIMEOUT_SECONDS);
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
