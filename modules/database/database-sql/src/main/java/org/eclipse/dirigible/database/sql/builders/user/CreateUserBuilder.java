/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.builders.user;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.AbstractCreateSqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class CreateUserBuilder.
 */
public class CreateUserBuilder extends AbstractCreateSqlBuilder {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(CreateUserBuilder.class);

    /** The user id. */
    private final String userId;

    /** The password. */
    private final String password;

    /**
     * Instantiates a new creates the user builder.
     *
     * @param dialect the dialect
     * @param userId the user id
     * @param password the password
     */
    public CreateUserBuilder(ISqlDialect dialect, String userId, String password) {
        super(dialect);
        this.userId = userId;
        this.password = password;
    }

    /**
     * Generate.
     *
     * @return the string
     */
    @Override
    public String generate() {
        String generated = generateCreateUserStatement(userId, password);
        logger.trace("generated: " + generated);

        return generated;
    }

    /**
     * Generate create user statement.
     *
     * @param user the user
     * @param pass the pass
     * @return the string
     */
    protected String generateCreateUserStatement(String user, String pass) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE USER ")
           .append(getEscapeSymbol())
           .append(user)
           .append(getEscapeSymbol())
           .append(SPACE)
           .append(" PASSWORD ")
           .append(getPasswordEscapeSymbol())
           .append(pass)
           .append(getPasswordEscapeSymbol());
        return sql.toString();
    }

    /**
     * Gets the password escape symbol.
     *
     * @return the password escape symbol
     */
    protected char getPasswordEscapeSymbol() {
        return '\'';
    }

}
