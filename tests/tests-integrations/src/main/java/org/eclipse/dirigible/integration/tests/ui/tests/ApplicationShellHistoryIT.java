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

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Selenide;

import io.restassured.http.ContentType;

/**
 * One user action is one browser-history entry in the application shell. Opening a record of a
 * hosted app's list is a step of its own ({@code #/app/<perspective>/<Entity>/<id>}): Back closes
 * the sheet and keeps the list, and closing the sheet takes its entry back, so no step is left that
 * Back would walk through without effect. In Settings the hosted setting's route is mirrored into
 * the address ({@code #/settings/<perspective>/<inner route>}), every pick is an entry, and the
 * frame element survives picks and Back, so the setting's own history stays intact.
 *
 * <p>
 * This needs a real browser: the entries are the browser's, and the shell's router, the hosted
 * frame and the page's popstate handling decide them together. Back is {@code history.back()} on
 * the shell window, which traverses the one joint history the hosted frame's entries are part of;
 * the driver's own back waits for a top-level load that a same-document traversal never fires.
 */
class ApplicationShellHistoryIT extends UserInterfaceIntegrationTest {

    private static final Duration WAIT = Duration.ofSeconds(60);

    private static final String PROJECT = "shellhistoryit";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APPLICATION_SHELL = "/services/web/application/index.html";
    private static final String RECEIPTS = "/services/java/" + PROJECT + "/gen/shellhistory/api/receipt/ReceiptController";

    private static final String INTENT_YAML = """
            name: shellhistory
            entities:
              - name: Receipt
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: summary, type: string }
              - name: ReceiptKind
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
            """;

    private static final String SETTING_ROUTE = "^#/settings/[^/]+/ReceiptKind$";

    /** The shell's address: the hash naming the hosted page. */
    private static final String ADDRESS = "return window.location.hash;";

    /**
     * The hosted Receipt list: {@code list;open=<sheet open>} once its rows are loaded.
     */
    private static final String HOSTED_LIST = """
            var frame = document.querySelector('iframe[title="Application"]');
            var doc = frame && frame.contentDocument;
            var alpine = doc && doc.defaultView.Alpine;
            if (!alpine) return 'not-rendered';
            var list = doc.querySelector('[x-data="ReceiptManageListPage"]');
            if (!list) return 'other';
            var page = alpine.$data(list);
            return page.items && page.items.length ? 'list;open=' + !!page.detailsOpen : 'loading';
            """;

    /** A click on the first record of the hosted list, as a user's. */
    private static final String OPEN_FIRST_RECORD = """
            var doc = document.querySelector('iframe[title="Application"]').contentDocument;
            doc.querySelector('[x-data="ReceiptManageListPage"] tr[data-hoverable][data-state]').click();
            """;

    /**
     * What every dismissal of the sheet (Escape, the close button, the scrim) does: the overlay owns
     * {@code detailsOpen} two-way, and the page's watcher takes the record's history entry back.
     */
    private static final String DISMISS_SHEET = """
            var doc = document.querySelector('iframe[title="Application"]').contentDocument;
            doc.defaultView.Alpine.$data(doc.querySelector('[x-data="ReceiptManageListPage"]')).detailsOpen = false;
            """;

    private static final String SETTINGS_FRAME = "document.querySelector('iframe[data-settings-frame]')";

    /** The settings frame is rendered and its app has booted its router. */
    private static final String SETTINGS_FRAME_READY = "var f = " + SETTINGS_FRAME + ";"
            + "return f && f.contentWindow && f.contentWindow.PineconeRouter && f.contentDocument.readyState === 'complete' ? 'ready' : 'waiting';";

    private static final String SETTINGS_FRAME_HASH = "var f = " + SETTINGS_FRAME + "; return f ? f.contentWindow.location.hash : '';";

    private static final String SETTINGS_FRAME_IS_TAGGED = "var f = " + SETTINGS_FRAME + "; return f && f.__itTag === 'same';";

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void a_record_of_a_hosted_list_is_one_history_entry() {
        generateAndPublish();
        String id = String.valueOf(createReceipt("first"));

        ide.openHomePage();
        browser.openPath(APPLICATION_SHELL + "#/dashboard");
        String before = awaitHash("#/dashboard"::equals, "the shell must open on its dashboard");
        openTheReceiptList();
        String list = awaitHash(hash -> hash.startsWith("#/app/") && hash.endsWith("/Receipt"), "the address must name the list");

        String record = list + "/" + id;
        Selenide.executeJavaScript(OPEN_FIRST_RECORD);
        awaitHash(record::equals, "opening a record must name it in the address");
        awaitHosted("list;open=true", "the record opens in the sheet");

        back();
        awaitHash(list::equals, "Back must return to the list route");
        awaitHosted("list;open=false", "Back must close the sheet and keep the list");

        back();
        awaitHash(before::equals, "Back from the list must leave the app for the page before it");

        // The sheet closed by the user takes its entry back: the next Back leaves the app, no dead step.
        openTheReceiptList();
        awaitHash(list::equals, "the list is opened again");
        Selenide.executeJavaScript(OPEN_FIRST_RECORD);
        awaitHash(record::equals, "the record is opened again");
        Selenide.executeJavaScript(DISMISS_SHEET);
        awaitHash(list::equals, "closing the sheet must take the record's entry back");
        awaitHosted("list;open=false", "the sheet is closed");

        back();
        awaitHash(before::equals, "after closing the sheet, Back must leave the app and not step through a dead entry");
    }

    @Test
    void a_setting_of_an_app_keeps_its_frame_and_its_own_history_in_settings() {
        generateAndPublish();

        ide.openHomePage();
        browser.openPath(APPLICATION_SHELL);
        Selenide.$(By.id("tenant-menu-trigger"))
                .shouldBe(visible, WAIT);
        browser.openPath(APPLICATION_SHELL + "#/settings");
        Selenide.$(By.xpath("//div[@id='app']//button[contains(normalize-space(.), 'Receipt Kind')]"))
                .shouldBe(visible, WAIT)
                .click();
        String list = awaitHash(hash -> hash.matches(SETTING_ROUTE), "picking a setting must name it in the address");
        awaitJs(SETTINGS_FRAME_READY, "ready"::equals, "the settings frame must show the setting");
        Selenide.executeJavaScript(SETTINGS_FRAME + ".__itTag = 'same';");

        // the hosted setting's own navigation is mirrored into the address
        Selenide.executeJavaScript(SETTINGS_FRAME + ".contentWindow.PineconeRouter.navigate('/ReceiptKind/create');");
        String create = awaitHash(hash -> hash.equals(list + "/create"), "the setting's create route must reach the address");
        awaitJs(SETTINGS_FRAME_HASH, hash -> hash.endsWith("/ReceiptKind/create"), "the frame must show the create route");
        Selenide.$("nav[x-h-breadcrumb]")
                .shouldHave(text("Create"), WAIT);

        // picking another entry is an entry of its own, and Back returns to the setting where it was left
        Selenide.$(By.xpath("//div[@id='app']//button[.//*[@data-lucide='globe']]"))
                .click();
        awaitHash("#/settings/region-language"::equals, "picking Region & Language must name it in the address");
        back();
        awaitHash(create::equals, "Back must return to the setting's last route");

        back();
        awaitHash(list::equals, "Back must step back inside the setting, to its list");
        awaitJs(SETTINGS_FRAME_HASH, hash -> hash.endsWith("/ReceiptKind") || hash.endsWith("/ReceiptKind/"),
                "the frame must follow the address back to the list");

        assertEquals(Boolean.TRUE, Selenide.executeJavaScript(SETTINGS_FRAME_IS_TAGGED),
                "the settings frame must be the same element across every pick and Back");
    }

    private void openTheReceiptList() {
        Selenide.$$("button[x-h-sidebar-menu-nav]")
                .findBy(text("Receipt"))
                .click();
        awaitHosted("list;open=false", "the application shell must host the Receipt list");
    }

    private void back() {
        Selenide.executeJavaScript("window.history.back();");
    }

    private void awaitHosted(String expected, String message) {
        awaitJs(HOSTED_LIST, expected::equals, message);
    }

    private String awaitHash(Predicate<String> expected, String message) {
        return awaitJs(ADDRESS, expected, message);
    }

    private String awaitJs(String script, Predicate<String> expected, String message) {
        AtomicReference<String> last = new AtomicReference<>();
        try {
            Awaitility.await()
                      .pollInSameThread() // Selenide binds the WebDriver to the test thread
                      .atMost(WAIT)
                      .pollInterval(Duration.ofMillis(250))
                      .until(() -> {
                          last.set(String.valueOf((Object) Selenide.executeJavaScript(script)));
                          return expected.test(last.get());
                      });
        } catch (ConditionTimeoutException e) {
            fail(message + " (last seen: " + last.get() + ")", e);
        }
        return last.get();
    }

    private Object createReceipt(String summary) {
        AtomicReference<Object> id = new AtomicReference<>();
        restAssuredExecutor.execute(() -> id.set(given().contentType(ContentType.JSON)
                                                        .body(Map.of("Summary", summary))
                                                        .when()
                                                        .post(RECEIPTS)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")));
        return id.get();
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
