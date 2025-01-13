/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.hana;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.CreateBranchingBuilder;
import org.eclipse.dirigible.database.sql.builders.synonym.CreateSynonymBuilder;

/**
 * The HANA Create Branching Builder.
 */
public class HanaCreateBranchingBuilder extends CreateBranchingBuilder {

    /**
     * Instantiates a new HANA create branching builder.
     *
     * @param dialect the dialect
     */
    protected HanaCreateBranchingBuilder(ISqlDialect dialect) {
        super(dialect);
    }

    /**
     * Table.
     *
     * @param table the table
     * @return the hana create table builder
     */
    @Override
    public HanaCreateTableBuilder table(String table) {
        return new HanaCreateTableBuilder(this.getDialect(), table, ISqlDialect.KEYWORD_COLUMN);
    }

    /**
     * Table.
     *
     * @param table the table
     * @param tableType the table type
     * @return the hana create table builder
     */
    @Override
    public HanaCreateTableBuilder table(String table, String tableType) {
        if (tableType.equalsIgnoreCase(KEYWORD_COLUMN) || tableType.equalsIgnoreCase(KEYWORD_COLUMNSTORE)
                || tableType.equalsIgnoreCase(KEYWORD_ROW) || tableType.equalsIgnoreCase(KEYWORD_ROWSTORE)
                || tableType.equalsIgnoreCase(KEYWORD_GLOBAL_TEMPORARY) || tableType.equalsIgnoreCase(KEYWORD_GLOBAL_TEMPORARY_COLUMN)) {
            return new HanaCreateTableBuilder(this.getDialect(), table, tableType);
        } else {
            throw new IllegalStateException(String.format("Unsupported table type is defined for table %s", table));
        }
    }

    /**
     * Temporary table.
     *
     * @param table the table
     * @return the hana create temporary table builder
     */
    public HanaCreateTemporaryTableBuilder temporaryTable(String table) {
        return new HanaCreateTemporaryTableBuilder(this.getDialect(), table);
    }


    /**
     * Column table.
     *
     * @param table the table
     * @return the creates the table builder
     */
    public HanaCreateTableBuilder columnTable(String table) {
        return new HanaCreateTableBuilder(this.getDialect(), table, ISqlDialect.KEYWORD_COLUMN);
    }

    /**
     * Row table.
     *
     * @param table the table
     * @return the creates the table builder
     */
    public HanaCreateTableBuilder rowTable(String table) {
        return new HanaCreateTableBuilder(this.getDialect(), table, ISqlDialect.KEYWORD_ROW);
    }

    /**
     * Table type.
     *
     * @param tableType the table type
     * @return the hana create table type builder
     */
    public HanaCreateTableTypeBuilder tableType(String tableType) {
        return new HanaCreateTableTypeBuilder(this.getDialect(), tableType);
    }

    /**
     * Public synonym.
     *
     * @param synonym the synonym
     * @return the creates the synonym builder
     */
    @Override
    public CreateSynonymBuilder publicSynonym(String synonym) {
        return new HanaCreatePublicSynonymBuilder(getDialect(), synonym);
    }
}
