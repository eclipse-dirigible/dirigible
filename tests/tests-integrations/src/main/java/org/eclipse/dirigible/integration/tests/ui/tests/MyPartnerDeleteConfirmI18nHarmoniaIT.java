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

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
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
 * #8733: the delete confirmation of the generated my and partner record pages - the form and the
 * document of each - names what it deletes in the user's language. The shared catalog text
 * interpolates {@code {{name}}}, and these four pages used to call it without one, so a bg user
 * read "изтриете ?" with a gap where the record's label belongs.
 *
 * <p>
 * This needs a real browser: the text is resolved by the generated page through the shared i18n
 * runtime, in the language the user picked.
 */
class MyPartnerDeleteConfirmI18nHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final String PROJECT = "delconfirmit";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/delconfirm/index.html#";
    private static final String API = "/services/java/" + PROJECT + "/gen/delconfirm/api";

    private static final String INTENT_YAML = """
            name: delconfirm
            languages: [en, bg]
            entities:
              - name: Employee
                identity: email
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: email, type: string, length: 100, unique: true }
              - name: Timesheet
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 100 }
                relations:
                  - { name: Employee, kind: manyToOne, to: Employee, required: true, personal: true }
              - name: Expense
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 100 }
                relations:
                  - { name: Employee, kind: manyToOne, to: Employee, required: true, partner: true }
              - name: Claim
                function: Document
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 100 }
                relations:
                  - { name: Employee, kind: manyToOne, to: Employee, required: true, personal: true }
              - name: ClaimItem
                function: DocumentItem
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 100 }
                relations:
                  - { name: Claim, kind: manyToOne, to: Claim, composition: true, required: true }
              - name: Bill
                function: Document
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 100 }
                relations:
                  - { name: Employee, kind: manyToOne, to: Employee, required: true, partner: true }
              - name: BillItem
                function: DocumentItem
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 100 }
                relations:
                  - { name: Bill, kind: manyToOne, to: Bill, composition: true, required: true }
            """;

    /**
     * A record page under test: its route, its entity, the entity's catalog key and its bg-BG label.
     */
    private record Page(String route, String entity, String key, String bgLabel) {
    }

    private static final List<Page> PAGES = List.of(new Page("my/Timesheet", "Timesheet", "DELCONFIRM_TIMESHEET", "Работна карта"),
            new Page("partner/Expense", "Expense", "DELCONFIRM_EXPENSE", "Разход"),
            new Page("my/Claim", "Claim", "DELCONFIRM_CLAIM", "Претенция"), new Page("partner/Bill", "Bill", "DELCONFIRM_BILL", "Сметка"));

    private static final String BG_OVERLAY = """
            {
              "delconfirm-model": {
                "t": {
                  "DELCONFIRM_TIMESHEET": "Работна карта",
                  "DELCONFIRM_EXPENSE": "Разход",
                  "DELCONFIRM_CLAIM": "Претенция",
                  "DELCONFIRM_BILL": "Сметка"
                }
              }
            }
            """;

    /**
     * Opens the page's delete confirmation once the record is loaded and answers its text, or
     * {@code pending}.
     */
    private static final String CONFIRMATION_TEXT = """
            var root = document.querySelector('[x-data$="FormPage"], [x-data$="DocumentPage"]');
            if (!root || !window.Alpine) return 'pending';
            var page = Alpine.$data(root);
            if (page.mode !== 'edit') return 'pending';
            page.deleteOpen = true;
            var text = root.querySelector('[x-h-dialog-overlay][data-open="true"] [x-h-dialog-content] p');
            return text ? text.innerText.trim() : 'pending';
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void the_delete_confirmation_of_the_my_and_partner_pages_names_the_record_in_the_users_language() {
        generate();
        String catalog = new String(repository.getResource(PROJECT_PATH + "/i18n/en-US/delconfirm.model.json")
                                              .getContent(),
                StandardCharsets.UTF_8);
        for (Page page : PAGES) {
            assertTrue(catalog.contains("\"" + page.key() + "\""),
                    "the en-US catalog must key the " + page.entity() + " label: " + catalog);
        }
        write(PROJECT_PATH + "/i18n/bg-BG/delconfirm.model.json", BG_OVERLAY);
        // The platform registers en-US only; a deployment that speaks Bulgarian registers the locale.
        write(PROJECT_PATH + "/configs/bg-BG.json",
                "{ \"id\": \"bg-BG\", \"label\": \"Български\", \"common\": \"/" + PROJECT + "/i18n/bg-BG/common.json\" }");
        write(PROJECT_PATH + "/i18n/bg-BG/common.json", "{}");
        write(PROJECT_PATH + "/extensions/bg-BG.extension",
                "{ \"extensionPoint\": \"application-locales\", \"module\": \"" + PROJECT + "/configs/bg-BG.json\" }");
        publish();
        // The browser signs in as admin: admin is the employee and the partner these records belong to.
        int admin = create("/employee/EmployeeController", Map.of("Email", "admin"));

        ide.openHomePage();
        Configuration.set(DirigibleConfig.APPLICATION_LANGUAGES.getKey(), "en,bg");
        Selenide.executeJavaScript("localStorage.setItem('codbex.harmonia.language', 'bg');");
        for (Page page : PAGES) {
            String entity = page.entity();
            int id = create("/" + entity.toLowerCase() + "/" + entity + "Controller", Map.of("Employee", admin, "Note", "one"));
            browser.openPath(APP + "/" + page.route() + "/" + id + "/edit");
            Selenide.refresh();
            assertEquals("Сигурни ли сте, че искате да изтриете " + page.bgLabel() + "? Действието не може да бъде отменено.",
                    poll(CONFIRMATION_TEXT, "Сигурни"), "the " + page.route() + " delete confirmation must name the record");
        }
    }

    private int create(String controller, Map<String, Object> body) {
        AtomicReference<Integer> id = new AtomicReference<>();
        restAssuredExecutor.execute(() -> id.set(given().contentType(ContentType.JSON)
                                                        .body(body)
                                                        .when()
                                                        .post(API + controller)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")));
        return id.get();
    }

    private static String poll(String script, String prefix) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
        String value;
        do {
            value = Selenide.executeJavaScript(script);
            if (value != null && value.startsWith(prefix)) {
                return value;
            }
            Selenide.sleep(POLL_INTERVAL_MILLIS);
        } while (System.currentTimeMillis() < deadline);
        return value;
    }

    private void write(String path, String content) {
        if (repository.hasResource(path)) {
            repository.getResource(path)
                      .setContent(content.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void generate() {
        write(PROJECT_PATH + "/app.intent", INTENT_YAML);
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
    }

    private void publish() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT + "/")
                                                 .then()
                                                 .statusCode(200));
        synchronizationProcessor.forceProcessSynchronizers();
    }

    @AfterEach
    void cleanup() {
        Selenide.executeJavaScript("localStorage.removeItem('codbex.harmonia.language');");
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
