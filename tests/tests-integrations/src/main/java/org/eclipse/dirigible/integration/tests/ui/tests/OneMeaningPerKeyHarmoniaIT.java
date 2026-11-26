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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * #8779: a key has one meaning, so a translated page says what the English one says. The Save
 * button of a generated document used {@code defaults.update} with the fallback "Save" - English
 * read "Save" and Bulgarian read "Обнови" (Update) - and every empty table used
 * {@code messages.noData}, so the line items and a record list both read "Няма налични данни."
 * whatever the English said. Each now has its own key.
 *
 * <p>
 * This needs a real browser: the words are resolved by the generated page through the shared i18n
 * runtime, in the language the user picked, from the platform's shared chrome catalog - the
 * application's own catalog carries none of them (#8782).
 */
class OneMeaningPerKeyHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final String PROJECT = "onemeaningit";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/onemeaning/index.html#";
    private static final String INVOICES = "/services/java/" + PROJECT + "/gen/onemeaning/api/invoice/InvoiceController";
    private static final String MODEL_CATALOG = PROJECT_PATH + "/i18n/en-US/onemeaning.model.json";
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String INTENT_YAML = """
            name: onemeaning
            languages: [en, bg]
            entities:
              - name: Invoice
                function: Document
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 100 }
              - name: InvoiceItem
                function: DocumentItem
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 100 }
                relations:
                  - { name: Invoice, kind: manyToOne, to: Invoice, composition: true, required: true }
              - name: InvoiceComment
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: text, type: string, length: 100 }
                relations:
                  - { name: Invoice, kind: manyToOne, to: Invoice, composition: true, required: true }
            """;

    /**
     * The document page once both of its empty collections settled: the visible buttons' texts, then
     * the page's text, separated by a line of {@code ---}.
     */
    private static final String PAGE_TEXT =
            """
                    var root = document.querySelector('[x-data$="DocumentPage"]');
                    if (!root || !window.Alpine) return 'pending';
                    var buttons = [...root.querySelectorAll('button')].filter(b => b.offsetParent !== null).map(b => b.innerText.trim()).filter(t => t);
                    return buttons.join('|') + '\\n---\\n' + root.innerText;
                    """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void the_save_button_and_the_empty_tables_say_the_same_thing_in_every_language() {
        generate();
        // The words below are the platform's: the application's catalog does not copy them (#8782).
        JsonNode catalog = JSON.readTree(repository.getResource(MODEL_CATALOG)
                                                   .getContent())
                               .path("onemeaning-model");
        assertTrue(catalog.has("t") && !catalog.has("defaults") && !catalog.has("messages"),
                "the application's en-US catalog must hold its own labels and none of the shared chrome: " + catalog);
        // The platform registers en-US only; a deployment that speaks Bulgarian registers the locale.
        write(PROJECT_PATH + "/configs/bg-BG.json",
                "{ \"id\": \"bg-BG\", \"label\": \"Български\", \"common\": \"/" + PROJECT + "/i18n/bg-BG/common.json\" }");
        write(PROJECT_PATH + "/i18n/bg-BG/common.json", "{}");
        write(PROJECT_PATH + "/extensions/bg-BG.extension",
                "{ \"extensionPoint\": \"application-locales\", \"module\": \"" + PROJECT + "/configs/bg-BG.json\" }");
        publish();
        int id = createInvoice();

        ide.openHomePage();
        browser.openPath(APP + "/Invoice/" + id + "/edit");
        String english = poll("No records yet.");
        assertTrue(buttons(english).contains("Save"), "the document's save button must read Save: " + english);
        assertTrue(english.contains("No line items yet.") && english.contains("No records yet."),
                "the empty line items and the empty record list must each say what they are: " + english);

        Configuration.set(DirigibleConfig.APPLICATION_LANGUAGES.getKey(), "en,bg");
        Selenide.executeJavaScript("localStorage.setItem('codbex.harmonia.language', 'bg');");
        Selenide.refresh();
        String bulgarian = poll("Все още няма записи.");
        List<String> buttons = buttons(bulgarian);
        assertTrue(buttons.contains("Запази") && !buttons.contains("Обнови"),
                "a bg user's save button must read Запази, never Обнови (Update): " + buttons);
        assertTrue(bulgarian.contains("Все още няма редове."), "the empty line items must read their own sentence: " + bulgarian);
        assertFalse(bulgarian.contains("Няма налични данни."), "no empty table may fall back to the generic sentence: " + bulgarian);
    }

    private static List<String> buttons(String pageText) {
        return List.of(pageText.substring(0, pageText.indexOf("\n---\n"))
                               .split("\\|"));
    }

    private int createInvoice() {
        AtomicReference<Integer> id = new AtomicReference<>();
        restAssuredExecutor.execute(() -> id.set(given().contentType(ContentType.JSON)
                                                        .body(Map.of("Note", "one"))
                                                        .when()
                                                        .post(INVOICES)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")));
        return id.get();
    }

    private static String poll(String expected) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
        String value;
        do {
            value = Selenide.executeJavaScript(PAGE_TEXT);
            if (value != null && value.contains(expected)) {
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
