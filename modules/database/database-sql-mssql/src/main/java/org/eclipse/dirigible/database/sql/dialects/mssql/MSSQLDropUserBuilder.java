/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.mssql;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.user.DropUserBuilder;

/**
 * The Class MSSQLDropUserBuilder.
 */
public class MSSQLDropUserBuilder extends DropUserBuilder {

    /**
     * Instantiates a new MSSQL drop user builder.
     *
     * @param dialect the dialect
     * @param userId the user id
     */
    public MSSQLDropUserBuilder(ISqlDialect dialect, String userId) {
        super(dialect, userId);
    }

    /**
     * Generate drop user statement.
     *
     * <p>
     * Both halves of what {@link MSSQLCreateUserBuilder} created. Dropping only the database user would
     * leave the server login behind, and the next attempt to create the same user fails on a login that
     * already exists.
     *
     * @param user the user
     * @return the string
     */
    @Override
    protected String generateDropUserStatement(String user) {
        String name = encapsulateIdentifier(user);

        return "DROP USER " + name + "; " + "DROP LOGIN " + name;
    }
}
