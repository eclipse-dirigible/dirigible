/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.builders;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.SqlException;
import org.eclipse.dirigible.database.sql.builders.sequence.AlterSequenceBuilder;
import org.eclipse.dirigible.database.sql.builders.table.AlterTableBuilder;

/**
 * The Create Branching Builder.
 */
public class AlterBranchingBuilder extends AbstractSqlBuilder {

    /**
     * Instantiates a new creates the branching builder.
     *
     * @param dialect the dialect
     */
    public AlterBranchingBuilder(ISqlDialect dialect) {
        super(dialect);
    }

    /**
     * Table branch.
     *
     * @param table the table
     * @return the alters the table builder
     */
    public AlterTableBuilder table(String table) {
        return new AlterTableBuilder(getDialect(), table);
    }

    /**
     * Sequence.
     *
     * @param sequence the sequence
     * @return the alter sequence builder
     */
    public AlterSequenceBuilder sequence(String sequence) {
        return new AlterSequenceBuilder(getDialect(), sequence);
    }

    /**
     * Generate.
     *
     * @return the string
     */
    @Override
    public String generate() {
        throw new SqlException("Invalid method invocation of generate() for Create Branching Builder");
    }

}
