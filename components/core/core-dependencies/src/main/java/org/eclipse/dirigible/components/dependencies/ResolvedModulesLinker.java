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

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Maintains the resolved-modules directory - the stable directory the launch classpath includes
 * (see the loader.path entry in the application's Dockerfile). At runtime the resolved jars are
 * served by the swappable modules classloader; this directory is the seed that keeps them on the
 * classpath from the first moment of the next launch.
 */
@Component
class ResolvedModulesLinker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResolvedModulesLinker.class);

    /**
     * The resolved-modules directory - the configured location or the platform-owned default.
     *
     * @return the directory
     */
    Path directory() {
        String configured = DirigibleConfig.DEPENDENCIES_DIR.getStringValue();
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }
        return Path.of(System.getProperty("user.home"), ".dirigible", "resolved-modules");
    }

    /**
     * Synchronizes the directory with the resolved artifacts - new jars are linked in (symlink, copy
     * where symlinks are unavailable) and, when the resolution was complete, jars no longer resolved
     * are removed. After a partial resolution nothing is removed, so a transient repository outage
     * never strips previously activated jars from the next launch's classpath.
     *
     * @param localRepository the local repository the artifacts live in
     * @param artifacts the resolved jar paths
     * @param removeStale whether jars no longer resolved are removed
     */
    void sync(Path localRepository, List<Path> artifacts, boolean removeStale) {
        Path directory = directory();
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create the resolved-modules directory [" + directory + "]", e);
        }
        Map<String, Path> desired = new LinkedHashMap<>();
        artifacts.forEach(artifact -> desired.putIfAbsent(linkName(localRepository, artifact), artifact));
        if (removeStale) {
            removeStaleEntries(directory, desired.keySet());
        }
        desired.forEach((name, target) -> place(directory.resolve(name), target));
        // the directory is operator configuration - strip line breaks so a crafted value cannot
        // forge log entries
        LOGGER.info("Resolved-modules directory [{}] holds [{}] jar(s) - the seed of the next launch's classpath", directory.toString()
                                                                                                                            .replaceAll(
                                                                                                                                    "[\\r\\n]",
                                                                                                                                    "_"),
                desired.size());
    }

    /**
     * The link name - the groupId prefixes the artifact file name so equally named artifacts of
     * different groups never collide.
     *
     * @param localRepository the local repository
     * @param artifact the artifact path inside it
     * @return the link name
     */
    private String linkName(Path localRepository, Path artifact) {
        Path fileName = artifact.getFileName();
        Path versionDirectory = artifact.getParent();
        Path artifactDirectory = versionDirectory != null ? versionDirectory.getParent() : null;
        Path groupDirectory = artifactDirectory != null ? artifactDirectory.getParent() : null;
        if (groupDirectory == null || !artifact.startsWith(localRepository)) {
            return fileName.toString();
        }
        String groupId = localRepository.relativize(groupDirectory)
                                        .toString()
                                        .replace(localRepository.getFileSystem()
                                                                .getSeparator(),
                                                ".");
        return groupId + "-" + fileName;
    }

    /**
     * Removes the jars no longer resolved.
     *
     * @param directory the resolved-modules directory
     * @param desired the link names that stay
     */
    private void removeStaleEntries(Path directory, Set<String> desired) {
        try (Stream<Path> entries = Files.list(directory)) {
            entries.filter(entry -> entry.getFileName()
                                         .toString()
                                         .endsWith(".jar"))
                   .filter(entry -> !desired.contains(entry.getFileName()
                                                           .toString()))
                   .forEach(entry -> {
                       try {
                           Files.delete(entry);
                           LOGGER.info("Removed the no longer resolved dependency jar [{}] from [{}]", entry.getFileName(), directory);
                       } catch (IOException e) {
                           LOGGER.warn("Could not remove the stale dependency jar [{}]", entry, e);
                       }
                   });
        } catch (IOException e) {
            LOGGER.warn("Could not list the resolved-modules directory [{}]", directory, e);
        }
    }

    /**
     * Places one artifact - an existing correct symlink is kept, an existing regular file is kept
     * (versioned artifact paths are immutable, so the same name means the same content), anything else
     * is (re)created.
     *
     * @param link the link path in the resolved-modules directory
     * @param target the artifact path in the local repository
     */
    private void place(Path link, Path target) {
        try {
            if (Files.isSymbolicLink(link)) {
                if (target.equals(Files.readSymbolicLink(link))) {
                    return;
                }
                Files.delete(link);
            } else if (Files.exists(link)) {
                return;
            }
            try {
                Files.createSymbolicLink(link, target);
            } catch (IOException | UnsupportedOperationException e) {
                LOGGER.debug("Symbolic links are unavailable for [{}]; copying instead", link, e);
                Files.copy(target, link, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to place the resolved dependency [" + target + "] as [" + link + "]", e);
        }
    }

}
