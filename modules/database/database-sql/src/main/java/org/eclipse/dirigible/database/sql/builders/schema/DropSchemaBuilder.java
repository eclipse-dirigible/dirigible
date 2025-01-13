/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.builders.schema;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.AbstractDropSqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Create Schema Builder.
 */
public class DropSchemaBuilder extends AbstractDropSqlBuilder {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(CreateSchemaBuilder.class);

    /** The name. */
    private final String name;
    private boolean cascade = false;

    /**
     * Instantiates a new creates the schema builder.
     *
     * @param dialect the dialect
     * @param name the schema name
     */
    public DropSchemaBuilder(ISqlDialect dialect, String name) {
        super(dialect);
        this.name = name;
    }

    /**
     * Generate.
     *
     * @return the string
     */
    @Override
    public String generate() {

        StringBuilder sql = new StringBuilder();

        // DROP
        generateDrop(sql);

        // SCHEMA
        generateSchema(sql);

        if (cascade) {
            sql.append(SPACE)
               .append(KEYWORD_DATABASE_DROP_CASCADE);
        }

        String generated = sql.toString();

        if (logger.isTraceEnabled()) {
            logger.trace("generated: " + generated);
        }

        return generated;
    }

    /**
     * Generate schema.
     *
     * @param sql the sql
     */
    protected void generateSchema(StringBuilder sql) {
        String schemaName = encapsulate(this.getName(), true);
        sql.append(SPACE)
           .append(KEYWORD_SCHEMA)
           .append(SPACE)
           .append(schemaName);
    }

    /**
     * Gets the schema name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    public DropSchemaBuilder cascade(boolean cascade) {
        this.cascade = cascade;
        return this;
    }
}
