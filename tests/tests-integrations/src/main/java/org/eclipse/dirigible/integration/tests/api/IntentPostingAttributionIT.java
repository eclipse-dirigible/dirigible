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
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.security.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * A row a posting writes in reaction to a user's write is attributed to that user (#8228).
 *
 * <p>
 * The posting handler runs off the source's topic, on a broker thread that serves no request, so
 * the audit columns of the entry it created read {@code guest} - the fallback of a thread without a
 * user - while the source carried the real one. The outbox now records the user with the event, the
 * platform's producer stamps it on the message, and the listener binds it for the handler.
 *
 * <p>
 * Both deliveries are walked: the in-process one right after the write's commit, by a named
 * non-admin user (so the attribution cannot be mistaken for the IT's own admin), and the relay's
 * redelivery of an entry left in the outbox, as a broker outage would have left one - planted
 * against a payment written straight into its table, so that no other delivery of the same moment
 * exists.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentPostingAttributionIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "attribution";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String PAYMENTS = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api/payment/PaymentController";
    private static final String ENTRIES = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api/entry/EntryController";
    /** What the generated repository publishes a created payment on. */
    private static final String PAYMENT_CREATED_TOPIC = PROJECT + "-Payment-Payment";
    private static final String OUTBOX_TABLE = "DIRIGIBLE_EVENT_OUTBOX";
    private static final String PAYMENT_TABLE = "ATTRIBUTION_PAYMENT";
    private static final long TIMEOUT_SECONDS = 90;
    /** The posting handlers run off the source's topics, after the commit. */
    private static final long POSTING_TIMEOUT_SECONDS = 30;

    private static final String CLERK = "attribution-clerk";
    private static final String PASSWORD = "attribution-it-password";
    /** The user a stranded outbox entry names; the relay is the only thing that can deliver it. */
    private static final String RELAYED_USER = "attribution-relayed";
    /** A payment written without an event, so the planted one is its only delivery. */
    private static final int RELAYED_PAYMENT = 900001;

    private static final String INTENT = """
            name: attribution
            description: a posting's entry is attributed to the user whose payment raised it

            entities:
              - name: Payment
                audit: true
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2 }

              - name: EntryStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: Entry
                audit: true
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: Status,  kind: manyToOne, to: EntryStatus, function: EntityStatus, init: 1 }
                  - { name: Payment, kind: manyToOne, to: Payment }

              - name: EntryLine
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: debit,  type: decimal, precision: 18, scale: 2 }
                  - { name: credit, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Entry, kind: manyToOne, to: Entry, composition: true, required: true }

            postings:
              - name: paymentPosting
                event: { onCreate: Payment }
                creates: Entry
                backReference: Payment
                items:
                  - { debit: "Amount" }
                  - { credit: "Amount" }

            seeds:
              - name: entry-statuses
                entity: EntryStatus
                rows:
                  - { id: 1, name: Draft }
                  - { id: 2, name: Posted }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Autowired
    private Scheduler scheduler;

    @Test
    void the_entry_a_posting_writes_is_attributed_to_the_user_whose_payment_raised_it() throws Exception {
        writeIntent();
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();
        securityUtil.createUserInDefaultTenant(CLERK, PASSWORD, PROJECT + ".Payment.PaymentFullAccess");

        // The in-process delivery: the clerk's own write, handled on a broker thread.
        int payment = createPaymentAsClerk();
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(PAYMENTS + "/" + payment)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("CreatedBy", equalTo(CLERK)),
                TIMEOUT_SECONDS);
        assertEntryCreatedBy(payment, CLERK);

        // The relay's redelivery: an entry the in-process delivery could not hand over, left for the
        // relay job, whose thread has no user of its own.
        insertPaymentWithoutEvent();
        plantStrandedEvent("{\"Id\":" + RELAYED_PAYMENT + ",\"Amount\":7}");
        scheduler.triggerJob(JobKey.jobKey("EventOutboxRelayJob", "system"));
        assertEntryCreatedBy(RELAYED_PAYMENT, RELAYED_USER);
    }

    private int createPaymentAsClerk() {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body("{\"Amount\":10}")
                                                        .when()
                                                        .post(PAYMENTS)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                CLERK, PASSWORD, TIMEOUT_SECONDS);
        return id.get();
    }

    /**
     * The single entry the posting derived from the payment, read as admin: its creator is the point.
     */
    private void assertEntryCreatedBy(int payment, String user) {
        String ofPayment = "findAll { it.Payment == " + payment + " }";
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ENTRIES)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(ofPayment, hasSize(1))
                                                 .body(ofPayment + "[0].CreatedBy", equalTo(user)),
                POSTING_TIMEOUT_SECONDS);
    }

    private void insertPaymentWithoutEvent() throws Exception {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO \"" + PAYMENT_TABLE + "\" (\"PAYMENT_ID\", \"PAYMENT_AMOUNT\") VALUES (?, ?)")) {
            statement.setInt(1, RELAYED_PAYMENT);
            statement.setInt(2, 7);
            statement.executeUpdate();
        }
    }

    /** An outbox entry already due, as a write whose in-process publish failed would have left one. */
    private void plantStrandedEvent(String payload) throws Exception {
        Timestamp overdue = Timestamp.from(Instant.now()
                                                  .minus(Duration.ofMinutes(5)));
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                PreparedStatement statement = connection.prepareStatement("INSERT INTO \"" + OUTBOX_TABLE
                        + "\" (\"EVENT_ID\", \"EVENT_TOPIC\", \"EVENT_PAYLOAD\", \"EVENT_CREATED_AT\", \"EVENT_ATTEMPTS\","
                        + " \"EVENT_NEXT_ATTEMPT_AT\", \"EVENT_USER\") VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            statement.setString(1, UUID.randomUUID()
                                       .toString());
            statement.setString(2, PAYMENT_CREATED_TOPIC);
            statement.setString(3, payload);
            statement.setTimestamp(4, overdue);
            statement.setInt(5, 0);
            statement.setTimestamp(6, overdue);
            statement.setString(7, RELAYED_USER);
            statement.executeUpdate();
        }
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
            existing.setContent(INTENT.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, INTENT.getBytes(StandardCharsets.UTF_8));
        }
    }

    @AfterEach
    void cleanup() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT)
                                                 .then()
                                                 .statusCode(both(greaterThanOrEqualTo(200)).and(lessThan(300))));
        if (repository.hasCollection(PROJECT_PATH)) {
            repository.removeCollection(PROJECT_PATH);
        }
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
