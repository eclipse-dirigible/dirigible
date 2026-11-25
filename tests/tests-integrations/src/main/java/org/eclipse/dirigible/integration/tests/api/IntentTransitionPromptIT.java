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
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
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
 * A {@code transitions:} button that asks for input (#7909): {@code prompt:} names the record's own
 * fields, the controller writes the answers in the same targeted write as the status move.
 *
 * <p>
 * The case it was filed from: a BG invoice annulment must record a reason, and the invoice is
 * {@code immutableWhen}-locked from the moment it is issued, so the clerk cannot type the reason on
 * the record before pressing Void either. Asserted on the running, generated code: a Void without
 * the required reason is the field's 400 and the status does not move; with it the status moves and
 * the reason and the optional replacing invoice are stored with it, through the lock. On an Issue
 * that also stamps the number, the prompted value reaches the checks gated on the target status
 * before the number is drawn.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentTransitionPromptIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "trprompt";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String INVOICES = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api/invoice/InvoiceController";
    private static final String TRANSITIONS = "/services/java/" + PROJECT + "/gen/events/" + PROJECT + "/";
    private static final String DUE_REFUSAL = "An issued invoice needs a due date";
    private static final String UUID = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String NUMBERS_JSON = """
            {"series": [{"name": "Prompt Invoice", "prefix": "PI", "size": 8}]}
            """;

    private static final String INTENT_YAML = """
            name: trprompt
            description: transition prompt fixture - Void writes a mandatory reason with the status move

            entities:
              - name: InvoiceStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: Invoice
                immutableWhen: "Status == 2 || Status == 3"
                checks:
                  - { kind: requiredWhen, field: dueDate, when: "Status == 2", status: 2, message: "An issued invoice needs a due date" }
                fields:
                  - { name: id,         type: integer, primaryKey: true, generated: true }
                  - { name: note,       type: string, length: 200 }
                  - { name: dueDate,    type: date }
                  - { name: number,     type: string, length: 100, number: { series: Prompt Invoice, stampOn: issue } }
                  - { name: voidReason, type: string, length: 200, label: Reason for voiding, readOnly: true }
                  - { name: voidedOn,   type: date, stampOn: { status: 3 } }
                relations:
                  - { name: Status,     kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
                  - { name: ReplacedBy, kind: manyToOne, to: Invoice, inlineCreate: false, readOnly: true }

            transitions:
              - name: IssueInvoice
                forEntity: Invoice
                from: [1]
                setStatus: 2
                stamp: number
                prompt:
                  - { field: dueDate }
              - name: VoidInvoice
                forEntity: Invoice
                from: [2]
                setStatus: 3
                label: Void
                prompt:
                  - { field: voidReason, required: true }
                  - { field: ReplacedBy }

            seeds:
              - name: invoice-statuses
                entity: InvoiceStatus
                rows:
                  - { id: 1, name: Draft }
                  - { id: 2, name: Issued }
                  - { id: 3, name: Voided }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_prompted_transition_writes_its_answers_with_the_move_and_refuses_one_left_out() {
        writeProjectFile(PROJECT + ".numbers", NUMBERS_JSON);
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int invoice = create("{\"Note\":\"first\"}");
        int replacement = create("{\"Note\":\"replacement\"}");

        // The Issue's gated check reads the prompted value: left out, the move is the check's 400 and
        // spends no number.
        transition("IssueInvoice", "{\"id\":" + invoice + "}").statusCode(400)
                                                              .body(containsString(DUE_REFUSAL));
        read(invoice).body("Status", equalTo(1))
                     .body("Number", matchesPattern(UUID));

        // Answered, it lands with the status and the number in the same write.
        transition("IssueInvoice", "{\"id\":" + invoice + ",\"values\":{\"DueDate\":\"2026-12-31\"}}").statusCode(200)
                                                                                                      .body("Status", equalTo(2))
                                                                                                      .body("DueDate",
                                                                                                              equalTo("2026-12-31"))
                                                                                                      .body("Number",
                                                                                                              matchesPattern("PI0*1"));

        // Issued, the record is locked: the clerk cannot type the reason on it before pressing Void.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + invoice + ",\"Note\":\"first\",\"VoidReason\":\"typed\"}")
                                                 .when()
                                                 .put(INVOICES + "/" + invoice)
                                                 .then()
                                                 .statusCode(409));

        // A Void without the required reason is the field's 400, and the status does not move.
        transition("VoidInvoice", "{\"id\":" + invoice + "}").statusCode(400)
                                                             .body("field", equalTo("VoidReason"))
                                                             .body("message", containsString("Reason for voiding"));
        transition("VoidInvoice", "{\"id\":" + invoice + ",\"values\":{\"VoidReason\":\"  \"}}").statusCode(400)
                                                                                                .body("field", equalTo("VoidReason"));
        read(invoice).body("Status", equalTo(2))
                     .body("VoidReason", nullValue())
                     .body("VoidedOn", nullValue());

        // Answered, the reason and the replacing invoice are written with the move, through the lock.
        // A to-one arrives as the dialog's dropdown sends it, the key as text.
        String answered = "{\"id\":" + invoice + ",\"values\":{\"VoidReason\":\"Wrong customer\",\"ReplacedBy\":\"" + replacement + "\"}}";
        transition("VoidInvoice", answered).statusCode(200)
                                           .body("Status", equalTo(3));
        read(invoice).body("Status", equalTo(3))
                     .body("VoidReason", equalTo("Wrong customer"))
                     .body("ReplacedBy", equalTo(replacement))
                     .body("VoidedOn", notNullValue());
    }

    private io.restassured.response.ValidatableResponse transition(String name, String body) {
        AtomicReference<io.restassured.response.ValidatableResponse> response = new AtomicReference<>();
        restAssuredExecutor.execute(() -> response.set(given().contentType("application/json")
                                                              .body(body)
                                                              .when()
                                                              .post(TRANSITIONS + name + "Transition/run")
                                                              .then()));
        return response.get();
    }

    private io.restassured.response.ValidatableResponse read(int invoice) {
        AtomicReference<io.restassured.response.ValidatableResponse> response = new AtomicReference<>();
        restAssuredExecutor.execute(() -> response.set(given().when()
                                                              .get(INVOICES + "/" + invoice)
                                                              .then()
                                                              .statusCode(200)));
        return response.get();
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
        writeProjectFile("app.intent", INTENT_YAML);
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

    private void writeProjectFile(String relativePath, String content) {
        String path = PROJECT_PATH + "/" + relativePath;
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
