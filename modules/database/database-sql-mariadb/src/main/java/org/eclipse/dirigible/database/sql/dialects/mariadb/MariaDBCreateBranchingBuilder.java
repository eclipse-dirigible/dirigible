/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.dialects.mariadb;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.CreateBranchingBuilder;
import org.eclipse.dirigible.database.sql.builders.sequence.CreateSequenceBuilder;

/**
 * The MariaDB Create Branching Builder.
 */
public class MariaDBCreateBranchingBuilder extends CreateBranchingBuilder {

    /**
     * Instantiates a new MariaDB create branching builder.
     *
     * @param dialect the dialect
     */
    public MariaDBCreateBranchingBuilder(ISqlDialect dialect) {
        super(dialect);
    }

    /**
     * Sequence.
     *
     * @param sequence the sequence
     * @return the creates the sequence builder
     */
    @Override
    public CreateSequenceBuilder sequence(String sequence) {
        return new MariaDBCreateSequenceBuilder(this.getDialect(), sequence);
    }

    /**
     * View.
     *
     * @param view the view
     * @return the my SQL create view builder
     */
    @Override
    public MariaDBCreateViewBuilder view(String view) {
        return new MariaDBCreateViewBuilder(this.getDialect(), view);
    }

}
