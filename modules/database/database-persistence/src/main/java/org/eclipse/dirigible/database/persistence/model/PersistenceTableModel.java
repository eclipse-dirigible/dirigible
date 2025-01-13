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

import org.eclipse.dirigible.database.sql.ISqlKeywords;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The Persistence Table Model transport object.
 */
public class PersistenceTableModel {

    /** The class name. */
    private String className;

    /** The table name. */
    private String tableName;

    /** The schema name. */
    private String schemaName;

    /** The table type. */
    private String tableType = ISqlKeywords.METADATA_TABLE;

    /** The columns. */
    private List<PersistenceTableColumnModel> columns = new ArrayList<>();

    /** The relations. */
    private List<PersistenceTableRelationModel> relations = new ArrayList<>();

    /** The relations. */
    private List<PersistenceTableIndexModel> indices = new ArrayList<>();

    /**
     * Instantiates a new persistence table model.
     *
     * @param tableName the table name
     * @param columns the columns
     * @param relations the relations
     * @param indices the indices
     */
    public PersistenceTableModel(String tableName, List<PersistenceTableColumnModel> columns, List<PersistenceTableRelationModel> relations,
            List<PersistenceTableIndexModel> indices) {
        this.tableName = tableName;
        this.columns = columns;
        this.relations = relations;
        this.indices = indices;
    }

    /**
     * Instantiates a new persistence table model.
     */
    public PersistenceTableModel() {}

    /**
     * Gets the table type.
     *
     * @return the table type
     */
    public String getTableType() {
        return tableType;
    }

    /**
     * Sets the table type.
     *
     * @param tableType the new table type
     */
    public void setTableType(String tableType) {
        this.tableType = tableType;
    }

    /**
     * Gets the class name.
     *
     * @return the class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Sets the class name.
     *
     * @param className the new class name
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Gets the table name.
     *
     * @return the table name
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Sets the table name.
     *
     * @param tableName the new table name
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Gets the schema name.
     *
     * @return the schema name
     */
    public String getSchemaName() {
        return schemaName;
    }

    /**
     * Sets the schema name.
     *
     * @param schemaName the new schema name
     */
    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    /**
     * Gets the columns.
     *
     * @return the columns
     */
    public List<PersistenceTableColumnModel> getColumns() {
        return columns;
    }

    /**
     * Sets the columns.
     *
     * @param columns the new columns
     */
    public void setColumns(List<PersistenceTableColumnModel> columns) {
        this.columns = columns;
    }

    /**
     * gets the relations.
     * <p>
     * the new columns
     *
     * @return the relations
     */
    public List<PersistenceTableRelationModel> getRelations() {
        return relations;
    }

    /**
     * Sets the relations.
     *
     * @param relations the new columns
     */
    public void setRelations(List<PersistenceTableRelationModel> relations) {
        this.relations = relations;
    }

    /**
     * Gets the indices.
     *
     * @return the indices
     */
    public List<PersistenceTableIndexModel> getIndices() {
        return indices;
    }

    /**
     * Sets the indices.
     *
     * @param indices the new indices
     */
    public void setIndices(List<PersistenceTableIndexModel> indices) {
        this.indices = indices;
    }

    /**
     * Equals.
     *
     * @param o the o
     * @return true, if successful
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PersistenceTableModel that = (PersistenceTableModel) o;
        return Objects.equals(className, that.className) && tableName.equals(that.tableName) && Objects.equals(schemaName, that.schemaName);
    }

    /**
     * Hash code.
     *
     * @return the int
     */
    @Override
    public int hashCode() {
        return Objects.hash(className, tableName, schemaName);
    }


}
