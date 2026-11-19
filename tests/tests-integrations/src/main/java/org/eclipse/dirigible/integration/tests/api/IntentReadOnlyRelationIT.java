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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import io.restassured.response.ValidatableResponse;

/**
 * A to-one only the system sets (intent {@code readOnly: true} on a relation, dirigible #8662).
 *
 * <p>
 * The case: a cash order's storno link names the order a reversal reverses. Only a posting delegate
 * writes it, yet the generated form offered it as an editable picker on New and on Edit. Asserted
 * on the running, generated code: the forms render no picker for it (a set value is shown in the
 * read-only details), a user create that carries it is stored without it, a user update cannot move
 * it, and a system write - a workflow step - still sets it.
 */
class IntentReadOnlyRelationIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "rorel";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String ORDERS = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api/cashorder/CashOrderController";
    private static final long TIMEOUT_SECONDS = 90;
    private static final long PROCESS_TIMEOUT_SECONDS = 60;

    private static final String INTENT_YAML = """
            name: rorel
            description: a cash order's storno link is set by the system only

            entities:
              - name: CashOrder
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 200 }
                relations:
                  - { name: StornoOf, kind: manyToOne, to: CashOrder, inlineCreate: false, readOnly: true }

            processes:
              # The system writer: a workflow step links every new order to the first one.
              - name: Link
                trigger: { onCreate: CashOrder }
                steps:
                  - { name: link, kind: serviceTask, args: { setRelationField: StornoOf, value: 1 } }
                  - { name: end,  kind: end }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_read_only_relation_is_set_by_the_system_and_never_by_a_user() {
        generateProject();
        assertGeneratedForm();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int first = create("{\"Note\":\"first\"}");
        assertEquals(1, first, "the step links to order 1");
        // The system write lands.
        awaitStornoOf(first, 1);

        // A user create that carries the link is stored without it.
        AtomicReference<ValidatableResponse> created = new AtomicReference<>();
        restAssuredExecutor.execute(() -> created.set(given().contentType("application/json")
                                                             .body("{\"Note\":\"second\",\"StornoOf\":1}")
                                                             .when()
                                                             .post(ORDERS)
                                                             .then()
                                                             .statusCode(200)
                                                             .body("StornoOf", nullValue())),
                TIMEOUT_SECONDS);
        int second = created.get()
                            .extract()
                            .path("Id");
        awaitStornoOf(second, 1);

        // A user update cannot move it, and one that omits it keeps it.
        put(first, "{\"Note\":\"moved\",\"StornoOf\":" + second + "}");
        read(first).body("StornoOf", equalTo(1))
                   .body("Note", equalTo("moved"));
        put(first, "{\"Note\":\"edited\"}");
        read(first).body("StornoOf", equalTo(1))
                   .body("Note", equalTo("edited"));
    }

    /** No picker on New or Edit; a set value reads as the target's label in the read-only details. */
    private void assertGeneratedForm() {
        String form = contentOf("gen/" + PROJECT + "/views/CashOrder/CashOrder-form.html");
        assertFalse(form.contains("id=\"f_StornoOf\""), form);
        assertFalse(form.contains("x-model=\"form.StornoOf\""), form);
        assertTrue(form.contains("fieldText(loadedRecord?.StornoOf, 'DROPDOWN', '', optionsStornoOf)"), form);
    }

    private void awaitStornoOf(int order, int stornoOf) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ORDERS + "/" + order)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("StornoOf", equalTo(stornoOf)),
                PROCESS_TIMEOUT_SECONDS);
    }

    private void put(int id, String body) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(body)
                                                 .when()
                                                 .put(ORDERS + "/" + id)
                                                 .then()
                                                 .statusCode(200),
                TIMEOUT_SECONDS);
    }

    private ValidatableResponse read(int id) {
        AtomicReference<ValidatableResponse> response = new AtomicReference<>();
        restAssuredExecutor.execute(() -> response.set(given().when()
                                                              .get(ORDERS + "/" + id)
                                                              .then()
                                                              .statusCode(200)));
        return response.get();
    }

    private int create(String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(ORDERS)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                TIMEOUT_SECONDS);
        return id.get();
    }

    private String contentOf(String fileName) {
        return new String(repository.getResource(PROJECT_PATH + "/" + fileName)
                                    .getContent(),
                StandardCharsets.UTF_8);
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
