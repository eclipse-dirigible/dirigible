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
import org.eclipse.dirigible.database.sql.builders.user.CreateUserBuilder;

public class MSSQLCreateUserBuilder extends CreateUserBuilder {

    public MSSQLCreateUserBuilder(ISqlDialect dialect, String userId, String password) {
        super(dialect, userId, password);
    }

    @Override
    protected String generateCreateUserStatement(String user, String pass) {
        String name = encapsulateIdentifier(user);

        // create server login
        return "CREATE LOGIN " + name + SPACE + "WITH PASSWORD =" + encapsulateLiteral(pass, getPasswordEscapeSymbol()) + "; "

        // create user mapped to the login
                + "CREATE USER " + name + SPACE + "FOR LOGIN " + name;
    }
}
