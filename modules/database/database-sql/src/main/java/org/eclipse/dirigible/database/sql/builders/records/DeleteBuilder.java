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
import org.eclipse.dirigible.database.sql.builders.AbstractQuerySqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Delete Builder.
 */
public class DeleteBuilder extends AbstractQuerySqlBuilder {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(DeleteBuilder.class);

    /** The table. */
    private String table;

    /** The wheres. */
    private final List<String> wheres = new ArrayList<String>();

    /**
     * Instantiates a new delete builder.
     *
     * @param dialect the dialect
     */
    public DeleteBuilder(ISqlDialect dialect) {
        super(dialect);
    }

    /**
     * From.
     *
     * @param table the table
     * @return the delete builder
     */
    public DeleteBuilder from(String table) {
        if (logger.isTraceEnabled()) {
            logger.trace("from: " + table);
        }
        this.table = table;
        return this;
    }

    /**
     * Where.
     *
     * @param condition the condition
     * @return the delete builder
     */
    public DeleteBuilder where(String condition) {
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
        generateDelete(sql);

        // TABLE
        generateTable(sql);

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
           .append(KEYWORD_FROM)
           .append(SPACE)
           .append(tableName);
    }

    /**
     * Generate delete.
     *
     * @param sql the sql
     */
    protected void generateDelete(StringBuilder sql) {
        sql.append(KEYWORD_DELETE);
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
     * Gets the wheres.
     *
     * @return the wheres
     */
    public List<String> getWheres() {
        return wheres;
    }

}
