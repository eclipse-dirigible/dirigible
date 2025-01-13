/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.snowflake;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.CreateBranchingBuilder;

/**
 * The Snowflake Create Branching Builder.
 */
public class SnowflakeCreateBranchingBuilder extends CreateBranchingBuilder {

    /**
     * Instantiates a new Snowflake create branching builder.
     *
     * @param dialect the dialect
     */
    protected SnowflakeCreateBranchingBuilder(ISqlDialect dialect) {
        super(dialect);
    }

    /**
     * Table.
     *
     * @param table the table
     * @return the Snowflake create table builder
     */
    @Override
    public SnowflakeCreateTableBuilder table(String table) {
        return new SnowflakeCreateTableBuilder(this.getDialect(), table, ISqlDialect.KEYWORD_HYBRID);
    }

    /**
     * Hybrid table.
     *
     * @param table the table
     * @return the creates the table builder
     */
    public SnowflakeCreateTableBuilder hybridTable(String table) {
        return new SnowflakeCreateTableBuilder(this.getDialect(), table, ISqlDialect.KEYWORD_HYBRID);
    }

    /**
     * Dynamic table.
     *
     * @param table the table
     * @return the creates the table builder
     */
    public SnowflakeCreateTableBuilder dynamicTable(String table) {
        return new SnowflakeCreateTableBuilder(this.getDialect(), table, ISqlDialect.KEYWORD_DYNAMIC);
    }

    /**
     * Event table.
     *
     * @param table the table
     * @return the creates the table builder
     */
    public SnowflakeCreateTableBuilder eventTable(String table) {
        return new SnowflakeCreateTableBuilder(this.getDialect(), table, ISqlDialect.KEYWORD_EVENT);
    }

    /**
     * External table.
     *
     * @param table the table
     * @return the creates the table builder
     */
    public SnowflakeCreateTableBuilder externalTable(String table) {
        return new SnowflakeCreateTableBuilder(this.getDialect(), table, ISqlDialect.KEYWORD_EXTERNAL);
    }

    /**
     * Iceberg table.
     *
     * @param table the table
     * @return the creates the table builder
     */
    public SnowflakeCreateTableBuilder icebergTable(String table) {
        return new SnowflakeCreateTableBuilder(this.getDialect(), table, ISqlDialect.KEYWORD_ICEBERG);
    }

}
