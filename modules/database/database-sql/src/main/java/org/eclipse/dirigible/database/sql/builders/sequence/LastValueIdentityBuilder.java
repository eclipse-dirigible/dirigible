/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.builders.sequence;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.AbstractQuerySqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Last Value Identity Builder.
 */
public class LastValueIdentityBuilder extends AbstractQuerySqlBuilder {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(LastValueIdentityBuilder.class);

    /**
     * Instantiates a new last value identity builder.
     *
     * @param dialect the dialect
     */
    public LastValueIdentityBuilder(ISqlDialect dialect) {
        super(dialect);
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

        // LAST VALUE
        generateLastValue(sql);

        String generated = sql.toString();

        if (logger.isTraceEnabled()) {
            logger.trace("generated: " + generated);
        }

        return generated;
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
     * Generate next value.
     *
     * @param sql the sql
     */
    protected void generateLastValue(StringBuilder sql) {
        sql.append(SPACE)
           .append(KEYWORD_IDENTITY);
    }

}
