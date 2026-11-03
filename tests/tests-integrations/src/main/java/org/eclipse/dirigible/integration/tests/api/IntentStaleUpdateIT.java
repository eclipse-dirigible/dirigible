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
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import io.restassured.response.Response;

/**
 * A full-row PUT built from a stale read does not write back the columns a process owns (dirigible
 * #8109).
 *
 * <p>
 * The generated update read the stored row - for the workflow-status check in the controller, for
 * the system-owned columns in the repository - and wrote the whole row later, with nothing held in
 * between. A process write committing in that window was reverted: the step that resolved a ticket
 * was undone by a PUT carrying back the status it had read (the flow had ended, so the ticket
 * stayed open with no task), and the trigger's {@code ProcessIds} stamp went back to what the PUT
 * had read (the record detached from its flow).
 *
 * <p>
 * The window is opened deterministically: the "process write" is a plain targeted UPDATE held
 * uncommitted on its own connection - exactly the statement a step's {@code updateProperties}
 * issues - and the PUT is sent while it is held. Only once the PUT is seen waiting on that row is
 * the write committed. Before the fix the PUT read the old values without waiting and blocked only
 * at its own UPDATE, so it wrote them back after the commit; now it locks the row before it reads
 * it.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentStaleUpdateIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "stalewrite";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String TICKETS = API + "/ticket/TicketController";
    private static final String POOLS = API + "/pool/PoolController";
    private static final String TICKET_TABLE = "STALEWRITE_TICKET";
    private static final String POOL_TABLE = "STALEWRITE_POOL";
    private static final String REFUSAL = "'Status' changes through the workflow, not a direct edit";
    private static final long TIMEOUT_SECONDS = 90;
    private static final long PROCESS_TIMEOUT_SECONDS = 60;
    /** How long the PUT may take to reach the held row, and then to finish once it is released. */
    private static final long CONCURRENCY_TIMEOUT_SECONDS = 30;

    private static final String INTENT_YAML = """
            name: stalewrite
            description: a full-row PUT read before a process write must not revert it

            entities:
              - name: TicketStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: Ticket
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: subject, type: string, length: 200 }
                  - { name: owner,   type: string, length: 100 }
                relations:
                  - { name: Status, kind: manyToOne, to: TicketStatus, function: EntityStatus, init: 1 }

              - name: Pool
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: note,  type: string, length: 200 }
                  - { name: total, type: integer, readOnly: true, defaultValue: 0 }

            processes:
              - name: TicketFlow
                trigger: { onCreate: Ticket }
                steps:
                  - { name: review,  kind: userTask, args: { assignee: owner, form: ReviewTicket } }
                  - { name: resolve, kind: serviceTask, args: { setRelationField: Status, value: 2 } }
                  - { name: end,     kind: end }

            forms:
              - { name: ReviewTicket, forEntity: Ticket, fields: [subject], editable: [subject], actions: [resolve] }

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

    @Test
    void a_stale_full_row_put_does_not_revert_a_process_write_committed_while_it_was_in_flight() throws Exception {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        // Shape 2 of the issue: the trigger's stamp. The PUT carries no ProcessIds - it was built from
        // a read made before the stamp landed - and the stamp committed while it was in flight survives.
        int stamped = createTicket();
        String stamp = awaitProcessStamp(stamped);
        String restamped = stamp + ",Marker=8109";
        Response stampPut = putWhileHeld(TICKETS + "/" + stamped, "{\"Subject\":\"edited\",\"Status\":1}",
                "UPDATE \"" + TICKET_TABLE + "\" SET \"TICKET_PROCESS_IDS\" = ? WHERE \"TICKET_ID\" = ?", restamped, stamped, TICKET_TABLE);
        assertEquals(200, stampPut.statusCode(), stampPut.asString());
        read(TICKETS, stamped).body("ProcessIds", equalTo(restamped))
                              .body("Subject", equalTo("edited"));

        // Shape 1: the process-owned status. The PUT carries the status it read (Open), which the step
        // moved to Resolved while it was in flight. The step's write stands; the PUT, judged against
        // the row as it now is, is the direct edit of the flow's column it has become.
        int resolved = createTicket();
        awaitProcessStamp(resolved);
        Response statusPut = putWhileHeld(TICKETS + "/" + resolved, "{\"Subject\":\"edited\",\"Status\":1}",
                "UPDATE \"" + TICKET_TABLE + "\" SET \"TICKET_STATUS\" = ? WHERE \"TICKET_ID\" = ?", 2, resolved, TICKET_TABLE);
        assertEquals(409, statusPut.statusCode(), statusPut.asString());
        assertThat(statusPut.asString(), containsString(REFUSAL));
        read(TICKETS, resolved).body("Status", equalTo(2))
                               .body("Subject", equalTo("new"));

        // The repository on its own, for an entity no workflow guards (so no controller lock): a
        // read-only column a system writer maintains keeps the value committed during the PUT.
        int pool = createPool();
        Response poolPut = putWhileHeld(POOLS + "/" + pool, "{\"Note\":\"edited\"}",
                "UPDATE \"" + POOL_TABLE + "\" SET \"POOL_TOTAL\" = ? WHERE \"POOL_ID\" = ?", 5, pool, POOL_TABLE);
        assertEquals(200, poolPut.statusCode(), poolPut.asString());
        read(POOLS, pool).body("Total", equalTo(5))
                         .body("Note", equalTo("edited"));
    }

    /**
     * Holds the given targeted write uncommitted, sends the PUT, waits until the PUT is blocked on the
     * held row, commits the write, and returns the PUT's response.
     */
    private Response putWhileHeld(String url, String body, String heldWrite, Object value, int id, String table) throws Exception {
        try (Connection held = dataSourcesManager.getDefaultDataSource()
                                                 .getConnection()) {
            held.setAutoCommit(false);
            try (PreparedStatement write = held.prepareStatement(heldWrite)) {
                write.setObject(1, value);
                write.setInt(2, id);
                assertEquals(1, write.executeUpdate(), "the row to write must exist");
            }
            CompletableFuture<Response> put =
                    CompletableFuture.supplyAsync(() -> restAssuredExecutor.executeWithResult(() -> given().contentType("application/json")
                                                                                                           .body(body)
                                                                                                           .when()
                                                                                                           .put(url)));
            await().atMost(CONCURRENCY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                   .pollInterval(100, TimeUnit.MILLISECONDS)
                   .until(() -> put.isDone() || isBlockedOn(table));
            held.commit();
            return put.get(CONCURRENCY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    /** Whether some session is waiting for a row lock while running a statement on the table. */
    private boolean isBlockedOn(String table) throws SQLException {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection()) {
            String product = connection.getMetaData()
                                       .getDatabaseProductName();
            String query = product.toLowerCase(Locale.ROOT)
                                  .contains("postgres")
                                          ? "SELECT COUNT(*) FROM pg_stat_activity WHERE wait_event_type = 'Lock' AND query LIKE ?"
                                          : "SELECT COUNT(*) FROM INFORMATION_SCHEMA.SESSIONS WHERE BLOCKER_ID IS NOT NULL"
                                                  + " AND EXECUTING_STATEMENT LIKE ?";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, "%" + table + "%");
                try (ResultSet result = statement.executeQuery()) {
                    return result.next() && result.getInt(1) > 0;
                }
            }
        }
    }

    private String awaitProcessStamp(int ticket) {
        AtomicReference<String> stamp = new AtomicReference<>();
        restAssuredExecutor.execute(() -> stamp.set(given().when()
                                                           .get(TICKETS + "/" + ticket)
                                                           .then()
                                                           .statusCode(200)
                                                           .body("ProcessIds", notNullValue())
                                                           .extract()
                                                           .path("ProcessIds")),
                PROCESS_TIMEOUT_SECONDS);
        return stamp.get();
    }

    private io.restassured.response.ValidatableResponse read(String controller, int id) {
        AtomicReference<io.restassured.response.ValidatableResponse> response = new AtomicReference<>();
        restAssuredExecutor.execute(() -> response.set(given().when()
                                                              .get(controller + "/" + id)
                                                              .then()
                                                              .statusCode(200)));
        return response.get();
    }

    private int createTicket() {
        return create(TICKETS, "{\"Subject\":\"new\",\"Owner\":\"admin\"}");
    }

    private int createPool() {
        return create(POOLS, "{\"Note\":\"new\"}");
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
