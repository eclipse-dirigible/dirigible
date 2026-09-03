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
import org.eclipse.dirigible.database.sql.builders.DropBranchingBuilder;

/**
 * The MSSQL Drop Branching Builder.
 */
public class MSSQLDropBranchingBuilder extends DropBranchingBuilder {

    /**
     * Instantiates a new MSSQL drop branching builder.
     *
     * @param dialect the dialect
     */
    public MSSQLDropBranchingBuilder(ISqlDialect dialect) {
        super(dialect);
    }

    /**
     * User.
     *
     * @param userId the user id
     * @return the mssql drop user builder
     */
    @Override
    public MSSQLDropUserBuilder user(String userId) {
        return new MSSQLDropUserBuilder(this.getDialect(), userId);
    }

}
