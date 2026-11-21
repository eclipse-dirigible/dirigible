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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
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

/**
 * dirigible #7827: the generated guards that read a row's state and then write are atomic per
 * guarded row. Each guard is raced by several identical requests released at once, and exactly one
 * of them may pass - the others are refused as they would be in sequence.
 *
 * <ul>
 * <li>A create-from's from-status guard ({@code fromStatus:} + {@code sourceStatus:}): concurrent
 * clicks on one quote each read it OPEN before any of them flipped it, and each created an order -
 * several invoices for one proforma.</li>
 * <li>A transition's {@code from:} guard: the same transition fired at once succeeded more than
 * once.</li>
 * <li>A capacity roll-up's guard: concurrent payments that each fit the remaining capacity alone
 * all passed against the same consumed total, and together drove the balance negative.</li>
 * </ul>
 */
class IntentConcurrentGuardsIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "concurrentguards";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String QUOTES = API + "/quote/QuoteController";
    private static final String ORDERS = API + "/salesorder/SalesOrderController";
    private static final String DESKS = API + "/cashdesk/CashDeskController";
    private static final String PAYMENTS = API + "/cashpayment/CashPaymentController";
    private static final String EVENTS = "/services/java/" + PROJECT + "/gen/events/" + PROJECT;
    private static final String GENERATE = EVENTS + "/OrderFromQuoteGenerate/run";
    private static final String TRANSITION = EVENTS + "/AcceptQuoteTransition/run";
    private static final long TIMEOUT_SECONDS = 90;

    /** How many identical requests race for one guarded row. */
    private static final int RACERS = 4;
    /**
     * How many times each race is run; the unguarded platform lost a race in a fraction of the runs.
     */
    private static final int RACE_ATTEMPTS = 10;
    private static final long RACE_TIMEOUT_SECONDS = 60;

    private static final String INTENT_YAML = """
            name: concurrentguards
            description: check-then-write guards raced by concurrent requests

            entities:
              - name: QuoteStatus
                function: Setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }

              - name: Quote
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 40 }
                relations:
                  - { name: Status, kind: manyToOne, to: QuoteStatus, function: EntityStatus, init: 1 }

              - name: SalesOrder
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 40 }

              - name: CashDesk
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: name,    type: string, length: 40 }
                  - { name: opening, type: decimal, precision: 18, scale: 2 }
                  - { name: paidOut, type: decimal, precision: 18, scale: 2 }

              - name: CashPayment
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: CashDesk, kind: manyToOne, to: CashDesk, required: true }

            generates:
              - name: order-from-quote
                from: Quote
                to: SalesOrder
                label: Order
                map: { name: name }
                fromStatus: [1]
                sourceStatus: 2

            transitions:
              - { name: AcceptQuote, forEntity: Quote, from: [1], setStatus: 3, label: Accept }

            rollups:
              - { name: deskPaidOut, entity: CashPayment, via: CashDesk, field: paidOut, op: sum, of: amount,
                  capacity: opening }

            seeds:
              - name: quote-statuses
                entity: QuoteStatus
                rows:
                  - { id: 1, name: OPEN }
                  - { id: 2, name: ORDERED }
                  - { id: 3, name: ACCEPTED }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_guard_and_the_write_it_protects_are_atomic() {
        writeIntent();
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        ExecutorService racers = Executors.newFixedThreadPool(RACERS);
        try {
            for (int attempt = 0; attempt < RACE_ATTEMPTS; attempt++) {
                assertOneCreateFromPerSource(racers, attempt);
                assertOneTransitionPerRecord(racers, attempt);
                assertTheCapacityHoldsAgainstConcurrentPayments(racers, attempt);
            }
        } finally {
            racers.shutdownNow();
        }
    }

    /**
     * Concurrent clicks of a create-from on one OPEN quote: one order, the other clicks refused 409.
     */
    private void assertOneCreateFromPerSource(ExecutorService racers, int attempt) {
        int quote = create(QUOTES, "{\"Name\":\"generate-" + attempt + "\"}");
        int ordersBefore = count(ORDERS);
        List<Integer> statuses = race(racers, () -> postOnce(GENERATE, "{\"id\":" + quote + "}"));
        assertExactlyOneWins("create-from, attempt " + attempt, statuses, 409);
        assertEquals(ordersBefore + 1, count(ORDERS), "create-from, attempt " + attempt + ": one quote must produce one order");
    }

    /** The same transition fired at once on one OPEN quote: one flip, the others refused 409. */
    private void assertOneTransitionPerRecord(ExecutorService racers, int attempt) {
        int quote = create(QUOTES, "{\"Name\":\"transition-" + attempt + "\"}");
        List<Integer> statuses = race(racers, () -> postOnce(TRANSITION, "{\"id\":" + quote + "}"));
        assertExactlyOneWins("transition, attempt " + attempt, statuses, 409);
    }

    /**
     * Concurrent payments, each of the desk's whole opening cash: one is stored, the others are refused
     * 400 by the capacity guard, and the desk never pays out more than it holds.
     */
    private void assertTheCapacityHoldsAgainstConcurrentPayments(ExecutorService racers, int attempt) {
        int desk = create(DESKS, "{\"Name\":\"desk-" + attempt + "\",\"Opening\":100}");
        List<Integer> statuses = race(racers, () -> postOnce(PAYMENTS, "{\"Amount\":100,\"CashDesk\":" + desk + "}"));
        assertExactlyOneWins("capacity, attempt " + attempt, statuses, 400);
    }

    private static void assertExactlyOneWins(String race, List<Integer> statuses, int refusal) {
        long won = statuses.stream()
                           .filter(status -> status == 200)
                           .count();
        long refused = statuses.stream()
                               .filter(status -> status == refusal)
                               .count();
        assertEquals(1, won, race + ": exactly one request may pass, answered " + statuses);
        assertEquals(RACERS - 1, refused, race + ": every other request must be refused " + refusal + ", answered " + statuses);
    }

    /**
     * Releases {@link #RACERS} copies of one request through one barrier; answers their status codes.
     */
    private List<Integer> race(ExecutorService racers, Callable<Integer> request) {
        return restAssuredExecutor.executeWithResult(() -> {
            CyclicBarrier start = new CyclicBarrier(RACERS);
            List<Future<Integer>> answers = new ArrayList<>();
            for (int i = 0; i < RACERS; i++) {
                answers.add(racers.submit(() -> {
                    start.await();
                    return request.call();
                }));
            }
            List<Integer> statuses = new ArrayList<>();
            try {
                for (Future<Integer> answer : answers) {
                    statuses.add(answer.get(RACE_TIMEOUT_SECONDS, TimeUnit.SECONDS));
                }
            } catch (InterruptedException e) {
                Thread.currentThread()
                      .interrupt();
                throw new IllegalStateException("interrupted while racing concurrent requests", e);
            } catch (Exception e) {
                throw new IllegalStateException("a racing request did not answer", e);
            }
            return statuses;
        });
    }

    /** One POST, sent once: a retry would turn a refused duplicate into a pass. */
    private static int postOnce(String endpoint, String body) {
        return given().contentType("application/json")
                      .body(body)
                      .when()
                      .post(endpoint)
                      .statusCode();
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

    private int count(String controller) {
        AtomicInteger count = new AtomicInteger();
        restAssuredExecutor.execute(() -> count.set(given().when()
                                                           .get(controller + "/count")
                                                           .then()
                                                           .statusCode(200)
                                                           .extract()
                                                           .jsonPath()
                                                           .getInt("count")));
        return count.get();
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
