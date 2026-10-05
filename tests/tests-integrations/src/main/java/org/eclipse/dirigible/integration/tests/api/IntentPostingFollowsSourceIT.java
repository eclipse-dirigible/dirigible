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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.logging.LogsAsserter;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import ch.qos.logback.classic.Level;

/**
 * An {@code onCreate} posting follows its source (dirigible #7634): an edit re-derives the entry, a
 * delete red-stornos it.
 *
 * <p>
 * A source with no status lifecycle - a booked payment - raises its posting's moment once, at the
 * insert. The amendment rewrite (#7071) is reached only by a second delivery of that moment, so an
 * edit left the DRAFT entry with the insert's figures, and a delete left the posted entry booking
 * money the source no longer carried, with no storno: {@code reverses:} bound only a status
 * transition, which such a source does not have.
 *
 * <p>
 * Walked as the issue walks it, cross-model like every payment-to-journal posting in the fleet:
 *
 * <ol>
 * <li>an edit while the entry is DRAFT rewrites it in place - the same entry, the new amounts;
 * <li>an edit after the entry is POSTED is reported and left alone, as an amended re-issue is;
 * <li>the delete succeeds (the back-reference declares {@code whenTargetDeleted: keep}) and the
 * posted entry is red-stornoed - by the amounts it was POSTED with, not the source's last edit,
 * since the storno must net the original to zero.
 * </ol>
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentPostingFollowsSourceIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    /** Owns the source. Its project name differs from the model alias on purpose. */
    private static final String OWNER = "follow-pay";
    /** Posts the source into its journal. */
    private static final String CONSUMER = "follow-ledger";
    private static final String OWNER_MODEL = "pay";
    private static final String CONSUMER_MODEL = "ledger";

    private static final String PAYMENTS = "/services/java/" + OWNER + "/gen/" + OWNER_MODEL + "/api/payment/PaymentController";
    private static final String ENTRIES = "/services/java/" + CONSUMER + "/gen/" + CONSUMER_MODEL + "/api/entry/EntryController";
    private static final String LINES = "/services/java/" + CONSUMER + "/gen/" + CONSUMER_MODEL + "/api/entry/EntryLineController";
    private static final String TRANSITIONS = "/services/java/" + CONSUMER + "/gen/events/" + CONSUMER_MODEL + "/";
    private static final long TIMEOUT_SECONDS = 90;
    /** The posting handlers run off the source's topics, after the commit. */
    private static final long POSTING_TIMEOUT_SECONDS = 30;

    private static final String OWNER_INTENT = """
            name: pay
            description: posting-follows-source fixture - a payment with no status lifecycle

            entities:
              - name: Payment
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2 }
            """;

    private static final String CONSUMER_INTENT = """
            name: ledger
            description: posting-follows-source fixture - the journal a payment is booked into

            uses:
              - { model: pay, project: follow-pay }

            entities:
              - name: EntryStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: Entry
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: Status,  kind: manyToOne, to: EntryStatus, function: EntityStatus, init: 1 }
                  # The link outlives the payment: the storno of a deleted payment follows it.
                  - { name: Payment, kind: manyToOne, to: Payment, model: pay, whenTargetDeleted: keep }
                  - { name: Storno,  kind: manyToOne, to: Entry }

              - name: EntryLine
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: debit,  type: decimal, precision: 18, scale: 2 }
                  - { name: credit, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Entry, kind: manyToOne, to: Entry, composition: true, required: true }

            transitions:
              # What "somebody has acted on the entry" is, in this fixture: the accountant posts it.
              - { name: PostEntry, forEntity: Entry, from: [1], setStatus: 2, label: Post }

            postings:
              - name: paymentPosting
                event: { onCreate: Payment, model: pay }
                creates: Entry
                backReference: Payment
                items:
                  - { debit: "Amount" }
                  - { credit: "Amount" }
              - name: paymentStorno
                event: { onDelete: Payment, model: pay }
                reverses: paymentPosting
                storno: Storno

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

    /**
     * The generated handlers log through the client SDK, which prefixes their names with {@code app.};
     * attached in {@code @BeforeEach} because Spring re-initializes logback while it starts the
     * context.
     */
    private LogsAsserter postingLogs;

    @BeforeEach
    void attachLogsAsserter() {
        postingLogs = new LogsAsserter("app.gen.events", Level.ERROR);
    }

    @Test
    void an_on_create_posting_follows_the_edit_and_the_delete_of_its_source() {
        writeIntent(OWNER, OWNER_INTENT);
        generateProject(OWNER);
        writeIntent(CONSUMER, CONSUMER_INTENT);
        generateProject(CONSUMER);
        publishProject(OWNER);
        publishProject(CONSUMER);
        synchronizationProcessor.forceProcessSynchronizers();

        int payment = create(PAYMENTS, "{\"Amount\":10}");
        int entry = onlyEntryOf(payment, false);
        awaitLines(entry, 10.0f);

        // 1. Edited while the entry is DRAFT: the same entry now says what the payment says.
        update(PAYMENTS + "/" + payment, "{\"Id\":" + payment + ",\"Amount\":12}");
        awaitLines(entry, 12.0f);
        assertEquals(entry, onlyEntryOf(payment, false), "an edited source must rewrite ITS post, never open a second one");

        // 2. Edited after the accountant posted the entry: reported, not overwritten.
        transition("PostEntry", entry);
        update(PAYMENTS + "/" + payment, "{\"Id\":" + payment + ",\"Amount\":15}");
        Awaitility.await()
                  .pollInterval(1, TimeUnit.SECONDS)
                  .atMost(POSTING_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .until(() -> postingLogs.containsMessage("was NOT rewritten", Level.ERROR));
        awaitLines(entry, 12.0f);

        // 3. Deleted: the delete is not refused by the entry that books it, and the posted entry is
        // red-stornoed by what it was POSTED with - 12, not the 15 of the last edit.
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete(PAYMENTS + "/" + payment)
                                                 .then()
                                                 .statusCode(both(greaterThanOrEqualTo(200)).and(lessThan(300))));
        int storno = onlyEntryOf(payment, true);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ENTRIES + "/" + storno)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Storno", equalTo(entry)),
                POSTING_TIMEOUT_SECONDS);
        awaitLines(storno, -12.0f);
        // The original is left as it was posted - the storno corrects it, nothing rewrites it.
        assertEquals(entry, onlyEntryOf(payment, false));
        awaitLines(entry, 12.0f);
    }

    /**
     * The single entry the payment carries - the original ({@code storno} false) or its red storno -
     * asserting there is exactly one. Picked out here: the back-reference is a plain association, so
     * the list endpoint ignores {@code ?Payment=}.
     */
    private int onlyEntryOf(int payment, boolean storno) {
        String ofPayment = "findAll { it.Payment == " + payment + " && it.Storno " + (storno ? "!=" : "==") + " null }";
        AtomicInteger entry = new AtomicInteger();
        restAssuredExecutor.execute(() -> entry.set(given().when()
                                                           .get(ENTRIES)
                                                           .then()
                                                           .statusCode(200)
                                                           .body(ofPayment, hasSize(1))
                                                           .extract()
                                                           .jsonPath()
                                                           .getInt(ofPayment + "[0].Id")),
                POSTING_TIMEOUT_SECONDS);
        return entry.get();
    }

    /** The balanced pair the posting derives: one debit line and one credit line, both the amount. */
    private void awaitLines(int entry, float amount) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(LINES + "?Entry=" + entry)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("", hasSize(2))
                                                 // Parenthesized: a bare "== -12.0" does not parse as a GPath comparison.
                                                 .body("findAll { it.Debit == (" + amount + ") }.size()", equalTo(1))
                                                 .body("findAll { it.Credit == (" + amount + ") }.size()", equalTo(1)),
                POSTING_TIMEOUT_SECONDS);
    }

    private void transition(String name, int id) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + id + "}")
                                                 .when()
                                                 .post(TRANSITIONS + name + "Transition/run")
                                                 .then()
                                                 .statusCode(200),
                TIMEOUT_SECONDS);
    }

    private void update(String path, String body) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(body)
                                                 .when()
                                                 .put(path)
                                                 .then()
                                                 .statusCode(200));
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

    private void generateProject(String project) {
        AtomicReference<List<Map<String, Object>>> plan = new AtomicReference<>();
        restAssuredExecutor.execute(() -> plan.set(given().when()
                                                          .post("/services/ide/intent/generate?workspace=" + WORKSPACE + "&project="
                                                                  + project + "&path=app.intent")
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

    private void publishProject(String project) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + project + "/")
                                                 .then()
                                                 .statusCode(200));
    }

    private void writeIntent(String project, String yaml) {
        String path = projectPath(project) + "/app.intent";
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(yaml.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, yaml.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String projectPath(String project) {
        return IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + project;
    }

    @AfterEach
    void cleanup() {
        for (String project : List.of(CONSUMER, OWNER)) {
            restAssuredExecutor.execute(() -> given().when()
                                                     .delete("/services/ide/publisher/" + WORKSPACE + "/" + project)
                                                     .then()
                                                     .statusCode(both(greaterThanOrEqualTo(200)).and(lessThan(300))));
            if (repository.hasCollection(projectPath(project))) {
                repository.removeCollection(projectPath(project));
            }
        }
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
