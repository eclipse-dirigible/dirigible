/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.mssql;

import org.eclipse.dirigible.database.sql.SqlFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * The Class UpdateTest.
 */
public class UpdateTest {

    /**
     * Update simple.
     */
    @Test
    public void updateSimple() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .update()
                               .table("CUSTOMERS")
                               .set("FIRST_NAME", "'John'")
                               .build();

        assertNotNull(sql);
        assertEquals("UPDATE \"CUSTOMERS\" SET \"FIRST_NAME\" = 'John'", sql);
    }

    /**
     * Update values.
     */
    @Test
    public void updateValues() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .update()
                               .table("CUSTOMERS")
                               .set("FIRST_NAME", "'John'")
                               .set("LAST_NAME", "'Smith'")
                               .build();

        assertNotNull(sql);
        assertEquals("UPDATE \"CUSTOMERS\" SET \"FIRST_NAME\" = 'John', \"LAST_NAME\" = 'Smith'", sql);
    }

    /**
     * Update where.
     */
    @Test
    public void updateWhere() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .update()
                               .table("CUSTOMERS")
                               .set("FIRST_NAME", "'John'")
                               .set("LAST_NAME", "'Smith'")
                               .where("AGE > ?")
                               .where("COMPANY = 'SAP'")
                               .build();

        assertNotNull(sql);
        assertEquals(
                "UPDATE \"CUSTOMERS\" SET \"FIRST_NAME\" = 'John', \"LAST_NAME\" = 'Smith' WHERE (\"AGE\" > ?) AND (\"COMPANY\" = 'SAP')",
                sql);
    }

    /**
     * Update where select.
     */
    @Test
    public void updateWhereSelect() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .update()
                               .table("CUSTOMERS")
                               .set("FIRST_NAME", "'John'")
                               .set("SALARY", "(" + SqlFactory.getNative(new MSSQLSqlDialect())
                                                              .select()
                                                              .column("MAX(SALARY)")
                                                              .from("BENEFITS")
                                                              .build()
                                       + ")")
                               .where("COMPANY = 'SAP'")
                               .build();

        assertNotNull(sql);
        assertEquals(
                "UPDATE \"CUSTOMERS\" SET \"FIRST_NAME\" = 'John', \"SALARY\" = (SELECT MAX(\"SALARY\") FROM \"BENEFITS\") WHERE (\"COMPANY\" = 'SAP')",
                sql);
    }

    /**
     * Update where expr.
     */
    @Test
    public void updateWhereExpr() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .update()
                               .table("CUSTOMERS")
                               .set("FIRST_NAME", "'John'")
                               .set("LAST_NAME", "'Smith'")
                               .where(SqlFactory.getNative(new MSSQLSqlDialect())
                                                .expression()
                                                .and("PRICE > ?")
                                                .or("AMOUNT < ?")
                                                .and("COMPANY = 'SAP'")
                                                .build())
                               .build();

        assertNotNull(sql);
        assertEquals(
                "UPDATE \"CUSTOMERS\" SET \"FIRST_NAME\" = 'John', \"LAST_NAME\" = 'Smith' WHERE (\"PRICE\" > ? OR \"AMOUNT\" < ? AND \"COMPANY\" = 'SAP')",
                sql);
    }

}
