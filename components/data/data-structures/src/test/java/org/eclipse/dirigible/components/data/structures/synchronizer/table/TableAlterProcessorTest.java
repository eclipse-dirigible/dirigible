/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.structures.synchronizer.table;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import org.eclipse.dirigible.components.data.structures.domain.Table;
import org.eclipse.dirigible.components.data.structures.domain.TableColumn;
import org.junit.jupiter.api.Test;

/**
 * The ALTER phase of a re-publish over an existing database, driven against a real H2 instance.
 */
class TableAlterProcessorTest {

    /**
     * A table whose columns the database reports in another - but equivalent - representation than the
     * definition declares must not fail the table: a {@code CLOB} column comes back as
     * {@code CHARACTER VARYING} once it was created from a plain {@code String} mapping, and an
     * {@code Instant} column comes back as {@code TIMESTAMP WITH TIME ZONE} (JDBC type 2014, which used
     * to have no mapping at all). Both used to abort the whole schema artefact on every re-publish.
     */
    @Test
    void equivalentColumnTypesAreNotAnIncompatibleChange() throws SQLException {
        try (Connection connection = connect("alter_equivalent")) {
            createTable(connection, "\"NOTE\" VARCHAR(255), \"CREATED_AT\" TIMESTAMP WITH TIME ZONE, \"STARTS_AT\" TIME WITH TIME ZONE");

            Table tableModel = new Table("T_NOTES");
            new TableColumn("ID", "INTEGER", null, tableModel);
            new TableColumn("NOTE", "CLOB", null, tableModel);
            new TableColumn("CREATED_AT", "TIMESTAMP", null, tableModel);
            new TableColumn("STARTS_AT", "TIME", null, tableModel);

            assertDoesNotThrow(() -> TableAlterProcessor.execute(connection, tableModel));

            // the existing columns are kept as they are, none of them re-created or dropped
            assertEquals(Types.VARCHAR, columnType(connection, "NOTE"));
            assertEquals(Types.TIMESTAMP_WITH_TIMEZONE, columnType(connection, "CREATED_AT"));
            assertEquals(Types.TIME_WITH_TIMEZONE, columnType(connection, "STARTS_AT"));
        }
    }

    /**
     * A genuine type change still fails the table - the tolerance above must not swallow it.
     */
    @Test
    void incompatibleColumnTypeStillFails() throws SQLException {
        try (Connection connection = connect("alter_incompatible")) {
            createTable(connection, "\"NOTE\" VARCHAR(255)");

            Table tableModel = new Table("T_NOTES");
            new TableColumn("ID", "INTEGER", null, tableModel);
            new TableColumn("NOTE", "INTEGER", null, tableModel);

            SQLException exception = assertThrows(SQLException.class, () -> TableAlterProcessor.execute(connection, tableModel));
            assertTrue(exception.getMessage()
                                .contains("Incompatible change of table"),
                    exception.getMessage());
        }
    }

    private static Connection connect(String database) throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:" + database, "sa", "");
    }

    private static void createTable(Connection connection, String columns) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE \"T_NOTES\" (\"ID\" INTEGER, " + columns + ")");
        }
    }

    private static int columnType(Connection connection, String column) throws SQLException {
        try (ResultSet columns = connection.getMetaData()
                                           .getColumns(null, connection.getSchema(), "T_NOTES", column)) {
            assertTrue(columns.next(), "Missing column [" + column + "]");
            return columns.getInt(5);
        }
    }
}
