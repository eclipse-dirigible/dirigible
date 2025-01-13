/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.hana;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.table.CreateTemporaryTableBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class HanaCreateTemporaryTableBuilder.
 */
public class HanaCreateTemporaryTableBuilder extends CreateTemporaryTableBuilder<HanaCreateTemporaryTableBuilder> {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(CreateTemporaryTableBuilder.class);

    /** The Constant NO_LIKE_TABLE_OR_AS_SELECT_QUERY_SPECIFIED. */
    private static final String NO_LIKE_TABLE_OR_AS_SELECT_QUERY_SPECIFIED = "No `like` table or `as` select query specified.";

    /**
     * Instantiates a new abstract sql builder.
     *
     * @param dialect the dialect
     * @param table the table
     */
    protected HanaCreateTemporaryTableBuilder(ISqlDialect dialect, String table) {
        super(dialect, table);
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

        sql.append(SPACE)
           .append(METADATA_LOCAL_TEMPORARY);

        // TABLE
        generateTable(sql);

        sql.append(SPACE);
        if (this.likeTable != null) {
            // LIKE table
            sql.append(KEYWORD_LIKE)
               .append(SPACE)
               .append(this.likeTable);
            appendWithNoDataKeywords(sql);
        } else if (this.asSelectQuery != null) {
            // AS select query
            sql.append(KEYWORD_AS)
               .append(SPACE)
               .append(OPEN)
               .append(this.asSelectQuery)
               .append(CLOSE);
            if (this.selectWithNoData) {
                appendWithNoDataKeywords(sql);
            }
        } else {
            throw new IllegalStateException(NO_LIKE_TABLE_OR_AS_SELECT_QUERY_SPECIFIED);
        }

        String generated = sql.toString();

        if (logger.isTraceEnabled()) {
            logger.trace("generated: " + generated);
        }

        return generated;
    }

    /**
     * Append with no data keywords.
     *
     * @param sql the sql
     */
    private void appendWithNoDataKeywords(StringBuilder sql) {
        sql.append(SPACE)
           .append(KEYWORD_WITH)
           .append(SPACE)
           .append(KEYWORD_NO)
           .append(SPACE)
           .append(KEYWORD_DATA);
    }
}
