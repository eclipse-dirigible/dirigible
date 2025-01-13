/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.builders.records;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.AbstractQuerySqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The Select Builder.
 */
public class SelectBuilder extends AbstractQuerySqlBuilder {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(SelectBuilder.class);

    /** The columns. */
    private final List<String> columns = new ArrayList<String>();

    /** The tables. */
    private final List<String> tables = new ArrayList<String>();

    /** The joins. */
    private final List<String> joins = new ArrayList<String>();

    /** The wheres. */
    private final List<String> wheres = new ArrayList<String>();

    /** The orders. */
    private final List<String> orders = new ArrayList<String>();

    /** The groups. */
    private final List<String> groups = new ArrayList<String>();

    /** The unions. */
    private final List<String> unions = new ArrayList<String>();

    /** The distinct. */
    private boolean distinct = false;

    /** The having. */
    private String having = null;

    /** The limit. */
    private int limit = -1;

    /** The offset. */
    private int offset = -1;

    /** The for update. */
    private boolean forUpdate = false;

    /** The schema. */
    private String schema = null;

    /**
     * Instantiates a new select builder.
     *
     * @param dialect the dialect
     */
    public SelectBuilder(ISqlDialect dialect) {
        super(dialect);
    }

    /**
     * Distinct.
     *
     * @return the select builder
     */
    public SelectBuilder distinct() {
        if (logger.isTraceEnabled()) {
            logger.trace("distinct");
        }
        this.distinct = true;
        return this;
    }

    /**
     * For update.
     *
     * @return the select builder
     */
    public SelectBuilder forUpdate() {
        if (logger.isTraceEnabled()) {
            logger.trace("forUpdate");
        }
        this.forUpdate = true;
        return this;
    }

    /**
     * Column.
     *
     * @param column the column
     * @return the select builder
     */
    public SelectBuilder column(String column) {
        if (logger.isTraceEnabled()) {
            logger.trace("column: " + column);
        }
        this.columns.add(column);
        return this;
    }

    /**
     * From.
     *
     * @param table the table
     * @return the select builder
     */
    public SelectBuilder from(String table) {
        if (logger.isTraceEnabled()) {
            logger.trace("from: " + table);
        }
        return from(table, null);
    }

    /**
     * From.
     *
     * @param table the table
     * @param alias the alias
     * @return the select builder
     */
    public SelectBuilder from(String table, String alias) {
        if (logger.isTraceEnabled()) {
            logger.trace("from: " + table + ", alias: " + alias);
        }
        StringBuilder snippet = new StringBuilder();
        snippet.append(encapsulate(table));
        if (alias != null) {
            snippet.append(SPACE)
                   .append(KEYWORD_AS)
                   .append(SPACE)
                   .append(encapsulate(alias));
        }
        this.tables.add(snippet.toString());
        return this;
    }

    /**
     * From.
     *
     * @param schema the schema
     * @return the select builder
     */
    public SelectBuilder schema(String schema) {
        if (logger.isTraceEnabled()) {
            logger.trace("schema: " + schema);
        }
        this.schema = schema;
        return this;
    }

    /**
     * Join.
     *
     * @param table the table
     * @param on the on
     * @return the select builder
     */
    public SelectBuilder join(String table, String on) {
        if (logger.isTraceEnabled()) {
            logger.trace("join: " + table + ", on: " + on);
        }
        return join(table, on, null);
    }

    /**
     * Join.
     *
     * @param table the table
     * @param on the on
     * @param alias the alias
     * @return the select builder
     */
    public SelectBuilder join(String table, String on, String alias) {
        if (logger.isTraceEnabled()) {
            logger.trace("join: " + table + ", on: " + on + ", alias: " + alias);
        }
        return genericJoin(KEYWORD_INNER, table, on, alias);
    }

    /**
     * Inner join.
     *
     * @param table the table
     * @param on the on
     * @return the select builder
     */
    public SelectBuilder innerJoin(String table, String on) {
        if (logger.isTraceEnabled()) {
            logger.trace("innerJoin: " + table + ", on: " + on);
        }
        return innerJoin(table, on, null);
    }

    /**
     * Inner join.
     *
     * @param table the table
     * @param on the on
     * @param alias the alias
     * @return the select builder
     */
    public SelectBuilder innerJoin(String table, String on, String alias) {
        if (logger.isTraceEnabled()) {
            logger.trace("innerJoin: " + table + ", on: " + on + ", alias: " + alias);
        }
        return genericJoin(KEYWORD_INNER, table, on, alias);
    }

    /**
     * Outer join.
     *
     * @param table the table
     * @param on the on
     * @return the select builder
     */
    public SelectBuilder outerJoin(String table, String on) {
        if (logger.isTraceEnabled()) {
            logger.trace("outerJoin: " + table + ", on: " + on);
        }
        return outerJoin(table, on, null);
    }

    /**
     * Outer join.
     *
     * @param table the table
     * @param on the on
     * @param alias the alias
     * @return the select builder
     */
    public SelectBuilder outerJoin(String table, String on, String alias) {
        if (logger.isTraceEnabled()) {
            logger.trace("outerJoin: " + table + ", on: " + on + ", alias: " + alias);
        }
        return genericJoin(KEYWORD_OUTER, table, on, alias);
    }

    /**
     * Left join.
     *
     * @param table the table
     * @param on the on
     * @return the select builder
     */
    public SelectBuilder leftJoin(String table, String on) {
        if (logger.isTraceEnabled()) {
            logger.trace("leftJoin: " + table + ", on: " + on);
        }
        return leftJoin(table, on, null);
    }

    /**
     * Left join.
     *
     * @param table the table
     * @param on the on
     * @param alias the alias
     * @return the select builder
     */
    public SelectBuilder leftJoin(String table, String on, String alias) {
        if (logger.isTraceEnabled()) {
            logger.trace("leftJoin: " + table + ", on: " + on + ", alias: " + alias);
        }
        return genericJoin(KEYWORD_LEFT, table, on, alias);
    }

    /**
     * Right join.
     *
     * @param table the table
     * @param on the on
     * @return the select builder
     */
    public SelectBuilder rightJoin(String table, String on) {
        if (logger.isTraceEnabled()) {
            logger.trace("rightJoin: " + table + ", on: " + on);
        }
        return rightJoin(table, on, null);
    }

    /**
     * Right join.
     *
     * @param table the table
     * @param on the on
     * @param alias the alias
     * @return the select builder
     */
    public SelectBuilder rightJoin(String table, String on, String alias) {
        if (logger.isTraceEnabled()) {
            logger.trace("rightJoin: " + table + ", on: " + on + ", alias: " + alias);
        }
        return genericJoin(KEYWORD_RIGHT, table, on, alias);
    }

    /**
     * Full join.
     *
     * @param table the table
     * @param on the on
     * @return the select builder
     */
    public SelectBuilder fullJoin(String table, String on) {
        if (logger.isTraceEnabled()) {
            logger.trace("fullJoin: " + table + ", on: " + on);
        }
        return fullJoin(table, on, null);
    }

    /**
     * Full join.
     *
     * @param table the table
     * @param on the on
     * @param alias the alias
     * @return the select builder
     */
    public SelectBuilder fullJoin(String table, String on, String alias) {
        if (logger.isTraceEnabled()) {
            logger.trace("fullJoin: " + table + ", on: " + on + ", alias: " + alias);
        }
        return genericJoin(KEYWORD_FULL, table, on, alias);
    }

    /**
     * Generic join.
     *
     * @param type the type
     * @param table the table
     * @param on the on
     * @param alias the alias
     * @return the select builder
     */
    public SelectBuilder genericJoin(String type, String table, String on, String alias) {
        if (logger.isTraceEnabled()) {
            logger.trace("genericJoin: " + type + ", table: " + table + ", on: " + on + ", alias: " + alias);
        }
        StringBuilder snippet = new StringBuilder();
        String schemaName = encapsulate(schema, true);
        String tableName = encapsulate(table, true);
        if (schema != null) {
            snippet.append(type)
                   .append(SPACE)
                   .append(KEYWORD_JOIN)
                   .append(SPACE)
                   .append(schemaName)
                   .append(DOT)
                   .append(tableName);
        } else {
            snippet.append(type)
                   .append(SPACE)
                   .append(KEYWORD_JOIN)
                   .append(SPACE)
                   .append(tableName);
        }
        if (alias != null) {
            String aliasName = encapsulate(alias);
            snippet.append(SPACE)
                   .append(KEYWORD_AS)
                   .append(SPACE)
                   .append(aliasName);
        }
        snippet.append(SPACE)
               .append(KEYWORD_ON)
               .append(SPACE)
               .append(traverseOn(on));
        this.joins.add(snippet.toString());
        return this;
    }

    /**
     * Where.
     *
     * @param condition the condition
     * @return the select builder
     */
    public SelectBuilder where(String condition) {
        if (logger.isTraceEnabled()) {
            logger.trace("where: " + condition);
        }
        this.wheres.add(OPEN + condition + CLOSE);
        return this;
    }

    /**
     * Order.
     *
     * @param column the column
     * @return the select builder
     */
    public SelectBuilder order(String column) {
        if (logger.isTraceEnabled()) {
            logger.trace("order: " + column);
        }
        String columnName = encapsulate(column);
        return order(columnName, true);
    }

    /**
     * Order.
     *
     * @param column the column
     * @param asc the asc
     * @return the select builder
     */
    public SelectBuilder order(String column, boolean asc) {
        if (logger.isTraceEnabled()) {
            logger.trace("order: " + column + ", asc: " + asc);
        }
        String columnName = encapsulate(column);
        if (asc) {
            this.orders.add(columnName + SPACE + KEYWORD_ASC);
        } else {
            this.orders.add(columnName + SPACE + KEYWORD_DESC);
        }

        return this;
    }

    /**
     * Group.
     *
     * @param column the column
     * @return the select builder
     */
    public SelectBuilder group(String column) {
        if (logger.isTraceEnabled()) {
            logger.trace("group: " + column);
        }
        this.groups.add(column);
        return this;
    }

    /**
     * Limit.
     *
     * @param limit the limit
     * @return the select builder
     */
    public SelectBuilder limit(int limit) {
        if (logger.isTraceEnabled()) {
            logger.trace("limit: " + limit);
        }
        this.limit = limit;
        return this;
    }

    /**
     * Limit.
     *
     * @param limit the limit
     * @return the select builder
     */
    public SelectBuilder limit(Double limit) {
        if (logger.isTraceEnabled()) {
            logger.trace("limit: " + limit);
        }
        return limit(limit.intValue());
    }

    /**
     * Offset.
     *
     * @param offset the offset
     * @return the select builder
     */
    public SelectBuilder offset(int offset) {
        if (logger.isTraceEnabled()) {
            logger.trace("offset: " + offset);
        }
        this.offset = offset;
        return this;
    }

    /**
     * Offset.
     *
     * @param offset the offset
     * @return the select builder
     */
    public SelectBuilder offset(Double offset) {
        if (logger.isTraceEnabled()) {
            logger.trace("offset: " + offset);
        }
        return offset(offset.intValue());
    }

    /**
     * Having.
     *
     * @param having the having
     * @return the select builder
     */
    public SelectBuilder having(String having) {
        if (logger.isTraceEnabled()) {
            logger.trace("having: " + having);
        }
        this.having = having;
        return this;
    }

    /**
     * Union.
     *
     * @param select the select
     * @return the select builder
     */
    public SelectBuilder union(String select) {
        if (logger.isTraceEnabled()) {
            logger.trace("union: " + select);
        }
        this.unions.add(select);
        return this;
    }

    /**
     * Generate.
     *
     * @return the string
     */
    @Override
    public String generate() {
        StringBuilder sql = new StringBuilder();

        // SELECT
        generateSelect(sql);

        // DISTINCT
        generateDistinct(sql);

        // COLUMNS
        generateColumns(sql);

        // TABLES
        generateTables(sql);

        // JOINS
        generateJoins(sql);

        // WHERE
        generateWhere(sql, wheres);

        // GROUP BY
        generateGroupBy(sql);

        // HAVING
        generateHaving(sql);

        // ORDER BY
        generateOrderBy(sql, orders);

        // LIMIT
        generateLimitAndOffset(sql, limit, offset);

        // UNION
        generateUnion(sql);

        // FOR UPDATE
        generateForUpdate(sql);

        String generated = sql.toString();

        if (logger.isTraceEnabled()) {
            logger.trace("generated: " + generated);
        }

        return generated;
    }

    /**
     * Generate union.
     *
     * @param sql the sql
     */
    protected void generateUnion(StringBuilder sql) {
        if (!unions.isEmpty()) {
            sql.append(SPACE)
               .append(KEYWORD_UNION)
               .append(SPACE)
               .append(traverseUnions());
        }
    }

    /**
     * Generate having.
     *
     * @param sql the sql
     */
    protected void generateHaving(StringBuilder sql) {
        if (having != null) {
            sql.append(SPACE)
               .append(KEYWORD_HAVING)
               .append(SPACE)
               .append(traverseHaving(having));
        }
    }

    /**
     * Generate group by.
     *
     * @param sql the sql
     */
    protected void generateGroupBy(StringBuilder sql) {
        if (!groups.isEmpty()) {
            sql.append(SPACE)
               .append(KEYWORD_GROUP_BY)
               .append(SPACE)
               .append(traverseGroups());
        }
    }

    /**
     * Generate joins.
     *
     * @param sql the sql
     */
    protected void generateJoins(StringBuilder sql) {
        if (!joins.isEmpty()) {
            sql.append(SPACE)
               .append(traverseJoins());
        }
    }

    /**
     * Generate tables.
     *
     * @param sql the sql
     */
    protected void generateTables(StringBuilder sql) {
        sql.append(SPACE)
           .append(KEYWORD_FROM)
           .append(SPACE)
           .append(traverseTables());
    }

    /**
     * Generate columns.
     *
     * @param sql the sql
     */
    protected void generateColumns(StringBuilder sql) {
        sql.append(SPACE)
           .append(traverseColumns());
    }

    /**
     * Generate distinct.
     *
     * @param sql the sql
     */
    protected void generateDistinct(StringBuilder sql) {
        if (distinct) {
            sql.append(SPACE)
               .append(KEYWORD_DISTINCT);
        }
    }

    /**
     * Generate for update.
     *
     * @param sql the sql
     */
    protected void generateForUpdate(StringBuilder sql) {
        if (forUpdate) {
            sql.append(SPACE)
               .append(KEYWORD_FOR_UPDATE);
        }
    }

    /**
     * Traverse columns.
     *
     * @return the string
     */
    protected String traverseColumns() {
        if (!this.columns.isEmpty()) {
            StringBuilder snippet = new StringBuilder();
            for (String column : this.columns) {
                String columnName = encapsulate(column);
                snippet.append(columnName)
                       .append(COMMA)
                       .append(SPACE);
            }
            return snippet.substring(0, snippet.length() - 2);
        }
        return STAR;
    }

    /**
     * Traverse tables.
     *
     * @return the string
     */
    protected String traverseTables() {
        StringBuilder snippet = new StringBuilder();
        for (String table : this.tables) {
            String schemaName = encapsulate(schema, true);
            String tableName = encapsulate(table, true);
            if (schema != null) {
                snippet.append(schemaName)
                       .append(DOT)
                       .append(tableName)
                       .append(COMMA)
                       .append(SPACE);
            } else {
                snippet.append(tableName)
                       .append(COMMA)
                       .append(SPACE);
            }
        }
        return snippet.substring(0, snippet.length() - 2);
    }

    /**
     * Traverse joins.
     *
     * @return the string
     */
    protected String traverseJoins() {
        StringBuilder snippet = new StringBuilder();
        for (String join : this.joins) {
            snippet.append(join)
                   .append(COMMA)
                   .append(SPACE);
        }
        return snippet.substring(0, snippet.length() - 2);
    }

    /**
     * Traverse groups.
     *
     * @return the string
     */
    protected String traverseGroups() {
        StringBuilder snippet = new StringBuilder();
        for (String group : this.groups) {
            String groupName = encapsulate(group);
            snippet.append(groupName)
                   .append(COMMA)
                   .append(SPACE);
        }
        return snippet.substring(0, snippet.length() - 2);
    }

    /**
     * Traverse unions.
     *
     * @return the string
     */
    protected String traverseUnions() {
        StringBuilder snippet = new StringBuilder();
        for (String unionSQL : this.unions) {
            snippet.append(unionSQL)
                   .append(SPACE);
        }
        return snippet.substring(0, snippet.length() - 1);
    }

    /**
     * Generate select.
     *
     * @param sql the sql
     */
    protected void generateSelect(StringBuilder sql) {
        sql.append(KEYWORD_SELECT);
    }

    /**
     * Gets the columns.
     *
     * @return the columns
     */
    public List<String> getColumns() {
        return columns;
    }

    /**
     * Gets the tables.
     *
     * @return the tables
     */
    public List<String> getTables() {
        return tables;
    }

    /**
     * Gets the joins.
     *
     * @return the joins
     */
    public List<String> getJoins() {
        return joins;
    }

    /**
     * Gets the wheres.
     *
     * @return the wheres
     */
    public List<String> getWheres() {
        return wheres;
    }

    /**
     * Gets the orders.
     *
     * @return the orders
     */
    public List<String> getOrders() {
        return orders;
    }

    /**
     * Gets the groups.
     *
     * @return the groups
     */
    public List<String> getGroups() {
        return groups;
    }

    /**
     * Gets the unions.
     *
     * @return the unions
     */
    public List<String> getUnions() {
        return unions;
    }

    /**
     * Checks if is distinct.
     *
     * @return true, if is distinct
     */
    public boolean isDistinct() {
        return distinct;
    }

    /**
     * Gets the having.
     *
     * @return the having
     */
    public String getHaving() {
        return having;
    }

    /**
     * Gets the limit.
     *
     * @return the limit
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Gets the offset.
     *
     * @return the offset
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Checks if is for update.
     *
     * @return true, if is for update
     */
    public boolean isForUpdate() {
        return forUpdate;
    }

}
