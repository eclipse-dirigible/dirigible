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
import static com.codeborne.selenide.Selenide.$$;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.ide.IDEFactory;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.security.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Selenide;

/**
 * A task form reads its related records in the user's language (issue #8912): the Post task of a
 * goods receipt showed its multilingual status as "SUBMITTED" in a Bulgarian UI, while the record's
 * own page showed it translated.
 * <p>
 * The form's fetch client sends the Region & Language setting as {@code Accept-Language}, the
 * related name is kept beside the foreign key instead of replacing it, and the step indicator
 * matches the status on its id - so the translated name shows, the current step is still marked,
 * and a completion posts ids back.
 */
class TaskFormRelationLanguageHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;
    private static final long POLL_INTERVAL_MILLIS = 250;
    private static final long PROCESS_TIMEOUT_SECONDS = 60;

    private static final String PROJECT = "task-language-it";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String API = "/services/java/" + PROJECT + "/gen/tasklanguage/api/";
    private static final String RECEIPTS = API + "receipt/ReceiptController";
    private static final String STORES = API + "store/StoreController";
    private static final String FORM = "/services/web/" + PROJECT + "/gen/PostReceipt/forms/PostReceipt/index.html";
    private static final String TASKS = "/services/inbox/tasks";
    private static final String LANGUAGE_KEY = "codbex.harmonia.language";

    private static final String PASSWORD = "task-language-it-password";
    private static final String POSTER = "task-language-it-poster";

    private static final String INTENT_YAML = """
            name: tasklanguage
            languages: [en, bg]
            entities:
              - name: ReceiptStatus
                kind: setting
                multilingual: true
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 40 }
              - name: Store
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 40 }
              - name: Receipt
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, length: 40 }
                relations:
                  - { name: Store,  kind: manyToOne, to: Store }
                  - { name: Status, kind: manyToOne, to: ReceiptStatus, function: EntityStatus, init: 1 }
            processes:
              - name: ReceiptPosting
                trigger: { onCreate: Receipt }
                steps:
                  - { name: post,   kind: userTask, args: { assignee: TaskLanguagePoster, form: PostReceipt } }
                  - { name: posted, kind: serviceTask, args: { setRelationField: Status, value: 2, next: end } }
                  - { name: end,    kind: end }
            forms:
              - { name: PostReceipt, forEntity: Receipt, fields: [number, Store, Status], actions: [confirm] }
            permissions:
              - { role: TaskLanguagePoster, can: [Receipt:write, Store:write, ReceiptStatus:read] }
            seeds:
              - name: receipt-statuses
                entity: ReceiptStatus
                rows:
                  - { id: 1, name: SUBMITTED }
                  - { id: 2, name: POSTED }
              - name: receipt-statuses-bg
                entity: ReceiptStatus
                language: bg
                rows:
                  - { id: 1, name: "Подаден" }
                  - { id: 2, name: "Осчетоводен" }
            """;

    /**
     * The read-only values in form order joined by |, then the marker of the step announced as the
     * current one ({@code none} without one).
     */
    private static final String FORM_STATE = """
            var values = Array.from(document.querySelectorAll('div.vbox > span:last-child')).map(s => s.textContent.trim());
            var current = document.querySelector('[x-h-step-indicator-trigger][aria-current="step"] [x-h-step-indicator-marker]');
            return values.join('|') + ';' + (current ? current.textContent.trim() : 'none');
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private IDEFactory ideFactory;

    @Test
    void the_task_form_names_related_records_in_the_users_language_and_keeps_their_ids() {
        generateAndPublish();
        securityUtil.ensureUserInDefaultTenant(POSTER, PASSWORD, "TaskLanguagePoster");
        Configuration.set(DirigibleConfig.APPLICATION_LANGUAGES.getKey(), "en,bg");

        int store = post(STORES, "{\"Name\":\"Main\"}");
        int receipt = post(RECEIPTS, "{\"Number\":\"R-8912\",\"Store\":" + store + "}");
        String task = taskFor(receipt);

        ideFactory.create(browser, POSTER, PASSWORD)
                  .openHomePage();
        // The multilingual status reads in Bulgarian, the plain store keeps its name, and SUBMITTED -
        // the flow's entry - is the current step although its name is translated.
        Selenide.executeJavaScript("localStorage.setItem('" + LANGUAGE_KEY + "', 'bg');");
        browser.openPath(FORM + "?taskId=" + task);
        String bulgarian = "R-8912|Main|Подаден;1";
        assertEquals(bulgarian, poll(FORM_STATE, bulgarian), "the task form must read the related records in the user's language");

        Selenide.executeJavaScript("localStorage.setItem('" + LANGUAGE_KEY + "', 'en');");
        Selenide.refresh();
        String english = "R-8912|Main|SUBMITTED;1";
        assertEquals(english, poll(FORM_STATE, english), "the same form in English must show the English names");

        // The completion posts the ids the form kept: the store is still the store, and the flow moves
        // on to POSTED.
        $$("button").findBy(text("Confirm"))
                    .click();
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(RECEIPTS + "/" + receipt)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Store", equalTo(store))
                                                 .body("Status", equalTo(2)),
                POSTER, PASSWORD, PROCESS_TIMEOUT_SECONDS);
    }

    private int post(String controller, String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(controller)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                POSTER, PASSWORD);
        return id.get();
    }

    /** The posting task of this receipt's instance, found by the business key the trigger stamped. */
    private String taskFor(int receipt) {
        AtomicReference<String> task = new AtomicReference<>();
        restAssuredExecutor.execute(() -> task.set(given().when()
                                                          .get(TASKS + "?type=groups")
                                                          .then()
                                                          .statusCode(200)
                                                          .body("find { it.processInstanceBusinessKey == '" + receipt + "' }.name",
                                                                  equalTo("Post"))
                                                          .extract()
                                                          .path("find { it.processInstanceBusinessKey == '" + receipt + "' }.id")),
                POSTER, PASSWORD, PROCESS_TIMEOUT_SECONDS);
        assertNotNull(task.get(), "the posting task must be the poster's");
        return task.get();
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
        Selenide.executeJavaScript("localStorage.removeItem('" + LANGUAGE_KEY + "');");
        Configuration.remove(DirigibleConfig.APPLICATION_LANGUAGES.getKey());
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
