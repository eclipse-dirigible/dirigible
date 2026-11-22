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

/**
 * The capacity guard of a sum roll-up whose summed child field is CALCULATED (dirigible #7943).
 *
 * <p>
 * A cash desk must never pay out more than it holds: receipts and payments go into one signed
 * calculated column ({@code outflow = Amount * (2 * Kind - 3)}, a receipt negative, a payment
 * positive), summed against the desk's opening cash as the capacity. Before the fix the generated
 * repository ran the guard before the calculated fields were assigned, so it read {@code outflow}
 * as null, counted 0 and accepted every payment - on create and on update alike.
 */
class IntentRollupGuardCalculatedChildIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "rollupcalcguard";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String DESKS = API + "/cashdesk/CashDeskController";
    private static final String ORDERS = API + "/cashorder/CashOrderController";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String INTENT_YAML = """
            name: rollupcalcguard
            description: a capacity guard over a calculated child amount

            entities:
              - name: CashDesk
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: name,    type: string, length: 40 }
                  - { name: opening, type: decimal, precision: 18, scale: 2 }
                  - { name: paidOut, type: decimal, precision: 18, scale: 2 }

              - name: CashOrder
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: amount,  type: decimal, precision: 18, scale: 2 }
                  # 1 = receipt, 2 = payment
                  - { name: kind,    type: integer }
                  - { name: outflow, type: decimal, precision: 18, scale: 2,
                      calculatedOnCreate: "Amount * (2 * Kind - 3)", calculatedOnUpdate: "Amount * (2 * Kind - 3)" }
                relations:
                  - { name: CashDesk, kind: manyToOne, to: CashDesk, required: true }

            rollups:
              - { name: deskPaidOut, entity: CashOrder, via: CashDesk, field: paidOut, op: sum, of: outflow,
                  capacity: opening }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_calculated_child_amount_is_guarded_on_create_and_on_update() {
        writeIntent();
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int desk = create(DESKS, "{\"Name\":\"Main\",\"Opening\":100}");
        // A receipt of 50 adds to the cash on hand (outflow -50), a payment of 120 then fits (70 of 100).
        create(ORDERS, "{\"Amount\":50,\"Kind\":1,\"CashDesk\":" + desk + "}");
        int payment = createOnce(ORDERS, "{\"Amount\":120,\"Kind\":2,\"CashDesk\":" + desk + "}");

        // A further payment of 40 would pay out 110 of 100: refused, its outflow is 40, not null. The
        // writes from here on are sent once - retried, an accepted over-capacity write would be refused
        // on its second attempt and pass.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Amount\":40,\"Kind\":2,\"CashDesk\":" + desk + "}")
                                                 .when()
                                                 .post(ORDERS)
                                                 .then()
                                                 .statusCode(400)
                                                 .body(containsString("The Cash Desk capacity is exceeded")));

        // Raising the stored payment to 200 would pay out 150 of 100: refused on the update path too.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + payment + ",\"Amount\":200,\"Kind\":2,\"CashDesk\":" + desk + "}")
                                                 .when()
                                                 .put(ORDERS + "/" + payment)
                                                 .then()
                                                 .statusCode(400)
                                                 .body(containsString("The Cash Desk capacity is exceeded")));

        // A payment that fits is still accepted, and the refused writes changed nothing.
        createOnce(ORDERS, "{\"Amount\":30,\"Kind\":2,\"CashDesk\":" + desk + "}");
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ORDERS + "/" + payment)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Amount", equalTo(120.0f))
                                                 .body("Outflow", equalTo(120.0f)),
                TIMEOUT_SECONDS);
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

    private int createOnce(String controller, String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(controller)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")));
        return id.get();
    }

    private void generateProject() {
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
