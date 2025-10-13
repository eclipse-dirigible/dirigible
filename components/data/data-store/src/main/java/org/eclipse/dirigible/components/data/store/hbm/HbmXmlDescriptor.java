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

    private String className;
    private String tableName;
    private HbmIdDescriptor id;
    private List<HbmPropertyDescriptor> properties = new ArrayList<>();

    /** Models the id element */
    public static class HbmIdDescriptor {
        private String name;
        private String column;
        private String generatorClass;

        public HbmIdDescriptor(String name, String column, String generatorClass) {
            this.name = name;
            this.column = column;
            this.generatorClass = generatorClass;
        }

        public String getName() {
            return name;
        }

        public String getColumn() {
            return column;
        }

        public String getGeneratorClass() {
            return generatorClass;
        }
    }

    /** Models a property element */
    public static class HbmPropertyDescriptor {
        private String name;
        private String column;
        private String type;
        private Integer length;

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

    public HbmXmlDescriptor(String className, String tableName, HbmIdDescriptor id) {
        this.className = className;
        this.tableName = tableName;
        this.id = id;
    }

    public void addProperty(HbmPropertyDescriptor property) {
        this.properties.add(property);
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
        xml.append(String.format("    <class name=\"%s\" table=\"%s\">\n", this.className, this.tableName));

        // --- ID Element ---
        HbmIdDescriptor idDesc = this.id;
        xml.append(String.format("        <id name=\"%s\" column=\"%s\">\n", idDesc.getName(), idDesc.getColumn()));
        xml.append(String.format("            <generator class=\"%s\"/>\n", idDesc.getGeneratorClass()));
        xml.append("        </id>\n");

        // --- Property Elements ---
        for (HbmPropertyDescriptor prop : this.properties) {
            String lengthAttr = prop.getLength() != null ? String.format(" length=\"%d\"", prop.getLength()) : "";
            xml.append(String.format("        <property name=\"%s\" column=\"%s\" type=\"%s\"%s/>\n", prop.getName(), prop.getColumn(),
                    prop.getType(), lengthAttr));
        }

        xml.append("    </class>\n");
        xml.append("</hibernate-mapping>\n");

        return xml.toString();
    }

    public static void main(String[] args) {
        // --- 1. Define Model Programmatically (Serialization Test) ---

        // Create the ID descriptor
        HbmIdDescriptor id = new HbmIdDescriptor("id", "CAR_ID", "native");

        // Create the main descriptor
        HbmXmlDescriptor carMapping = new HbmXmlDescriptor("com.example.Car", "T_CARS", id);

        // Add properties
        carMapping.addProperty(new HbmPropertyDescriptor("make", "MAKE_NAME", "string", 255));
        carMapping.addProperty(new HbmPropertyDescriptor("model", "MODEL_NAME", "string", null)); // Null length
        carMapping.addProperty(new HbmPropertyDescriptor("price", "DAILY_RATE", "big_decimal", null));

        String serializedXml = carMapping.serialize();
        System.out.println(serializedXml);

    }

}
