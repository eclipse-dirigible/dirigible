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
 *
 * <p>
 * The flow shares the column with a roll-up (dirigible #7869): once payments settle the invoice,
 * Paid is the roll-up's, and an approval completed after the money arrived used to write Approved
 * over it for good - nothing put Paid back, since the roll-up moves the status only when its sum
 * moves. The flow's status now waits in the column the roll-up restores from, and comes back when
 * the money goes.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentWorkflowStatusIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "flowstatus";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String INVOICES = API + "/invoice/InvoiceController";
    private static final String PAYMENTS = API + "/invoice/InvoicePaymentController";
    private static final String TASKS = "/services/inbox/tasks";
    private static final String REFUSAL = "'Status' changes through the workflow, not a direct edit";
    private static final String APPROVER_REQUIRED = "An approved invoice names its approver";
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
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: note,     type: string, length: 200 }
                  - { name: approver, type: string, length: 100 }
                  - { name: total,    type: decimal, precision: 18, scale: 2 }
                  - { name: paid,     type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
                checks:
                  # UNGATED on purpose (#7595): the condition names the status the approve step writes,
                  # so the rule is gated on it and enforced where the step writes - the repository -
                  # and the step runs in the completing transaction so the refusal reaches the approver.
                  - { kind: requiredWhen, field: approver, when: "Status == Approved", message: "An approved invoice names its approver" }

              - name: InvoicePayment
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2, required: true }
                relations:
                  - { name: Invoice, kind: manyToOne, to: Invoice, composition: true, required: true }

            rollups:
              # The money settles the invoice: Partial and Paid are the roll-up's while its sum holds them.
              - { name: invoicePaid, entity: InvoicePayment, via: Invoice, field: paid, op: sum, of: amount,
                  capacity: total, status: Status, statusWhenFull: 4, statusWhenPartial: 3 }

            processes:
              - name: InvoiceApproval
                trigger: { onCreate: Invoice }
                steps:
                  - { name: review,  kind: userTask, args: { assignee: approver, form: DecideInvoice } }
                  - { name: approve, kind: serviceTask, args: { setRelationField: Status, value: 2 } }
                  - { name: end,     kind: end }

            forms:
              - { name: DecideInvoice, forEntity: Invoice, fields: [note, approver], editable: [note, approver], actions: [approve] }

            seeds:
              - name: invoice-statuses
                entity: InvoiceStatus
                rows:
                  - { id: 1, name: Draft }
                  - { id: 2, name: Approved }
                  - { id: 3, name: Partial }
                  - { id: 4, name: Paid }
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

        // The review task completes only with an action its form offers (#7551): a decision after it
        // branches on that value, so a missing one (a 500 from the gateway's expression) and one the
        // form does not list (whatever branch is left over) are both refused before anything
        // completes - the task stays in the inbox and the record where it was.
        String task = taskFor(invoice);
        completeRefused(task, "{}", "one of: approve");
        completeRefused(task, "{\"action\":\"reject\"}", "The action is not one this task offers - use one of: approve");
        read(invoice).body("Status", equalTo(1));

        // A requiredWhen on the status the flow writes runs where the flow writes it (#7595): approving
        // without an approver is refused AT task completion - a 400 the approver sees, the task still
        // in the inbox and the record still a draft - instead of a controller-only rule the step skips.
        completeRefused(task, "{\"action\":\"approve\"}", APPROVER_REQUIRED);
        read(invoice).body("Status", equalTo(1));

        // The flow's own writer is untouched: it reaches the repository through the targeted
        // updateProperties primitive, never through the controller this guard sits in. The approver
        // the reviewer types is written in the same transaction, so the gate is satisfied.
        complete(task, "{\"action\":\"approve\",\"Approver\":\"Ann\"}");
        awaitStatus(invoice, 2);
        read(invoice).body("Approver", equalTo("Ann"));
    }

    @Test
    void a_flow_status_written_while_the_roll_up_holds_the_status_waits_under_it_until_the_money_goes() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        // Paid in full before anyone approves - the normal order for a customer paying by transfer.
        int invoice = create("{\"Note\":\"prepaid\",\"Total\":100}");
        int payment = create(PAYMENTS, "{\"Invoice\":" + invoice + ",\"Amount\":100}");
        awaitStatus(invoice, 4, 1);

        // The approval completes after the money arrived: Paid stays, and Approved is recorded as what
        // the invoice holds once it is no longer settled. Before #7869 this read Approved with 100 paid.
        complete(taskFor(invoice), "{\"action\":\"approve\",\"Approver\":\"Ann\"}");
        awaitStatus(invoice, 4, 2);
        read(invoice).body("Paid", equalTo(100.0F));

        // The payment goes: the roll-up lets go and restores the approval, not the Draft it found.
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete(PAYMENTS + "/" + payment)
                                                 .then()
                                                 .statusCode(200));
        awaitStatus(invoice, 2, null);
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

    private void awaitStatus(int invoice, int status, Integer displaced) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(INVOICES + "/" + invoice)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(status))
                                                 .body("DisplacedStatus", displaced == null ? nullValue() : equalTo(displaced)),
                PROCESS_TIMEOUT_SECONDS);
    }

    private void complete(String task, String data) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"action\":\"COMPLETE\",\"data\":" + data + "}")
                                                 .when()
                                                 .post(TASKS + "/" + task)
                                                 .then()
                                                 .statusCode(200));
    }

    private void completeRefused(String task, String data, String expectedMessage) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"action\":\"COMPLETE\",\"data\":" + data + "}")
                                                 .when()
                                                 .post(TASKS + "/" + task)
                                                 .then()
                                                 .statusCode(400)
                                                 .body(containsString(expectedMessage)));
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
        return create(INVOICES, body);
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
