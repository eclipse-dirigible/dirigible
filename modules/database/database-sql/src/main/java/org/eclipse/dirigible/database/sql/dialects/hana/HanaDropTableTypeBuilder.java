/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: 2021 SAP SE or an SAP affiliate company and Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.hana;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.tableType.DropTableTypeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HanaDropTableTypeBuilder extends DropTableTypeBuilder {

    private static final Logger logger = LoggerFactory.getLogger(HanaDropTableTypeBuilder.class);

    private String tableType;

    /**
     * Instantiates a new abstract drop sql builder.
     *
     * @param dialect   the dialect
     * @param tableType
     */
    public HanaDropTableTypeBuilder(ISqlDialect dialect, String tableType) {
        super(dialect, tableType);
        this.tableType=tableType;
    }

    @Override
    public String generate() {

        StringBuilder sql = new StringBuilder();

        // DROP
        generateDrop(sql);

        // TABLE TYPE
        generateTableType(sql);

        String generated = sql.toString();

        logger.trace("generated: " + generated);

        return generated;
    }

    /**
     * Generate table type.
     *
     * @param sql
     *            the sql
     */
    private void generateTableType(StringBuilder sql) {
        String tableTypeName = (isCaseSensitive()) ? encapsulate(this.getTableType()) : this.getTableType();
        sql.append(SPACE).append(KEYWORD_TABLE_TYPE).append(SPACE).append(tableTypeName);
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
