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
import org.eclipse.dirigible.components.base.converters.ArrayOfStringsToCsvConverter;

import java.util.Arrays;

/**
 * The Class TableConstraintForeignKey.
 */
@Entity
@jakarta.persistence.Table(name = "DIRIGIBLE_DATA_TABLE_FOREIGNKEYS")
public class TableConstraintForeignKey extends TableConstraint {

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FOREIGNKEY_ID", nullable = false)
    private Long id;

    /** The referenced table. */
    @Column(name = "FOREIGNKEY_REF_TABLE", columnDefinition = "VARCHAR", nullable = true, length = 255)
    @Nullable
    @Expose
    private String referencedTable;

    /** The referenced schema. */
    @Column(name = "FOREIGNKEY_REF_SCHEMA", columnDefinition = "VARCHAR", nullable = true, length = 255)
    @Nullable
    @Expose
    private String referencedSchema;

    /** The referenced columns. */
    @Column(name = "FOREIGNKEY_REF_COLUMNS", columnDefinition = "VARCHAR", nullable = true, length = 2000)
    @Nullable
    // @ElementCollection
    // @OrderColumn
    @Convert(converter = ArrayOfStringsToCsvConverter.class)
    @Expose
    private String[] referencedColumns;

    /**
     * Instantiates a new table constraint foreign key.
     *
     * @param referencedTable the referenced table
     * @param referencedSchema the referenced schema
     * @param columnName the column name
     * @param referencedColumnName the referenced column name
     * @param constraints the constraints
     */
    public TableConstraintForeignKey(String referencedTable, String referencedSchema, String columnName, String referencedColumnName,
            TableConstraints constraints) {
        this(constraints.getTable()
                        .getName()
                + "_" + referencedTable, null, new String[] {columnName}, referencedTable, referencedSchema,
                new String[] {referencedColumnName}, constraints);
    }

    /**
     * Instantiates a new table constraint foreign key.
     *
     * @param name the name
     * @param modifiers the modifiers
     * @param columns the columns
     * @param referencedTable the referenced table
     * @param referencedSchema the referenced schema
     * @param referencedColumns the referenced columns
     * @param constraints the constraints
     */
    public TableConstraintForeignKey(String name, String[] modifiers, String[] columns, String referencedTable, String referencedSchema,
            String[] referencedColumns, TableConstraints constraints) {
        super(name, modifiers, columns, constraints);
        this.referencedTable = referencedTable;
        this.referencedSchema = referencedSchema;
        this.referencedColumns = referencedColumns;
        this.constraints.getForeignKeys()
                        .add(this);
    }

    /**
     * Instantiates a new table constraint foreign key.
     */
    public TableConstraintForeignKey() {
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
     * Gets the referenced table.
     *
     * @return the referencedTable
     */
    public String getReferencedTable() {
        return referencedTable;
    }

    /**
     * Sets the referenced table.
     *
     * @param referencedTable the referencedTable to set
     */
    public void setReferencedTable(String referencedTable) {
        this.referencedTable = referencedTable;
    }

    /**
     * Gets the referenced schema.
     *
     * @return the referenced schema
     */
    public String getReferencedSchema() {
        return referencedSchema;
    }

    /**
     * Sets the referenced schema.
     *
     * @param referencedSchema the new referenced schema
     */
    public void setReferencedSchema(String referencedSchema) {
        this.referencedSchema = referencedSchema;
    }

    /**
     * Gets the referenced columns.
     *
     * @return the referencedColumns
     */
    public String[] getReferencedColumns() {
        return referencedColumns;
    }

    /**
     * Sets the referenced columns.
     *
     * @param referencedColumns the referencedColumns to set
     */
    public void setReferencedColumns(String[] referencedColumns) {
        this.referencedColumns = referencedColumns;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "TableConstraintForeignKey [id=" + id + ", referencedTable=" + referencedTable + ", referencedColumns="
                + Arrays.toString(referencedColumns) + ", name=" + name + ", modifiers=" + Arrays.toString(modifiers) + ", columns="
                + Arrays.toString(columns) + ", constraints.table="
                + (null == constraints ? null
                        : (null == constraints.getTable() ? null
                                : constraints.getTable()
                                             .getName()))
                + "]";
    }

    public void addReferencedColumns(String[] referencedColumns) {
        if (referencedColumns == null || referencedColumns.length == 0) {
            return;
        }

        if (null == this.referencedColumns) {
            this.referencedColumns = Arrays.copyOf(referencedColumns, referencedColumns.length);
        } else {
            int oldLength = this.referencedColumns.length;
            int newLength = oldLength + referencedColumns.length;
            this.referencedColumns = Arrays.copyOf(this.referencedColumns, newLength);
            System.arraycopy(referencedColumns, 0, this.referencedColumns, oldLength, referencedColumns.length);
        }
    }
}
