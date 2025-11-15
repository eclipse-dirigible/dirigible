/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.builders.view;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.AbstractCreateSqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Create View Builder.
 */
public class CreateViewBuilder extends AbstractCreateSqlBuilder {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(CreateViewBuilder.class);
    /** The columns. */
    private final List<String> columns = new ArrayList<String>();
    /** The view. */
    private String view = null;
    /** The select. */
    private String select = null;

    /**
     * Instantiates a new creates the view builder.
     *
     * @param dialect the dialect
     * @param view the view
     */
    public CreateViewBuilder(ISqlDialect dialect, String view) {
        super(dialect);
        this.view = view;
    }

    /**
     * Gets the columns.
     *
     * @return the columns
     */
    protected List<String> getColumns() {
        return columns;
    }

    /**
     * Gets the select.
     *
     * @return the select
     */
    protected String getSelect() {
        return select;
    }

    /**
     * Sets the select.
     *
     * @param select the new select
     */
    protected void setSelect(String select) {
        if (logger.isTraceEnabled()) {
            logger.trace("setSelect: " + select);
        }
        this.select = select;
    }

    /**
     * Column.
     *
     * @param name the name
     * @return the creates the view builder
     */
    public CreateViewBuilder column(String name) {
        if (logger.isTraceEnabled()) {
            logger.trace("column: " + name);
        }
        this.columns.add(name);
        return this;
    }

    /**
     * As select.
     *
     * @param select the select
     * @return the creates the view builder
     */
    public CreateViewBuilder asSelect(String select) {
        if (logger.isTraceEnabled()) {
            logger.trace("asSelect: " + select);
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

        // CREATE
        generateCreate(sql);

        // VIEW
        generateView(sql);

        // COLUMNS
        generateColumns(sql);

        // SELECT
        generateAsSelect(sql);

        String generated = sql.toString();

        logger.trace("generated sql: [{}]", generated);

        return generated;
    }

    /**
     * Generate view.
     *
     * @param sql the sql
     */
    protected void generateView(StringBuilder sql) {
        String viewName = encapsulate(this.getView(), true);
        sql.append(SPACE)
           .append(KEYWORD_VIEW)
           .append(SPACE)
           .append(viewName);
    }

    /**
     * Gets the view.
     *
     * @return the view
     */
    protected String getView() {
        return view;
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
        snippet.append(SPACE);
        for (String column : this.columns) {
            String columnName = encapsulate(column);
            snippet.append(columnName)
                   .append(SPACE);
            snippet.append(COMMA)
                   .append(SPACE);
        }
        return snippet.substring(0, snippet.length() - 2);
    }

    /**
     * Generate as select.
     *
     * @param sql the sql
     */
    protected void generateAsSelect(StringBuilder sql) {
        sql.append(SPACE)
           .append(KEYWORD_AS)
           .append(SPACE)
           .append(this.select);
    }

}
