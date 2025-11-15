/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.builders.records;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.eclipse.dirigible.database.sql.SqlFactory;
import org.junit.Test;

/**
 * The Class SelectTest.
 */
public class SelectRecordTest {

    /**
     * Select star.
     */
    @Test
    public void selectStar() {
        String sql = SqlFactory.getDefault()
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
        String sql = SqlFactory.getDefault()
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
        String sql = SqlFactory.getDefault()
                               .select()
                               .column("c.FIRST_NAME")
                               .column("c.LAST_NAME")
                               .from("CUSTOMERS", "c")
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT \"c\".\"FIRST_NAME\", \"c\".\"LAST_NAME\" FROM \"CUSTOMERS\" AS \"c\"", sql);
        System.out.println(sql);
    }

    /**
     * Select columns from table join.
     */
    @Test
    public void selectColumnsFromTableJoin() {
        String sql = SqlFactory.getDefault()
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
        String sql = SqlFactory.getDefault()
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
        String sql = SqlFactory.getDefault()
                               .select()
                               .column("FIRST_NAME")
                               .column("LAST_NAME")
                               .from("CUSTOMERS")
                               .outerJoin("ADDRESSES", "CUSTOMERS.ADDRESS_ID=ADDRESSES.ADDRESS_ID")
                               .build();

        assertNotNull(sql);
        assertEquals(
                "SELECT \"FIRST_NAME\", \"LAST_NAME\" FROM \"CUSTOMERS\" OUTER JOIN \"ADDRESSES\" ON \"CUSTOMERS\".\"ADDRESS_ID\"=\"ADDRESSES\".\"ADDRESS_ID\"",
                sql);
    }

    /**
     * Select columns from table left join.
     */
    @Test
    public void selectColumnsFromTableLeftJoin() {
        String sql = SqlFactory.getDefault()
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
        String sql = SqlFactory.getDefault()
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
        String sql = SqlFactory.getDefault()
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
        String sql = SqlFactory.getDefault()
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
        String sql = SqlFactory.getDefault()
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
        String sql = SqlFactory.getDefault()
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
        String sql = SqlFactory.getDefault()
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
        String sql = SqlFactory.getDefault()
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
        String sql = SqlFactory.getDefault()
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
        String sql = SqlFactory.getDefault()
                               .select()
                               .column("*")
                               .from("CUSTOMERS")
                               .where(SqlFactory.getDefault()
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
        String sql = SqlFactory.getDefault()
                               .select()
                               .column("*")
                               .from("CUSTOMERS")
                               .limit(10)
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT * FROM \"CUSTOMERS\" LIMIT 10", sql);
    }

    /**
     * Select limit offset.
     */
    @Test
    public void selectLimitOffset() {
        String sql = SqlFactory.getDefault()
                               .select()
                               .column("*")
                               .from("CUSTOMERS")
                               .limit(10)
                               .offset(20)
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT * FROM \"CUSTOMERS\" LIMIT 10 OFFSET 20", sql);
    }

    /**
     * Select having.
     */
    @Test
    public void selectHaving() {
        String sql = SqlFactory.getDefault()
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
        String sql = SqlFactory.getDefault()
                               .select()
                               .column("COUNTRY")
                               .from("CUSTOMERS")
                               .union(SqlFactory.getDefault()
                                                .select()
                                                .column("COUNTRY")
                                                .from("SUPPLIERS")
                                                .build())
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT \"COUNTRY\" FROM \"CUSTOMERS\" UNION SELECT \"COUNTRY\" FROM \"SUPPLIERS\"", sql);
    }

    /**
     * Select star omitted.
     */
    @Test
    public void selectStarOmitted() {
        String sql = SqlFactory.getDefault()
                               .select()
                               .from("CUSTOMERS")
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT * FROM \"CUSTOMERS\"", sql);
    }

    /**
     * Select star in case sensitive mode.
     */
    @Test
    public void selectStarCaseSensitive() {
        String sql = SqlFactory.getDefault()
                               .select()
                               .column("*")
                               .from("CUSTOMERS")
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT * FROM \"CUSTOMERS\"", sql);

    }

    /**
     * Select star in case sensitive mode.
     */
    @Test
    public void selectStarAndColumnCaseSensitive() {
        String sql = SqlFactory.getDefault()
                               .select()
                               .column("*")
                               .column("FIRST_NAME")
                               .from("CUSTOMERS")
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT *, \"FIRST_NAME\" FROM \"CUSTOMERS\"", sql);
    }

    /**
     * Select count in case sensitive mode.
     */
    @Test
    public void selectCountCaseSensitive() {
        String sql = SqlFactory.getDefault()
                               .select()
                               .column("COUNT(*)")
                               .from("CUSTOMERS")
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT COUNT(*) FROM \"CUSTOMERS\"", sql);
    }

    /**
     * Select column in case sensitive mode.
     */
    @Test
    public void selectColumnCaseSensitive() {
        String sql = SqlFactory.getDefault()
                               .select()
                               .column("FIRST_NAME")
                               .from("CUSTOMERS")
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT \"FIRST_NAME\" FROM \"CUSTOMERS\"", sql);
    }

    /**
     * Select column and where clause in case sensitive mode.
     */
    @Test
    public void selectColumnWithWhereCaseSensitive() {
        String sql = SqlFactory.getDefault()
                               .select()
                               .column("FIRST_NAME")
                               .from("CUSTOMERS")
                               .where("PRICE > ?")
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT \"FIRST_NAME\" FROM \"CUSTOMERS\" WHERE (\"PRICE\" > ?)", sql);
    }

    /**
     * Select column and where clause in case sensitive mode.
     */
    @Test
    public void selectComplexColumnCaseSensitive() {
        String sql = SqlFactory.getDefault()
                               .select()
                               .column("POWER(PRICE, QTY)")
                               .from("CUSTOMERS")
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT POWER(\"PRICE\", \"QTY\") FROM \"CUSTOMERS\"", sql);
    }

    /**
     * Select column and where clause in case sensitive mode.
     */
    @Test
    public void selectComplexColumnsCaseSensitive() {
        String sql = SqlFactory.getDefault()
                               .select()
                               .column("(PRICE - QTY)")
                               .from("CUSTOMERS")
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT (\"PRICE\" - \"QTY\") FROM \"CUSTOMERS\"", sql);
    }

    /**
     * Select count and where clause is in case sensitive mode.
     */
    @Test
    public void selectArtifactWithNamespaceAndSchemaNameCaseSensitive() {
        String sql = SqlFactory.getDefault()
                               .select()
                               .column("COUNT(*)")
                               .from("\"ShemaName\".\"projName.db::MySynonym\"")
                               .build();
        assertNotNull(sql);
        assertEquals("SELECT COUNT(*) FROM \"ShemaName\".\"projName.db::MySynonym\"", sql);
    }

    /**
     * Select count and where clause is in case sensitive mode.
     */
    @Test
    public void selectArtifactWithNamespaceCaseSensitive() {
        String sql = SqlFactory.getDefault()
                               .select()
                               .column("COUNT(*)")
                               .from("projName::MySynonym")
                               .build();
        assertNotNull(sql);
        assertEquals("SELECT COUNT(*) FROM \"projName::MySynonym\"", sql);
    }

    /**
     * Select column and where clause in case sensitive mode.
     */
    @Test
    public void selectFunctionCaseSensitive() {
        String sql = SqlFactory.getDefault()
                               .select()
                               .column("COS(0.0) c")
                               .from("DUMMY")
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT COS(0.0) \"c\" FROM \"DUMMY\"", sql);
    }

    /**
     * Select column and where clause in case sensitive mode.
     */
    @Test
    public void selectColumnWithSpecialCharsCaseSensitive() {
        String sql = SqlFactory.getDefault()
                               .select()
                               .column("FIRST_NAME1$")
                               .from("CUSTOMERS")
                               .where("PRICE_BASIC1$ LIKE ?")
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT \"FIRST_NAME1$\" FROM \"CUSTOMERS\" WHERE (\"PRICE_BASIC1$\" LIKE ?)", sql);
    }

    /**
     * Select column and where clause in case sensitive mode.
     */
    @Test
    public void selectColumnWithSpecialCharsAndNumbersCaseSensitive() {
        String sql = SqlFactory.getDefault()
                               .select()
                               .column("FIRST_NAME1$")
                               .from("CUSTOMERS")
                               .where("PRICE_BASIC1$ LIKE ?")
                               .where("PRICE_BASIC2$ == 0")
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT \"FIRST_NAME1$\" FROM \"CUSTOMERS\" WHERE (\"PRICE_BASIC1$\" LIKE ?) AND (\"PRICE_BASIC2$\" == 0)", sql);
    }

    /**
     * Select column and where clause with schema in case sensitive mode.
     */
    @Test
    public void selectColumnWhereAndSchemaCaseSensitive() {
        String sql = SqlFactory.getDefault()
                               .select()
                               .column("FIRST_NAME")
                               .from("CUSTOMERS")
                               .schema("PUBLIC")
                               .where("PRICE_BASIC LIKE ?")
                               .build();

        assertNotNull(sql);
        assertEquals("SELECT \"FIRST_NAME\" FROM \"PUBLIC\".\"CUSTOMERS\" WHERE (\"PRICE_BASIC\" LIKE ?)", sql);
    }

}
