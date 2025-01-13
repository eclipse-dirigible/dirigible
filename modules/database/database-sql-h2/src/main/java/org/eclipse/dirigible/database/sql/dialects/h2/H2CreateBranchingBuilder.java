/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.h2;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.CreateBranchingBuilder;

/**
 * The H2 Create Branching Builder.
 */
public class H2CreateBranchingBuilder extends CreateBranchingBuilder {

    /**
     * Instantiates a new H2 create branching builder.
     *
     * @param dialect the dialect
     */
    protected H2CreateBranchingBuilder(ISqlDialect dialect) {
        super(dialect);
    }

    /**
     * Table.
     *
     * @param table the table
     * @return the h2 create table builder
     */
    @Override
    public H2CreateTableBuilder table(String table) {
        return new H2CreateTableBuilder(this.getDialect(), table);
    }

}
