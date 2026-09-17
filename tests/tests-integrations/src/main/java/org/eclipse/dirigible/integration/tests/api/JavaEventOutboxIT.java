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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.components.api.messaging.MessagingFacade;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.base.ProjectUtil;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An entity write and the event announcing it commit together, and an event the broker did not take
 * is delivered later instead of being lost (issue #6816).
 *
 * <p>
 * The repository used to commit the row and only then publish, with no shared transaction and no
 * catch: a broker that was briefly unavailable both lost the event for good — nothing retried it —
 * and raised to the REST caller whose row had actually been written, inviting a retry that
 * duplicated the record. The event now goes into the tenant's {@code DIRIGIBLE_EVENT_OUTBOX} on the
 * write's own connection, and a relay drains whatever the in-process publish could not deliver.
 *
 * <p>
 * Both halves are asserted through the real machinery: the first from a client repository's save,
 * the second from an entry planted in the outbox the way a broker outage would have left one —
 * which is the only failure a test can stage without tearing the broker down under the whole suite.
 *
 * <p>
 * The same fixture asserts what a delete announces (issue #7146): the row as it was read inside the
 * deleting transaction, not the partial instance the caller passed in.
 */
class JavaEventOutboxIT extends IntegrationTest {

    private static final String PROJECT = "JavaEventOutboxIT";
    private static final String CONTROLLER = "/services/java/" + PROJECT + "/things/OutboxThingController";
    private static final String ENTITY_TABLE = "EVENT_OUTBOX_THING";
    private static final String OUTBOX_TABLE = "DIRIGIBLE_EVENT_OUTBOX";
    private static final String CREATED_TOPIC = "event-outbox-it-thing";
    private static final String DELETED_TOPIC = "event-outbox-it-thing-deleted";
    private static final String ECHO_QUEUE = "event-outbox-it-echo";
    private static final String DELETED_ECHO_QUEUE = "event-outbox-it-echo-deleted";
    private static final long TIMEOUT_SECONDS = 60;
    private static final long RECEIVE_TIMEOUT_MILLIS = 2000;
    private static final String STRANDED_PAYLOAD = "{\"name\":\"stranded\"}";

    @Autowired
    private IRepository repository;

    @Autowired
    private ProjectUtil projectUtil;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Autowired
    private Scheduler scheduler;

    @Test
    void a_write_publishes_through_the_outbox_a_delete_announces_the_stored_row_and_a_stranded_entry_is_relayed() throws Exception {
        ClientJavaProjectDeployer.deploy(repository, projectUtil, synchronizationProcessor, PROJECT, PROJECT);

        // A create still reaches its listener - the write records the event in the outbox and hands it
        // to the broker straight after the commit, so nothing about the timing changes for a healthy
        // system. The whole exchange is retried because a topic keeps nothing for a subscriber that is
        // not there yet, and the client class is compiled and subscribed asynchronously.
        Awaitility.await()
                  .pollInterval(1, TimeUnit.SECONDS)
                  .atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .ignoreExceptions()
                  .until(() -> {
                      seed();
                      return receiveEcho() != null;
                  });

        // Nothing is left behind: what the in-process dispatch delivered, it also cleared.
        Awaitility.await()
                  .pollInterval(1, TimeUnit.SECONDS)
                  .atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .until(() -> countOutboxEntries() == 0);

        // A targeted write's EXTRA events (the shape of the generated DAO's "-rekeyed" pair) ride the
        // same outbox: one mutation, two notices - the written row and the caller's own message - both
        // delivered and both cleared. Without the event-carrying overload these were bare publishes a
        // broker outage would swallow.
        drainEchoQueue();
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(CONTROLLER + "/retarget")
                                                 .then()
                                                 .statusCode(200));
        Set<String> notices = new HashSet<>();
        Awaitility.await()
                  .pollInterval(1, TimeUnit.SECONDS)
                  .atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .until(() -> {
                      String echo = receiveEcho();
                      if (echo != null) {
                          notices.add(echo);
                      }
                      return notices.size() >= 2;
                  });
        assertTrue(notices.stream()
                          .anyMatch(notice -> notice.contains("moved")),
                "the targeted write must record the written row: " + notices);
        assertTrue(notices.stream()
                          .anyMatch(notice -> notice.contains("previous")),
                "the targeted write must record the caller's extra event with the same write: " + notices);
        Awaitility.await()
                  .pollInterval(1, TimeUnit.SECONDS)
                  .atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .until(() -> countOutboxEntries() == 0);

        // Now the failure the issue is about: an entry the broker refused, left behind by a write that
        // nonetheless succeeded. Nothing else will publish it - only the relay can. The queue is drained
        // first so the echo that arrives can only be this one.
        drainEchoQueue();
        String strandedId = plantStrandedEntry(STRANDED_PAYLOAD);
        scheduler.triggerJob(JobKey.jobKey("EventOutboxRelayJob", "system"));

        String relayed = Awaitility.await()
                                   .pollInterval(1, TimeUnit.SECONDS)
                                   .atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                                   .until(this::receiveEcho, echo -> echo != null);
        assertEquals(STRANDED_PAYLOAD, relayed, "the relay must publish exactly what the write recorded");
        assertTrue(entryIsGone(strandedId), "a delivered entry must be cleared, so it is not published twice");

        // A delete announces the row that was removed, not the instance the caller happened to hold: the
        // controller deletes through a snapshot carrying nothing but the id, and the payload must still
        // name the row's stored value (issue #7146). Published the caller's way, every unset column
        // arrived null and a reaction reading one - the abort listener reading ProcessIds - no-oped in
        // silence, leaving the process running after its record.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(CONTROLLER + "/deletePartial")
                                                 .then()
                                                 .statusCode(200));
        String deleted = Awaitility.await()
                                   .pollInterval(1, TimeUnit.SECONDS)
                                   .atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                                   .until(this::receiveDeletedEcho, echo -> echo != null);
        assertTrue(deleted.contains("doomed"),
                "the delete must announce the row as it was stored, not the caller's partial snapshot: " + deleted);
    }

    private void seed() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(CONTROLLER + "/seed")
                                                 .then()
                                                 .statusCode(200));
    }

    /** Empties the echo queue so a later assertion cannot read somebody else's message. */
    private void drainEchoQueue() {
        while (receiveEcho() != null) {
            // keep reading until the queue is empty
        }
    }

    private String receiveEcho() {
        try {
            return MessagingFacade.receiveFromQueue(ECHO_QUEUE, RECEIVE_TIMEOUT_MILLIS);
        } catch (RuntimeException nothingYet) {
            return null;
        }
    }

    private String receiveDeletedEcho() {
        try {
            return MessagingFacade.receiveFromQueue(DELETED_ECHO_QUEUE, RECEIVE_TIMEOUT_MILLIS);
        } catch (RuntimeException nothingYet) {
            return null;
        }
    }

    /**
     * Writes an outbox entry that is already due, exactly as a write whose in-process publish failed
     * would have left one.
     */
    private String plantStrandedEntry(String payload) throws Exception {
        String id = UUID.randomUUID()
                        .toString();
        Timestamp overdue = Timestamp.from(Instant.now()
                                                  .minus(Duration.ofMinutes(5)));
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                PreparedStatement statement = connection.prepareStatement("INSERT INTO \"" + OUTBOX_TABLE
                        + "\" (\"EVENT_ID\", \"EVENT_TOPIC\", \"EVENT_PAYLOAD\", \"EVENT_CREATED_AT\", \"EVENT_ATTEMPTS\", \"EVENT_NEXT_ATTEMPT_AT\") VALUES (?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, id);
            statement.setString(2, CREATED_TOPIC);
            statement.setString(3, payload);
            statement.setTimestamp(4, overdue);
            statement.setInt(5, 0);
            statement.setTimestamp(6, overdue);
            statement.executeUpdate();
        }
        return id;
    }

    private boolean entryIsGone(String id) throws Exception {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                PreparedStatement statement =
                        connection.prepareStatement("SELECT COUNT(*) FROM \"" + OUTBOX_TABLE + "\" WHERE \"EVENT_ID\" = ?")) {
            statement.setString(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) == 0;
            }
        }
    }

    private int countOutboxEntries() throws Exception {
        if (!outboxTableExists()) {
            return 0;
        }
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM \"" + OUTBOX_TABLE + "\"")) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private boolean outboxTableExists() throws Exception {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                ResultSet tables = connection.getMetaData()
                                             .getTables(null, null, OUTBOX_TABLE, null)) {
            return tables.next();
        }
    }

    /**
     * The fixture files go away with the Dirigible folder the base class wipes per test class; the
     * entity table would survive a local run against an unclean target and carry its rows into the next
     * one. The outbox table itself is deliberately left in place - it belongs to the tenant, not to
     * this test, and the store remembers that it exists.
     */
    @AfterEach
    void cleanUp() throws Exception {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS \"" + ENTITY_TABLE + "\"");
            if (outboxTableExists()) {
                statement.execute(
                        "DELETE FROM \"" + OUTBOX_TABLE + "\" WHERE \"EVENT_TOPIC\" IN ('" + CREATED_TOPIC + "', '" + DELETED_TOPIC + "')");
            }
        }
    }
}
