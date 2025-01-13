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
import org.eclipse.dirigible.database.sql.builders.tableType.DropTableTypeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class HanaDropTableTypeBuilder.
 */
public class HanaDropTableTypeBuilder extends DropTableTypeBuilder {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(HanaDropTableTypeBuilder.class);

    /** The table type. */
    private final String tableType;

    /**
     * Instantiates a new abstract drop sql builder.
     *
     * @param dialect the dialect
     * @param tableType the table type
     */
    public HanaDropTableTypeBuilder(ISqlDialect dialect, String tableType) {
        super(dialect, tableType);
        this.tableType = tableType;
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

        // TABLE TYPE
        generateTableType(sql);

        String generated = sql.toString();

        if (logger.isTraceEnabled()) {
            logger.trace("generated: " + generated);
        }

        return generated;
    }

    /**
     * Generate table type.
     *
     * @param sql the sql
     */
    private void generateTableType(StringBuilder sql) {
        String tableTypeName = encapsulate(this.getTableType(), true);
        sql.append(SPACE)
           .append(KEYWORD_TABLE_TYPE)
           .append(SPACE)
           .append(tableTypeName);
    }

    /**
     * Gets the table type.
     *
     * @return the table type
     */
    public String getTableType() {
        return tableType;
    }

}
