/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.data.store;

import java.io.FileInputStream;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.commons.utils.xml2json.Xml2Json2;

/**
 * The Class EntityTransformer.
 */
public class EntityTransformer {

    /**
     * To entity.
     *
     * @param xml the xml
     * @return the string
     * @throws Exception the exception
     */
    public static String toEntity(String xml) throws Exception {
        xml = xml.replace("hibernate-mapping>", "entity-mapping>");
        return Xml2Json2.toJson(xml);
    }

    /**
     * From entity.
     *
     * @param json the json
     * @return the string
     * @throws Exception the exception
     */
    public static String fromEntity(String json) throws Exception {
        json = json.replace("entity-mapping", "hibernate-mapping");
        return Xml2Json2.toXml(json);
    }

    /**
     * The main method.
     *
     * @param args the arguments
     * @throws Exception the exception
     */
    public static void main(String[] args) throws Exception {
        if (args.length > 0) {
            String xml = IOUtils.toString(new FileInputStream(args[0]));
            System.out.println(EntityTransformer.toEntity(xml));
        } else {
            System.err.println("Missing file argument");
        }
    }

}
