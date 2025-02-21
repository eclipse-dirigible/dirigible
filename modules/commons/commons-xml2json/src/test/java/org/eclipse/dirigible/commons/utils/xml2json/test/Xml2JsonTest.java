/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.commons.utils.xml2json.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.commons.utils.xml2json.Xml2Json;
import org.junit.Test;

/**
 * The Class Xml2JsonTest.
 */
public class Xml2JsonTest {

    /**
     * To json.
     *
     * @param xmlFile the xml file
     * @param jsonFile the json file
     */
    private void toJson(String xmlFile, String jsonFile) {
        try {
            String json = null;
            InputStream inXml = Xml2JsonTest.class.getResourceAsStream(xmlFile);
            try {
                @SuppressWarnings("deprecation")
                String xml = IOUtils.toString(inXml);
                json = Xml2Json.toJson(xml);
                System.out.println(xml);
                System.out.println(json);
            } finally {
                if (inXml != null) {
                    inXml.close();
                }
            }
            InputStream inJson = Xml2JsonTest.class.getResourceAsStream(jsonFile);
            try {
                @SuppressWarnings("deprecation")
                String jsonExpected = IOUtils.toString(inJson);
                jsonExpected = condense(jsonExpected);
                json = condense(json);
                assertEquals(jsonExpected, json);
            } finally {
                if (inJson != null) {
                    inJson.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * To xml.
     *
     * @param jsonFile the json file
     * @param xmlFile the xml file
     */
    private void toXml(String jsonFile, String xmlFile) {
        try {
            InputStream inJson = Xml2JsonTest.class.getResourceAsStream(jsonFile);
            String xml = null;
            try {
                @SuppressWarnings("deprecation")
                String json = IOUtils.toString(inJson);
                Xml2Json xml2json = new Xml2Json();
                xml = Xml2Json.toXml(json);
                xml = xml2json.prettyPrintXml(xml);
                System.out.println(json);
                System.out.println(xml);
            } finally {
                if (inJson != null) {
                    inJson.close();
                }
            }
            InputStream inXml = Xml2JsonTest.class.getResourceAsStream(xmlFile);
            try {
                @SuppressWarnings("deprecation")
                String xmlExpected = IOUtils.toString(inXml);
                xmlExpected = condense(xmlExpected);
                xml = condense(xml);
                xmlExpected = condense(xmlExpected);
                assertEquals(xmlExpected, xml);
            } finally {
                if (inXml != null) {
                    inXml.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Normalize line endings.
     *
     * @param content the content
     * @return the string
     */
    private String condense(String content) {
        content = content.replace("\r\n", "");
        content = content.replace("\n\r", "");
        content = content.replace("\r", "");
        content = content.replace("\n", "");
        content = content.replace("\t", "");
        content = content.replace(" ", "");
        return content;
    }

    /**
     * Test to json basic.
     */
    @Test
    public void testToJsonBasic() {
        toJson("xml2json/basic.xml", "xml2json/basic.json");
    }

    /**
     * Test to json attrs.
     */
    @Test
    public void testToJsonAttrs() {
        toJson("xml2json/attrs.xml", "xml2json/attrs.json");
    }

    /**
     * Test to json element with attrs.
     */
    @Test
    public void testToJsonElementWithAttrs() {
        toJson("xml2json/element_attrs.xml", "xml2json/element_attrs.json");
    }

    /**
     * Test to json element with attrs multiple.
     */
    @Test
    public void testToJsonElementWithAttrsMultiple() {
        toJson("xml2json/element_attrs_multiple.xml", "xml2json/element_attrs_multiple.json");
    }

    /**
     * Test to json C data.
     */
    @Test
    public void testToJsonCData() {
        toJson("xml2json/cdata.xml", "xml2json/cdata.json");
    }

    /**
     * Test to json C data attrs.
     */
    @Test
    public void testToJsonCDataAttrs() {
        toJson("xml2json/cdata_attrs.xml", "xml2json/cdata_attrs.json");
    }

    /**
     * Test to json array.
     */
    @Test
    public void testToJsonArray() {
        toJson("xml2json/array.xml", "xml2json/array.json");
    }

    /**
     * Test to xml basic.
     */
    @Test
    public void testToXmlBasic() {
        toXml("xml2json/basic.json", "xml2json/basic.xml");
    }

    /**
     * Test to xml attrs.
     */
    @Test
    public void testToXmlAttrs() {
        toXml("xml2json/attrs.json", "xml2json/attrs.xml");
    }

    /**
     * Test to xml element with attrs.
     */
    @Test
    public void testToXmlElementWithAttrs() {
        toXml("xml2json/element_attrs.json", "xml2json/element_attrs.xml");
    }

    /**
     * Test to xml element with attrs multiple.
     */
    @Test
    public void testToXmlElementWithAttrsMultiple() {
        toXml("xml2json/element_attrs_multiple.json", "xml2json/element_attrs_multiple.xml");
    }

    /**
     * Test to xml C data.
     */
    @Test
    public void testToXmlCData() {
        toXml("xml2json/cdata.json", "xml2json/cdata.xml");
    }

    /**
     * Test to xml C data attrs.
     */
    @Test
    public void testToXmlCDataAttrs() {
        toXml("xml2json/cdata_attrs.json", "xml2json/cdata_attrs.xml");
    }

    /**
     * Test to xml array.
     */
    @Test
    public void testToXmlArray() {
        toXml("xml2json/array.json", "xml2json/array.xml");
    }

}
