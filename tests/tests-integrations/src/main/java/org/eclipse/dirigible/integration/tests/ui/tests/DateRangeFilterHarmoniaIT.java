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

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.visible;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Selenide;

import io.restassured.http.ContentType;

/**
 * #8131: a date column of a generated list filters by a From / To range of whole days, filled by
 * hand or by a quick period, instead of matching one exact day. The rows sit on both sides of the
 * current month's edges, so This month has to keep exactly the two inside it - on a DATE column and
 * on a timestamp column, where To must include the whole last day (23:30) and From must start at
 * the first day's local midnight.
 *
 * <p>
 * This needs a real browser: the range, the period and the bounds sent to {@code /search} are all
 * computed by the page, in the browser's time zone, which is the one this JVM runs in.
 */
class DateRangeFilterHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final String PROJECT = "daterangeit";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/daterange/index.html#";
    private static final String VOUCHERS = "/services/java/" + PROJECT + "/gen/daterange/api/voucher/VoucherController";

    private static final String INTENT_YAML = """
            name: daterange
            entities:
              - name: Voucher
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: label,    type: string }
                  - { name: docDate,  type: date }
                  - { name: postedAt, type: timestamp }
            """;

    private static final String PAGE = "Alpine.$data(document.querySelector('[x-data=\"VoucherManageListPage\"]'))";

    /**
     * The list once a query has settled: the labels of its rows, sorted, then the text of every filter
     * chip - {@code monthEnd,monthStart|Doc Date: ...}.
     */
    private static final String LIST_STATE = """
            var root = document.querySelector('[x-data="VoucherManageListPage"]');
            if (!root || !window.Alpine) return 'not-rendered';
            var page = Alpine.$data(root);
            if (page.state === 'loading' || page.filtering) return 'pending';
            var labels = page.items.map(i => i.Label).sort().join(',');
            return labels + '|' + page.activeFilterChips.map(c => c.text).join(';');
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void a_date_column_filters_by_a_range_and_its_quick_periods() {
        generateAndPublish();
        LocalDate firstOfMonth = LocalDate.now()
                                          .withDayOfMonth(1);
        LocalDate lastOfMonth = firstOfMonth.plusMonths(1)
                                            .minusDays(1);
        createVoucher("lastMonthEnd", firstOfMonth.minusDays(1), firstOfMonth.minusDays(1), 23, 30);
        createVoucher("monthStart", firstOfMonth, firstOfMonth, 0, 0);
        createVoucher("monthEnd", lastOfMonth, lastOfMonth, 23, 30);
        createVoucher("nextMonthStart", lastOfMonth.plusDays(1), lastOfMonth.plusDays(1), 0, 30);

        ide.openHomePage();
        browser.openPath(APP + "/Voucher");
        assertEquals("lastMonthEnd,monthEnd,monthStart,nextMonthStart|", poll(state -> state.endsWith("|")),
                "the unfiltered list must show every voucher");

        String dateRange = applyThisMonth("DocDate");
        assertTrue(dateRange.startsWith("monthEnd,monthStart|") && dateRange.contains(" - "),
                "This month on a DATE column must keep the month's two vouchers and show the range in its chip: " + dateRange);

        Selenide.executeJavaScript(PAGE + ".removeFilter('DocDate');");
        assertEquals("lastMonthEnd,monthEnd,monthStart,nextMonthStart|", poll(state -> state.endsWith("|")),
                "removing the chip must drop the range");

        String timestampRange = applyThisMonth("PostedAt");
        assertTrue(timestampRange.startsWith("monthEnd,monthStart|"),
                "This month on a timestamp column must include its last day's 23:30 and exclude its neighbours' edges: " + timestampRange);

        Selenide.executeJavaScript(PAGE + ".removeFilter('PostedAt'); " + PAGE + ".columnFilters = { DocDate: { from: '" + firstOfMonth
                + "', to: '' } }; " + PAGE + ".applyServerFilter();");
        String openEnded = poll(state -> state.startsWith("monthEnd,monthStart,nextMonthStart|"));
        assertTrue(openEnded.startsWith("monthEnd,monthStart,nextMonthStart|From "),
                "a range with only From must be open towards the future and say so in its chip: " + openEnded);
    }

    /**
     * Opens the column's range editor, clicks This month and Apply as a user would, and waits for the
     * query.
     */
    private String applyThisMonth(String column) {
        // Two steps, as the user takes them: opening the menu resets it to its column list (a watcher
        // that runs after this call returns), and only then is the column picked.
        Selenide.executeJavaScript(PAGE + ".filterOpen = true;");
        Selenide.sleep(POLL_INTERVAL_MILLIS);
        Selenide.executeJavaScript(PAGE + ".openFilterEditor('" + column + "');");
        Selenide.$$("button")
                .filterBy(visible)
                .findBy(exactText("This month"))
                .click();
        Selenide.$$("button")
                .filterBy(visible)
                .findBy(exactText("Apply"))
                .click();
        return poll(state -> state.startsWith("monthEnd,monthStart|"));
    }

    private void createVoucher(String label, LocalDate docDate, LocalDate postedDay, int hour, int minute) {
        String postedAt = postedDay.atTime(hour, minute)
                                   .atZone(ZoneId.systemDefault())
                                   .toInstant()
                                   .toString();
        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body(Map.of("Label", label, "DocDate", docDate.toString(), "PostedAt", postedAt))
                                                 .when()
                                                 .post(VOUCHERS)
                                                 .then()
                                                 .statusCode(200));
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

    private void generateAndPublish() {
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
