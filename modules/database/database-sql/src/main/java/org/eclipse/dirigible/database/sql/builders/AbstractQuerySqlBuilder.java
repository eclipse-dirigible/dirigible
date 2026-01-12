/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.builders;

import java.util.List;

import org.eclipse.dirigible.database.sql.ISqlDialect;

/**
 * The Abstract Query SQL Builder.
 */
public abstract class AbstractQuerySqlBuilder extends AbstractSqlBuilder {

    /**
     * Instantiates a new abstract query sql builder.
     *
     * @param dialect the dialect
     */
    protected AbstractQuerySqlBuilder(ISqlDialect dialect) {
        super(dialect);
    }

    /**
     * Generate create.
     *
     * @param sql the sql
     */
    protected void generateCreate(StringBuilder sql) {
        sql.append(KEYWORD_CREATE);
    }

    /**
     * Generate where.
     *
     * @param sql the sql
     * @param wheres the wheres
     */
    protected void generateWhere(StringBuilder sql, List<String> wheres) {
        if (!wheres.isEmpty()) {
            sql.append(SPACE)
               .append(KEYWORD_WHERE)
               .append(SPACE)
               .append(traverseWheres(wheres));
        }
    }

    /**
     * Generate order by.
     *
     * @param sql the sql
     * @param orders the orders
     */
    protected void generateOrderBy(StringBuilder sql, List<String> orders) {
        if (!orders.isEmpty()) {
            sql.append(SPACE)
               .append(KEYWORD_ORDER_BY)
               .append(SPACE)
               .append(traverseOrders(orders));
        }
    }

    /**
     * Generate limit and offset.
     *
     * @param sql the sql
     * @param limit the limit
     * @param offset the offset
     */
    protected void generateLimitAndOffset(StringBuilder sql, int limit, int offset) {
        if (limit > -1) {
            sql.append(SPACE)
               .append(KEYWORD_LIMIT)
               .append(SPACE)
               .append(limit);
        }
        if (offset > -1) {
            sql.append(SPACE)
               .append(KEYWORD_OFFSET)
               .append(SPACE)
               .append(offset);
        }
    }

    /**
     * Traverse wheres.
     *
     * @param wheres the wheres
     * @return the string
     */
    protected String traverseWheres(List<String> wheres) {
        StringBuilder snippet = new StringBuilder();
        for (String where : wheres) {
            where = encapsulateWhere(where);

            snippet.append(where)
                   .append(SPACE)
                   .append(KEYWORD_AND)
                   .append(SPACE);
        }
        return snippet.substring(0, snippet.length() - 5);
    }

    protected String traverseOn(String on) {
        StringBuilder snippet = new StringBuilder();
        on = encapsulateMany(on, getEscapeSymbol());

        snippet.append(on)
               .append(SPACE)
               .append(KEYWORD_AND)
               .append(SPACE);
        return snippet.substring(0, snippet.length() - 5);
    }

    protected String traverseHaving(String having) {
        StringBuilder snippet = new StringBuilder();
        having = encapsulateMany(having, getEscapeSymbol());

        snippet.append(having)
               .append(SPACE)
               .append(KEYWORD_AND)
               .append(SPACE);
        return snippet.substring(0, snippet.length() - 5);
    }

    /**
     * Traverse orders.
     *
     * @param orders the orders
     * @return the string
     */
    private String traverseOrders(List<String> orders) {
        StringBuilder snippet = new StringBuilder();
        for (String order : orders) {
            snippet.append(order)
                   .append(COMMA)
                   .append(SPACE);
        }
        return snippet.substring(0, snippet.length() - 2);
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return generate();
    }

}
