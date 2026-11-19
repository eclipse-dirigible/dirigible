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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.dirigible.components.data.structures.domain.Table;
import org.eclipse.dirigible.components.data.structures.domain.TableColumn;
import org.eclipse.dirigible.components.data.structures.domain.TableConstraintUnique;
import org.eclipse.dirigible.components.data.structures.domain.TableConstraints;
import org.junit.jupiter.api.Test;

class TableCreateProcessorTest {

    /**
     * A string column's default is the text the model carries - `web`, not a column named web. The
     * generated repository fills the default on create itself, so the DB DEFAULT is only ever read by a
     * row written past the repository; the CREATE TABLE has to be accepted all the same, which a bare
     * word after DEFAULT is not on PostgreSQL (dirigible #7765). A row inserted without the column is
     * the proof the literal landed in the DDL.
     */
    @Test
    void aStringDefaultIsCreatedAsALiteral() throws SQLException {
        try (Connection connection = connect("create_string_default")) {
            Table tableModel = new Table("T_REORDER");
            tableModel.setSchema(null);
            new TableColumn("ID", "INTEGER", null, false, true, null, "0", "0", false, tableModel);
            new TableColumn("CHANNEL", "VARCHAR", "20", true, false, "web", "0", "0", false, tableModel);
            new TableColumn("COPY", "VARCHAR", "40", true, false, "Owner's copy", "0", "0", false, tableModel);
            new TableColumn("STAGE", "VARCHAR", "20", true, false, "'draft'", "0", "0", false, tableModel);
            new TableColumn("NOTE", "VARCHAR", "20", true, false, "", "0", "0", false, tableModel);
            new TableColumn("COPIES", "INTEGER", null, true, false, "2", "0", "0", false, tableModel);
            new TableColumn("RUSH", "BOOLEAN", null, true, false, "true", "0", "0", false, tableModel);
            new TableColumn("TOKEN", "VARCHAR", "40", true, false, "RANDOM_UUID()", "0", "0", false, tableModel);
            new TableColumn("RECORDED_AT", "TIMESTAMP", null, true, false, "CURRENT_TIMESTAMP", "0", "0", false, tableModel);
            new TableColumn("NOTHING", "VARCHAR", "20", true, false, "NULL", "0", "0", false, tableModel);

            TableCreateProcessor.execute(connection, tableModel);
            try (Statement statement = connection.createStatement()) {
                statement.execute("INSERT INTO \"T_REORDER\" (\"ID\") VALUES (1)");
            }

            assertEquals("web", value(connection, "CHANNEL"), "a bare text is the literal, not a column reference");
            assertEquals("Owner's copy", value(connection, "COPY"), "an apostrophe inside the text survives");
            assertEquals("draft", value(connection, "STAGE"), "a text the author already quoted is not quoted twice");
            assertEquals("", value(connection, "NOTE"), "the empty default is still the empty string");
            assertEquals("2", value(connection, "COPIES"));
            assertEquals("TRUE", value(connection, "RUSH"));
            assertTrue(value(connection, "TOKEN").length() == 36, "a function call on a string column is evaluated, not quoted");
            assertNotNull(value(connection, "RECORDED_AT"), "an expression on a non-character column is kept as written");
            assertNull(value(connection, "NOTHING"), "the NULL literal is kept as written");
        }
    }

    /**
     * A composite unique index is the key the model declares, also when one of its columns is unique on
     * its own: built without that column it refuses a second row sharing only the other one (dirigible
     * #8730).
     */
    @Test
    void compositeUniqueIndexKeepsAUniqueColumn() throws SQLException {
        try (Connection connection = connect("create_composite_unique_column")) {
            Table tableModel = new Table("T_ITEM");
            tableModel.setSchema(null);
            new TableColumn("ID", "INTEGER", null, false, true, tableModel);
            new TableColumn("CODE", "VARCHAR", "36", true, false, null, "0", "0", true, tableModel);
            new TableColumn("GROUP_ID", "INTEGER", null, tableModel);
            declareUnique(tableModel, "T_Code_Group", "CODE", "GROUP_ID");

            TableCreateProcessor.execute(connection, tableModel);

            assertEquals(Set.of("CODE", "GROUP_ID"), uniqueColumns(connection, "T_Code_Group"));
            try (Statement statement = connection.createStatement()) {
                statement.execute("INSERT INTO \"T_ITEM\" (\"ID\", \"CODE\", \"GROUP_ID\") VALUES (1, 'a', 1)");
                statement.execute("INSERT INTO \"T_ITEM\" (\"ID\", \"CODE\", \"GROUP_ID\") VALUES (2, 'b', 1)");
            }
        }
    }

    @Test
    void compositeUniqueIndexKeepsThePrimaryKeyColumn() throws SQLException {
        try (Connection connection = connect("create_composite_unique_primary_key")) {
            Table tableModel = new Table("T_ITEM");
            tableModel.setSchema(null);
            new TableColumn("ID", "INTEGER", null, false, true, tableModel);
            new TableColumn("GROUP_ID", "INTEGER", null, tableModel);
            declareUnique(tableModel, "T_Id_Group", "ID", "GROUP_ID");

            TableCreateProcessor.execute(connection, tableModel);

            assertEquals(Set.of("ID", "GROUP_ID"), uniqueColumns(connection, "T_Id_Group"));
            try (Statement statement = connection.createStatement()) {
                statement.execute("INSERT INTO \"T_ITEM\" (\"ID\", \"GROUP_ID\") VALUES (1, 1)");
                statement.execute("INSERT INTO \"T_ITEM\" (\"ID\", \"GROUP_ID\") VALUES (2, 1)");
            }
        }
    }

    @Test
    void singleColumnIndexOnAUniqueColumnIsStillSkipped() throws SQLException {
        try (Connection connection = connect("create_single_unique_skipped")) {
            Table tableModel = new Table("T_ITEM");
            tableModel.setSchema(null);
            new TableColumn("ID", "INTEGER", null, false, true, tableModel);
            new TableColumn("CODE", "VARCHAR", "36", true, false, null, "0", "0", true, tableModel);
            new TableColumn("GROUP_ID", "INTEGER", null, tableModel);
            declareUnique(tableModel, "T_Code", "CODE");
            declareUnique(tableModel, "T_Id", "ID");
            declareUnique(tableModel, "T_Group", "GROUP_ID");

            TableCreateProcessor.execute(connection, tableModel);

            assertEquals(Set.of(), uniqueColumns(connection, "T_Code"), "the column's own UNIQUE is the index");
            assertEquals(Set.of(), uniqueColumns(connection, "T_Id"), "the primary key is the index");
            assertEquals(Set.of("GROUP_ID"), uniqueColumns(connection, "T_Group"), "a plain column still gets its declared index");
        }
    }

    private static void declareUnique(Table tableModel, String name, String... columns) {
        TableConstraints constraints = tableModel.getConstraints();
        constraints.getUniqueIndexes()
                   .add(new TableConstraintUnique(name, null, columns, constraints, null, null));
    }

    /**
     * The columns of the unique constraint or unique index of that name; empty when there is none. A
     * set, because the SQL builder writes a key's columns sorted by name, whatever order they are
     * declared in.
     */
    private static Set<String> uniqueColumns(Connection connection, String name) throws SQLException {
        String constraintColumns = "SELECT kcu.COLUMN_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc"
                + " JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu ON kcu.CONSTRAINT_NAME = tc.CONSTRAINT_NAME"
                + " WHERE tc.CONSTRAINT_TYPE = 'UNIQUE' AND tc.CONSTRAINT_NAME = ? ORDER BY kcu.ORDINAL_POSITION";
        String indexColumns = "SELECT ic.COLUMN_NAME FROM INFORMATION_SCHEMA.INDEXES i"
                + " JOIN INFORMATION_SCHEMA.INDEX_COLUMNS ic ON ic.INDEX_NAME = i.INDEX_NAME"
                + " WHERE i.INDEX_TYPE_NAME = 'UNIQUE INDEX' AND i.INDEX_NAME = ? ORDER BY ic.ORDINAL_POSITION";
        Set<String> columns = new HashSet<>();
        for (String query : List.of(constraintColumns, indexColumns)) {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, name);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        columns.add(rs.getString(1));
                    }
                }
            }
            if (!columns.isEmpty()) {
                break;
            }
        }
        return columns;
    }

    private static Connection connect(String database) throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:" + database, "sa", "");
    }

    private static String value(Connection connection, String column) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("SELECT \"" + column + "\" FROM \"T_REORDER\" WHERE \"ID\" = 1")) {
            assertTrue(rs.next(), "Missing row 1");
            return rs.getString(1);
        }
    }
}
