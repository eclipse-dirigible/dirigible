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
 * #8134: deleting the record open in a generated list's detail drawer closes the drawer, and the
 * confirmation names the record - on the list and on the record's own page.
 *
 * <p>
 * The delete cleared the selection but left the drawer open, so it stayed on screen as a record
 * that no longer exists - titled "#null", every field empty, Edit enabled - until the user closed
 * it by hand; and the confirmation asked about "this record" without saying which. Both are
 * client-side state, so this needs a real browser.
 */
class ListDeleteDrawerHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final String PROJECT = "listdeleteit";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/listdelete/index.html#";
    private static final String STORES = "/services/java/" + PROJECT + "/gen/listdelete/api/store/StoreController";

    private static final String INTENT_YAML = """
            name: listdelete
            entities:
              - name: Store
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
            """;

    private static final String PAGE = "Alpine.$data(document.querySelector('[x-data=\"StoreManageListPage\"]'))";

    /**
     * The list as the user sees it once it has loaded: {@code <state>;rows=<n>;drawer=<open>;
     * selected=<Name or none>;hash=<location hash>}.
     */
    private static final String LIST_STATE = """
            var root = document.querySelector('[x-data="StoreManageListPage"]');
            if (!root || !window.Alpine) return 'not-rendered';
            var page = Alpine.$data(root);
            if (page.state === 'loading') return 'pending';
            return page.state + ';rows=' + page.items.length + ';drawer=' + page.detailsOpen
                + ';selected=' + (page.selected ? page.selected.Name : 'none') + ';hash=' + window.location.hash;
            """;

    private static final String FORM = "Alpine.$data(document.querySelector('[x-data=\"StoreFormPage\"]'))";

    private static final String FORM_NAME = """
            var root = document.querySelector('[x-data="StoreFormPage"]');
            return root && window.Alpine ? String(Alpine.$data(root).form.Name) : 'not-rendered';
            """;

    private static final String CONFIRMATION = """
            var dialog = document.querySelector('[x-h-dialog-overlay][data-open]');
            return dialog ? dialog.querySelector('[x-h-dialog-content] p').textContent.trim() : 'no-dialog';
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void deleting_the_record_open_in_the_drawer_closes_the_drawer_and_the_confirmation_names_it() {
        generateAndPublish();
        createStores("WH-DEL", "WH-KEEP");
        ide.openHomePage();
        browser.openPath(APP + "/Store");
        assertEquals("default;rows=2;drawer=false;selected=none;hash=#/Store",
                poll(LIST_STATE, "default;rows=2;drawer=false;selected=none;hash=#/Store"), "the list must render both stores");

        // Open WH-DEL in the drawer, as a click on its row does.
        Selenide.executeJavaScript(PAGE + ".selectRow(" + PAGE + ".items.find(r => r.Name === 'WH-DEL'));");
        String opened = Selenide.executeJavaScript(LIST_STATE);
        assertEquals("default;rows=2;drawer=true;selected=WH-DEL", opened.substring(0, opened.indexOf(";hash=")),
                "the drawer must show the selected store");

        // "..." -> Delete on the open record: the confirmation says which record it is about.
        Selenide.executeJavaScript(PAGE + ".askDelete(" + PAGE + ".selected);");
        assertEquals("Are you sure you want to delete Store WH-DEL? This action cannot be undone.",
                poll(CONFIRMATION, "Are you sure you want to delete Store WH-DEL? This action cannot be undone."),
                "the confirmation must name the record");

        Selenide.$$("[x-h-dialog-overlay][data-open] button")
                .findBy(exactText("Delete"))
                .click();
        assertEquals("default;rows=1;drawer=false;selected=none;hash=#/Store",
                poll(LIST_STATE, "default;rows=1;drawer=false;selected=none;hash=#/Store"),
                "deleting the open record must close its drawer and take its id out of the URL");

        // The record's own page asks the same question, naming the same way.
        long keptId = Selenide.executeJavaScript("return " + PAGE + ".items[0].Id;");
        browser.openPath(APP + "/Store/" + keptId + "/edit");
        assertEquals("WH-KEEP", poll(FORM_NAME, "WH-KEEP"), "the record page must load the store");
        Selenide.executeJavaScript(FORM + ".askDelete();");
        assertEquals("Are you sure you want to delete Store WH-KEEP? This action cannot be undone.",
                poll(CONFIRMATION, "Are you sure you want to delete Store WH-KEEP? This action cannot be undone."),
                "the record page's confirmation must name the record too");
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

    private String poll(String script, String expected) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
        String value = "";
        do {
            value = Selenide.executeJavaScript(script);
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
