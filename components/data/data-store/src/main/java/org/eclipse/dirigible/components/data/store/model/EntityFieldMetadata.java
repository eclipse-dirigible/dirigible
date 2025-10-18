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

    private String generationStrategy;

    private String documentation;

    private boolean isIdentifier = false;

    private boolean isCollection = false;

    private ColumnDetails columnDetails;

    private CollectionDetails collectionDetails;

    private boolean isAssociation;

    private AssociationDetails associationDetails;

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

    public String getGenerationStrategy() {
        return generationStrategy;
    }

    public void setGenerationStrategy(String generationStrategy) {
        this.generationStrategy = generationStrategy;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public boolean isIdentifier() {
        return isIdentifier;
    }

    public void setIdentifier(boolean isIdentifier) {
        this.isIdentifier = isIdentifier;
    }

    public boolean isCollection() {
        return isCollection;
    }

    public void setCollection(boolean isCollection) {
        this.isCollection = isCollection;
    }

    public ColumnDetails getColumnDetails() {
        return columnDetails;
    }

    public void setColumnDetails(ColumnDetails columnDetails) {
        this.columnDetails = columnDetails;
    }

    public CollectionDetails getCollectionDetails() {
        return collectionDetails;
    }

    public void setCollectionDetails(CollectionDetails collectionDetails) {
        this.collectionDetails = collectionDetails;
    }

    public boolean isAssociation() {
        return this.isAssociation;
    }

    public void setAssociation(boolean isAssociation) {
        this.isAssociation = isAssociation;
    }

    public AssociationDetails getAssociationDetails() {
        return associationDetails;
    }

    public void setAssociationDetails(AssociationDetails associationDetails) {
        this.associationDetails = associationDetails;
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

    public static class CollectionDetails {

        private String name;

        private String tableName;

        private String joinColumn;

        private String entityName;

        private String cascade = "none";

        private boolean inverse = false;

        private boolean lazy = true;

        private String fetch = "select";

        private boolean joinColumnNotNull = true;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public String getJoinColumn() {
            return joinColumn;
        }

        public void setJoinColumn(String joinColumn) {
            this.joinColumn = joinColumn;
        }

        public String getEntityName() {
            return entityName;
        }

        public void setEntityName(String entityName) {
            this.entityName = entityName;
        }

        public String getCascade() {
            return cascade;
        }

        public void setCascade(String cascade) {
            this.cascade = cascade;
        }

        public boolean isInverse() {
            return inverse;
        }

        public void setInverse(boolean inverse) {
            this.inverse = inverse;
        }

        public boolean isLazy() {
            return lazy;
        }

        public void setLazy(boolean lazy) {
            this.lazy = lazy;
        }

        public String getFetch() {
            return fetch;
        }

        public void setFetch(String fetch) {
            this.fetch = fetch;
        }

        public boolean isJoinColumnNotNull() {
            return joinColumnNotNull;
        }

        public void setJoinColumnNotNull(boolean joinColumnNotNull) {
            this.joinColumnNotNull = joinColumnNotNull;
        }
    }

    public static class AssociationDetails {

        private String name;
        private String entityName;
        private String joinColumn;
        private String cascade;
        private boolean notNull;
        private String lazy;

        public AssociationDetails() {
            this.notNull = false;
            this.lazy = "proxy";
            this.cascade = "none";
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEntityName() {
            return entityName;
        }

        public void setEntityName(String entityName) {
            this.entityName = entityName;
        }

        public String getJoinColumn() {
            return joinColumn;
        }

        public void setJoinColumn(String joinColumn) {
            this.joinColumn = joinColumn;
        }

        public String getCascade() {
            return cascade;
        }

        public void setCascade(String cascade) {
            this.cascade = cascade;
        }

        public boolean isNotNull() {
            return notNull;
        }

        public void setNotNull(boolean notNull) {
            this.notNull = notNull;
        }

        public String getLazy() {
            return lazy;
        }

        public void setLazy(String lazy) {
            this.lazy = lazy;
        }
    }

}
