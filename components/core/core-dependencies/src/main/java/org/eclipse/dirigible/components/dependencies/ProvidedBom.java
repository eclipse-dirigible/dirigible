/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.dependencies;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The provided-BOM: the {@code groupId:artifactId:version} inventory of every JAR the platform's
 * fat jar ships in {@code BOOT-INF/lib}, generated at build time from the build's own resolved
 * dependencies and embedded as {@code META-INF/dirigible-provided-bom.xml} - so an offline or
 * air-gapped instance knows what is provided without any remote fetch, and a distribution building
 * its own fat jar embeds its own inventory.
 *
 * <p>
 * The resolver treats every listed coordinate as <b>provided</b>: never downloaded into the modules
 * tier, never appended to the system classloader. A project declaring a listed
 * {@code groupId:artifactId} at the platform's version is simply satisfied; at a <b>different</b>
 * version it is reported as {@code shadowed} with both versions - loudly, in the log, the endpoint
 * and the IDE view.
 *
 * <p>
 * <b>Why the report is the fix, and child-first loading was rejected.</b> The modules classloader
 * is deliberately parent-first, so a platform-provided class always wins and a differing declared
 * version is inert. Child-first ("give the project the Jackson it asked for") looks like the
 * obliging alternative, but it trades a <i>visible</i> problem for an <i>invisible</i> one: the
 * platform's own code keeps loading its version while module code loads another, and the two meet
 * across every API boundary that passes such an object - producing {@code LinkageError}s and
 * {@code ClassCastException}s of the same FQN at arbitrary call sites, at arbitrary times, with
 * stack traces that name neither declaration. A {@code shadowed} report names the exact coordinate
 * and both versions at resolution time; that is the feature.
 */
final class ProvidedBom {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvidedBom.class);

    /** The embedded BOM resource the application build generates. */
    static final String BOM_RESOURCE = "META-INF/dirigible-provided-bom.xml";

    /** The classpath BOM, loaded once - the embedded resource cannot change within a process. */
    private static volatile ProvidedBom classpathBom;

    /** The provided version per groupId:artifactId. */
    private final Map<String, String> providedVersions;

    /**
     * Instantiates a new provided BOM.
     *
     * @param providedVersions the provided version per groupId:artifactId
     */
    ProvidedBom(Map<String, String> providedVersions) {
        this.providedVersions = Map.copyOf(providedVersions);
    }

    /**
     * The BOM embedded in the running platform - empty when the classpath carries none (plain unit
     * tests, exploded developer runs of a single module).
     *
     * @return the provided BOM
     */
    static ProvidedBom fromClasspath() {
        ProvidedBom bom = classpathBom;
        if (bom == null) {
            synchronized (ProvidedBom.class) {
                bom = classpathBom;
                if (bom == null) {
                    bom = load();
                    classpathBom = bom;
                }
            }
        }
        return bom;
    }

    /**
     * Loads the embedded BOM resource.
     *
     * @return the provided BOM, empty when absent or unreadable
     */
    private static ProvidedBom load() {
        try (InputStream in = ProvidedBom.class.getClassLoader()
                                               .getResourceAsStream(BOM_RESOURCE)) {
            if (in == null) {
                LOGGER.info("No provided-BOM on the classpath ([{}]) - shadowing detection is inactive", BOM_RESOURCE);
                return new ProvidedBom(Map.of());
            }
            ProvidedBom bom = parse(in);
            LOGGER.info("Provided-BOM loaded: the platform provides [{}] artifact(s)", bom.providedVersions.size());
            return bom;
        } catch (IOException | ParserConfigurationException | SAXException e) {
            LOGGER.error("The provided-BOM [{}] is unreadable - shadowing detection is inactive", BOM_RESOURCE, e);
            return new ProvidedBom(Map.of());
        }
    }

    /**
     * Parses a standard {@code dependencyManagement} BOM.
     *
     * @param in the BOM content
     * @return the provided BOM
     * @throws ParserConfigurationException when the parser cannot be configured
     * @throws SAXException when the content is not well-formed XML
     * @throws IOException when the content is unreadable
     */
    static ProvidedBom parse(InputStream in) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        Document document = factory.newDocumentBuilder()
                                   .parse(in);
        Map<String, String> versions = new LinkedHashMap<>();
        NodeList dependencies = document.getElementsByTagName("dependency");
        for (int index = 0; index < dependencies.getLength(); index++) {
            Element dependency = (Element) dependencies.item(index);
            String groupId = childText(dependency, "groupId");
            String artifactId = childText(dependency, "artifactId");
            String version = childText(dependency, "version");
            if (groupId != null && artifactId != null && version != null) {
                versions.putIfAbsent(groupId + ":" + artifactId, version);
            }
        }
        return new ProvidedBom(versions);
    }

    /**
     * The version the platform provides for a groupId:artifactId.
     *
     * @param groupArtifact the groupId:artifactId
     * @return the provided version, null when the platform does not provide the artifact
     */
    String providedVersion(String groupArtifact) {
        return providedVersions.get(groupArtifact);
    }

    /**
     * Whether the BOM lists nothing.
     *
     * @return true when empty
     */
    boolean isEmpty() {
        return providedVersions.isEmpty();
    }

    /**
     * The text of a direct child element.
     *
     * @param parent the parent element
     * @param name the child element name
     * @return the trimmed text, null when the child is absent or blank
     */
    private static String childText(Element parent, String name) {
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE && name.equals(child.getNodeName())) {
                String text = child.getTextContent();
                return text == null || text.isBlank() ? null : text.trim();
            }
        }
        return null;
    }

}
