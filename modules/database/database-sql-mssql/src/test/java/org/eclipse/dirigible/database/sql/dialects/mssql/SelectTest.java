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
 * The Class SelectTest.
 */
public class SelectTest {

    /**
     * Select star.
     */
    @Test
    public void selectStar() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .select()
                               .column("*")
                               .from("CUSTOMERS")
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT * FROM \"CUSTOMERS\"", sql);
    }

    /**
     * Select columns from table.
     */
    @Test
    public void selectColumnsFromTable() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .select()
                               .column("FIRST_NAME")
                               .column("LAST_NAME")
                               .from("CUSTOMERS")
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT \"FIRST_NAME\", \"LAST_NAME\" FROM \"CUSTOMERS\"", sql);
    }

    /**
     * Select columns from table aliases.
     */
    @Test
    public void selectColumnsFromTableAliases() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .select()
                               .column("c.FIRST_NAME")
                               .column("c.LAST_NAME")
                               .from("CUSTOMERS", "c")
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT \"c\".\"FIRST_NAME\", \"c\".\"LAST_NAME\" FROM \"CUSTOMERS\" AS \"c\"", sql);
    }

    /**
     * Select columns from table join.
     */
    @Test
    public void selectColumnsFromTableJoin() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .select()
                               .column("FIRST_NAME")
                               .column("LAST_NAME")
                               .from("CUSTOMERS")
                               .join("ADDRESSES", "CUSTOMERS.ADDRESS_ID=ADDRESSES.ADDRESS_ID")
                               .build();

        assertNotNull(sql);
        assertEquals(
                "SELECT \"FIRST_NAME\", \"LAST_NAME\" FROM \"CUSTOMERS\" INNER JOIN \"ADDRESSES\" ON \"CUSTOMERS\".\"ADDRESS_ID\"=\"ADDRESSES\".\"ADDRESS_ID\"",
                sql);
    }

    /**
     * Select columns from table inner join.
     */
    @Test
    public void selectColumnsFromTableInnerJoin() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .select()
                               .column("FIRST_NAME")
                               .column("LAST_NAME")
                               .from("CUSTOMERS")
                               .innerJoin("ADDRESSES", "CUSTOMERS.ADDRESS_ID=ADDRESSES.ADDRESS_ID")
                               .build();

        assertNotNull(sql);
        assertEquals(
                "SELECT \"FIRST_NAME\", \"LAST_NAME\" FROM \"CUSTOMERS\" INNER JOIN \"ADDRESSES\" ON \"CUSTOMERS\".\"ADDRESS_ID\"=\"ADDRESSES\".\"ADDRESS_ID\"",
                sql);
    }

    /**
     * Select columns from table outer join.
     */
    @Test
    public void selectColumnsFromTableOuterJoin() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .select()
                               .column("FIRST_NAME")
                               .column("LAST_NAME")
                               .from("CUSTOMERS")
                               .outerJoin("ADDRESSES", "CUSTOMERS.ADDRESS_ID=ADDRESSES.ADDRESS_ID")
                               .build();

        assertNotNull(sql);
        System.out.println(sql);
        assertEquals(
                "SELECT \"FIRST_NAME\", \"LAST_NAME\" FROM \"CUSTOMERS\" FULL OUTER JOIN \"ADDRESSES\" ON \"CUSTOMERS\".\"ADDRESS_ID\"=\"ADDRESSES\".\"ADDRESS_ID\"",
                sql);
    }

    /**
     * Select columns from table left join.
     */
    @Test
    public void selectColumnsFromTableLeftJoin() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .select()
                               .column("FIRST_NAME")
                               .column("LAST_NAME")
                               .from("CUSTOMERS")
                               .leftJoin("ADDRESSES", "CUSTOMERS.ADDRESS_ID=ADDRESSES.ADDRESS_ID")
                               .build();

        assertNotNull(sql);
        assertEquals(
                "SELECT \"FIRST_NAME\", \"LAST_NAME\" FROM \"CUSTOMERS\" LEFT JOIN \"ADDRESSES\" ON \"CUSTOMERS\".\"ADDRESS_ID\"=\"ADDRESSES\".\"ADDRESS_ID\"",
                sql);
    }

    /**
     * Select columns from table right join.
     */
    @Test
    public void selectColumnsFromTableRightJoin() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .select()
                               .column("FIRST_NAME")
                               .column("LAST_NAME")
                               .from("CUSTOMERS")
                               .rightJoin("ADDRESSES", "CUSTOMERS.ADDRESS_ID=ADDRESSES.ADDRESS_ID")
                               .build();

        assertNotNull(sql);
        assertEquals(
                "SELECT \"FIRST_NAME\", \"LAST_NAME\" FROM \"CUSTOMERS\" RIGHT JOIN \"ADDRESSES\" ON \"CUSTOMERS\".\"ADDRESS_ID\"=\"ADDRESSES\".\"ADDRESS_ID\"",
                sql);
    }

    /**
     * Select columns from table full join.
     */
    @Test
    public void selectColumnsFromTableFullJoin() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .select()
                               .column("FIRST_NAME")
                               .column("LAST_NAME")
                               .from("CUSTOMERS")
                               .fullJoin("ADDRESSES", "CUSTOMERS.ADDRESS_ID=ADDRESSES.ADDRESS_ID")
                               .build();

        assertNotNull(sql);
        assertEquals(
                "SELECT \"FIRST_NAME\", \"LAST_NAME\" FROM \"CUSTOMERS\" FULL JOIN \"ADDRESSES\" ON \"CUSTOMERS\".\"ADDRESS_ID\"=\"ADDRESSES\".\"ADDRESS_ID\"",
                sql);
    }

    /**
     * Select distinct columns from table.
     */
    @Test
    public void selectDistinctColumnsFromTable() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .select()
                               .distinct()
                               .column("FIRST_NAME")
                               .column("LAST_NAME")
                               .from("CUSTOMERS")
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT DISTINCT \"FIRST_NAME\", \"LAST_NAME\" FROM \"CUSTOMERS\"", sql);
    }

    /**
     * Select columns from table order by and desc.
     */
    @Test
    public void selectColumnsFromTableOrderByAndDesc() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .select()
                               .column("FIRST_NAME")
                               .column("LAST_NAME")
                               .from("CUSTOMERS")
                               .order("FIRST_NAME")
                               .order("LAST_NAME", false)
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT \"FIRST_NAME\", \"LAST_NAME\" FROM \"CUSTOMERS\" ORDER BY \"FIRST_NAME\" ASC, \"LAST_NAME\" DESC", sql);
    }

    /**
     * Select columns from table group by.
     */
    @Test
    public void selectColumnsFromTableGroupBy() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .select()
                               .column("FIRST_NAME")
                               .column("LAST_NAME")
                               .from("CUSTOMERS")
                               .group("FIRST_NAME")
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT \"FIRST_NAME\", \"LAST_NAME\" FROM \"CUSTOMERS\" GROUP BY \"FIRST_NAME\"", sql);
    }

    /**
     * Select where simple.
     */
    @Test
    public void selectWhereSimple() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .select()
                               .column("*")
                               .from("CUSTOMERS")
                               .where("PRICE > ?")
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT * FROM \"CUSTOMERS\" WHERE (\"PRICE\" > ?)", sql);
    }

    /**
     * Select where and.
     */
    @Test
    public void selectWhereAnd() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .select()
                               .column("*")
                               .from("CUSTOMERS")
                               .where("PRICE > ?")
                               .where("AMOUNT < ?")
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT * FROM \"CUSTOMERS\" WHERE (\"PRICE\" > ?) AND (\"AMOUNT\" < ?)", sql);
    }

    /**
     * Select where or.
     */
    @Test
    public void selectWhereOr() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .select()
                               .column("*")
                               .from("CUSTOMERS")
                               .where("PRICE > ? OR AMOUNT < ?")
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT * FROM \"CUSTOMERS\" WHERE (\"PRICE\" > ? OR \"AMOUNT\" < ?)", sql);
    }

    /**
     * Select where expr.
     */
    @Test
    public void selectWhereExpr() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .select()
                               .column("*")
                               .from("CUSTOMERS")
                               .where(SqlFactory.getNative(new MSSQLSqlDialect())
                                                .expression()
                                                .and("PRICE > ?")
                                                .or("AMOUNT < ?")
                                                .build())
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT * FROM \"CUSTOMERS\" WHERE (\"PRICE\" > ? OR \"AMOUNT\" < ?)", sql);
    }

    /**
     * Select limit.
     */
    @Test
    public void selectLimit() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .select()
                               .column("*")
                               .from("CUSTOMERS")
                               .limit(10)
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT TOP 10 * FROM \"CUSTOMERS\"", sql);
    }

    /**
     * Select limit offset.
     */
    @Test
    public void selectLimitOffsetAndOrderBy() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .select()
                               .column("*")
                               .from("CUSTOMERS")
                               .order("CUSTOMER_ID")
                               .limit(10)
                               .offset(20)
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT * FROM \"CUSTOMERS\" ORDER BY \"CUSTOMER_ID\" ASC OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY", sql);
    }

    @Test
    public void selectLimitOffsetAndMissingOrderBy() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .select()
                               .column("*")
                               .from("CUSTOMERS")
                               .limit(10)
                               .offset(20)
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT * FROM \"CUSTOMERS\" ORDER BY (SELECT NULL) OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY", sql);
    }

    /**
     * Select having.
     */
    @Test
    public void selectHaving() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .select()
                               .column("COUNT(FIRST_NAME)")
                               .column("COUNTRY")
                               .from("CUSTOMERS")
                               .group("COUNTRY")
                               .having("COUNT(FIRST_NAME) > 5")
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT COUNT(\"FIRST_NAME\"), \"COUNTRY\" FROM \"CUSTOMERS\" GROUP BY \"COUNTRY\" HAVING COUNT(\"FIRST_NAME\") > 5",
                sql);
    }

    /**
     * Select union.
     */
    @Test
    public void selectUnion() {
        String sql = SqlFactory.getNative(new MSSQLSqlDialect())
                               .select()
                               .column("COUNTRY")
                               .from("CUSTOMERS")
                               .union(SqlFactory.getNative(new MSSQLSqlDialect())
                                                .select()
                                                .column("COUNTRY")
                                                .from("SUPPLIERS")
                                                .build())
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT \"COUNTRY\" FROM \"CUSTOMERS\" UNION SELECT \"COUNTRY\" FROM \"SUPPLIERS\"", sql);
    }

}
