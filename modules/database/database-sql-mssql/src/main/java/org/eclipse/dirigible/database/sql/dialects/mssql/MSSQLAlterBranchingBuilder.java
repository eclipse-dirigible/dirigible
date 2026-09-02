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
import org.eclipse.dirigible.database.sql.builders.AlterBranchingBuilder;

/**
 * The MSSQL Alter Branching Builder.
 */
public class MSSQLAlterBranchingBuilder extends AlterBranchingBuilder {

    /**
     * Instantiates a new MSSQL alter branching builder.
     *
     * @param dialect the dialect
     */
    public MSSQLAlterBranchingBuilder(ISqlDialect dialect) {
        super(dialect);
    }

    /**
     * User.
     *
     * @param userId the user id
     * @param password the new password
     * @return the mssql alter user builder
     */
    @Override
    public MSSQLAlterUserBuilder user(String userId, String password) {
        return new MSSQLAlterUserBuilder(this.getDialect(), userId, password);
    }

}
