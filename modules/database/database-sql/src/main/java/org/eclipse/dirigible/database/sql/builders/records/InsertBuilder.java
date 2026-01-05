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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.AbstractSqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Insert Builder.
 */
public class InsertBuilder extends AbstractSqlBuilder {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(InsertBuilder.class);
    /** The columns. */
    private final List<String> columns = new ArrayList<String>();
    /** The values. */
    private final List<String> values = new ArrayList<String>();
    /** The table. */
    private String table = null;
    /** The select. */
    private String select = null;

    /**
     * Instantiates a new insert builder.
     *
     * @param dialect the dialect
     */
    public InsertBuilder(ISqlDialect dialect) {
        super(dialect);
    }

    /**
     * Into.
     *
     * @param table the table
     * @return the insert builder
     */
    public InsertBuilder into(String table) {
        if (logger.isTraceEnabled()) {
            logger.trace("into: " + table);
        }
        this.table = table;
        return this;
    }

    /**
     * Column.
     *
     * @param name the name
     * @return the insert builder
     */
    public InsertBuilder column(String name) {
        if (logger.isTraceEnabled()) {
            logger.trace("column: " + name);
        }
        this.columns.add(name);
        return this;
    }

    /**
     * Value.
     *
     * @param value the value
     * @return the insert builder
     */
    public InsertBuilder value(String value) {
        if (logger.isTraceEnabled()) {
            logger.trace("value: " + value);
        }
        this.values.add(value);
        return this;
    }

    /**
     * Select.
     *
     * @param select the select
     * @return the insert builder
     */
    public InsertBuilder select(String select) {
        if (logger.isTraceEnabled()) {
            logger.trace("select: " + select);
        }
        this.select = select;
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

        // INSERT
        generateInsert(sql);

        // TABLE
        generateTable(sql);

        // COLUMNS
        generateColumns(sql);

        // VALUES
        generateValues(sql);

        // SELECT
        generateSelect(sql);

        String generated = sql.toString();

        if (logger.isTraceEnabled()) {
            logger.trace("generated: " + generated);
        }

        return generated;
    }

    /**
     * Generate table.
     *
     * @param sql the sql
     */
    protected void generateTable(StringBuilder sql) {
        String tableName = encapsulate(this.getTable(), true);
        sql.append(SPACE)
           .append(KEYWORD_INTO)
           .append(SPACE)
           .append(tableName);
    }

    /**
     * Gets the table.
     *
     * @return the table
     */
    public String getTable() {
        return table;
    }

    /**
     * Generate columns.
     *
     * @param sql the sql
     */
    protected void generateColumns(StringBuilder sql) {
        if (!this.columns.isEmpty()) {
            sql.append(SPACE)
               .append(OPEN)
               .append(traverseColumns())
               .append(CLOSE);
        }
    }

    /**
     * Traverse columns.
     *
     * @return the string
     */
    protected String traverseColumns() {
        StringBuilder snippet = new StringBuilder();
        for (String column : this.columns) {
            String columnName = encapsulate(column, true);
            snippet.append(columnName)
                   .append(COMMA)
                   .append(SPACE);
        }
        return snippet.substring(0, snippet.length() - 2);
    }

    /**
     * Generate values.
     *
     * @param sql the sql
     */
    protected void generateValues(StringBuilder sql) {
        if (!this.values.isEmpty()) {
            sql.append(SPACE)
               .append(KEYWORD_VALUES)
               .append(SPACE)
               .append(OPEN)
               .append(traverseValues())
               .append(CLOSE);
        } else if (!this.columns.isEmpty() && (this.select == null)) {
            sql.append(SPACE)
               .append(KEYWORD_VALUES)
               .append(SPACE)
               .append(OPEN)
               .append(enumerateValues())
               .append(CLOSE);
        }
    }

    /**
     * Traverse values.
     *
     * @return the string
     */
    protected String traverseValues() {
        StringBuilder snippet = new StringBuilder();
        for (String value : this.values) {
            snippet.append(value)
                   .append(COMMA)
                   .append(SPACE);
        }
        return snippet.substring(0, snippet.length() - 2);
    }

    /**
     * Enumerate values.
     *
     * @return the string
     */
    protected String enumerateValues() {
        StringBuilder snippet = new StringBuilder();
        for (int i = 0; i < columns.size(); i++) {
            snippet.append(QUESTION)
                   .append(COMMA)
                   .append(SPACE);
        }
        return snippet.substring(0, snippet.length() - 2);
    }

    /**
     * Generate select.
     *
     * @param sql the sql
     */
    protected void generateSelect(StringBuilder sql) {
        if (this.select != null) {
            sql.append(SPACE)
               .append(this.select);
        }
    }

    /**
     * Generate insert.
     *
     * @param sql the sql
     */
    protected void generateInsert(StringBuilder sql) {
        sql.append(KEYWORD_INSERT);
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
     * Gets the values.
     *
     * @return the values
     */
    public List<String> getValues() {
        return values;
    }

    /**
     * Gets the select.
     *
     * @return the select
     */
    public String getSelect() {
        return select;
    }

}
