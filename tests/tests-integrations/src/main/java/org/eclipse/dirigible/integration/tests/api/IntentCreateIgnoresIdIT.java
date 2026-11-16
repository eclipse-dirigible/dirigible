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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
 * A create whose body carries the primary key (dirigible #8103) - a re-imported export, a client
 * echoing the row it read - is created under a new key on the power, the personal and the partner
 * surface, and the row whose key it carried is left as it was.
 *
 * <p>
 * The key was handed to the save as it came, and under the IDENTITY strategy Hibernate read the row
 * as a detached entity: every such create answered 500 "Detached entity passed to persist", whether
 * the key existed or not. Asserted on the running, generated code, with a key that names a stored
 * row and one that names none.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentCreateIgnoresIdIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "createid";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String TICKETS = API + "/ticket/TicketController";
    private static final String MY_TICKETS = API + "/ticket/TicketMyController";
    private static final String EXPENSES = API + "/expense/ExpenseController";
    private static final String PARTNER_EXPENSES = API + "/expense/ExpensePartnerController";
    /** A key no row holds. */
    private static final int FREE_ID = 999999;
    private static final long TIMEOUT_SECONDS = 90;

    private static final String INTENT_YAML = """
            name: createid
            description: a create that carries a primary key is created under a new one

            entities:
              - name: Employee
                identity: email
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: name,  type: string, required: true, length: 100 }
                  - { name: email, type: string, required: true, length: 100 }

              # The power and the personal surface.
              - name: Ticket
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: subject, type: string, required: true, length: 100 }
                relations:
                  - { name: Employee, kind: manyToOne, to: Employee, required: true, personal: true }

              # The partner surface.
              - name: Expense
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, required: true, length: 100 }
                relations:
                  - { name: Employee, kind: manyToOne, to: Employee, required: true, partner: true }

            seeds:
              - name: employees
                entity: Employee
                rows:
                  - { id: 1, name: Admin, email: admin }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_create_carrying_a_primary_key_is_created_under_a_new_one_on_every_surface() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int ticket = create(TICKETS, "{\"Subject\":\"stored\",\"Employee\":1}");
        assertCreatedUnderANewKey(TICKETS, ticket, "{\"Id\":" + ticket + ",\"Subject\":\"echoed\",\"Employee\":1}",
                "{\"Id\":" + FREE_ID + ",\"Subject\":\"imported\",\"Employee\":1}", "Subject", "stored");
        assertCreatedUnderANewKey(MY_TICKETS, ticket, "{\"Id\":" + ticket + ",\"Subject\":\"mine, echoed\"}",
                "{\"Id\":" + FREE_ID + ",\"Subject\":\"mine, imported\"}", "Subject", "stored");

        int expense = create(EXPENSES, "{\"Note\":\"stored\",\"Employee\":1}");
        assertCreatedUnderANewKey(PARTNER_EXPENSES, expense, "{\"Id\":" + expense + ",\"Note\":\"partner, echoed\"}",
                "{\"Id\":" + FREE_ID + ",\"Note\":\"partner, imported\"}", "Note", "stored");
    }

    /**
     * Posts a body carrying a stored row's key and one carrying a free key: both are created under a
     * new key, the stored row keeps its value, and the free key names no row afterwards.
     */
    private void assertCreatedUnderANewKey(String controller, int storedId, String withStoredId, String withFreeId, String property,
            String storedValue) {
        int echoed = create(controller, withStoredId);
        assertNotEquals(storedId, echoed, controller + " must create the echoed row under a new key");
        read(controller, storedId).body(property, equalTo(storedValue));

        int imported = create(controller, withFreeId);
        assertNotEquals(FREE_ID, imported, controller + " must not take the key the request carried");
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(controller + "/" + FREE_ID)
                                                 .then()
                                                 .statusCode(404));
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

    private ValidatableResponse read(String controller, int id) {
        AtomicReference<ValidatableResponse> response = new AtomicReference<>();
        restAssuredExecutor.execute(() -> response.set(given().when()
                                                              .get(controller + "/" + id)
                                                              .then()
                                                              .statusCode(200)));
        return response.get();
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
