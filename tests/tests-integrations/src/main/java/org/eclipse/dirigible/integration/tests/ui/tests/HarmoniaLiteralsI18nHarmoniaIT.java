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

/**
 * A generated application read in Bulgarian, in a browser whose own locale is English, shows and
 * announces no English text of the templates or the shell (dirigible #8778): the "New ..." button
 * of an empty list is one sentence with the entity's label inside it, a failed custom action toasts
 * in Bulgarian, a date picker's trigger is announced in Bulgarian, and a tenant export's time
 * follows the application's date format rather than the browser's.
 */
class HarmoniaLiteralsI18nHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;
    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final String PROJECT = "literalsit";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/literals/index.html#";

    private static final String INTENT_YAML = """
            name: literals
            languages: [en, bg]
            entities:
              - name: Currency
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: code,   type: string, length: 3 }
                  - { name: issued, type: date }
            """;

    private static final String BG_OVERLAY = """
            {
              "literals-model": {
                "t": { "LITERALS_CURRENCY": "Валута" }
              }
            }
            """;

    /** The texts of the visible buttons of the empty list, once it has rendered. */
    private static final String LIST_BUTTONS = """
            var root = document.querySelector('[x-data$="ListPage"]');
            if (!root) return 'pending';
            return Array.from(root.querySelectorAll('button'))
                        .filter(function (b) { return b.offsetParent !== null; })
                        .map(function (b) { return b.innerText.trim(); })
                        .join('|') || 'pending';
            """;

    /** The accessible name of the form's first date picker trigger. */
    private static final String DATE_TRIGGER = """
            var trigger = document.querySelector('[x-h-date-picker-trigger]');
            return trigger ? (trigger.getAttribute('aria-label') || 'pending') : 'pending';
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void a_bulgarian_user_reads_no_english_text_of_the_templates_or_the_shell() {
        generate();
        write(PROJECT_PATH + "/i18n/bg-BG/literals.model.json", BG_OVERLAY);
        // The platform registers en-US only; a deployment that speaks Bulgarian registers the locale.
        write(PROJECT_PATH + "/configs/bg-BG.json",
                "{ \"id\": \"bg-BG\", \"label\": \"Български\", \"common\": \"/" + PROJECT + "/i18n/bg-BG/common.json\" }");
        write(PROJECT_PATH + "/i18n/bg-BG/common.json", "{}");
        write(PROJECT_PATH + "/extensions/bg-BG.extension",
                "{ \"extensionPoint\": \"application-locales\", \"module\": \"" + PROJECT + "/configs/bg-BG.json\" }");
        publish();
        ide.openHomePage();
        Configuration.set(DirigibleConfig.APPLICATION_LANGUAGES.getKey(), "en,bg");
        Selenide.executeJavaScript("localStorage.setItem('codbex.harmonia.language', 'bg');"
                + "localStorage.setItem('codbex.harmonia.format.datetime', 'dd.MM.yyyy HH:mm');");

        // The empty list's New button: one sentence, the label lower-cased inside it.
        browser.openPath(APP + "/Currency");
        Selenide.refresh();
        String buttons = poll(LIST_BUTTONS, "Създаване на валута");
        assertTrue(buttons.contains("Създаване на валута"), "the empty list must offer \"Създаване на валута\": " + buttons);

        // A failed custom action's toast, and a tenant export's time in the application's format.
        assertEquals("Действието е неуспешно",
                Selenide.executeJavaScript("return Alpine.store('customActions').failureMessage({ httpStatus: 500 });"));
        String exported = Selenide.executeJavaScript("return Alpine.store('tenantExport').time('2026-11-19T22:30:00Z');");
        assertTrue(exported != null && exported.matches("\\d{2}\\.\\d{2}\\.2026 \\d{2}:\\d{2}"),
                "a tenant export's time must follow the application's format, not the browser's: " + exported);

        // The date picker's trigger is announced in Bulgarian.
        browser.openPath(APP + "/Currency/create");
        Selenide.refresh();
        assertEquals("Избор на дата", poll(DATE_TRIGGER, "Избор"), "the date picker trigger's accessible name");
    }

    private static String poll(String script, String expected) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
        String value;
        do {
            value = Selenide.executeJavaScript(script);
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
        Selenide.executeJavaScript(
                "localStorage.removeItem('codbex.harmonia.language');" + "localStorage.removeItem('codbex.harmonia.format.datetime');");
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
