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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

/**
 * A {@code reversals:} action (issue #7886) reverses a posted journal entry at runtime, through the
 * published application: the generated create-from compiles, creates the negated twin with every
 * line on its own side, links the two both ways past the original's {@code immutableWhen}, nets the
 * pair to zero, and refuses a second reversal, the reversal of a reversal, a draft, and a twin
 * dated into a CLOSED period.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentReversalIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "reversal";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String EVENTS = "/services/java/" + PROJECT + "/gen/events/" + PROJECT;
    private static final String REVERSE = EVENTS + "/ReverseEntryGenerate/run";
    private static final String POST = EVENTS + "/PostEntryTransition/run";
    private static final String ENTRIES = API + "/entry/EntryController";
    private static final String LINES = API + "/entry/EntryItemController";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String INTENT_YAML = """
            name: reversal
            description: manual Reverse of a posted entry - the negated twin, linked both ways

            entities:
              - name: EntryStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string,  required: true, length: 100 }

              - name: PeriodStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string,  required: true, length: 100 }

              - name: FiscalPeriod
                period: { start: startDate, end: endDate, closedWhen: "Status == CLOSED" }
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: name,      type: string, length: 100 }
                  - { name: startDate, type: date, required: true }
                  - { name: endDate,   type: date, required: true }
                relations:
                  - { name: Status, kind: manyToOne, to: PeriodStatus, function: EntityStatus, init: 1 }

              - name: Entry
                immutableWhen: "Status == POSTED"
                immutableInPeriod: { period: FiscalPeriod, date: date }
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: date, type: date, required: true }
                  - { name: note, type: string, length: 200 }
                relations:
                  - { name: Status,     kind: manyToOne, to: EntryStatus, function: EntityStatus, init: 1 }
                  - { name: Storno,     kind: manyToOne, to: Entry }
                  - { name: ReversedBy, kind: manyToOne, to: Entry }

              - name: EntryItem
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: debit,  type: decimal, precision: 18, scale: 2 }
                  - { name: credit, type: decimal, precision: 18, scale: 2 }
                  - { name: rate,   type: decimal, precision: 18, scale: 6 }
                  - { name: note,   type: string, length: 200 }
                relations:
                  - { name: Entry, kind: manyToOne, to: Entry, composition: true, required: true }

            seeds:
              - name: entry-statuses
                entity: EntryStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: POSTED }
              - name: period-statuses
                entity: PeriodStatus
                rows:
                  - { id: 1, name: OPEN }
                  - { id: 2, name: CLOSED }

            transitions:
              - name: PostEntry
                forEntity: Entry
                from: [DRAFT]
                setStatus: POSTED

            reversals:
              - name: reverseEntry
                forEntity: Entry
                when: "Status == POSTED"
                label: Reverse
                link: Storno
                backLink: ReversedBy
                negate: [debit, credit]
                defaults: { date: now }
                refuse:
                  - { when: "ReversedBy != null", message: "This entry is already reversed" }
                  - { when: "Storno != null", message: "A reversal cannot be reversed - reverse the original entry" }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_posted_entry_is_reversed_by_its_negated_twin_linked_both_ways() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        YearMonth month = YearMonth.now();
        int period = create(API + "/fiscalperiod/FiscalPeriodController", "{\"Name\":\"" + month + "\",\"StartDate\":\"" + month.atDay(1)
                + "\",\"EndDate\":\"" + month.atEndOfMonth() + "\",\"Status\":1}");
        LocalDate now = LocalDate.now();
        String today = now.toString();

        int original = postedEntry(today, "accrual");

        // A draft is not offered the action, and refused it when called anyway.
        int draft = create(ENTRIES, "{\"Date\":\"" + today + "\",\"Note\":\"draft\"}");
        reverse(draft, 409, "current status is [1]");

        // Reverse the posted entry: the twin is a DRAFT dated today, pointing at the original.
        int twin = reverse(original);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ENTRIES + "/" + twin)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Storno", equalTo(original))
                                                 .body("Status", equalTo(1))
                                                 .body("Date", equalTo(today))
                                                 .body("Note", equalTo("accrual"))
                                                 .body("ReversedBy", nullValue()),
                TIMEOUT_SECONDS);
        // Every line is copied onto its own side, the amount negated; the rate is copied as it stands.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(LINES + "?Entry=" + twin)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("", hasSize(2))
                                                 .body("Debit", equalTo(java.util.Arrays.asList(-100.0f, null)))
                                                 .body("Credit", equalTo(java.util.Arrays.asList(null, -100.0f)))
                                                 .body("Rate", equalTo(List.of(1.5f, 1.5f)))
                                                 .body("Note", equalTo(List.of("expense", "liability"))),
                TIMEOUT_SECONDS);
        // The original stays as it was, and names its reversal - written past its immutableWhen.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ENTRIES + "/" + original)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("ReversedBy", equalTo(twin))
                                                 .body("Status", equalTo(2)),
                TIMEOUT_SECONDS);
        // The pair nets to zero on both sides once the twin is posted.
        post(twin);
        assertEquals(0.0, sum(original, "Debit") + sum(twin, "Debit"), 0.0001);
        assertEquals(0.0, sum(original, "Credit") + sum(twin, "Credit"), 0.0001);

        // A second Reverse of the same entry and a Reverse of the reversal are refused by name.
        reverse(original, 409, "This entry is already reversed");
        reverse(twin, 409, "A reversal cannot be reversed - reverse the original entry");

        // A twin dated into a CLOSED period is refused by the period lock, and nothing of it remains.
        int another = postedEntry(today, "rent");
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + period + ",\"Name\":\"" + month + "\",\"StartDate\":\"" + month.atDay(1)
                                                         + "\",\"EndDate\":\"" + month.atEndOfMonth() + "\",\"Status\":2}")
                                                 .when()
                                                 .put(API + "/fiscalperiod/FiscalPeriodController/" + period)
                                                 .then()
                                                 .statusCode(200));
        reverse(another, 400, "The FiscalPeriod covering this date is closed");
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ENTRIES + "/" + another)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("ReversedBy", nullValue()),
                TIMEOUT_SECONDS);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ENTRIES)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Storno", not(hasItem(another))),
                TIMEOUT_SECONDS);
    }

    /** A POSTED entry dated {@code date}: a debit line and a credit line of 100 each. */
    private int postedEntry(String date, String note) {
        int entry = create(ENTRIES, "{\"Date\":\"" + date + "\",\"Note\":\"" + note + "\"}");
        create(LINES, "{\"Entry\":" + entry + ",\"Debit\":100,\"Rate\":1.5,\"Note\":\"expense\"}");
        create(LINES, "{\"Entry\":" + entry + ",\"Credit\":100,\"Rate\":1.5,\"Note\":\"liability\"}");
        post(entry);
        return entry;
    }

    private void post(int entry) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + entry + "}")
                                                 .when()
                                                 .post(POST)
                                                 .then()
                                                 .statusCode(200),
                TIMEOUT_SECONDS);
    }

    private int reverse(int entry) {
        AtomicReference<String> answer = new AtomicReference<>();
        restAssuredExecutor.execute(() -> answer.set(given().contentType("application/json")
                                                            .body("{\"id\":" + entry + "}")
                                                            .when()
                                                            .post(REVERSE)
                                                            .then()
                                                            .statusCode(200)
                                                            .extract()
                                                            .asString()),
                TIMEOUT_SECONDS);
        Matcher id = Pattern.compile("\"Id\"\\s*:\\s*(\\d+)")
                            .matcher(answer.get());
        assertTrue(id.find(), "the reversal should answer the created twin, got: " + answer.get());
        return Integer.parseInt(id.group(1));
    }

    private void reverse(int entry, int status, String message) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + entry + "}")
                                                 .when()
                                                 .post(REVERSE)
                                                 .then()
                                                 .statusCode(status)
                                                 .body(containsString(message)),
                TIMEOUT_SECONDS);
    }

    private double sum(int entry, String side) {
        AtomicReference<List<Float>> values = new AtomicReference<>();
        restAssuredExecutor.execute(() -> values.set(given().when()
                                                            .get(LINES + "?Entry=" + entry)
                                                            .then()
                                                            .statusCode(200)
                                                            .extract()
                                                            .jsonPath()
                                                            .getList(side, Float.class)),
                TIMEOUT_SECONDS);
        return values.get()
                     .stream()
                     .filter(java.util.Objects::nonNull)
                     .mapToDouble(Float::doubleValue)
                     .sum();
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
