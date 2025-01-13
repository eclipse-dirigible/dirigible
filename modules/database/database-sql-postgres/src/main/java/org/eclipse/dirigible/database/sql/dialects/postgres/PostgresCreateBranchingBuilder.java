/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.postgres;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.CreateBranchingBuilder;

/**
 * The PostgreSQL Create Branching Builder.
 */
public class PostgresCreateBranchingBuilder extends CreateBranchingBuilder {

    /**
     * Instantiates a new PostgreSQL create branching builder.
     *
     * @param dialect the dialect
     */
    public PostgresCreateBranchingBuilder(ISqlDialect dialect) {
        super(dialect);
    }

    /**
     * View.
     *
     * @param view the view
     * @return the postgres create view builder
     */
    @Override
    public PostgresCreateViewBuilder view(String view) {
        return new PostgresCreateViewBuilder(this.getDialect(), view);
    }

}
