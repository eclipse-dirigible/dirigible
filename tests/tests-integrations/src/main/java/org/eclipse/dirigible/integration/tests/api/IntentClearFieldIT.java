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
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * A process can take back a field it wrote - dirigible #7386.
 *
 * <p>
 * A {@code setField} value is refused when it is blank, so a process had no declarative erasure at
 * all: the generated error route writes the failure text, and an instance re-driven to success
 * ended in a success status still carrying the previous failure's explanation. {@code clearField:}
 * is that erasure, and this asserts it end to end - the generated setter delegate really compiles,
 * really runs, and the column really goes back to empty.
 *
 * <p>
 * Both halves are asserted in one journey, because either alone proves nothing: a column that reads
 * empty at the end may simply never have been written, so the flow writes the field first, the test
 * reads it back, and only then does the erasure step run.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentClearFieldIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "clearfield";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String DEPLOYMENTS = API + "/deployment/DeploymentController";
    private static final String TASKS = "/services/inbox/tasks";
    private static final String FAILURE = "the tenant database refused the connection";
    private static final long TIMEOUT_SECONDS = 90;
    /** The steps run once the create event has started the instance. */
    private static final long PROCESS_TIMEOUT_SECONDS = 60;

    private static final String INTENT_YAML =
            """
                    name: clearfield
                    description: a process clears the failure text it wrote once the deployment is re-driven to success

                    entities:
                      - name: Deployment
                        fields:
                          - { name: id,           type: integer, primaryKey: true, generated: true }
                          - { name: tenant,       type: string, required: true, length: 100 }
                          - { name: errorMessage, type: string, length: 400 }

                    processes:
                      - name: DeploymentProvisioning
                        trigger: { onCreate: Deployment }
                        steps:
                          - { name: recordFailure, kind: serviceTask, args: { setField: errorMessage, value: "the tenant database refused the connection", next: reviewFailure } }
                          - { name: reviewFailure,  kind: userTask, args: { assignee: operator, form: RetryDeployment } }
                          - { name: resetError,    kind: serviceTask, args: { clearField: errorMessage, next: done } }
                          - { name: done,          kind: end }

                    forms:
                      - { name: RetryDeployment, forEntity: Deployment, fields: [tenant], actions: [retry] }
                    """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_process_writes_a_failure_text_and_then_clears_it() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int deployment = create("{\"Tenant\":\"acme\"}");

        // The flow's first step writes the failure text...
        awaitErrorMessage(deployment, equalTo(FAILURE));

        // ...and the erasure step behind the retry task takes it back.
        complete(taskFor(deployment));
        awaitErrorMessage(deployment, nullValue());
    }

    private void awaitErrorMessage(int deployment, Matcher<?> expected) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(DEPLOYMENTS + "/" + deployment)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("ErrorMessage", expected),
                PROCESS_TIMEOUT_SECONDS);
    }

    private void complete(String task) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"action\":\"COMPLETE\",\"data\":{\"action\":\"retry\"}}")
                                                 .when()
                                                 .post(TASKS + "/" + task)
                                                 .then()
                                                 .statusCode(200));
    }

    /** The retry task of this deployment's instance, found by the business key the trigger stamped. */
    private String taskFor(int deployment) {
        AtomicReference<String> task = new AtomicReference<>();
        restAssuredExecutor.execute(() -> task.set(given().when()
                                                          .get(TASKS + "?type=groups")
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .path("find { it.processInstanceBusinessKey == '" + deployment + "' }.id")),
                PROCESS_TIMEOUT_SECONDS);
        return task.get();
    }

    private int create(String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(DEPLOYMENTS)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                TIMEOUT_SECONDS);
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
