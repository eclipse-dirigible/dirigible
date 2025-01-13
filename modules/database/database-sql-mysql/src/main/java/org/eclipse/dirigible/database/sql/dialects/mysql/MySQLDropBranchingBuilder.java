/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.mysql;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.DropBranchingBuilder;
import org.eclipse.dirigible.database.sql.builders.sequence.DropSequenceBuilder;

/**
 * The MySQL Drop Branching Builder.
 */
public class MySQLDropBranchingBuilder extends DropBranchingBuilder {

    /**
     * Instantiates a new MySQL create branching builder.
     *
     * @param dialect the dialect
     */
    public MySQLDropBranchingBuilder(ISqlDialect dialect) {
        super(dialect);
    }

    /**
     * Sequence.
     *
     * @param sequence the sequence
     * @return the drop sequence builder
     */
    @Override
    public DropSequenceBuilder sequence(String sequence) {
        return new MySQLDropSequenceBuilder(this.getDialect(), sequence);
    }

}
