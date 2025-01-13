/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql.builders.table;

import java.util.Set;
import java.util.TreeSet;

import org.eclipse.dirigible.database.sql.ISqlDialect;
import org.eclipse.dirigible.database.sql.builders.AbstractSqlBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Abstract Create Table Constraint Builder.
 *
 * @param <CONSTRAINT> the generic type
 */
public abstract class AbstractCreateTableConstraintBuilder<CONSTRAINT extends AbstractCreateTableConstraintBuilder>
        extends AbstractSqlBuilder {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(AbstractCreateTableConstraintBuilder.class);

    /** The name. */
    private String name;

    /** The modifiers. */
    private Set<String> modifiers = new TreeSet<String>();

    /** The columns. */
    private Set<String> columns = new TreeSet<String>();

    /**
     * Instantiates a new abstract create table constraint builder.
     *
     * @param dialect the dialect
     * @param name the name
     */
    AbstractCreateTableConstraintBuilder(ISqlDialect dialect, String name) {
        super(dialect);
        this.name = name;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the modifiers.
     *
     * @return the modifiers
     */
    public Set<String> getModifiers() {
        return modifiers;
    }

    /**
     * Gets the columns.
     *
     * @return the columns
     */
    public Set<String> getColumns() {
        return columns;
    }

    /**
     * Sets the columns.
     *
     * @param columns the columns
     */
    public void setColumns(Set<String> columns) {
        this.columns = columns;
    }

    /**
     * Modifier.
     *
     * @param modifier the modifier
     * @return the constraint
     */
    public CONSTRAINT modifier(String modifier) {
        this.modifiers.add(modifier);
        return (CONSTRAINT) this;
    }

    /**
     * Column.
     *
     * @param column the column
     * @return the constraint
     */
    public CONSTRAINT column(String column) {
        if (logger.isTraceEnabled()) {
            logger.trace("column: " + column);
        }
        this.columns.add(column);
        return (CONSTRAINT) this;
    }

    /**
     * Generate.
     *
     * @return the string
     */
    @Override
    public String generate() {
        throw new IllegalStateException("Direct use of generate on the constraint level is not needed.");
    }

}
