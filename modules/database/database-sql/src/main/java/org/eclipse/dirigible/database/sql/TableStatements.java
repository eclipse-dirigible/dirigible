/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.sql;

import java.util.Collection;
import java.util.Objects;

/**
 * Table object containing SQL statements needed to create it.
 */
public class TableStatements {

    /** The create table statement. */
    private final String createTableStatement;

    /** The create indices statements. */
    private final Collection<String> createIndicesStatements;

    /**
     * Instantiates a new table.
     *
     * @param createTableStatement the create table statement
     * @param createIndicesStatements the create indices statements
     */
    public TableStatements(String createTableStatement, Collection<String> createIndicesStatements) {
        this.createTableStatement = createTableStatement;
        this.createIndicesStatements = createIndicesStatements;
    }

    /**
     * Gets the creates the table statement.
     *
     * @return the creates the table statement
     */
    public String getCreateTableStatement() {
        return createTableStatement;
    }

    /**
     * Gets the creates the indices statements.
     *
     * @return the creates the indices statements
     */
    public Collection<String> getCreateIndicesStatements() {
        return createIndicesStatements;
    }

    /**
     * Equals.
     *
     * @param o the o
     * @return true, if successful
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TableStatements table = (TableStatements) o;
        return createTableStatement.equals(table.createTableStatement) && createIndicesStatements.equals(table.createIndicesStatements);
    }

    /**
     * Hash code.
     *
     * @return the int
     */
    @Override
    public int hashCode() {
        return Objects.hash(createTableStatement, createIndicesStatements);
    }
}
