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

/**
 * The relation element of the persistence model transport object.
 */
public class PersistenceTableRelationModel {

    /** The from table name. */
    private String fromTableName;

    /** The to table name. */
    private String toTableName;

    /** The fk column name. */
    private String fkColumnName;

    /** The pk column name. */
    private String pkColumnName;

    /** The fk name. */
    private String fkName;

    /** The pk name. */
    private String pkName;

    /**
     * Instantiates a new persistence table relation model.
     */
    public PersistenceTableRelationModel() {}

    /**
     * Instantiates a new persistence table relation model.
     *
     * @param fromTableName the from table name
     * @param toTableName the to table name
     * @param fkColumnName the fk column name
     * @param pkColumnName the pk column name
     * @param fkName the fk name
     * @param pkName the pk name
     */
    public PersistenceTableRelationModel(String fromTableName, String toTableName, String fkColumnName, String pkColumnName, String fkName,
            String pkName) {
        this.fromTableName = fromTableName;
        this.toTableName = toTableName;
        this.fkColumnName = fkColumnName;
        this.pkColumnName = pkColumnName;
        this.fkName = fkName;
        this.pkName = pkName;
    }

    /**
     * Gets the name of the table with the foreign key.
     * <p>
     * the new columns
     *
     * @return the from table name
     */
    public String getFromTableName() {
        return fromTableName;
    }

    /**
     * Sets the name of the table with the foreign key.
     *
     * @param fromTableName the new from table name
     */
    public void setFromTableName(String fromTableName) {
        this.fromTableName = fromTableName;
    }

    /**
     * Gets the name of the table with the primary key.
     *
     * @return the to table name
     */
    public String getToTableName() {
        return toTableName;
    }

    /**
     * Sets the name of the table with the primary key.
     *
     * @param toTableName the new to table name
     */
    public void setToTableName(String toTableName) {
        this.toTableName = toTableName;
    }

    /**
     * Gets the name of the foreign key column.
     *
     * @return the fk column name
     */
    public String getFkColumnName() {
        return fkColumnName;
    }

    /**
     * Sets the name of the foreign key column.
     *
     * @param fkColumnName the new fk column name
     */
    public void setFkColumnName(String fkColumnName) {
        this.fkColumnName = fkColumnName;
    }

    /**
     * Gets the name of the primary key column.
     *
     * @return the pk column name
     */
    public String getPkColumnName() {
        return pkColumnName;
    }

    /**
     * Sets the name of the primary key column.
     *
     * @param pkColumnName the new pk column name
     */
    public void setPkColumnName(String pkColumnName) {
        this.pkColumnName = pkColumnName;
    }

    /**
     * Gets the name of the foreign key.
     *
     * @return the fk name
     */
    public String getFkName() {
        return fkName;
    }

    /**
     * Sets the name of the foreign key.
     *
     * @param fkName the new fk name
     */
    public void setFkName(String fkName) {
        this.fkName = fkName;
    }

    /**
     * Gets the name of the primary key.
     *
     * @return the pk name
     */
    public String getPkName() {
        return pkName;
    }

    /**
     * Sets the name of the primary key.
     *
     * @param pkName the new pk name
     */
    public void setPkName(String pkName) {
        this.pkName = pkName;
    }
}

