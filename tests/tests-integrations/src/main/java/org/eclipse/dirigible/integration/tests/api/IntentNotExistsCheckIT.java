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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
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

/**
 * dirigible #8002 / #8623: {@code checks: kind: notExists} - a move into a status is refused WHILE
 * a row of another entity matches - end to end on generated, published projects.
 *
 * <p>
 * A month must not be closed while a DRAFT journal entry is dated inside it: the entries belong to
 * the period by date window, not by a foreign key, and a transition guard could only read the
 * period's own fields, so the close was a 200. A deal must not be marked lost while one of its
 * quotations is accepted or ordered: the quotation's model uses the opportunity's, so the
 * opportunity's model counts the quotations by name, at runtime. Both checks are gated on the
 * status the button writes, and both refuse it with the authored message (400), leaving the status
 * as it was.
 */
class IntentNotExistsCheckIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String JOURNAL = "notexists-journal";
    private static final String OPPORTUNITIES = "notexists-opportunities";
    private static final String QUOTATIONS = "notexists-quotations";

    private static final String JOURNAL_INTENT = """
            name: journal
            description: notExists fixture - a period is not closed while a draft entry is dated inside it

            entities:
              - name: PeriodStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 50 }
              - name: EntryStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 50 }
              - name: FiscalPeriod
                checks:
                  - kind: notExists
                    entity: JournalEntry
                    where: ["Status == DRAFT", "date >= fromDate", "date <= toDate"]
                    status: CLOSED
                    message: "{count} draft entries are dated in this period - post or delete them before closing it"
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: name,     type: string, required: true, length: 50 }
                  - { name: fromDate, type: date, required: true }
                  - { name: toDate,   type: date, required: true }
                relations:
                  - { name: Status, kind: manyToOne, to: PeriodStatus, function: EntityStatus, init: OPEN }
              - name: JournalEntry
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: memo, type: string, length: 100 }
                  - { name: date, type: date, required: true }
                relations:
                  - { name: Status, kind: manyToOne, to: EntryStatus, function: EntityStatus, init: DRAFT }

            transitions:
              - { name: ClosePeriod, forEntity: FiscalPeriod, from: [OPEN], setStatus: CLOSED, label: Close }
              - { name: PostEntry,   forEntity: JournalEntry, from: [DRAFT], setStatus: POSTED, label: Post }
              - { name: CancelEntry, forEntity: JournalEntry, from: [DRAFT], setStatus: CANCELLED, label: Cancel }

            seeds:
              - name: period-statuses
                entity: PeriodStatus
                rows:
                  - { id: 1, name: OPEN }
                  - { id: 2, name: CLOSED }
              - name: entry-statuses
                entity: EntryStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: POSTED }
                  - { id: 3, name: CANCELLED }
            """;

    private static final String OPPORTUNITIES_INTENT = """
            name: opportunities
            description: notExists fixture - a deal is not marked lost while one of its quotations is accepted

            entities:
              - name: OpportunityStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 50 }
              - name: Opportunity
                checks:
                  # The quotations model uses this one, so it is not used back: its rows are counted by name.
                  - { kind: notExists, entity: Quotation, model: quotations, match: { Opportunity: id },
                      where: "Status in [2, 3]", status: LOST,
                      message: "A deal with an accepted or ordered quotation is not marked lost" }
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }
                relations:
                  - { name: Status, kind: manyToOne, to: OpportunityStatus, function: EntityStatus, init: OPEN }

            transitions:
              - { name: MarkWon,  forEntity: Opportunity, from: [OPEN], setStatus: WON,  label: Won }
              - { name: MarkLost, forEntity: Opportunity, from: [OPEN], setStatus: LOST, label: Lost }

            seeds:
              - name: opportunity-statuses
                entity: OpportunityStatus
                rows:
                  - { id: 1, name: OPEN }
                  - { id: 2, name: WON }
                  - { id: 3, name: LOST }
            """;

    private static final String QUOTATIONS_INTENT = """
            name: quotations
            description: notExists fixture - quotations of an opportunity

            uses:
              - { model: opportunities, project: notexists-opportunities }

            entities:
              - name: QuotationStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 50 }
              - name: Quotation
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Opportunity, kind: manyToOne, to: Opportunity, model: opportunities, required: true }
                  - { name: Status, kind: manyToOne, to: QuotationStatus, function: EntityStatus, init: DRAFT }

            seeds:
              - name: quotation-statuses
                entity: QuotationStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: ACCEPTED }
                  - { id: 3, name: ORDERED }
                  - { id: 4, name: REJECTED }
            """;

    @Autowired
    private IRepository repository;
    @Autowired
    private RestAssuredExecutor restAssuredExecutor;
    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_status_move_is_refused_while_a_matching_row_of_another_entity_exists() {
        for (String[] project : List.of(new String[] {JOURNAL, JOURNAL_INTENT}, new String[] {OPPORTUNITIES, OPPORTUNITIES_INTENT},
                new String[] {QUOTATIONS, QUOTATIONS_INTENT})) {
            writeResource(project[0], "app.intent", project[1]);
            generateProject(project[0]);
        }
        publishProject(JOURNAL);
        publishProject(OPPORTUNITIES);
        publishProject(QUOTATIONS);
        synchronizationProcessor.forceProcessSynchronizers();

        aPeriodIsNotClosedWhileADraftEntryIsDatedInsideIt();
        aDealIsNotMarkedLostWhileOneOfItsQuotationsIsAccepted();
    }

    /** dirigible #8002: the date-window form, no foreign key between the two. */
    private void aPeriodIsNotClosedWhileADraftEntryIsDatedInsideIt() {
        String api = "/services/java/" + JOURNAL + "/gen/journal/api";
        String periods = api + "/fiscalperiod/FiscalPeriodController";
        String entries = api + "/journalentry/JournalEntryController";
        String events = "/services/java/" + JOURNAL + "/gen/events/journal/";

        int january = create(periods, "{\"Name\":\"2026-01\",\"FromDate\":\"2026-01-01\",\"ToDate\":\"2026-01-31\"}");
        int draft = create(entries, "{\"Memo\":\"accrual\",\"Date\":\"2026-01-15\"}");
        // Neither of these blocks the close: a cancelled entry inside the month, a draft one outside it.
        int cancelled = create(entries, "{\"Memo\":\"mistake\",\"Date\":\"2026-01-20\"}");
        runTransition(events + "CancelEntryTransition/run", cancelled, 200);
        create(entries, "{\"Memo\":\"next month\",\"Date\":\"2026-02-01\"}");

        // The issue's reproduction: the close was a 200 with the draft entry inside the month.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + january + "}")
                                                 .when()
                                                 .post(events + "ClosePeriodTransition/run")
                                                 .then()
                                                 .statusCode(400)
                                                 .body(containsString(
                                                         "1 draft entries are dated in this period - post or delete them before closing it")),
                60);
        assertStatus(periods + "/" + january, 1);

        // Once the entry is posted, the month closes.
        runTransition(events + "PostEntryTransition/run", draft, 200);
        runTransition(events + "ClosePeriodTransition/run", january, 200);
        assertStatus(periods + "/" + january, 2);
    }

    /** dirigible #8623: the match form, on a model that uses this one - counted by name at runtime. */
    private void aDealIsNotMarkedLostWhileOneOfItsQuotationsIsAccepted() {
        String deals = "/services/java/" + OPPORTUNITIES + "/gen/opportunities/api/opportunity/OpportunityController";
        String quotations = "/services/java/" + QUOTATIONS + "/gen/quotations/api/quotation/QuotationController";
        String events = "/services/java/" + OPPORTUNITIES + "/gen/events/opportunities/";

        int deal = create(deals, "{\"Name\":\"Apollo\"}");
        int other = create(deals, "{\"Name\":\"Gemini\"}");
        create(quotations, "{\"Opportunity\":" + deal + ",\"Amount\":100,\"Status\":3}");
        // A quotation in a status that does not match, and one of ANOTHER deal, do not block.
        create(quotations, "{\"Opportunity\":" + other + ",\"Amount\":100,\"Status\":4}");

        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + deal + "}")
                                                 .when()
                                                 .post(events + "MarkLostTransition/run")
                                                 .then()
                                                 .statusCode(400)
                                                 .body(containsString("A deal with an accepted or ordered quotation is not marked lost")),
                60);
        assertStatus(deals + "/" + deal, 1);
        // Mark Won stays possible with the same quotation, and the other deal is marked lost.
        runTransition(events + "MarkLostTransition/run", other, 200);
        assertStatus(deals + "/" + other, 3);
        runTransition(events + "MarkWonTransition/run", deal, 200);
        assertStatus(deals + "/" + deal, 2);
    }

    private void runTransition(String endpoint, int id, int status) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + id + "}")
                                                 .when()
                                                 .post(endpoint)
                                                 .then()
                                                 .statusCode(status),
                60);
    }

    private void assertStatus(String record, int status) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(record)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(status)));
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
                60);
        return id.get();
    }

    /**
     * Generate the model files from the intent and drive model-to-code from the generate response's own
     * plan.
     */
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

    private void writeResource(String project, String file, String content) {
        String path = projectPath(project) + "/" + file;
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(content.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String projectPath(String project) {
        return IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + project;
    }

    @AfterEach
    void cleanup() {
        for (String project : List.of(QUOTATIONS, OPPORTUNITIES, JOURNAL)) {
            restAssuredExecutor.execute(() -> given().when()
                                                     .delete("/services/ide/publisher/" + WORKSPACE + "/" + project)
                                                     .then()
                                                     .statusCode(both(greaterThanOrEqualTo(200)).and(lessThan(300))));
            if (repository.hasCollection(projectPath(project))) {
                repository.removeCollection(projectPath(project));
            }
        }
    }
}
