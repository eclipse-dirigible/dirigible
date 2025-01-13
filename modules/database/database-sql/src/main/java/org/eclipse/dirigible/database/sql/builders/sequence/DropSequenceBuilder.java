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
import org.eclipse.dirigible.database.sql.builders.AbstractDropSqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Drop Sequence Builder.
 */
public class DropSequenceBuilder extends AbstractDropSqlBuilder {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(DropSequenceBuilder.class);

    /** The sequence. */
    private final String sequence;

    private String dropOption;

    /**
     * Instantiates a new drop sequence builder.
     *
     * @param dialect the dialect
     * @param sequence the sequence
     */
    public DropSequenceBuilder(ISqlDialect dialect, String sequence) {
        super(dialect);
        this.sequence = sequence;
    }

    public DropSequenceBuilder(ISqlDialect dialect, String sequence, String dropOption) {
        super(dialect);
        this.sequence = sequence;
        this.dropOption = dropOption;
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

        generateDropOption(sql);

        String generated = sql.toString();

        logger.trace("generated: " + generated);

        return generated;
    }

    protected void generateDropOption(StringBuilder sql) {
        if (dropOption != null) {
            sql.append(SPACE)
               .append(dropOption);
        }
    }

    /**
     * Generate sequence.
     *
     * @param sql the sql
     */
    protected void generateSequence(StringBuilder sql) {
        String sequenceName = encapsulate(this.getSequence(), true);
        sql.append(SPACE)
           .append(KEYWORD_SEQUENCE)
           .append(SPACE)
           .append(sequenceName);
    }

    /**
     * Gets the sequence.
     *
     * @return the sequence
     */
    public String getSequence() {
        return sequence;
    }

    public void setDropOption(String dropOption) {
        this.dropOption = dropOption;
    }

    public DropSequenceBuilder unsetDropOption() {
        this.dropOption = null;
        return this;
    }

}
