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
import org.eclipse.dirigible.database.sql.builders.DropBranchingBuilder;
import org.eclipse.dirigible.database.sql.builders.sequence.DropSequenceBuilder;
import org.eclipse.dirigible.database.sql.builders.synonym.DropSynonymBuilder;
import org.eclipse.dirigible.database.sql.builders.tableType.DropTableTypeBuilder;

/**
 * The Class HanaDropBranchingBuilder.
 */
public class HanaDropBranchingBuilder extends DropBranchingBuilder {

    /**
     * Instantiates a new hana drop branching builder.
     *
     * @param dialect the dialect
     */
    public HanaDropBranchingBuilder(ISqlDialect dialect) {
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
        return new HanaDropSequenceBuilder(this.getDialect(), sequence);
    }

    /**
     * Table type.
     *
     * @param tableType the table type
     * @return the drop table type builder
     */
    @Override
    public DropTableTypeBuilder tableType(String tableType) {
        return new HanaDropTableTypeBuilder(this.getDialect(), tableType);
    }

    /**
     * Public synonym.
     *
     * @param synonym the synonym
     * @return the drop synonym builder
     */
    @Override
    public DropSynonymBuilder publicSynonym(String synonym) {
        return new HanaDropPublicSynonymBuilder(getDialect(), synonym);
    }
}
