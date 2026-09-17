/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.mariadb;

import static org.junit.Assert.assertEquals;

import org.eclipse.dirigible.database.sql.DataType;
import org.eclipse.dirigible.database.sql.Modifiers;
import org.eclipse.dirigible.database.sql.SqlFactory;
import org.junit.Test;

/**
 * A name the caller already quoted with the ANSI symbol - which is what every data-structure
 * processor hands the builders - must be re-quoted with the backtick, not wrapped in it (#7021).
 */
public class PreQuotedNameTest {

    /**
     * Creates a table whose name and columns arrive ANSI-quoted.
     */
    @Test
    public void createTableWithAnsiQuotedNames() {
        String sql = SqlFactory.getNative(new MariaDBSqlDialect())
                               .create()
                               .table("\"T_NOTES\"")
                               .column("\"ID\"", DataType.INTEGER, Modifiers.PRIMARY_KEY, Modifiers.NOT_NULL, Modifiers.NON_UNIQUE)
                               .build();

        assertEquals("CREATE TABLE `T_NOTES` ( `ID` INTEGER NOT NULL PRIMARY KEY )", sql);
    }

    /**
     * Adds a column to a table whose name arrives ANSI-quoted.
     */
    @Test
    public void alterTableAddColumnWithAnsiQuotedNames() {
        String sql = SqlFactory.getNative(new MariaDBSqlDialect())
                               .alter()
                               .table("\"T_NOTES\"")
                               .add()
                               .column("\"COMPANY\"", DataType.VARCHAR, Modifiers.REGULAR, Modifiers.NULLABLE, Modifiers.NON_UNIQUE, "(20)")
                               .build();

        assertEquals("ALTER TABLE `T_NOTES` ADD `COMPANY` VARCHAR (20) ;", sql);
    }

    /**
     * Adds a unique constraint to a table whose name arrives ANSI-quoted.
     */
    @Test
    public void alterTableAddUniqueWithAnsiQuotedTableName() {
        String sql = SqlFactory.getNative(new MariaDBSqlDialect())
                               .alter()
                               .table("\"T_NOTES\"")
                               .add()
                               .unique("Notes_Company_Number", new String[] {"COMPANY", "NUMBER"})
                               .build();

        assertEquals("ALTER TABLE `T_NOTES` ADD CONSTRAINT `Notes_Company_Number` UNIQUE ( `COMPANY` , `NUMBER` );", sql);
    }

    /**
     * Drops a table whose name arrives ANSI-quoted.
     */
    @Test
    public void dropTableWithAnsiQuotedName() {
        String sql = SqlFactory.getNative(new MariaDBSqlDialect())
                               .drop()
                               .table("\"T_NOTES\"")
                               .build();

        assertEquals("DROP TABLE `T_NOTES`", sql);
    }

    /**
     * A schema-qualified ANSI-quoted name keeps its parts separate.
     */
    @Test
    public void dropSchemaQualifiedAnsiQuotedName() {
        String sql = SqlFactory.getNative(new MariaDBSqlDialect())
                               .drop()
                               .table("\"MYSCHEMA\".\"T_NOTES\"")
                               .build();

        assertEquals("DROP TABLE `MYSCHEMA`.`T_NOTES`", sql);
    }

}
