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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.structures.synchronizer.table.TableAlterProcessor;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.logging.LogsAsserter;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ch.qos.logback.classic.Level;

/**
 * A multilingual entity's language table holds one translation per record and language, its code in
 * lower case (#8794) - the rule the entity repositories, the search and the report overlay all read
 * by. A row breaking it is refused whoever writes it, and a table generated before the rule gets it
 * on a re-publish once its rows comply.
 */
class LanguageTableConstraintsIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "language-table-constraints";
    private static final String LANGUAGE_TABLE = "LANGRULE_UNIT_LANG";
    private static final String CHECK = "LANGRULE_UNIT_LANG_LOWER";
    private static final String KEY = "LANGRULE_UNIT_LANG_UNIQUE";

    private static final String INTENT = """
            name: langrule
            description: a language table holds one lower-case translation per record
            languages: [en, bg]

            entities:
              - name: Unit
                kind: setting
                multilingual: true
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string,  required: true, length: 100 }
            %s
            seeds:
              - name: units
                entity: Unit
                rows:
                  - { id: 1, name: Piece }
                  - { id: 2, name: Box }
              - name: units-bg
                entity: Unit
                language: bg
                rows:
                  - { id: 1, name: "Брой" }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    private LogsAsserter alterLogs;

    @BeforeEach
    void attachLogs() {
        alterLogs = new LogsAsserter(TableAlterProcessor.class, Level.WARN);
    }

    @Test
    void a_language_table_refuses_an_upper_case_code_and_a_second_translation_for_the_same_language() {
        publish("");

        assertThrows(SQLException.class,
                () -> execute("INSERT INTO \"" + LANGUAGE_TABLE + "\" (\"Id\", \"Name\", \"Language\")" + " VALUES (2, 'Кутия', 'BG')"),
                "an upper-case code is refused");
        assertThrows(SQLException.class,
                () -> execute("INSERT INTO \"" + LANGUAGE_TABLE + "\" (\"Id\", \"Name\", \"Language\")" + " VALUES (1, 'Бройка', 'bg')"),
                "a second bg translation of the same record is refused");
    }

    @Test
    void a_table_generated_before_the_rule_gets_it_once_its_rows_comply() throws SQLException {
        publish("");
        // A deployment generated before #8794: the table holds neither constraint, and a hand-loaded
        // row breaks the check.
        execute("ALTER TABLE \"" + LANGUAGE_TABLE + "\" DROP CONSTRAINT \"" + CHECK + "\"");
        execute("DROP INDEX \"" + KEY + "\"");
        execute("INSERT INTO \"" + LANGUAGE_TABLE + "\" (\"Id\", \"Name\", \"Language\") VALUES (2, 'Кутия', 'BG')");

        // A re-publish adds the key the rows allow and leaves the check out, saying why - the publish
        // itself is not refused.
        publish("  - name: Pack\n    fields:\n      - { name: id, type: integer, primaryKey: true, generated: true }");
        alterLogs.assertLoggedMessage("Check constraint [" + CHECK + "] on table [" + LANGUAGE_TABLE + "] is not added - 1 row(s) break it",
                Level.WARN);
        assertThrows(SQLException.class,
                () -> execute("INSERT INTO \"" + LANGUAGE_TABLE + "\" (\"Id\", \"Name\", \"Language\")" + " VALUES (1, 'Бройка', 'bg')"),
                "the key is added");

        // Once the row complies, the next publish adds the check.
        execute("UPDATE \"" + LANGUAGE_TABLE + "\" SET \"Language\" = 'bg' WHERE \"Language\" = 'BG'");
        publish("  - name: Crate\n    fields:\n      - { name: id, type: integer, primaryKey: true, generated: true }");
        assertThrows(SQLException.class,
                () -> execute("INSERT INTO \"" + LANGUAGE_TABLE + "\" (\"Id\", \"Name\", \"Language\")" + " VALUES (1, 'Brick', 'EN')"),
                "the check is added");
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    /** Generates and publishes the intent, with {@code moreEntities} appended to its entity list. */
    private void publish(String moreEntities) {
        writeResource(projectPath() + "/app.intent", INTENT.formatted(moreEntities));
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
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT + "/")
                                                 .then()
                                                 .statusCode(200));
        synchronizationProcessor.forceProcessSynchronizers();
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
