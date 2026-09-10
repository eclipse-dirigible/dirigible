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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * The flat per-item post ({@code posts:} with {@code forEach:}) writes the rows one source event
 * derives as ONE transaction - at runtime, through the published application.
 *
 * <p>
 * Each row used to be saved in its own transaction, so a row the target repository refused (here a
 * required column the derived row leaves null) left the rows before it durable. This mode's
 * idempotency guard is coarser than the posting's - it asks whether ANY row back-references the
 * source - so that partial set read as a finished post and no redelivery ever wrote the rest: the
 * half-post was PERMANENT (issue #7179). The scenario below is exactly that sequence: a refused
 * row, the cause repaired, the event redelivered - and the ledger must end up carrying the whole
 * post.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("slow")
class IntentPostsAtomicityIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "postsatomic";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String TRANSITION = "/services/java/" + PROJECT + "/gen/events/" + PROJECT + "/";
    private static final long TIMEOUT_SECONDS = 90;

    /**
     * A goods issue whose lines post one stock movement each. {@code StockMovement.quantity} is
     * REQUIRED, which is how a derived row gets refused: an item with no quantity derives a movement
     * the target repository will not accept. The sibling {@code posts:} rule into {@code StockNote} is
     * the observable proof that the event was delivered and consumed, so "no movement rows" can be
     * asserted at a point where the handler has demonstrably run.
     */
    private static final String INTENT_YAML = """
            name: postsatomic
            description: flat per-item post fixture - a refused row leaves nothing behind

            entities:
              - name: GoodsIssueStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string,  required: true, length: 100 }

              - name: GoodsIssue
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, length: 40 }
                relations:
                  - { name: Status, kind: manyToOne, to: GoodsIssueStatus, function: EntityStatus, init: 1 }

              - name: GoodsIssueItem
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: quantity, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: GoodsIssue, kind: manyToOne, to: GoodsIssue, composition: true, required: true }

              - name: StockMovement
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: quantity, type: decimal, precision: 18, scale: 2, required: true }
                relations:
                  - { name: GoodsIssue, kind: manyToOne, to: GoodsIssue }

              - name: StockNote
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 40 }
                relations:
                  - { name: GoodsIssue, kind: manyToOne, to: GoodsIssue }

            transitions:
              - { name: PostGoodsIssue,   forEntity: GoodsIssue, from: [1], setStatus: 2, label: Post,   icon: check }
              - { name: ReopenGoodsIssue, forEntity: GoodsIssue, from: [2], setStatus: 3, label: Reopen, icon: undo }
              - { name: RepostGoodsIssue, forEntity: GoodsIssue, from: [3], setStatus: 2, label: Repost, icon: check }

            posts:
              - name: goodsIssueLedger
                forEntity: GoodsIssue
                event: 2
                forEach: items
                into: StockMovement
                idempotentBy: GoodsIssue
                set:
                  Quantity: item.Quantity
              - name: goodsIssueNote
                forEntity: GoodsIssue
                event: 2
                into: StockNote
                idempotentBy: GoodsIssue
                set:
                  Note: source.Number

            seeds:
              - name: goods-issue-statuses
                entity: GoodsIssueStatus
                rows:
                  - { id: 1, name: Draft }
                  - { id: 2, name: Posted }
                  - { id: 3, name: Reopened }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_refused_row_leaves_no_post_and_the_redelivery_writes_the_whole_one() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        // A goods issue with two lines, the second one missing its quantity - so the movement derived
        // from it is a row the target repository must refuse.
        int issue = create("/goodsissue/GoodsIssueController", "{\"Number\":\"GI-1\"}");
        create("/goodsissue/GoodsIssueItemController", "{\"GoodsIssue\":" + issue + ",\"Quantity\":5}");
        int blank = create("/goodsissue/GoodsIssueItemController", "{\"GoodsIssue\":" + issue + "}");

        // Post it: the event is raised, both handlers hear it, and the ledger's second row is refused.
        transition("PostGoodsIssue", issue);
        // The sibling post's row is the delivery receipt - once it exists, the event has been consumed.
        awaitCount("/stocknote/StockNoteController", issue, 1);

        // ...and the ledger carries NOTHING. One row per transaction, this was a durable single row -
        // which the guard below then reads as a finished post.
        assertCount("/stockmovement/StockMovementController", issue, 0);

        // Repair the line and redeliver the event (reopen, post again). With a partial set on file the
        // guard answers "already posted" and this is where the missing row was lost for good.
        update("/goodsissue/GoodsIssueItemController/" + blank, "{\"Id\":" + blank + ",\"GoodsIssue\":" + issue + ",\"Quantity\":3}");
        transition("ReopenGoodsIssue", issue);
        transition("RepostGoodsIssue", issue);

        // The whole post, exactly once: one movement per line, and no duplicate of the row that had
        // succeeded on the failed tick.
        awaitCount("/stockmovement/StockMovementController", issue, 2);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/stockmovement/StockMovementController")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("findAll { it.GoodsIssue == " + issue + " }.Quantity.sum()", equalTo(8.0)));
    }

    private void transition(String name, int id) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + id + "}")
                                                 .when()
                                                 .post(TRANSITION + name + "Transition/run")
                                                 .then()
                                                 .statusCode(200));
    }

    /** The row count against one goods issue, retried until it holds (the post is asynchronous). */
    private void awaitCount(String controller, int issue, int expected) {
        restAssuredExecutor.execute(() -> assertCount(controller, issue, expected), TIMEOUT_SECONDS);
    }

    private void assertCount(String controller, int issue, int expected) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + controller)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("findAll { it.GoodsIssue == " + issue + " }.size()", equalTo(expected)));
    }

    private int create(String controller, String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(API + controller)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                TIMEOUT_SECONDS);
        return id.get();
    }

    private void update(String controller, String body) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(body)
                                                 .when()
                                                 .put(API + controller)
                                                 .then()
                                                 .statusCode(200));
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
