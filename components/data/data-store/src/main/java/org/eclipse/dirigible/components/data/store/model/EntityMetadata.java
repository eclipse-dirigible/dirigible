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

public class EntityMetadata {

    private String entityName;

    private String tableName;

    private String documentation;

    private List<EntityFieldMetadata> fields = new ArrayList<>();

    // Getters and Setters (simplified)
    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public List<EntityFieldMetadata> getFields() {
        return fields;
    }

    public void setFields(List<EntityFieldMetadata> fields) {
        this.fields = fields;
    }

}
