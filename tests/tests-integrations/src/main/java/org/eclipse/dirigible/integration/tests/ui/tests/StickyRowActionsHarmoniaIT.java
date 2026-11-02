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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Dimension;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;

/**
 * #8372: a generated table keeps each row's actions on screen when its columns scroll sideways.
 *
 * <p>
 * The row-actions column is pinned with Harmonia's {@code data-sticky="end"}. On a 1280 px window a
 * list of eight long text columns, a composition panel and a {@code related:} register of 500
 * character values all scroll sideways; at either end of that scroll the row's actions button lies
 * inside the table container's visible rect and is the element on top at its centre (not a scrolled
 * cell), and a real click on the list's and the panel's "..." opens that row's menu.
 */
class StickyRowActionsHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final String PROJECT = "stickyactionsit";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/freight/index.html#";
    private static final String API = "/services/java/" + PROJECT + "/gen/freight/api";

    private static final String LIST_MENU = "[x-data=\"ShipmentManageListPage\"] td[data-row-actions] button[aria-label=\"Row actions\"]";
    private static final String PANEL_MENU = "[x-data^=\"detailPanel\"] td[data-row-actions] button[aria-label=\"Row actions\"]";
    private static final String REGISTER_OPEN = "[x-data^=\"relatedPanel\"] td[data-row-actions] button[aria-label=\"Open\"]";

    private static final String AT_START = "inside=true;onTop=true;scrolled=false";
    private static final String AT_END = "inside=true;onTop=true;scrolled=true";

    private static final String INTENT_YAML = """
            name: freight
            entities:
              - name: Shipment
                list: [number, customer, carrier, origin, destination, route, reference, notes]
                fields:
                  - { name: id,          type: integer, primaryKey: true, generated: true }
                  - { name: number,      type: string }
                  - { name: customer,    type: string }
                  - { name: carrier,     type: string }
                  - { name: origin,      type: string }
                  - { name: destination, type: string }
                  - { name: route,       type: string }
                  - { name: reference,   type: string }
                  - { name: notes,       type: string }
                relations:
                  - { name: parcels, kind: oneToMany, to: Parcel }
                related:
                  - entity: Ticket
              - name: Parcel
                list: [description, handling, remarks]
                fields:
                  - { name: id,          type: integer, primaryKey: true, generated: true }
                  - { name: description, type: string, length: 1000 }
                  - { name: handling,    type: string, length: 1000 }
                  - { name: remarks,     type: string, length: 1000 }
                relations:
                  - { name: Shipment, kind: manyToOne, to: Shipment, composition: true }
              - name: Ticket
                list: [subject, details, resolution]
                fields:
                  - { name: id,         type: integer, primaryKey: true, generated: true }
                  - { name: subject,    type: string, length: 1000 }
                  - { name: details,    type: string, length: 1000 }
                  - { name: resolution, type: string, length: 1000 }
                relations:
                  - { name: shipment, kind: manyToOne, to: Shipment }
            """;

    /**
     * The first visible button matching {@code arguments[0]}, its table container scrolled to
     * {@code arguments[1]} ({@code start} or {@code end}):
     * {@code inside=<within the container's visible rect>;onTop=<the element at its centre is the
     * button>;scrolled=<scrollLeft > 0>}.
     */
    private static final String ACTIONS_AT =
            """
                    var button = Array.from(document.querySelectorAll(arguments[0])).find(b => b.checkVisibility());
                    if (!button) return 'no-button:' + document.querySelectorAll(arguments[0]).length + ' found;' + document.querySelectorAll('td[data-row-actions]').length + ' cells;' + location.hash + ';' + Array.from(document.querySelectorAll('td[data-row-actions]')).map(td => (td.closest('[x-data]').getAttribute('x-data') || '').slice(0, 20) + '=' + td.innerHTML.replace(/\\s+/g, ' ').slice(0, 300)).join(' | ');
                    var container = button.closest('[data-slot="table-container"]');
                    if (!container) return 'no-container';
                    if (container.scrollWidth <= container.clientWidth + 1) return 'no-overflow:' + container.scrollWidth + '/' + container.clientWidth;
                    button.scrollIntoView({ block: 'center', inline: 'nearest' });
                    container.scrollLeft = arguments[1] === 'end' ? container.scrollWidth : 0;
                    var c = container.getBoundingClientRect();
                    var b = button.getBoundingClientRect();
                    var inside = b.width > 0 && b.left >= Math.max(c.left, 0) - 0.5 && b.right <= Math.min(c.right, window.innerWidth) + 0.5;
                    var hit = document.elementFromPoint(b.left + b.width / 2, b.top + b.height / 2);
                    var onTop = !!hit && (hit === button || button.contains(hit));
                    return 'inside=' + inside + ';onTop=' + onTop + ';scrolled=' + (container.scrollLeft > 0);
                    """;

    /** The number of open row menus under {@code arguments[0]}. */
    private static final String OPEN_MENUS = """
            return String(Array.from(document.querySelectorAll(arguments[0] + ' td[data-row-actions] [x-h-menu]'))
                               .filter(m => m.checkVisibility()).length);
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void a_list_row_menu_is_on_screen_at_either_scroll_end() {
        generateAndPublish();
        create("/shipment/ShipmentController", shipment("SH-1"));
        create("/shipment/ShipmentController", shipment("SH-2"));
        openNarrow(APP + "/Shipment");

        assertEquals(AT_START, actionsAt(LIST_MENU, "start"), "at the start of the scroll the list row's \"...\" must be on screen");
        assertEquals(AT_END, actionsAt(LIST_MENU, "end"), "at the end of the scroll the list row's \"...\" must be on screen");
        firstVisible(LIST_MENU).click();
        assertEquals("1", poll(OPEN_MENUS, "1", "[x-data=\"ShipmentManageListPage\"]"),
                "a click on the pinned \"...\" must open the row's menu");
    }

    @Test
    void a_panel_rows_actions_are_on_screen() {
        generateAndPublish();
        int shipment = create("/shipment/ShipmentController", shipment("SH-3"));
        create("/shipment/ParcelController", "{\"Shipment\":" + shipment + ",\"Description\":\"" + text("Pallet", 500)
                + "\",\"Handling\":\"" + text("Keep dry", 500) + "\",\"Remarks\":\"" + text("Fragile", 500) + "\"}");
        create("/ticket/TicketController", "{\"Shipment\":" + shipment + ",\"Subject\":\"" + text("Late pickup", 500) + "\",\"Details\":\""
                + text("Driver delayed", 500) + "\",\"Resolution\":\"" + text("Rebooked", 500) + "\"}");
        openNarrow(APP + "/Shipment/" + shipment + "/edit");

        assertEquals(AT_START, actionsAt(PANEL_MENU, "start"),
                "at the start of the scroll the composition row's \"...\" must be on screen");
        assertEquals(AT_END, actionsAt(PANEL_MENU, "end"), "at the end of the scroll the composition row's \"...\" must be on screen");
        firstVisible(PANEL_MENU).click();
        assertEquals("1", poll(OPEN_MENUS, "1", "[x-data^=\"detailPanel\"]"),
                "a click on the pinned \"...\" must open the panel row's menu");
        Selenide.executeJavaScript("document.body.click();");

        assertEquals(AT_START, actionsAt(REGISTER_OPEN, "start"), "at the start of the scroll the register row's Open must be on screen");
        assertEquals(AT_END, actionsAt(REGISTER_OPEN, "end"), "at the end of the scroll the register row's Open must be on screen");
    }

    /** Signs in, narrows the window to 1280 px and opens the page. */
    private void openNarrow(String path) {
        ide.openHomePage();
        WebDriverRunner.getWebDriver()
                       .manage()
                       .window()
                       .setSize(new Dimension(1280, 900));
        browser.openPath(path);
    }

    private String actionsAt(String selector, String end) {
        return poll(ACTIONS_AT, end.equals("end") ? AT_END : AT_START, selector, end);
    }

    private static com.codeborne.selenide.SelenideElement firstVisible(String selector) {
        return Selenide.$$(selector)
                       .filter(com.codeborne.selenide.Condition.visible)
                       .first();
    }

    private static String shipment(String number) {
        return "{\"Number\":\"" + number + "\",\"Customer\":\"" + text("Northwind Traders", 60) + "\",\"Carrier\":\""
                + text("Contoso Freight", 60) + "\",\"Origin\":\"" + text("Rotterdam", 60) + "\",\"Destination\":\"" + text("Hamburg", 60)
                + "\",\"Route\":\"" + text("North Sea", 60) + "\",\"Reference\":\"" + text("PO", 60) + "\",\"Notes\":\""
                + text("Deliver to the side entrance", 60) + "\"}";
    }

    /**
     * A value of exactly {@code length} characters that starts with {@code prefix}, its tail one
     * unbreakable word: a panel's text cell wraps up to {@code max-width: 28rem}, so a value made of
     * short words would only wrap, and the panel would not scroll at all.
     */
    private static String text(String prefix, int length) {
        StringBuilder value = new StringBuilder(prefix);
        while (value.length() < length) {
            value.append("_lorem");
        }
        return value.substring(0, length);
    }

    /** POSTs a record to a generated controller - retried until the published controller answers. */
    private int create(String controller, String body) {
        AtomicReference<Integer> id = new AtomicReference<>();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(API + controller)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                30);
        return id.get();
    }

    private static String poll(String script, String expected, Object... args) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
        String value = "";
        do {
            value = Selenide.executeJavaScript(script, args);
            if (expected.equals(value)) {
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
        WebDriverRunner.getWebDriver()
                       .manage()
                       .window()
                       .setSize(new Dimension(1920, 1080));
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
