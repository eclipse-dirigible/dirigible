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
import org.eclipse.dirigible.database.sql.builders.AbstractDropSqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class DropUserBuilder.
 */
public class DropUserBuilder extends AbstractDropSqlBuilder {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(DropUserBuilder.class);

    /** The user id. */
    private final String userId;

    /**
     * Instantiates a new drop user builder.
     *
     * @param dialect the dialect
     * @param userId the user id
     */
    public DropUserBuilder(ISqlDialect dialect, String userId) {
        super(dialect);
        this.userId = userId;
    }

    /**
     * Generate.
     *
     * @return the string
     */
    @Override
    public String generate() {
        String generated = generateDropUserStatement(userId);

        // Safe to log in full - a drop statement carries an identifier and no credential.
        logger.trace("generated: {}", generated);

        return generated;
    }

    /**
     * Generate drop user statement.
     *
     * <p>
     * A dialect that creates a user as more than one object overrides this to remove all of them - see
     * the MSSQL dialect, where a user is a database principal plus a server login.
     *
     * @param user the user
     * @return the string
     */
    protected String generateDropUserStatement(String user) {
        return "DROP USER " + encapsulateIdentifier(user);
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
