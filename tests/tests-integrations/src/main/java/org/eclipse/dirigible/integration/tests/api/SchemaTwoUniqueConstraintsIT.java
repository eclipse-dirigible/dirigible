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
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
        repository.removeResource(SCHEMA_PATH);
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS \"" + TABLE_NAME + "\"");
        }
    }
}
