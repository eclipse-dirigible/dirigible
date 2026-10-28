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
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Selenide;

import io.restassured.http.ContentType;

/**
 * #8259: opening a row's "..." menu closes the menu opened on another row.
 *
 * <p>
 * The row-actions cell carried {@code @click.stop}, so the row click would not open the drawer.
 * That also kept the click from the document, where Harmonia closes an open menu, so every row menu
 * the user opened stayed open and they stacked over each other. The row now ignores clicks from its
 * own controls instead of the controls stopping the event. Asserted with real clicks in a browser:
 * one menu at a time, the "..." still does not open the row, a click on the row still does.
 */
class ListRowMenuHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final String PROJECT = "rowmenuit";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/rowmenu/index.html#";
    private static final String STORES = "/services/java/" + PROJECT + "/gen/rowmenu/api/store/StoreController";

    private static final String INTENT_YAML = """
            name: rowmenu
            entities:
              - name: Store
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
            """;

    /**
     * The list as the user sees it: {@code rows=<n>;menus=<open row menus>;drawer=<open>}, once loaded.
     */
    private static final String LIST_STATE = """
            var root = document.querySelector('[x-data="StoreManageListPage"]');
            if (!root || !window.Alpine) return 'not-rendered';
            var page = Alpine.$data(root);
            if (page.state === 'loading') return 'pending';
            var menus = Array.from(root.querySelectorAll('td[data-row-actions] [x-h-menu]')).filter(m => m.checkVisibility()).length;
            return 'rows=' + page.items.length + ';menus=' + menus + ';drawer=' + page.detailsOpen;
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void opening_a_row_menu_closes_the_one_opened_on_another_row() {
        generateAndPublish();
        createStores("WH-1", "WH-2", "WH-3");
        ide.openHomePage();
        browser.openPath(APP + "/Store");
        assertEquals("rows=3;menus=0;drawer=false", poll("rows=3;menus=0;drawer=false"), "the list must render the three stores");

        // Bottom row first, then upwards, as in the report: a menu opens downwards, so the trigger of
        // a row above it is never covered by it.
        rowActions(2).click();
        assertEquals("rows=3;menus=1;drawer=false", poll("rows=3;menus=1;drawer=false"),
                "the last row's menu must open, and the \"...\" must not open the row in the drawer");
        rowActions(1).click();
        assertEquals("rows=3;menus=1;drawer=false", poll("rows=3;menus=1;drawer=false"),
                "opening another row's menu must close the one already open");
        rowActions(0).click();
        assertEquals("rows=3;menus=1;drawer=false", poll("rows=3;menus=1;drawer=false"), "still one menu at a time");

        // A click on the row itself still opens it.
        Selenide.$$("[x-data=\"StoreManageListPage\"] tbody tr td")
                .first()
                .click();
        assertEquals("rows=3;menus=0;drawer=true", poll("rows=3;menus=0;drawer=true"),
                "a click on the row must close the open menu and open the row in the drawer");
    }

    private static com.codeborne.selenide.SelenideElement rowActions(int index) {
        return Selenide.$$("[x-data=\"StoreManageListPage\"] button[aria-label=\"Row actions\"]")
                       .get(index);
    }

    private void createStores(String... names) {
        for (String name : names) {
            restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                     .body(Map.of("Name", name))
                                                     .when()
                                                     .post(STORES)
                                                     .then()
                                                     .statusCode(200));
        }
    }

    private String poll(String expected) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
        String value = "";
        do {
            value = Selenide.executeJavaScript(LIST_STATE);
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
