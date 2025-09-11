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

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.SqlException;
import org.eclipse.dirigible.database.sql.TableStatements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * The Create Table Builder.
 *
 * @param <TABLE_BUILDER> the generic type
 */
public class CreateTableBuilder<TABLE_BUILDER extends CreateTableBuilder> extends AbstractTableBuilder<TABLE_BUILDER> {

    /** The Constant DELIMITER. */
    public static final String STATEMENT_DELIMITER = "; ";
    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(CreateTableBuilder.class);
    /** The foreign keys. */
    protected final List<CreateTableForeignKeyBuilder> foreignKeys = new ArrayList<>();

    /** The unique indices. */
    protected final List<CreateTableUniqueIndexBuilder> uniqueIndices = new ArrayList<>();

    /** The checks. */
    protected final List<CreateTableCheckBuilder> checks = new ArrayList<>();

    /** The indices. */
    protected final List<CreateTableIndexBuilder> indices = new ArrayList<>();
    /** The primary key. */
    protected CreateTablePrimaryKeyBuilder primaryKey;

    /**
     * Instantiates a new creates the table builder.
     *
     * @param dialect the dialect
     * @param table the table
     */
    public CreateTableBuilder(ISqlDialect dialect, String table) {
        super(dialect, table);
    }

    /**
     * Primary key.
     *
     * @param name the name
     * @param columns the columns
     * @return the creates the table builder
     */
    public TABLE_BUILDER primaryKey(String name, String columns) {
        if (logger.isTraceEnabled()) {
            logger.trace("primaryKey: " + name + ", columns" + columns);
        }
        String[] array = splitValues(columns);
        return primaryKey(name, array);
    }

    /**
     * Primary key.
     *
     * @param name the name
     * @param columns the columns
     * @return the creates the table builder
     */
    public TABLE_BUILDER primaryKey(String name, String[] columns) {
        if (logger.isTraceEnabled()) {
            logger.trace("primaryKey: " + name + ", columns" + Arrays.toString(columns));
        }
        if (this.primaryKey != null) {
            throw new SqlException("Setting of primary key must be called only once");
        }
        this.primaryKey = new CreateTablePrimaryKeyBuilder(this.getDialect(), name);
        for (String column : columns) {
            this.primaryKey.column(column);
        }
        return (TABLE_BUILDER) this;
    }

    /**
     * Primary key.
     *
     * @param columns the columns
     * @return the creates the table builder
     */
    public TABLE_BUILDER primaryKey(String[] columns) {
        if (logger.isTraceEnabled()) {
            logger.trace("primaryKey: <unnamed>, columns" + Arrays.toString(columns));
        }
        return primaryKey(null, columns);
    }

    /**
     * Primary key.
     *
     * @param columns the columns
     * @return the creates the table builder
     */
    public TABLE_BUILDER primaryKey(String columns) {
        if (logger.isTraceEnabled()) {
            logger.trace("primaryKey: <unnamed>, columns" + columns);
        }
        return primaryKey(null, splitValues(columns));
    }

    /**
     * Foreign key.
     *
     * @param name the name
     * @param columns the columns
     * @param referencedTable the referenced table
     * @param referencedColumns the referenced columns
     * @return the creates the table builder
     */
    public TABLE_BUILDER foreignKey(String name, String[] columns, String referencedTable, String[] referencedColumns) {
        return foreignKey(name, columns, referencedTable, null, referencedColumns);
    }

    /**
     * Foreign key.
     *
     * @param name the name
     * @param columns the columns
     * @param referencedTable the referenced table
     * @param referencedTableSchema the referenced table schema
     * @param referencedColumns the referenced columns
     * @return the table builder
     */
    public TABLE_BUILDER foreignKey(String name, String[] columns, String referencedTable, String referencedTableSchema,
            String[] referencedColumns) {
        if (logger.isTraceEnabled()) {
            logger.trace("foreignKey: " + name + ", columns" + Arrays.toString(columns) + ", referencedTable: " + referencedTable
                    + ", referencedTableSchema: " + referencedTableSchema + ", referencedColumns: " + Arrays.toString(referencedColumns));
        }
        CreateTableForeignKeyBuilder foreignKey = new CreateTableForeignKeyBuilder(this.getDialect(), name);
        for (String column : columns) {
            foreignKey.column(column);
        }
        foreignKey.referencedTable(referencedTable);
        for (String column : referencedColumns) {
            foreignKey.referencedColumn(column);
        }
        foreignKey.referencedTableSchema(referencedTableSchema);
        this.foreignKeys.add(foreignKey);
        return (TABLE_BUILDER) this;
    }

    /**
     * Foreign key.
     *
     * @param name the name
     * @param columns the columns
     * @param referencedTable the referenced table
     * @param referencedTableSchema the referenced table schema
     * @param referencedColumns the referenced columns
     * @return the creates the table builder
     */
    public TABLE_BUILDER foreignKey(String name, String columns, String referencedTable, String referencedTableSchema,
            String referencedColumns) {
        if (logger.isTraceEnabled()) {
            logger.trace("foreignKey: " + name + ", columns" + columns + ", referencedTable: " + referencedTable + ", referencedColumns: "
                    + referencedColumns);
        }
        return foreignKey(name, splitValues(columns), referencedTable, referencedTableSchema, splitValues(referencedColumns));
    }

    /**
     * Unique.
     *
     * @param name the name
     * @param columns the columns
     * @return the creates the table builder
     */
    public TABLE_BUILDER unique(String name, String columns) {
        if (logger.isTraceEnabled()) {
            logger.trace("unique: " + name + ", columns" + columns);
        }
        return unique(name, splitValues(columns));
    }

    /**
     * Unique.
     *
     * @param name the name
     * @param columns the columns
     * @return the creates the table builder
     */
    @Override
    public TABLE_BUILDER unique(String name, String[] columns) {
        if (logger.isTraceEnabled()) {
            logger.trace("unique: " + name + ", columns" + Arrays.toString(columns));
        }
        CreateTableUniqueIndexBuilder uniqueIndex = new CreateTableUniqueIndexBuilder(this.getDialect(), name);
        for (String column : columns) {
            uniqueIndex.column(column);
        }
        this.uniqueIndices.add(uniqueIndex);
        return (TABLE_BUILDER) this;
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
    public TABLE_BUILDER unique(String name, String[] columns, String type, String order) {
        return unique(name, columns);
    }

    /**
     * Check.
     *
     * @param name the name
     * @param expression the expression
     * @return the creates the table builder
     */
    public TABLE_BUILDER check(String name, String expression) {
        if (logger.isTraceEnabled()) {
            logger.trace("check: " + name + ", expression" + expression);
        }
        CreateTableCheckBuilder check = new CreateTableCheckBuilder(this.getDialect(), name);
        check.expression(expression);
        this.checks.add(check);
        return (TABLE_BUILDER) this;
    }

    /**
     * Index.
     *
     * @param name the name
     * @param isUnique the isUnique
     * @param order the order
     * @param type the type
     * @param columns the index columns
     * @return the creates the table builder
     */
    public TABLE_BUILDER index(String name, Boolean isUnique, String order, String type, Set<String> columns) {
        if (logger.isTraceEnabled()) {
            logger.trace("index: " + name + ", isUnique" + isUnique + ", type" + type + ", columns" + columns);
        }
        CreateTableIndexBuilder index = new CreateTableIndexBuilder(getDialect(), name);
        index.setIndexType(type);
        index.setUnique(isUnique);
        index.setOrder(order);
        index.setColumns(columns);
        this.indices.add(index);
        return (TABLE_BUILDER) this;
    }

    /**
     * Generate.
     *
     * @return the string
     */
    @Override
    public String generate() {
        TableStatements table = buildTable();

        String generated = table.getCreateTableStatement();
        if (!table.getCreateIndicesStatements()
                  .isEmpty()) {
            String uniqueIndices = table.getCreateIndicesStatements()
                                        .stream()
                                        .collect(Collectors.joining(STATEMENT_DELIMITER));
            generated = String.join(STATEMENT_DELIMITER, generated, uniqueIndices);
        }

        if (logger.isTraceEnabled()) {
            logger.trace("generated: " + generated);
        }

        return generated;
    }

    /**
     * Build {@link TableStatements} object containing the SQL statements.
     *
     * @return {@link TableStatements}
     */
    public TableStatements buildTable() {

        StringBuilder sql = new StringBuilder();

        // CREATE
        generateCreate(sql);

        // TABLE
        generateTable(sql);

        sql.append(SPACE)
           .append(OPEN);

        // COLUMNS
        generateColumns(sql);

        // PRIMARY KEY
        generatePrimaryKey(sql);

        // FOREIGN KEYS
        generateForeignKeys(sql);

        // CHECKS
        generateChecks(sql);

        sql.append(CLOSE);

        String createTableStatement = sql.toString();

        // INDICES
        Collection<String> createIndicesStatements = new HashSet<>();
        createIndicesStatements.addAll(generateIndices());
        createIndicesStatements.addAll(generateUniqueIndices());

        return new TableStatements(createTableStatement, createIndicesStatements);
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
               .append(traverseNames(this.primaryKey.getColumns()))
               .append(CLOSE);
        } else {
            if (isCompositeKey) {
                sql.append(COMMA)
                   .append(SPACE);
                ArrayList<String> keys = new ArrayList<>();
                allPrimaryKeys.forEach(el -> keys.add(encapsulate(el[0])));
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
     * Generate foreign keys.
     *
     * @param sql the sql
     */
    protected void generateForeignKeys(StringBuilder sql) {
        for (CreateTableForeignKeyBuilder foreignKey : this.foreignKeys) {
            generateForeignKey(sql, foreignKey);
        }
    }

    /**
     * Generate foreign key.
     *
     * @param sql the sql
     * @param foreignKey the foreign key
     */
    protected void generateForeignKey(StringBuilder sql, CreateTableForeignKeyBuilder foreignKey) {
        if (foreignKey != null) {
            sql.append(COMMA)
               .append(SPACE);
            if (foreignKey.getName() != null) {
                String foreignKeyName = encapsulate(foreignKey.getName());
                sql.append(KEYWORD_CONSTRAINT)
                   .append(SPACE)
                   .append(foreignKeyName)
                   .append(SPACE);
            }
            String referencedTableName = encapsulate(foreignKey.getReferencedTable(), true);
            sql.append(KEYWORD_FOREIGN)
               .append(SPACE)
               .append(KEYWORD_KEY)
               .append(SPACE)
               .append(OPEN)
               .append(traverseNames(foreignKey.getColumns()))
               .append(CLOSE)
               .append(SPACE)
               .append(KEYWORD_REFERENCES)
               .append(SPACE);

            if (foreignKey.getReferencedTableSchema() != null) {
                String refTableSchema = encapsulate(foreignKey.getReferencedTableSchema(), true);
                sql.append(refTableSchema)
                   .append(DOT);
            }

            sql.append(referencedTableName)
               .append(OPEN)
               .append(traverseNames(foreignKey.getReferencedColumns()))
               .append(CLOSE);
        }
    }

    /**
     * Generate create statements for indices.
     *
     * @return Collection of create index statements
     */
    protected Collection<String> generateUniqueIndices() {
        Collection<String> indices = new HashSet<>();
        for (CreateTableUniqueIndexBuilder uniqueIndex : this.uniqueIndices) {
            indices.add(generateUniqueIndex(uniqueIndex));
        }

        return indices;
    }

    /**
     * Generate unique index.
     *
     * @param uniqueIndex the unique index
     * @return Create index statement
     */
    protected String generateUniqueIndex(CreateTableUniqueIndexBuilder uniqueIndex) {
        StringBuilder sql = new StringBuilder();
        if (uniqueIndex != null) {
            sql.append(KEYWORD_CREATE)
               .append(SPACE);
            sql.append(KEYWORD_UNIQUE)
               .append(SPACE);
            if (uniqueIndex.getIndexType() != null) {
                sql.append(uniqueIndex.getIndexType())
                   .append(SPACE);
            }
            sql.append(KEYWORD_INDEX)
               .append(SPACE);
            if (uniqueIndex.getName() != null) {
                sql.append(encapsulate(uniqueIndex.getName()))
                   .append(SPACE);
            }
            sql.append(KEYWORD_ON)
               .append(SPACE)
               .append(encapsulate(this.getTable()));
            sql.append(SPACE)
               .append(OPEN)
               .append(traverseNames(uniqueIndex.getColumns()))
               .append(CLOSE);
            if (uniqueIndex.getOrder() != null) {
                sql.append(SPACE)
                   .append(uniqueIndex.getOrder());
            }

        }

        return sql.toString();
    }

    /**
     * Generate checks.
     *
     * @param sql the sql
     */
    protected void generateChecks(StringBuilder sql) {
        for (CreateTableCheckBuilder index : this.checks) {
            generateCheck(sql, index);
        }
    }

    /**
     * Generate check.
     *
     * @param sql the sql
     * @param check the check
     */
    protected void generateCheck(StringBuilder sql, CreateTableCheckBuilder check) {
        if (check != null) {
            sql.append(COMMA)
               .append(SPACE);
            if (check.getName() != null) {
                String checkName = encapsulate(check.getName());
                sql.append(KEYWORD_CONSTRAINT)
                   .append(SPACE)
                   .append(checkName)
                   .append(SPACE);
            }
            sql.append(KEYWORD_CHECK)
               .append(SPACE)
               .append(OPEN)
               .append(check.getExpression())
               .append(CLOSE);
        }
    }

    /**
     * Generate create statements for indices.
     *
     * @return Collection of create index statements
     */
    protected Collection<String> generateIndices() {
        Collection<String> indices = new HashSet<>();
        for (CreateTableIndexBuilder index : this.indices) {
            indices.add(generateIndex(index));
        }

        return indices;
    }

    /**
     * Generate index create statement.
     *
     * @param index IndexBuilder
     * @return Generated statement
     */
    protected String generateIndex(CreateTableIndexBuilder index) {
        StringBuilder sql = new StringBuilder();
        if (index != null && !index.isUnique()) {
            sql.append(KEYWORD_CREATE)
               .append(SPACE);
            if (index.getIndexType() != null) {
                sql.append(index.getIndexType())
                   .append(SPACE);
            }
            sql.append(KEYWORD_INDEX)
               .append(SPACE)
               .append(encapsulate(index.getName()))
               .append(SPACE)
               .append(KEYWORD_ON)
               .append(SPACE)
               .append(encapsulate(this.getTable()));
            sql.append(SPACE)
               .append(OPEN)
               .append(traverseNames(index.getColumns()))
               .append(CLOSE);
            if (index.getOrder() != null) {
                sql.append(SPACE)
                   .append(index.getOrder());
            }
        }

        return sql.toString();
    }

}
