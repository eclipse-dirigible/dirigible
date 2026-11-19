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
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * A relation picker over a multilingual entity reads the generated controller's
 * {@code POST /search} (dirigible #8633): its rows come back in the caller's language, and a
 * {@code CONTAINS} typed in that language finds the row by its translation - with
 * {@code POST /count} counting the same rows. The entity declares {@code orderBy:}, so its list
 * ({@code GET /}) reads the ordered {@code findAll(limit, offset, order)}, which is translated too
 * (dirigible #8670).
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MultilingualSearchIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "multilingual-search";
    private static final String COUNTRIES = "/services/java/" + PROJECT + "/gen/lingo/api/settings/CountryController";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String INTENT = """
            name: lingo
            description: a picker over a multilingual entity searches in the reader's language
            languages: [en, bg]

            entities:
              - name: Country
                kind: setting
                multilingual: true
                # The list reads the ordered findAll(limit, offset, order) - translated too (#8670).
                orderBy: [name]
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string,  required: true, length: 100 }

            seeds:
              - name: countries
                entity: Country
                rows:
                  - { id: 1, name: Afghanistan }
                  - { id: 2, name: Albania }
                  - { id: 3, name: Bulgaria }
              - name: countries-bg
                entity: Country
                language: bg
                rows:
                  - { id: 1, name: "Афганистан" }
                  - { id: 2, name: "Албания" }
                  - { id: 3, name: "България" }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void ordered_list_reads_the_callers_language() {
        writeResource(projectPath() + "/app.intent", INTENT);
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        // GET / and GET /{id} come back in the caller's language - the list through the ordered
        // findAll(limit, offset, order) that orderBy: switches it to (#8670).
        assertList("bg", contains("Афганистан", "Албания", "България"));
        assertList(null, contains("Afghanistan", "Albania", "Bulgaria"));
        restAssuredExecutor.execute(() -> request("bg").when()
                                                       .get(COUNTRIES + "/3")
                                                       .then()
                                                       .statusCode(200)
                                                       .body("Name", equalTo("България")),
                TIMEOUT_SECONDS);
    }

    private void assertList(String language, Matcher<?> matcher) {
        restAssuredExecutor.execute(() -> request(language).when()
                                                           .get(COUNTRIES + "?$limit=10")
                                                           .then()
                                                           .statusCode(200)
                                                           .body("Name", matcher),
                TIMEOUT_SECONDS);
    }

    @Test
    void search_reads_and_matches_the_callers_language() {
        writeResource(projectPath() + "/app.intent", INTENT);
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        // The page /search answers is translated, as GET / and GET /{id} already were.
        assertSearch("{}", "bg", "Name", contains("Афганистан", "Албания", "България"));
        assertSearch("{}", null, "Name", contains("Afghanistan", "Albania", "Bulgaria"));

        // A CONTAINS typed in the reader's language finds the row by its translation, case-insensitively,
        // and the base value still matches as before.
        String typedBg = nameContains("БЪЛГ");
        assertSearch(typedBg, "bg", "Id", contains(3));
        assertSearch(typedBg, "bg", "Name", contains("България"));
        assertSearch(nameContains("Bulg"), "bg", "Id", contains(3));
        assertSearch(nameContains("ан"), "bg", "Id", containsInAnyOrder(1, 2));

        // The count reads the same condition.
        assertCount(typedBg, "bg", 1);
        assertCount(nameContains("ан"), "bg", 2);

        // Without the language, a Bulgarian condition matches nothing.
        assertSearch(typedBg, null, "Id", empty());
        assertCount(typedBg, null, 0);
    }

    private static String nameContains(String text) {
        return "{\"conditions\":[{\"propertyName\":\"Name\",\"operator\":\"CONTAINS\",\"value\":\"" + text + "\"}]}";
    }

    private void assertSearch(String body, String language, String path, Matcher<?> matcher) {
        restAssuredExecutor.execute(() -> request(language).body(body)
                                                           .when()
                                                           .post(COUNTRIES + "/search")
                                                           .then()
                                                           .statusCode(200)
                                                           .body(path, matcher),
                TIMEOUT_SECONDS);
    }

    private void assertCount(String body, String language, int count) {
        restAssuredExecutor.execute(() -> request(language).body(body)
                                                           .when()
                                                           .post(COUNTRIES + "/count")
                                                           .then()
                                                           .statusCode(200)
                                                           .body("count", equalTo(count)),
                TIMEOUT_SECONDS);
    }

    private static RequestSpecification request(String language) {
        RequestSpecification request = given().contentType(ContentType.JSON);
        return language == null ? request : request.header("Accept-Language", language);
    }

    private void generateProject() {
        AtomicReference<List<Map<String, Object>>> plan = new AtomicReference<>();
        restAssuredExecutor.execute(() -> plan.set(given().when()
                                                          .post("/services/ide/intent/generate?workspace=" + WORKSPACE + "&project="
                                                                  + PROJECT + "&path=app.intent")
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

    private void publishProject() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT + "/")
                                                 .then()
                                                 .statusCode(200));
    }

    private void writeResource(String path, String content) {
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(content.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String projectPath() {
        return IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    }

    @AfterEach
    void cleanup() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT)
                                                 .then()
                                                 .statusCode(both(greaterThanOrEqualTo(200)).and(lessThan(300))));
        if (repository.hasCollection(projectPath())) {
            repository.removeCollection(projectPath());
        }
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
