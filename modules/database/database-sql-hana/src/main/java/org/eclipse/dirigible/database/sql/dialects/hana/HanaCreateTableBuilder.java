/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.hana;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.table.CreateTableBuilder;
import org.eclipse.dirigible.database.sql.builders.table.CreateTableIndexBuilder;
import org.eclipse.dirigible.database.sql.builders.table.CreateTableUniqueIndexBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Set;

/**
 * The HANA Create Table Builder.
 */
public class HanaCreateTableBuilder extends CreateTableBuilder<HanaCreateTableBuilder> {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(HanaCreateTableBuilder.class);

    /** The table type. */
    private String tableType = "";

    /**
     * Instantiates a new hana create table builder.
     *
     * @param dialect the dialect
     * @param table the table
     * @param tableType the table type
     */

    public HanaCreateTableBuilder(ISqlDialect dialect, String table, String tableType) {
        super(dialect, table);
        this.tableType = tableType;
    }

    /**
     * Generate table.
     *
     * @param sql the sql
     */
    @Override
    protected void generateTable(StringBuilder sql) {
        String tableName = encapsulate(this.getTable(), true);
        String tableType = "";

        if (this.tableType.equalsIgnoreCase(KEYWORD_COLUMN) || this.tableType.equalsIgnoreCase(KEYWORD_COLUMNSTORE)) {
            tableType = KEYWORD_COLUMN;
        } else if (this.tableType.equalsIgnoreCase(KEYWORD_ROW) || this.tableType.equalsIgnoreCase(KEYWORD_ROWSTORE)) {
            tableType = KEYWORD_ROW;
        } else if (this.tableType.equalsIgnoreCase(KEYWORD_GLOBAL_TEMPORARY)) {
            tableType = METADATA_GLOBAL_TEMPORARY;
        } else if (this.tableType.equalsIgnoreCase(KEYWORD_GLOBAL_TEMPORARY_COLUMN)) {
            tableType = METADATA_GLOBAL_TEMPORARY_COLUMN;
        }

        sql.append(SPACE)
           .append(tableType)
           .append(SPACE)
           .append(KEYWORD_TABLE)
           .append(SPACE)
           .append(generateSchema())
           .append(tableName);
    }

    /**
     * Unique.
     *
     * @param name the name
     * @param columns the columns
     * @param type the type
     * @param order the order
     * @return the creates the table builder
     */
    @Override
    public HanaCreateTableBuilder unique(String name, String[] columns, String type, String order) {
        if (logger.isTraceEnabled()) {
            logger.trace("unique: " + name + ", columns" + Arrays.toString(columns) + ", indexType " + type + ", order " + order);
        }
        CreateTableUniqueIndexBuilder uniqueIndex = new CreateTableUniqueIndexBuilder(this.getDialect(), name);
        for (String column : columns) {
            uniqueIndex.column(column);
        }
        uniqueIndex.setIndexType(type);
        uniqueIndex.setOrder(order);
        this.uniqueIndices.add(uniqueIndex);
        return this;

    }

    /**
     * Index.
     *
     * @param name the name
     * @param isUnique the isUnique
     * @param order the order
     * @param indexType the indexType
     * @param indexColumns the indexColumns
     * @return the creates the table builder
     */
    public HanaCreateTableBuilder index(String name, Boolean isUnique, String order, String indexType, Set<String> indexColumns) {

        if (logger.isTraceEnabled()) {
            logger.trace("index: " + name + ", columns" + indexColumns);
        }
        CreateTableIndexBuilder index = new CreateTableIndexBuilder(this.getDialect(), name);
        index.setIndexType(indexType);
        index.setOrder(order);
        index.setColumns(indexColumns);
        index.setUnique(isUnique);
        this.indices.add(index);

        return this;
    }
}
