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
 * A {@code checks:} refusal is answered to the person who pressed the button - as the 400 their
 * task completion returns, on every route that reaches the gate.
 *
 * <p>
 * #7063 was the shape every approve/reject flow has: the user task falls through a decision into
 * the service task that writes the gated status, and any async boundary on the way commits the
 * completion before the gate is reached. The refusal then failed a detached job - the approver saw
 * the Approve button vanish, no message, the document still DRAFT, and only an administrator could
 * retry the parked incident from Monitoring. The fix takes the whole path out of async; it was
 * pinned by assertions over the emitted BPMN's {@code flowable:async} attributes (issue #7162),
 * which is a statement about the XML and not about where the message goes.
 *
 * <p>
 * The gate here is reached by TWO routes - one hop on the approve arm, two on the arm that has to
 * resolve the customer's rating first. The second route is the one the shared visited-set defect of
 * #7139 left asynchronous: the walk back from the gate had already marked the resolver seen. An
 * approver taking it is waiting on the gate just as much, so both are asserted, and the ungated
 * reject branch of the same decision is the control that keeps this from passing on a build that
 * had simply stopped making anything asynchronous.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentCheckGateRefusalIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "gate";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String INVOICES = API + "/invoice/InvoiceController";
    private static final String ITEMS = API + "/invoice/InvoiceItemController";
    private static final String CUSTOMERS = API + "/customer/CustomerController";
    private static final String TASKS = "/services/inbox/tasks";
    private static final String REFUSAL = "Invoice needs at least one line before it can be approved";
    private static final long TIMEOUT_SECONDS = 90;
    /** The task appears once the create event has started the instance. */
    private static final long PROCESS_TIMEOUT_SECONDS = 60;

    private static final String INTENT_YAML = """
            name: gate
            description: check-gate fixture - the refusal travels back to whoever completed the task

            entities:
              - name: InvoiceStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: Customer
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: name,   type: string, length: 100 }
                  - { name: rating, type: integer }

              - name: Invoice
                checks:
                  - { kind: itemsMin, count: 1, status: 2, message: "Invoice needs at least one line before it can be approved" }
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 200 }
                relations:
                  - { name: Status,   kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
                  - { name: Customer, kind: manyToOne, to: Customer }

              - name: InvoiceItem
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: quantity, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Invoice, kind: manyToOne, to: Invoice, composition: true, required: true }

            processes:
              # The gate (activate) is one hop from the task on the approve arm and two on the
              # other, behind the resolver the rating decision needs. Both are somebody's action.
              - name: InvoiceApproval
                trigger: { onCreate: Invoice }
                steps:
                  - { name: review,   kind: userTask, args: { assignee: approver, form: DecideInvoice } }
                  - { name: decide,   kind: decision, args: { if: "action == 'approve'", then: activate, else: rated } }
                  - { name: rated,    kind: decision, args: { if: "Customer.rating > 0", then: activate, else: reject } }
                  - { name: activate, kind: serviceTask, args: { setRelationField: Status, value: 2, next: done } }
                  - { name: reject,   kind: serviceTask, args: { setRelationField: Status, value: 8, next: done } }
                  - { name: done,     kind: end }

            forms:
              - { name: DecideInvoice, forEntity: Invoice, fields: [note], editable: [note], actions: [approve, reject] }

            permissions:
              - { role: Approver, description: Approver, can: [Invoice:read] }

            seeds:
              - name: invoice-statuses
                entity: InvoiceStatus
                rows:
                  - { id: 1, name: Draft }
                  - { id: 2, name: Approved }
                  - { id: 8, name: Rejected }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_refused_document_check_answers_the_approver_on_every_route_to_the_gate() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int rated = create(CUSTOMERS, "{\"Name\":\"rated\",\"Rating\":5}");
        int unrated = create(CUSTOMERS, "{\"Name\":\"unrated\",\"Rating\":0}");

        // Route 1 - the approve arm, one hop from the task to the gate. Its customer is deliberately the
        // UNRATED one, so this route is the only way to the gate: were the action the approver chose not
        // reaching the decision, the flow would fall to the rating arm, find 0 and end at Rejected.
        int direct = create(INVOICES, "{\"Note\":\"direct\",\"Customer\":" + unrated + "}");
        assertCompletionRefused(direct, "approve");
        assertApprovedAfterALineIsAdded(direct, "approve");

        // Route 2 - the arm that converges on the same gate through the rating decision, whose
        // resolver is the node the shared visited set left asynchronous.
        int converging = create(INVOICES, "{\"Note\":\"converging\",\"Customer\":" + rated + "}");
        assertCompletionRefused(converging, "reject");
        assertApprovedAfterALineIsAdded(converging, "reject");

        // The control: the same decision's UNGATED arm. Nothing stands in front of that status write,
        // so it keeps its async boundary and the completion succeeds with no line at all.
        int ungated = create(INVOICES, "{\"Note\":\"ungated\",\"Customer\":" + unrated + "}");
        complete(taskFor(ungated), "reject", 200);
        awaitStatus(ungated, 8);
    }

    /**
     * The refusal, at the outermost layer: the completion answers 400 with the authored message, the
     * document is untouched, and the task is still the approver's to retry - not a dead-letter job an
     * administrator has to find in Monitoring.
     */
    private void assertCompletionRefused(int invoice, String action) {
        String task = taskFor(invoice);
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"action\":\"COMPLETE\",\"data\":{\"action\":\"" + action + "\"}}")
                                                 .when()
                                                 .post(TASKS + "/" + task)
                                                 .then()
                                                 .statusCode(400)
                                                 .body(containsString(REFUSAL)));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(INVOICES + "/" + invoice)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(1)));
        assertEquals(task, taskFor(invoice), "the refused completion rolled back, so the task is still the approver's");
    }

    /** ...and once the document says what the gate asks, the very same completion goes through. */
    private void assertApprovedAfterALineIsAdded(int invoice, String action) {
        create(ITEMS, "{\"Invoice\":" + invoice + ",\"Quantity\":1}");
        complete(taskFor(invoice), action, 200);
        awaitStatus(invoice, 2);
    }

    private void complete(String task, String action, int expectedStatus) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"action\":\"COMPLETE\",\"data\":{\"action\":\"" + action + "\"}}")
                                                 .when()
                                                 .post(TASKS + "/" + task)
                                                 .then()
                                                 .statusCode(expectedStatus));
    }

    private void awaitStatus(int invoice, int status) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(INVOICES + "/" + invoice)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(status)),
                PROCESS_TIMEOUT_SECONDS);
    }

    /** The review task of one invoice's instance, found by the business key the trigger stamped. */
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
