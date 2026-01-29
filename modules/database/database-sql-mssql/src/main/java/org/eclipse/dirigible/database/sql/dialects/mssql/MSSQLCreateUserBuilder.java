/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.mssql;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.user.CreateUserBuilder;

public class MSSQLCreateUserBuilder extends CreateUserBuilder {

    public MSSQLCreateUserBuilder(ISqlDialect dialect, String userId, String password) {
        super(dialect, userId, password);
    }

    @Override
    protected String generateCreateUserStatement(String user, String pass) {
        // create server login
        return "CREATE LOGIN " + getEscapeSymbol() + user + getEscapeSymbol() + SPACE + "WITH PASSWORD =" + getPasswordEscapeSymbol() + pass
                + getPasswordEscapeSymbol() + "; "

                // create user mapped to the login
                + "CREATE USER " + getEscapeSymbol() + user + getEscapeSymbol() + SPACE + "FOR LOGIN " + getEscapeSymbol() + user
                + getEscapeSymbol();
    }
}
