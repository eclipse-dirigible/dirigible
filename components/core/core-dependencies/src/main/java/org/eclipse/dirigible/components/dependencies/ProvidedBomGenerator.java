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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Generates the provided-BOM at <b>build time</b> - invoked by the application build (see
 * {@code build/application/pom.xml}) on the output of {@code maven-dependency-plugin:list}, i.e.
 * the build's own resolved runtime dependencies, which are exactly what the Spring Boot repackage
 * puts into {@code BOOT-INF/lib}. Deriving the inventory from the resolved dependency list (and
 * never by unzipping the assembled artifact) keeps the BOM correct for reactor-built modules whose
 * jars do not live at local-repository paths.
 *
 * <p>
 * The output is a standard {@code dependencyManagement} POM
 * ({@code org.eclipse.dirigible:dirigible-provided-bom}), written into the application's classes as
 * {@link ProvidedBom#BOM_RESOURCE} so it ships inside the artifact itself. Entries are sorted, so
 * two builds of the same dependency set produce byte-identical BOMs.
 */
public final class ProvidedBomGenerator {

    /**
     * Instantiates are not needed.
     */
    private ProvidedBomGenerator() {
        // build-time tool
    }

    /**
     * Generates the BOM.
     *
     * @param args the dependency:list output file, the BOM output file and the BOM version
     * @throws IOException when the list is unreadable or the BOM cannot be written
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            throw new IllegalArgumentException("Usage: ProvidedBomGenerator <dependency-list-file> <bom-output-file> <bom-version>");
        }
        Path listFile = Path.of(args[0]);
        Path bomFile = Path.of(args[1]);
        String version = args[2];

        Map<String, String> provided = parseDependencyList(Files.readAllLines(listFile, StandardCharsets.UTF_8));
        if (provided.isEmpty()) {
            throw new IllegalStateException("The dependency list [" + listFile + "] yielded no artifacts - the provided-BOM would"
                    + " be empty and shadowing detection dead");
        }
        Files.createDirectories(bomFile.getParent());
        Files.writeString(bomFile, bom(provided, version), StandardCharsets.UTF_8);
    }

    /**
     * Parses {@code dependency:list} output lines - {@code groupId:artifactId:type[:classifier]:
     * version:scope}, one artifact per line, surrounded by header and module noise that is skipped.
     *
     * @param lines the output lines
     * @return the version per groupId:artifactId, sorted
     */
    static Map<String, String> parseDependencyList(List<String> lines) {
        Map<String, String> provided = new TreeMap<>();
        for (String line : lines) {
            String trimmed = line.trim();
            int space = trimmed.indexOf(' ');
            String candidate = space < 0 ? trimmed : trimmed.substring(0, space);
            String[] parts = candidate.split(":", -1);
            // g:a:type:v:scope or g:a:type:classifier:v:scope
            if (parts.length != 5 && parts.length != 6) {
                continue;
            }
            String type = parts[2];
            String version = parts[parts.length - 2];
            String scope = parts[parts.length - 1];
            if (!"jar".equals(type) || parts[0].isBlank() || parts[1].isBlank() || version.isBlank() || scope.isBlank()) {
                continue;
            }
            provided.putIfAbsent(parts[0] + ":" + parts[1], version);
        }
        return provided;
    }

    /**
     * The BOM content - a standard {@code dependencyManagement} POM.
     *
     * @param provided the version per groupId:artifactId, sorted
     * @param version the BOM version
     * @return the POM XML
     */
    static String bom(Map<String, String> provided, String version) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n");
        xml.append("    <modelVersion>4.0.0</modelVersion>\n");
        xml.append("    <groupId>org.eclipse.dirigible</groupId>\n");
        xml.append("    <artifactId>dirigible-provided-bom</artifactId>\n");
        xml.append("    <version>")
           .append(escape(version))
           .append("</version>\n");
        xml.append("    <packaging>pom</packaging>\n");
        xml.append("    <dependencyManagement>\n");
        xml.append("        <dependencies>\n");
        provided.forEach((groupArtifact, artifactVersion) -> {
            int separator = groupArtifact.indexOf(':');
            xml.append("            <dependency>\n");
            xml.append("                <groupId>")
               .append(escape(groupArtifact.substring(0, separator)))
               .append("</groupId>\n");
            xml.append("                <artifactId>")
               .append(escape(groupArtifact.substring(separator + 1)))
               .append("</artifactId>\n");
            xml.append("                <version>")
               .append(escape(artifactVersion))
               .append("</version>\n");
            xml.append("            </dependency>\n");
        });
        xml.append("        </dependencies>\n");
        xml.append("    </dependencyManagement>\n");
        xml.append("</project>\n");
        return xml.toString();
    }

    /**
     * Escapes the XML-special characters.
     *
     * @param value the value
     * @return the escaped value
     */
    private static String escape(String value) {
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
    }

}
