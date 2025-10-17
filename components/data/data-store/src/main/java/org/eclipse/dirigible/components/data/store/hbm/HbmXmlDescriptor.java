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

    private final String entityName;
    private final String tableName;
    private final HbmIdDescriptor id;
    private final List<HbmPropertyDescriptor> properties = new ArrayList<>();
    private final List<HbmCollectionDescriptor> collections = new ArrayList<>();
    private final List<HbmAssociationDescriptor> associations = new ArrayList<>();

    public HbmXmlDescriptor(String entityName, String tableName, HbmIdDescriptor id) {
        this.entityName = entityName;
        this.tableName = tableName;
        this.id = id;
    }

    /** Models the id element */
    public static class HbmIdDescriptor {
        private final String name;
        private final String column;
        private final String type;
        private final String generatorClass;

        public HbmIdDescriptor(String name, String column, String type, String generatorClass) {
            this.name = name;
            this.column = column;
            this.type = type;
            this.generatorClass = generatorClass;
        }

        public String getName() {
            return name;
        }

        public String getColumn() {
            return column;
        }

        public String getType() {
            return type;
        }

        public String getGeneratorClass() {
            return generatorClass;
        }
    }


    /** Models a property element */
    public static class HbmPropertyDescriptor {
        private final String name;
        private final String column;
        private final String type;
        private final Integer length;

        public HbmPropertyDescriptor(String name, String column, String type, Integer length) {
            this.name = name;
            this.column = column;
            this.type = type;
            this.length = length;
        }

        public String getName() {
            return name;
        }

        public String getColumn() {
            return column;
        }

        public String getType() {
            return type;
        }

        public Integer getLength() {
            return length;
        }
    }


    public static class HbmCollectionDescriptor {
        public String name;
        public String tableName;
        public String joinColumn;
        public String entityName;
        public boolean inverse;
        public boolean lazy;
        public String fetch;
        public String cascade;
        public boolean joinColumnNotNull;

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

        public String getName() {
            return name;
        }

        public String getTableName() {
            return tableName;
        }

        public String getJoinColumn() {
            return joinColumn;
        }

        public String getEntityName() {
            return entityName;
        }

        public boolean isInverse() {
            return inverse;
        }

        public boolean isLazy() {
            return lazy;
        }

        public String getFetch() {
            return fetch;
        }

        public String getCascade() {
            return cascade;
        }

        public boolean isJoinColumnNotNull() {
            return joinColumnNotNull;
        }

        public String serialize() {
            String xml = String.format("        <bag name=\"%s\" table=\"%s\" inverse=\"%s\" lazy=\"%s\" fetch=\"%s\" cascade=\"%s\">\n",
                    name, tableName, inverse, lazy, fetch, cascade) + "            <key>\n"
                    + String.format("                <column name=\"%s\" not-null=\"%s\" />\n", joinColumn, joinColumnNotNull)
                    + "            </key>\n" + String.format("            <one-to-many entity-name=\"%s\" />\n", entityName)
                    + "        </bag>\n";
            return xml;
        }

    }

    public static class HbmAssociationDescriptor {
        private final String name;
        private final String entityName;
        private final String joinColumn;
        private final String cascade;
        private final boolean notNull;
        private final String lazy;

        public HbmAssociationDescriptor(String name, String entityName, String joinColumn, String cascade, boolean notNull, String lazy) {
            this.name = name;
            this.entityName = entityName;
            this.joinColumn = joinColumn;
            this.cascade = cascade;
            this.notNull = notNull;
            this.lazy = lazy;
        }

        public String getName() {
            return name;
        }

        public String getEntityName() {
            return entityName;
        }

        public String getJoinColumn() {
            return joinColumn;
        }

        public String getCascade() {
            return cascade;
        }

        public boolean isNotNull() {
            return notNull;
        }

        public String getLazy() {
            return lazy;
        }

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

    public void addProperty(HbmPropertyDescriptor property) {
        this.properties.add(property);
    }

    public void addCollection(HbmCollectionDescriptor collection) {
        this.collections.add(collection);
    }

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
        xml.append(String.format("    <class entity-name=\"%s\" table=\"%s\">\n", this.entityName, this.tableName));

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
