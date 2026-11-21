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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * A period register partitions time (intent {@code period:}, dirigible #7829).
 *
 * <p>
 * The register accepted overlapping rows, so one date could be covered by an OPEN and a CLOSED
 * period at once and "the period covering this date" had no single answer. The generated repository
 * now refuses a create or an update whose window overlaps another row with a 409 naming that row.
 * Both bounds are inclusive, as for the period lock: windows that share a day overlap, windows that
 * only touch (the 31st and the 1st) do not. An update that keeps its window stays writable, and
 * concurrent creates of one window cannot all pass - the register's rows are locked from the check
 * to the commit.
 */
class IntentPeriodOverlapIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "povjournal";
    private static final String PERIODS = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api/fiscalperiod/FiscalPeriodController";
    private static final String OVERLAPS = "overlaps FiscalPeriod ";
    private static final int CONCURRENT_CREATES = 12;
    private static final long TIMEOUT_SECONDS = 90;

    private static final String YAML = """
            name: povjournal
            description: a period register whose windows must not overlap

            entities:
              - name: PeriodStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: FiscalPeriod
                period: { start: startDate, end: endDate, closedWhen: "Status == CLOSED" }
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: startDate, type: date, required: true }
                  - { name: endDate,   type: date, required: true }
                relations:
                  - { name: Status, kind: manyToOne, to: PeriodStatus, function: EntityStatus, init: 1 }

            seeds:
              - name: period-statuses
                entity: PeriodStatus
                rows:
                  - { id: 1, name: OPEN }
                  - { id: 2, name: CLOSED }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @LocalServerPort
    private int port;

    private final HttpClient http = HttpClient.newBuilder()
                                              .connectTimeout(Duration.ofSeconds(10))
                                              .build();

    @Test
    void a_window_overlapping_another_period_is_refused_and_a_touching_one_is_not() throws Exception {
        deploy();
        int march = create(window("2031-03-01", "2031-03-31", 1));

        // Overlapping the middle, and sharing only the last day - both inclusive bounds.
        refusedCreate(window("2031-03-15", "2031-04-15", 2), march);
        refusedCreate(window("2031-03-31", "2031-04-30", 1), march);
        // Enclosing it.
        refusedCreate(window("2031-02-01", "2031-04-30", 1), march);

        // Touching without overlapping: the 31st ends March, the 1st starts April.
        int april = create(window("2031-04-01", "2031-04-30", 1));

        // An update that moves a bound into March is refused; one that keeps the window is not.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(withId(april, window("2031-03-20", "2031-04-30", 1)))
                                                 .when()
                                                 .put(PERIODS + "/" + april)
                                                 .then()
                                                 .statusCode(409)
                                                 .body(containsString(OVERLAPS + march)));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(withId(april, window("2031-04-01", "2031-04-30", 1)))
                                                 .when()
                                                 .put(PERIODS + "/" + april)
                                                 .then()
                                                 .statusCode(200));

        // Concurrent creates of one window: exactly one passes, every other one is told it overlaps. The
        // creates alternate the status they name, or H2 - whose share lock is exclusive - would already
        // queue them on the status row their reference check locks, and the race would go untested.
        for (int month = 5; month <= 10; month++) {
            java.time.YearMonth yearMonth = java.time.YearMonth.of(2031, month);
            List<Integer> statuses = createConcurrently(yearMonth.atDay(1)
                                                                 .toString(),
                    yearMonth.atEndOfMonth()
                             .toString());
            Map<Integer, Long> counts = statuses.stream()
                                                .collect(Collectors.groupingBy(status -> status, Collectors.counting()));
            assertEquals(1L, counts.getOrDefault(200, 0L), "exactly one concurrent create of " + yearMonth + " wins: " + statuses);
            assertTrue(Set.of(200, 409)
                          .containsAll(counts.keySet()),
                    "a losing concurrent create is a 409, never a 500: " + statuses);
        }
    }

    private List<Integer> createConcurrently(String start, String end) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_CREATES);
        try {
            CountDownLatch together = new CountDownLatch(1);
            List<CompletableFuture<Integer>> answers = new ArrayList<>();
            for (int i = 0; i < CONCURRENT_CREATES; i++) {
                String body = window(start, end, i % 2 + 1);
                answers.add(CompletableFuture.supplyAsync(() -> {
                    awaitQuietly(together);
                    return post(body);
                }, pool));
            }
            together.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (CompletableFuture<Integer> answer : answers) {
                statuses.add(answer.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            }
            return statuses;
        } finally {
            pool.shutdownNow();
        }
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread()
                  .interrupt();
            throw new IllegalStateException("Interrupted while waiting to create", ex);
        }
    }

    /**
     * A plain HTTP call with explicit credentials: {@link RestAssuredExecutor} swaps REST Assured's
     * static base URI and authentication around each call, which two threads cannot share.
     */
    private int post(String body) {
        DirigibleTestTenant tenant = DirigibleTestTenant.createDefaultTenant();
        String credentials = Base64.getEncoder()
                                   .encodeToString((tenant.getUsername() + ":" + tenant.getPassword()).getBytes(StandardCharsets.UTF_8));
        try {
            return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + PERIODS))
                                        .header("Content-Type", "application/json")
                                        .header("Authorization", "Basic " + credentials)
                                        .timeout(Duration.ofSeconds(30))
                                        .POST(HttpRequest.BodyPublishers.ofString(body))
                                        .build(),
                    HttpResponse.BodyHandlers.discarding())
                       .statusCode();
        } catch (InterruptedException ex) {
            Thread.currentThread()
                  .interrupt();
            throw new IllegalStateException("Interrupted while creating a period", ex);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Failed to create a period", ex);
        }
    }

    private void refusedCreate(String body, int overlapped) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(body)
                                                 .when()
                                                 .post(PERIODS)
                                                 .then()
                                                 .statusCode(409)
                                                 .body(containsString(OVERLAPS + overlapped)));
    }

    private int create(String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(PERIODS)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                TIMEOUT_SECONDS);
        return id.get();
    }

    private static String window(String start, String end, int status) {
        return "{\"StartDate\":\"" + start + "\",\"EndDate\":\"" + end + "\",\"Status\":" + status + "}";
    }

    private static String withId(int id, String window) {
        return "{\"Id\":" + id + "," + window.substring(1);
    }

    private void deploy() {
        String path = projectPath() + "/app.intent";
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(YAML.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, YAML.getBytes(StandardCharsets.UTF_8));
        }
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
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT + "/")
                                                 .then()
                                                 .statusCode(200));
        synchronizationProcessor.forceProcessSynchronizers();
    }

    private static String projectPath() {
        return IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    }

    @AfterEach
    void cleanup() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT)
                                                 .then()
                                                 .statusCode(greaterThanOrEqualTo(200)));
        if (repository.hasCollection(projectPath())) {
            repository.removeCollection(projectPath());
        }
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
