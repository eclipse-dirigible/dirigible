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
import com.codeborne.selenide.WebDriverRunner;

import io.restassured.http.ContentType;

/**
 * #8638: the application shell's address bar names the page of the app it hosts
 * ({@code #/app/<perspective>/<inner route>}), and the hosted app follows it - an address changed
 * to another record of the same app shows that record, and a reload shows what the address names,
 * not the page the hosted frame showed before. An edited form is not left behind the user's back:
 * the address asks the form's unsaved-changes guard first, as a navigation inside the app does.
 *
 * <p>
 * This needs a real browser: both are decided by the shell's router and the browser's own handling
 * of the hosted frame across a reload.
 */
class ApplicationShellAddressIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final String PROJECT = "shelladdressit";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APPLICATION_SHELL = "/services/web/application/index.html";
    private static final String RECEIPTS = "/services/java/" + PROJECT + "/gen/shelladdress/api/receipt/ReceiptController";

    private static final String INTENT_YAML = """
            name: shelladdress
            entities:
              - name: Receipt
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: summary, type: string }
            """;

    /**
     * What the hosted app shows: {@code list}, or {@code form;id=<id>;summary=<Summary>} once loaded.
     */
    private static final String HOSTED_PAGE = """
            var frame = document.querySelector('iframe[title="Application"]');
            var doc = frame && frame.contentDocument;
            var alpine = doc && doc.defaultView.Alpine;
            if (!alpine) return 'not-rendered';
            var form = doc.querySelector('[x-data="ReceiptFormPage"]');
            if (form) {
              var page = alpine.$data(form);
              return 'form;id=' + page.id + ';summary=' + (page.form.Summary || '');
            }
            var list = doc.querySelector('[x-data="ReceiptManageListPage"]');
            return list ? 'list' : 'other';
            """;

    /** The shell's address: the hash naming the hosted page. */
    private static final String ADDRESS = "return window.location.hash;";

    /** The hosted Receipt form page's Alpine data. */
    private static final String HOSTED_FORM = "document.querySelector('iframe[title=\"Application\"]').contentWindow.Alpine"
            + ".$data(document.querySelector('iframe[title=\"Application\"]').contentDocument.querySelector('[x-data=\"ReceiptFormPage\"]'))";

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void the_hosted_app_follows_the_address_bar_and_a_reload() {
        generateAndPublish();
        Object first = createReceipt("first");
        Object second = createReceipt("second");

        ide.openHomePage();
        browser.openPath(APPLICATION_SHELL);
        Selenide.$$("button[x-h-sidebar-menu-nav]")
                .findBy(text("Receipt"))
                .click();
        assertEquals("list", poll(HOSTED_PAGE, "list"), "the application shell must host the Receipt list");
        String listHash = Selenide.executeJavaScript(ADDRESS);
        assertTrue(listHash.startsWith("#/app/") && listHash.endsWith("/Receipt"), "the address must name the list: " + listHash);
        String appHash = listHash.substring(0, listHash.length() - "/Receipt".length());

        typeAddress(appHash + "/Receipt/" + first + "/edit");
        String firstForm = "form;id=" + first + ";summary=first";
        assertEquals(firstForm, poll(HOSTED_PAGE, firstForm), "an address naming a record of the hosted app must open it");

        typeAddress(appHash + "/Receipt/" + second + "/edit");
        String secondForm = "form;id=" + second + ";summary=second";
        assertEquals(secondForm, poll(HOSTED_PAGE, secondForm), "an address naming another record must switch the hosted form to it");

        typeAddress(listHash);
        assertEquals("list", poll(HOSTED_PAGE, "list"), "an address naming the list must show it");

        typeAddress(appHash + "/Receipt/" + first + "/edit");
        assertEquals(firstForm, poll(HOSTED_PAGE, firstForm), "the hosted app must open the record again");
        // The user changes the address to the list and reloads instead of pressing Enter.
        Selenide.executeJavaScript("history.replaceState(history.state, '', arguments[0]);", listHash);
        Selenide.refresh();
        assertEquals("list", poll(HOSTED_PAGE, "list"), "a reload must show what the address names, not the form shown before");
        assertEquals(listHash, Selenide.executeJavaScript(ADDRESS), "the reload must keep the address");

        // An edited form is not left behind the user's back: the address naming another record asks
        // first, keeps the form and its address, and opens the other record once the user discards.
        typeAddress(appHash + "/Receipt/" + first + "/edit");
        assertEquals(firstForm, poll(HOSTED_PAGE, firstForm), "the hosted app must open the record again");
        Selenide.executeJavaScript(HOSTED_FORM + ".form.Summary = 'edited';");
        typeAddress(appHash + "/Receipt/" + second + "/edit");
        String editedForm = "form;id=" + first + ";summary=edited";
        assertEquals(editedForm, poll(HOSTED_PAGE, editedForm), "an edited form must stay on screen");
        assertEquals(Boolean.TRUE, Selenide.executeJavaScript("return " + HOSTED_FORM + ".leaveOpen;"),
                "the address naming another record must raise the unsaved-changes dialog");
        assertEquals(appHash + "/Receipt/" + first + "/edit", poll(ADDRESS, appHash + "/Receipt/" + first + "/edit"),
                "the address must name the form that stayed");
        Selenide.executeJavaScript(HOSTED_FORM + ".leaveDiscard();");
        assertEquals(secondForm, poll(HOSTED_PAGE, secondForm), "discarding must open the record the address named");
        assertEquals(appHash + "/Receipt/" + second + "/edit", poll(ADDRESS, appHash + "/Receipt/" + second + "/edit"),
                "the address must name the record shown");
    }

    /** As if typed into the address bar and confirmed: a same-document navigation to the new hash. */
    private void typeAddress(String hash) {
        String current = WebDriverRunner.getWebDriver()
                                        .getCurrentUrl();
        int hashAt = current.indexOf('#');
        WebDriverRunner.getWebDriver()
                       .navigate()
                       .to((hashAt == -1 ? current : current.substring(0, hashAt)) + hash);
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

    private String poll(String script, String expected) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
        String value;
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
