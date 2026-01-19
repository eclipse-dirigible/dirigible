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

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.records.SelectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MSSQLSelectBuilder extends SelectBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(MSSQLSelectBuilder.class);

    public MSSQLSelectBuilder(ISqlDialect dialect) {
        super(dialect);
    }

    @Override
    public SelectBuilder outerJoin(String table, String on, String alias) {
        LOGGER.warn("In MSSQL, the term OUTER JOIN is incomplete. It will be treated as FULL OUTER JOIN");
        return genericJoin(KEYWORD_FULL_OUTER, table, on, alias);
    }

    @Override
    public String generate() {
        StringBuilder sql = new StringBuilder();

        // SELECT
        generateSelect(sql);

        // ADD LIMIT IF MISSING OFFSET
        if (this.getOffset() == -1 && this.getLimit() > -1) {
            sql.append(SPACE)
               .append(KEYWORD_TOP)
               .append(SPACE)
               .append(this.getLimit());
        }

        // DISTINCT
        generateDistinct(sql);

        // COLUMNS
        generateColumns(sql);

        // TABLES
        generateTables(sql);

        // JOINS
        generateJoins(sql);

        // WHERE
        generateWhere(sql, getWheres());

        // GROUP BY
        generateGroupBy(sql);

        // HAVING
        generateHaving(sql);

        // ORDER BY
        generateOrderBy(sql, getOrders());

        // LIMIT AND OFFSET
        generateLimitAndOffset(sql, getLimit(), getOffset());

        // UNION
        generateUnion(sql);

        // FOR UPDATE
        generateForUpdate(sql);

        return sql.toString();
    }

    @Override
    protected void generateLimitAndOffset(StringBuilder sql, int limit, int offset) {
        if (limit > -1 && offset > -1) {
            if (getOrders().isEmpty()) {
                sql.append(SPACE)
                   .append(KEYWORD_ORDER_BY)
                   .append(SPACE)
                   .append("(SELECT NULL)");
            }
            sql.append(SPACE)
               .append(KEYWORD_OFFSET)
               .append(SPACE)
               .append(offset)
               .append(SPACE)
               .append("ROWS FETCH NEXT")
               .append(SPACE)
               .append(limit)
               .append(SPACE)
               .append("ROWS ONLY");
        }
    }

}
