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
 */
class IntentCrossModelPostingPeriodIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String JOURNAL = "cppjournal";
    private static final String CASH = "cppcash";
    private static final String PERIODS = api(JOURNAL) + "/fiscalperiod/FiscalPeriodController";
    private static final String RECEIPTS = api(CASH) + "/receipt/ReceiptController";
    private static final String ENTRIES = api(CASH) + "/cashentry/CashEntryController";
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

            transitions:
              - { name: ClosePeriod,  forEntity: FiscalPeriod, from: [1], setStatus: 2, label: Close }

            seeds:
              - name: period-statuses
                entity: PeriodStatus
                rows:
                  - { id: 1, name: OPEN }
                  - { id: 2, name: CLOSED }
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
    void a_posting_into_a_target_locked_by_another_models_closed_period_waits_without_failing() {
        deploy(JOURNAL, JOURNAL_YAML);
        int period = create(PERIODS, "{\"StartDate\":\"2026-03-01\",\"EndDate\":\"2026-03-31\",\"Status\":1}");
        transition(JOURNAL, "ClosePeriod", period);
        deploy(CASH, CASH_YAML);

        // The generated handlers log under the applications' root logger, "app." (LogFacade), and that
        // logger may be quieter than WARN: the level is set for the run and restored after it.
        Logger postingLog = (Logger) LoggerFactory.getLogger("app.gen.events." + CASH);
        Level previousLevel = postingLog.getLevel();
        postingLog.setLevel(Level.INFO);
        ListAppender<ILoggingEvent> captured = new ListAppender<>();
        captured.start();
        postingLog.addAppender(captured);
        try {
            int march = create(RECEIPTS, "{\"Date\":\"2026-03-10\",\"Amount\":10}");
            transition(CASH, "IssueReceipt", march);
            // Asked before the write: the receipt waits on the closed period, named in a warning, and
            // the message is acked - no listener failure, no redelivery into the target's refusal.
            Awaitility.await()
                      .atMost(Duration.ofSeconds(TIMEOUT_SECONDS))
                      .until(() -> logged(captured, Level.WARN, "stays unposted"));

            // An April receipt is not in the closed period: it posts.
            int april = create(RECEIPTS, "{\"Date\":\"2026-04-02\",\"Amount\":7}");
            transition(CASH, "IssueReceipt", april);
            restAssuredExecutor.execute(() -> given().when()
                                                     .get(ENTRIES + "/count")
                                                     .then()
                                                     .statusCode(200)
                                                     .body("count", equalTo(1)),
                    TIMEOUT_SECONDS);
            assertTrue(!logged(captured, Level.ERROR, ""), "the closed period must not fail the posting listener: " + messages(captured));
        } finally {
            postingLog.detachAppender(captured);
            postingLog.setLevel(previousLevel);
        }
    }

    private static boolean logged(ListAppender<ILoggingEvent> captured, Level level, String text) {
        for (ILoggingEvent event : snapshot(captured)) {
            if (event.getLevel()
                     .equals(level)
                    && event.getFormattedMessage()
                            .contains(text)) {
                return true;
            }
        }
        return false;
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
