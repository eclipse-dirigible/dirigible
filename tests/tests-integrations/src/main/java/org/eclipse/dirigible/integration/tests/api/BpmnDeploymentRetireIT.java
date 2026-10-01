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
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.flowable.engine.ProcessEngine;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import io.restassured.http.ContentType;

/**
 * A {@code .bpmn} artefact that disappears from the registry does not take its running instances
 * with it (#7597). The synchronizer's DELETE retires the deployment: with an instance parked on a
 * user task it is kept and suspended (no new start, the instance keeps running), the artefact
 * coming back is the next version and starts instances again, and once the old version's instance
 * has ended the next DELETE removes everything while the history keeps the finished instances
 * (#7598). HTTP-level, no browser: the artefact is written straight into the registry and the
 * synchronizers are run to completion.
 */
class BpmnDeploymentRetireIT extends IntegrationTest {

    private static final String PROJECT = "bpmn-retire-it";
    private static final String PROCESS_KEY = "bpmn-retire-it-approval";
    private static final String BPMN_LOCATION = "/" + PROJECT + "/approval.bpmn";
    private static final String BPMN_REGISTRY_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + BPMN_LOCATION;

    private static final long ASSERTION_TIMEOUT_SECONDS = 60;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private ProcessEngine processEngine;

    @Test
    void a_removed_bpmn_keeps_its_running_instances_and_comes_back_as_the_next_version() {
        deployArtefact();
        String parked = startProcess();
        assertInstanceListed(parked);

        // The artefact goes away - a module upgrade, a registry empty for one cycle.
        repository.removeResource(BPMN_REGISTRY_PATH);
        synchronizationProcessor.forceProcessSynchronizers();

        assertInstanceListed(parked);
        assertEquals(1, deployments(), "the deployment with work in flight is kept");
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/bpm/bpm-processes/definitions?key=" + PROCESS_KEY)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("suspended", hasItem(true)),
                ASSERTION_TIMEOUT_SECONDS);
        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body(startBody())
                                                 .when()
                                                 .post("/services/bpm/bpm-processes/instance")
                                                 .then()
                                                 .statusCode(200)
                                                 .body(emptyString()),
                ASSERTION_TIMEOUT_SECONDS);

        // The artefact comes back: the next version, live again, the old instance still parked.
        deployArtefact();
        String fresh = startProcess();
        assertInstanceListed(fresh);
        assertInstanceListed(parked);
        assertEquals(2, deployments(), "the retired version stays while its instance runs");

        // Both instances end; the next removal takes everything out, the history keeps both.
        complete(parked);
        complete(fresh);
        repository.removeResource(BPMN_REGISTRY_PATH);
        synchronizationProcessor.forceProcessSynchronizers();

        assertEquals(0, deployments(), "no running instance left: the deployments are deleted");
        // Unfiltered on purpose: Flowable answers a definitionKey filter through the definition table,
        // which the delete just emptied for this key, while the history rows themselves stay.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/bpm/bpm-processes/historic-instances")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("id", hasItem(parked))
                                                 .body("id", hasItem(fresh)),
                ASSERTION_TIMEOUT_SECONDS);
    }

    @Test
    void an_idle_bpmn_is_deleted_outright_with_its_history_kept() {
        deployArtefact();
        String ended = startProcess();
        complete(ended);

        repository.removeResource(BPMN_REGISTRY_PATH);
        synchronizationProcessor.forceProcessSynchronizers();

        assertEquals(0, deployments());
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/bpm/bpm-processes/historic-instances")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("id", hasItem(ended))
                                                 .body("findAll { it.id == '" + ended + "' }", hasSize(1)),
                ASSERTION_TIMEOUT_SECONDS);
    }

    @AfterEach
    void cleanup() {
        if (repository.hasResource(BPMN_REGISTRY_PATH)) {
            repository.removeResource(BPMN_REGISTRY_PATH);
        }
        processEngine.getRuntimeService()
                     .createProcessInstanceQuery()
                     .processDefinitionKey(PROCESS_KEY)
                     .list()
                     .forEach(instance -> processEngine.getRuntimeService()
                                                       .deleteProcessInstance(instance.getId(), "test cleanup"));
        synchronizationProcessor.forceProcessSynchronizers();
    }

    private void deployArtefact() {
        repository.createResource(BPMN_REGISTRY_PATH, bpmnSource().getBytes(StandardCharsets.UTF_8), false, "application/xml", true);
        synchronizationProcessor.forceProcessSynchronizers();
    }

    private String startProcess() {
        return restAssuredExecutor.executeWithResult(() -> given().contentType(ContentType.JSON)
                                                                  .body(startBody())
                                                                  .when()
                                                                  .post("/services/bpm/bpm-processes/instance")
                                                                  .then()
                                                                  .statusCode(200)
                                                                  .body(not(emptyString()))
                                                                  .extract()
                                                                  .asString());
    }

    private static String startBody() {
        return "{\"processDefinitionKey\":\"" + PROCESS_KEY + "\",\"businessKey\":\"bpmn-retire-it\",\"parameters\":\"{}\"}";
    }

    private void assertInstanceListed(String processInstanceId) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/bpm/bpm-processes/instance/" + processInstanceId)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("id", equalTo(processInstanceId)),
                ASSERTION_TIMEOUT_SECONDS);
    }

    private void complete(String processInstanceId) {
        Task task = processEngine.getTaskService()
                                 .createTaskQuery()
                                 .processInstanceId(processInstanceId)
                                 .singleResult();
        processEngine.getTaskService()
                     .complete(task.getId());
    }

    private long deployments() {
        return processEngine.getRepositoryService()
                            .createDeploymentQuery()
                            .deploymentKey(BPMN_LOCATION)
                            .count();
    }

    private static String bpmnSource() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:flowable="http://flowable.org/bpmn"
                             targetNamespace="http://www.flowable.org/processdef">
                  <process id="%s" name="BPMN retire IT" isExecutable="true">
                    <startEvent id="start"/>
                    <sequenceFlow id="f1" sourceRef="start" targetRef="approve"/>
                    <userTask id="approve" name="Approve" flowable:candidateGroups="ADMINISTRATOR"/>
                    <sequenceFlow id="f2" sourceRef="approve" targetRef="end"/>
                    <endEvent id="end"/>
                  </process>
                </definitions>
                """.formatted(PROCESS_KEY);
    }
}
