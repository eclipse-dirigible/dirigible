/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.builders.table;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.AbstractDropSqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Drop Table Builder.
 */
public class DropTableBuilder extends AbstractDropSqlBuilder {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(DropTableBuilder.class);

    /** The table. */
    private String table = null;

    private boolean cascade = false;

    /**
     * Instantiates a new drop table builder.
     *
     * @param dialect the dialect
     * @param table the table
     */
    public DropTableBuilder(ISqlDialect dialect, String table) {
        super(dialect);
        this.table = table;
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

        // TABLE
        generateTable(sql);

        if (cascade) {
            sql.append(SPACE)
               .append(KEYWORD_DATABASE_DROP_CASCADE);
        }

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
           .append(KEYWORD_TABLE)
           .append(SPACE)
           .append(tableName);
    }

    /**
     * Getter for the table.
     *
     * @return the table
     */
    public String getTable() {
        return table;
    }

    public DropTableBuilder cascade(boolean cascade) {
        this.cascade = cascade;
        return this;
    }

}
