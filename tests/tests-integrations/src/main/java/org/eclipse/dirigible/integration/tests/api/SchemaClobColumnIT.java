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
import java.sql.ResultSet;
import java.sql.Statement;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.database.sql.DataTypeUtils;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A {@code .table} declaring a {@code CLOB} column - what a {@code text} field generates - creates
 * on every supported database. Each renders large text its own way: H2 keeps the {@code CLOB},
 * PostgreSQL has no such type and takes its unbounded {@code TEXT}. Emitting {@code CLOB} verbatim
 * there failed the whole table with {@code type "clob" does not exist}.
 */
class SchemaClobColumnIT extends IntegrationTest {

    private static final String PROJECT = "schema-clob-column-it";

    private static final String TABLE_PATH = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/tables/note.table";

    private static final String TABLE_NAME = "CLOB_COLUMN_NOTE";

    /** A length no database picks by itself - only an explicitly bounded column can report it. */
    private static final int BOUNDED_LENGTH = 255;

    private static final String TABLE_SOURCE = """
            {
                "name": "CLOB_COLUMN_NOTE",
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

    @Test
    void a_clob_column_is_created_as_the_database_large_text_type() throws Exception {
        repository.createResource(TABLE_PATH, TABLE_SOURCE.getBytes(StandardCharsets.UTF_8), false, "text/plain", true);
        synchronizationProcessor.forceProcessSynchronizers();

        Column text = column("NOTE_TEXT");
        assertThat(DataTypeUtils.isCharacterType(text.typeName)).as("NOTE_TEXT is %s", text)
                                                                .isTrue();
        assertThat(text.size).as("NOTE_TEXT is %s", text)
                             .isGreaterThan(BOUNDED_LENGTH);
    }

    private Column column(String name) throws Exception {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                ResultSet columns = connection.getMetaData()
                                              .getColumns(null, connection.getSchema(), TABLE_NAME, name)) {
            assertThat(columns.next()).as("missing column [%s] - the table was not created at all", name)
                                      .isTrue();
            return new Column(columns.getString(6), columns.getInt(7));
        }
    }

    /** A column as the database reports it back, rendered into every assertion message. */
    private record Column(String typeName, int size) {

        @Override
        public String toString() {
            return typeName + "(" + size + ")";
        }
    }

    @AfterEach
    void dropTable() throws Exception {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS \"" + TABLE_NAME + "\"");
        }
    }
}
