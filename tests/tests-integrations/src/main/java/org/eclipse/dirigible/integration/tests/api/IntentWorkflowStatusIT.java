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
 * A status a {@code processes:} flow drives cannot be moved by a direct REST write (dirigible
 * #7339).
 *
 * <p>
 * The generated controllers treated that column as an ordinary writable property, so a plain
 * {@code PUT} carrying {@code "Status": 2} approved a document with the whole flow bypassed - no
 * check ran, no task was ever raised, nothing the flow charges was charged, and the record read
 * approved. {@code immutableWhen:} cannot close it: it locks the way OUT of a final status, while
 * this is the way IN, from a status that is mutable by definition.
 *
 * <p>
 * So the four answers are asserted together, because each of them is a way the guard could be
 * wrong: the jump is refused, the flow's own write of the very same column still lands, an ordinary
 * edit that carries the status back unchanged still saves, and an edit that omits it does not erase
 * it.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentWorkflowStatusIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "flowstatus";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String INVOICES = API + "/invoice/InvoiceController";
    private static final String TASKS = "/services/inbox/tasks";
    private static final String REFUSAL = "'Status' changes through the workflow, not a direct edit";
    private static final long TIMEOUT_SECONDS = 90;
    /** The task appears once the create event has started the instance. */
    private static final long PROCESS_TIMEOUT_SECONDS = 60;

    private static final String INTENT_YAML = """
            name: flowstatus
            description: a status the approval flow owns - the plain create/update cannot move it

            entities:
              - name: InvoiceStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: Invoice
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 200 }
                relations:
                  - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }

            processes:
              - name: InvoiceApproval
                trigger: { onCreate: Invoice }
                steps:
                  - { name: review,  kind: userTask, args: { assignee: approver, form: DecideInvoice } }
                  - { name: approve, kind: serviceTask, args: { setRelationField: Status, value: 2 } }
                  - { name: end,     kind: end }

            forms:
              - { name: DecideInvoice, forEntity: Invoice, fields: [note], editable: [note], actions: [approve] }

            seeds:
              - name: invoice-statuses
                entity: InvoiceStatus
                rows:
                  - { id: 1, name: Draft }
                  - { id: 2, name: Approved }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_flow_driven_status_is_refused_to_a_direct_rest_write_and_still_written_by_the_flow() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int invoice = create("{\"Note\":\"draft\"}");
        awaitStatus(invoice, 1);

        // The jump the whole flow exists to prevent.
        put(invoice, "{\"Note\":\"draft\",\"Status\":2}", 409, REFUSAL);
        // ...and a create cannot start mid-lifecycle either: the record starts where `init:` says.
        createRefused("{\"Note\":\"born approved\",\"Status\":2}");

        // An ordinary edit still saves - it carries the status back unchanged...
        put(invoice, "{\"Note\":\"edited\",\"Status\":1}", 200, null);
        // ...and one that does not mention the status does not erase it either.
        put(invoice, "{\"Note\":\"edited again\"}", 200, null);
        read(invoice).body("Status", equalTo(1))
                     .body("Note", equalTo("edited again"));

        // The flow's own writer is untouched: it reaches the repository through the targeted
        // updateProperties primitive, never through the controller this guard sits in.
        complete(taskFor(invoice));
        awaitStatus(invoice, 2);
    }

    private void put(int invoice, String body, int expectedStatus, String expectedMessage) {
        restAssuredExecutor.execute(() -> {
            var response = given().contentType("application/json")
                                  .body(body)
                                  .when()
                                  .put(INVOICES + "/" + invoice)
                                  .then()
                                  .statusCode(expectedStatus);
            if (expectedMessage != null) {
                response.body(containsString(expectedMessage));
            }
        });
    }

    private void createRefused(String body) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(body)
                                                 .when()
                                                 .post(INVOICES)
                                                 .then()
                                                 .statusCode(409)
                                                 .body(containsString(REFUSAL)));
    }

    private io.restassured.response.ValidatableResponse read(int invoice) {
        AtomicReference<io.restassured.response.ValidatableResponse> response = new AtomicReference<>();
        restAssuredExecutor.execute(() -> response.set(given().when()
                                                              .get(INVOICES + "/" + invoice)
                                                              .then()
                                                              .statusCode(200)));
        return response.get();
    }

    private void awaitStatus(int invoice, int status) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(INVOICES + "/" + invoice)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(status)),
                PROCESS_TIMEOUT_SECONDS);
    }

    private void complete(String task) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"action\":\"COMPLETE\",\"data\":{\"action\":\"approve\"}}")
                                                 .when()
                                                 .post(TASKS + "/" + task)
                                                 .then()
                                                 .statusCode(200));
    }

    /** The review task of this invoice's instance, found by the business key the trigger stamped. */
    private String taskFor(int invoice) {
        AtomicReference<String> task = new AtomicReference<>();
        restAssuredExecutor.execute(() -> task.set(given().when()
                                                          .get(TASKS + "?type=groups")
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .path("find { it.processInstanceBusinessKey == '" + invoice + "' }.id")),
                PROCESS_TIMEOUT_SECONDS);
        return task.get();
    }

    private int create(String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(INVOICES)
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
