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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.data.structures.domain.Table;
import org.eclipse.dirigible.components.data.structures.service.TableService;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.database.sql.DataTypeUtils;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.util.TestConditionsChecker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A table can be created by another writer than the schema synchronizer - Hibernate's
 * {@code hbm2ddl.auto = update} builds the tables of the client-Java {@code @Entity} classes, and
 * it does so with its own type choices: a plain {@code String} property becomes a {@code VARCHAR}
 * where the model declares a {@code CLOB}, and a {@code java.time.Instant} one becomes a
 * {@code TIMESTAMP WITH TIME ZONE} where the model declares a {@code TIMESTAMP}.
 *
 * <p>
 * Publishing the model over such a table has to succeed: the two representations hold the same kind
 * of value. It used to fail the artefact instead - once with a false "Incompatible change of table
 * ... of type VARCHAR to be changed to CLOB" and once with "Type [2014] not supported", on every
 * single re-publish (issue #6346).
 *
 * <p>
 * Pure JDBC / repository - no Selenide, no IDE.
 */
class SchemaRepublishTypeToleranceIT extends IntegrationTest {

    private static final String PROJECT = "schema-republish-tolerance-it";

    private static final String TABLE_LOCATION = "/" + PROJECT + "/tables/note.table";

    private static final String TABLE_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + TABLE_LOCATION;

    private static final String TABLE_NAME = "REPUBLISH_NOTE";

    /** The length the fixture's own CREATE TABLE gives NOTE_TEXT. */
    private static final int FIXTURE_TEXT_LENGTH = 255;

    /** What the model declares: a text field is a CLOB, an audit timestamp is a TIMESTAMP. */
    private static final String TABLE_SOURCE = """
            {
                "name": "REPUBLISH_NOTE",
                "type": "TABLE",
                "columns": [
                    {
                        "type": "INTEGER",
                        "primaryKey": true,
                        "identity": true,
                        "nullable": false,
                        "name": "NOTE_ID"
                    },
                    {
                        "type": "CLOB",
                        "nullable": true,
                        "name": "NOTE_TEXT"
                    },
                    {
                        "type": "TIMESTAMP",
                        "nullable": true,
                        "name": "NOTE_CREATED_AT"
                    }
                ]
            }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Autowired
    private TableService tableService;

    @Autowired
    private TestConditionsChecker testConditionsChecker;

    @Test
    void publishing_over_an_equivalently_typed_table_succeeds() throws Exception {
        // TIMESTAMP WITH TIME ZONE is not an MSSQL type; MSSQL is not a CI leg either.
        assumeTrue(testConditionsChecker.isH2OrPostgresDefaultDB(), "Skipping: the fixture DDL is H2 / PostgreSQL syntax.");

        // The other writer got there first, with its own representation of the same two columns.
        executeStatement("CREATE TABLE \"" + TABLE_NAME + "\" (\"NOTE_ID\" INTEGER, \"NOTE_TEXT\" VARCHAR(255),"
                + " \"NOTE_CREATED_AT\" TIMESTAMP WITH TIME ZONE)");

        repository.createResource(TABLE_PATH, TABLE_SOURCE.getBytes(StandardCharsets.UTF_8), false, "text/plain", true);
        synchronizationProcessor.forceProcessSynchronizers();

        List<Table> tables = tableService.findByLocation(TABLE_LOCATION);
        assertThat(tables).hasSize(1);
        Table table = tables.get(0);
        assertThat(table.getError()).isNullOrEmpty();
        assertThat(table.getLifecycle()).isIn(ArtefactLifecycle.CREATED, ArtefactLifecycle.UPDATED);

        // The column is left as it is - still the bounded character one the other writer created,
        // neither widened to the declared CLOB nor rebuilt behind the user's back. Each database
        // spells the type its own way ("CHARACTER VARYING" on H2, "varchar" on PostgreSQL), so the
        // assertion goes through the type family and the declared length.
        Column text = column("NOTE_TEXT");
        assertThat(DataTypeUtils.isCharacterType(text.typeName())).as("NOTE_TEXT is %s", text)
                                                                  .isTrue();
        assertThat(text.size()).as("NOTE_TEXT is %s", text)
                               .isEqualTo(FIXTURE_TEXT_LENGTH);
    }

    private Column column(String name) throws Exception {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                ResultSet columns = connection.getMetaData()
                                              .getColumns(null, connection.getSchema(), TABLE_NAME, name)) {
            assertThat(columns.next()).as("missing column [%s]", name)
                                      .isTrue();
            return new Column(columns.getString("TYPE_NAME"), columns.getInt("COLUMN_SIZE"));
        }
    }

    /** A column as the database reports it back, rendered into every assertion message. */
    private record Column(String typeName, int size) {

        @Override
        public String toString() {
            return typeName + "(" + size + ")";
        }
    }

    private void executeStatement(String sql) throws Exception {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    @AfterEach
    void cleanup() throws Exception {
        if (repository.hasResource(TABLE_PATH)) {
            repository.removeResource(TABLE_PATH);
        }
        synchronizationProcessor.forceProcessSynchronizers();
        // The synchronizer keeps the data of a removed .table, so drop the table for the next run.
        executeStatement("DROP TABLE IF EXISTS \"" + TABLE_NAME + "\"");
    }
}
