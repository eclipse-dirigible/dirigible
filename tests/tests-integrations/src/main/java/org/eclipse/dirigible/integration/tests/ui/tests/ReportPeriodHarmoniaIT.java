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

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
 * A report's movable window (#8506), in the browser: the report page opens on the default preset
 * with both dates filled and the window named in its title, choosing another preset in the Period
 * select refills the dates and renames the window, and a date set by hand switches the select to
 * Custom. The dashboard tile names the window too.
 */
class ReportPeriodHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final String PROJECT = "reportperiodit";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/shop/index.html";
    private static final String PAGE = "/services/web/" + PROJECT + "/gen/orderedproducts/reports/OrderedProducts/index.html";

    private static final String INTENT_YAML = """
            name: shop
            entities:
              - name: Product
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: OrderLine
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: orderedOn, type: date }
                  - { name: quantity,  type: decimal }
                relations:
                  - { name: product, kind: manyToOne, to: Product }
            reports:
              - name: OrderedProducts
                source: OrderLine
                dimensions: [product.name]
                measures: ["sum(quantity)"]
                widget: { kind: list, limit: 5 }
                period: { target: orderedOn, default: last30Days, presets: [last30Days, previousMonth, yearToDate] }
            seeds:
              - name: products
                entity: Product
                rows:
                  - { id: 1, name: Bolt }
              - name: order-lines
                entity: OrderLine
                rows:
                  - { id: 1, orderedOn: "2026-01-15", quantity: 3, product: 1 }
            """;

    /** The page's period state once it rendered: preset | from | to | title. */
    private static final String PAGE_STATE =
            """
                    var root = document.querySelector('[x-data="reportPage"]');
                    if (!root || !window.Alpine) return 'not-rendered';
                    var page = Alpine.$data(root);
                    if (!page.period) return 'no-period';
                    return page.preset + '|' + page.params.fromDate + '|' + page.params.toDate + '|' + document.querySelector('h1').textContent.trim();
                    """;

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
    void the_presets_fill_the_window_and_a_hand_edit_makes_it_custom() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        ide.openHomePage();
        browser.openPath(PAGE);

        assertEquals("last30Days|" + today.minusDays(29) + "|" + today + "|Ordered Products (Last 30 days)",
                poll(state -> state.startsWith("last30Days|")), "the page opens on the default preset, named in the title");

        Selenide.$("[data-slot='select-input']")
                .shouldBe(Condition.visible)
                .click();
        Selenide.$("[data-slot='select-option'][data-value='previousMonth']")
                .shouldBe(Condition.visible)
                .click();
        assertEquals("previousMonth|" + monthStart.minusMonths(1) + "|" + monthStart.minusDays(1) + "|Ordered Products (Previous month)",
                poll(state -> state.startsWith("previousMonth|")), "choosing a preset refills both dates and renames the window");

        Selenide.executeJavaScript("Alpine.$data(document.querySelector('[x-data=\"reportPage\"]')).params.fromDate = '2026-01-01';");
        String custom = poll(state -> state.startsWith("custom|"));
        assertEquals("custom|2026-01-01|" + monthStart.minusDays(1), custom.substring(0, custom.lastIndexOf('|')),
                "a date set by hand leaves the preset: the select says Custom");
    }

    @Test
    void the_dashboard_tile_names_the_window() {
        ide.openHomePage();
        browser.openPath(APP + "#/");
        // The sign-in redirect drops the fragment, so the dashboard is opened again once the session
        // exists.
        browser.openPath(APP + "#/");
        Selenide.$x("//*[normalize-space()='Ordered Products (Last 30 days)']")
                .shouldBe(Condition.visible);
    }

    private String poll(Predicate<String> settled) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
        String value = "";
        do {
            value = Selenide.executeJavaScript(PAGE_STATE);
            if (value != null && settled.test(value)) {
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
