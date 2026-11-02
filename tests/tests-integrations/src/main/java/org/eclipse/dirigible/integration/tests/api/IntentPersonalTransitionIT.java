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
import static org.hamcrest.Matchers.nullValue;
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
import org.eclipse.dirigible.tests.framework.security.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * The owner of a record runs a {@code personal: true} transition on it from the personal surface
 * (#8406).
 *
 * <p>
 * A transition used to run from the power surface only: its endpoint gated on the entity's write
 * roles and loaded any record by id, so an employee could not withdraw their own leave request, and
 * granting them a write role would have let them withdraw a colleague's. The personal run resolves
 * the caller's identity record as the entity's personal controller does - no identity is a 403, a
 * record that is not theirs the same 404 as a missing one, both before the {@code from:} guard -
 * and then writes exactly as the power run does, its {@code -transitioned} event included. Its
 * answer is the record as the personal surface serves it, so a sensitive field never leaves through
 * it, while the power run is unchanged.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentPersonalTransitionIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "personaltransition";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String EMPLOYEES = API + "/employee/EmployeeController";
    private static final String REQUESTS = API + "/vacationrequest/VacationRequestController";
    private static final String EVENTS = "/services/java/" + PROJECT + "/gen/events/" + PROJECT;
    private static final String POWER_RUN = EVENTS + "/WithdrawVacationRequestTransition/run";
    private static final String PERSONAL_RUN = EVENTS + "/WithdrawVacationRequestTransitionMy/run";
    private static final long TIMEOUT_SECONDS = 90;
    /** The roll-up runs off the request's topics, after the commit. */
    private static final long ROLLUP_TIMEOUT_SECONDS = 30;

    private static final String PASSWORD = "personal-transition-it-password";
    /** Mapped to an employee record, holding no role at all. */
    private static final String OWNER = "personal-transition-it-owner";
    /** Mapped to another employee record, holding no role at all. */
    private static final String COLLEAGUE = "personal-transition-it-colleague";
    /** Signed in, mapped to no employee record. */
    private static final String STRANGER = "personal-transition-it-stranger";

    private static final int DRAFT = 1;
    private static final int SUBMITTED = 2;
    private static final int APPROVED = 3;
    private static final int WITHDRAWN = 6;

    private static final String INTENT_YAML = """
            name: personaltransition
            description: an employee withdraws their own leave request from the personal surface

            entities:
              - name: Employee
                identity: email
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: email,     type: string, length: 100, unique: true }
                  - { name: withdrawn, type: integer }

              - name: VacationStatus
                function: Setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 40 }

              - name: VacationRequest
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: note,   type: string, length: 100 }
                  - { name: hrNote, type: string, length: 100, sensitive: true }
                relations:
                  - { name: Employee, kind: manyToOne, to: Employee, required: true, personal: true }
                  - { name: Status, kind: manyToOne, to: VacationStatus, function: EntityStatus, init: 1 }

            transitions:
              - { name: WithdrawVacationRequest, forEntity: VacationRequest, from: [2, 3], setStatus: 6, label: Withdraw, personal: true }

            rollups:
              - name: employeeWithdrawn
                entity: VacationRequest
                via: Employee
                field: withdrawn
                op: count
                where:
                  - { field: Status, op: eq, value: 6 }

            seeds:
              - name: vacation-statuses
                entity: VacationStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: SUBMITTED }
                  - { id: 3, name: APPROVED }
                  - { id: 6, name: WITHDRAWN }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private SecurityUtil securityUtil;

    @Test
    void the_owner_runs_the_transition_on_their_own_record_and_on_nobody_elses() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();
        securityUtil.ensureUserInDefaultTenant(OWNER, PASSWORD);
        securityUtil.ensureUserInDefaultTenant(COLLEAGUE, PASSWORD);
        securityUtil.ensureUserInDefaultTenant(STRANGER, PASSWORD);

        int owner = create(EMPLOYEES, "{\"Email\":\"" + OWNER + "\"}");
        int colleague = create(EMPLOYEES, "{\"Email\":\"" + COLLEAGUE + "\"}");
        int submitted = create(REQUESTS, request(owner, SUBMITTED));
        int draft = create(REQUESTS, request(owner, DRAFT));
        int colleagues = create(REQUESTS, request(colleague, SUBMITTED));

        // No identity record mapped: refused before anything is read - an id that does not exist alike.
        refused(STRANGER, PERSONAL_RUN, submitted, 403, "no identity record is mapped to the current user");
        refused(STRANGER, PERSONAL_RUN, 987654, 403, "no identity record is mapped to the current user");

        // A colleague's record is the same 404 as a missing one, and stays as it was.
        refused(OWNER, PERSONAL_RUN, colleagues, 404, "VacationRequest not found");
        refused(OWNER, PERSONAL_RUN, 987654, 404, "VacationRequest not found");
        assertEquals(SUBMITTED, status(colleagues));

        // Their own record in a status outside from: the guard's 409, nothing written.
        refused(OWNER, PERSONAL_RUN, draft, 409, "allowed only from status [2, 3]");
        assertEquals(DRAFT, status(draft));

        // The power run is unchanged: it still demands the entity's write gate, which the owner lacks.
        refused(OWNER, POWER_RUN, submitted, 403, "forbidden");
        assertEquals(SUBMITTED, status(submitted));

        // Their own record in an allowed status: the status is written, and the record comes back as
        // the personal surface serves it - its sensitive field withheld.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + submitted + "}")
                                                 .when()
                                                 .post(PERSONAL_RUN)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Id", equalTo(submitted))
                                                 .body("Status", equalTo(WITHDRAWN))
                                                 .body("HrNote", nullValue()),
                OWNER, PASSWORD, TIMEOUT_SECONDS);
        assertEquals(WITHDRAWN, status(submitted));
        // The -transitioned event rode the write: the roll-up counting withdrawn requests saw it.
        awaitWithdrawn(owner, 1);

        // The power run answers as it always did, the whole record included.
        int approved = create(REQUESTS, request(owner, APPROVED));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + approved + "}")
                                                 .when()
                                                 .post(POWER_RUN)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(WITHDRAWN))
                                                 .body("HrNote", equalTo("hr only")),
                TIMEOUT_SECONDS);
        awaitWithdrawn(owner, 2);
        awaitWithdrawn(colleague, 0);
    }

    private static String request(int employee, int status) {
        return "{\"Employee\":" + employee + ",\"Status\":" + status + ",\"Note\":\"leave\",\"HrNote\":\"hr only\"}";
    }

    private void refused(String user, String endpoint, int id, int expectedStatus, String reason) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + id + "}")
                                                 .when()
                                                 .post(endpoint)
                                                 .then()
                                                 .statusCode(expectedStatus)
                                                 .body("error", containsString(reason)),
                user, PASSWORD, TIMEOUT_SECONDS);
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

    private int status(int request) {
        AtomicInteger status = new AtomicInteger();
        restAssuredExecutor.execute(() -> status.set(given().when()
                                                            .get(REQUESTS + "/" + request)
                                                            .then()
                                                            .statusCode(200)
                                                            .body("Id", equalTo(request))
                                                            .extract()
                                                            .path("Status")));
        return status.get();
    }

    private void awaitWithdrawn(int employee, int withdrawn) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(EMPLOYEES + "/" + employee)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Withdrawn", equalTo(withdrawn)),
                ROLLUP_TIMEOUT_SECONDS);
    }

    private void generateProject() {
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
