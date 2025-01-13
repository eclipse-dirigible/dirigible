/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.hana;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.AlterBranchingBuilder;
import org.eclipse.dirigible.database.sql.builders.table.AlterTableBuilder;

/**
 * The Class HanaAlterBranchingBuilder.
 */
public class HanaAlterBranchingBuilder extends AlterBranchingBuilder {
    /**
     * Instantiates a new creates the branching builder.
     *
     * @param dialect the dialect
     */
    public HanaAlterBranchingBuilder(ISqlDialect dialect) {
        super(dialect);
    }

    /**
     * Table.
     *
     * @param table the table
     * @return the alter table builder
     */
    @Override
    public AlterTableBuilder table(String table) {
        return new HanaAlterTableBuilder(getDialect(), table);
    }
}
