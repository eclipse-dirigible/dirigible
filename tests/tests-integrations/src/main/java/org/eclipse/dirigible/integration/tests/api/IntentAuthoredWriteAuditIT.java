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
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
 * A status change somebody made moves the record's {@code UpdatedAt} / {@code UpdatedBy}, and a
 * value the platform derives moves neither (issue #8690).
 *
 * <p>
 * A transition and a workflow setter write the status through a targeted write, which left every
 * column it did not name alone - the audit columns included - so an approved leave request read as
 * untouched since its last form save, and a poller could not ask which requests changed since a
 * given moment. The authored writes now stamp both columns in their own statement: the workflow
 * setter on the job executor with the user who completed the review task, the transition with the
 * user who pressed it. The roll-up the transition moves on the employee is a derived write, and the
 * process id the trigger stamps back is bookkeeping: neither moves an audit column.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentAuthoredWriteAuditIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "authoredaudit";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String EMPLOYEES = API + "/employee/EmployeeController";
    private static final String REQUESTS = API + "/vacationrequest/VacationRequestController";
    private static final String WITHDRAW =
            "/services/java/" + PROJECT + "/gen/events/" + PROJECT + "/WithdrawVacationRequestTransition/run";
    private static final String TASKS = "/services/inbox/tasks";
    private static final long TIMEOUT_SECONDS = 90;
    /**
     * The setter runs on the job executor, the roll-up off the transition's topic - both after a
     * commit.
     */
    private static final long ASYNC_TIMEOUT_SECONDS = 60;

    private static final String ADMIN = "admin";
    private static final String PASSWORD = "authored-audit-it-password";
    /** Files the requests. */
    private static final String REQUESTER = "authored-audit-it-requester";
    /** Presses Withdraw. */
    private static final String MANAGER = "authored-audit-it-manager";
    private static final String WRITE_ROLE = PROJECT + ".VacationRequest.VacationRequestFullAccess";

    private static final int SUBMITTED = 2;
    private static final int APPROVED = 3;
    private static final int WITHDRAWN = 6;

    private static final String INTENT_YAML = """
            name: authoredaudit
            description: a transition and a workflow setter stamp the audit columns, a roll-up does not

            entities:
              - name: Employee
                audit: true
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: email,     type: string, length: 100 }
                  - { name: withdrawn, type: integer }

              - name: VacationStatus
                function: Setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 40 }

              - name: VacationRequest
                audit: true
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 100 }
                relations:
                  - { name: Employee, kind: manyToOne, to: Employee, required: true }
                  - { name: Status, kind: manyToOne, to: VacationStatus, function: EntityStatus, init: 2 }

            transitions:
              - { name: WithdrawVacationRequest, forEntity: VacationRequest, from: [2, 3], setStatus: 6, label: Withdraw }

            processes:
              - name: VacationApproval
                trigger: { onCreate: VacationRequest }
                steps:
                  - { name: review,  kind: userTask, args: { assignee: approver, form: DecideVacationRequest } }
                  - { name: approve, kind: serviceTask, args: { setRelationField: Status, value: 3 } }
                  - { name: end,     kind: end }

            forms:
              - { name: DecideVacationRequest, forEntity: VacationRequest, fields: [note], editable: [note], actions: [approve] }

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
    void an_authored_status_change_stamps_the_audit_columns_and_a_derived_write_does_not() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();
        securityUtil.ensureUserInDefaultTenant(REQUESTER, PASSWORD, WRITE_ROLE);
        securityUtil.ensureUserInDefaultTenant(MANAGER, PASSWORD, WRITE_ROLE);

        int employee = create(EMPLOYEES, "{\"Email\":\"ann@example.com\"}");
        int approved = create(REQUESTS, request(employee), REQUESTER, PASSWORD);
        int untouched = create(REQUESTS, request(employee), REQUESTER, PASSWORD);
        // Each create started an approval; the trigger stamps its process id back - bookkeeping.
        String review = taskFor(approved);
        taskFor(untouched);
        Map<String, Object> filed = read(REQUESTS, approved);
        Map<String, Object> untouchedFiled = read(REQUESTS, untouched);
        Map<String, Object> employeeBefore = read(EMPLOYEES, employee);

        // The workflow setter runs on the job executor, after the review task completed: it stamps the
        // approval with the user who completed that task, not with nobody.
        complete(review, "{\"action\":\"approve\"}");
        Map<String, Object> afterApproval = awaitStatus(approved, APPROVED);
        assertEquals(ADMIN, afterApproval.get("UpdatedBy"), "the approval is attributed to whoever completed the review");
        Instant approvedAt = instant(afterApproval.get("UpdatedAt"));
        assertNotNull(approvedAt, "the approval moved UpdatedAt");
        if (filed.get("UpdatedAt") != null) {
            assertFalse(approvedAt.isBefore(instant(filed.get("UpdatedAt"))), "UpdatedAt moved forward on approval");
        }

        // The transition stamps the user who pressed it.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + approved + "}")
                                                 .when()
                                                 .post(WITHDRAW)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(WITHDRAWN))
                                                 .body("UpdatedBy", equalTo(MANAGER))
                                                 .body("UpdatedAt", notNullValue()),
                MANAGER, PASSWORD, TIMEOUT_SECONDS);
        Map<String, Object> afterWithdrawal = read(REQUESTS, approved);
        assertEquals(MANAGER, afterWithdrawal.get("UpdatedBy"));
        assertFalse(instant(afterWithdrawal.get("UpdatedAt")).isBefore(approvedAt), "UpdatedAt moved forward on withdrawal");

        // The roll-up the withdrawal moved on the employee is derived: neither audit column moves.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(EMPLOYEES + "/" + employee)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Withdrawn", equalTo(1)),
                ASYNC_TIMEOUT_SECONDS);
        Map<String, Object> employeeAfter = read(EMPLOYEES, employee);
        assertEquals(employeeBefore.get("UpdatedAt"), employeeAfter.get("UpdatedAt"), "a roll-up does not move UpdatedAt");
        assertEquals(employeeBefore.get("UpdatedBy"), employeeAfter.get("UpdatedBy"), "a roll-up does not move UpdatedBy");

        // Nor did the process id stamped back on the request nobody decided on.
        Map<String, Object> untouchedNow = read(REQUESTS, untouched);
        assertNotNull(untouchedNow.get("ProcessId"), "the trigger stamped the process id");
        assertEquals(untouchedFiled.get("UpdatedAt"), untouchedNow.get("UpdatedAt"));
        assertEquals(untouchedFiled.get("UpdatedBy"), untouchedNow.get("UpdatedBy"));
        assertNotEquals(ADMIN, untouchedNow.get("UpdatedBy"));

        // So a poller finds exactly the requests whose status changed since a moment.
        assertEquals(List.of(approved), changedSince(afterApproval.get("UpdatedAt")));
    }

    private static String request(int employee) {
        return "{\"Employee\":" + employee + ",\"Note\":\"leave\"}";
    }

    private List<Integer> changedSince(Object moment) {
        String filter = "{\"conditions\":[{\"propertyName\":\"UpdatedAt\",\"operator\":\"GTE\",\"value\":\"" + moment + "\"}]}";
        AtomicReference<List<Integer>> ids = new AtomicReference<>();
        restAssuredExecutor.execute(() -> ids.set(given().contentType("application/json")
                                                         .body(filter)
                                                         .when()
                                                         .post(REQUESTS + "/search")
                                                         .then()
                                                         .statusCode(200)
                                                         .extract()
                                                         .jsonPath()
                                                         .getList("Id", Integer.class)),
                TIMEOUT_SECONDS);
        return ids.get();
    }

    private static Instant instant(Object value) {
        if (value instanceof Number epochMillis) {
            return Instant.ofEpochMilli(epochMillis.longValue());
        }
        return value == null ? null : Instant.parse(value.toString());
    }

    private Map<String, Object> awaitStatus(int request, int status) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(REQUESTS + "/" + request)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(status)),
                ASYNC_TIMEOUT_SECONDS);
        return read(REQUESTS, request);
    }

    private Map<String, Object> read(String controller, int id) {
        AtomicReference<Map<String, Object>> row = new AtomicReference<>();
        restAssuredExecutor.execute(() -> row.set(given().when()
                                                         .get(controller + "/" + id)
                                                         .then()
                                                         .statusCode(200)
                                                         .extract()
                                                         .jsonPath()
                                                         .getMap("")),
                TIMEOUT_SECONDS);
        return row.get();
    }

    private String taskFor(int request) {
        AtomicReference<String> task = new AtomicReference<>();
        restAssuredExecutor.execute(() -> task.set(given().when()
                                                          .get(TASKS + "?type=groups")
                                                          .then()
                                                          .statusCode(200)
                                                          .body("find { it.processInstanceBusinessKey == '" + request + "' }",
                                                                  notNullValue())
                                                          .extract()
                                                          .path("find { it.processInstanceBusinessKey == '" + request + "' }.id")),
                ASYNC_TIMEOUT_SECONDS);
        return task.get();
    }

    private void complete(String task, String data) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"action\":\"COMPLETE\",\"data\":" + data + "}")
                                                 .when()
                                                 .post(TASKS + "/" + task)
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

    private int create(String controller, String body, String user, String password) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(controller)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                user, password, TIMEOUT_SECONDS);
        return id.get();
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
