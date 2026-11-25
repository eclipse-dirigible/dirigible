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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * A posting INTO a record locked by the period register of ANOTHER model (dirigible #8755).
 *
 * <p>
 * Since #8687 the cash entry's own repository refuses a write dated inside the journal's closed
 * period, but the posting that writes it from an issued receipt did not ask first: it ran into the
 * repository's refusal inside the listener, threw, and was redelivered against a period that stays
 * closed for days. A posting into a target locked by a register of its own model asks the register
 * BEFORE it writes, logs which document waits on which period and acks the message (#7703). The
 * foreign-locked target now gets the same pre-check, through {@code PeriodRegisters.isClosed}: an
 * issued March receipt is left unposted with that warning and no listener failure, and an April
 * receipt still posts.
 *
 * <p>
 * And the same reopen sweep: the journal relays the reopening of its period onto
 * {@code PeriodRegisters.reopenedTopic}, which the cash model's sweep binds without naming anything
 * of the journal but its model and register, so reopening March books the waiting receipt. The
 * flattened {@code posts:} construct gets both halves too (dirigible #8772) - a row into a target
 * locked by the journal's register, and one into a target locked by a register of its own model.
 */
class IntentCrossModelPostingPeriodIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String JOURNAL = "cppjournal";
    private static final String CASH = "cppcash";
    private static final String PERIODS = api(JOURNAL) + "/fiscalperiod/FiscalPeriodController";
    private static final String RECEIPTS = api(CASH) + "/receipt/ReceiptController";
    private static final String ENTRIES = api(CASH) + "/cashentry/CashEntryController";
    private static final String BOOK_ROWS = api(CASH) + "/cashbookrow/CashBookRowController";
    private static final String ADJUSTMENTS = api(JOURNAL) + "/adjustment/AdjustmentController";
    private static final String ADJUSTMENT_ROWS = api(JOURNAL) + "/adjustmentrow/AdjustmentRowController";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String JOURNAL_YAML = """
            name: cppjournal
            description: the ledger's period register

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

              - name: AdjustmentStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: Adjustment
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: date,   type: date, required: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Status, kind: manyToOne, to: AdjustmentStatus, function: EntityStatus, init: 1 }

              - name: AdjustmentRow
                immutableInPeriod: { period: FiscalPeriod, date: date }
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: date,   type: date }
                  - { name: amount, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Adjustment, kind: manyToOne, to: Adjustment }

            transitions:
              - { name: ClosePeriod,     forEntity: FiscalPeriod, from: [1], setStatus: 2, label: Close }
              - { name: ReopenPeriod,    forEntity: FiscalPeriod, from: [2], setStatus: 1, label: Reopen }
              - { name: IssueAdjustment, forEntity: Adjustment,   from: [1], setStatus: 2, label: Issue }

            posts:
              - name: adjustmentPost
                forEntity: Adjustment
                event: 2
                into: AdjustmentRow
                idempotentBy: Adjustment
                set: { date: source.date, amount: source.amount }

            seeds:
              - name: period-statuses
                entity: PeriodStatus
                rows:
                  - { id: 1, name: OPEN }
                  - { id: 2, name: CLOSED }
              - name: adjustment-statuses
                entity: AdjustmentStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: ISSUED }
            """;

    private static final String CASH_YAML = """
            name: cppcash
            description: a posting into a record locked by the ledger's period, without using the ledger

            entities:
              - name: ReceiptStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: Receipt
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: date,   type: date, required: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Status, kind: manyToOne, to: ReceiptStatus, function: EntityStatus, init: 1 }

              - name: CashEntry
                immutableInPeriod: { period: FiscalPeriod, model: cppjournal, date: date }
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: date, type: date }
                relations:
                  - { name: Receipt, kind: manyToOne, to: Receipt }

              - name: CashEntryLine
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: CashEntry, kind: manyToOne, to: CashEntry, composition: true, required: true }

              - name: CashBookRow
                immutableInPeriod: { period: FiscalPeriod, model: cppjournal, date: date }
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: date,   type: date }
                  - { name: amount, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Receipt, kind: manyToOne, to: Receipt }

            transitions:
              - { name: IssueReceipt, forEntity: Receipt, from: [1], setStatus: 2, label: Issue }

            postings:
              - name: receiptPosting
                event: { onTransition: Receipt, when: "Status == 2" }
                creates: CashEntry
                backReference: Receipt
                map: { date: date }
                items:
                  - { amount: "Amount" }

            posts:
              - name: receiptBookPost
                forEntity: Receipt
                event: 2
                into: CashBookRow
                idempotentBy: Receipt
                set: { date: source.date, amount: source.amount }

            seeds:
              - name: receipt-statuses
                entity: ReceiptStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: ISSUED }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_posting_into_a_closed_period_waits_without_failing_and_is_booked_when_the_period_reopens() {
        deploy(JOURNAL, JOURNAL_YAML);
        int period = create(PERIODS, "{\"StartDate\":\"2026-03-01\",\"EndDate\":\"2026-03-31\",\"Status\":1}");
        transition(JOURNAL, "ClosePeriod", period);
        deploy(CASH, CASH_YAML);

        // The generated handlers log under the applications' root logger, "app." (LogFacade), and that
        // logger may be quieter than WARN: the level is set for the run and restored after it.
        Logger cashLog = (Logger) LoggerFactory.getLogger("app.gen.events." + CASH);
        Logger journalLog = (Logger) LoggerFactory.getLogger("app.gen.events." + JOURNAL);
        Level previousCashLevel = cashLog.getLevel();
        Level previousJournalLevel = journalLog.getLevel();
        cashLog.setLevel(Level.INFO);
        journalLog.setLevel(Level.INFO);
        ListAppender<ILoggingEvent> captured = new ListAppender<>();
        captured.start();
        cashLog.addAppender(captured);
        journalLog.addAppender(captured);
        try {
            int march = create(RECEIPTS, "{\"Date\":\"2026-03-10\",\"Amount\":10}");
            transition(CASH, "IssueReceipt", march);
            int marchAdjustment = create(ADJUSTMENTS, "{\"Date\":\"2026-03-12\",\"Amount\":3}");
            transition(JOURNAL, "IssueAdjustment", marchAdjustment);
            // Asked before the write: the posting (#8755) and both posts: rows (#8772) wait on the closed
            // period, each named in a warning, and the message is acked - no listener failure, no
            // redelivery into the target's refusal.
            Awaitility.await()
                      .atMost(Duration.ofSeconds(TIMEOUT_SECONDS))
                      .until(() -> count(captured, Level.WARN, "stays unposted") >= 3);
            assertCount(ENTRIES, 0);
            assertCount(BOOK_ROWS, 0);
            assertCount(ADJUSTMENT_ROWS, 0);

            // An April receipt is not in the closed period: it posts.
            int april = create(RECEIPTS, "{\"Date\":\"2026-04-02\",\"Amount\":7}");
            transition(CASH, "IssueReceipt", april);
            assertCount(ENTRIES, 1);
            assertCount(BOOK_ROWS, 1);

            // Reopening March books what it refused: the journal relays the reopening to the cash model's
            // sweeps, and its own posts: row is swept off the register's topic.
            transition(JOURNAL, "ReopenPeriod", period);
            assertCount(ENTRIES, 2);
            assertCount(BOOK_ROWS, 2);
            assertCount(ADJUSTMENT_ROWS, 1);
            assertTrue(count(captured, Level.ERROR, "") == 0, "the closed period must not fail a posting listener: " + messages(captured));
        } finally {
            cashLog.detachAppender(captured);
            journalLog.detachAppender(captured);
            cashLog.setLevel(previousCashLevel);
            journalLog.setLevel(previousJournalLevel);
        }
    }

    private void assertCount(String controller, int expected) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(controller + "/count")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("count", equalTo(expected)),
                TIMEOUT_SECONDS);
    }

    private static long count(ListAppender<ILoggingEvent> captured, Level level, String text) {
        return snapshot(captured).stream()
                                 .filter(event -> event.getLevel()
                                                       .equals(level)
                                         && event.getFormattedMessage()
                                                 .contains(text))
                                 .count();
    }

    private static List<String> messages(ListAppender<ILoggingEvent> captured) {
        List<String> messages = new ArrayList<>();
        for (ILoggingEvent event : snapshot(captured)) {
            messages.add(event.getLevel() + " " + event.getFormattedMessage());
        }
        return messages;
    }

    private static List<ILoggingEvent> snapshot(ListAppender<ILoggingEvent> captured) {
        synchronized (captured.list) {
            return new ArrayList<>(captured.list);
        }
    }

    private void transition(String project, String name, int id) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + id + "}")
                                                 .when()
                                                 .post("/services/java/" + project + "/gen/events/" + project + "/" + name
                                                         + "Transition/run")
                                                 .then()
                                                 .statusCode(200),
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

    private void deploy(String project, String yaml) {
        String path = projectPath(project) + "/app.intent";
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(yaml.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, yaml.getBytes(StandardCharsets.UTF_8));
        }
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
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + project + "/")
                                                 .then()
                                                 .statusCode(200));
        synchronizationProcessor.forceProcessSynchronizers();
    }

    private static String api(String project) {
        return "/services/java/" + project + "/gen/" + project + "/api";
    }

    private static String projectPath(String project) {
        return IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + project;
    }

    @AfterEach
    void cleanup() {
        for (String project : List.of(CASH, JOURNAL)) {
            restAssuredExecutor.execute(() -> given().when()
                                                     .delete("/services/ide/publisher/" + WORKSPACE + "/" + project)
                                                     .then()
                                                     .statusCode(greaterThanOrEqualTo(200)));
            if (repository.hasCollection(projectPath(project))) {
                repository.removeCollection(projectPath(project));
            }
        }
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
