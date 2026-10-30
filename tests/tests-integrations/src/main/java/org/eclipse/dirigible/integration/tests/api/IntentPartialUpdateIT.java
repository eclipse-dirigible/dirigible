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

import io.restassured.response.ValidatableResponse;

/**
 * A PUT that leaves a defaulted field out keeps its stored value (dirigible #8105).
 *
 * <p>
 * A generated PUT replaced the whole row, and {@code defaultValue:} / {@code init:} apply on create
 * only - so every client that sent only what it changed, and every form that does not render the
 * field, wrote NULL: a ticket's priority bucket went blank, a departed employee's {@code active}
 * flag turned NULL and let their claims through a {@code forbidWhen}, and a defaulted NOT NULL flag
 * answered the PUT with a 500 quoting the SQL statement.
 *
 * <p>
 * Asserted on the running, generated code, together with the two boundaries the fix must not move:
 * an explicit null still clears an ordinary optional field, and a required field without a default
 * that is left out is a 400 naming it.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentPartialUpdateIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "partialput";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String TICKETS = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api/ticket/TicketController";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String INTENT_YAML = """
            name: partialput
            description: a PUT that leaves a defaulted field out keeps it

            entities:
              - name: Priority
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: Ticket
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: subject,  type: string, required: true, length: 100 }
                  - { name: note,     type: string, length: 200 }
                  - { name: internal, type: boolean, required: true, defaultValue: false }
                  - { name: active,   type: boolean, defaultValue: true }
                relations:
                  - { name: Priority, kind: manyToOne, to: Priority, init: 2 }

            seeds:
              - name: priorities
                entity: Priority
                rows:
                  - { id: 1, name: Low }
                  - { id: 2, name: Normal }
                  - { id: 3, name: High }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_put_that_leaves_a_defaulted_field_out_keeps_the_stored_value() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int ticket = create("{\"Subject\":\"printer\",\"Note\":\"3rd floor\",\"Internal\":true,\"Active\":false,\"Priority\":3}");

        // Only what changed: the defaulted fields are kept - the NOT NULL one is no longer a 500.
        put(ticket, "{\"Subject\":\"printer jam\"}").statusCode(200);
        read(ticket).body("Subject", equalTo("printer jam"))
                    .body("Internal", equalTo(true))
                    .body("Active", equalTo(false))
                    .body("Priority", equalTo(3));

        // A defaulted field the payload does carry is still the caller's to change.
        put(ticket, "{\"Subject\":\"printer jam\",\"Internal\":false,\"Priority\":1}").statusCode(200);
        read(ticket).body("Internal", equalTo(false))
                    .body("Priority", equalTo(1))
                    .body("Active", equalTo(false));

        // An explicit null still clears an ordinary optional field.
        put(ticket, "{\"Subject\":\"printer jam\",\"Note\":null}").statusCode(200);
        read(ticket).body("Note", nullValue());

        // A required field without a default that is left out is a 400 naming it, never a 500.
        put(ticket, "{\"Note\":\"no subject\"}").statusCode(400)
                                                .body(containsString("Subject"));
    }

    private ValidatableResponse put(int ticket, String body) {
        AtomicReference<ValidatableResponse> response = new AtomicReference<>();
        restAssuredExecutor.execute(() -> response.set(given().contentType("application/json")
                                                              .body(body)
                                                              .when()
                                                              .put(TICKETS + "/" + ticket)
                                                              .then()),
                TIMEOUT_SECONDS);
        return response.get();
    }

    private ValidatableResponse read(int ticket) {
        AtomicReference<ValidatableResponse> response = new AtomicReference<>();
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

    private void generateProject() {
        writeResource(PROJECT_PATH + "/app.intent", INTENT_YAML);
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

    private void writeResource(String path, String content) {
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(content.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, content.getBytes(StandardCharsets.UTF_8));
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
