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
import static com.codeborne.selenide.Condition.text;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * #8130: a generated list re-reads its rows on the header's Refresh - keeping the filter in force
 * and moving its "Updated" stamp - and when the user navigates to the list already on screen, both
 * through the generated shell's own sidebar and through the application shell that hosts the app,
 * neither of which re-renders the page. Before, rows created elsewhere stayed invisible until a
 * browser reload.
 *
 * <p>
 * This needs a real browser: every path is a client-side decision (a button, a router that ignores
 * a navigation to the current route, a shell that leaves its hosted frame alone).
 */
class ListRefreshHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final String PROJECT = "listrefreshit";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/listrefresh/index.html#";
    private static final String APPLICATION_SHELL = "/services/web/application/index.html";
    private static final String RECEIPTS = "/services/java/" + PROJECT + "/gen/listrefresh/api/receipt/ReceiptController";

    private static final String INTENT_YAML = """
            name: listrefresh
            entities:
              - name: Receipt
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: summary, type: string }
              - name: Store
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
            """;

    /**
     * The Receipt list in {@code doc} as the user sees it, once it has loaded:
     * {@code <state>;rows=<n>;filter=<Summary filter>;stamp=<"Updated" stamp shown>}.
     */
    private static final String LIST_STATE = """
            var doc = %s;
            var root = doc && doc.querySelector('[x-data="ReceiptManageListPage"]');
            var alpine = doc && doc.defaultView.Alpine;
            if (!root || !alpine) return 'not-rendered';
            var page = alpine.$data(root);
            if (page.state === 'loading') return 'pending';
            var stamp = Array.from(root.querySelectorAll('span')).some(s => /^Updated /.test(s.textContent.trim()) && s.checkVisibility());
            return page.state + ';rows=' + page.items.length + ';filter=' + (page.columnFilters.Summary || '') + ';stamp=' + stamp;
            """;

    private static final String SHELL_LIST = String.format(LIST_STATE, "document");

    private static final String HOSTED_LIST =
            String.format(LIST_STATE, "(document.querySelector('iframe[title=\"Application\"]') || {}).contentDocument");

    private static final String PAGE = "Alpine.$data(document.querySelector('[x-data=\"ReceiptManageListPage\"]'))";

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void a_list_re_reads_on_refresh_and_when_navigated_to_again() {
        generateAndPublish();
        ide.openHomePage();
        browser.openPath(APP + "/Receipt");
        assertEquals("empty;rows=0;filter=;stamp=true", poll(SHELL_LIST, "empty;rows=0;filter=;stamp=true"),
                "the empty list must render with its Updated stamp");

        createReceipts("kept one", "kept two", "other one");
        Selenide.$$("button[x-h-sidebar-menu-nav]")
                .findBy(text("Receipt"))
                .click();
        assertEquals("default;rows=3;filter=;stamp=true", poll(SHELL_LIST, "default;rows=3;filter=;stamp=true"),
                "the sidebar entry of the list on screen must re-read it");

        Selenide.executeJavaScript(PAGE + ".columnFilters.Summary = 'kept'; " + PAGE + ".applyServerFilter();");
        assertEquals("default;rows=2;filter=kept;stamp=true", poll(SHELL_LIST, "default;rows=2;filter=kept;stamp=true"),
                "the column filter must narrow the list");

        long stampBefore = Selenide.executeJavaScript("return " + PAGE + ".lastUpdated.getTime();");
        createReceipts("kept three", "other two");
        Selenide.$$("button")
                .findBy(exactText("Refresh"))
                .click();
        assertEquals("default;rows=3;filter=kept;stamp=true", poll(SHELL_LIST, "default;rows=3;filter=kept;stamp=true"),
                "Refresh must re-read the rows under the filter in force");
        long stampAfter = Selenide.executeJavaScript("return " + PAGE + ".lastUpdated.getTime();");
        assertTrue(stampAfter > stampBefore, "Refresh must move the Updated stamp");

        browser.openPath(APPLICATION_SHELL);
        Selenide.$$("button[x-h-sidebar-menu-nav]")
                .findBy(text("Receipt"))
                .click();
        assertEquals("default;rows=5;filter=;stamp=true", poll(HOSTED_LIST, "default;rows=5;filter=;stamp=true"),
                "the application shell must host the list");

        createReceipts("hosted one");
        Selenide.$$("button[x-h-sidebar-menu-nav]")
                .findBy(text("Receipt"))
                .click();
        assertEquals("default;rows=6;filter=;stamp=true", poll(HOSTED_LIST, "default;rows=6;filter=;stamp=true"),
                "re-clicking the hosted app's entry must re-read its list");
    }

    private void createReceipts(String... summaries) {
        for (String summary : summaries) {
            restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                     .body(Map.of("Summary", summary))
                                                     .when()
                                                     .post(RECEIPTS)
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
