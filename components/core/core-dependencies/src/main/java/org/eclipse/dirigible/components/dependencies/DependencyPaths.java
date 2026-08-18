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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Path derivations shared by the dependency tiers: the jars already on the launch classpath and the
 * coordinate a local-repository artifact path encodes.
 */
final class DependencyPaths {

    private static final Logger LOGGER = LoggerFactory.getLogger(DependencyPaths.class);

    /**
     * Instantiates are not needed.
     */
    private DependencyPaths() {
        // utility
    }

    /**
     * The jars already on the launch classpath via {@code loader.path} / {@code LOADER_PATH} - the
     * {@code /modules} drop-in directory and the resolved-modules seed. Constant for the process
     * lifetime.
     *
     * @return the launch-classpath jars
     */
    static List<Path> launchClasspathJars() {
        String property = System.getProperty("loader.path");
        String raw = property != null ? property : System.getenv("LOADER_PATH");
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<Path> jars = new ArrayList<>();
        for (String segment : raw.split(",")) {
            String trimmed = segment.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            Path path = Path.of(trimmed);
            if (Files.isDirectory(path)) {
                try (Stream<Path> stream = Files.list(path)) {
                    stream.filter(candidate -> candidate.toString()
                                                        .endsWith(".jar"))
                          .sorted()
                          .forEach(jars::add);
                } catch (IOException e) {
                    LOGGER.warn("Could not list the loader.path directory [{}]", path, e);
                }
            } else if (Files.isRegularFile(path) && trimmed.endsWith(".jar")) {
                jars.add(path);
            }
        }
        return List.copyOf(jars);
    }

    /**
     * Derives the {@code groupId:artifactId:version} coordinate from a local-repository artifact path;
     * a path outside the local repository (a launch-classpath jar) reports its file name.
     *
     * @param localRepository the local repository
     * @param jar the jar
     * @return the coordinate, or the file name when the jar is not a local-repository artifact
     */
    static String coordinate(Path localRepository, Path jar) {
        if (!jar.startsWith(localRepository)) {
            return String.valueOf(jar.getFileName());
        }
        Path relative = localRepository.relativize(jar);
        int segments = relative.getNameCount();
        if (segments < 4) {
            return String.valueOf(jar.getFileName());
        }
        String version = relative.getName(segments - 2)
                                 .toString();
        String artifactId = relative.getName(segments - 3)
                                    .toString();
        String groupId = relative.subpath(0, segments - 3)
                                 .toString()
                                 .replace(relative.getFileSystem()
                                                  .getSeparator(),
                                         ".");
        return groupId + ":" + artifactId + ":" + version;
    }

}
