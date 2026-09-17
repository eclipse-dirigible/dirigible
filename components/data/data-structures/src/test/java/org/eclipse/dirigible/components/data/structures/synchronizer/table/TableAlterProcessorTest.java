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
import org.eclipse.dirigible.components.data.structures.domain.TableConstraints;
import org.eclipse.dirigible.components.data.structures.domain.TableConstraintUnique;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.ArrayList;
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

    /**
     * #7019: a model that moves a key from one column to a composite gets exactly that on an existing
     * table.
     */
    @Test
    void aWithdrawnColumnUniqueIsDroppedAndTheDeclaredCompositeKeyIsAdded() throws SQLException {
        try (Connection connection = connect("alter_unique_move")) {
            createTable(connection, "\"COMPANY\" INTEGER, \"NUMBER\" VARCHAR(100) UNIQUE");
            Table tableModel = modelWithCompositeKey();

            TableAlterProcessor.execute(connection, tableModel);

            Map<String, List<String>> uniques = uniqueConstraints(connection);
            assertEquals(1, uniques.size(), "the single-column UNIQUE is gone and the composite key is there: " + uniques);
            assertEquals(List.of("COMPANY", "NUMBER"), uniques.get("Notes_Company_Number"),
                    "the declared name and column order: " + uniques);
        }
    }

    @Test
    void aDeclaredColumnUniqueIsAddedToAnExistingTable() throws SQLException {
        try (Connection connection = connect("alter_unique_add")) {
            createTable(connection, "\"CODE\" VARCHAR(20)");
            Table tableModel = new Table("T_NOTES");
            new TableColumn("ID", "INTEGER", null, tableModel);
            new TableColumn("CODE", "VARCHAR", "20", true, false, null, null, null, true, false, tableModel);

            TableAlterProcessor.execute(connection, tableModel);

            Map<String, List<String>> uniques = uniqueConstraints(connection);
            assertEquals(List.of(List.of("CODE")), new ArrayList<>(uniques.values()), "the model's unique column is enforced: " + uniques);
        }
    }

    @Test
    void aMatchingKeyIsKeptWhateverItsNameAndASecondRunChangesNothing() throws SQLException {
        try (Connection connection = connect("alter_unique_idempotent")) {
            createTable(connection,
                    "\"COMPANY\" INTEGER, \"NUMBER\" VARCHAR(100), CONSTRAINT \"HAND_MADE\" UNIQUE (\"COMPANY\", \"NUMBER\")");
            Table tableModel = modelWithCompositeKey();

            TableAlterProcessor.execute(connection, tableModel);
            TableAlterProcessor.execute(connection, tableModel);

            Map<String, List<String>> uniques = uniqueConstraints(connection);
            assertEquals(Map.of("HAND_MADE", List.of("COMPANY", "NUMBER")), uniques,
                    "an equal key under another name is not churned, and the primary key is untouched");
        }
    }

    @Test
    void aRefusedConstraintChangeDoesNotFailTheAlter() throws SQLException {
        try (Connection connection = connect("alter_unique_refused")) {
            createTable(connection, "\"COMPANY\" INTEGER, \"NUMBER\" VARCHAR(100)");
            try (Statement statement = connection.createStatement()) {
                statement.execute("INSERT INTO \"T_NOTES\" VALUES (1, 1, 'SI1'), (2, 1, 'SI1')");
            }
            Table tableModel = modelWithCompositeKey();

            assertDoesNotThrow(() -> TableAlterProcessor.execute(connection, tableModel),
                    "duplicate rows make the ADD fail - logged, not thrown");
            assertTrue(uniqueConstraints(connection).isEmpty());
        }
    }

    private static Table modelWithCompositeKey() {
        Table tableModel = new Table("T_NOTES");
        new TableColumn("ID", "INTEGER", null, tableModel);
        new TableColumn("COMPANY", "INTEGER", null, tableModel);
        new TableColumn("NUMBER", "VARCHAR", "100", tableModel);
        TableConstraints constraints = new TableConstraints(tableModel);
        tableModel.setConstraints(constraints);
        constraints.getUniqueIndexes()
                   .add(new TableConstraintUnique("Notes_Company_Number", null, new String[] {"COMPANY", "NUMBER"}, constraints, null,
                           null));
        return tableModel;
    }

    private static Map<String, List<String>> uniqueConstraints(Connection connection) throws SQLException {
        Map<String, List<String>> uniques = new LinkedHashMap<>();
        try (Statement statement = connection.createStatement();
                ResultSet rs =
                        statement.executeQuery("SELECT tc.CONSTRAINT_NAME, kcu.COLUMN_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc"
                                + " JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu ON kcu.CONSTRAINT_NAME = tc.CONSTRAINT_NAME"
                                + " WHERE tc.CONSTRAINT_TYPE = 'UNIQUE' AND tc.TABLE_NAME = 'T_NOTES' ORDER BY tc.CONSTRAINT_NAME, kcu.ORDINAL_POSITION")) {
            while (rs.next()) {
                uniques.computeIfAbsent(rs.getString(1), name -> new ArrayList<>())
                       .add(rs.getString(2));
            }
        }
        return uniques;
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
