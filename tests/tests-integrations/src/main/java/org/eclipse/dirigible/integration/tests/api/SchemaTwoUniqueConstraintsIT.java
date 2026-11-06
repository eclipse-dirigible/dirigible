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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.structures.synchronizer.table.TableAlterProcessor;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * A table may declare more than one unique constraint, and every one of them reaches the database.
 *
 * <p>
 * {@code TableConstraint} joined its owning {@code TableConstraints} with {@code @OneToOne}, so
 * Hibernate put a unique index on the join column and the second constraint of a table could not be
 * persisted at all: the insert failed with a duplicate key, the whole {@code .schema} was marked
 * failed, and on a fresh instance none of its tables were ever created (#7343). Nothing in front of
 * that said so - the model, the generated {@code .schema} and the publish were all green, and the
 * key the author declared simply constrained nothing.
 */
class SchemaTwoUniqueConstraintsIT extends IntegrationTest {

    private static final String PROJECT = "schema-two-unique-constraints-it";

    private static final String SCHEMA_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/two-unique-constraints.schema";

    private static final String TABLE_NAME = "TWO_KEYS_TIMESHEET";

    private static final String FIRST_KEY = "TwoKeysTimesheet_Project_Employee";

    private static final String SECOND_KEY = "TwoKeysTimesheet_Company_Number";

    private static final String OTHER_SCHEMA_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/other.schema";

    private static final String OTHER_TABLE_NAME = "TWO_KEYS_OTHER";

    private static final String SCHEMA_SOURCE = """
            {
                "schema": {
                    "structures": [
                        {
                            "name": "TWO_KEYS_TIMESHEET",
                            "type": "TABLE",
                            "columns": [
                                { "name": "ID", "type": "INTEGER", "primaryKey": true, "identity": true, "nullable": false },
                                { "name": "PROJECT", "type": "VARCHAR", "length": "50", "nullable": true },
                                { "name": "EMPLOYEE", "type": "VARCHAR", "length": "50", "nullable": true },
                                { "name": "COMPANY", "type": "VARCHAR", "length": "50", "nullable": true },
                                { "name": "NUMBER", "type": "VARCHAR", "length": "50", "nullable": true }
                            ],
                            "constraints": {
                                "uniqueIndexes": [
                                    {
                                        "name": "TwoKeysTimesheet_Project_Employee",
                                        "columns": ["PROJECT", "EMPLOYEE"]
                                    },
                                    {
                                        "name": "TwoKeysTimesheet_Company_Number",
                                        "columns": ["COMPANY", "NUMBER"]
                                    }
                                ]
                            }
                        }
                    ]
                }
            }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Test
    void bothDeclaredKeysReachTheCreatedTableAndRefuseADuplicate() throws Exception {
        repository.createResource(SCHEMA_PATH, SCHEMA_SOURCE.getBytes(StandardCharsets.UTF_8), false, "application/json", true);
        synchronizationProcessor.forceProcessSynchronizers();

        assertThat(uniqueIndexNames()).as("both declared keys must be created on [%s]", TABLE_NAME)
                                      .contains(FIRST_KEY, SECOND_KEY);

        insertRow("P1", "E1", "C1", "N1");

        assertThat(refusalOf("P2", "E2", "C1", "N1")).as("the second key must refuse a duplicate (company, number)")
                                                     .contains(SECOND_KEY);
        assertThat(refusalOf("P1", "E1", "C2", "N2")).as("the first key must refuse a duplicate (project, employee)")
                                                     .contains(FIRST_KEY);
    }

    /**
     * #8317: a key the alter could not add over duplicate rows is attempted again on a later pass, once
     * the duplicates are resolved - although the schema itself did not change again.
     */
    @Test
    void aKeyRefusedOverDuplicatesIsAddedOnALaterPassOnceTheyAreResolved() throws Exception {
        publish(SCHEMA_PATH, SCHEMA_SOURCE.replaceAll("(?s)\"uniqueIndexes\": \\[.*?\n\\s*\\]", "\"uniqueIndexes\": []"));
        insertRow("P1", "E1", "C1", "N1");
        insertRow("P2", "E2", "C1", "N1");

        publish(SCHEMA_PATH, SCHEMA_SOURCE);
        // an added constraint's index is named after it, with a vendor suffix on H2
        assertThat(uniqueIndexNames()).as("the duplicates refuse the second key, the first one is added")
                                      .anyMatch(name -> name.startsWith(FIRST_KEY))
                                      .noneMatch(name -> name.startsWith(SECOND_KEY));

        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("DELETE FROM \"" + TABLE_NAME + "\" WHERE \"PROJECT\" = 'P2'");
        }
        // any later pass carrying a change - here another project file - attempts the key again
        publish(OTHER_SCHEMA_PATH, """
                {
                    "schema": {
                        "structures": [
                            {
                                "name": "TWO_KEYS_OTHER",
                                "type": "TABLE",
                                "columns": [ { "name": "ID", "type": "INTEGER", "primaryKey": true, "nullable": false } ]
                            }
                        ]
                    }
                }
                """);

        assertThat(uniqueIndexNames()).as("the second key is added once the duplicates are gone")
                                      .anyMatch(name -> name.startsWith(SECOND_KEY));
        assertThat(refusalOf("P3", "E3", "C1", "N1")).as("the second key refuses a duplicate (company, number)")
                                                     .contains(SECOND_KEY);
    }

    /**
     * #8498: the keys a table was born with are UNIQUE INDEXes, which the constraint catalog does not
     * list. A later publish that alters the table - here an added column - must take them as present:
     * on PostgreSQL an ADD CONSTRAINT of the same name is refused ("relation already exists"), one
     * ERROR per key on every upgrade, and #8317's retry would repeat it on every pass.
     */
    @Test
    void anAlterKeepsTheKeysTheTableWasBornWithAsUniqueIndexes() throws Exception {
        publish(SCHEMA_PATH, SCHEMA_SOURCE);
        insertRow("P1", "E1", "C1", "N1");
        String withAddedColumn = SCHEMA_SOURCE.replace("""
                { "name": "NUMBER", "type": "VARCHAR", "length": "50", "nullable": true }
                """.strip(), """
                { "name": "NUMBER", "type": "VARCHAR", "length": "50", "nullable": true },
                { "name": "NOTE", "type": "VARCHAR", "length": "50", "nullable": true }
                """.strip());
        assertThat(withAddedColumn).as("the republished schema adds a column")
                                   .contains("\"NOTE\"");

        ListAppender<ILoggingEvent> alterLog = new ListAppender<>();
        alterLog.start();
        Logger alterLogger = (Logger) LoggerFactory.getLogger(TableAlterProcessor.class);
        alterLogger.addAppender(alterLog);
        try {
            publish(SCHEMA_PATH, withAddedColumn);
        } finally {
            alterLogger.detachAppender(alterLog);
        }

        assertThat(alterLog.list).as("the alter added the column")
                                 .anyMatch(event -> event.getFormattedMessage()
                                                         .contains("\"NOTE\""));
        assertThat(alterLog.list).as("no declared key is added again over the index it was born as")
                                 .noneMatch(event -> event.getLevel()
                                                          .isGreaterOrEqual(Level.WARN)
                                         && event.getFormattedMessage()
                                                 .startsWith("Unique constraint"))
                                 .noneMatch(event -> event.getFormattedMessage()
                                                          .startsWith("Adding unique constraint"));
        // an added constraint's index would carry a vendor suffix on H2, and on PostgreSQL the ADD is
        // refused
        assertThat(uniqueIndexNames()).as("the declared keys are still the indexes the table was born with, and only those")
                                      .filteredOn(name -> name.startsWith("TwoKeysTimesheet_"))
                                      .containsExactlyInAnyOrder(FIRST_KEY, SECOND_KEY);
        assertThat(refusalOf("P2", "E2", "C1", "N1")).as("the second key still refuses a duplicate (company, number)")
                                                     .contains(SECOND_KEY);
    }

    private void publish(String path, String content) {
        repository.createResource(path, content.getBytes(StandardCharsets.UTF_8), false, "application/json", true);
        synchronizationProcessor.forceProcessSynchronizers();
    }

    /**
     * A declared unique key materializes as a unique index (that is the DDL
     * {@code TableCreateProcessor} emits), so the database's own index catalog is what proves it
     * reached the table - on every vendor, through one JDBC call.
     */
    private List<String> uniqueIndexNames() throws Exception {
        List<String> names = new ArrayList<>();
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                ResultSet indexes = connection.getMetaData()
                                              .getIndexInfo(null, connection.getSchema(), TABLE_NAME, true, false)) {
            while (indexes.next()) {
                String name = indexes.getString("INDEX_NAME");
                if (null != name && !names.contains(name)) {
                    names.add(name);
                }
            }
        }
        return names;
    }

    private void insertRow(String project, String employee, String company, String number) throws Exception {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO \"" + TABLE_NAME + "\" (\"PROJECT\", \"EMPLOYEE\", \"COMPANY\", \"NUMBER\") VALUES (?, ?, ?, ?)")) {
            statement.setString(1, project);
            statement.setString(2, employee);
            statement.setString(3, company);
            statement.setString(4, number);
            statement.executeUpdate();
        }
    }

    /**
     * The message the database refuses the insert with - it names the violated key on every vendor, so
     * the assertion is about the key that fired and not merely about something having gone wrong.
     */
    private String refusalOf(String project, String employee, String company, String number) {
        try {
            insertRow(project, employee, company, number);
            throw new AssertionError("the insert was accepted - the key does not constrain anything");
        } catch (SQLException refused) {
            return refused.getMessage();
        } catch (Exception unexpected) {
            throw new IllegalStateException("the insert failed for another reason than the key", unexpected);
        }
    }

    @AfterEach
    void cleanUp() throws Exception {
        for (String path : List.of(SCHEMA_PATH, OTHER_SCHEMA_PATH)) {
            if (repository.hasResource(path)) {
                repository.removeResource(path);
            }
        }
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS \"" + TABLE_NAME + "\"");
            statement.execute("DROP TABLE IF EXISTS \"" + OTHER_TABLE_NAME + "\"");
        }
    }
}
