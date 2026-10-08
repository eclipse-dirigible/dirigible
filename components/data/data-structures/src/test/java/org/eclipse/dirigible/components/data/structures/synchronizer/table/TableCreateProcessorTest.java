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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.eclipse.dirigible.components.data.structures.domain.Table;
import org.eclipse.dirigible.components.data.structures.domain.TableColumn;
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
