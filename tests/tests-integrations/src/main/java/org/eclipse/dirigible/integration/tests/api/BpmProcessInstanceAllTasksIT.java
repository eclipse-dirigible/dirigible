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
import static org.hamcrest.Matchers.hasSize;

import java.nio.charset.StandardCharsets;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.restassured.http.ContentType;

/**
 * The operator-facing instance tasks listing returns every task of the instance, not only the
 * viewer's own (issue #8076).
 *
 * <p>
 * The Monitoring Processes page's Tasks tab and the IDE's instance view ask
 * {@code /bpm-processes/instance/{id}/tasks} for the instance's open tasks, but a missing
 * {@code type} defaulted to {@code assignee} - so an instance parked on someone else's task read
 * "No open user tasks." to the operator looking at it. {@code type=all} now returns the instance's
 * tasks whoever they are addressed to, behind the operator role gate, while {@code type=assignee}
 * stays scoped to the caller.
 */
class BpmProcessInstanceAllTasksIT extends IntegrationTest {

    private static final String PROJECT = "bpm-instance-all-tasks-it";
    private static final String PROCESS_KEY = "bpm-instance-all-tasks-it-process";
    private static final String BUSINESS_KEY = "bpm-instance-all-tasks-it-bk";
    private static final String OTHER_ASSIGNEE = "another-operator";
    private static final String BPMN_REGISTRY_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/process.bpmn";
    private static final long ASSERTION_TIMEOUT_SECONDS = 60;

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
    void all_lists_the_instance_task_though_it_is_assigned_to_another_user() {
        repository.createResource(BPMN_REGISTRY_PATH, BPMN.getBytes(StandardCharsets.UTF_8), false, "application/xml", true);
        synchronizationProcessor.forceProcessSynchronizers();
        startProcess();
        String instanceId = instanceId();
        String tasks = "/services/bpm/bpm-processes/instance/" + instanceId + "/tasks";

        // type=all: the operator sees the instance's task even though it is assigned to someone else.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(tasks + "?type=all")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("name", contains("Approve"))
                                                 .body("assignee", contains(OTHER_ASSIGNEE)),
                ASSERTION_TIMEOUT_SECONDS);

        // type=assignee: scoped to the caller (the operator is not the assignee), so it returns nothing -
        // the "No open user tasks." the bug showed for the whole tab.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(tasks + "?type=assignee")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("$", hasSize(0)));
    }

    private void startProcess() {
        String body = "{\"processDefinitionKey\":\"" + PROCESS_KEY + "\",\"businessKey\":\"" + BUSINESS_KEY + "\",\"parameters\":\"{}\"}";
        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body(body)
                                                 .when()
                                                 .post("/services/bpm/bpm-processes/instance")
                                                 .then()
                                                 .statusCode(200));
    }

    private String instanceId() {
        String[] id = new String[1];
        restAssuredExecutor.execute(() -> id[0] = given().when()
                                                         .get("/services/bpm/bpm-processes/instances?businessKey=" + BUSINESS_KEY)
                                                         .then()
                                                         .statusCode(200)
                                                         .body("businessKey", contains(BUSINESS_KEY))
                                                         .extract()
                                                         .path("id[0]"),
                ASSERTION_TIMEOUT_SECONDS);
        return id[0];
    }

    /** The process parks on a user task assigned to someone other than the operator who views it. */
    private static final String BPMN = """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                         xmlns:flowable="http://flowable.org/bpmn"
                         targetNamespace="http://www.flowable.org/processdef">
              <process id="%s" name="All Tasks" isExecutable="true">
                <startEvent id="start"/>
                <sequenceFlow id="f1" sourceRef="start" targetRef="approve"/>
                <userTask id="approve" name="Approve" flowable:assignee="%s"/>
                <sequenceFlow id="f2" sourceRef="approve" targetRef="end"/>
                <endEvent id="end"/>
              </process>
            </definitions>
            """.formatted(PROCESS_KEY, OTHER_ASSIGNEE);
}
