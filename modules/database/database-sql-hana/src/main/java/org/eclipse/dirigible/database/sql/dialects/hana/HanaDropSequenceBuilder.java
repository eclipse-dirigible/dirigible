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
import org.eclipse.dirigible.database.sql.builders.sequence.DropSequenceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class HanaDropSequenceBuilder.
 */
public class HanaDropSequenceBuilder extends DropSequenceBuilder {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(HanaDropSequenceBuilder.class);

    /**
     * Instantiates a new drop sequence builder.
     *
     * @param dialect the dialect
     * @param sequence the sequence
     */
    public HanaDropSequenceBuilder(ISqlDialect dialect, String sequence) {
        super(dialect, sequence, KEYWORD_DATABASE_DROP_RESTRICT);
    }

    /**
     * Generate.
     *
     * @return the string
     */
    @Override
    public String generate() {

        StringBuilder sql = new StringBuilder();

        // DROP
        generateDrop(sql);

        // SEQUENCE
        generateSequence(sql);

        // RESTRICT
        generateDropOption(sql);

        String generated = sql.toString();

        if (logger.isTraceEnabled()) {
            logger.trace("generated: " + generated);
        }

        return generated;
    }

}
