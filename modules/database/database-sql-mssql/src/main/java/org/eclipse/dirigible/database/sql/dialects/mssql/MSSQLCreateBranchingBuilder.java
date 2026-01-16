/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.mssql;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.CreateBranchingBuilder;

/**
 * The MSSQL Create Branching Builder.
 */
public class MSSQLCreateBranchingBuilder extends CreateBranchingBuilder {

    /**
     * Instantiates a new MSSQL create branching builder.
     *
     * @param dialect the dialect
     */
    public MSSQLCreateBranchingBuilder(ISqlDialect dialect) {
        super(dialect);
    }

    /**
     * View.
     *
     * @param view the view
     * @return the mssql create view builder
     */
    @Override
    public MSSQLCreateViewBuilder view(String view) {
        return new MSSQLCreateViewBuilder(this.getDialect(), view);
    }

    @Override
    public MSSQLCreateUserBuilder user(String userId, String password) {
        return new MSSQLCreateUserBuilder(getDialect(), userId, password);
    }

}
