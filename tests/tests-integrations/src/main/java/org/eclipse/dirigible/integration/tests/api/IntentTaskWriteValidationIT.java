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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * A task form's {@code editable:} write-back holds the reviewer's edits to the entity's own rules
 * before the task completes (dirigible #7552).
 *
 * <p>
 * The generated {@code <Process><Step>Write} delegate wrote the edits with no validation, behind an
 * async boundary: a value longer than its column was accepted with 200, then failed in a background
 * job with the task already gone - the record stranded mid-flow and the person told it succeeded.
 * Now the writer validates the edited row with the entity controller's own rules inside the
 * completing transaction, so the refusal is the controller's message as a 400, the task stays in
 * the inbox, and nothing is written.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentTaskWriteValidationIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "taskwrite";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String TICKETS = API + "/ticket/TicketController";
    private static final String TASKS = "/services/inbox/tasks";
    private static final long TIMEOUT_SECONDS = 90;
    /** The task appears once the create event has started the instance. */
    private static final long PROCESS_TIMEOUT_SECONDS = 60;

    private static final String INTENT_YAML = """
            name: taskwrite
            description: a resolve form writes the solution back - held to the entity's own rules

            entities:
              - name: TicketStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: Ticket
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: subject,  type: string, length: 100 }
                  - { name: solution, type: string, length: 20 }
                relations:
                  - { name: Status, kind: manyToOne, to: TicketStatus, function: EntityStatus, init: 1 }

            processes:
              - name: TicketResolution
                trigger: { onCreate: Ticket }
                steps:
                  - { name: resolve, kind: userTask, args: { assignee: agent, form: ResolveTicket } }
                  - { name: close,   kind: serviceTask, args: { setRelationField: Status, value: 2 } }
                  - { name: end,     kind: end }

            forms:
              - { name: ResolveTicket, forEntity: Ticket, fields: [subject, solution], editable: [solution], actions: [resolve] }

            seeds:
              - name: ticket-statuses
                entity: TicketStatus
                rows:
                  - { id: 1, name: Open }
                  - { id: 2, name: Resolved }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void an_over_long_edit_is_refused_before_the_task_completes_and_a_valid_one_is_written() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int ticket = create("{\"Subject\":\"printer jam\"}");
        String task = taskFor(ticket);

        // 23 characters into a 20-character column: the controller's own refusal, before anything
        // completes - so the task is still the ticket's, and the ticket is exactly as it was.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"action\":\"COMPLETE\",\"data\":{\"action\":\"resolve\","
                                                         + "\"Solution\":\"replaced the whole drum\"}}")
                                                 .when()
                                                 .post(TASKS + "/" + task)
                                                 .then()
                                                 .statusCode(400)
                                                 .body(containsString("The 'Solution' exceeds the maximum length of 20")));
        assertEquals(task, taskFor(ticket), "the refused completion must leave the task in the inbox");
        read(ticket).body("Status", equalTo(1))
                    .body("Solution", nullValue());

        // ...and a value that fits completes the task and is written back.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"action\":\"COMPLETE\",\"data\":{\"action\":\"resolve\","
                                                         + "\"Solution\":\"new drum\"}}")
                                                 .when()
                                                 .post(TASKS + "/" + task)
                                                 .then()
                                                 .statusCode(200));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(TICKETS + "/" + ticket)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(2))
                                                 .body("Solution", equalTo("new drum")),
                PROCESS_TIMEOUT_SECONDS);
    }

    private io.restassured.response.ValidatableResponse read(int ticket) {
        AtomicReference<io.restassured.response.ValidatableResponse> response = new AtomicReference<>();
        restAssuredExecutor.execute(() -> response.set(given().when()
                                                              .get(TICKETS + "/" + ticket)
                                                              .then()
                                                              .statusCode(200)));
        return response.get();
    }

    private int create(String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(TICKETS)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                TIMEOUT_SECONDS);
        return id.get();
    }

    /** The resolve task of this ticket's instance, found by the business key the trigger stamped. */
    private String taskFor(int ticket) {
        AtomicReference<String> task = new AtomicReference<>();
        restAssuredExecutor.execute(() -> task.set(given().when()
                                                          .get(TASKS + "?type=groups")
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .path("find { it.processInstanceBusinessKey == '" + ticket + "' }.id")),
                PROCESS_TIMEOUT_SECONDS);
        return task.get();
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
