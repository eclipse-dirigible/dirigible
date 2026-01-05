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

/**
 * The Class EntityFieldMetadata.
 */
public class EntityFieldMetadata {

    /** The property name. */
    private String propertyName;

    /** The type script type. */
    private String typeScriptType;

    /** The generation strategy. */
    private String generationStrategy;

    /** The documentation. */
    private String documentation;

    /** The is identifier. */
    private boolean isIdentifier = false;

    /** The is collection. */
    private boolean isCollection = false;

    /** The column details. */
    private ColumnDetails columnDetails;

    /** The collection details. */
    private CollectionDetails collectionDetails;

    /** The is association. */
    private boolean isAssociation;

    /** The association details. */
    private AssociationDetails associationDetails;

    /**
     * Gets the property name.
     *
     * @return the property name
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Sets the property name.
     *
     * @param propertyName the new property name
     */
    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    /**
     * Gets the type script type.
     *
     * @return the type script type
     */
    public String getTypeScriptType() {
        return typeScriptType;
    }

    /**
     * Sets the type script type.
     *
     * @param typeScriptType the new type script type
     */
    public void setTypeScriptType(String typeScriptType) {
        this.typeScriptType = typeScriptType;
    }

    /**
     * Gets the generation strategy.
     *
     * @return the generation strategy
     */
    public String getGenerationStrategy() {
        return generationStrategy;
    }

    /**
     * Sets the generation strategy.
     *
     * @param generationStrategy the new generation strategy
     */
    public void setGenerationStrategy(String generationStrategy) {
        this.generationStrategy = generationStrategy;
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
     * Checks if is identifier.
     *
     * @return true, if is identifier
     */
    public boolean isIdentifier() {
        return isIdentifier;
    }

    /**
     * Sets the identifier.
     *
     * @param isIdentifier the new identifier
     */
    public void setIdentifier(boolean isIdentifier) {
        this.isIdentifier = isIdentifier;
    }

    /**
     * Checks if is collection.
     *
     * @return true, if is collection
     */
    public boolean isCollection() {
        return isCollection;
    }

    /**
     * Sets the collection.
     *
     * @param isCollection the new collection
     */
    public void setCollection(boolean isCollection) {
        this.isCollection = isCollection;
    }

    /**
     * Gets the column details.
     *
     * @return the column details
     */
    public ColumnDetails getColumnDetails() {
        return columnDetails;
    }

    /**
     * Sets the column details.
     *
     * @param columnDetails the new column details
     */
    public void setColumnDetails(ColumnDetails columnDetails) {
        this.columnDetails = columnDetails;
    }

    /**
     * Gets the collection details.
     *
     * @return the collection details
     */
    public CollectionDetails getCollectionDetails() {
        return collectionDetails;
    }

    /**
     * Sets the collection details.
     *
     * @param collectionDetails the new collection details
     */
    public void setCollectionDetails(CollectionDetails collectionDetails) {
        this.collectionDetails = collectionDetails;
    }

    /**
     * Checks if is association.
     *
     * @return true, if is association
     */
    public boolean isAssociation() {
        return this.isAssociation;
    }

    /**
     * Sets the association.
     *
     * @param isAssociation the new association
     */
    public void setAssociation(boolean isAssociation) {
        this.isAssociation = isAssociation;
    }

    /**
     * Gets the association details.
     *
     * @return the association details
     */
    public AssociationDetails getAssociationDetails() {
        return associationDetails;
    }

    /**
     * Sets the association details.
     *
     * @param associationDetails the new association details
     */
    public void setAssociationDetails(AssociationDetails associationDetails) {
        this.associationDetails = associationDetails;
    }

    /**
     * The Class ColumnDetails.
     */
    public static class ColumnDetails {

        /** The column name. */
        private String columnName;

        /** The database type. */
        private String databaseType;

        /** The length. */
        private Integer length;

        /** The is nullable. */
        private boolean isNullable;

        /** The default value. */
        private String defaultValue;

        /**
         * Gets the column name.
         *
         * @return the column name
         */
        public String getColumnName() {
            return columnName;
        }

        /**
         * Sets the column name.
         *
         * @param columnName the new column name
         */
        public void setColumnName(String columnName) {
            this.columnName = columnName;
        }

        /**
         * Gets the database type.
         *
         * @return the database type
         */
        public String getDatabaseType() {
            return databaseType;
        }

        /**
         * Sets the database type.
         *
         * @param databaseType the new database type
         */
        public void setDatabaseType(String databaseType) {
            this.databaseType = databaseType;
        }

        /**
         * Gets the length.
         *
         * @return the length
         */
        public Integer getLength() {
            return length;
        }

        /**
         * Sets the length.
         *
         * @param length the new length
         */
        public void setLength(Integer length) {
            this.length = length;
        }

        /**
         * Checks if is nullable.
         *
         * @return true, if is nullable
         */
        public boolean isNullable() {
            return isNullable;
        }

        /**
         * Sets the nullable.
         *
         * @param nullable the new nullable
         */
        public void setNullable(boolean nullable) {
            this.isNullable = nullable;
        }

        /**
         * Gets the default value.
         *
         * @return the default value
         */
        public String getDefaultValue() {
            return defaultValue;
        }

        /**
         * Sets the default value.
         *
         * @param defaultValue the new default value
         */
        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }
    }

    /**
     * The Class CollectionDetails.
     */
    public static class CollectionDetails {

        /** The name. */
        private String name;

        /** The table name. */
        private String tableName;

        /** The join column. */
        private String joinColumn;

        /** The entity name. */
        private String entityName;

        /** The cascade. */
        private String cascade = "none";

        /** The inverse. */
        private boolean inverse = false;

        /** The lazy. */
        private boolean lazy = true;

        /** The fetch. */
        private String fetch = "select";

        /** The join column not null. */
        private boolean joinColumnNotNull = true;

        /**
         * Gets the name.
         *
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the name.
         *
         * @param name the new name
         */
        public void setName(String name) {
            this.name = name;
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
         * Gets the join column.
         *
         * @return the join column
         */
        public String getJoinColumn() {
            return joinColumn;
        }

        /**
         * Sets the join column.
         *
         * @param joinColumn the new join column
         */
        public void setJoinColumn(String joinColumn) {
            this.joinColumn = joinColumn;
        }

        /**
         * Gets the entity name.
         *
         * @return the entity name
         */
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
         * Gets the cascade.
         *
         * @return the cascade
         */
        public String getCascade() {
            return cascade;
        }

        /**
         * Sets the cascade.
         *
         * @param cascade the new cascade
         */
        public void setCascade(String cascade) {
            this.cascade = cascade;
        }

        /**
         * Checks if is inverse.
         *
         * @return true, if is inverse
         */
        public boolean isInverse() {
            return inverse;
        }

        /**
         * Sets the inverse.
         *
         * @param inverse the new inverse
         */
        public void setInverse(boolean inverse) {
            this.inverse = inverse;
        }

        /**
         * Checks if is lazy.
         *
         * @return true, if is lazy
         */
        public boolean isLazy() {
            return lazy;
        }

        /**
         * Sets the lazy.
         *
         * @param lazy the new lazy
         */
        public void setLazy(boolean lazy) {
            this.lazy = lazy;
        }

        /**
         * Gets the fetch.
         *
         * @return the fetch
         */
        public String getFetch() {
            return fetch;
        }

        /**
         * Sets the fetch.
         *
         * @param fetch the new fetch
         */
        public void setFetch(String fetch) {
            this.fetch = fetch;
        }

        /**
         * Checks if is join column not null.
         *
         * @return true, if is join column not null
         */
        public boolean isJoinColumnNotNull() {
            return joinColumnNotNull;
        }

        /**
         * Sets the join column not null.
         *
         * @param joinColumnNotNull the new join column not null
         */
        public void setJoinColumnNotNull(boolean joinColumnNotNull) {
            this.joinColumnNotNull = joinColumnNotNull;
        }
    }

    /**
     * The Class AssociationDetails.
     */
    public static class AssociationDetails {

        /** The name. */
        private String name;

        /** The entity name. */
        private String entityName;

        /** The join column. */
        private String joinColumn;

        /** The cascade. */
        private String cascade;

        /** The not null. */
        private boolean notNull;

        /** The lazy. */
        private String lazy;

        /**
         * Instantiates a new association details.
         */
        public AssociationDetails() {
            this.notNull = false;
            this.lazy = "proxy";
            this.cascade = "none";
        }

        /**
         * Gets the name.
         *
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the name.
         *
         * @param name the new name
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Gets the entity name.
         *
         * @return the entity name
         */
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
         * Gets the join column.
         *
         * @return the join column
         */
        public String getJoinColumn() {
            return joinColumn;
        }

        /**
         * Sets the join column.
         *
         * @param joinColumn the new join column
         */
        public void setJoinColumn(String joinColumn) {
            this.joinColumn = joinColumn;
        }

        /**
         * Gets the cascade.
         *
         * @return the cascade
         */
        public String getCascade() {
            return cascade;
        }

        /**
         * Sets the cascade.
         *
         * @param cascade the new cascade
         */
        public void setCascade(String cascade) {
            this.cascade = cascade;
        }

        /**
         * Checks if is not null.
         *
         * @return true, if is not null
         */
        public boolean isNotNull() {
            return notNull;
        }

        /**
         * Sets the not null.
         *
         * @param notNull the new not null
         */
        public void setNotNull(boolean notNull) {
            this.notNull = notNull;
        }

        /**
         * Gets the lazy.
         *
         * @return the lazy
         */
        public String getLazy() {
            return lazy;
        }

        /**
         * Sets the lazy.
         *
         * @param lazy the new lazy
         */
        public void setLazy(String lazy) {
            this.lazy = lazy;
        }
    }

}
