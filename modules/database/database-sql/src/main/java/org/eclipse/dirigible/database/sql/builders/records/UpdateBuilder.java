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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.AbstractQuerySqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Update Builder.
 */
public class UpdateBuilder extends AbstractQuerySqlBuilder {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(UpdateBuilder.class);

    /** The table. */
    private String table;

    /** The values. */
    private final Map<String, String> values = new LinkedHashMap<String, String>();

    /** The wheres. */
    private final List<String> wheres = new ArrayList<String>();

    /**
     * Instantiates a new update builder.
     *
     * @param dialect the dialect
     */
    public UpdateBuilder(ISqlDialect dialect) {
        super(dialect);
    }

    /**
     * Table.
     *
     * @param table the table
     * @return the update builder
     */
    public UpdateBuilder table(String table) {
        if (logger.isTraceEnabled()) {
            logger.trace("table: " + table);
        }
        this.table = table;
        return this;
    }

    /**
     * Sets the.
     *
     * @param column the column
     * @param value the value
     * @return the update builder
     */
    public UpdateBuilder set(String column, String value) {
        if (logger.isTraceEnabled()) {
            logger.trace("set: " + column + ", value: " + value);
        }
        values.put(column, value);
        return this;
    }

    /**
     * Where.
     *
     * @param condition the condition
     * @return the update builder
     */
    public UpdateBuilder where(String condition) {
        if (logger.isTraceEnabled()) {
            logger.trace("where: " + condition);
        }
        wheres.add(OPEN + condition + CLOSE);
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

        // UPDATE
        generateUpdate(sql);

        // TABLE
        generateTable(sql);

        // SET
        generateSetValues(sql);

        // WHERE
        generateWhere(sql, wheres);

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
           .append(tableName);
    }

    /**
     * Generate set values.
     *
     * @param sql the sql
     */
    protected void generateSetValues(StringBuilder sql) {
        sql.append(SPACE)
           .append(KEYWORD_SET);
        for (Entry<String, String> next : values.entrySet()) {
            String columnName = encapsulate(next.getKey());
            sql.append(SPACE)
               .append(columnName)
               .append(SPACE)
               .append(EQUALS)
               .append(SPACE)
               .append(next.getValue())
               .append(COMMA);
        }
        if (values.entrySet()
                  .size() > 0) {
            sql.delete(sql.length() - 1, sql.length());
        }
    }

    /**
     * Generate update.
     *
     * @param sql the sql
     */
    protected void generateUpdate(StringBuilder sql) {
        sql.append(KEYWORD_UPDATE);
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
     * Gets the values.
     *
     * @return the values
     */
    public Map<String, String> getValues() {
        return values;
    }

    /**
     * Gets the wheres.
     *
     * @return the wheres
     */
    public List<String> getWheres() {
        return wheres;
    }

}
