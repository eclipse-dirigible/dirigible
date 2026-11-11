/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.postgres;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.dirigible.database.sql.DataType;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.junit.Test;

/**
 * The Class CreateTableTest.
 */
public class CreateTableTest {

    /**
     * Creates the table generic.
     */
    @Test
    public void createTableGeneric() {
        String sql = SqlFactory.getNative(new PostgresSqlDialect())
                               .create()
                               .table("CUSTOMERS")
                               .column("ID", DataType.INTEGER, true, false, false)
                               .column("FIRST_NAME", DataType.VARCHAR, false, false, true, "(20)")
                               .column("LAST_NAME", DataType.VARCHAR, false, true, false, "(30)")
                               .build();

        assertNotNull(sql);
        assertEquals(
                "CREATE TABLE \"CUSTOMERS\" ( \"ID\" INTEGER NOT NULL PRIMARY KEY , \"FIRST_NAME\" VARCHAR (20) NOT NULL UNIQUE , \"LAST_NAME\" VARCHAR (30) )",
                sql);
    }

    /**
     * Creates the table type safe.
     */
    @Test
    public void createTableTypeSafe() {
        String sql = SqlFactory.getNative(new PostgresSqlDialect())
                               .create()
                               .table("CUSTOMERS")
                               .columnInteger("ID", true, false, false)
                               .columnVarchar("FIRST_NAME", 20, false, true, true)
                               .columnVarchar("LAST_NAME", 30, false, true, false)
                               .build();

        assertNotNull(sql);
        assertEquals(
                "CREATE TABLE \"CUSTOMERS\" ( \"ID\" INTEGER NOT NULL PRIMARY KEY , \"FIRST_NAME\" VARCHAR (20) UNIQUE , \"LAST_NAME\" VARCHAR (30) )",
                sql);
    }

    /**
     * PostgreSQL has no CLOB / NCLOB type, so the large character types all render as its unbounded
     * TEXT. A table declaring a CLOB column - what a text field generates - used to fail to create with
     * 'type "clob" does not exist'.
     */
    @Test
    public void createTableWithLargeCharacterColumns() {
        String sql = SqlFactory.getNative(new PostgresSqlDialect())
                               .create()
                               .table("NOTES")
                               .column("NOTE", DataType.CLOB)
                               .column("SUMMARY", DataType.NCLOB)
                               .column("BODY", DataType.CHARACTER_LARGE_OBJECT)
                               .build();

        assertNotNull(sql);
        assertEquals("CREATE TABLE \"NOTES\" ( \"NOTE\" TEXT , \"SUMMARY\" TEXT , \"BODY\" TEXT )", sql);
    }

    /**
     * PostgreSQL has no VARBINARY / BINARY VARYING / BINARY type - its binary type is bytea, which
     * takes no length (#8291). The table used to fail with 'type "varbinary" does not exist', and
     * bytea(100) is a syntax error.
     */
    @Test
    public void createTableWithBinaryColumns() {
        String sql = SqlFactory.getNative(new PostgresSqlDialect())
                               .create()
                               .table("FILES")
                               .column("ID", DataType.INTEGER, true, false, false)
                               .column("VB", DataType.VARBINARY, false, true, false, "(100)")
                               .column("BV", DataType.BINARY_VARYING, false, false, false, "(50)")
                               .column("BI", DataType.BINARY, false, true, false, "(16)")
                               .column("BL", DataType.BINARY_LARGE_OBJECT)
                               .columnVarbinary("PLAIN", true)
                               .build();

        assertNotNull(sql);
        assertEquals("CREATE TABLE \"FILES\" ( \"ID\" INTEGER NOT NULL PRIMARY KEY , \"VB\" bytea , \"BV\" bytea NOT NULL ,"
                + " \"BI\" bytea , \"BL\" bytea , \"PLAIN\" bytea )", sql);
    }

    /**
     * A length dropped from a bytea column leaves the arguments after it in place.
     */
    @Test
    public void createTableWithBinaryColumnKeepsTheArgumentsAfterTheLength() {
        String sql = SqlFactory.getNative(new PostgresSqlDialect())
                               .create()
                               .table("FILES")
                               .column("VB", DataType.VARBINARY, false, true, false, "(100) DEFAULT NULL")
                               .build();

        assertEquals("CREATE TABLE \"FILES\" ( \"VB\" bytea DEFAULT NULL )", sql);
    }

    /**
     * An added VARBINARY column renders as bytea without its length as well.
     */
    @Test
    public void alterTableAddBinaryColumn() {
        String sql = SqlFactory.getNative(new PostgresSqlDialect())
                               .alter()
                               .table("FILES")
                               .add()
                               .column("VB", DataType.VARBINARY, false, true, false, "(100)")
                               .build();

        assertEquals("ALTER TABLE \"FILES\" ADD \"VB\" bytea ;", sql);
    }

    /**
     * A schema name that is not a plain identifier is quoted as one identifier, not word by word.
     */
    @Test
    public void createTableInSchemaWithHyphen() {
        String sql = SqlFactory.getNative(new PostgresSqlDialect())
                               .create()
                               .table("CUSTOMERS")
                               .schema("my-schema")
                               .column("ID", DataType.INTEGER)
                               .build();

        assertNotNull(sql);
        assertEquals("CREATE TABLE \"my-schema\".\"CUSTOMERS\" ( \"ID\" INTEGER )", sql);
    }

}
