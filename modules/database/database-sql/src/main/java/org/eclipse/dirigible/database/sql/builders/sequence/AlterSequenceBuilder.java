/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.builders.sequence;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class AlterSequenceBuilder.
 */
public class AlterSequenceBuilder extends CreateSequenceBuilder {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(AlterSequenceBuilder.class);

    /** The restart with. */
    private Integer restartWith;

    /**
     * Restart with.
     *
     * @param restartWith the restart with
     * @return the creates the sequence builder
     */
    public CreateSequenceBuilder restartWith(Integer restartWith) {
        if (logger.isTraceEnabled()) {
            logger.trace("restartWith: " + restartWith);
        }
        this.restartWith = restartWith;
        return this;
    }

    /**
     * Instantiates a new alter sequence builder.
     *
     * @param dialect the dialect
     * @param sequence the sequence
     */
    public AlterSequenceBuilder(ISqlDialect dialect, String sequence) {
        super(dialect, sequence);
    }

    /**
     * Generate reset by.
     *
     * @param sql the sql
     */
    protected void generateRestartWith(StringBuilder sql) {
        if (this.restartWith != null) {
            generateSequenceParameter(sql, KEYWORD_SEQUENCE_RESTART_WITH, String.valueOf(this.restartWith));
        }

    }

    /**
     * Generate.
     *
     * @return the string
     */
    @Override
    public String generate() {
        StringBuilder sql = new StringBuilder();

        // ALTER
        generateAlter(sql);

        // SEQUENCE
        generateSequence(sql);

        // START_WITH
        generateStart(sql);

        // INCREMENT_BY
        generateIncrement(sql);

        // MAXVALUE
        generateMaxvalue(sql);

        // NO MAXVALUE
        generateNomaxvalue(sql);

        // MINVALUE
        generateMinvalue(sql);

        // NO MINVALUE
        generateNominvalue(sql);

        // CYCLE
        generateCycle(sql);

        // RESET BY
        generateResetBy(sql);

        // RESTART WITH
        generateRestartWith(sql);

        String generated = sql.toString();

        if (logger.isTraceEnabled()) {
            logger.trace("generated: " + generated);
        }

        return generated;
    }

    /**
     * Generate start.
     *
     * @param sql the sql
     */
    @Override
    protected void generateStart(StringBuilder sql) {
        if (this.getStart() != null) {
            generateSequenceParameter(sql, KEYWORD_SEQUENCE_RESTART_WITH, String.valueOf(this.getStart()));
        }

    }
}
