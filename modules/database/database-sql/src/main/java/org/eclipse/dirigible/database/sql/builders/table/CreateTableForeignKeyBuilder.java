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

import java.util.Set;
import java.util.TreeSet;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Create Table Foreign Key Builder.
 */
public class CreateTableForeignKeyBuilder extends AbstractCreateTableConstraintBuilder<CreateTableForeignKeyBuilder> {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(CreateTableForeignKeyBuilder.class);

    /** The referenced table. */
    private String referencedTable;

    /** The referenced table schema. */
    private String referencedTableSchema;

    /** The referenced columns. */
    private Set<String> referencedColumns = new TreeSet<String>();


    /**
     * Instantiates a new creates the table foreign key builder.
     *
     * @param dialect the dialect
     * @param name the name
     */
    CreateTableForeignKeyBuilder(ISqlDialect dialect, String name) {
        super(dialect, name);
    }

    /**
     * Gets the referenced table.
     *
     * @return the referenced table
     */
    public String getReferencedTable() {
        return referencedTable;
    }

    /**
     * Gets the referenced columns.
     *
     * @return the referenced columns
     */
    public Set<String> getReferencedColumns() {
        return referencedColumns;
    }

    /**
     * Referenced table.
     *
     * @param referencedTable the referenced table
     * @return created table foreign key builder
     */
    public CreateTableForeignKeyBuilder referencedTable(String referencedTable) {
        if (logger.isTraceEnabled()) {
            logger.trace("referencedTable: " + referencedTable);
        }
        this.referencedTable = referencedTable;
        return this;
    }

    /**
     * Referenced column.
     *
     * @param referencedColumn the referenced column
     * @return created table foreign key builder
     */
    public CreateTableForeignKeyBuilder referencedColumn(String referencedColumn) {
        if (logger.isTraceEnabled()) {
            logger.trace("referencedColumn: " + referencedColumn);
        }
        this.referencedColumns.add(referencedColumn);
        return this;
    }

    /**
     * Gets the referenced table schema.
     *
     * @return the referenced table schema
     */
    public String getReferencedTableSchema() {
        return referencedTableSchema;
    }

    /**
     * Referenced table schema.
     *
     * @param referencedTableSchema the schema name of the reference table
     * @return created table foreign key builder
     */
    public CreateTableForeignKeyBuilder referencedTableSchema(String referencedTableSchema) {
        if (logger.isTraceEnabled()) {
            logger.trace("setReferencedTableSchema: " + referencedTableSchema);
        }
        this.referencedTableSchema = referencedTableSchema;
        return this;
    }
}
