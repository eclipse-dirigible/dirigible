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

public class EntityFieldMetadata {

    private String propertyName;

    private String typeScriptType;

    private boolean isIdentifier;

    private String generationStrategy;

    private ColumnDetails columnDetails;

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getTypeScriptType() {
        return typeScriptType;
    }

    public void setTypeScriptType(String typeScriptType) {
        this.typeScriptType = typeScriptType;
    }

    public boolean isIdentifier() {
        return isIdentifier;
    }

    public void setIdentifier(boolean identifier) {
        isIdentifier = identifier;
    }

    public String getGenerationStrategy() {
        return generationStrategy;
    }

    public void setGenerationStrategy(String generationStrategy) {
        this.generationStrategy = generationStrategy;
    }

    public ColumnDetails getColumnDetails() {
        return columnDetails;
    }

    public void setColumnDetails(ColumnDetails columnDetails) {
        this.columnDetails = columnDetails;
    }

    public static class ColumnDetails {

        private String columnName;

        private String databaseType;

        private Integer length;

        private boolean isNullable;

        private String defaultValue;

        public String getColumnName() {
            return columnName;
        }

        public void setColumnName(String columnName) {
            this.columnName = columnName;
        }

        public String getDatabaseType() {
            return databaseType;
        }

        public void setDatabaseType(String databaseType) {
            this.databaseType = databaseType;
        }

        public Integer getLength() {
            return length;
        }

        public void setLength(Integer length) {
            this.length = length;
        }

        public boolean isNullable() {
            return isNullable;
        }

        public void setNullable(boolean nullable) {
            this.isNullable = nullable;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }
    }

}
