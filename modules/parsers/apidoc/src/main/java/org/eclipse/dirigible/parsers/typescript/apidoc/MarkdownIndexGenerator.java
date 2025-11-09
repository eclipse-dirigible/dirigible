/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.parsers.typescript.apidoc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MarkdownIndexGenerator {

    /**
     * Generate index.md files in each directory, including a master index in the root.
     *
     * @param rootDir the documentation root directory
     */
    public static void generateMarkdownIndexes(Path rootDir) {
        try (Stream<Path> walk = Files.walk(rootDir)) {

            List<Path> directories = walk.filter(Files::isDirectory)
                                         .sorted()
                                         .collect(Collectors.toList());

            // Generate index.md per directory
            for (Path dir : directories) {
                generateIndexForDirectory(dir);
            }

            // Generate master root index.md
            generateMasterIndex(rootDir);

        } catch (IOException e) {
            throw new RuntimeException("Failed generating markdown indexes", e);
        }
    }

    private static void generateIndexForDirectory(Path dir) throws IOException {

        List<Path> mdFiles;
        try (Stream<Path> files = Files.list(dir)) {

            mdFiles = files.filter(Files::isRegularFile)
                           .filter(p -> p.getFileName()
                                         .toString()
                                         .endsWith(".md"))
                           .filter(p -> !p.getFileName()
                                          .toString()
                                          .equals("index.md"))
                           .sorted(Comparator.comparing(Path::getFileName))
                           .collect(Collectors.toList());
        }

        if (mdFiles.isEmpty()) {
            return;
        }

        String title = dir.getFileName()
                          .toString();
        title = title.isEmpty() ? "Documentation" : formatTitle(title);

        StringBuilder indexContent = new StringBuilder("# " + title + "\n\n");

        for (Path md : mdFiles) {
            String fileName = md.getFileName()
                                .toString();
            String cleanName = fileName.substring(0, fileName.length() - 3);
            indexContent.append("- [")
                        .append(formatTitle(cleanName))
                        .append("](")
                        .append(fileName)
                        .append(")\n");
        }

        Files.write(dir.resolve("index.md"), indexContent.toString()
                                                         .getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Generate a master index.md linking to ALL markdown files under the root.
     */
    private static void generateMasterIndex(Path rootDir) throws IOException {

        Map<Path, List<Path>> grouped = new TreeMap<>();

        try (Stream<Path> walk = Files.walk(rootDir)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> p.getFileName()
                              .toString()
                              .endsWith(".md"))
                .filter(p -> !p.getFileName()
                               .toString()
                               .equals("index.md"))
                .forEach(md -> {
                    Path parent = md.getParent();
                    grouped.computeIfAbsent(parent, k -> new ArrayList<>())
                           .add(md);
                });
        }

        StringBuilder index = new StringBuilder("# API Index\n\n");

        for (Map.Entry<Path, List<Path>> entry : grouped.entrySet()) {

            Path dir = entry.getKey();
            List<Path> files = entry.getValue()
                                    .stream()
                                    .sorted()
                                    .collect(Collectors.toList());

            String groupName = dir.equals(rootDir) ? "ROOT"
                    : dir.getFileName()
                         .toString()
                         .toUpperCase();

            index.append("## ")
                 .append(groupName)
                 .append("\n\n");

            for (Path md : files) {
                Path rel = rootDir.relativize(md);
                String cleanName = md.getFileName()
                                     .toString()
                                     .replace(".md", "");
                index.append("- [")
                     .append(formatTitle(cleanName))
                     .append("](")
                     .append(rel.toString()
                                .replace("\\", "/"))
                     .append(")\n");
            }

            index.append("\n");
        }

        Files.write(rootDir.resolve("index.md"), index.toString()
                                                      .getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }


    /**
     * Convert CamelCase → "Camel Case"
     */
    private static String formatTitle(String raw) {
        return raw.replaceAll("([a-z])([A-Z])", "$1 $2")
                  .replace("-", " ")
                  .replace("_", " ")
                  .trim();
    }
}
