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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.dirigible.database.sql.DataType;
import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.SqlException;
import org.eclipse.dirigible.database.sql.builders.table.CreateTablePrimaryKeyBuilder;
import org.eclipse.dirigible.database.sql.builders.tableType.CreateTableTypeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class HanaCreateTableTypeBuilder.
 */
public class HanaCreateTableTypeBuilder extends CreateTableTypeBuilder {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(HanaCreateTableTypeBuilder.class);

    /** The Constant ARGS_DATA_TYPES. */
    private static final List<DataType> ARGS_DATA_TYPES = new ArrayList<>(
            List.of(DataType.VARCHAR, DataType.CHAR, DataType.NVARCHAR, DataType.ALPHANUM, DataType.SHORTTEXT, DataType.DECIMAL));

    /** The table type. */
    private final String tableType;

    /** The columns. */
    private final List<String[]> columns = new ArrayList<>();

    /** The primary key. */
    private CreateTablePrimaryKeyBuilder primaryKey;

    /**
     * Instantiates a new hana create table builder.
     *
     * @param dialect the dialect
     * @param tableType the tableType
     */
    public HanaCreateTableTypeBuilder(ISqlDialect dialect, String tableType) {
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

        // CREATE
        generateCreate(sql);

        // TABLE TYPE
        generateTableType(sql);

        sql.append(SPACE)
           .append(OPEN);

        // COLUMNS
        generateStructureColumns(sql);

        // PRIMARY KEY
        generatePrimaryKey(sql);

        sql.append(CLOSE);

        String generated = sql.toString();

        if (logger.isTraceEnabled()) {
            logger.trace("generated: " + generated);
        }

        return generated;
    }

    /**
     * Generate structure columns.
     *
     * @param sql the sql
     */
    private void generateStructureColumns(StringBuilder sql) {
        if (!this.getColumns()
                 .isEmpty()) {
            sql.append(iterateColumns());
        }
    }

    /**
     * Iterate columns.
     *
     * @return the string
     */
    private String iterateColumns() {

        List<String[]> allPrimaryKeys = this.columns.stream()
                                                    .filter(el -> Arrays.stream(el)
                                                                        .anyMatch(x -> x.equals(getDialect().getPrimaryKeyArgument())))
                                                    .collect(Collectors.toList());
        boolean isCompositeKey = allPrimaryKeys.size() > 1;

        StringBuilder snippet = new StringBuilder();
        snippet.append(SPACE);

        for (String[] column : this.columns) {
            boolean isColumnName = true;
            for (String arg : column) {
                if (isColumnName) {
                    String columnName = encapsulate(arg);
                    snippet.append(columnName)
                           .append(SPACE);
                    isColumnName = false;
                    continue;
                }
                if (isCompositeKey && arg.equals(getDialect().getPrimaryKeyArgument())) {
                    continue;
                }
                snippet.append(arg)
                       .append(SPACE);
            }
            snippet.append(COMMA)
                   .append(SPACE);
        }
        return snippet.substring(0, snippet.length() - 2);
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
           .append(tableTypeName)
           .append(SPACE)
           .append(KEYWORD_AS)
           .append(SPACE)
           .append(KEYWORD_TABLE);
    }

    /**
     * Column.
     *
     * @param name the name
     * @param type the type
     * @return the creates the table type builder
     */
    @Override
    public CreateTableTypeBuilder column(String name, DataType type) {
        String[] definition = new String[] {name, getDialect().getDataTypeName(type)};
        this.columns.add(definition);
        return this;
    }

    /**
     * Column.
     *
     * @param name the name
     * @param type the type
     * @param length the length
     * @return the creates the table type builder
     */
    @Override
    public CreateTableTypeBuilder column(String name, DataType type, String length) {
        if (type == DataType.VARCHAR || type == DataType.NVARCHAR || type == DataType.CHAR) {
            String[] definition = new String[] {name, String.valueOf(type), OPEN + length + CLOSE};
            this.columns.add(definition);
        }
        return this;
    }

    /**
     * Column.
     *
     * @param name the name
     * @param type the type
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param args the args
     * @return the creates the table type builder
     */
    @Override
    public CreateTableTypeBuilder column(String name, DataType type, Boolean isPrimaryKey, Boolean isNullable, String args) {
        if (logger.isTraceEnabled()) {
            logger.trace("column: " + name + ", type: " + (type != null ? type.name() : null) + ", isPrimaryKey: " + isPrimaryKey
                    + ", isNullable: " + isNullable + OPEN + args + CLOSE);
        }

        if (type == null) {
            throw new SqlException("The type of the column cannot be null.");
        }

        String[] definition;

        if (ARGS_DATA_TYPES.contains(type)) {
            args = OPEN + args + CLOSE;
            definition = new String[] {name, getDialect().getDataTypeName(type), args};
        } else {
            definition = new String[] {name, getDialect().getDataTypeName(type)};
        }

        if (!isNullable) {
            definition = Stream.of(definition, new String[] {getDialect().getNotNullArgument()})
                               .flatMap(Stream::of)
                               .toArray(String[]::new);
        }
        if (isPrimaryKey) {
            definition = Stream.of(definition, new String[] {getDialect().getPrimaryKeyArgument()})
                               .flatMap(Stream::of)
                               .toArray(String[]::new);
        }

        this.columns.add(definition);
        return this;
    }

    /**
     * Generate primary key.
     *
     * @param sql the sql
     */
    protected void generatePrimaryKey(StringBuilder sql) {
        List<String[]> allPrimaryKeys = this.getColumns()
                                            .stream()
                                            .filter(el -> Arrays.stream(el)
                                                                .anyMatch(x -> x.equals(getDialect().getPrimaryKeyArgument())))
                                            .collect(Collectors.toList());
        boolean isCompositeKey = allPrimaryKeys.size() > 1;

        if ((this.primaryKey != null) && allPrimaryKeys.size() == 0 && !this.primaryKey.getColumns()
                                                                                       .isEmpty()) {
            sql.append(COMMA)
               .append(SPACE);
            if (this.primaryKey.getName() != null) {
                String primaryKeyName = encapsulate(this.primaryKey.getName());
                sql.append(KEYWORD_CONSTRAINT)
                   .append(SPACE)
                   .append(primaryKeyName)
                   .append(SPACE);
            }
            sql.append(KEYWORD_PRIMARY)
               .append(SPACE)
               .append(KEYWORD_KEY)
               .append(SPACE)
               .append(OPEN)
               .append(traverseColumnNames(this.primaryKey.getColumns()))
               .append(CLOSE);
        } else {
            if (isCompositeKey) {
                sql.append(COMMA)
                   .append(SPACE);
                ArrayList<String> keys = new ArrayList<>();
                allPrimaryKeys.forEach(el -> keys.add(el[0]));
                sql.append(KEYWORD_PRIMARY)
                   .append(SPACE)
                   .append(KEYWORD_KEY)
                   .append(OPEN)
                   .append(String.join(" , ", keys))
                   .append(CLOSE)
                   .append(SPACE);
            }
        }
    }

    /**
     * Gets the table type.
     *
     * @return the table type
     */
    public String getTableType() {
        return tableType;
    }

    /**
     * Gets the structure columns.
     *
     * @return the structure columns
     */
    public List<String[]> getColumns() {
        return columns;
    }

    /**
     * Traverse column names.
     *
     * @param names the columns
     * @return the string
     */
    protected String traverseColumnNames(Set<String> names) {
        StringBuilder snippet = new StringBuilder();
        snippet.append(SPACE);
        for (String column : names) {
            String columnName = encapsulate(column);
            snippet.append(columnName)
                   .append(SPACE)
                   .append(COMMA)
                   .append(SPACE);
        }
        return snippet.substring(0, snippet.length() - 2);
    }

}
