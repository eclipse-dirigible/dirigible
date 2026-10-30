/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;

/**
 * A report row opens the records it counts (#8374): each row of a plain aggregation over listed
 * relation dimensions links, on the report page and on its dashboard list tile, to the source's
 * list filtered by the row's labels - so "High | REOPENED | 3" lands on exactly those three
 * tickets, with one filter chip per dimension. A report the list cannot repeat (a month bucket)
 * renders no links.
 */
class ReportDrillDownHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final String PROJECT = "drilldownit";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/helpdesk/index.html";
    private static final String BY_STATUS_PAGE = "/services/web/" + PROJECT + "/gen/ticketsbystatus/reports/TicketsByStatus/index.html";
    private static final String BY_MONTH_PAGE = "/services/web/" + PROJECT + "/gen/ticketsbymonth/reports/TicketsByMonth/index.html";

    private static final String INTENT_YAML = """
            name: helpdesk
            entities:
              - name: TicketPriority
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: TicketStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Ticket
                list: [subject, priority, status, escalated]
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: subject,   type: string }
                  - { name: escalated, type: boolean }
                  - { name: openedOn,  type: date }
                relations:
                  - { name: priority, kind: manyToOne, to: TicketPriority }
                  - { name: status,   kind: manyToOne, to: TicketStatus, function: EntityStatus }
            reports:
              - name: TicketsByStatus
                source: Ticket
                dimensions: [priority, status]
                measures: ["count(*)"]
                widget: { kind: list, limit: 5 }
              - name: TicketsByMonth
                source: Ticket
                scope: all
                dimensions: ["month(openedOn)"]
                measures: ["count(*)"]
            seeds:
              - name: ticket-priorities
                entity: TicketPriority
                rows:
                  - { id: 1, name: High }
                  - { id: 2, name: Low }
              - name: ticket-statuses
                entity: TicketStatus
                rows:
                  - { id: 1, name: OPEN }
                  - { id: 2, name: REOPENED }
              - name: tickets
                entity: Ticket
                rows:
                  - { id: 1, subject: A, escalated: true,  openedOn: "2026-09-01", priority: 1, status: 2 }
                  - { id: 2, subject: B, escalated: false, openedOn: "2026-09-02", priority: 1, status: 2 }
                  - { id: 3, subject: C, escalated: false, openedOn: "2026-10-03", priority: 1, status: 2 }
                  - { id: 4, subject: D, escalated: false, openedOn: "2026-10-04", priority: 1, status: 1 }
                  - { id: 5, subject: E, escalated: true,  openedOn: "2026-10-05", priority: 2, status: 2 }
            """;

    /**
     * The Tickets list once its first query has settled: the subjects of its rows, sorted, then the
     * text of every filter chip - {@code A,B,C|High;REOPENED}.
     */
    private static final String LIST_STATE = """
            var root = document.querySelector('[x-data="TicketManageListPage"]');
            if (!root || !window.Alpine) return 'not-rendered';
            var page = Alpine.$data(root);
            if (page.state === 'loading' || page.filtering) return 'pending';
            var subjects = page.items.map(i => i.Subject).sort().join(',');
            return subjects + '|' + page.activeFilterChips.map(c => c.text).join(';');
            """;

    /** The link of the report row "High | REOPENED", on the report page or the dashboard tile. */
    private static final String HIGH_REOPENED_LINK = "//tr[td[normalize-space()='REOPENED']]/td[1]/a[normalize-space()='High']";

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @BeforeEach
    void generateAndPublish() {
        String path = PROJECT_PATH + "/app.intent";
        if (repository.hasResource(path)) {
            repository.getResource(path)
                      .setContent(INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        }
        AtomicReference<List<Map<String, Object>>> plan = new AtomicReference<>();
        restAssuredExecutor.execute(() -> plan.set(given().when()
                                                          .post(GENERATE_URL)
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

    @Test
    void a_report_row_opens_the_list_of_exactly_its_records() {
        ide.openHomePage();
        browser.openPath(BY_STATUS_PAGE);
        Selenide.$x(HIGH_REOPENED_LINK)
                .shouldBe(Condition.visible)
                .click();
        assertEquals("A,B,C|High;REOPENED", poll(state -> state.startsWith("A,B,C|")),
                "the row must open the Tickets list filtered to its priority and status, with both chips");
        String hash = Selenide.executeJavaScript("return window.location.hash;");
        assertEquals("#/Ticket?Priority=High&Status=REOPENED", hash, "the report page opens the application at the list's route");
    }

    @Test
    void a_list_tile_row_does_the_same() {
        ide.openHomePage();
        browser.openPath(APP + "#/");
        // The sign-in redirect drops the fragment, so the dashboard is opened again once the session
        // exists.
        browser.openPath(APP + "#/");
        Selenide.$x(HIGH_REOPENED_LINK)
                .shouldBe(Condition.visible)
                .click();
        assertEquals("A,B,C|High;REOPENED", poll(state -> state.startsWith("A,B,C|")),
                "the tile's row must open the Tickets list filtered to its priority and status, with both chips");
    }

    @Test
    void a_bucketed_report_has_no_links() {
        ide.openHomePage();
        browser.openPath(BY_MONTH_PAGE);
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
        Number rows = 0;
        while (System.currentTimeMillis() < deadline && rows.intValue() == 0) {
            rows = Selenide.executeJavaScript("return document.querySelectorAll('tbody tr').length;");
            Selenide.sleep(POLL_INTERVAL_MILLIS);
        }
        assertTrue(rows.intValue() >= 2, "the month report must list its two months, got " + rows);
        assertEquals(0L, ((Number) Selenide.executeJavaScript("return document.querySelectorAll('tbody a').length;")).longValue(),
                "a month bucket cannot be a list filter, so no row is a link");
    }

    private String poll(Predicate<String> settled) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
        String value = "";
        do {
            value = Selenide.executeJavaScript(LIST_STATE);
            if (value != null && !"pending".equals(value) && !"not-rendered".equals(value) && settled.test(value)) {
                return value;
            }
            Selenide.sleep(POLL_INTERVAL_MILLIS);
        } while (System.currentTimeMillis() < deadline);
        return value;
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
