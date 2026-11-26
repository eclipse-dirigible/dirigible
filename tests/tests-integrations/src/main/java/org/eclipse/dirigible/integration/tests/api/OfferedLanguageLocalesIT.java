/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import java.nio.charset.StandardCharsets;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * A language the instance offers ({@code DIRIGIBLE_APPLICATION_LANGUAGES}) loads its catalogs
 * without any project registering it as a locale (#8774), a project that does register it keeps its
 * label and common catalog, and a language no locale stands for is the caller's error, not a 500
 * (#8159).
 */
// One Dirigible boot for the whole class: each method removes its project and its setting.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OfferedLanguageLocalesIT extends IntegrationTest {

    private static final String PROJECT = "offered-locale-it";

    private static final String PROJECT_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT;

    private static final String LOCALES = "/services/platform/extensions/locales";

    private static final String BG = "translations.'bg-BG'";

    private static final long ASSERTION_TIMEOUT_SECONDS = 30;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @BeforeEach
    void offerBulgarian() {
        Configuration.set(DirigibleConfig.APPLICATION_LANGUAGES.getKey(), "en,bg");
        write("/i18n/en-US/orders.json", "{ \"title\": \"Orders\" }");
        write("/i18n/bg-BG/orders.json", "{ \"title\": \"Поръчки\" }");
    }

    @AfterEach
    void removeProject() {
        Configuration.remove(DirigibleConfig.APPLICATION_LANGUAGES.getKey());
        if (repository.hasCollection(PROJECT_PATH)) {
            repository.removeCollection(PROJECT_PATH);
        }
        synchronizationProcessor.forceProcessSynchronizers();
    }

    @Test
    void an_offered_language_loads_the_shell_and_module_catalogs_without_a_registration() {
        synchronizationProcessor.forceProcessSynchronizers();

        restAssuredExecutor.execute(() -> given().queryParam("extensionPoints", "platform-locales", "application-locales")
                                                 .queryParam("langs", "bg")
                                                 .queryParam("namespaces", "application-core", PROJECT)
                                                 .when()
                                                 .get(LOCALES)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("locales.id", hasItem("bg-BG"))
                                                 .body("locales.find { it.id == 'bg-BG' }.label", equalTo("Български"))
                                                 .body(BG + ".common", anEmptyMap())
                                                 .body(BG + ".'application-core'.shell.nav.dashboard", equalTo("Табло"))
                                                 .body(BG + ".'" + PROJECT + "'.title", equalTo("Поръчки")),
                ASSERTION_TIMEOUT_SECONDS);
    }

    @Test
    void a_registered_locale_keeps_its_label_and_common_catalog() {
        write("/configs/bg-BG.json",
                "{ \"id\": \"bg-BG\", \"label\": \"Bulgarian (registered)\", \"common\": \"/" + PROJECT + "/i18n/bg-BG/common.json\" }");
        write("/i18n/bg-BG/common.json", "{ \"save\": \"Запиши\" }");
        write("/extensions/bg-BG.extension",
                "{ \"extensionPoint\": \"application-locales\", \"module\": \"" + PROJECT + "/configs/bg-BG.json\" }");
        synchronizationProcessor.forceProcessSynchronizers();

        restAssuredExecutor.execute(() -> given().queryParam("extensionPoints", "platform-locales", "application-locales")
                                                 .queryParam("langs", "bg")
                                                 .queryParam("namespaces", PROJECT)
                                                 .when()
                                                 .get(LOCALES)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("locales.findAll { it.id == 'bg-BG' }", hasSize(1))
                                                 .body("locales.find { it.id == 'bg-BG' }.label", equalTo("Bulgarian (registered)"))
                                                 .body(BG + ".common.save", equalTo("Запиши"))
                                                 .body(BG + ".'" + PROJECT + "'.title", equalTo("Поръчки")),
                ASSERTION_TIMEOUT_SECONDS);
    }

    @Test
    void a_language_code_resolves_and_an_unknown_one_is_the_callers_error() {
        given().queryParam("langs", "en")
               .queryParam("namespaces", "common")
               .when()
               .get(LOCALES)
               .then()
               .statusCode(200)
               .body("translations.'en-US'.common", notNullValue());

        given().queryParam("langs", "fr")
               .when()
               .get(LOCALES)
               .then()
               .statusCode(404)
               .body("message", containsString("'fr'"));
    }

    private void write(String path, String content) {
        repository.createResource(PROJECT_PATH + path, content.getBytes(StandardCharsets.UTF_8), false, "application/json", true);
    }
}
