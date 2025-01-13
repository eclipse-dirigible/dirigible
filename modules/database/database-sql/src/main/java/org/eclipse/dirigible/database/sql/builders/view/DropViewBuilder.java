/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.builders.view;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.AbstractDropSqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Drop View Builder.
 */
public class DropViewBuilder extends AbstractDropSqlBuilder {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(DropViewBuilder.class);

    /** The view. */
    private final String view;
    private final String schema;

    /**
     * Instantiates a new drop view builder.
     *
     * @param dialect the dialect
     * @param view the view
     */
    public DropViewBuilder(ISqlDialect dialect, String schema, String view) {
        super(dialect);
        this.schema = schema;
        this.view = view;
    }

    /**
     * Generate.
     *
     * @return the string
     */
    @Override
    public String generate() {

        StringBuilder sql = new StringBuilder();

        // DROP
        generateDrop(sql);

        // VIEW
        generateView(sql);

        String generated = sql.toString();

        if (logger.isTraceEnabled()) {
            logger.trace("generated: " + generated);
        }

        return generated;
    }

    /**
     * Generate view.
     *
     * @param sql the sql
     */
    protected void generateView(StringBuilder sql) {
        sql.append(SPACE)
           .append(KEYWORD_VIEW)
           .append(SPACE);
        if (schema != null) {
            String schemaName = encapsulate(this.getView(), true);
            sql.append(schemaName)
               .append(".");
        }
        String viewName = encapsulate(this.getView(), true);
        sql.append(viewName);
    }

    /**
     * Getter for the view.
     *
     * @return the view
     */
    public String getView() {
        return view;
    }

}
