/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.structures.domain;

import com.google.gson.annotations.Expose;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.util.Arrays;

/**
 * The Class TableConstraintCheck.
 */
@Entity
@jakarta.persistence.Table(name = "DIRIGIBLE_DATA_TABLE_CHECKS")
public class TableConstraintCheck extends TableConstraint {

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CHECK_ID", nullable = false)
    private Long id;

    /** The expression. */
    @Column(name = "CHECK_EXPRESSION", columnDefinition = "VARCHAR", nullable = true, length = 255)
    @Nullable
    @Expose
    private String expression;

    /**
     * Instantiates a new table constraint check.
     *
     * @param name the name
     * @param modifiers the modifiers
     * @param columns the columns
     * @param constraints the constraints
     * @param expression the expression
     */
    public TableConstraintCheck(String name, String[] modifiers, String[] columns, TableConstraints constraints, String expression) {
        super(name, modifiers, columns, constraints);
        this.expression = expression;
        this.constraints.getChecks()
                        .add(this);
    }

    /**
     * Instantiates a new table constraint check.
     */
    public TableConstraintCheck() {
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
     * Gets the expression.
     *
     * @return the expression
     */
    public String getExpression() {
        return expression;
    }

    /**
     * Sets the expression.
     *
     * @param expression the expression to set
     */
    public void setExpression(String expression) {
        this.expression = expression;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "TableConstraintCheck [id=" + id + ", expression=" + expression + ", name=" + name + ", modifiers="
                + Arrays.toString(modifiers) + ", columns=" + Arrays.toString(columns) + ", constraints.table="
                + (null == constraints ? null
                        : (null == constraints.getTable() ? null
                                : constraints.getTable()
                                             .getName()))
                + "]";
    }

}
