/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.db;

import org.eclipse.dirigible.components.database.NamedParameterStatement;

import java.sql.PreparedStatement;

/**
 * The Class IndexedOrNamedStatement.
 */
public class IndexedOrNamedStatement {

    /** The indexed. */
    private PreparedStatement indexed;

    /** The named. */
    private NamedParameterStatement named;

    /**
     * Instantiates a new indexed or named statement.
     *
     * @param indexed the indexed
     */
    public IndexedOrNamedStatement(PreparedStatement indexed) {
        super();
        this.indexed = indexed;
    }

    /**
     * Instantiates a new indexed or named statement.
     *
     * @param named the named
     */
    public IndexedOrNamedStatement(NamedParameterStatement named) {
        super();
        this.named = named;
    }

    /**
     * Gets the indexed.
     *
     * @return the indexed
     */
    public PreparedStatement getIndexed() {
        return indexed;
    }

    /**
     * Gets the named.
     *
     * @return the named
     */
    public NamedParameterStatement getNamed() {
        return named;
    }

    /**
     * Checks if is indexed.
     *
     * @return true, if is indexed
     */
    public boolean isIndexed() {
        return this.indexed != null;
    }

    /**
     * Sets the indexed.
     *
     * @param indexed the indexed to set
     */
    public void setIndexed(PreparedStatement indexed) {
        this.indexed = indexed;
    }

    /**
     * Checks if is named.
     *
     * @return true, if is named
     */
    public boolean isNamed() {
        return this.named != null;
    }

    /**
     * Sets the named.
     *
     * @param named the named to set
     */
    public void setNamed(NamedParameterStatement named) {
        this.named = named;
    }

    @Override
    public String toString() {
        return "IndexedOrNamedStatement{" + "indexed=" + indexed + ", named=" + named + '}';
    }
}
