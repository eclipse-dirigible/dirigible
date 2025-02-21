/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.commons.utils.xml2json;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/**
 * The Class Xml2Json.
 */
public class Xml2Json {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(Xml2Json.class);

    /** The Constant CDATA_CLOSE. */
    private static final String CDATA_CLOSE = "]]>";

    /** The Constant CDATA_OPEN. */
    private static final String CDATA_OPEN = "<![CDATA[";

    /** The Constant ESQ. */
    private static final String ESQ = "=\"";

    /** The Constant SPACE. */
    private static final String SPACE = " ";

    /** The Constant EQ. */
    private static final String EQ = "\"";

    /** The Constant EMPTY. */
    private static final String EMPTY = "";

    /** The Constant ATTR_TEXT. */
    private static final String ATTR_TEXT = "#text";

    /** The Constant ATTR_CDATA. */
    private static final String ATTR_CDATA = "#cdata-section";

    /** The Constant LTS. */
    private static final String LTS = "</";

    /** The Constant GT. */
    private static final String GT = ">";

    /** The Constant LT. */
    private static final String LT = "<";

    /** The added by value. */
    static List<Object> ADDED_BY_VALUE = new ArrayList<Object>();

    /** The reorganized. */
    static Map<String, JsonElement> REORGANIZED = new HashMap<String, JsonElement>();

    /**
     * Transform XML to JSON.
     *
     * @param xml the xml
     * @return the string
     * @throws ParserConfigurationException the parser configuration exception
     * @throws SAXException the SAX exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static String toJson(String xml) throws ParserConfigurationException, SAXException, IOException {
        JsonObject rootJson = new JsonObject();
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document doc = documentBuilder.parse(new InputSource(new StringReader(xml)));
        if (doc.hasChildNodes()) {
            traverseNode(doc, rootJson, null);
        }
        Gson gson = new GsonBuilder().setPrettyPrinting()
                                     .disableHtmlEscaping()
                                     .create();
        return gson.toJson(rootJson);
    }

    /**
     * Traverse node.
     *
     * @param parentNode the parent node
     * @param parentJson the parent json
     * @param upperJson the upper json
     */
    private static void traverseNode(Node parentNode, JsonObject parentJson, JsonObject upperJson) {
        NodeList childList = parentNode.getChildNodes();
        for (int i = 0; i < childList.getLength(); i++) {
            JsonObject childJson = new JsonObject();
            Node childNode = childList.item(i);

            if (childNode.getNodeType() == Node.TEXT_NODE) {
                if (childNode.getNodeValue()
                             .trim()
                             .length() != 0) {
                    // non empty text node reached, so add to the parent
                    processTextNode(parentNode, upperJson, childJson, childNode);
                }
            } else if (childNode.getNodeType() == Node.ELEMENT_NODE) {

                if (childNode.hasAttributes()) {
                    // attributes exist, so go thru them
                    traverseAttributes(childJson, childNode);
                }

                if (childNode.hasChildNodes()) {
                    // child nodes exist, so go into them
                    traverseNode(childNode, childJson, parentJson);
                }

                if (childNode.getNodeType() != Node.TEXT_NODE) {
                    // non text node element
                    if (ADDED_BY_VALUE.contains(childNode)) {
                        // already added as a value
                        if (parentJson.has(childNode.getNodeName())) {
                            // there is already such an element as expected
                            JsonElement existing = parentJson.get(childNode.getNodeName());
                            if (existing instanceof JsonPrimitive) {
                                // it is a primitive as expected
                                Iterator attrs = childJson.entrySet()
                                                          .iterator();
                                if (attrs.hasNext()) {
                                    // there are attributes, so reorganize the element to include the attributes and the
                                    // value as property - #text
                                    reorganizeForAttributes(parentJson, childNode, existing, attrs);
                                }
                            } else if (existing instanceof JsonArray) {
                                // already added and reorganized as an array, so take the last element of this type and
                                // add the attributes
                                Iterator attrs = childJson.entrySet()
                                                          .iterator();
                                if (attrs.hasNext()) {
                                    reorganizeAddAttributes(childNode, attrs);
                                }
                            } else if (existing instanceof JsonObject) {
                                if (logger.isErrorEnabled()) {
                                    logger.error("Found object, but expected primitive or array");
                                }
                            }
                        } else {
                            if (logger.isErrorEnabled()) {
                                logger.error("Expected element, but it does not exist");
                            }
                        }
                        // remove it from the list
                        ADDED_BY_VALUE.remove(childNode);
                    } else {
                        if (parentJson.has(childNode.getNodeName())) {
                            // parent already has such an element
                            JsonElement existing = parentJson.get(childNode.getNodeName());
                            if (existing instanceof JsonArray) {
                                // and it is already an array, so just add the child to the array
                                ((JsonArray) existing).add(childJson);
                            } else if (existing instanceof JsonObject) {
                                // and it not an array, so reorganize the element
                                reorganizeElement(parentNode, parentJson, childJson, childNode, existing);
                            }
                        } else {
                            // no such an element yet, so add it to the parent
                            parentJson.add(childNode.getNodeName(), childJson);
                        }
                    }
                }
            } else if (childNode.getNodeType() == Node.CDATA_SECTION_NODE) {
                // processTextNode(parentNode, upperJson, childJson, childNode);
                String base64 = Base64.getEncoder()
                                      .encodeToString(childNode.getNodeValue()
                                                               .getBytes(StandardCharsets.UTF_8));
                parentJson.addProperty(childNode.getNodeName(), base64);
            } else {
                if (logger.isErrorEnabled()) {
                    logger.error("Unsupported node type: {}", childNode.getNodeType());
                }
            }
        }
    }

    /**
     * Process text node.
     *
     * @param parentNode the parent node
     * @param upperJson the upper json
     * @param childJson the child json
     * @param childNode the child node
     */
    private static void processTextNode(Node parentNode, JsonObject upperJson, JsonObject childJson, Node childNode) {
        if (upperJson.has(parentNode.getNodeName())) {
            // upper already has such an element
            JsonElement existing = upperJson.get(parentNode.getNodeName());
            if (existing instanceof JsonArray) {
                // adding to the already reorganized array
                ((JsonArray) existing).add(new JsonPrimitive(childNode.getNodeValue()));
                REORGANIZED.put(parentNode.hashCode() + EMPTY, childJson);
            } else if (existing instanceof JsonObject) {
                // found it as an object, so reorganize it
                reorganizeObjectToArray(parentNode, upperJson, childJson, childNode, existing);
            } else {
                // found as a primitive, so reorganize it
                reorganizePrimitiveToArray(parentNode, upperJson, childJson, childNode, existing);
            }
        } else {
            // no such a node exists yet, so add it as a property to the upper element
            upperJson.addProperty(parentNode.getNodeName(), childNode.getNodeValue());
        }
        // add the parent node to the added as a value list
        ADDED_BY_VALUE.add(parentNode);
    }

    /**
     * Reorganize element.
     *
     * @param parentNode the parent node
     * @param parentJson the parent json
     * @param childJson the child json
     * @param childNode the child node
     * @param existing the existing
     */
    private static void reorganizeElement(Node parentNode, JsonObject parentJson, JsonObject childJson, Node childNode,
            JsonElement existing) {
        parentJson.remove(childNode.getNodeName());
        JsonArray arrayJson = new JsonArray();
        arrayJson.add(existing);
        arrayJson.add(childJson);
        parentJson.add(childNode.getNodeName(), arrayJson);
    }

    /**
     * Reorganize object to array.
     *
     * @param parentNode the parent node
     * @param upperJson the upper json
     * @param childJson the child json
     * @param childNode the child node
     * @param existing the existing
     */
    private static void reorganizeObjectToArray(Node parentNode, JsonObject upperJson, JsonObject childJson, Node childNode,
            JsonElement existing) {
        upperJson.remove(parentNode.getNodeName());
        JsonArray arrayJson = new JsonArray();
        arrayJson.add(existing);
        childJson.addProperty(childNode.getNodeName(), childNode.getNodeValue());
        arrayJson.add(childJson);
        upperJson.add(parentNode.getNodeName(), arrayJson);
        REORGANIZED.put(parentNode.hashCode() + EMPTY, childJson);
    }

    /**
     * Reorganize primitive to array.
     *
     * @param parentNode the parent node
     * @param upperJson the upper json
     * @param childJson the child json
     * @param childNode the child node
     * @param existing the existing
     */
    private static void reorganizePrimitiveToArray(Node parentNode, JsonObject upperJson, JsonObject childJson, Node childNode,
            JsonElement existing) {
        upperJson.remove(parentNode.getNodeName());
        JsonArray arrayJson = new JsonArray();
        arrayJson.add(existing);
        arrayJson.add(new JsonPrimitive(childNode.getNodeValue()));
        upperJson.add(parentNode.getNodeName(), arrayJson);
        REORGANIZED.put(parentNode.hashCode() + EMPTY, childJson);
    }

    /**
     * Reorganize add attributes.
     *
     * @param childNode the child node
     * @param attrs the attrs
     */
    private static void reorganizeAddAttributes(Node childNode, Iterator attrs) {
        JsonElement reorganizedJson = REORGANIZED.get(childNode.hashCode() + EMPTY);
        if (reorganizedJson instanceof JsonObject objectJson) {
            while (attrs.hasNext()) {
                Entry entry = (Entry) attrs.next();
                objectJson.addProperty(entry.getKey()
                                            .toString(),
                        entry.getValue()
                             .toString()
                             .replace(EQ, EMPTY));
            }
        } else {
            if (logger.isErrorEnabled()) {
                logger.error("Expected object, found element or null");
            }
        }
        REORGANIZED.remove(childNode.hashCode() + EMPTY);
    }

    /**
     * Reorganize for attributes.
     *
     * @param parentJson the parent json
     * @param childNode the child node
     * @param existing the existing
     * @param attrs the attrs
     */
    private static void reorganizeForAttributes(JsonObject parentJson, Node childNode, JsonElement existing, Iterator attrs) {
        parentJson.remove(childNode.getNodeName());
        JsonObject objectJson = new JsonObject();
        objectJson.addProperty(ATTR_TEXT, ((JsonPrimitive) existing).getAsString());
        while (attrs.hasNext()) {
            Entry entry = (Entry) attrs.next();
            objectJson.addProperty(entry.getKey()
                                        .toString(),
                    entry.getValue()
                         .toString()
                         .replace(EQ, EMPTY));
        }
        parentJson.add(childNode.getNodeName(), objectJson);
    }

    /**
     * Traverse attributes.
     *
     * @param childJson the child json
     * @param childNode the child node
     */
    private static void traverseAttributes(JsonObject childJson, Node childNode) {
        NamedNodeMap attrNodeMap = childNode.getAttributes();
        for (int j = 0; j < attrNodeMap.getLength(); j++) {
            Node attrNode = attrNodeMap.item(j);
            childJson.addProperty("-" + attrNode.getNodeName(), attrNode.getNodeValue());
        }
    }

    /**
     * To xml.
     *
     * @param json the json
     * @return the string
     */
    public static String toXml(String json) {
        JsonParser parser = new JsonParser();
        JsonElement rootJson = parser.parse(json);
        StringBuffer buff = new StringBuffer();
        serializeObjectAsXml((JsonObject) rootJson, buff);
        return buff.toString();
    }

    /**
     * Serialize object as xml.
     *
     * @param objectJson the object json
     * @param buff the buff
     */
    private static void serializeObjectAsXml(JsonObject objectJson, StringBuffer buff) {
        Iterator elements = objectJson.entrySet()
                                      .iterator();
        while (elements.hasNext()) {
            Entry entry = (Entry) elements.next();
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof JsonObject) {
                buff.append(LT)
                    .append(key);
                serializeObjectAttributes((JsonObject) value, buff);
                buff.append(GT);
                serializeObjectAsXml((JsonObject) value, buff);
                buff.append(LTS)
                    .append(key)
                    .append(GT);
            } else if (value instanceof JsonArray) {
                serializeArrayAsXml(buff, key, value);
            } else if (value instanceof JsonPrimitive) {
                if (ATTR_TEXT.equals(key)) {
                    buff.append(value.toString()
                                     .replace(EQ, EMPTY));
                } else if (ATTR_CDATA.equals(key)) {
                    buff.append(CDATA_OPEN)
                        .append(new String(Base64.getDecoder()
                                                 .decode(value.toString()
                                                              .replace(EQ, EMPTY)),
                                StandardCharsets.UTF_8))
                        .append(CDATA_CLOSE);
                } else {
                    if (!key.toString()
                            .startsWith("-")) {
                        buff.append(LT)
                            .append(key.toString())
                            .append(GT)
                            .append(value.toString()
                                         .replace(EQ, EMPTY))
                            .append(LTS)
                            .append(key.toString())
                            .append(GT);
                    }
                }
            } else {
                if (logger.isErrorEnabled()) {
                    logger.error("Unhandled element");
                }
            }
        }
    }

    /**
     * Serialize object attributes.
     *
     * @param objectJson the object json
     * @param buff the buff
     */
    private static void serializeObjectAttributes(JsonObject objectJson, StringBuffer buff) {
        Iterator elements = objectJson.entrySet()
                                      .iterator();
        while (elements.hasNext()) {
            Entry entry = (Entry) elements.next();
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof JsonPrimitive) {
                if (key.toString()
                       .startsWith("-")) {
                    buff.append(SPACE)
                        .append(key.toString()
                                   .substring(1))
                        .append(ESQ)
                        .append(value.toString()
                                     .replace(EQ, EMPTY))
                        .append(EQ);
                }
            }
        }
    }

    /**
     * Serialize array as xml.
     *
     * @param buff the buff
     * @param key the key
     * @param value the value
     */
    private static void serializeArrayAsXml(StringBuffer buff, Object key, Object value) {
        JsonArray array = (JsonArray) value;
        for (int i = 0; i < array.size(); i++) {
            JsonElement elementJson = array.get(i);
            if (elementJson instanceof JsonObject) {
                buff.append(LT)
                    .append(key);
                serializeObjectAttributes((JsonObject) elementJson, buff);
                buff.append(GT);
                serializeObjectAsXml((JsonObject) elementJson, buff);
                buff.append(LTS)
                    .append(key)
                    .append(GT);
            } else if (elementJson instanceof JsonArray) {
                serializeArrayAsXml(buff, key, elementJson);
            } else if (elementJson instanceof JsonPrimitive elementPrimitive) {
                if (ATTR_TEXT.equals(key)) {
                    buff.append(elementPrimitive.toString()
                                                .replace(EQ, EMPTY));
                } else if (ATTR_CDATA.equals(key)) {
                    buff.append(CDATA_OPEN)
                        .append(new String(Base64.getDecoder()
                                                 .decode(elementPrimitive.toString()
                                                                         .replace(EQ, EMPTY)),
                                StandardCharsets.UTF_8))
                        .append(CDATA_CLOSE);
                } else {
                    // System.err.println("ERROR: content attributes must be #text");
                    buff.append(LT)
                        .append(key);
                    buff.append(GT);
                    buff.append(elementPrimitive.toString()
                                                .replace(EQ, EMPTY));
                    buff.append(LTS)
                        .append(key)
                        .append(GT);
                }
            }
        }
    }

    /**
     * Print the XML with indentation.
     *
     * @param xml the xml
     * @return pretty xml
     * @throws TransformerFactoryConfigurationError the transformer factory configuration error
     * @throws ParserConfigurationException the parser configuration exception
     * @throws SAXException the SAX exception
     * @throws IOException Signals that an I/O exception has occurred.
     * @throws TransformerException the transformer exception
     */
    public String prettyPrintXml(String xml)
            throws TransformerFactoryConfigurationError, ParserConfigurationException, SAXException, IOException, TransformerException {
        Transformer transformer = TransformerFactory.newInstance()
                                                    .newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        StreamResult result = new StreamResult(new StringWriter());
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document doc = documentBuilder.parse(new InputSource(new StringReader(xml)));
        DOMSource source = new DOMSource(doc);
        transformer.transform(source, result);
        return result.getWriter()
                     .toString();
    }

    /**
     * Pretty print json.
     *
     * @param xml the xml
     * @return the string
     */
    public String prettyPrintJson(String xml) {
        return null;
    }
}
