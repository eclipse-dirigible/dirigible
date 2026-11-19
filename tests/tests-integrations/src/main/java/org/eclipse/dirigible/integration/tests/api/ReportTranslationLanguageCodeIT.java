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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A translation row is matched by one rule in a report and on the entity's own pages (#8737). The
 * entity repository's {@code Translator} compares the {@code
 *
<TABLE>
 * _LANG} row's code trimmed and case-insensitively; the report's overlay join compared it exactly,
 * so a row a hand-written CSV or SQL stored as {@code BG} translated the list and was ignored by
 * the report beside it.
 */
class ReportTranslationLanguageCodeIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "report-language-code";
    private static final String UNITS = "/services/java/" + PROJECT + "/gen/langcode/api/settings/UnitController";
    private static final String REPORT = "/services/java/" + PROJECT + "/gen/claimsbyunit/api/reports/ClaimsByUnitController";
    private static final String LANGUAGE_TABLE = "LANGCODE_UNIT_LANG";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String INTENT = """
            name: langcode
            description: a report translates a row whose language code is stored in another case
            languages: [en, bg]

            entities:
              - name: Unit
                kind: setting
                multilingual: true
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string,  required: true, length: 100 }
              - name: Claim
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 100 }
                relations:
                  - { name: Unit, kind: manyToOne, to: Unit }

            reports:
              - name: ClaimsByUnit
                source: Claim
                dimensions: [Unit]
                measures: ["count(*)"]

            seeds:
              - name: units
                entity: Unit
                rows:
                  - { id: 1, name: Piece }
              - name: units-bg
                entity: Unit
                language: bg
                rows:
                  - { id: 1, name: "Брой" }
              - name: claims
                entity: Claim
                rows:
                  - { id: 1, note: first, Unit: 1 }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Test
    void a_report_translates_a_row_whose_language_code_is_stored_upper_case() {
        writeResource(projectPath() + "/app.intent", INTENT);
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();
        assertUnitName("Брой");

        // The translation as a hand-written CSV or SQL loads it.
        storeLanguageCode("BG");

        // The list still reads it, as it always did ...
        assertUnitName("Брой");
        // ... and the report beside it now reads it too.
        restAssuredExecutor.execute(() -> given().header("Accept-Language", "bg-BG")
                                                 .when()
                                                 .get(REPORT)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("[0].Unit", equalTo("Брой")),
                TIMEOUT_SECONDS);
        // A caller in another language still reads the base value.
        restAssuredExecutor.execute(() -> given().header("Accept-Language", "de")
                                                 .when()
                                                 .get(REPORT)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("[0].Unit", equalTo("Piece")),
                TIMEOUT_SECONDS);
    }

    private void assertUnitName(String name) {
        restAssuredExecutor.execute(() -> given().header("Accept-Language", "bg-BG")
                                                 .when()
                                                 .get(UNITS + "/1")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Name", equalTo(name)),
                TIMEOUT_SECONDS);
    }

    private void storeLanguageCode(String code) {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                PreparedStatement update = connection.prepareStatement("UPDATE \"" + LANGUAGE_TABLE + "\" SET \"Language\" = ?")) {
            update.setString(1, code);
            assertEquals(1, update.executeUpdate(), "the seeded translation must exist");
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to store the language code", ex);
        }
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
