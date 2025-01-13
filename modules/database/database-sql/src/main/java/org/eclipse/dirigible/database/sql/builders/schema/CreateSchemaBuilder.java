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
import org.eclipse.dirigible.database.sql.builders.AbstractCreateSqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Create Schema Builder.
 */
public class CreateSchemaBuilder extends AbstractCreateSqlBuilder {

    /** The Constant AUTHORIZATION_KEYWORD. */
    private static final String AUTHORIZATION_KEYWORD = "AUTHORIZATION";

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(CreateSchemaBuilder.class);

    /** The name. */
    private final String name;

    /** The authorization. */
    private String authorization;

    /**
     * Instantiates a new creates the schema builder.
     *
     * @param dialect the dialect
     * @param name the schema name
     */
    public CreateSchemaBuilder(ISqlDialect dialect, String name) {
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

        // CREATE
        generateCreate(sql);

        // SCHEMA
        generateSchema(sql);

        generateAuthorization(sql);

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

    /**
     * Authorization.
     *
     * @param roleSpecification the role specification
     * @return the creates the schema builder
     */
    public CreateSchemaBuilder authorization(String roleSpecification) {
        this.authorization = roleSpecification;
        return this;
    }

    /**
     * Generate authorization.
     *
     * @param sql the sql
     */
    private void generateAuthorization(StringBuilder sql) {
        if (null == authorization) {
            return;
        }
        sql.append(SPACE)
           .append(AUTHORIZATION_KEYWORD)
           .append(SPACE)
           .append(getEscapeSymbol())
           .append(authorization)
           .append(getEscapeSymbol());

    }
}
