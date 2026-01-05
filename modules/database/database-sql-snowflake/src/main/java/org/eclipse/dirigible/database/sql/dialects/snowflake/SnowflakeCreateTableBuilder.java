/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.snowflake;

import java.util.Arrays;
import java.util.stream.Stream;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.database.sql.DataType;
import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.table.CreateTableBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class SnowflakeCreateTableBuilder.
 */
public class SnowflakeCreateTableBuilder extends CreateTableBuilder<SnowflakeCreateTableBuilder> {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(SnowflakeCreateTableBuilder.class);

    // empty type means permanent as described here
    // https://docs.snowflake.com/en/sql-reference/sql/create-table
    private static final String DEFAULT_TABLE_TYPE = "";

    /** The table type. */
    private final String tableType;

    public SnowflakeCreateTableBuilder(ISqlDialect dialect, String table) {
        this(dialect, table, DEFAULT_TABLE_TYPE);
    }

    /**
     * Instantiates a new h 2 create table builder.
     *
     * @param dialect the dialect
     * @param table the table
     */
    public SnowflakeCreateTableBuilder(ISqlDialect dialect, String table, String tableType) {
        super(dialect, table);
        this.tableType = tableType;
    }

    /**
     * Column.
     *
     * @param name the name
     * @param type the type
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param isIdentity the is identity
     * @param isFuzzyIndexEnabled the is fuzzy index enabled
     * @param args the args
     * @return the h 2 create table builder
     */
    public SnowflakeCreateTableBuilder column(String name, DataType type, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique,
            Boolean isAutoincrement, Boolean isIdentity, Boolean isFuzzyIndexEnabled, String... args) {
        if (logger.isTraceEnabled()) {
            logger.trace("column: " + name + ", type: " + (type != null ? type.name() : null) + ", isPrimaryKey: " + isPrimaryKey
                    + ", isNullable: " + isNullable + ", isUnique: " + isUnique + ", isIdentity: " + isIdentity + ", args: "
                    + Arrays.toString(args));
        }
        String[] definition;
        if (isIdentity) {
            definition = new String[] {name};
        } else {
            definition = new String[] {name, getDialect().getDataTypeName(type)};
        }
        String[] column;
        if (isIdentity) {
            column = Stream.of(definition, args, new String[] {getDialect().getIdentityArgument()})
                           .flatMap(Stream::of)
                           .toArray(String[]::new);
        } else {
            column = Stream.of(definition, args)
                           .flatMap(Stream::of)
                           .toArray(String[]::new);
        }
        if (!isNullable) {
            column = Stream.of(column, new String[] {getDialect().getNotNullArgument()})
                           .flatMap(Stream::of)
                           .toArray(String[]::new);
        }
        if (isAutoincrement) {
            column = Stream.of(column, new String[] {getDialect().getAutoincrementArgument()})
                           .flatMap(Stream::of)
                           .toArray(String[]::new);
        }
        if (isPrimaryKey) {
            column = Stream.of(column, new String[] {getDialect().getPrimaryKeyArgument()})
                           .flatMap(Stream::of)
                           .toArray(String[]::new);
        }
        if (isUnique && !isPrimaryKey) {
            column = Stream.of(column, new String[] {getDialect().getUniqueArgument()})
                           .flatMap(Stream::of)
                           .toArray(String[]::new);
        }
        if (isFuzzyIndexEnabled) {
            column = Stream.of(column, new String[] {getDialect().getFuzzySearchIndex()})
                           .flatMap(Stream::of)
                           .toArray(String[]::new);
        }

        this.columns.add(column);
        return this;
    }

    /**
     * Generate table.
     *
     * @param sql the sql
     */
    @Override
    protected void generateTable(StringBuilder sql) {
        String tableName = encapsulate(this.getTable(), true);
        String determinedTableType = Configuration.get("SNOWFLAKE_DEFAULT_TABLE_TYPE", DEFAULT_TABLE_TYPE);

        if (this.tableType.equalsIgnoreCase(KEYWORD_HYBRID)) {
            determinedTableType = KEYWORD_HYBRID;
        } else if (this.tableType.equalsIgnoreCase(KEYWORD_DYNAMIC)) {
            determinedTableType = KEYWORD_DYNAMIC;
        } else if (this.tableType.equalsIgnoreCase(KEYWORD_EVENT)) {
            determinedTableType = KEYWORD_EVENT;
        } else if (this.tableType.equalsIgnoreCase(KEYWORD_EXTERNAL)) {
            determinedTableType = KEYWORD_EXTERNAL;
        } else if (this.tableType.equalsIgnoreCase(KEYWORD_ICEBERG)) {
            determinedTableType = KEYWORD_ICEBERG;
        }

        sql.append(SPACE)
           .append(determinedTableType)
           .append(SPACE)
           .append(KEYWORD_TABLE)
           .append(SPACE)
           .append(generateSchema())
           .append(tableName);
    }

}
