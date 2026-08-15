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
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.restassured.filter.session.SessionFilter;
import io.restassured.http.ContentType;

/**
 * End-to-end test for the failure modes of a FORGOTTEN act-as (delegated entry) arming (#6694): the
 * armed state expires on its own, the Inbox says what the arming is hiding instead of rendering an
 * indistinguishable empty list, and a back-office group task claimed while armed is stamped with
 * the REAL user - never with the person being acted for, who never made that decision.
 *
 * <p>
 * The override lives in the server-side HTTP session, so every sequence pins one session through a
 * {@link SessionFilter}.
 */
@Tag("slow")
class ActAsSessionIT extends IntegrationTest {

    private static final String ACT_AS_ENDPOINT = "/services/core/actas";
    private static final String INBOX_ACT_AS_ENDPOINT = "/services/inbox/act-as";

    private static final String ACTING_IDENTITY = "act-as-session-it@example.com";
    private static final String REAL_USER = "admin";

    private static final String PROJECT = "act-as-session-it";
    private static final String PROCESS_KEY = "act-as-session-it-process";
    private static final String BUSINESS_KEY = "act-as-session-it-key";
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
        DirigibleConfig.ACT_AS_TTL_SECONDS.setStringValue(DirigibleConfig.ACT_AS_TTL_SECONDS.getDefaultValue());
        if (repository.hasResource(BPMN_REGISTRY_PATH)) {
            repository.removeResource(BPMN_REGISTRY_PATH);
            synchronizationProcessor.forceProcessSynchronizers();
        }
    }

    @Test
    void the_armed_state_reports_the_deadline_it_expires_at() {
        restAssuredExecutor.execute(() -> {
            SessionFilter session = new SessionFilter();
            given().filter(session)
                   .when()
                   .get(ACT_AS_ENDPOINT)
                   .then()
                   .statusCode(200)
                   .body("entitled", equalTo(true))
                   .body("actingAs", nullValue())
                   .body("expiresAt", nullValue());

            long armedAt = System.currentTimeMillis();
            long window = DirigibleConfig.ACT_AS_TTL_SECONDS.getIntValue() * 1000L;
            given().filter(session)
                   .contentType(ContentType.JSON)
                   .body("{\"username\":\"" + ACTING_IDENTITY + "\"}")
                   .when()
                   .put(ACT_AS_ENDPOINT)
                   .then()
                   .statusCode(200)
                   .body("actingAs", equalTo(ACTING_IDENTITY))
                   .body("expiresAt", notNullValue())
                   .body("expiresAt", greaterThanOrEqualTo(armedAt))
                   .body("expiresAt", lessThanOrEqualTo(System.currentTimeMillis() + window));

            given().filter(session)
                   .when()
                   .delete(ACT_AS_ENDPOINT)
                   .then()
                   .statusCode(200)
                   .body("actingAs", nullValue());
        });
    }

    /**
     * The incident this guards against: an arming made for one delegated entry stayed armed for hours.
     * The window is absolute, so no amount of activity renews it.
     */
    @Test
    void a_forgotten_arming_expires_on_its_own() {
        DirigibleConfig.ACT_AS_TTL_SECONDS.setStringValue("1");

        SessionFilter session = new SessionFilter();
        restAssuredExecutor.execute(() -> given().filter(session)
                                                 .contentType(ContentType.JSON)
                                                 .body("{\"username\":\"" + ACTING_IDENTITY + "\"}")
                                                 .when()
                                                 .put(ACT_AS_ENDPOINT)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("actingAs", equalTo(ACTING_IDENTITY)));

        // The retry loop is the wait: the same session stops reporting the acting identity once the
        // window has elapsed, without anyone disarming it.
        restAssuredExecutor.execute(() -> given().filter(session)
                                                 .when()
                                                 .get(ACT_AS_ENDPOINT)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("actingAs", nullValue())
                                                 .body("expiresAt", nullValue()),
                ASSERTION_TIMEOUT_SECONDS);
    }

    /**
     * A back-office group task reached through the REAL user's roles is claimed by the real user even
     * while armed (the acted-as person never made that decision and could not see the task), and the
     * Inbox reports how many of the real user's own tasks the arming is hiding.
     */
    @Test
    void a_role_addressed_task_is_claimed_by_the_real_user_and_reported_as_hidden() {
        repository.createResource(BPMN_REGISTRY_PATH, bpmnSource().getBytes(StandardCharsets.UTF_8), false, "application/xml", true);
        synchronizationProcessor.forceProcessSynchronizers();

        String processInstanceId = startProcess();
        String taskId = awaitTaskId(processInstanceId);

        // Claim it from an ARMED session - the assignee must still be the real user.
        restAssuredExecutor.execute(() -> {
            SessionFilter session = new SessionFilter();
            given().filter(session)
                   .contentType(ContentType.JSON)
                   .body("{\"username\":\"" + ACTING_IDENTITY + "\"}")
                   .when()
                   .put(ACT_AS_ENDPOINT)
                   .then()
                   .statusCode(200);
            given().filter(session)
                   .contentType(ContentType.JSON)
                   .body("{\"action\":\"CLAIM\"}")
                   .when()
                   .post("/services/inbox/tasks/" + taskId)
                   .then()
                   .statusCode(200);
            // ... and the armed Inbox now says so: the task it just claimed for the real user is one
            // of the tasks this arming hides from that same Inbox.
            given().filter(session)
                   .when()
                   .get(INBOX_ACT_AS_ENDPOINT)
                   .then()
                   .statusCode(200)
                   .body("actingAs", equalTo(ACTING_IDENTITY))
                   .body("hiddenTasks", greaterThanOrEqualTo(1));
        });

        // An unarmed session sees the task as its own, assigned to the real user.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get("/services/inbox/tasks?type=assignee")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("find { it.id == '" + taskId + "' }.assignee", equalTo(REAL_USER)),
                ASSERTION_TIMEOUT_SECONDS);
        // Nothing is armed, so the Inbox has nothing to report.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(INBOX_ACT_AS_ENDPOINT)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("actingAs", nullValue())
                                                 .body("hiddenTasks", equalTo(0)));

        // Complete the task so the instance ends and leaves no task behind for the next test.
        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body("{\"action\":\"COMPLETE\"}")
                                                 .when()
                                                 .post("/services/inbox/tasks/" + taskId)
                                                 .then()
                                                 .statusCode(200));
    }

    private String startProcess() {
        String body = "{\"processDefinitionKey\":\"" + PROCESS_KEY + "\",\"businessKey\":\"" + BUSINESS_KEY + "\",\"parameters\":\"{}\"}";
        return restAssuredExecutor.executeWithResult(() -> given().contentType(ContentType.JSON)
                                                                  .body(body)
                                                                  .when()
                                                                  .post("/services/bpm/bpm-processes/instance")
                                                                  .then()
                                                                  .statusCode(200)
                                                                  .extract()
                                                                  .asString());
    }

    private String awaitTaskId(String processInstanceId) {
        AtomicReference<String> taskId = new AtomicReference<>();
        restAssuredExecutor.execute(() -> {
            String id = given().when()
                               .get("/services/inbox/instance/" + processInstanceId + "/tasks?type=groups")
                               .then()
                               .statusCode(200)
                               .extract()
                               .path("[0].id");
            assertNotNull(id, "The process instance has no group task yet");
            taskId.set(id);
        }, ASSERTION_TIMEOUT_SECONDS);
        return taskId.get();
    }

    private static String bpmnSource() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                             xmlns:flowable="http://flowable.org/bpmn"
                             targetNamespace="http://www.flowable.org/processdef">
                  <process id="%s" name="Act as session IT" isExecutable="true">
                    <startEvent id="start"/>
                    <sequenceFlow id="f1" sourceRef="start" targetRef="back-office-task"/>
                    <userTask id="back-office-task" name="Send" flowable:candidateGroups="ADMINISTRATOR"/>
                    <sequenceFlow id="f2" sourceRef="back-office-task" targetRef="end"/>
                    <endEvent id="end"/>
                  </process>
                </definitions>
                """.formatted(PROCESS_KEY);
    }

}
