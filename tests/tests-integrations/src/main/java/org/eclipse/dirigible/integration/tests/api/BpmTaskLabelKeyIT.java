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
import static org.hamcrest.Matchers.nullValue;
import io.restassured.http.ContentType;
import java.nio.charset.StandardCharsets;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A BPM task names itself in the user's language (#6522).
 *
 * <p>
 * The Inbox, the notification bell and the task-form dialog are shared by every deployed
 * application, so they cannot know which module's translation catalog holds a task's name - which
 * is why an Approve button stayed English on an otherwise fully translated app. A generated process
 * therefore declares that catalog on its {@code <process>} element, and the inbox serves each task
 * the key of its own entry: the catalog plus the task's BPMN id, which is the authored step name.
 *
 * <p>
 * This drives the runtime half over HTTP - that Flowable carries the declaration through deployment
 * and that the keys reach the client. That the generator writes the declaration, and that the
 * catalog holds the matching entries, is covered by {@code IntentEngineIT}.
 */
class BpmTaskLabelKeyIT extends IntegrationTest {

    private static final String PROJECT = "bpm-task-label-key-it";
    private static final String PROCESS_KEY = "bpm-task-label-key-it-process";
    private static final String CATALOG = "sales:sales-model.processes";
    private static final String BPMN_REGISTRY_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/process.bpmn";
    private static final String UNKEYED_REGISTRY_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/hand-authored.bpmn";

    private static final long ASSERTION_TIMEOUT_SECONDS = 60;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @AfterEach
    void cleanup() {
        boolean removed = removeIfPresent(BPMN_REGISTRY_PATH) | removeIfPresent(UNKEYED_REGISTRY_PATH);
        if (removed) {
            synchronizationProcessor.forceProcessSynchronizers();
        }
    }

    private boolean removeIfPresent(String path) {
        if (!repository.hasResource(path)) {
            return false;
        }
        repository.removeResource(path);
        return true;
    }

    @Test
    void a_task_carries_the_key_of_its_own_module_catalog() {
        deploy(BPMN_REGISTRY_PATH,
                bpmnSource(PROCESS_KEY, "  <extensionElements>\n" + "    <flowable:property name=\"taskLabelCatalog\" value=\"" + CATALOG
                        + "\"></flowable:property>\n" + "  </extensionElements>\n"));
        String processInstanceId = startProcess(PROCESS_KEY);

        // The task's BPMN id is the authored step name, which is exactly how the module's catalog
        // keys its label - so the key is composed, never guessed.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/inbox/instance/" + processInstanceId + "/tasks?type=groups")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("[0].name", equalTo("Approve"))
                                                 .body("[0].nameKey", equalTo(CATALOG + ".approve"))
                                                 .body("[0].processDefinitionNameKey", equalTo(CATALOG + "." + PROCESS_KEY)),
                ASSERTION_TIMEOUT_SECONDS);

        completeFirstTask(processInstanceId);
    }

    /**
     * A hand-authored process declares no catalog, and must not be given a key that resolves to
     * nothing: with no key the client renders the raw BPMN name, exactly as it always did.
     */
    @Test
    void a_process_declaring_no_catalog_leaves_the_task_unkeyed() {
        String processKey = PROCESS_KEY + "-unkeyed";
        deploy(UNKEYED_REGISTRY_PATH, bpmnSource(processKey, ""));
        String processInstanceId = startProcess(processKey);

        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/inbox/instance/" + processInstanceId + "/tasks?type=groups")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("[0].name", equalTo("Approve"))
                                                 .body("[0].nameKey", nullValue())
                                                 .body("[0].processDefinitionNameKey", nullValue()),
                ASSERTION_TIMEOUT_SECONDS);

        completeFirstTask(processInstanceId);
    }

    private void deploy(String path, String content) {
        repository.createResource(path, content.getBytes(StandardCharsets.UTF_8), false, "application/xml", true);
        synchronizationProcessor.forceProcessSynchronizers();
    }

    private String startProcess(String processKey) {
        String body = "{\"processDefinitionKey\":\"" + processKey + "\",\"businessKey\":\"" + processKey + "-key\",\"parameters\":\"{}\"}";
        return restAssuredExecutor.executeWithResult(() -> given().contentType(ContentType.JSON)
                                                                  .body(body)
                                                                  .when()
                                                                  .post("/services/bpm/bpm-processes/instance")
                                                                  .then()
                                                                  .statusCode(200)
                                                                  .extract()
                                                                  .asString());
    }

    /** Ends the instance so it leaves no task behind for the next test. */
    private void completeFirstTask(String processInstanceId) {
        String taskId = restAssuredExecutor.executeWithResult(() -> given().when()
                                                                           .get("/services/inbox/instance/" + processInstanceId
                                                                                   + "/tasks?type=groups")
                                                                           .then()
                                                                           .statusCode(200)
                                                                           .extract()
                                                                           .path("[0].id"));
        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body("{\"action\":\"COMPLETE\"}")
                                                 .when()
                                                 .post("/services/inbox/tasks/" + taskId)
                                                 .then()
                                                 .statusCode(200));
    }

    /** The shape the intent generator emits, with the declaration under test injected verbatim. */
    private static String bpmnSource(String processKey, String processExtensionElements) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:flowable="http://flowable.org/bpmn"
                             targetNamespace="http://www.flowable.org/processdef">
                  <process id="%s" name="Approval" isExecutable="true">
                %s  <startEvent id="start"/>
                    <sequenceFlow id="f1" sourceRef="start" targetRef="approve"/>
                    <userTask id="approve" name="Approve" flowable:candidateGroups="ADMINISTRATOR"/>
                    <sequenceFlow id="f2" sourceRef="approve" targetRef="end"/>
                    <endEvent id="end"/>
                  </process>
                </definitions>
                """.formatted(processKey, processExtensionElements);
    }

}
