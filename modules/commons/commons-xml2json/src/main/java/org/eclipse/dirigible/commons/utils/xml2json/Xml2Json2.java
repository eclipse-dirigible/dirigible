/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.commons.utils.xml2json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Utility class for performing guaranteed lossless and reversible conversion between XML Documents
 * and structured JSON (using Jackson's Tree Model). This class handles: 1. Root Element name
 * preservation. 2. Attributes, child elements, and simple text content. 3. Special XML nodes: CDATA
 * sections and Comments. Custom JSON keys for fidelity: - "_text": Used for simple element text
 * content. - "_cdata": Used for CDATA section content. - "_comment": Used for XML comment content.
 */
public class Xml2Json2 {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // --- Reserved Keys for Lossless Reversal ---
    private static final String KEY_TEXT = "_text";
    private static final String KEY_CDATA = "_cdata";
    private static final String KEY_COMMENT = "_comment";
    // Jackson's default for repeated elements is an array, which we preserve.

    /**
     * Converts an XML string into a structured, lossless JSON string. The root XML element is preserved
     * as the top-level JSON key.
     *
     * @param xmlString The XML content to convert.
     * @return The resulting pretty-printed JSON string.
     */
    public static String toJson(String xmlString) throws Exception {
        Document doc = parseXmlToDom(xmlString);
        Element rootElement = doc.getDocumentElement();

        ObjectNode rootNode = MAPPER.createObjectNode();
        ObjectNode contentNode = MAPPER.createObjectNode();

        // Recursively convert XML element and its children
        convertXmlElementToJson(rootElement, contentNode);

        // Wrap the content with the original root tag name (e.g., {"a": {...}})
        rootNode.set(rootElement.getNodeName(), contentNode);

        return MAPPER.writerWithDefaultPrettyPrinter()
                     .writeValueAsString(rootNode);
    }

    /**
     * Recursive method to convert an XML Element and its children to a JSON ObjectNode. This handles
     * element attributes, text, CDATA, and comments.
     *
     * @param element The current XML Element being processed.
     * @param node The JSON ObjectNode corresponding to the element.
     */
    private static void convertXmlElementToJson(Element element, ObjectNode node) {

        // Handle ATTRIBUTES (Jackson's default handles these)
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attr = attributes.item(i);
            // Attributes are simply added as key-value pairs to the element's node
            node.put(attr.getNodeName(), attr.getNodeValue());
        }

        // Handle CHILD NODES (Elements, Text, CDATA, Comments)
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if (child.getNodeType() == Node.ELEMENT_NODE) {
                // If the child is an ELEMENT, we recurse.
                String childName = child.getNodeName();
                ObjectNode childNode = MAPPER.createObjectNode();
                convertXmlElementToJson((Element) child, childNode);

                // Handle repeating elements: If the key already exists, convert to an ArrayNode
                if (node.has(childName)) {
                    JsonNode existing = node.get(childName);
                    if (existing.isArray()) {
                        ((ArrayNode) existing).add(childNode);
                    } else {
                        ArrayNode arrayNode = MAPPER.createArrayNode();
                        arrayNode.add(existing);
                        arrayNode.add(childNode);
                        node.set(childName, arrayNode);
                    }
                } else {
                    node.set(childName, childNode);
                }

            } else if (child.getNodeType() == Node.TEXT_NODE && !child.getNodeValue()
                                                                      .trim()
                                                                      .isEmpty()) {
                // Handle TEXT node (used for mixed content or simple text)
                node.put(KEY_TEXT, child.getNodeValue()
                                        .trim());
            } else if (child.getNodeType() == Node.CDATA_SECTION_NODE) {
                // Handle CDATA node (Crucial for lossless)
                node.put(KEY_CDATA, child.getNodeValue());
            } else if (child.getNodeType() == Node.COMMENT_NODE) {
                // Handle COMMENT node (Crucial for lossless)
                node.put(KEY_COMMENT, child.getNodeValue());
            }
        }
    }

    /**
     * Converts a structured, lossless JSON string back into an XML string.
     *
     * @param jsonString The structured JSON content to convert.
     * @return The resulting XML string.
     */
    public static String toXml(String jsonString) throws Exception {
        JsonNode rootJson = MAPPER.readTree(jsonString);
        if (!rootJson.isObject() || rootJson.size() != 1) {
            throw new IllegalArgumentException("JSON must contain a single root object key.");
        }

        // Get the single root key (e.g., "a")
        String rootName = rootJson.fieldNames()
                                  .next();
        JsonNode contentJson = rootJson.get(rootName);

        // Create a new XML Document
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        // Create the root element
        Element rootElement = doc.createElement(rootName);
        doc.appendChild(rootElement);

        // Recursively convert JSON to XML
        convertJsonToXmlElement(contentJson, rootElement, doc);

        // Transform the DOM Document to a formatted XML string
        return transformDomToXml(doc);
    }

    /**
     * Recursive method to convert a JSON Node back into an XML Element.
     *
     * @param jsonNode The current JSON Node being processed.
     * @param xmlElement The corresponding XML Element.
     * @param doc The XML Document instance.
     */
    private static void convertJsonToXmlElement(JsonNode jsonNode, Element xmlElement, Document doc) {
        if (jsonNode.isObject()) {
            jsonNode.fields()
                    .forEachRemaining(entry -> {
                        String key = entry.getKey();
                        JsonNode value = entry.getValue();

                        // Check for special reserved keys (Lossless features)
                        if (key.equals(KEY_TEXT)) {
                            // Add simple text content
                            xmlElement.appendChild(doc.createTextNode(value.asText()));
                        } else if (key.equals(KEY_CDATA)) {
                            // Add CDATA section
                            xmlElement.appendChild(doc.createCDATASection(value.asText()));
                        } else if (key.equals(KEY_COMMENT)) {
                            // Add XML comment
                            xmlElement.appendChild(doc.createComment(value.asText()));
                        } else if (value.isArray()) {
                            // Handle arrays (Repeated Elements)
                            value.forEach(item -> {
                                Element childElement = doc.createElement(key);
                                convertJsonToXmlElement(item, childElement, doc);
                                xmlElement.appendChild(childElement);
                            });
                        } else if (value.isObject()) {
                            // Handle nested objects (Child Element)
                            Element childElement = doc.createElement(key);
                            convertJsonToXmlElement(value, childElement, doc);
                            xmlElement.appendChild(childElement);
                        } else {
                            // Everything else is treated as an ATTRIBUTE (Jackson convention)
                            xmlElement.setAttribute(key, value.asText());
                        }
                    });
        }
    }


    private static Document parseXmlToDom(String xmlString) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // Crucial for recognizing CDATA/Comments correctly
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xmlString));
        return builder.parse(is);
    }

    private static String transformDomToXml(Document doc) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

}
