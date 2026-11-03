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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
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
import org.springframework.test.annotation.DirtiesContext;

/**
 * Deleting a process-triggered record retires its process every time, and a task of a deleted
 * record is refused rather than executed (dirigible #8108).
 *
 * <p>
 * A DELETE landing while one of the record's tasks was being completed removed the row with a 200,
 * but the generated abort-on-delete listener's single, fail-soft cancel lost the optimistic lock to
 * the completion, so the process moved on and its NEXT task was handed to every agent's Inbox for a
 * record that no longer existed. Now the platform retries a cancel that lost the lock until the
 * instance is gone, and a completion first asks the process's trigger whether its record still
 * exists: when it does not, the task is answered 404 and the process cancelled.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentDeleteRetiresProcessIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "deleteretire";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String TICKETS = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api/ticket/TicketController";
    private static final String TASKS = "/services/inbox/tasks";
    private static final String INSTANCES = "/services/bpm/bpm-processes/instances?key=TicketHandling";
    private static final long TIMEOUT_SECONDS = 90;
    private static final long PROCESS_TIMEOUT_SECONDS = 60;
    private static final int RACING_TICKETS = 8;

    private static final String INTENT_YAML = """
            name: deleteretire
            description: deleting a ticket retires its handling process

            entities:
              - name: TicketStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: Ticket
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: subject, type: string, length: 100 }
                relations:
                  - { name: Status, kind: manyToOne, to: TicketStatus, function: EntityStatus, init: 1 }

            processes:
              - name: TicketHandling
                trigger: { onCreate: Ticket }
                steps:
                  - { name: triage, kind: userTask, args: { assignee: agent, form: TriageTicket } }
                  - { name: work,   kind: userTask, args: { assignee: agent, form: WorkTicket } }
                  - { name: close,  kind: serviceTask, args: { setRelationField: Status, value: 2 } }
                  - { name: end,    kind: end }

            forms:
              - { name: TriageTicket, forEntity: Ticket, fields: [subject], actions: [confirm] }
              - { name: WorkTicket,   forEntity: Ticket, fields: [subject], actions: [resolve] }

            seeds:
              - name: ticket-statuses
                entity: TicketStatus
                rows:
                  - { id: 1, name: Open }
                  - { id: 2, name: Resolved }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @LocalServerPort
    private int port;

    private final HttpClient http = HttpClient.newBuilder()
                                              .connectTimeout(Duration.ofSeconds(10))
                                              .build();

    /**
     * The row is gone and nothing told the process - the state a cancel that lost its race leaves
     * behind. Completing the task is refused with a 404 and retires the process, so no next task is
     * handed out for the record.
     */
    @Test
    void a_task_of_a_deleted_record_is_refused_and_its_process_retired() {
        generateAndPublish();

        int ticket = create();
        String triage = taskFor(ticket);
        deleteRowBehindTheRepository(ticket);

        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"action\":\"COMPLETE\",\"data\":{\"action\":\"confirm\"}}")
                                                 .when()
                                                 .post(TASKS + "/" + triage)
                                                 .then()
                                                 .statusCode(404)
                                                 .body(equalTo("The record this task is about no longer exists")));

        restAssuredExecutor.execute(() -> {
            assertEquals(List.of(), runningInstancesFor(List.of(ticket)), "the deleted record's process must be cancelled");
            assertEquals(List.of(), tasksFor(List.of(ticket)), "no task may be handed out for the deleted record");
        });
    }

    /**
     * The hunt's race (base-services#54): every ticket's triage is completed while the ticket is
     * deleted. Whichever wins, the record's process must end up retired and its Inbox empty.
     */
    @Test
    void a_delete_racing_a_task_completion_retires_the_process() throws Exception {
        generateAndPublish();

        List<Integer> tickets = new ArrayList<>();
        for (int i = 0; i < RACING_TICKETS; i++) {
            tickets.add(create());
        }
        List<String> triages = new ArrayList<>();
        for (int ticket : tickets) {
            triages.add(taskFor(ticket));
        }

        ExecutorService racers = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < tickets.size(); i++) {
                int ticket = tickets.get(i);
                String triage = triages.get(i);
                CountDownLatch together = new CountDownLatch(1);
                CompletableFuture<Integer> completion = CompletableFuture.supplyAsync(() -> {
                    awaitQuietly(together);
                    return send(HttpRequest.newBuilder(uri(TASKS + "/" + triage))
                                           .header("Content-Type", "application/json")
                                           .POST(HttpRequest.BodyPublishers.ofString(
                                                   "{\"action\":\"COMPLETE\",\"data\":{\"action\":\"confirm\"}}")));
                }, racers);
                CompletableFuture<Integer> deletion = CompletableFuture.supplyAsync(() -> {
                    awaitQuietly(together);
                    return send(HttpRequest.newBuilder(uri(TICKETS + "/" + ticket))
                                           .DELETE());
                }, racers);
                together.countDown();
                completion.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertTrue(List.of(200, 204)
                               .contains(deletion.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)),
                        "the delete of ticket " + ticket + " must succeed");
            }
        } finally {
            racers.shutdownNow();
        }

        restAssuredExecutor.execute(() -> {
            assertEquals(List.of(), runningInstancesFor(tickets), "every deleted ticket's process must be retired");
            assertEquals(List.of(), tasksFor(tickets), "no task may be left in the Inbox for a deleted ticket");
        }, PROCESS_TIMEOUT_SECONDS);
    }

    private void generateAndPublish() {
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
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT + "/")
                                                 .then()
                                                 .statusCode(200));
        synchronizationProcessor.forceProcessSynchronizers();
    }

    private int create() {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body("{\"Subject\":\"printer jam\"}")
                                                        .when()
                                                        .post(TICKETS)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                TIMEOUT_SECONDS);
        return id.get();
    }

    /** The open task of this ticket's instance, found by the business key the trigger stamped. */
    private String taskFor(int ticket) {
        AtomicReference<String> task = new AtomicReference<>();
        restAssuredExecutor.execute(() -> {
            task.set(given().when()
                            .get(TASKS + "?type=groups")
                            .then()
                            .statusCode(200)
                            .extract()
                            .path("find { it.processInstanceBusinessKey == '" + ticket + "' }.id"));
            assertNotNull(task.get(), "ticket " + ticket + " must have its triage task");
        }, PROCESS_TIMEOUT_SECONDS);
        return task.get();
    }

    /** The Inbox tasks of these tickets - called inside a {@link RestAssuredExecutor} execution. */
    private static List<String> tasksFor(List<Integer> tickets) {
        return given().when()
                      .get(TASKS + "?type=groups")
                      .then()
                      .statusCode(200)
                      .extract()
                      .path("findAll { " + keyFilter("processInstanceBusinessKey", tickets) + " }.id");
    }

    /** The running process instances of these tickets - called inside an execution too. */
    private static List<String> runningInstancesFor(List<Integer> tickets) {
        return given().when()
                      .get(INSTANCES)
                      .then()
                      .statusCode(200)
                      .extract()
                      .path("findAll { " + keyFilter("businessKey", tickets) + " }.id");
    }

    private static String keyFilter(String property, List<Integer> tickets) {
        return "it." + property + " in [" + String.join(",", tickets.stream()
                                                                    .map(ticket -> "'" + ticket + "'")
                                                                    .toList())
                + "]";
    }

    /** Removes the row without the repository, so no -deleted event reaches the process. */
    private void deleteRowBehindTheRepository(int ticket) {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                PreparedStatement delete = connection.prepareStatement("DELETE FROM \"DELETERETIRE_TICKET\" WHERE \"TICKET_ID\" = ?")) {
            delete.setInt(1, ticket);
            assertEquals(1, delete.executeUpdate(), "the ticket must exist");
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to delete ticket " + ticket, ex);
        }
    }

    private int send(HttpRequest.Builder request) {
        DirigibleTestTenant tenant = DirigibleTestTenant.createDefaultTenant();
        String credentials = Base64.getEncoder()
                                   .encodeToString((tenant.getUsername() + ":" + tenant.getPassword()).getBytes(StandardCharsets.UTF_8));
        try {
            return http.send(request.header("Authorization", "Basic " + credentials)
                                    .timeout(Duration.ofSeconds(30))
                                    .build(),
                    HttpResponse.BodyHandlers.discarding())
                       .statusCode();
        } catch (InterruptedException ex) {
            Thread.currentThread()
                  .interrupt();
            throw new IllegalStateException("Interrupted while calling the application", ex);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to call the application", ex);
        }
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread()
                  .interrupt();
            throw new IllegalStateException("Interrupted while waiting to race", ex);
        }
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
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
