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
 * #8634: a composition child shown on its master's page reads in the user's language - the
 * section's title, every column header (a plain text column too, not only a relation), the title of
 * its Add dialog, which names the child, and the Preview crumb of the breadcrumb.
 *
 * <p>
 * This needs a real browser: the labels are resolved by the generated page through the shared i18n
 * runtime, in the language the user picked.
 */
class DetailCollectionI18nHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final String PROJECT = "detaili18nit";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/detaili18n/index.html#";
    private static final String COMPANIES = "/services/java/" + PROJECT + "/gen/detaili18n/api/company/CompanyController";

    private static final String INTENT_YAML = """
            name: detaili18n
            languages: [en, bg]
            entities:
              - name: Company
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: CompanyBankAccount
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: iban, type: string }
                relations:
                  - { name: Company, kind: manyToOne, to: Company, composition: true, required: true }
            """;

    /** The child's plural label and its plain text column, as the generated catalog keys them. */
    private static final String CHILD_PLURAL_KEY = "DETAILI18N_COMPANY_BANK_ACCOUNT_plural";
    private static final String IBAN_KEY = "COMPANY_BANK_ACCOUNT_IBAN";

    /** A translator's bg-BG catalog for the two labels the panel used to print in English. */
    private static final String BG_OVERLAY = """
            {
              "detaili18n-model": {
                "t": {
                  "%s": "Банкови сметки",
                  "%s": "Международна сметка"
                }
              }
            }
            """.formatted(CHILD_PLURAL_KEY, IBAN_KEY);

    private static final String PANEL = "document.querySelector('[x-data^=\"detailPanel\"]')";

    /** The panel's title and header cells once it rendered, joined by {@code |}. */
    private static final String PANEL_TEXT = """
            var panel = %s;
            if (!panel || !window.Alpine) return 'not-rendered';
            var title = panel.querySelector('h3');
            var heads = [...panel.querySelectorAll('thead th')].map(th => th.innerText.trim()).filter(t => t);
            return title ? [title.innerText.trim()].concat(heads).join('|') : 'pending';
            """.formatted(PANEL);

    /** The breadcrumb's last crumb (the page it names, never a link). */
    private static final String LAST_CRUMB = """
            var crumbs = [...document.querySelectorAll('[x-h-breadcrumb-page]')].filter(c => c.offsetParent !== null);
            return crumbs.length ? crumbs[crumbs.length - 1].innerText.trim() : 'pending';
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void a_detail_collection_reads_in_the_language_the_user_picked() {
        generate();
        String catalog = new String(repository.getResource(PROJECT_PATH + "/i18n/en-US/detaili18n.model.json")
                                              .getContent(),
                StandardCharsets.UTF_8);
        assertTrue(catalog.contains("\"" + CHILD_PLURAL_KEY + "\"") && catalog.contains("\"" + IBAN_KEY + "\""),
                "the en-US catalog must key the child's title and its column: " + catalog);
        write(PROJECT_PATH + "/i18n/bg-BG/detaili18n.model.json", BG_OVERLAY);
        // The platform registers en-US only; a deployment that speaks Bulgarian registers the locale.
        write(PROJECT_PATH + "/configs/bg-BG.json",
                "{ \"id\": \"bg-BG\", \"label\": \"Български\", \"common\": \"/" + PROJECT + "/i18n/bg-BG/common.json\" }");
        write(PROJECT_PATH + "/i18n/bg-BG/common.json", "{}");
        write(PROJECT_PATH + "/extensions/bg-BG.extension",
                "{ \"extensionPoint\": \"application-locales\", \"module\": \"" + PROJECT + "/configs/bg-BG.json\" }");
        publish();
        int companyId = createCompany("Acme");

        ide.openHomePage();
        Selenide.executeJavaScript("localStorage.setItem('codbex.harmonia.language', 'bg');");
        browser.openPath(APP + "/Company/" + companyId + "/edit");
        Selenide.refresh();
        assertEquals("Банкови сметки|Международна сметка", poll(PANEL_TEXT, text -> text.startsWith("Банкови")),
                "the section's title and its plain text column must be translated");

        evaluate("Alpine.$data(" + PANEL + ").addRow(); return '';");
        assertEquals("Банкови сметки: нов запис",
                poll("var title = Alpine.store('related').title; return title ? String(title) : 'pending';", title -> true),
                "the Add dialog must name the child it adds, in the user's language");

        browser.openPath(APP + "/Company/" + companyId + "/preview");
        Selenide.refresh();
        assertEquals("Преглед", poll(LAST_CRUMB, crumb -> !"Edit".equals(crumb) && !"Редактиране".equals(crumb)),
                "the Preview crumb must be translated");
        assertEquals("Банкови сметки|Международна сметка", poll(PANEL_TEXT, text -> text.startsWith("Банкови")),
                "the Preview page's section must be translated too");
    }

    private int createCompany(String name) {
        AtomicReference<Integer> id = new AtomicReference<>();
        restAssuredExecutor.execute(() -> id.set(given().contentType(ContentType.JSON)
                                                        .body(Map.of("Name", name))
                                                        .when()
                                                        .post(COMPANIES)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .jsonPath()
                                                        .getInt("Id")));
        return id.get();
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
