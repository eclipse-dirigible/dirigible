/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.structures.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * The Class TableConstraintPrimaryKey.
 */
@Entity
@jakarta.persistence.Table(name = "DIRIGIBLE_DATA_TABLE_PRIMARYKEYS")
public class TableConstraintPrimaryKey extends TableConstraint {

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PRIMARYKEY_ID", nullable = false)
    private Long id;

    /**
     * Instantiates a new table constraint primary key.
     *
     * @param name the name
     * @param modifiers the modifiers
     * @param columns the columns
     * @param constraints the constraints
     */
    public TableConstraintPrimaryKey(String name, String[] modifiers, String[] columns, TableConstraints constraints) {
        super(name, modifiers, columns, constraints);
        this.getConstraints()
            .setPrimaryKey(this);
    }

    /**
     * Instantiates a new table constraint primary key.
     */
    public TableConstraintPrimaryKey() {
        super();
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "TableConstraintPrimaryKey [id=" + id + ", name=" + name + ", modifiers=" + modifiers + ", columns=" + columns
                + ", constraints.table=" + constraints.getTable()
                                                      .getName()
                + "]";
    }

}
