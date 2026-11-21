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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
 * A self-reference declared a tree (intent {@code tree: true}, dirigible #7988) - and the edge an
 * entity names in {@code hierarchy:} - stays a tree: the generated repository refuses a write whose
 * parent is the row itself or one of its descendants, so nothing that walks the edge upwards (a
 * tree view, a roll-up, a breadcrumb) can loop.
 *
 * <p>
 * Asserted on the running, generated code: the self-parent and the descendant-parent are refused
 * with a 400 carrying the authored (or the generated) message, an ordinary re-parent and a create
 * still save, and two re-parents that would close a cycle together, sent at once, never both
 * commit.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentTreeEdgeIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "treeedge";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String TASKS = API + "/projecttask/ProjectTaskController";
    private static final String ACCOUNTS = API + "/account/AccountController";
    private static final String TASK_REFUSAL = "A task cannot sit under itself or one of its subtasks";
    private static final String ACCOUNT_REFUSAL =
            "'Parent' cannot be this Account itself or one of its descendants - the Account hierarchy would become a cycle";
    private static final long TIMEOUT_SECONDS = 90;
    private static final int RACE_ROUNDS = 5;

    private static final String INTENT_YAML =
            """
                    name: treeedge
                    description: a task tree and an account hierarchy that cannot become cycles

                    entities:
                      - name: ProjectTask
                        fields:
                          - { name: id,   type: integer, primaryKey: true, generated: true }
                          - { name: name, type: string, required: true, length: 100 }
                        relations:
                          - { name: Parent, kind: manyToOne, to: ProjectTask, tree: true, treeMessage: "A task cannot sit under itself or one of its subtasks" }

                      - name: Account
                        hierarchy: Parent
                        fields:
                          - { name: id,   type: integer, primaryKey: true, generated: true }
                          - { name: name, type: string, required: true, length: 100 }
                        relations:
                          - { name: Parent, kind: manyToOne, to: Account }
                    """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_tree_edge_refuses_the_row_itself_and_its_descendants_as_parent() {
        generateProject();
        assertTrue(contentOf("i18n/en-US/treeedge.model.json").contains("\"ProjectTask_tree_Parent\": \"" + TASK_REFUSAL + "\""),
                "the refusal's default text must be in the en-US catalog");
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        // Creates are unaffected: a root, its child and its grandchild.
        int root = create(TASKS, "{\"Name\":\"root\"}");
        int child = create(TASKS, "{\"Name\":\"child\",\"Parent\":" + root + "}");
        int grandchild = create(TASKS, "{\"Name\":\"grandchild\",\"Parent\":" + child + "}");
        int other = create(TASKS, "{\"Name\":\"other\"}");

        // The row as its own parent.
        put(TASKS, child, "{\"Name\":\"child\",\"Parent\":" + child + "}").statusCode(400)
                                                                          .body("message", equalTo(TASK_REFUSAL));
        // The row under its own child, and under its own grandchild.
        put(TASKS, root, "{\"Name\":\"root\",\"Parent\":" + child + "}").statusCode(400)
                                                                        .body("message", equalTo(TASK_REFUSAL));
        put(TASKS, root, "{\"Name\":\"root\",\"Parent\":" + grandchild + "}").statusCode(400)
                                                                             .body("message", equalTo(TASK_REFUSAL));
        read(TASKS, root).body("Parent", equalTo(null));

        // An ordinary re-parent within the tree saves, and so does an edit that keeps the parent.
        put(TASKS, grandchild, "{\"Name\":\"grandchild\",\"Parent\":" + root + "}").statusCode(200)
                                                                                   .body("Parent", equalTo(root));
        put(TASKS, child, "{\"Name\":\"child renamed\",\"Parent\":" + root + "}").statusCode(200);
        // A moved subtree is judged from where it now is: the root may go under another root, which
        // then cannot go under the row the root just adopted.
        put(TASKS, root, "{\"Name\":\"root\",\"Parent\":" + other + "}").statusCode(200);
        put(TASKS, other, "{\"Name\":\"other\",\"Parent\":" + grandchild + "}").statusCode(400)
                                                                               .body("message", equalTo(TASK_REFUSAL));

        // The hierarchy: edge is a tree without declaring it, refused with the generated sentence.
        int account = create(ACCOUNTS, "{\"Name\":\"assets\"}");
        int subAccount = create(ACCOUNTS, "{\"Name\":\"cash\",\"Parent\":" + account + "}");
        put(ACCOUNTS, account, "{\"Name\":\"assets\",\"Parent\":" + account + "}").statusCode(400)
                                                                                  .body("message", equalTo(ACCOUNT_REFUSAL));
        put(ACCOUNTS, account, "{\"Name\":\"assets\",\"Parent\":" + subAccount + "}").statusCode(400)
                                                                                     .body("message", equalTo(ACCOUNT_REFUSAL));
        put(ACCOUNTS, subAccount, "{\"Name\":\"cash\"}").statusCode(200);
    }

    /**
     * A under B and B under A, sent at once: each check alone passes against the stored data, so only
     * the locks the walk takes keep both from committing. One may be answered 409 (the database broke
     * the wait between the two) instead of 400, but never both 200.
     */
    @Test
    void two_moves_that_close_a_cycle_together_never_both_commit() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int first = create(TASKS, "{\"Name\":\"first\"}");
        int second = create(TASKS, "{\"Name\":\"second\"}");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < RACE_ROUNDS; round++) {
                put(TASKS, first, "{\"Name\":\"first\"}").statusCode(200);
                put(TASKS, second, "{\"Name\":\"second\"}").statusCode(200);
                AtomicInteger firstStatus = new AtomicInteger();
                AtomicInteger secondStatus = new AtomicInteger();
                // One executor scope for both threads: it holds the connection settings they send with.
                restAssuredExecutor.execute(() -> {
                    CountDownLatch start = new CountDownLatch(1);
                    Future<Integer> firstUnderSecond = pool.submit(() -> moveAfter(start, first, "first", second));
                    Future<Integer> secondUnderFirst = pool.submit(() -> moveAfter(start, second, "second", first));
                    start.countDown();
                    firstStatus.set(await(firstUnderSecond));
                    secondStatus.set(await(secondUnderFirst));
                });
                int a = firstStatus.get();
                int b = secondStatus.get();

                assertFalse(a == 200 && b == 200, "round " + round + ": both moves committed - the tree is a cycle");
                assertTrue(List.of(200, 400, 409)
                               .containsAll(List.of(a, b)),
                        "round " + round + ": unexpected statuses " + a + " / " + b);
                Object firstParent = read(TASKS, first).extract()
                                                       .path("Parent");
                Object secondParent = read(TASKS, second).extract()
                                                         .path("Parent");
                assertFalse(Integer.valueOf(second)
                                   .equals(firstParent)
                        && Integer.valueOf(first)
                                  .equals(secondParent),
                        "round " + round + ": stored a cycle " + first + " <-> " + second);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private static int await(Future<Integer> move) {
        try {
            return move.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread()
                  .interrupt();
            throw new IllegalStateException("interrupted while waiting for a move", e);
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e) {
            throw new IllegalStateException("a move did not complete", e);
        }
    }

    private static int moveAfter(CountDownLatch start, int id, String name, int parent) throws InterruptedException {
        start.await();
        return given().contentType("application/json")
                      .body("{\"Name\":\"" + name + "\",\"Parent\":" + parent + "}")
                      .when()
                      .put(TASKS + "/" + id)
                      .then()
                      .extract()
                      .statusCode();
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
