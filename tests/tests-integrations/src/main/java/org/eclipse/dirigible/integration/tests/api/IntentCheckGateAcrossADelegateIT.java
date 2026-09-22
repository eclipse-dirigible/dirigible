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
 * A {@code checks:} refusal reaches the person who pressed the button even when the flow does real
 * work on the way to the gate - a custom {@code delegate:} step between the decision and the status
 * setter (issue #7371).
 *
 * <p>
 * This is the one shape #7063 left behind. The setter itself was already emitted synchronously, and
 * so were the decisions in front of it, but the walk back from the gate stopped at an authored
 * service task - the reasoning being that its own completion is what the person waited for. It is
 * not: the boundary commits the user-task completion, the setter then runs as a detached job, and
 * the gate refuses THAT. Measured on {@code base-inventory}'s six posting flows, the poster got
 * {@code 200} with an empty body, the task consumed, the document still DRAFT, and - once the job's
 * retries were exhausted - an instance stranded with no task in anyone's Inbox and no way back
 * short of an administrator retriggering the dead letter.
 *
 * <p>
 * The fixture is that flow, reduced: {@code post} (user task) -> {@code postDecision} ->
 * {@code apply} (a client {@link org.flowable.engine.delegate.JavaDelegate} bound with
 * {@code delegate:}, the boundary that used to commit) -> {@code activate} (the gated status
 * write). The assertions are the outermost ones - the completion's own response, the stored status,
 * and whether the task is still the poster's - so they say where the refusal WENT, not what the
 * emitted XML looks like. The ungated {@code cancel} arm of the same decision is the control.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentCheckGateAcrossADelegateIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "posting";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String RECEIPTS = API + "/goodsreceipt/GoodsReceiptController";
    private static final String LINES = API + "/goodsreceipt/GoodsReceiptLineController";
    private static final String TASKS = "/services/inbox/tasks";
    private static final String REFUSAL = "Goods receipt needs at least one line before it can be posted";
    private static final String DELEGATE_REFUSAL = "Negative stock blocked: the store holds less than this receipt returns";
    private static final long TIMEOUT_SECONDS = 90;
    /** The task appears once the create event has started the instance. */
    private static final long PROCESS_TIMEOUT_SECONDS = 60;

    private static final String INTENT_YAML = """
            name: posting
            description: check-gate fixture - the refusal travels back across a custom delegate step

            entities:
              - name: ReceiptStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: GoodsReceipt
                checks:
                  - { kind: itemsMin, count: 1, status: 2, message: "Goods receipt needs at least one line before it can be posted" }
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 200 }
                relations:
                  - { name: Status, kind: manyToOne, to: ReceiptStatus, function: EntityStatus, init: 1 }

              - name: GoodsReceiptLine
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: quantity, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: GoodsReceipt, kind: manyToOne, to: GoodsReceipt, composition: true, required: true }

            processes:
              # base-inventory's posting shape: the work the posting does (stock movements there, a
              # process variable here) sits between the decision and the gated status write.
              - name: GoodsReceiptPosting
                trigger: { onCreate: GoodsReceipt }
                steps:
                  - { name: post,         kind: userTask, args: { assignee: approver, form: PostGoodsReceipt } }
                  - { name: postDecision, kind: decision, args: { if: "action == 'post'", then: apply, else: cancel } }
                  - name: apply
                    kind: serviceTask
                    args:
                      delegate: custom.GoodsReceiptPostDelegate
                      next: activate
                  - { name: activate, kind: serviceTask, args: { setRelationField: Status, value: 2, next: done } }
                  - { name: cancel,   kind: serviceTask, args: { setRelationField: Status, value: 3, next: done } }
                  - { name: done,     kind: end }

            forms:
              - { name: PostGoodsReceipt, forEntity: GoodsReceipt, fields: [note], editable: [note], actions: [post, cancel] }

            permissions:
              - { role: Approver, description: Approver, can: [GoodsReceipt:read] }

            seeds:
              - name: receipt-statuses
                entity: ReceiptStatus
                rows:
                  - { id: 1, name: Draft }
                  - { id: 2, name: Posted }
                  - { id: 3, name: Cancelled }
            """;

    /**
     * The posting work: a client delegate bound with {@code delegate:} (so it is emitted on the
     * {@code flowable:class} path, the one that used to hard-code {@code flowable:async}). It normally
     * just succeeds - what is measured is where the refusal of the step AFTER it lands - and on the
     * {@code refuse} variable it refuses itself, once as the SDK's {@code ValidationException} and once
     * as a plain {@code IllegalStateException}, which is what the delegate's own status code is decided
     * by (issue #7450).
     */
    private static final String POST_DELEGATE_JAVA = """
            package custom;

            import org.eclipse.dirigible.sdk.db.ValidationException;
            import org.flowable.engine.delegate.DelegateExecution;
            import org.flowable.engine.delegate.JavaDelegate;

            public class GoodsReceiptPostDelegate implements JavaDelegate {

                @Override
                public void execute(DelegateExecution execution) {
                    Object refuse = execution.getVariable("refuse");
                    if ("validation".equals(refuse)) {
                        throw new ValidationException("%s");
                    }
                    if ("plain".equals(refuse)) {
                        throw new IllegalStateException("%s");
                    }
                    execution.setVariable("posted", Boolean.TRUE);
                }
            }
            """.formatted(DELEGATE_REFUSAL, DELEGATE_REFUSAL);

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_refused_document_check_answers_the_poster_across_the_delegate_that_posts() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int empty = create(RECEIPTS, "{\"Note\":\"empty\"}");
        assertCompletionRefused(empty);
        assertPostedAfterALineIsAdded(empty);

        // The control: the same decision's UNGATED arm. Nothing stands in front of that status write,
        // so it keeps its async boundary and the completion succeeds with no line at all.
        int cancelled = create(RECEIPTS, "{\"Note\":\"cancelled\"}");
        complete(taskFor(cancelled), "cancel", 200);
        awaitStatus(cancelled, 3);
    }

    /**
     * The refusal, at the outermost layer: the completion answers 400 with the authored message, the
     * document is untouched, and the task is still the poster's to retry - not a task consumed by a 200
     * with an empty body while the refusal fails a detached job.
     */
    private void assertCompletionRefused(int receipt) {
        String task = taskFor(receipt);
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"action\":\"COMPLETE\",\"data\":{\"action\":\"post\"}}")
                                                 .when()
                                                 .post(TASKS + "/" + task)
                                                 .then()
                                                 .statusCode(400)
                                                 .body(containsString(REFUSAL)));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(RECEIPTS + "/" + receipt)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(1)));
        assertEquals(task, taskFor(receipt), "the refused completion rolled back, so the task is still the poster's");
    }

    /**
     * The delegate's OWN refusal, on the same synchronous stretch: what reaches the poster is decided
     * by the exception TYPE, not by where the platform puts the async boundary (issue #7450). A
     * business rule the person can act on is a {@code ValidationException} and comes back as 400 with
     * the authored message - the same status the rule would have had as a {@code checks:} gate. The
     * same sentence thrown as a plain {@code IllegalStateException} is a server fault and stays a 500:
     * the platform cannot tell a refusal from a breakage except by the type the author chose.
     */
    @Test
    void a_delegates_own_refusal_is_a_client_error_only_when_it_is_authored_as_one() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int receipt = create(RECEIPTS, "{\"Note\":\"delegate refuses\"}");
        create(LINES, "{\"GoodsReceipt\":" + receipt + ",\"Quantity\":1}");

        String task = taskFor(receipt);
        completeRefusing(task, "validation", 400, true);
        assertStillTheirs(receipt, task);

        completeRefusing(task, "plain", 500, false);
        assertStillTheirs(receipt, task);

        // Neither refusal consumed anything: the poster's next attempt, with nothing to refuse, posts.
        complete(taskFor(receipt), "post", 200);
        awaitStatus(receipt, 2);
    }

    /**
     * Complete the post task with the delegate armed to refuse, asserting the status the refusal comes
     * back with - and, for the authored client error, that the delegate's own sentence is the body the
     * task form shows. The server fault's body is Spring's error payload, deliberately not asserted:
     * what it must not be is a 400.
     */
    private void completeRefusing(String task, String refusal, int expectedStatus, boolean carriesTheMessage) {
        restAssuredExecutor.execute(() -> {
            var response = given().contentType("application/json")
                                  .body("{\"action\":\"COMPLETE\",\"data\":{\"action\":\"post\",\"refuse\":\"" + refusal + "\"}}")
                                  .when()
                                  .post(TASKS + "/" + task)
                                  .then()
                                  .statusCode(expectedStatus);
            if (carriesTheMessage) {
                response.body(containsString(DELEGATE_REFUSAL));
            }
        });
    }

    /** The refused completion rolled back whole: the document is DRAFT and the task is still theirs. */
    private void assertStillTheirs(int receipt, String task) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(RECEIPTS + "/" + receipt)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(1)));
        assertEquals(task, taskFor(receipt), "the refused completion rolled back, so the task is still the poster's");
    }

    /** ...and once the document says what the gate asks, the very same completion goes through. */
    private void assertPostedAfterALineIsAdded(int receipt) {
        create(LINES, "{\"GoodsReceipt\":" + receipt + ",\"Quantity\":1}");
        complete(taskFor(receipt), "post", 200);
        awaitStatus(receipt, 2);
    }

    private void complete(String task, String action, int expectedStatus) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"action\":\"COMPLETE\",\"data\":{\"action\":\"" + action + "\"}}")
                                                 .when()
                                                 .post(TASKS + "/" + task)
                                                 .then()
                                                 .statusCode(expectedStatus));
    }

    private void awaitStatus(int receipt, int status) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(RECEIPTS + "/" + receipt)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(status)),
                PROCESS_TIMEOUT_SECONDS);
    }

    /** The post task of one receipt's instance, found by the business key the trigger stamped. */
    private String taskFor(int receipt) {
        AtomicReference<String> task = new AtomicReference<>();
        restAssuredExecutor.execute(() -> task.set(given().when()
                                                          .get(TASKS + "?type=groups")
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .path("find { it.processInstanceBusinessKey == '" + receipt + "' }.id")),
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
        writeProjectFile("app.intent", INTENT_YAML);
        writeProjectFile("custom/GoodsReceiptPostDelegate.java", POST_DELEGATE_JAVA);
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

    private void writeProjectFile(String fileName, String content) {
        String path = PROJECT_PATH + "/" + fileName;
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
