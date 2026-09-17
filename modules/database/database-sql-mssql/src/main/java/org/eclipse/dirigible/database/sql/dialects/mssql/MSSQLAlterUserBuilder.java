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
import org.eclipse.dirigible.database.sql.builders.user.AlterUserBuilder;

/**
 * The Class MSSQLAlterUserBuilder.
 */
public class MSSQLAlterUserBuilder extends AlterUserBuilder {

    /**
     * Instantiates a new MSSQL alter user builder.
     *
     * @param dialect the dialect
     * @param userId the user id
     * @param password the new password
     */
    public MSSQLAlterUserBuilder(ISqlDialect dialect, String userId, String password) {
        super(dialect, userId, password);
    }

    /**
     * Generate alter user statement.
     *
     * <p>
     * {@code ALTER LOGIN}, not {@code ALTER USER}: on SQL Server the password belongs to the server
     * login, and the database user of the same name has none to change.
     *
     * @param user the user
     * @param pass the new password
     * @return the string
     */
    @Override
    protected String generateAlterUserStatement(String user, String pass) {
        return "ALTER LOGIN " + encapsulateIdentifier(user) + SPACE + "WITH PASSWORD ="
                + encapsulateLiteral(pass, getPasswordEscapeSymbol());
    }
}
