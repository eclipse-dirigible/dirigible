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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.flowable.engine.ProcessEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A record stamped with a process instance the engine no longer has is not stuck (#7599). The
 * generated trigger distrusts a stamp naming an instance that is neither running nor in the history
 * and starts a new one on the next qualifying event; and for the records no event will touch again,
 * {@code POST /services/bpm/bpm-processes/restart} re-runs the trigger - refused while the stamped
 * instance is still running, starting and re-stamping once it is not. The intent is generated,
 * published and compiled as a developer's would be, so the trigger under test is the real generated
 * one.
 */
class ProcessTriggerRestartIT extends IntegrationTest {

    private static final String PROJECT = "trigger-restart-it";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String API = "/services/java/" + PROJECT + "/gen/tickets/api/ticket/TicketController";
    private static final String RESTART_URL = "/services/bpm/bpm-processes/restart";
    private static final String PROCESS = "TicketApproval";

    private static final long ASSERTION_TIMEOUT_SECONDS = 120;

    private static final String INTENT_YAML = """
            name: tickets
            description: process trigger restart fixture

            entities:
              - name: Ticket
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: title, type: string,  required: true, length: 100 }

            processes:
              - name: TicketApproval
                trigger: { onUpdate: Ticket }
                steps:
                  - name: review
                    kind: userTask
                    args: { assignee: admin }
                  - name: done
                    kind: end
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private ProcessEngine processEngine;

    @Test
    void a_dangling_stamp_is_distrusted_by_the_trigger_and_repaired_through_the_restart_endpoint() {
        generateAndPublish();

        int id = create("first");
        update(id, "second");
        String first = awaitProcessId(id, null);

        // The stamp names a running instance: the next update starts nothing.
        update(id, "third");
        assertEquals(first, processIdOf(id), "a running instance is trusted - no second start");

        // The instance vanishes without a trace - what a cascade delete or a lost database leaves
        // behind. The stamp still says "started"; the next qualifying event starts a new instance.
        processEngine.getRuntimeService()
                     .deleteProcessInstance(first, "lost");
        processEngine.getHistoryService()
                     .deleteHistoricProcessInstance(first);
        update(id, "fourth");
        String second = awaitProcessId(id, first);
        assertNotEquals(first, second);

        // An operator's restart is refused while the stamped instance still runs.
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(RESTART_URL + "?process=" + PROCESS + "&id=" + id)
                                                 .then()
                                                 .statusCode(409)
                                                 .body("message",
                                                         equalTo("Ticket [" + id + "] still has its " + PROCESS + " instance [" + second
                                                                 + "] running - complete or cancel it before restarting")),
                ASSERTION_TIMEOUT_SECONDS);

        // Cancelled (and so in the history): no event will touch the record again, the restart
        // starts and re-stamps it - the same variables and stamping as the create path.
        processEngine.getRuntimeService()
                     .deleteProcessInstance(second, "cancelled by an operator");
        String third = restAssuredExecutor.executeWithResult(() -> given().when()
                                                                          .post(RESTART_URL + "?process=" + PROCESS + "&id=" + id)
                                                                          .then()
                                                                          .statusCode(200)
                                                                          .extract()
                                                                          .asString());
        assertNotEquals(second, third);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/" + id)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("ProcessId", equalTo(third))
                                                 .body("ProcessIds", equalTo(PROCESS + "=" + third)),
                ASSERTION_TIMEOUT_SECONDS);
        assertEquals(1, processEngine.getRuntimeService()
                                     .createProcessInstanceQuery()
                                     .processInstanceId(third)
                                     .count(),
                "the restarted instance runs");

        // The two misses an operator can make.
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(RESTART_URL + "?process=NoSuchFlow&id=" + id)
                                                 .then()
                                                 .statusCode(404),
                ASSERTION_TIMEOUT_SECONDS);
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(RESTART_URL + "?process=" + PROCESS + "&id=999999")
                                                 .then()
                                                 .statusCode(404)
                                                 .body("message", equalTo("No Ticket [999999]")),
                ASSERTION_TIMEOUT_SECONDS);
    }

    private void generateAndPublish() {
        writeProjectFile("app.intent", INTENT_YAML);
        AtomicReference<List<Map<String, Object>>> plan = new AtomicReference<>();
        restAssuredExecutor.execute(() -> plan.set(given().when()
                                                          .post(GENERATE_URL)
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

    private int create(String title) {
        return restAssuredExecutor.executeWithResult(() -> given().contentType("application/json")
                                                                  .body("{\"Title\":\"" + title + "\"}")
                                                                  .when()
                                                                  .post(API)
                                                                  .then()
                                                                  .statusCode(200)
                                                                  .extract()
                                                                  .path("Id"));
    }

    private void update(int id, String title) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + id + ",\"Title\":\"" + title + "\"}")
                                                 .when()
                                                 .put(API + "/" + id)
                                                 .then()
                                                 .statusCode(200));
    }

    /** The record's ProcessId once the trigger has written one other than {@code previous}. */
    private String awaitProcessId(int id, String previous) {
        AtomicReference<String> processId = new AtomicReference<>();
        restAssuredExecutor.execute(() -> processId.set(given().when()
                                                               .get(API + "/" + id)
                                                               .then()
                                                               .statusCode(200)
                                                               .body("ProcessId", not(nullValue()))
                                                               .body("ProcessId", not(equalTo(previous)))
                                                               .extract()
                                                               .path("ProcessId")),
                ASSERTION_TIMEOUT_SECONDS);
        return processId.get();
    }

    private String processIdOf(int id) {
        return restAssuredExecutor.executeWithResult(() -> given().when()
                                                                  .get(API + "/" + id)
                                                                  .then()
                                                                  .statusCode(200)
                                                                  .extract()
                                                                  .path("ProcessId"));
    }

    private void writeProjectFile(String fileName, String content) {
        String path = PROJECT_PATH + "/" + fileName;
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(content.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, content.getBytes(StandardCharsets.UTF_8));
        }
    }
}
