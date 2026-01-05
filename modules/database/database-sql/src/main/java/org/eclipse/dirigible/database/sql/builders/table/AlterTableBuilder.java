/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.builders.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Create Table Builder.
 */
public class AlterTableBuilder extends AbstractTableBuilder<AlterTableBuilder> {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(AlterTableBuilder.class);

    /** The action. */
    private String action = null;

    /** The foreign keys. */
    private final List<CreateTableForeignKeyBuilder> foreignKeys = new ArrayList<CreateTableForeignKeyBuilder>();

    /** The unique indices. */
    private final List<CreateTableUniqueIndexBuilder> uniqueIndices = new ArrayList<CreateTableUniqueIndexBuilder>();

    /**
     * Instantiates a new creates the table builder.
     *
     * @param dialect the dialect
     * @param table the table
     */
    public AlterTableBuilder(ISqlDialect dialect, String table) {
        super(dialect, table);
    }

    /**
     * Adds the.
     *
     * @return the alter table builder
     */
    public AlterTableBuilder add() {
        this.action = KEYWORD_ADD;
        return this;
    }

    /**
     * Alter.
     *
     * @return the alter table builder
     */
    public AlterTableBuilder alter() {
        this.action = KEYWORD_ALTER;
        return this;
    }

    /**
     * Drop.
     *
     * @return the alter table builder
     */
    public AlterTableBuilder drop() {
        this.action = KEYWORD_DROP;
        return this;
    }

    /**
     * Gets the action.
     *
     * @return the action
     */
    protected String getAction() {
        return action;
    }

    /**
     * Gets the foreignKeys list.
     *
     * @return the foreignKeys
     */
    public List<CreateTableForeignKeyBuilder> getForeignKeys() {
        return foreignKeys;
    }

    /**
     * Gets the unique indices.
     *
     * @return the unique indices
     */
    public List<CreateTableUniqueIndexBuilder> getUniqueIndices() {
        return uniqueIndices;
    }

    /**
     * Foreign Key.
     *
     * @param name the name of the foreign key
     * @param columns the local columns
     * @param referencedTable the reference table
     * @param referencedColumns the referenced columns
     * @return the AlterTableBuilder object
     */
    public AlterTableBuilder foreignKey(String name, String[] columns, String referencedTable, String[] referencedColumns) {
        if (logger.isTraceEnabled()) {
            logger.trace("foreignKey: " + name + ", columns" + Arrays.toString(columns) + ", referencedTable: " + referencedTable
                    + ", referencedColumns: " + Arrays.toString(referencedColumns));
        }
        CreateTableForeignKeyBuilder foreignKey = new CreateTableForeignKeyBuilder(this.getDialect(), name);
        for (String column : columns) {
            foreignKey.column(column);
        }
        foreignKey.referencedTable(referencedTable);
        for (String column : referencedColumns) {
            foreignKey.referencedColumn(column);
        }
        this.foreignKeys.add(foreignKey);
        return this;
    }

    /**
     * Unique.
     *
     * @param name the name
     * @param columns the columns
     * @return the alter table builder
     */
    @Override
    public AlterTableBuilder unique(String name, String[] columns) {
        if (logger.isTraceEnabled()) {
            logger.trace("unique: " + name + ", columns" + Arrays.toString(columns));
        }
        CreateTableUniqueIndexBuilder uniqueIndex = new CreateTableUniqueIndexBuilder(this.getDialect(), name);
        for (String column : columns) {
            uniqueIndex.column(column);
        }
        this.uniqueIndices.add(uniqueIndex);
        return this;
    }

    /**
     * Unique.
     *
     * @param name the name
     * @param columns the columns
     * @param type the type
     * @param order the order
     * @return the alter table builder
     */
    @Override
    public AlterTableBuilder unique(String name, String[] columns, String type, String order) {
        return unique(name, columns);
    }

    /**
     * Generate.
     *
     * @return the string
     */
    @Override
    public String generate() {

        StringBuilder sql = new StringBuilder();

        // ALTER
        generateAlter(sql);

        // TABLE
        generateTable(sql);

        sql.append(SPACE);

        if (KEYWORD_ADD.equals(this.action)) {
            sql.append(KEYWORD_ADD);
            if (!getColumns().isEmpty()) {
                // COLUMNS
                generateColumns(sql);
            }
            if (!getForeignKeys().isEmpty()) {
                // FOREIGN KEYS
                generateForeignKeyNames(sql);
            }
            if (!getUniqueIndices().isEmpty()) {
                generateUniqueIndices(sql);
            }
        } else if (KEYWORD_DROP.equals(this.action)) {
            if (!getColumns().isEmpty()) {
                // COLUMNS
                generateColumnNamesForDrop(sql);
            }
            if (!getForeignKeys().isEmpty()) {
                // FOREIGN KEYS
                generateForeignKeyNamesForDrop(sql);
            }
            if (!getUniqueIndices().isEmpty()) {
                generateUniqueIndicesForDrop(sql);
            }
        } else {
            if (!getColumns().isEmpty()) {
                // COLUMNS
                sql.append(KEYWORD_ALTER);
                generateColumnsForAlter(sql);
            }
        }

        String generated = sql.append(SEMICOLON)
                              .toString()
                              .trim();

        if (logger.isTraceEnabled()) {
            logger.trace("generated: " + generated);
        }

        return generated;
    }

    /**
     * Generate foreign key names for drop.
     *
     * @param sql the sql
     */
    private void generateForeignKeyNamesForDrop(StringBuilder sql) {
        if (!this.getForeignKeys()
                 .isEmpty()) {
            sql.append(KEYWORD_DROP)
               .append(SPACE)
               .append(KEYWORD_CONSTRAINT)
               .append(SPACE);
            this.getForeignKeys()
                .forEach(fk -> sql.append(fk.getName() + ", "));
            sql.delete(sql.length() - 2, sql.length());
        }
    }

    /**
     * Generate unique indices for drop.
     *
     * @param sql the sql
     */
    private void generateUniqueIndicesForDrop(StringBuilder sql) {
        if (!this.getUniqueIndices()
                 .isEmpty()) {
            sql.append(KEYWORD_DROP)
               .append(SPACE)
               .append(KEYWORD_CONSTRAINT)
               .append(SPACE);
            this.getUniqueIndices()
                .forEach(ui -> sql.append(ui.getName() + ", "));
            sql.delete(sql.length() - 2, sql.length());
        }
    }

    /**
     * Generate foreign key names.
     *
     * @param sql the sql
     */
    protected void generateForeignKeyNames(StringBuilder sql) {
        StringBuilder snippet = new StringBuilder();
        for (CreateTableForeignKeyBuilder foreignKey : this.getForeignKeys()) {
            snippet.append(SPACE)
                   .append(KEYWORD_CONSTRAINT)
                   .append(SPACE)
                   .append(encapsulate(foreignKey.getName()))
                   .append(SPACE)
                   .append(KEYWORD_FOREIGN)
                   .append(SPACE)
                   .append(KEYWORD_KEY)
                   .append(SPACE)
                   .append(OPEN)
                   .append(traverseNames(foreignKey.getColumns()))
                   .append(CLOSE)
                   .append(SPACE)
                   .append(KEYWORD_REFERENCES)
                   .append(SPACE)
                   .append(encapsulate(foreignKey.getReferencedTable()))
                   .append(SPACE)
                   .append(OPEN)
                   .append(traverseNames(foreignKey.getReferencedColumns()))
                   .append(CLOSE)
                   .append(COMMA)
                   .append(SPACE);
        }
        sql.append(snippet.substring(0, snippet.length() - 2));
    }

    /**
     * Generate unique indices.
     *
     * @param sql the sql
     */
    protected void generateUniqueIndices(StringBuilder sql) {
        for (CreateTableUniqueIndexBuilder uniqueIndex : this.uniqueIndices) {
            generateUniqueIndex(sql, uniqueIndex);
            sql.append(COMMA);
        }
    }

    /**
     * Generate unique index.
     *
     * @param sql the sql
     * @param uniqueIndex the unique index
     */
    protected void generateUniqueIndex(StringBuilder sql, CreateTableUniqueIndexBuilder uniqueIndex) {
        if (uniqueIndex != null) {
            if (uniqueIndex.getName() != null) {
                String uniqueIndexName = encapsulate(uniqueIndex.getName());
                sql.append(KEYWORD_CONSTRAINT)
                   .append(SPACE)
                   .append(uniqueIndexName)
                   .append(SPACE);
            }
            sql.append(KEYWORD_UNIQUE)
               .append(SPACE)
               .append(OPEN)
               .append(traverseNames(uniqueIndex.getColumns()))
               .append(CLOSE);
        }
    }

}
