/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.builders.table;

import org.eclipse.dirigible.database.sql.ISqlDialect;

/**
 * The Class CreateTemporaryTableBuilder.
 *
 * @param <TABLE_BUILDER> the generic type
 */
public class CreateTemporaryTableBuilder<TABLE_BUILDER extends CreateTemporaryTableBuilder> extends AbstractTableBuilder<TABLE_BUILDER> {

    /** The like table. */
    protected String likeTable;

    /** The as select query. */
    protected String asSelectQuery;

    /** The select with no data. */
    protected boolean selectWithNoData;

    /** The Constant TEMPORARY_TABLES_NOT_SUPPORTED_FOR_THIS_DATABASE_TYPE. */
    private static final String TEMPORARY_TABLES_NOT_SUPPORTED_FOR_THIS_DATABASE_TYPE =
            "Temporary tables not supported for this Database type!";

    /**
     * Instantiates a new abstract sql builder.
     *
     * @param dialect the dialect
     * @param table the table
     */
    public CreateTemporaryTableBuilder(ISqlDialect dialect, String table) {
        super(dialect, table);
        this.selectWithNoData = false;
    }

    /**
     * Generate.
     *
     * @return the string
     */
    @Override
    public String generate() {
        throw new IllegalStateException(TEMPORARY_TABLES_NOT_SUPPORTED_FOR_THIS_DATABASE_TYPE);
    }

    /**
     * Sets the like table.
     *
     * @param likeTable the like table
     * @return the creates the temporary table builder
     */
    public CreateTemporaryTableBuilder<TABLE_BUILDER> setLikeTable(String likeTable) {
        this.likeTable = likeTable;
        return this;
    }

    /**
     * Sets the as select query.
     *
     * @param asSelectQuery the as select query
     * @return the creates the temporary table builder
     */
    public CreateTemporaryTableBuilder<TABLE_BUILDER> setAsSelectQuery(String asSelectQuery) {
        this.asSelectQuery = asSelectQuery;
        return this;
    }

    /**
     * Sets the select with no data.
     *
     * @param selectWithNoData the select with no data
     * @return the creates the temporary table builder
     */
    public CreateTemporaryTableBuilder<TABLE_BUILDER> setSelectWithNoData(boolean selectWithNoData) {
        this.selectWithNoData = selectWithNoData;
        return this;
    }
}
