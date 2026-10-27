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
import static org.hamcrest.Matchers.hasSize;

import java.nio.charset.StandardCharsets;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.restassured.http.ContentType;

/**
 * A user task addressed by {@code flowable:candidateUsers} only - no candidate group, no assignee -
 * is listed, claimed and completed by the named candidate (issue #8077).
 *
 * <p>
 * The Inbox's "available" bucket ({@code type=groups}) used to query candidate GROUPS alone, so a
 * task modelled with candidate users and nothing else was returned by neither {@code type=groups}
 * nor {@code type=assignee}: its candidates never saw it, nobody could claim it, and the instance
 * waited forever. The listing now matches candidate-group OR candidate-user, and the same listing
 * backs the claim/complete permission gate - so this one task drives the whole lifecycle over HTTP.
 */
class BpmInboxCandidateUserIT extends IntegrationTest {

    private static final String PROJECT = "bpm-inbox-candidate-user-it";
    private static final String PROCESS_KEY = "bpm-inbox-candidate-user-it-process";
    private static final String BPMN_REGISTRY_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/process.bpmn";
    private static final String GROUP_TASKS = "/services/inbox/tasks?type=groups";
    private static final String OUR_TASKS = "findAll { it.processDefinitionId.startsWith('" + PROCESS_KEY + ":') }";
    private static final long ASSERTION_TIMEOUT_SECONDS = 60;

    /** The candidate user the task names: the identity the default-tenant session authenticates as. */
    private static final String CANDIDATE_USER = DirigibleTestTenant.createDefaultTenant()
                                                                    .getUsername();

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @AfterEach
    void cleanup() {
        if (repository.hasResource(BPMN_REGISTRY_PATH)) {
            repository.removeResource(BPMN_REGISTRY_PATH);
            synchronizationProcessor.forceProcessSynchronizers();
        }
    }

    @Test
    void candidate_user_task_is_listed_claimed_and_completed() {
        repository.createResource(BPMN_REGISTRY_PATH, bpmn().getBytes(StandardCharsets.UTF_8), false, "application/xml", true);
        synchronizationProcessor.forceProcessSynchronizers();
        startProcess();

        // The regression: the candidate-user-only task appears in the "available" bucket.
        String taskId = listOurOpenTask();

        // The same listing gates the actions, so a candidate can now claim and complete it.
        executeTaskAction(taskId, "CLAIM");
        executeTaskAction(taskId, "COMPLETE");

        // Completing the process's only task ends it, so the task leaves the inbox.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(GROUP_TASKS)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(OUR_TASKS, hasSize(0)),
                ASSERTION_TIMEOUT_SECONDS);
    }

    private String listOurOpenTask() {
        String[] taskId = new String[1];
        restAssuredExecutor.execute(() -> taskId[0] = given().when()
                                                             .get(GROUP_TASKS)
                                                             .then()
                                                             .statusCode(200)
                                                             .body(OUR_TASKS, hasSize(1))
                                                             .extract()
                                                             .path(OUR_TASKS + ".id[0]"),
                ASSERTION_TIMEOUT_SECONDS);
        return taskId[0];
    }

    private void startProcess() {
        String body = "{\"processDefinitionKey\":\"" + PROCESS_KEY + "\",\"businessKey\":\"" + PROCESS_KEY + "\",\"parameters\":\"{}\"}";
        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body(body)
                                                 .when()
                                                 .post("/services/bpm/bpm-processes/instance")
                                                 .then()
                                                 .statusCode(200));
    }

    private void executeTaskAction(String taskId, String action) {
        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body("{\"action\":\"" + action + "\"}")
                                                 .when()
                                                 .post("/services/inbox/tasks/" + taskId)
                                                 .then()
                                                 .statusCode(200));
    }

    /** The process's only step is a user task offered to one candidate user - no group, no assignee. */
    private static String bpmn() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:flowable="http://flowable.org/bpmn"
                             targetNamespace="http://www.flowable.org/processdef">
                  <process id="%s" name="Candidate User" isExecutable="true">
                    <startEvent id="start"/>
                    <sequenceFlow id="f1" sourceRef="start" targetRef="review"/>
                    <userTask id="review" name="Review" flowable:candidateUsers="%s"/>
                    <sequenceFlow id="f2" sourceRef="review" targetRef="end"/>
                    <endEvent id="end"/>
                  </process>
                </definitions>
                """.formatted(PROCESS_KEY, CANDIDATE_USER);
    }
}
