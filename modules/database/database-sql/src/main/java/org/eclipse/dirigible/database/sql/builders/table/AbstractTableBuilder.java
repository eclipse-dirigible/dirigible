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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.dirigible.database.sql.DataType;
import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.AbstractCreateSqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Create Table Builder.
 *
 * @param <TABLE_BUILDER> the generic type
 */
public abstract class AbstractTableBuilder<TABLE_BUILDER extends AbstractTableBuilder> extends AbstractCreateSqlBuilder {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(AbstractTableBuilder.class);

    /** The Constant OPERATION_NOT_SUPPORTED_FOR_THIS_DATABASE_TYPE_ERROR. */
    private static final String OPERATION_NOT_SUPPORTED_FOR_THIS_DATABASE_TYPE_ERROR = "Operation not supported for this Database type!";
    /** The columns. */
    protected final List<String[]> columns = new ArrayList<>();
    /** The table. */
    private final String table;
    private String schema;

    /**
     * Instantiates a new creates the table builder.
     *
     * @param dialect the dialect
     * @param table the table
     */
    public AbstractTableBuilder(ISqlDialect dialect, String table) {
        super(dialect);
        this.table = table;
    }

    /**
     * Unique.
     *
     * @param name the name
     * @param columns the columns
     * @return the table builder
     */
    public TABLE_BUILDER unique(String name, String[] columns) {
        throw new IllegalStateException(OPERATION_NOT_SUPPORTED_FOR_THIS_DATABASE_TYPE_ERROR);
    }

    /**
     * Unique.
     *
     * @param name the name
     * @param columns the columns
     * @param type the type
     * @param order the order
     * @return the table builder
     */
    public TABLE_BUILDER unique(String name, String[] columns, String type, String order) {
        throw new IllegalStateException(OPERATION_NOT_SUPPORTED_FOR_THIS_DATABASE_TYPE_ERROR);
    }

    /**
     * Primary key.
     *
     * @param name the name
     * @param columns the columns
     * @return the table builder
     */
    public TABLE_BUILDER primaryKey(String name, String[] columns) {
        throw new IllegalStateException(OPERATION_NOT_SUPPORTED_FOR_THIS_DATABASE_TYPE_ERROR);
    }

    /**
     * Primary key.
     *
     * @param columns the columns
     * @return the table builder
     */
    public TABLE_BUILDER primaryKey(String[] columns) {
        throw new IllegalStateException(OPERATION_NOT_SUPPORTED_FOR_THIS_DATABASE_TYPE_ERROR);
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
        throw new IllegalStateException(OPERATION_NOT_SUPPORTED_FOR_THIS_DATABASE_TYPE_ERROR);
    }

    /**
     * Check.
     *
     * @param name the name
     * @param expression the expression
     * @return the table builder
     */
    public TABLE_BUILDER check(String name, String expression) {
        throw new IllegalStateException(OPERATION_NOT_SUPPORTED_FOR_THIS_DATABASE_TYPE_ERROR);
    }

    /**
     * Index.
     *
     * @param name the name
     * @param isUnique whether the index is unique
     * @param order the order
     * @param type the type
     * @param columns the list of the columns names
     * @return the table builder
     */
    public TABLE_BUILDER index(String name, Boolean isUnique, String order, String type, Set<String> columns) {
        throw new IllegalStateException(OPERATION_NOT_SUPPORTED_FOR_THIS_DATABASE_TYPE_ERROR);
    }

    /**
     * Column.
     *
     * @param name the name
     * @param type the type
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param isFuzzyIndexEnabled the is fuzzy index enabled
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER column(String name, DataType type, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique,
            Boolean isFuzzyIndexEnabled, String... args) {
        return column(name, type, isPrimaryKey, isNullable, isUnique, isFuzzyIndexEnabled, args);
    }

    /**
     * Column.
     *
     * @param name the name
     * @param type the type
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @return the creates the table builder
     */
    public TABLE_BUILDER column(String name, DataType type, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique) {
        return column(name, type, isPrimaryKey, isNullable, isUnique, new String[] {});
    }

    /**
     * Column.
     *
     * @param name the name
     * @param type the type
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER column(String name, DataType type, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, String... args) {
        return column(name, type, isPrimaryKey, isNullable, isUnique, false, false, false, args);
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
     * @return the creates the table builder
     */
    public TABLE_BUILDER column(String name, DataType type, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique,
            Boolean isAutoincrement, Boolean isIdentity, Boolean isFuzzyIndexEnabled, String... args) {
        if (logger.isTraceEnabled()) {
            logger.trace("column: " + name + ", type: " + (type != null ? type.name() : null) + ", isPrimaryKey: " + isPrimaryKey
                    + ", isNullable: " + isNullable + ", isUnique: " + isUnique + ", isAutoincrement: " + isAutoincrement + ", isIdentity: "
                    + isIdentity + ", args: " + Arrays.toString(args));
        }
        String[] definition = new String[] {name, getDialect().getDataTypeName(type)};
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
        return (TABLE_BUILDER) this;
    }

    public TABLE_BUILDER schema(String schemaName) {
        this.schema = schemaName;
        return (TABLE_BUILDER) this;
    }

    /**
     * Column.
     *
     * @param name the name
     * @param type the type
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @return the creates the table builder
     */
    public TABLE_BUILDER column(String name, DataType type, Boolean isPrimaryKey, Boolean isNullable) {
        return column(name, type, isPrimaryKey, isNullable, false, new String[] {});
    }

    /**
     * Column.
     *
     * @param name the name
     * @param type the type
     * @param isPrimaryKey the is primary key
     * @return the creates the table builder
     */
    public TABLE_BUILDER column(String name, DataType type, Boolean isPrimaryKey) {
        return column(name, type, isPrimaryKey, true, false, new String[] {});
    }

    /**
     * Column.
     *
     * @param name the name
     * @param type the type
     * @return the creates the table builder
     */
    public TABLE_BUILDER column(String name, DataType type) {
        return column(name, type, false, true, false, new String[] {});
    }

    /**
     * Column.
     *
     * @param name the name
     * @param type the type
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER column(String name, int type, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, String... args) {
        return column(name, DataType.values()[type], isPrimaryKey, isNullable, isUnique, args);
    }

    /**
     * Column.
     *
     * @param name the name
     * @param type the type
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @return the creates the table builder
     */
    public TABLE_BUILDER column(String name, int type, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique) {
        return column(name, DataType.values()[type], isPrimaryKey, isNullable, isUnique, new String[] {});
    }

    /**
     * Column.
     *
     * @param name the name
     * @param type the type
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @return the creates the table builder
     */
    public TABLE_BUILDER column(String name, int type, Boolean isPrimaryKey, Boolean isNullable) {
        return column(name, DataType.values()[type], isPrimaryKey, isNullable, false, new String[] {});
    }

    /**
     * Column.
     *
     * @param name the name
     * @param type the type
     * @param isPrimaryKey the is primary key
     * @return the creates the table builder
     */
    public TABLE_BUILDER column(String name, int type, Boolean isPrimaryKey) {
        return column(name, DataType.values()[type], isPrimaryKey, true, false, new String[] {});
    }

    /**
     * Column.
     *
     * @param name the name
     * @param type the type
     * @return the creates the table builder
     */
    public TABLE_BUILDER column(String name, int type) {
        return column(name, DataType.values()[type], false, true, false, new String[] {});
    }

    /**
     * Column.
     *
     * @param name the name
     * @param type the type
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER column(String name, int type, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, String args) {
        return column(name, DataType.values()[type], isPrimaryKey, isNullable, isUnique, splitValues(args));
    }

    /**
     * Split values.
     *
     * @param columns the columns
     * @return the string[]
     */
    protected String[] splitValues(String columns) {
        String[] array = new String[] {};
        if (columns != null) {
            array = columns.split(",");
        }
        for (int i = 0; i < array.length; i++) {
            array[i] = array[i].trim();
        }
        return array;
    }

    /**
     * Column.
     *
     * @param name the name
     * @param type the type
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER column(String name, Double type, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, String... args) {
        return column(name, DataType.values()[type.intValue()], isPrimaryKey, isNullable, isUnique, args);
    }

    /**
     * Column.
     *
     * @param name the name
     * @param type the type
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @return the creates the table builder
     */
    public TABLE_BUILDER column(String name, Double type, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique) {
        return column(name, DataType.values()[type.intValue()], isPrimaryKey, isNullable, isUnique, new String[] {});
    }

    /**
     * Column.
     *
     * @param name the name
     * @param type the type
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @return the creates the table builder
     */
    public TABLE_BUILDER column(String name, Double type, Boolean isPrimaryKey, Boolean isNullable) {
        return column(name, DataType.values()[type.intValue()], isPrimaryKey, isNullable, false, new String[] {});
    }

    /**
     * Column.
     *
     * @param name the name
     * @param type the type
     * @param isPrimaryKey the is primary key
     * @return the creates the table builder
     */
    public TABLE_BUILDER column(String name, Double type, Boolean isPrimaryKey) {
        return column(name, DataType.values()[type.intValue()], isPrimaryKey, true, false, new String[] {});
    }

    /**
     * Column.
     *
     * @param name the name
     * @param type the type
     * @return the creates the table builder
     */
    public TABLE_BUILDER column(String name, Double type) {
        return column(name, DataType.values()[type.intValue()], false, true, false, new String[] {});
    }

    /**
     * Column.
     *
     * @param name the name
     * @param type the type
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER column(String name, Double type, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, String args) {
        return column(name, DataType.values()[type.intValue()], isPrimaryKey, isNullable, isUnique, splitValues(args));
    }

    /**
     * Column.
     *
     * @param name the name
     * @param type the type
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER column(String name, String type, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, String... args) {
        return column(name, DataType.valueOfByName(type), isPrimaryKey, isNullable, isUnique, args);
    }

    /**
     * Column.
     *
     * @param name the name
     * @param type the type
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @return the creates the table builder
     */
    public TABLE_BUILDER column(String name, String type, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique) {
        return column(name, DataType.valueOfByName(type), isPrimaryKey, isNullable, isUnique, new String[] {});
    }

    /**
     * Column.
     *
     * @param name the name
     * @param type the type
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @return the creates the table builder
     */
    public TABLE_BUILDER column(String name, String type, Boolean isPrimaryKey, Boolean isNullable) {
        return column(name, DataType.valueOfByName(type), isPrimaryKey, isNullable, false, new String[] {});
    }

    /**
     * Column.
     *
     * @param name the name
     * @param type the type
     * @param isPrimaryKey the is primary key
     * @return the creates the table builder
     */
    public TABLE_BUILDER column(String name, String type, Boolean isPrimaryKey) {
        return column(name, DataType.valueOfByName(type), isPrimaryKey, true, false, new String[] {});
    }

    /**
     * Column.
     *
     * @param name the name
     * @param type the type
     * @return the creates the table builder
     */
    public TABLE_BUILDER column(String name, String type) {
        return column(name, DataType.valueOfByName(type), false, true, false, new String[] {});
    }

    /**
     * Column.
     *
     * @param name the name
     * @param type the type
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER column(String name, String type, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, String args) {
        return column(name, DataType.valueOfByName(type), isPrimaryKey, isNullable, isUnique, splitValues(args));
    }

    /**
     * Column varchar.
     *
     * @param name the name
     * @param length the length
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param isIdentity the is identity
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnVarchar(String name, int length, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique,
            Boolean isIdentity, String args) {
        return columnVarchar(name, length, isPrimaryKey, isNullable, isUnique, isIdentity, splitValues(args));
    }

    /**
     * Column varchar.
     *
     * @param name the name
     * @param length the length
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param isIdentity the is identity
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnVarchar(String name, int length, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique,
            Boolean isIdentity, String... args) {
        String[] definition = new String[] {OPEN + length + CLOSE};
        String[] coulmn = Stream.of(definition, args)
                                .flatMap(Stream::of)
                                .toArray(String[]::new);
        return this.column(name, DataType.VARCHAR, isPrimaryKey, isNullable, isUnique, false, isIdentity, false, coulmn);
    }

    /**
     * Column varchar.
     *
     * @param name the name
     * @param length the length
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnVarchar(String name, int length) {
        return columnVarchar(name, length, false);
    }

    /**
     * Column varchar.
     *
     * @param name the name
     * @param length the length
     * @param isPrimaryKey the is primary key
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnVarchar(String name, int length, Boolean isPrimaryKey) {
        return columnVarchar(name, length, isPrimaryKey, true);
    }

    /**
     * Column varchar.
     *
     * @param name the name
     * @param length the length
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnVarchar(String name, int length, Boolean isPrimaryKey, Boolean isNullable) {
        return columnVarchar(name, length, isPrimaryKey, isNullable, false);
    }

    /**
     * Column varchar.
     *
     * @param name the name
     * @param length the length
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnVarchar(String name, int length, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique) {
        return columnVarchar(name, length, isPrimaryKey, isNullable, isUnique, false);
    }

    /**
     * Column varchar.
     *
     * @param name the name
     * @param length the length
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param isIdentity the is unique
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnVarchar(String name, int length, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique,
            Boolean isIdentity) {
        return columnVarchar(name, length, isPrimaryKey, isNullable, isUnique, isIdentity, new String[] {});
    }

    /**
     * Column nvarchar.
     *
     * @param name the name
     * @param length the length
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param isIdentity the is identity
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnNvarchar(String name, int length, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique,
            Boolean isIdentity, String args) {
        return columnNvarchar(name, length, isPrimaryKey, isNullable, isUnique, isIdentity, splitValues(args));
    }

    /**
     * Column nvarchar.
     *
     * @param name the name
     * @param length the length
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param isIdentity the is identity
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnNvarchar(String name, int length, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique,
            Boolean isIdentity, String... args) {
        String[] definition = new String[] {OPEN + length + CLOSE};
        String[] coulmn = Stream.of(definition, args)
                                .flatMap(Stream::of)
                                .toArray(String[]::new);
        return this.column(name, DataType.NVARCHAR, isPrimaryKey, isNullable, isUnique, false, isIdentity, false, coulmn);
    }

    /**
     * Column nvarchar.
     *
     * @param name the name
     * @param length the length
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnNvarchar(String name, int length) {
        return columnNvarchar(name, length, false);
    }

    /**
     * Column nvarchar.
     *
     * @param name the name
     * @param length the length
     * @param isPrimaryKey the is primary key
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnNvarchar(String name, int length, Boolean isPrimaryKey) {
        return columnNvarchar(name, length, isPrimaryKey, true);
    }

    /**
     * Column nvarchar.
     *
     * @param name the name
     * @param length the length
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnNvarchar(String name, int length, Boolean isPrimaryKey, Boolean isNullable) {
        return columnNvarchar(name, length, isPrimaryKey, isNullable, false);
    }

    /**
     * Column nvarchar.
     *
     * @param name the name
     * @param length the length
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnNvarchar(String name, int length, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique) {
        return columnNvarchar(name, length, isPrimaryKey, isNullable, isUnique, false);
    }

    /**
     * Column nvarchar.
     *
     * @param name the name
     * @param length the length
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param isIdentity the is unique
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnNvarchar(String name, int length, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique,
            Boolean isIdentity) {
        return columnNvarchar(name, length, isPrimaryKey, isNullable, isUnique, isIdentity, new String[] {});
    }

    /**
     * Column char.
     *
     * @param name the name
     * @param length the length
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnChar(String name, int length, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, String args) {
        return columnChar(name, length, isPrimaryKey, isNullable, isUnique, false, splitValues(args));
    }

    /**
     * Column char.
     *
     * @param name the name
     * @param length the length
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param isIdentity the is identity
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnChar(String name, int length, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, Boolean isIdentity,
            String... args) {
        String[] definition = new String[] {OPEN + length + CLOSE};
        String[] coulmn = Stream.of(definition, args)
                                .flatMap(Stream::of)
                                .toArray(String[]::new);
        return this.column(name, DataType.CHAR, isPrimaryKey, isNullable, isUnique, false, isIdentity, false, coulmn);
    }

    /**
     * Column char.
     *
     * @param name the name
     * @param length the length
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnChar(String name, int length) {
        return columnChar(name, length, false);
    }

    /**
     * Column char.
     *
     * @param name the name
     * @param length the length
     * @param isPrimaryKey the is primary key
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnChar(String name, int length, Boolean isPrimaryKey) {
        return columnChar(name, length, isPrimaryKey, true);
    }

    /**
     * Column char.
     *
     * @param name the name
     * @param length the length
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnChar(String name, int length, Boolean isPrimaryKey, Boolean isNullable) {
        return columnChar(name, length, isPrimaryKey, isNullable, false);
    }

    /**
     * Column char.
     *
     * @param name the name
     * @param length the length
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnChar(String name, int length, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique) {
        return columnChar(name, length, isPrimaryKey, isNullable, isUnique, false);
    }

    /**
     * Column date.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnDate(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, String args) {
        return columnDate(name, isPrimaryKey, isNullable, isUnique, splitValues(args));
    }

    /**
     * Column date.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnDate(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, String... args) {
        return this.column(name, DataType.DATE, isPrimaryKey, isNullable, isUnique, args);
    }

    /**
     * Column date.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnDate(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique) {
        return columnDate(name, isPrimaryKey, isNullable, isUnique, new String[] {});
    }

    /**
     * Column date.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnDate(String name, Boolean isPrimaryKey, Boolean isNullable) {
        return columnDate(name, isPrimaryKey, isNullable, false, new String[] {});
    }

    /**
     * Column date.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnDate(String name, Boolean isPrimaryKey) {
        return columnDate(name, isPrimaryKey, true, false, new String[] {});
    }

    /**
     * Column date.
     *
     * @param name the name
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnDate(String name) {
        return columnDate(name, false, true, false, new String[] {});
    }

    /**
     * Column time.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnTime(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, String args) {
        return columnTime(name, isPrimaryKey, isNullable, isUnique, splitValues(args));
    }

    /**
     * Column time.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnTime(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, String... args) {
        return this.column(name, DataType.TIME, isPrimaryKey, isNullable, isUnique, args);
    }

    /**
     * Column time.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnTime(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique) {
        return columnTime(name, isPrimaryKey, isNullable, isUnique, new String[] {});
    }

    /**
     * Column time.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnTime(String name, Boolean isPrimaryKey, Boolean isNullable) {
        return columnTime(name, isPrimaryKey, isNullable, false, new String[] {});
    }

    /**
     * Column time.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnTime(String name, Boolean isPrimaryKey) {
        return columnTime(name, isPrimaryKey, true, false, new String[] {});
    }

    /**
     * Column time.
     *
     * @param name the name
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnTime(String name) {
        return columnTime(name, false, true, false, new String[] {});
    }

    /**
     * Column timestamp.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnTimestamp(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, String args) {
        return columnTimestamp(name, isPrimaryKey, isNullable, isUnique, splitValues(args));
    }

    /**
     * Column timestamp.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnTimestamp(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, String... args) {
        return this.column(name, DataType.TIMESTAMP, isPrimaryKey, isNullable, isUnique, args);
    }

    /**
     * Column timestamp.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnTimestamp(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique) {
        return columnTimestamp(name, isPrimaryKey, isNullable, isUnique, new String[] {});
    }

    /**
     * Column timestamp.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnTimestamp(String name, Boolean isPrimaryKey, Boolean isNullable) {
        return columnTimestamp(name, isPrimaryKey, isNullable, false, new String[] {});
    }

    /**
     * Column timestamp.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnTimestamp(String name, Boolean isPrimaryKey) {
        return columnTimestamp(name, isPrimaryKey, true, false, new String[] {});
    }

    /**
     * Column timestamp.
     *
     * @param name the name
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnTimestamp(String name) {
        return columnTimestamp(name, false, true, false, new String[] {});
    }

    /**
     * Column integer.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param isIdentity the is identity
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnInteger(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, Boolean isIdentity,
            String args) {
        return columnInteger(name, isPrimaryKey, isNullable, isUnique, isIdentity, splitValues(args));
    }

    /**
     * Column integer.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param isIdentity the is identity
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnInteger(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, Boolean isIdentity,
            String... args) {
        return this.column(name, DataType.INTEGER, isPrimaryKey, isNullable, isUnique, false, isIdentity, false, args);
    }

    /**
     * Column integer.
     *
     * @param name the name
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnInteger(String name) {
        return columnInteger(name, false);
    }

    /**
     * Column integer.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnInteger(String name, Boolean isPrimaryKey) {
        return columnInteger(name, isPrimaryKey, true);
    }

    /**
     * Column integer.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnInteger(String name, Boolean isPrimaryKey, Boolean isNullable) {
        return columnInteger(name, isPrimaryKey, isNullable, false);
    }

    /**
     * Column integer.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnInteger(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique) {
        return columnInteger(name, isPrimaryKey, isNullable, isUnique, false);
    }

    /**
     * Column tinyint.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnTinyint(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, String args) {
        return columnTinyint(name, isPrimaryKey, isNullable, isUnique, splitValues(args));
    }

    /**
     * Column tinyint.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnTinyint(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, String... args) {
        return this.column(name, DataType.TINYINT, isPrimaryKey, isNullable, isUnique, args);
    }

    /**
     * Column tinyint.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnTinyint(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique) {
        return columnTinyint(name, isPrimaryKey, isNullable, isUnique, new String[] {});
    }

    /**
     * Column tinyint.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnTinyint(String name, Boolean isPrimaryKey, Boolean isNullable) {
        return columnTinyint(name, isPrimaryKey, isNullable, false, new String[] {});
    }

    /**
     * Column tinyint.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnTinyint(String name, Boolean isPrimaryKey) {
        return columnTinyint(name, isPrimaryKey, true, false, new String[] {});
    }

    /**
     * Column tinyint.
     *
     * @param name the name
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnTinyint(String name) {
        return columnTinyint(name, false, true, false, new String[] {});
    }

    /**
     * Column bigint.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param isIdentity the is identity
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnBigint(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, Boolean isIdentity,
            String args) {
        return columnBigint(name, isPrimaryKey, isNullable, isUnique, isIdentity, splitValues(args));
    }

    /**
     * Column bigint.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param isIdentity the is identity
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnBigint(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, Boolean isIdentity,
            String... args) {
        return this.column(name, DataType.BIGINT, isPrimaryKey, isNullable, isUnique, false, isIdentity, false, args);
    }

    /**
     * Column bigint.
     *
     * @param name the name
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnBigint(String name) {
        return columnBigint(name, false);
    }

    /**
     * Column bigint.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnBigint(String name, Boolean isPrimaryKey) {
        return columnBigint(name, isPrimaryKey, true);
    }

    /**
     * Column bigint.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnBigint(String name, Boolean isPrimaryKey, Boolean isNullable) {
        return columnBigint(name, isPrimaryKey, isNullable, false);
    }

    /**
     * Column bigint.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnBigint(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique) {
        return columnBigint(name, isPrimaryKey, isNullable, isUnique, false);
    }

    /**
     * Column smallint.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnSmallint(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, String args) {
        return columnSmallint(name, isPrimaryKey, isNullable, isUnique, splitValues(args));
    }

    /**
     * Column smallint.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnSmallint(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, String... args) {
        return this.column(name, DataType.SMALLINT, isPrimaryKey, isNullable, isUnique, args);
    }

    /**
     * Column smallint.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnSmallint(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique) {
        return columnSmallint(name, isPrimaryKey, isNullable, isUnique, new String[] {});
    }

    /**
     * Column smallint.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnSmallint(String name, Boolean isPrimaryKey, Boolean isNullable) {
        return columnSmallint(name, isPrimaryKey, isNullable, false, new String[] {});
    }

    /**
     * Column smallint.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnSmallint(String name, Boolean isPrimaryKey) {
        return columnSmallint(name, isPrimaryKey, true, false, new String[] {});
    }

    /**
     * Column smallint.
     *
     * @param name the name
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnSmallint(String name) {
        return columnSmallint(name, false, true, false, new String[] {});
    }

    /**
     * Column real.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnReal(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, String args) {
        return columnReal(name, isPrimaryKey, isNullable, isUnique, splitValues(args));
    }

    /**
     * Column real.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnReal(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, String... args) {
        return this.column(name, DataType.REAL, isPrimaryKey, isNullable, isUnique, args);
    }

    /**
     * Column real.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnReal(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique) {
        return columnReal(name, isPrimaryKey, isNullable, isUnique, new String[] {});
    }

    /**
     * Column real.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnReal(String name, Boolean isPrimaryKey, Boolean isNullable) {
        return columnReal(name, isPrimaryKey, isNullable, false, new String[] {});
    }

    /**
     * Column real.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnReal(String name, Boolean isPrimaryKey) {
        return columnReal(name, isPrimaryKey, true, false, new String[] {});
    }

    /**
     * Column real.
     *
     * @param name the name
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnReal(String name) {
        return this.column(name, DataType.REAL, false, true, false, new String[] {});
    }

    /**
     * Column float.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnFloat(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, String... args) {
        return this.column(name, DataType.FLOAT, isPrimaryKey, isNullable, isUnique, args);
    }

    /**
     * Column float.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnFloat(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, String args) {
        return columnReal(name, isPrimaryKey, isNullable, isUnique, splitValues(args));
    }

    /**
     * Column float.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnFloat(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique) {
        return columnReal(name, isPrimaryKey, isNullable, isUnique, new String[] {});
    }

    /**
     * Column float.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnFloat(String name, Boolean isPrimaryKey, Boolean isNullable) {
        return columnReal(name, isPrimaryKey, isNullable, false, new String[] {});
    }

    /**
     * Column float.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnFloat(String name, Boolean isPrimaryKey) {
        return columnReal(name, isPrimaryKey, true, false, new String[] {});
    }

    /**
     * Column float.
     *
     * @param name the name
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnFloat(String name) {
        return columnSmallint(name, false, true, false, new String[] {});
    }

    /**
     * Column double.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnDouble(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, String args) {
        return columnDouble(name, isPrimaryKey, isNullable, isUnique, splitValues(args));
    }

    /**
     * Column double.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnDouble(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, String... args) {
        return this.column(name, DataType.DOUBLE, isPrimaryKey, isNullable, isUnique, args);
    }

    /**
     * Column double.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnDouble(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique) {
        return columnDouble(name, isPrimaryKey, isNullable, isUnique, new String[] {});
    }

    /**
     * Column double.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnDouble(String name, Boolean isPrimaryKey, Boolean isNullable) {
        return columnDouble(name, isPrimaryKey, isNullable, false, new String[] {});
    }

    /**
     * Column double.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnDouble(String name, Boolean isPrimaryKey) {
        return columnDouble(name, isPrimaryKey, true, false, new String[] {});
    }

    /**
     * Column double.
     *
     * @param name the name
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnDouble(String name) {
        return this.column(name, DataType.DOUBLE, false, true, false, new String[] {});
    }

    /**
     * Column boolean.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnBoolean(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, String args) {
        return columnBoolean(name, isPrimaryKey, isNullable, isUnique, splitValues(args));
    }

    /**
     * Column boolean.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnBoolean(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique, String... args) {
        return this.column(name, DataType.BOOLEAN, isPrimaryKey, isNullable, isUnique, args);
    }

    /**
     * Column boolean.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnBoolean(String name, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique) {
        return columnBoolean(name, isPrimaryKey, isNullable, isUnique, new String[] {});
    }

    /**
     * Column boolean.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnBoolean(String name, Boolean isPrimaryKey, Boolean isNullable) {
        return columnBoolean(name, isPrimaryKey, isNullable, false, new String[] {});
    }

    /**
     * Column boolean.
     *
     * @param name the name
     * @param isPrimaryKey the is primary key
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnBoolean(String name, Boolean isPrimaryKey) {
        return columnBoolean(name, isPrimaryKey, true, false, new String[] {});
    }

    /**
     * Column boolean.
     *
     * @param name the name
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnBoolean(String name) {
        return columnBoolean(name, false, true, false, new String[] {});
    }

    /**
     * Column blob.
     *
     * @param name the name
     * @param isNullable the is nullable
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnBlob(String name, Boolean isNullable, String args) {
        return columnBlob(name, isNullable, splitValues(args));
    }

    /**
     * Column blob.
     *
     * @param name the name
     * @param isNullable the is nullable
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnBlob(String name, Boolean isNullable, String... args) {
        return this.column(name, DataType.BLOB, false, isNullable, false, args);
    }

    /**
     * Column blob.
     *
     * @param name the name
     * @param isNullable the is nullable
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnBlob(String name, Boolean isNullable) {
        return columnBlob(name, isNullable, new String[] {});
    }

    /**
     * Column blob.
     *
     * @param name the name
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnBlob(String name) {
        return columnBlob(name, false, new String[] {});
    }

    /**
     * Column decimal.
     *
     * @param name the name
     * @param length the length
     * @param scale the scale
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param isIdentity the is identity
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnDecimal(String name, int length, int scale, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique,
            Boolean isIdentity, String args) {
        return columnDecimal(name, length, scale, isPrimaryKey, isNullable, isUnique, isIdentity, splitValues(args));
    }

    /**
     * Column decimal.
     *
     * @param name the name
     * @param length the length
     * @param scale the scale
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @param isIdentity the is identity
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnDecimal(String name, int length, int scale, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique,
            Boolean isIdentity, String... args) {
        String[] definition = new String[] {OPEN + length + "," + scale + CLOSE};
        String[] column = Stream.of(definition, args)
                                .flatMap(Stream::of)
                                .toArray(String[]::new);
        return this.column(name, DataType.DECIMAL, isPrimaryKey, isNullable, isUnique, false, isIdentity, false, column);
    }

    /**
     * Column decimal.
     *
     * @param name the name
     * @param length the length
     * @param scale the scale
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnDecimal(String name, int length, int scale) {
        return columnDecimal(name, length, scale, false);
    }

    /**
     * Column decimal.
     *
     * @param name the name
     * @param length the length
     * @param scale the scale
     * @param isPrimaryKey the is primary key
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnDecimal(String name, int length, int scale, Boolean isPrimaryKey) {
        return columnDecimal(name, length, scale, isPrimaryKey, true);
    }

    /**
     * Column decimal.
     *
     * @param name the name
     * @param length the length
     * @param scale the scale
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnDecimal(String name, int length, int scale, Boolean isPrimaryKey, Boolean isNullable) {
        return columnDecimal(name, length, scale, isPrimaryKey, isNullable, false);
    }

    /**
     * Column decimal.
     *
     * @param name the name
     * @param length the length
     * @param scale the scale
     * @param isPrimaryKey the is primary key
     * @param isNullable the is nullable
     * @param isUnique the is unique
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnDecimal(String name, int length, int scale, Boolean isPrimaryKey, Boolean isNullable, Boolean isUnique) {
        return columnDecimal(name, length, scale, isPrimaryKey, isNullable, isUnique, false);
    }

    /**
     * Column bit.
     *
     * @param name the name
     * @param isNullable the is nullable
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnBit(String name, Boolean isNullable, String args) {
        return columnBit(name, isNullable, splitValues(args));
    }

    /**
     * Column bit.
     *
     * @param name the name
     * @param isNullable the is nullable
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnBit(String name, Boolean isNullable, String... args) {
        return this.column(name, DataType.BIT, false, isNullable, false, args);
    }

    /**
     * Column bit.
     *
     * @param name the name
     * @param isNullable the is nullable
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnBit(String name, Boolean isNullable) {
        return columnBit(name, isNullable, new String[] {});
    }

    /**
     * Column bit.
     *
     * @param name the name
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnBit(String name) {
        return columnBit(name, false, new String[] {});
    }

    /**
     * Column varbinary.
     *
     * @param name the name
     * @param isNullable the is nullable
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnVarbinary(String name, Boolean isNullable, String... args) {
        return this.column(name, DataType.VARBINARY, false, isNullable, false, args);
    }

    /**
     * Column varbinary.
     *
     * @param name the name
     * @param isNullable the is nullable
     * @param args the args
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnVarbinary(String name, Boolean isNullable, String args) {
        return columnBlob(name, isNullable, splitValues(args));
    }

    /**
     * Column varbinary.
     *
     * @param name the name
     * @param isNullable the is nullable
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnVarbinary(String name, Boolean isNullable) {
        return columnBlob(name, isNullable, new String[] {});
    }

    /**
     * Column varbinary.
     *
     * @param name the name
     * @return the creates the table builder
     */
    public TABLE_BUILDER columnVarbinary(String name) {
        return columnBlob(name, false, new String[] {});
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
           .append(generateSchema())
           .append(tableName);
    }

    protected String generateSchema() {
        Optional<String> schemaOpt = getSchema();
        if (schemaOpt.isEmpty()) {
            return "";
        }
        String encapsulatedSchema = encapsulate(schemaOpt.get());
        return encapsulatedSchema + DOT;
    }

    protected Optional<String> getSchema() {
        return Optional.ofNullable(schema);
    }

    /**
     * Gets the table.
     *
     * @return the table
     */
    protected String getTable() {
        return table;
    }

    /**
     * Generate columns.
     *
     * @param sql the sql
     */
    protected void generateColumns(StringBuilder sql) {
        if (!this.getColumns()
                 .isEmpty()) {
            sql.append(traverseColumns());
        }
    }

    /**
     * Gets the columns.
     *
     * @return the columns
     */
    protected List<String[]> getColumns() {
        return columns;
    }

    /**
     * Traverse columns.
     *
     * @return the string
     */
    protected String traverseColumns() {

        List<String[]> allPrimaryKeys = this.columns.stream()
                                                    .filter(el -> Arrays.stream(el)
                                                                        .anyMatch(x -> x.equals(getDialect().getPrimaryKeyArgument())))
                                                    .collect(Collectors.toList());
        boolean isCompositeKey = allPrimaryKeys.size() > 1;

        return getTraversedColumnsSnippet(isCompositeKey, this.columns);
    }

    /**
     * Gets the traversed columns snippet.
     *
     * @param isCompositeKey the is composite key
     * @param columns the columns
     * @return the traversed columns snippet
     */
    private String getTraversedColumnsSnippet(boolean isCompositeKey, List<String[]> columns) {
        StringBuilder snippet = new StringBuilder();
        snippet.append(SPACE);
        for (String[] column : columns) {
            boolean first = true;
            for (String arg : column) {
                if (first) {
                    String columnName = encapsulate(arg);
                    snippet.append(columnName)
                           .append(SPACE);
                    first = false;
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
     * Generate column names.
     *
     * @param sql the sql
     */
    protected void generateColumnNamesForDrop(StringBuilder sql) {
        if (!this.getColumns()
                 .isEmpty()) {
            sql.append(traverseColumnNamesForDrop());
        }
    }

    /**
     * Traverse columns.
     *
     * @return the string
     */
    protected String traverseColumnNamesForDrop() {
        StringBuilder snippet = new StringBuilder();
        for (String[] column : this.columns) {
            String columnName = encapsulate(column[0]);
            snippet.append(KEYWORD_DROP)
                   .append(SPACE)
                   .append(KEYWORD_COLUMN)
                   .append(SPACE);
            snippet.append(columnName)
                   .append(SPACE);
            snippet.append(COMMA)
                   .append(SPACE);
        }
        return snippet.substring(0, snippet.length() - 2);
    }

    /**
     * Generate columns for alter.
     *
     * @param sql the sql
     */
    protected void generateColumnsForAlter(StringBuilder sql) {
        if (!this.getColumns()
                 .isEmpty()) {
            sql.append(traverseColumnsForAlter());
        }
    }

    /**
     * Traverse columns for alter.
     *
     * @return the string
     */
    protected String traverseColumnsForAlter() {
        List<String[]> allPrimaryKeys = this.columns.stream()
                                                    .filter(el -> Arrays.stream(el)
                                                                        .anyMatch(x -> x.equals(getDialect().getPrimaryKeyArgument())))
                                                    .collect(Collectors.toList());
        List<String[]> columnsToAlter = this.columns.stream()
                                                    .map(columnArr -> (Arrays.stream(columnArr)
                                                                             .anyMatch(x -> x.equals(getDialect().getPrimaryKeyArgument())))
                                                                                     ? Arrays.stream(columnArr)
                                                                                             .filter(x -> !x.contains(KEYWORD_PRIMARY))
                                                                                             .toArray(String[]::new)
                                                                                     : columnArr)
                                                    .collect(Collectors.toList());
        boolean isCompositeKey = allPrimaryKeys.size() > 1;

        return getTraversedColumnsSnippet(isCompositeKey, columnsToAlter);
    }

    /**
     * Traverse column names.
     *
     * @param names the columns
     * @return the string
     */
    protected String traverseNames(Set<String> names) {
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
