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
import static org.hamcrest.Matchers.notNullValue;
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

import io.restassured.response.Response;

/**
 * {@code separateFrom: personal} keeps a record's owner off its own approval when the process has
 * no submit task - the record is submitted by being created - and the approval reaches the owner
 * through a group they belong to (dirigible #8560). The owner is who the record is FOR: a request a
 * clerk files on the employee's behalf keeps that employee off as well.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentSeparateFromPersonalIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "leaveowner";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String EMPLOYEES = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api/employee/EmployeeController";
    private static final String LEAVES = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api/leaverequest/LeaveRequestController";
    private static final String TASKS = "/services/inbox/tasks";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String PASSWORD = "separate-from-personal-it-password";
    /** A manager filing their own leave: in the approving group, and the record's owner. */
    private static final String OWNER = "leave-owner";
    /** Another member of the approving group. */
    private static final String APPROVER = "leave-approver";
    /** Files requests on the employees' behalf, approves none. */
    private static final String CLERK = "leave-clerk";

    private static final String INTENT_YAML = """
            name: leaveowner
            description: a leave request is submitted by being created and approved by a manager who is not its owner

            entities:
              - name: Employee
                identity: email
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: email, type: string, required: true, unique: true, length: 100 }
              - name: LeaveRequest
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: days, type: integer }
                relations:
                  - { name: Employee, kind: manyToOne, to: Employee, required: true, personal: true }

            processes:
              - name: LeaveApproval
                trigger: { onCreate: LeaveRequest }
                steps:
                  - { name: approve, kind: userTask, args: { assignee: manager, form: ApproveLeave, separateFrom: personal } }

            forms:
              - { name: ApproveLeave, forEntity: LeaveRequest, fields: [days], actions: [approve] }

            permissions:
              - { role: Clerk,    can: [Employee:write, LeaveRequest:write] }
              - { role: Manager,  can: [LeaveRequest:write] }
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
    void the_records_owner_may_neither_claim_nor_complete_its_approval_whoever_filed_it() {
        generateAndPublish();
        securityUtil.createUserInDefaultTenant(OWNER, PASSWORD, "Manager");
        securityUtil.createUserInDefaultTenant(APPROVER, PASSWORD, "Manager");
        securityUtil.createUserInDefaultTenant(CLERK, PASSWORD, "Clerk");

        int employee = createEmployee(OWNER);

        // Filed by the owner: no submit task records them, the owner relation does.
        int own = createLeave(OWNER, employee);
        assertOwnerKeptOffAndAnotherManagerApproves(taskFor(own));

        // Filed by a clerk on the owner's behalf: still the owner's request.
        int onBehalf = createLeave(CLERK, employee);
        assertOwnerKeptOffAndAnotherManagerApproves(taskFor(onBehalf));
    }

    private void assertOwnerKeptOffAndAnotherManagerApproves(String approve) {
        // Each refusal is attempted ONCE: a retried 200 would have claimed or completed the task.
        restAssuredExecutor.execute(() -> act(approve, "{\"action\":\"CLAIM\"}").then()
                                                                                .statusCode(403)
                                                                                .body(containsString(
                                                                                        "You cannot approve your own leave request")),
                "localhost", OWNER, PASSWORD);
        restAssuredExecutor.execute(() -> act(approve, completion()).then()
                                                                    .statusCode(403)
                                                                    .body(containsString("You cannot approve your own leave request")),
                "localhost", OWNER, PASSWORD);
        restAssuredExecutor.execute(() -> act(approve, completion()).then()
                                                                    .statusCode(200),
                "localhost", APPROVER, PASSWORD);
    }

    private int createEmployee(String email) {
        return create(CLERK, EMPLOYEES, "{\"Email\":\"" + email + "\"}");
    }

    private int createLeave(String user, int employee) {
        return create(user, LEAVES, "{\"Days\":3,\"Employee\":" + employee + "}");
    }

    private int create(String user, String url, String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(url)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                user, PASSWORD, TIMEOUT_SECONDS);
        return id.get();
    }

    /** The open approval of this request's instance, as the administrator's Inbox lists it. */
    private String taskFor(int leave) {
        String match = "find { it.processInstanceBusinessKey == '" + leave + "' && it.name == 'Approve' }";
        AtomicReference<String> task = new AtomicReference<>();
        restAssuredExecutor.execute(() -> task.set(given().when()
                                                          .get(TASKS + "?type=groups")
                                                          .then()
                                                          .statusCode(200)
                                                          .body(match, notNullValue())
                                                          .extract()
                                                          .path(match + ".id")),
                TIMEOUT_SECONDS);
        return task.get();
    }

    private static String completion() {
        return "{\"action\":\"COMPLETE\",\"data\":{\"action\":\"approve\"}}";
    }

    private static Response act(String task, String body) {
        return given().contentType("application/json")
                      .body(body)
                      .post(TASKS + "/" + task);
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
