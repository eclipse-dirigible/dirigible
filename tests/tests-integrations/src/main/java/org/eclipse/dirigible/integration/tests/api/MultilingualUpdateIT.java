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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
import org.eclipse.dirigible.tests.framework.security.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;

/**
 * Saving a multilingual record in a language it is translated into keeps its base values (dirigible
 * #8735). The generated form sends back the whole record it read - translated - and the
 * property-security update merges the payload onto the translated {@code findOne}; either way the
 * translation used to become the base value and the default-language text was lost. Now a property
 * the read overlays keeps its stored value, the other properties are stored as sent, and a write
 * the read did not translate (the default language, a record without a translation) edits the base
 * row as before.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MultilingualUpdateIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "multilingual-update";
    private static final String API = "/services/java/" + PROJECT + "/gen/lingual/api/settings/";
    private static final String UNITS = API + "UnitController";
    private static final String GRADES = API + "GradeController";
    private static final long TIMEOUT_SECONDS = 90;
    private static final String ADMIN = "admin";
    private static final String ADMIN_PASSWORD = "admin";
    private static final String CLERK = "grade-clerk";
    private static final String CLERK_PASSWORD = "grade-clerk-password";

    private static final String INTENT = """
            name: lingual
            description: saving a record in a translated language keeps its base values
            languages: [en, bg]

            entities:
              # The plain update: the controller stores the payload through repository.update.
              - name: Unit
                kind: setting
                multilingual: true
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: name,  type: string,  required: true, length: 100 }
                  - { name: price, type: integer }
              # Property security: the update merges the payload onto the stored record read through
              # findOne, and keeps a field the caller may not write (note) from that read.
              - name: Grade
                kind: setting
                multilingual: true
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string,  required: true, length: 100 }
                  - { name: note, type: string,  length: 100, visibleTo: [Payroll] }
                  - { name: rank, type: integer }

            permissions:
              - { role: Clerk, description: Maintains the grades, can: [Grade:write] }
              - { role: Payroll, description: May read and write a grade's note }

            seeds:
              - name: units
                entity: Unit
                rows:
                  - { id: 1, name: Kilogram, price: 10 }
                  - { id: 2, name: Litre,    price: 20 }
                  - { id: 3, name: Metre,    price: 30 }
              - name: units-bg
                entity: Unit
                language: bg
                rows:
                  - { id: 1, name: "Килограм" }
                  - { id: 2, name: "Литър" }
              - name: grades
                entity: Grade
                rows:
                  - { id: 1, name: Junior, note: Entry level, rank: 1 }
              - name: grades-bg
                entity: Grade
                language: bg
                rows:
                  - { id: 1, name: "Младши", note: "Начално ниво" }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Autowired
    private SecurityUtil securityUtil;

    @Test
    void a_translated_save_keeps_the_base_values() {
        deployProject();

        // The plain update: the bg reader sends back the record it read, translated, with the price
        // changed.
        assertEquals("Килограм", read(ADMIN, UNITS + "/1", "bg").getString("Name"));
        put(ADMIN, UNITS + "/1", "bg", "{\"Id\":1,\"Name\":\"Килограм\",\"Price\":15}");
        assertUnit(1, "en", "Kilogram", 15);
        assertUnit(1, null, "Kilogram", 15);
        assertUnit(1, "bg", "Килограм", 15);

        // In the default language the base row is what the user edits.
        put(ADMIN, UNITS + "/2", "en", "{\"Id\":2,\"Name\":\"Liter\",\"Price\":25}");
        assertUnit(2, "en", "Liter", 25);
        assertUnit(2, "bg", "Литър", 25);
        put(ADMIN, UNITS + "/2", null, "{\"Id\":2,\"Name\":\"Litre\",\"Price\":25}");
        assertUnit(2, "en", "Litre", 25);

        // A record the reader's language has no translation for was read untranslated - its save
        // edits the base row in that language too.
        put(ADMIN, UNITS + "/3", "bg", "{\"Id\":3,\"Name\":\"Meter\",\"Price\":35}");
        assertUnit(3, "en", "Meter", 35);
    }

    @Test
    void a_translated_save_through_property_security_keeps_the_base_values() throws SQLException {
        deployProject();

        securityUtil.createUserInDefaultTenant(CLERK, CLERK_PASSWORD, "Clerk");

        // Property security: the clerk holds no Payroll role, so the note is withheld from the read and
        // kept from the stored record on the write - from the base row, not from its translation.
        JsonPath grade = read(CLERK, GRADES + "/1", "bg");
        assertEquals("Младши", grade.getString("Name"));
        assertNull(grade.getString("Note"), "the clerk must be outside the note's roles");
        put(CLERK, GRADES + "/1", "bg", "{\"Id\":1,\"Name\":\"Младши\",\"Rank\":2}");
        assertEquals("Junior", read(CLERK, GRADES + "/1", "en").getString("Name"));
        assertEquals("Junior | Entry level | 2", storedGrade(1));

        // A caller who may write every field sends the whole translated record back.
        put(ADMIN, GRADES + "/1", "bg", "{\"Id\":1,\"Name\":\"Младши\",\"Note\":\"Начално ниво\",\"Rank\":3}");
        assertEquals("Junior | Entry level | 3", storedGrade(1));

        // ...and in the default language the merge still writes the base row.
        put(CLERK, GRADES + "/1", "en", "{\"Id\":1,\"Name\":\"Junior grade\",\"Rank\":4}");
        assertEquals("Junior grade | Entry level | 4", storedGrade(1));
    }

    private void deployProject() {
        writeResource(projectPath() + "/app.intent", INTENT);
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();
    }

    private void assertUnit(int id, String language, String name, int price) {
        JsonPath unit = read(ADMIN, UNITS + "/" + id, language);
        assertEquals(name, unit.getString("Name"));
        assertEquals(price, unit.getInt("Price"));
    }

    private JsonPath read(String user, String path, String language) {
        AtomicReference<JsonPath> body = new AtomicReference<>();
        restAssuredExecutor.execute(() -> body.set(request(language).when()
                                                                    .get(path)
                                                                    .then()
                                                                    .statusCode(200)
                                                                    .extract()
                                                                    .jsonPath()),
                user, passwordOf(user), TIMEOUT_SECONDS);
        return body.get();
    }

    private void put(String user, String path, String language, String body) {
        restAssuredExecutor.execute(() -> request(language).body(body)
                                                           .when()
                                                           .put(path)
                                                           .then()
                                                           .statusCode(200),
                user, passwordOf(user), TIMEOUT_SECONDS);
    }

    private static String passwordOf(String user) {
        return ADMIN.equals(user) ? ADMIN_PASSWORD : CLERK_PASSWORD;
    }

    /** The grade's base row as stored - the note included, which no response serves this reader. */
    private String storedGrade(int id) throws SQLException {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "SELECT \"GRADE_NAME\", \"GRADE_NOTE\", \"GRADE_RANK\" FROM \"LINGUAL_GRADE\" WHERE \"GRADE_ID\" = ?")) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getString(1) + " | " + result.getString(2) + " | " + result.getInt(3);
            }
        }
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
