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
import java.util.function.Predicate;

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
 * #8564: a boolean of a generated list reads as words, never as the raw {@code true} /
 * {@code false} - in the list cell, the record sheet, the filter chip and the export. A field
 * declaring {@code labels:} reads as its own words, translated by the language catalogs under
 * {@code <COLUMN>_TRUE} / {@code _FALSE} and falling back to the authored word; a field without
 * them reads as the module's shared Yes / No.
 *
 * <p>
 * This needs a real browser: the words are resolved by the generated page through the shared i18n
 * runtime, in the language the user picked.
 */
class BooleanWordsHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final String PROJECT = "boolwordsit";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/boolwords/index.html#";
    private static final String PRODUCTS = "/services/java/" + PROJECT + "/gen/boolwords/api/product/ProductController";

    private static final String INTENT_YAML = """
            name: boolwords
            languages: [en, bg]
            entities:
              - name: Product
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: name,     type: string }
                  - name: active
                    type: boolean
                    labels: { true: Active, false: Inactive }
                  - { name: archived, type: boolean }
            """;

    /**
     * A translator's bg-BG catalog: it translates the word for {@code true} only, so {@code false}
     * falls back to the authored word.
     */
    private static final String BG_OVERLAY = """
            {
              "boolwords-model": {
                "t": {
                  "PRODUCT_ACTIVE_TRUE": "Активен"
                }
              }
            }
            """;

    private static final String PAGE = "Alpine.$data(document.querySelector('[x-data=\"ProductManageListPage\"]'))";

    /**
     * The rendered table once the rows arrived: each row's cell texts joined by {@code |}, rows sorted
     * and separated by {@code ;}.
     */
    private static final String TABLE = """
            var root = document.querySelector('[x-data="ProductManageListPage"]');
            if (!root || !window.Alpine) return 'not-rendered';
            var page = Alpine.$data(root);
            if (page.state === 'loading' || page.items.length < 2) return 'pending';
            return [...root.querySelectorAll('tbody tr')]
              .map(tr => [...tr.querySelectorAll('td:not([data-row-actions])')].map(td => td.innerText.trim()).filter(t => t).join('|'))
              .filter(r => r).sort().join(';');
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void a_boolean_reads_as_its_words_in_every_language() {
        generate();
        String catalog = new String(repository.getResource(PROJECT_PATH + "/i18n/en-US/boolwords.model.json")
                                              .getContent(),
                StandardCharsets.UTF_8);
        assertTrue(catalog.contains("\"PRODUCT_ACTIVE_TRUE\": \"Active\"") && catalog.contains("\"PRODUCT_ACTIVE_FALSE\": \"Inactive\""),
                "the en-US catalog must carry the field's words next to its label: " + catalog);
        write(PROJECT_PATH + "/i18n/bg-BG/boolwords.model.json", BG_OVERLAY);
        // The platform registers en-US only; a deployment that speaks Bulgarian registers the locale.
        write(PROJECT_PATH + "/configs/bg-BG.json",
                "{ \"id\": \"bg-BG\", \"label\": \"Български\", \"common\": \"/" + PROJECT + "/i18n/bg-BG/common.json\" }");
        write(PROJECT_PATH + "/i18n/bg-BG/common.json", "{}");
        write(PROJECT_PATH + "/extensions/bg-BG.extension",
                "{ \"extensionPoint\": \"application-locales\", \"module\": \"" + PROJECT + "/configs/bg-BG.json\" }");
        publish();
        createProduct("on", true, false);
        createProduct("off", false, true);

        ide.openHomePage();
        browser.openPath(APP + "/Product");
        assertEquals("off|Inactive|Yes;on|Active|No", poll(TABLE, table -> !table.contains("—")),
                "a boolean cell must read as the field's words, or as Yes / No without them");
        assertEquals("Active|No",
                evaluate("var page = " + PAGE + "; var row = page.items.find(r => r.Name === 'on');"
                        + " return page.exportColumns.filter(c => c.bool).map(c => page.exportCell(row, c)).join('|');"),
                "the export must write the same words");
        assertEquals("Inactive",
                evaluate("var page = " + PAGE + "; page.columnFilters = { Active: 'false' };"
                        + " return page.filterValueText(page.filterColumns.find(c => c.name === 'Active'));"),
                "the filter chip must name the word");
        evaluate("var page = " + PAGE + "; page.columnFilters = {}; page.selectRow(page.items.find(r => r.Name === 'on')); return '';");
        String sheet = poll("return document.body.innerText;", text -> text.contains("Back to list"));
        assertTrue(sheet.contains("Active") && !sheet.matches("(?s).*\\b(true|false)\\b.*"),
                "the record sheet must show the word, never true / false: " + sheet);

        Selenide.executeJavaScript("localStorage.setItem('codbex.harmonia.language', 'bg');");
        browser.openPath(APP + "/Product");
        Selenide.refresh();
        String translated = poll(TABLE, table -> table.contains("Активен"));
        assertEquals("off|Inactive|Да;on|Активен|Не", translated,
                "a translated word must win, an untranslated one must fall back to the authored word, and Yes / No must translate");
        Selenide.executeJavaScript("localStorage.removeItem('codbex.harmonia.language');");
    }

    private void createProduct(String name, boolean active, boolean archived) {
        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body(Map.of("Name", name, "Active", active, "Archived", archived))
                                                 .when()
                                                 .post(PRODUCTS)
                                                 .then()
                                                 .statusCode(200));
    }

    private static String evaluate(String script) {
        return Selenide.executeJavaScript(script);
    }

    private static String poll(String script, Predicate<String> settled) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
        String value;
        do {
            value = Selenide.executeJavaScript(script);
            if (value != null && !"pending".equals(value) && !"not-rendered".equals(value) && settled.test(value)) {
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
