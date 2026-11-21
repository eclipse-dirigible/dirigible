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
 * A sum roll-up over integral columns (dirigible #7944).
 *
 * <p>
 * The parser accepts {@code integer} / {@code long} for a sum roll-up's {@code of:},
 * {@code field:}, {@code capacity:} and {@code balance:}, but every emission site computed in
 * {@code BigDecimal} and assigned as if each column were one - {@code sum.add(row.Places)},
 * {@code parent.Booked = sum}, {@code guardConsumed.add(guardRow.Places)},
 * {@code compareTo(guardParent.Seats)}. None of it compiled, and since one javac task compiles the
 * whole client-Java batch, every controller of the application answered 404. Two shapes are
 * published side by side, so the first create already proves the batch compiled: an enforced seat
 * count (integer throughout, with a refusal message that names the remaining seats) and an
 * unenforced hour budget (long totals over integer hours), whose balance the parent's own
 * repository also recomputes when the budget is edited.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentRollupIntegralColumnsIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "rollupintegral";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String SESSIONS = API + "/session/SessionController";
    private static final String BOOKINGS = API + "/booking/BookingController";
    private static final String BUDGETS = API + "/budget/BudgetController";
    private static final String TIMESHEETS = API + "/timesheet/TimesheetController";
    private static final long TIMEOUT_SECONDS = 90;
    /** The roll-up runs off the child's topics, after the commit. */
    private static final long ROLLUP_TIMEOUT_SECONDS = 30;

    private static final String INTENT_YAML = """
            name: rollupintegral
            description: sum roll-ups over integer and long columns

            entities:
              - name: Session
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: name,   type: string, length: 40 }
                  - { name: seats,  type: integer }
                  - { name: booked, type: integer }
                  - { name: free,   type: integer }

              - name: Booking
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: places, type: integer }
                relations:
                  - { name: Session, kind: manyToOne, to: Session, required: true }

              - name: Budget
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: name,      type: string, length: 40 }
                  - { name: hours,     type: long }
                  - { name: spent,     type: long }
                  - { name: remaining, type: long }

              - name: Timesheet
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: hours, type: integer }
                relations:
                  - { name: Budget, kind: manyToOne, to: Budget, required: true }

            rollups:
              - { name: sessionBooked, entity: Booking, via: Session, field: booked, op: sum, of: places,
                  capacity: seats, balance: free, message: "Only {remaining} seats left" }
              - { name: budgetSpent, entity: Timesheet, via: Budget, field: spent, op: sum, of: hours,
                  capacity: hours, balance: remaining, enforce: false }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void integral_sum_roll_ups_compile_count_guard_and_measure() {
        writeIntent();
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        // Enforced, integer throughout: the total and the free seats follow the bookings, and a booking
        // past the seats is refused with the remaining seats in the message.
        int session = create(SESSIONS, "{\"Name\":\"Keynote\",\"Seats\":10}");
        create(BOOKINGS, "{\"Places\":4,\"Session\":" + session + "}");
        awaitRow(SESSIONS, session, "Booked", 4, "Free", 6);
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Places\":7,\"Session\":" + session + "}")
                                                 .when()
                                                 .post(BOOKINGS)
                                                 .then()
                                                 .statusCode(400)
                                                 .body("message", containsString("Only 6 seats left")),
                TIMEOUT_SECONDS);
        create(BOOKINGS, "{\"Places\":6,\"Session\":" + session + "}");
        awaitRow(SESSIONS, session, "Booked", 10, "Free", 0);

        // Unenforced, long totals over integer hours: booking past the budget is information, and an edit
        // of the budget moves the balance through the parent's own repository.
        int budget = create(BUDGETS, "{\"Name\":\"Migration\",\"Hours\":100}");
        create(TIMESHEETS, "{\"Hours\":30,\"Budget\":" + budget + "}");
        awaitRow(BUDGETS, budget, "Spent", 30, "Remaining", 70);
        create(TIMESHEETS, "{\"Hours\":90,\"Budget\":" + budget + "}");
        awaitRow(BUDGETS, budget, "Spent", 120, "Remaining", -20);
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + budget + ",\"Name\":\"Migration\",\"Hours\":200}")
                                                 .when()
                                                 .put(BUDGETS + "/" + budget)
                                                 .then()
                                                 .statusCode(200),
                TIMEOUT_SECONDS);
        awaitRow(BUDGETS, budget, "Spent", 120, "Remaining", 80);
    }

    private void awaitRow(String controller, int id, String total, int totalValue, String balance, int balanceValue) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(controller + "/" + id)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(total, equalTo(totalValue))
                                                 .body(balance, equalTo(balanceValue)),
                ROLLUP_TIMEOUT_SECONDS);
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
