/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.builders.user;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.AbstractSqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class AlterUserBuilder - changes the password of an existing user.
 */
public class AlterUserBuilder extends AbstractSqlBuilder {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(AlterUserBuilder.class);

    /** The user id. */
    private final String userId;

    /** The password. */
    private final String password;

    /**
     * Instantiates a new alter user builder.
     *
     * @param dialect the dialect
     * @param userId the user id
     * @param password the new password
     */
    public AlterUserBuilder(ISqlDialect dialect, String userId, String password) {
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
        // The user, never the statement: the statement carries the new password in clear text, and a
        // trace log is exactly the wrong place for it to end up.
        logger.trace("generating an alter user statement for [{}]", userId);

        return generateAlterUserStatement(userId, password);
    }

    /**
     * Generate alter user statement.
     *
     * <p>
     * A dialect whose credentials live on a different object than the user overrides this - see the
     * MSSQL dialect, where the password belongs to the server login rather than to the database user.
     *
     * @param user the user
     * @param pass the new password
     * @return the string
     */
    protected String generateAlterUserStatement(String user, String pass) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER USER ")
           .append(encapsulateIdentifier(user))
           .append(SPACE)
           .append("PASSWORD ")
           .append(encapsulateLiteral(pass, getPasswordEscapeSymbol()));
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

    /**
     * Gets the user id.
     *
     * @return the user id
     */
    public String getUserId() {
        return userId;
    }

}
