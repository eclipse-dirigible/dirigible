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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
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
 * #8789: the "Incomplete translations" part of Settings reports what an application's language
 * tables hold, not the languages it declares. The application below declares {@code bg}; its
 * countries are seeded with a Bulgarian row each and its units with none. Settings names the units
 * (declaring {@code bg} no longer hides an empty language table) and not the countries - until a
 * country is added without a Bulgarian row, which it then counts.
 *
 * <p>
 * This needs a real browser: the warning is composed by the shell from the coverage each entity's
 * controller answers, in the language the user picked.
 */
class LanguageDataCoverageHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final String PROJECT = "lingocovit";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String SETTINGS = "/services/web/application/index.html#/settings";

    private static final String INTENT_YAML = """
            name: lingocov
            languages: [en, bg]
            entities:
              - name: Country
                multilingual: true
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }
              - name: Unit
                multilingual: true
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }
            # Ids above the identity's start: the test adds a country, whose id the database draws.
            seeds:
              - name: countries
                entity: Country
                rows:
                  - { id: 101, name: Bulgaria }
                  - { id: 102, name: Greece }
                  - { id: 103, name: Romania }
              - name: countries-bg
                entity: Country
                language: bg
                rows:
                  - { id: 101, name: "България" }
                  - { id: 102, name: "Гърция" }
                  - { id: 103, name: "Румъния" }
              - name: units
                entity: Unit
                rows:
                  - { id: 101, name: Piece }
                  - { id: 102, name: Kilogram }
            """;

    private static final String BG_OVERLAY = """
            {
              "lingocov-model": {
                "t": {
                  "LINGOCOV_COUNTRY_plural": "Държави",
                  "LINGOCOV_UNIT_plural": "Мерни единици"
                }
              }
            }
            """;

    /**
     * The application's warning in the Region and Language section, line by line, or {@code pending}.
     */
    private static final String WARNING = """
            var alert = Array.from(document.querySelectorAll('#app [x-h-alert][data-variant="warning"]'))
                             .find(a => a.innerText.indexOf(arguments[0]) >= 0);
            return alert ? Array.from(alert.querySelectorAll('[x-h-alert-description] p')).map(p => p.innerText.trim()).join('\\n')
                         : 'pending';
            """;

    /** One multilingual entity of the generated config: its coverage URL. */
    private static final Pattern COVERAGE_URL = Pattern.compile("\"url\":\"([^\"]+/%sController/translations)\"");

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void settings_counts_the_records_without_a_translation_whatever_the_app_declares() {
        Configuration.set(DirigibleConfig.APPLICATION_LANGUAGES.getKey(), "en,bg");
        generate();
        write(PROJECT_PATH + "/i18n/bg-BG/lingocov.model.json", BG_OVERLAY);
        // The platform registers en-US only; a deployment that speaks Bulgarian registers the locale.
        write(PROJECT_PATH + "/configs/bg-BG.json",
                "{ \"id\": \"bg-BG\", \"label\": \"Български\", \"common\": \"/" + PROJECT + "/i18n/bg-BG/common.json\" }");
        write(PROJECT_PATH + "/i18n/bg-BG/common.json", "{}");
        write(PROJECT_PATH + "/extensions/bg-BG.extension",
                "{ \"extensionPoint\": \"application-locales\", \"module\": \"" + PROJECT + "/configs/bg-BG.json\" }");
        publish();

        String config = new String(repository.getResource(PROJECT_PATH + "/gen/lingocov/js/config.js")
                                             .getContent(),
                StandardCharsets.UTF_8);
        String countries = coverageUrl(config, "Country");
        String units = coverageUrl(config, "Unit");
        assertCoverage(countries, 3, 0);
        assertCoverage(units, 2, 2);

        ide.openHomePage();
        Selenide.executeJavaScript("localStorage.setItem('codbex.harmonia.language', 'bg');");
        assertEquals("Мерни единици: 2 от 2 записа нямат превод на български.", settingsWarning(),
                "the units declare bg and hold no Bulgarian row; every country has one");

        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body(Map.of("Name", "Serbia"))
                                                 .when()
                                                 .post(countries.substring(0, countries.lastIndexOf('/')))
                                                 .then()
                                                 .statusCode(200));
        assertCoverage(countries, 4, 1);
        assertEquals("Държави: 1 от 4 записа няма превод на български.\nМерни единици: 2 от 2 записа нямат превод на български.",
                settingsWarning(), "a country added without a Bulgarian row is counted");
    }

    private static String coverageUrl(String config, String entity) {
        Matcher matcher = Pattern.compile(COVERAGE_URL.pattern()
                                                      .formatted(entity))
                                 .matcher(config);
        assertTrue(matcher.find(), "the config must list " + entity + " as translatable: " + config);
        return matcher.group(1);
    }

    private void assertCoverage(String url, int total, int untranslated) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(url)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("total", equalTo(total))
                                                 .body("untranslated.bg", equalTo(untranslated)));
    }

    /** Opens Settings afresh (the scan runs once per page) and reads the application's warning. */
    private String settingsWarning() {
        ide.openPath(SETTINGS);
        Selenide.refresh();
        Selenide.$(By.xpath("//div[@id='app']//button[.//*[@data-lucide='globe']]"))
                .click();
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
        String value;
        do {
            value = Selenide.executeJavaScript(WARNING, PROJECT);
            if (value != null && !"pending".equals(value)) {
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
