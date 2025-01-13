/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.anonymize.domain;

/**
 * The Class DataAnonymizeParameters.
 */
public class DataAnonymizeParameters {

    /** The datasource. */
    private String datasource;

    /** The schema. */
    private String schema;

    /** The table. */
    private String table;

    /** The column. */
    private String column;

    /** The primaryKey. */
    private String primaryKey;

    /** The type. */
    private String type;

    /**
     * Gets the datasource.
     *
     * @return the datasource
     */
    public String getDatasource() {
        return datasource;
    }

    /**
     * Sets the datasource.
     *
     * @param datasource the datasource to set
     */
    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    /**
     * Gets the schema.
     *
     * @return the schema
     */
    public String getSchema() {
        return schema;
    }

    /**
     * Sets the schema.
     *
     * @param schema the schema to set
     */
    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * Gets the table.
     *
     * @return the table
     */
    public String getTable() {
        return table;
    }

    /**
     * Sets the table.
     *
     * @param table the table to set
     */
    public void setTable(String table) {
        this.table = table;
    }

    /**
     * Gets the column.
     *
     * @return the column
     */
    public String getColumn() {
        return column;
    }

    /**
     * Sets the column.
     *
     * @param column the column to set
     */
    public void setColumn(String column) {
        this.column = column;
    }

    /**
     * Gets the primaryKey.
     *
     * @return the primaryKey
     */
    public String getPrimaryKey() {
        return primaryKey;
    }

    /**
     * Sets the primaryKey.
     *
     * @param primaryKey the primaryKey to set
     */
    public void setPrimaryKey(String primaryKey) {
        this.primaryKey = primaryKey;
    }

    /**
     * Gets the type.
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type.
     *
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }



}
