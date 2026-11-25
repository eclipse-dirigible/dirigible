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
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.springframework.test.annotation.DirtiesContext;

import io.restassured.response.ValidatableResponse;

/**
 * A tree record that leafOnly references point at may not gain a child (dirigible #7826): the child
 * would turn the referenced leaf into a group, and the references - a journal line's account -
 * would stay on it, booking a balance on a group account.
 *
 * <p>
 * Asserted on the running, generated code: a create and a re-parent under a referenced leaf are
 * refused with a 400 naming the referencing records, while an unreferenced leaf still gains a
 * child, an edit that keeps a row's parent still saves, and a leafOnly reference to the new group
 * is refused as before.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentLeafReferencedChildIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "leafchild";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String ACCOUNTS = API + "/account/AccountController";
    private static final String LINES = API + "/journalline/JournalLineController";
    private static final String REFUSAL_TAIL = "and cannot be given a child";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String INTENT_YAML = """
            name: leafchild
            description: a chart of accounts whose referenced leaves cannot become groups

            entities:
              - name: Account
                hierarchy: Parent
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }
                relations:
                  - { name: Parent, kind: manyToOne, to: Account }

              - name: JournalLine
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: Account, kind: manyToOne, to: Account, required: true, leafOnly: true }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_leaf_that_leafOnly_references_point_at_cannot_gain_a_child() {
        generateProject();
        assertTrue(contentOf("i18n/en-US/leafchild.model.json").contains("\"leafReferenced\""),
                "the refusal's default text must be in the en-US catalog");
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int booked = create(ACCOUNTS, "{\"Name\":\"4999901\"}");
        int unbooked = create(ACCOUNTS, "{\"Name\":\"4999902\"}");
        int loose = create(ACCOUNTS, "{\"Name\":\"4999903\"}");
        create(LINES, "{\"Amount\":100,\"Account\":" + booked + "}");

        // A child created under the referenced leaf is refused, naming the references...
        post(ACCOUNTS, "{\"Name\":\"49999011\",\"Parent\":" + booked + "}").statusCode(400)
                                                                           .body("message", containsString("referenced as a leaf by 1"))
                                                                           .body("message", containsString(REFUSAL_TAIL));
        // ...and so is a re-parent under it.
        put(ACCOUNTS, loose, "{\"Name\":\"4999903\",\"Parent\":" + booked + "}").statusCode(400)
                                                                                .body("message", containsString(REFUSAL_TAIL));
        read(ACCOUNTS, loose).body("Parent", equalTo(null));

        // An unreferenced leaf still gains a child, by create and by re-parent.
        int child = create(ACCOUNTS, "{\"Name\":\"49999021\",\"Parent\":" + unbooked + "}");
        put(ACCOUNTS, loose, "{\"Name\":\"4999903\",\"Parent\":" + unbooked + "}").statusCode(200)
                                                                                  .body("Parent", equalTo(unbooked));
        // An edit that keeps the row's parent is not a new child.
        put(ACCOUNTS, child, "{\"Name\":\"49999021 renamed\",\"Parent\":" + unbooked + "}").statusCode(200);
        // The group it became takes no leafOnly reference, as before.
        post(LINES, "{\"Amount\":5,\"Account\":" + unbooked + "}").statusCode(400)
                                                                  .body("message", containsString("must reference a leaf Account"));
    }

    private ValidatableResponse post(String controller, String body) {
        AtomicReference<ValidatableResponse> response = new AtomicReference<>();
        restAssuredExecutor.execute(() -> response.set(given().contentType("application/json")
                                                              .body(body)
                                                              .when()
                                                              .post(controller)
                                                              .then()),
                TIMEOUT_SECONDS);
        return response.get();
    }

    private String contentOf(String fileName) {
        return new String(repository.getResource(PROJECT_PATH + "/" + fileName)
                                    .getContent(),
                StandardCharsets.UTF_8);
    }

    private ValidatableResponse put(String controller, int id, String body) {
        AtomicReference<ValidatableResponse> response = new AtomicReference<>();
        restAssuredExecutor.execute(() -> response.set(given().contentType("application/json")
                                                              .body(body)
                                                              .when()
                                                              .put(controller + "/" + id)
                                                              .then()),
                TIMEOUT_SECONDS);
        return response.get();
    }

    private ValidatableResponse read(String controller, int id) {
        AtomicReference<ValidatableResponse> response = new AtomicReference<>();
        restAssuredExecutor.execute(() -> response.set(given().when()
                                                              .get(controller + "/" + id)
                                                              .then()
                                                              .statusCode(200)));
        return response.get();
    }

    private int create(String controller, String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(controller)
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
