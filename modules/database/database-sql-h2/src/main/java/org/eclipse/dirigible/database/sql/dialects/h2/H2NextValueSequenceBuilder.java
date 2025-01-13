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

import static java.text.MessageFormat.format;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.sequence.NextValueSequenceBuilder;

/**
 * The H2 Next Value Sequence Builder.
 */
public class H2NextValueSequenceBuilder extends NextValueSequenceBuilder {

    /** The Constant PATTERN_SELECT_NEXT_VAL_SEQUENCE. */
    private static final String PATTERN_SELECT_NEXT_VAL_SEQUENCE = "SELECT NEXTVAL( ''{0}'' )";

    /**
     * Instantiates a new H2 next value sequence builder.
     *
     * @param dialect the dialect
     * @param sequence the sequence
     */
    public H2NextValueSequenceBuilder(ISqlDialect dialect, String sequence) {
        super(dialect, sequence);
    }

    /**
     * Generate.
     *
     * @return the string
     */
    @Override
    public String generate() {
        String sequenceName = this.getSequence();
        String sql = format(PATTERN_SELECT_NEXT_VAL_SEQUENCE, sequenceName);
        return sql;
    }
}
