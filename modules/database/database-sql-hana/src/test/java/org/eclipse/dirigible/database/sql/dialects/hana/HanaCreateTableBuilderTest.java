/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.hana;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.eclipse.dirigible.database.sql.DataType;
import org.eclipse.dirigible.database.sql.ISqlKeywords;
import org.eclipse.dirigible.database.sql.Modifiers;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.eclipse.dirigible.database.sql.TableStatements;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * The Class HanaCreateTableBuilderTest.
 */
public class HanaCreateTableBuilderTest {

    /**
     * Creates the table generic.
     */
    @Test
    public void createTableGeneric() {
        String sql = SqlFactory.getNative(new HanaSqlDialect())
                               .create()
                               .table("CUSTOMERS")
                               .column("ID", DataType.INTEGER, true, false, false)
                               .column("FIRST_NAME", DataType.VARCHAR, false, false, true, "(20)")
                               .column("LAST_NAME", DataType.VARCHAR, false, true, false, "(30)")
                               .build();

        assertNotNull(sql);
        assertEquals(
                "CREATE COLUMN TABLE \"CUSTOMERS\" ( \"ID\" INTEGER NOT NULL PRIMARY KEY , \"FIRST_NAME\" VARCHAR (20) NOT NULL UNIQUE , \"LAST_NAME\" VARCHAR (30) )",
                sql);
    }

    /**
     * Creates the table generic with case sensitive flag.
     */
    @Test
    public void createTableCaseSensitiveGeneric() {
        String sql = SqlFactory.getDefault()
                               .create()
                               .table("myapp::test.customers")
                               .column("Id", DataType.INTEGER, Modifiers.PRIMARY_KEY, Modifiers.NOT_NULL, Modifiers.NON_UNIQUE)
                               .column("First_Name", DataType.VARCHAR, Modifiers.REGULAR, Modifiers.NOT_NULL, Modifiers.UNIQUE, "(20)")
                               .column("Last_Name", DataType.VARCHAR, Modifiers.REGULAR, Modifiers.NULLABLE, Modifiers.NON_UNIQUE, "(30)")
                               .build();

        assertNotNull(sql);
        assertEquals(
                "CREATE TABLE \"myapp::test.customers\" ( \"Id\" INTEGER NOT NULL PRIMARY KEY , \"First_Name\" VARCHAR (20) NOT NULL UNIQUE , \"Last_Name\" VARCHAR (30) )",
                sql);
    }

    /**
     * Creates the table column.
     */
    @Test
    public void createTableColumn() {
        String sql = SqlFactory.getNative(new HanaSqlDialect())
                               .create()
                               .columnTable("CUSTOMERS")
                               .column("ID", DataType.INTEGER, true, false, false)
                               .column("FIRST_NAME", DataType.VARCHAR, false, false, true, "(20)")
                               .column("LAST_NAME", DataType.VARCHAR, false, true, false, "(30)")
                               .build();

        assertNotNull(sql);
        assertEquals(
                "CREATE COLUMN TABLE \"CUSTOMERS\" ( \"ID\" INTEGER NOT NULL PRIMARY KEY , \"FIRST_NAME\" VARCHAR (20) NOT NULL UNIQUE , \"LAST_NAME\" VARCHAR (30) )",
                sql);
    }

    /**
     * Creates the row table.
     */
    @Test
    public void createRowTable() {
        String sql = SqlFactory.getNative(new HanaSqlDialect())
                               .create()
                               .rowTable("CUSTOMERS")
                               .column("ID", DataType.INTEGER, true, false, false)
                               .column("FIRST_NAME", DataType.VARCHAR, false, false, true, "(20)")
                               .column("LAST_NAME", DataType.VARCHAR, false, true, false, "(30)")
                               .build();

        assertNotNull(sql);
        assertEquals(
                "CREATE ROW TABLE \"CUSTOMERS\" ( \"ID\" INTEGER NOT NULL PRIMARY KEY , \"FIRST_NAME\" VARCHAR (20) NOT NULL UNIQUE , \"LAST_NAME\" VARCHAR (30) )",
                sql);
    }

    /**
     * Creates the table column type safe.
     */
    @Test
    public void createTableColumnTypeSafe() {
        String sql = SqlFactory.getNative(new HanaSqlDialect())
                               .create()
                               .columnTable("CUSTOMERS")
                               .columnInteger("ID", true, false, false)
                               .columnVarchar("FIRST_NAME", 20, false, true, true)
                               .columnVarchar("LAST_NAME", 30, false, true, false)
                               .build();

        assertNotNull(sql);
        assertEquals(
                "CREATE COLUMN TABLE \"CUSTOMERS\" ( \"ID\" INTEGER NOT NULL PRIMARY KEY , \"FIRST_NAME\" VARCHAR (20) UNIQUE , \"LAST_NAME\" VARCHAR (30) )",
                sql);
    }

    /**
     * Creates the table with composite key with set PK on column level.
     */
    @Test
    public void createTableWithCompositeKeyWithSetPKOnColumnLevel() {
        String sql = SqlFactory.getNative(new HanaSqlDialect())
                               .create()
                               .columnTable("CUSTOMERS")
                               .columnInteger("ID", true, false, false)
                               .columnInteger("ID2", true, false, false)
                               .columnVarchar("FIRST_NAME", 20, false, true, true)
                               .columnVarchar("LAST_NAME", 30, false, true, false)
                               .build();

        assertNotNull(sql);
        assertEquals(
                "CREATE COLUMN TABLE \"CUSTOMERS\" ( \"ID\" INTEGER NOT NULL , \"ID2\" INTEGER NOT NULL , \"FIRST_NAME\" VARCHAR (20) UNIQUE , \"LAST_NAME\" VARCHAR (30) , PRIMARY KEY(\"ID\" , \"ID2\") )",
                sql);
    }

    /**
     * Creates the table with composite key with set PK on constraint level.
     */
    @Test
    public void createTableWithCompositeKeyWithSetPKOnConstraintLevel() {
        String sql = SqlFactory.getNative(new HanaSqlDialect())
                               .create()
                               .columnTable("CUSTOMERS")
                               .columnInteger("ID", false, false, false)
                               .columnInteger("ID2", false, false, false)
                               .columnVarchar("FIRST_NAME", 20, false, true, true)
                               .columnVarchar("LAST_NAME", 30, false, true, false)
                               .primaryKey(new String[] {"ID", "ID2"})
                               .build();

        assertNotNull(sql);
        assertEquals(
                "CREATE COLUMN TABLE \"CUSTOMERS\" ( \"ID\" INTEGER NOT NULL , \"ID2\" INTEGER NOT NULL , \"FIRST_NAME\" VARCHAR (20) UNIQUE , \"LAST_NAME\" VARCHAR (30) , PRIMARY KEY ( \"ID\" , \"ID2\" ))",
                sql);
    }

    /**
     * Creates the table with composite key with set PK on constraint and column level.
     */
    @Test
    public void createTableWithCompositeKeyWithSetPKOnConstraintAndColumnLevel() {
        String sql = SqlFactory.getNative(new HanaSqlDialect())
                               .create()
                               .columnTable("CUSTOMERS")
                               .columnInteger("ID", true, false, false)
                               .columnInteger("ID2", true, false, false)
                               .columnVarchar("FIRST_NAME", 20, false, true, true)
                               .columnVarchar("LAST_NAME", 30, false, true, false)
                               .primaryKey(new String[] {"ID", "ID2"})
                               .build();

        assertNotNull(sql);
        assertEquals(
                "CREATE COLUMN TABLE \"CUSTOMERS\" ( \"ID\" INTEGER NOT NULL , \"ID2\" INTEGER NOT NULL , \"FIRST_NAME\" VARCHAR (20) UNIQUE , \"LAST_NAME\" VARCHAR (30) , PRIMARY KEY(\"ID\" , \"ID2\") )",
                sql);
    }

    /**
     * Creates the table with set PK on constraint and column level.
     */
    @Test
    public void createTableWithSetPKOnConstraintAndColumnLevel() {
        String sql = SqlFactory.getNative(new HanaSqlDialect())
                               .create()
                               .columnTable("CUSTOMERS")
                               .columnInteger("ID", true, false, false)
                               .columnVarchar("FIRST_NAME", 20, false, true, true)
                               .primaryKey(new String[] {"ID"})
                               .build();

        assertNotNull(sql);
        assertEquals("CREATE COLUMN TABLE \"CUSTOMERS\" ( \"ID\" INTEGER NOT NULL PRIMARY KEY , \"FIRST_NAME\" VARCHAR (20) UNIQUE )", sql);
    }

    /**
     * Parses the table without PK.
     */
    @Test
    public void parseTableWithoutPK() {
        String sql = SqlFactory.getNative(new HanaSqlDialect())
                               .create()
                               .columnTable("CUSTOMERS")
                               .columnInteger("ID", false, false, false)
                               .columnVarchar("FIRST_NAME", 20, false, true, true)
                               .primaryKey(new String[] {})
                               .build();

        assertNotNull(sql);
        assertEquals("CREATE COLUMN TABLE \"CUSTOMERS\" ( \"ID\" INTEGER NOT NULL , \"FIRST_NAME\" VARCHAR (20) UNIQUE )", sql);
    }

    /**
     * Creates the row table with indexes.
     */
    @Test
    public void createRowTableWithIndexes() {
        String sql = SqlFactory.getNative(new HanaSqlDialect())
                               .create()
                               .rowTable("CUSTOMERS")
                               .column("ID", DataType.INTEGER, true, false, false)
                               .column("FIRST_NAME", DataType.VARCHAR, false, false, true, "(20)")
                               .column("LAST_NAME", DataType.VARCHAR, false, true, false, "(30)")
                               .index("I1", false, "DESC", "BTREE", new HashSet<>(List.of("LAST_NAME")))
                               .unique("I2", new String[] {"ID"}, "CPBTREE", "ASC")
                               .build();

        assertNotNull("Unexpected result from builder", sql);
        assertTrue("Expected create table statement was not found", sql.contains(
                "CREATE ROW TABLE \"CUSTOMERS\" ( \"ID\" INTEGER NOT NULL PRIMARY KEY , \"FIRST_NAME\" VARCHAR (20) NOT NULL UNIQUE , \"LAST_NAME\" VARCHAR (30) );"));
        assertThat(sql).contains("CREATE UNIQUE CPBTREE INDEX \"I2\" ON \"CUSTOMERS\" ( \"ID\" ) ASC");
        assertThat(sql).contains("CREATE BTREE INDEX \"I1\" ON \"CUSTOMERS\" ( \"LAST_NAME\" ) DESC");
        int expectedStatementLength =
                "CREATE ROW TABLE \"CUSTOMERS\" ( \"ID\" INTEGER NOT NULL PRIMARY KEY , \"FIRST_NAME\" VARCHAR (20) NOT NULL UNIQUE , \"LAST_NAME\" VARCHAR (30) ); CREATE UNIQUE CPBTREE INDEX \"I2\" ON \"CUSTOMERS\" ( \"ID\" ) ASC; CREATE BTREE INDEX \"I1\" ON \"CUSTOMERS\" ( \"LAST_NAME\" ) DESC".length();
        assertEquals("Unexpected length of statement", expectedStatementLength, sql.length());
    }

    /**
     * Test create table with indexes.
     */
    @Test
    public void testCreateTableWithIndexes() {
        TableStatements table = SqlFactory.getNative(new HanaSqlDialect())
                                          .create()
                                          .rowTable("CUSTOMERS")
                                          .column("ID", DataType.INTEGER, true, false, false)
                                          .column("FIRST_NAME", DataType.VARCHAR, false, false, true, "(20)")
                                          .column("LAST_NAME", DataType.VARCHAR, false, true, false, "(30)")
                                          .index("I1", false, "DESC", "BTREE", new HashSet<>(List.of("LAST_NAME")))
                                          .unique("I2", new String[] {"ID"}, "CPBTREE", "ASC")
                                          .buildTable();

        assertEquals("Unexpected create table statement",
                "CREATE ROW TABLE \"CUSTOMERS\" ( \"ID\" INTEGER NOT NULL PRIMARY KEY , \"FIRST_NAME\" VARCHAR (20) NOT NULL UNIQUE , \"LAST_NAME\" VARCHAR (30) )",
                table.getCreateTableStatement());

        Collection<String> expected = Arrays.asList("CREATE UNIQUE CPBTREE INDEX \"I2\" ON \"CUSTOMERS\" ( \"ID\" ) ASC",
                "CREATE BTREE INDEX \"I1\" ON \"CUSTOMERS\" ( \"LAST_NAME\" ) DESC");

        MatcherAssert.assertThat("Indices equality without order", table.getCreateIndicesStatements(),
                Matchers.containsInAnyOrder(expected.toArray()));
    }

    /**
     * Test create global temporary table.
     */
    @Test
    public void testCreateGlobalTemporaryTable() {
        String sql = SqlFactory.getNative(new HanaSqlDialect())
                               .create()
                               .table("CUSTOMERS", ISqlKeywords.KEYWORD_GLOBAL_TEMPORARY)
                               .column("ID", DataType.INTEGER, true, false, false)
                               .column("FIRST_NAME", DataType.VARCHAR, false, false, true, "(20)")
                               .column("LAST_NAME", DataType.VARCHAR, false, true, false, "(30)")
                               .build();

        assertNotNull(sql);
        assertEquals(
                "CREATE GLOBAL TEMPORARY TABLE \"CUSTOMERS\" ( \"ID\" INTEGER NOT NULL PRIMARY KEY , \"FIRST_NAME\" VARCHAR (20) NOT NULL UNIQUE , \"LAST_NAME\" VARCHAR (30) )",
                sql);
    }

    /**
     * Test create global temporary column table.
     */
    @Test
    public void testCreateGlobalTemporaryColumnTable() {
        String sql = SqlFactory.getNative(new HanaSqlDialect())
                               .create()
                               .table("CUSTOMERS", ISqlKeywords.KEYWORD_GLOBAL_TEMPORARY_COLUMN)
                               .column("FIRST_NAME", DataType.VARCHAR, false, false, true, "(20)")
                               .column("LAST_NAME", DataType.VARCHAR, false, true, false, "(30)")
                               .build();

        assertNotNull(sql);
        assertEquals(
                "CREATE GLOBAL TEMPORARY COLUMN TABLE \"CUSTOMERS\" ( \"FIRST_NAME\" VARCHAR (20) NOT NULL UNIQUE , \"LAST_NAME\" VARCHAR (30) )",
                sql);
    }

}
