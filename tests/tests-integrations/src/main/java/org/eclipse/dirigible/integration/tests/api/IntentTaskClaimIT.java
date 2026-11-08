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
 * A user task's claim moves its record, so the approver decides over lines nobody can change -
 * issue #8461.
 *
 * <p>
 * A create-triggered approval left its order DRAFT for the whole approval window, so the lines -
 * frozen by {@code forbidWhen: "SalesOrder.Status != DRAFT"} - stayed editable while the approver
 * read the total, and the order was confirmed at a total nobody approved. {@code onClaim} /
 * {@code onUnclaim} on the user task move the order to SUBMITTED while the task is claimed and back
 * to DRAFT when it is unclaimed, through the generated {@code assignment} task listener - which
 * this test compiles and runs - and a completion that never claimed the task is refused.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentTaskClaimIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "claim";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String ORDERS = API + "/salesorder/SalesOrderController";
    private static final String LINES = API + "/salesorder/SalesOrderLineController";
    private static final String TASKS = "/services/inbox/tasks";
    private static final String FROZEN = "A submitted order's lines are frozen";
    private static final int DRAFT = 1;
    private static final int SUBMITTED = 2;
    private static final int CONFIRMED = 3;
    private static final long TIMEOUT_SECONDS = 90;
    private static final long PROCESS_TIMEOUT_SECONDS = 60;

    private static final String INTENT_YAML = """
            name: claim
            description: task-claim fixture - the order is held while its approval task is claimed

            entities:
              - name: SalesOrderStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: SalesOrder
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: customer, type: string, length: 100 }
                relations:
                  - { name: Status, kind: manyToOne, to: SalesOrderStatus, function: EntityStatus, init: DRAFT }
                lifecycle:
                  edges:
                    - { from: DRAFT, to: [SUBMITTED] }
                    - { from: SUBMITTED, to: [DRAFT, CONFIRMED] }

              - name: SalesOrderLine
                checks:
                  - { kind: forbidWhen, when: "SalesOrder.Status != DRAFT", message: "A submitted order's lines are frozen" }
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: quantity, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: SalesOrder, kind: manyToOne, to: SalesOrder, composition: true, required: true }

            processes:
              - name: OrderApproval
                trigger: { onCreate: SalesOrder }
                steps:
                  - { name: confirm, kind: userTask, args: { assignee: approver, form: ConfirmOrder,
                      onClaim: { setRelationField: Status, value: SUBMITTED },
                      onUnclaim: { setRelationField: Status, value: DRAFT } } }
                  - { name: markConfirmed, kind: serviceTask, args: { setRelationField: Status, value: CONFIRMED, next: done } }
                  - { name: done, kind: end }

            forms:
              - { name: ConfirmOrder, forEntity: SalesOrder, fields: [customer], actions: [confirm] }

            permissions:
              - { role: Approver, description: Approver, can: [SalesOrder:read] }

            seeds:
              - name: sales-order-statuses
                entity: SalesOrderStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: SUBMITTED }
                  - { id: 3, name: CONFIRMED }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_claimed_approval_holds_the_order_and_its_lines_until_it_is_decided_or_unclaimed() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int order = create(ORDERS, "{\"Customer\":\"ACME\"}");
        int line = create(LINES, "{\"SalesOrder\":" + order + ",\"Quantity\":1}");
        String task = taskFor(order);
        assertStatus(order, DRAFT);

        // Nobody claimed the task, so nothing holds the order: deciding now would decide over lines
        // the author may still be changing.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"action\":\"COMPLETE\",\"data\":{\"action\":\"confirm\"}}")
                                                 .when()
                                                 .post(TASKS + "/" + task)
                                                 .then()
                                                 .statusCode(409)
                                                 .body(containsString("Claim this task before completing it")));
        assertStatus(order, DRAFT);

        // The claim moves the order to SUBMITTED - inside the claim, so it is there when the claim returns.
        act(task, "CLAIM");
        assertStatus(order, SUBMITTED);
        // ... and the lines are frozen while the approver decides: no new line, no changed quantity.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"SalesOrder\":" + order + ",\"Quantity\":1000}")
                                                 .when()
                                                 .post(LINES)
                                                 .then()
                                                 .statusCode(400)
                                                 .body(containsString(FROZEN)));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + line + ",\"SalesOrder\":" + order + ",\"Quantity\":1000}")
                                                 .when()
                                                 .put(LINES + "/" + line)
                                                 .then()
                                                 .statusCode(400)
                                                 .body(containsString(FROZEN)));

        // An unclaim hands the order back to its author, whose lines are editable again.
        act(task, "UNCLAIM");
        assertStatus(order, DRAFT);
        create(LINES, "{\"SalesOrder\":" + order + ",\"Quantity\":2}");

        // Claimed again and decided: the order is confirmed at the lines the approver saw.
        act(task, "CLAIM");
        assertStatus(order, SUBMITTED);
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"action\":\"COMPLETE\",\"data\":{\"action\":\"confirm\"}}")
                                                 .when()
                                                 .post(TASKS + "/" + task)
                                                 .then()
                                                 .statusCode(200));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ORDERS + "/" + order)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(CONFIRMED)),
                PROCESS_TIMEOUT_SECONDS);
    }

    private void act(String task, String action) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"action\":\"" + action + "\"}")
                                                 .when()
                                                 .post(TASKS + "/" + task)
                                                 .then()
                                                 .statusCode(200));
    }

    private void assertStatus(int order, int status) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ORDERS + "/" + order)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(status)));
    }

    /** The approval task of one order's instance, found by the business key the trigger stamped. */
    private String taskFor(int order) {
        AtomicReference<String> task = new AtomicReference<>();
        restAssuredExecutor.execute(() -> task.set(given().when()
                                                          .get(TASKS + "?type=groups")
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .path("find { it.processInstanceBusinessKey == '" + order + "' }.id")),
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
