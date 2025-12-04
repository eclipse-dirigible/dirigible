/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store.hbm;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the structure of a Hibernate *.hbm.xml mapping descriptor.
 *
 * This class includes simplified serialization (to XML string)
 */
public class HbmXmlDescriptor {

    /** The entity name. */
    private final String entityName;

    /** The table name. */
    private final String tableName;

    /** The id. */
    private final HbmIdDescriptor id;

    /** The properties. */
    private final List<HbmPropertyDescriptor> properties = new ArrayList<>();

    /** The collections. */
    private final List<HbmCollectionDescriptor> collections = new ArrayList<>();

    /** The associations. */
    private final List<HbmAssociationDescriptor> associations = new ArrayList<>();

    /**
     * Instantiates a new hbm xml descriptor.
     *
     * @param entityName the entity name
     * @param tableName the table name
     * @param id the id
     */
    public HbmXmlDescriptor(String entityName, String tableName, HbmIdDescriptor id) {
        this.entityName = entityName;
        this.tableName = tableName;
        this.id = id;
    }

    /**
     * Models the id element.
     */
    public static class HbmIdDescriptor {

        /** The name. */
        private final String name;

        /** The column. */
        private final String column;

        /** The type. */
        private final String type;

        /** The generator class. */
        private final String generatorClass;

        /**
         * Instantiates a new hbm id descriptor.
         *
         * @param name the name
         * @param column the column
         * @param type the type
         * @param generatorClass the generator class
         */
        public HbmIdDescriptor(String name, String column, String type, String generatorClass) {
            this.name = name;
            this.column = column;
            this.type = type;
            this.generatorClass = generatorClass;
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
         * Gets the column.
         *
         * @return the column
         */
        public String getColumn() {
            return column;
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
         * Gets the generator class.
         *
         * @return the generator class
         */
        public String getGeneratorClass() {
            return generatorClass;
        }
    }


    /**
     * Models a property element.
     */
    public static class HbmPropertyDescriptor {

        /** The name. */
        private final String name;

        /** The column. */
        private final String column;

        /** The type. */
        private final String type;

        /** The length. */
        private final Integer length;

        /**
         * Instantiates a new hbm property descriptor.
         *
         * @param name the name
         * @param column the column
         * @param type the type
         * @param length the length
         */
        public HbmPropertyDescriptor(String name, String column, String type, Integer length) {
            this.name = name;
            this.column = column;
            this.type = type;
            this.length = length;
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
         * Gets the column.
         *
         * @return the column
         */
        public String getColumn() {
            return column;
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
         * Gets the length.
         *
         * @return the length
         */
        public Integer getLength() {
            return length;
        }
    }


    /**
     * The Class HbmCollectionDescriptor.
     */
    public static class HbmCollectionDescriptor {

        /** The name. */
        public String name;

        /** The table name. */
        public String tableName;

        /** The join column. */
        public String joinColumn;

        /** The entity name. */
        public String entityName;

        /** The inverse. */
        public boolean inverse;

        /** The lazy. */
        public boolean lazy;

        /** The fetch. */
        public String fetch;

        /** The cascade. */
        public String cascade;

        /** The join column not null. */
        public boolean joinColumnNotNull;

        /**
         * Instantiates a new hbm collection descriptor.
         *
         * @param name the name
         * @param tableName the table name
         * @param joinColumn the join column
         * @param entityName the entity name
         * @param inverse the inverse
         * @param lazy the lazy
         * @param fetch the fetch
         * @param cascade the cascade
         * @param joinColumnNotNull the join column not null
         */
        public HbmCollectionDescriptor(String name, String tableName, String joinColumn, String entityName, boolean inverse, boolean lazy,
                String fetch, String cascade, boolean joinColumnNotNull) {
            super();
            this.name = name;
            this.tableName = tableName;
            this.joinColumn = joinColumn;
            this.entityName = entityName;
            this.inverse = inverse;
            this.lazy = lazy;
            this.fetch = fetch;
            this.cascade = cascade;
            this.joinColumnNotNull = joinColumnNotNull;
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
         * Gets the table name.
         *
         * @return the table name
         */
        public String getTableName() {
            return tableName;
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
         * Gets the entity name.
         *
         * @return the entity name
         */
        public String getEntityName() {
            return entityName;
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
         * Checks if is lazy.
         *
         * @return true, if is lazy
         */
        public boolean isLazy() {
            return lazy;
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
         * Gets the cascade.
         *
         * @return the cascade
         */
        public String getCascade() {
            return cascade;
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
         * Serialize.
         *
         * @return the string
         */
        public String serialize() {
            String xml = String.format("        <bag name=\"%s\" table=\"`%s`\" inverse=\"%s\" lazy=\"%s\" fetch=\"%s\" cascade=\"%s\">\n",
                    name, tableName, inverse, lazy, fetch, cascade) + "            <key>\n"
                    + String.format("                <column name=\"%s\" not-null=\"%s\" />\n", joinColumn, joinColumnNotNull)
                    + "            </key>\n" + String.format("            <one-to-many entity-name=\"%s\" />\n", entityName)
                    + "        </bag>\n";
            return xml;
        }

    }

    /**
     * The Class HbmAssociationDescriptor.
     */
    public static class HbmAssociationDescriptor {

        /** The name. */
        private final String name;

        /** The entity name. */
        private final String entityName;

        /** The join column. */
        private final String joinColumn;

        /** The cascade. */
        private final String cascade;

        /** The not null. */
        private final boolean notNull;

        /** The lazy. */
        private final String lazy;

        /**
         * Instantiates a new hbm association descriptor.
         *
         * @param name the name
         * @param entityName the entity name
         * @param joinColumn the join column
         * @param cascade the cascade
         * @param notNull the not null
         * @param lazy the lazy
         */
        public HbmAssociationDescriptor(String name, String entityName, String joinColumn, String cascade, boolean notNull, String lazy) {
            this.name = name;
            this.entityName = entityName;
            this.joinColumn = joinColumn;
            this.cascade = cascade;
            this.notNull = notNull;
            this.lazy = lazy;
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
         * Gets the entity name.
         *
         * @return the entity name
         */
        public String getEntityName() {
            return entityName;
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
         * Gets the cascade.
         *
         * @return the cascade
         */
        public String getCascade() {
            return cascade;
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
         * Gets the lazy.
         *
         * @return the lazy
         */
        public String getLazy() {
            return lazy;
        }

        /**
         * Serialize.
         *
         * @return the string
         */
        public String serialize() {
            // Build attribute list
            StringBuilder attrs = new StringBuilder();
            attrs.append(String.format(" name=\"%s\"", name));
            attrs.append(String.format(" entity-name=\"%s\"", entityName));
            attrs.append(String.format(" column=\"%s\"", joinColumn));

            // Optional attributes
            if (cascade != null && !cascade.isEmpty() && !"none".equalsIgnoreCase(cascade)) {
                attrs.append(String.format(" cascade=\"%s\"", cascade));
            }
            if (notNull) {
                attrs.append(" not-null=\"true\"");
            }
            if (lazy != null && !lazy.isEmpty() && !"false".equalsIgnoreCase(lazy)) {
                attrs.append(String.format(" lazy=\"%s\"", lazy));
            }

            return String.format("        <many-to-one%s/>\n", attrs.toString());
        }
    }

    /**
     * Adds the property.
     *
     * @param property the property
     */
    public void addProperty(HbmPropertyDescriptor property) {
        this.properties.add(property);
    }

    /**
     * Adds the collection.
     *
     * @param collection the collection
     */
    public void addCollection(HbmCollectionDescriptor collection) {
        this.collections.add(collection);
    }

    /**
     * Adds the association.
     *
     * @param association the association
     */
    public void addAssociation(HbmAssociationDescriptor association) {
        this.associations.add(association);
    }

    /**
     * Serializes the Java object model into the standard Hibernate *.hbm.xml format.
     *
     * @return A string containing the XML descriptor.
     */
    public String serialize() {
        StringBuilder xml = new StringBuilder();

        xml.append("<?xml version=\"1.0\"?>\n");
        xml.append("<!DOCTYPE hibernate-mapping PUBLIC\n");
        xml.append("        \"-//Hibernate/Hibernate Mapping DTD 3.0//EN\"\n");
        xml.append("        \"http://www.hibernate.org/dtd/hibernate-mapping-3.0.dtd\">\n\n");
        xml.append("<hibernate-mapping>\n");

        // --- Class Element ---
        xml.append(String.format("    <class entity-name=\"%s\" table=\"`%s`\">\n", this.entityName, this.tableName));

        // --- ID Element ---
        HbmIdDescriptor idDesc = this.id;
        xml.append(String.format("        <id name=\"%s\" column=\"%s\" type=\"%s\">\n", idDesc.getName(), idDesc.getColumn(),
                idDesc.getType()));
        xml.append(String.format("            <generator class=\"%s\"/>\n", idDesc.getGeneratorClass()));
        xml.append("        </id>\n");

        // --- Association Elements (ManyToOne) ---
        for (HbmAssociationDescriptor association : this.associations) {
            xml.append(association.serialize());
        }

        // --- Property Elements ---
        for (HbmPropertyDescriptor prop : this.properties) {
            String lengthAttr = prop.getLength() != null ? String.format(" length=\"%d\"", prop.getLength()) : "";
            xml.append(String.format("        <property name=\"%s\" column=\"%s\" type=\"%s\"%s/>\n", prop.getName(), prop.getColumn(),
                    prop.getType(), lengthAttr));
        }

        // --- Collection Elements ---
        for (HbmCollectionDescriptor collection : this.collections) {
            xml.append(collection.serialize());
        }

        xml.append("    </class>\n");
        xml.append("</hibernate-mapping>\n");

        return xml.toString();
    }

}
