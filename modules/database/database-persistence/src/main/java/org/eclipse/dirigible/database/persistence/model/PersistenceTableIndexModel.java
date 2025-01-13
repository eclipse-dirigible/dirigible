/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.database.persistence.model;

import java.util.HashSet;
import java.util.Set;

/**
 * The relation element of the persistence model transport object.
 */
public class PersistenceTableIndexModel {

    /** The name. */
    private String name;

    /** The index type. */
    private String type;

    /** The unique. */
    private Boolean unique;

    /** The index columns. */
    private Set<String> columns = new HashSet<>();

    /**
     * Instantiates a new persistence table index model.
     */
    public PersistenceTableIndexModel() {}

    /**
     * Instantiates a new persistence table index model.
     *
     * @param name the name
     * @param type the type
     * @param unique the unique
     * @param columns the columns
     */
    public PersistenceTableIndexModel(String name, String type, Boolean unique, Set<String> columns) {
        super();
        this.name = name;
        this.type = type;
        this.unique = unique;
        this.columns = columns;
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
     * Sets the name.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the index type.
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the index type.
     *
     * @param type the index type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets the unique.
     *
     * @return the unique
     */
    public Boolean getUnique() {
        return unique;
    }

    /**
     * Sets the unique.
     *
     * @param unique the unique to set
     */
    public void setUnique(Boolean unique) {
        this.unique = unique;
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
     * @param columns the columns to set
     */
    public void setColumns(Set<String> columns) {
        this.columns = columns;
    }

}
