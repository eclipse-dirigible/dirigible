/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The Class EntityMetadata.
 */
public class EntityMetadata {

    /** The entity name. */
    private String entityName;

    /** The table name. */
    private String tableName;

    /** The documentation. */
    private String documentation;

    /** The fields. */
    private List<EntityFieldMetadata> fields = new ArrayList<>();

    /**
     * Gets the entity name.
     *
     * @return the entity name
     */
    // Getters and Setters (simplified)
    public String getEntityName() {
        return entityName;
    }

    /**
     * Sets the entity name.
     *
     * @param entityName the new entity name
     */
    public void setEntityName(String entityName) {
        this.entityName = entityName;
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
     * Gets the documentation.
     *
     * @return the documentation
     */
    public String getDocumentation() {
        return documentation;
    }

    /**
     * Sets the documentation.
     *
     * @param documentation the new documentation
     */
    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    /**
     * Gets the fields.
     *
     * @return the fields
     */
    public List<EntityFieldMetadata> getFields() {
        return fields;
    }

    /**
     * Sets the fields.
     *
     * @param fields the new fields
     */
    public void setFields(List<EntityFieldMetadata> fields) {
        this.fields = fields;
    }

}
