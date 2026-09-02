/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.h2;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.user.AlterUserBuilder;

/**
 * The Class H2AlterUserBuilder.
 */
public class H2AlterUserBuilder extends AlterUserBuilder {

    /**
     * Instantiates a new H2 alter user builder.
     *
     * @param dialect the dialect
     * @param userId the user id
     * @param password the new password
     */
    public H2AlterUserBuilder(ISqlDialect dialect, String userId, String password) {
        super(dialect, userId, password);
    }

    /**
     * Generate alter user statement.
     *
     * <p>
     * H2 spells the assignment out - {@code SET PASSWORD} - where PostgreSQL takes the bare
     * {@code PASSWORD} the default builder emits.
     *
     * @param user the user
     * @param pass the new password
     * @return the string
     */
    @Override
    protected String generateAlterUserStatement(String user, String pass) {
        return "ALTER USER " + encapsulateIdentifier(user) + SPACE + "SET PASSWORD " + encapsulateLiteral(pass, getPasswordEscapeSymbol());
    }
}
