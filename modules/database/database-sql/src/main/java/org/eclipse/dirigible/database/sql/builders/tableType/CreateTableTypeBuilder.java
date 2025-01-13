/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.builders.tableType;

import org.eclipse.dirigible.database.sql.DataType;
import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.AbstractCreateSqlBuilder;

/**
 * The Create Table Type Builder.
 */
public class CreateTableTypeBuilder extends AbstractCreateSqlBuilder {

    /**
     * Instantiates a new creates the table builder.
     *
     * @param dialect the dialect
     * @param tableType the tableType
     */
    public CreateTableTypeBuilder(ISqlDialect dialect, String tableType) {
        super(dialect);
    }

    /**
     * Generate.
     *
     * @return the string
     */
    @Override
    public String generate() {
        throw new IllegalStateException("Table Type is not supported for this dialect!");
    }

    /**
     * Column.
     *
     * @param name the name
     * @param type the type
     * @return the creates the table type builder
     */
    public CreateTableTypeBuilder column(String name, DataType type) {
        throw new IllegalStateException("Table Type is not supported for this dialect!");
    }

    /**
     * Column.
     *
     * @param name the name
     * @param type the type
     * @param length the length
     * @return the creates the table type builder
     */
    public CreateTableTypeBuilder column(String name, DataType type, String length) {
        throw new IllegalStateException("Table Type is not supported for this dialect!");
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
    public CreateTableTypeBuilder column(String name, DataType type, Boolean isPrimaryKey, Boolean isNullable, String args) {
        throw new IllegalStateException("Table Type is not supported for this dialect!");
    }
}
