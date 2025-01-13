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
import org.eclipse.dirigible.database.sql.builders.AbstractCreateSqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Create Sequence Builder.
 */
public class CreateSequenceBuilder extends AbstractCreateSqlBuilder {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(CreateSequenceBuilder.class);

    /** The sequence. */
    private final String sequence;

    /** The start. */
    private Integer start;

    /** The increment. */
    private Integer increment;

    /** The maxvalue. */
    private Integer maxvalue;

    /** The nomaxvalue. */
    private Boolean nomaxvalue;

    /** The minvalue. */
    private Integer minvalue;

    /** The nominvalue. */
    private Boolean nominvalue;

    /** The cycles. */
    private Boolean cycles;

    /** The reset by. */
    private String resetBy;

    /** The publicc. */
    private Boolean publicc;

    /**
     * Instantiates a new creates the sequence builder.
     *
     * @param dialect the dialect
     * @param sequence the sequence
     */
    public CreateSequenceBuilder(ISqlDialect dialect, String sequence) {
        super(dialect);
        this.sequence = sequence;
    }

    /**
     * Gets the sequence.
     *
     * @return the sequence
     */
    public String getSequence() {
        return sequence;
    }

    /**
     * Gets the start.
     *
     * @return the start
     */
    public Integer getStart() {
        return start;
    }

    /**
     * Gets the increment.
     *
     * @return the increment
     */
    public Integer getIncrement() {
        return increment;
    }

    /**
     * Start.
     *
     * @param start the start
     * @return the creates the sequence builder
     */
    public CreateSequenceBuilder start(Integer start) {
        if (logger.isTraceEnabled()) {
            logger.trace("start: " + start);
        }
        this.start = start;
        return this;
    }

    /**
     * Increment.
     *
     * @param increment the increment
     * @return the creates the sequence builder
     */
    public CreateSequenceBuilder increment(int increment) {
        if (logger.isTraceEnabled()) {
            logger.trace("increment: " + increment);
        }
        this.increment = increment;
        return this;
    }

    /**
     * Maxvalue.
     *
     * @param maxvalue the maxvalue
     * @return the creates the sequence builder
     */
    public CreateSequenceBuilder maxvalue(Integer maxvalue) {
        if (logger.isTraceEnabled()) {
            logger.trace("maxvalue: " + maxvalue);
        }
        this.maxvalue = maxvalue;
        return this;
    }

    /**
     * Nomaxvalue.
     *
     * @param nomaxvalue the nomaxvalue
     * @return the creates the sequence builder
     */
    public CreateSequenceBuilder nomaxvalue(Boolean nomaxvalue) {
        if (logger.isTraceEnabled()) {
            logger.trace("nomaxvalue: " + nomaxvalue);
        }
        this.nomaxvalue = nomaxvalue;
        return this;
    }

    /**
     * Minvalue.
     *
     * @param minvalue the minvalue
     * @return the creates the sequence builder
     */
    public CreateSequenceBuilder minvalue(Integer minvalue) {
        if (logger.isTraceEnabled()) {
            logger.trace("minvalue: " + minvalue);
        }
        this.minvalue = minvalue;
        return this;
    }

    /**
     * Nominvalue.
     *
     * @param nominvalue the nominvalue
     * @return the creates the sequence builder
     */
    public CreateSequenceBuilder nominvalue(Boolean nominvalue) {
        if (logger.isTraceEnabled()) {
            logger.trace("nominvalue: " + nominvalue);
        }
        this.nominvalue = nominvalue;
        return this;
    }

    /**
     * Cycles.
     *
     * @param cycles the cycles
     * @return the creates the sequence builder
     */
    public CreateSequenceBuilder cycles(Boolean cycles) {
        if (logger.isTraceEnabled()) {
            logger.trace("cycles: " + cycles);
        }
        this.cycles = cycles;
        return this;
    }

    /**
     * Reset by.
     *
     * @param resetBy the reset by
     * @return the creates the sequence builder
     */
    public CreateSequenceBuilder resetBy(String resetBy) {
        if (logger.isTraceEnabled()) {
            logger.trace("resetBy: " + resetBy);
        }
        this.resetBy = resetBy;
        return this;
    }

    /**
     * Publicc.
     *
     * @param publicc the publicc
     * @return the creates the sequence builder
     */
    public CreateSequenceBuilder publicc(Boolean publicc) {
        if (logger.isTraceEnabled()) {
            logger.trace("public: " + publicc);
        }
        this.publicc = publicc;
        return this;
    }

    /**
     * Generate.
     *
     * @return the string
     */
    @Override
    public String generate() {
        StringBuilder sql = new StringBuilder();

        // CREATE
        generateCreate(sql);

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

        String generated = sql.toString();

        if (logger.isTraceEnabled()) {
            logger.trace("generated: " + generated);
        }

        return generated;
    }

    /**
     * Generate sequence.
     *
     * @param sql the sql
     */
    protected void generateSequence(StringBuilder sql) {
        String sequenceName = encapsulate(this.sequence, true);
        sql.append(SPACE)
           .append(KEYWORD_SEQUENCE)
           .append(SPACE)
           .append(sequenceName);
    }

    /**
     * Generate start.
     *
     * @param sql the sql
     */
    protected void generateStart(StringBuilder sql) {
        if (this.start != null) {
            generateSequenceParameter(sql, KEYWORD_SEQUENCE_START_WITH, String.valueOf(this.start));
        }

    }

    /**
     * Generate increment.
     *
     * @param sql the sql
     */
    protected void generateIncrement(StringBuilder sql) {
        if (this.increment != null) {
            generateSequenceParameter(sql, KEYWORD_SEQUENCE_INCREMENT_BY, String.valueOf(this.increment));
        }
    }

    /**
     * Generate maxvalue.
     *
     * @param sql the sql
     */
    protected void generateMaxvalue(StringBuilder sql) {
        if (this.maxvalue != null) {
            generateSequenceParameter(sql, KEYWORD_SEQUENCE_MAXVALUE, String.valueOf(this.maxvalue));
        }

    }

    /**
     * Generate nomaxvalue.
     *
     * @param sql the sql
     */
    protected void generateNomaxvalue(StringBuilder sql) {
        if ((this.nomaxvalue != null && this.nomaxvalue)) {
            generateSequenceParameter(sql, KEYWORD_SEQUENCE_NO_MAXVALUE, KEYWORD_SEQUENCE_EMPTY_STRING_FOR_BOOLEAN_TYPES);
        }

    }

    /**
     * Generate minvalue.
     *
     * @param sql the sql
     */
    protected void generateMinvalue(StringBuilder sql) {
        if (this.minvalue != null) {
            generateSequenceParameter(sql, KEYWORD_SEQUENCE_MINVALUE, String.valueOf(this.minvalue));
        }
    }

    /**
     * Generate nominvalue.
     *
     * @param sql the sql
     */
    protected void generateNominvalue(StringBuilder sql) {
        if ((this.nominvalue != null && this.nominvalue)) {
            generateSequenceParameter(sql, KEYWORD_SEQUENCE_NO_MINVALUE, KEYWORD_SEQUENCE_EMPTY_STRING_FOR_BOOLEAN_TYPES);
        }
    }

    /**
     * Generate cycle.
     *
     * @param sql the sql
     */
    protected void generateCycle(StringBuilder sql) {
        if (this.cycles != null && this.cycles) {
            generateSequenceParameter(sql, KEYWORD_SEQUENCE_CYCLE, KEYWORD_SEQUENCE_EMPTY_STRING_FOR_BOOLEAN_TYPES);
        }

    }

    /**
     * Generate reset by.
     *
     * @param sql the sql
     */
    protected void generateResetBy(StringBuilder sql) {
        if (this.resetBy != null) {
            generateSequenceParameter(sql, KEYWORD_SEQUENCE_RESET_BY, this.resetBy);
        }

    }

    /**
     * Generate sequence parameter.
     *
     * @param sql the sql
     * @param parameterName the parameter name
     * @param parameterValue the parameter value
     */
    protected void generateSequenceParameter(StringBuilder sql, String parameterName, String parameterValue) {
        sql.append(SPACE)
           .append(parameterName)
           .append(SPACE)
           .append(parameterValue);
    }

}
