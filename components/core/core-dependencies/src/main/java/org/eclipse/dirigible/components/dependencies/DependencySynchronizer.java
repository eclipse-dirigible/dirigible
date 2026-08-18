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

import org.eclipse.dirigible.components.base.dependencies.DependenciesChangedEvent;
import org.eclipse.dirigible.components.dependencies.ModuleJarInspector.Inspection;
import org.eclipse.dirigible.components.initializers.classpath.ClasspathExpander;
import org.eclipse.dirigible.engine.java.runtime.ModulesClassLoader;
import org.eclipse.dirigible.engine.java.runtime.ModulesClassLoaderHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The restartless dependency swap - reconciles the resolved JAR set into the running system as one
 * pipeline:
 *
 * <ol>
 * <li>Union resolution ran upstream (see {@code DependenciesService}) - the input here is its
 * resolved JAR set; a resolution with failures never reaches this point, so a broken declaration
 * leaves the installed generation serving (no partial swap).</li>
 * <li>Validate every arriving JAR before anything is touched: it must be a readable archive and
 * must not carry native libraries - the JVM binds a native library to exactly one classloader, so a
 * swappable loader would break on the first upgrade; such a dependency belongs to
 * {@code scope: "platform"} (a later phase).</li>
 * <li>Registry payload: the {@code META-INF/dirigible/&lt;project&gt;/**} content of removed JARs
 * leaves the registry, arriving JARs lay theirs in - the per-artefact synchronizers reconcile the
 * runtime state of those files on their next pass.</li>
 * <li>Swap the {@link ModulesClassLoaderHolder} to a fresh {@link ModulesClassLoader} generation
 * over the launch-classpath JARs plus the resolved set.</li>
 * <li>Publish a {@link DependenciesChangedEvent} - the Java engine reacts synchronously by
 * rediscovering AOT compiled modules through the new generation, invalidating its compile classpath
 * and rebuilding the client sources; other listeners (monitoring) observe the change.</li>
 * </ol>
 *
 * One swap runs at a time; the Java engine's own lock discipline serializes the triggered rebuild
 * with regular synchronization passes. A parent-first shadowed artifact (also present on the
 * platform classpath) is reported with a WARN - detection stays best-effort until the shadowing
 * report of a later phase.
 */
@Component
class DependencySynchronizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DependencySynchronizer.class);

    /** The loader holder. */
    private final ModulesClassLoaderHolder loaderHolder;

    /** The classpath expander. */
    private final ClasspathExpander classpathExpander;

    /** The event publisher. */
    private final ApplicationEventPublisher eventPublisher;

    /** The launch-classpath jars (loader.path / LOADER_PATH) - constant for the process lifetime. */
    private final List<Path> launchClasspathJars;

    /**
     * Instantiates a new dependency synchronizer.
     *
     * @param loaderHolder the loader holder
     * @param classpathExpander the classpath expander
     * @param eventPublisher the event publisher
     */
    DependencySynchronizer(ModulesClassLoaderHolder loaderHolder, ClasspathExpander classpathExpander,
            ApplicationEventPublisher eventPublisher) {
        this.loaderHolder = loaderHolder;
        this.classpathExpander = classpathExpander;
        this.eventPublisher = eventPublisher;
        this.launchClasspathJars = launchClasspathJars();
    }

    /**
     * The outcome of a swap attempt.
     *
     * @param swapped whether a new generation was installed
     * @param error the abort reason, null when the swap succeeded or nothing changed
     * @param added the arrived coordinates
     * @param removed the left coordinates
     */
    record SwapOutcome(boolean swapped, String error, Set<String> added, Set<String> removed) {

        /**
         * Kept the current generation.
         *
         * @param error the reason, null when nothing changed
         * @return the outcome
         */
        static SwapOutcome kept(String error) {
            return new SwapOutcome(false, error, Set.of(), Set.of());
        }
    }

    /**
     * Reconciles the resolved JAR set into a new modules-classloader generation.
     *
     * @param localRepository the local repository the artifacts live in, for coordinate derivation
     * @param resolvedJars the resolved JAR set
     * @param mediated the mediated versions, forwarded to the change event
     * @return the outcome
     */
    synchronized SwapOutcome swap(Path localRepository, List<Path> resolvedJars, Map<String, String> mediated) {
        List<Path> target = new ArrayList<>(launchClasspathJars);
        for (Path jar : resolvedJars) {
            if (!target.contains(jar)) {
                target.add(jar);
            }
        }

        ModulesClassLoader current = loaderHolder.current();
        Set<Path> currentJars = new LinkedHashSet<>(current.jars());
        if (loaderHolder.generation() > 0 && currentJars.equals(new LinkedHashSet<>(target))) {
            return SwapOutcome.kept(null);
        }

        List<Path> added = target.stream()
                                 .filter(jar -> !currentJars.contains(jar))
                                 .toList();
        List<Path> removed = currentJars.stream()
                                        .filter(jar -> !target.contains(jar))
                                        .toList();

        // validate everything BEFORE the first side effect - an aborted swap leaves generation N
        // installed and serving, with no partial state anywhere
        Map<Path, Inspection> inspections = new LinkedHashMap<>();
        for (Path jar : added) {
            Inspection inspection;
            try {
                inspection = ModuleJarInspector.inspect(jar);
            } catch (IOException e) {
                String error = "Jar [" + jar + "] is not a readable archive - keeping the installed generation. Cause: " + e.getMessage();
                LOGGER.error("Dependency swap aborted: {}", error, e);
                return SwapOutcome.kept(error);
            }
            if (!inspection.nativeLibraries()
                           .isEmpty()) {
                String error = "Jar [" + jar.getFileName() + "] contains native libraries " + inspection.nativeLibraries()
                        + " which the swappable module tier cannot host (a native library binds to exactly one classloader)."
                        + " Declare it with scope \"platform\" once supported, or bake it into the image.";
                LOGGER.error("Dependency swap aborted: {}", error);
                return SwapOutcome.kept(error);
            }
            inspections.put(jar, inspection);
        }

        warnOnPlatformShadowing(inspections);
        reconcileRegistryPayload(added, removed, inspections);

        loaderHolder.swap(target);
        int generation = loaderHolder.generation();

        Set<String> addedCoordinates = coordinates(localRepository, added);
        Set<String> removedCoordinates = coordinates(localRepository, removed);
        if (!addedCoordinates.isEmpty() || !removedCoordinates.isEmpty()) {
            LOGGER.info("Dependency layer swapped to generation [{}]: added {}, removed {}", generation, addedCoordinates,
                    removedCoordinates);
            eventPublisher.publishEvent(new DependenciesChangedEvent(this, addedCoordinates, removedCoordinates, mediated, generation));
        }
        return new SwapOutcome(true, null, addedCoordinates, removedCoordinates);
    }

    /**
     * Removes the registry payload of leaving JARs and lays the payload of arriving ones. A project
     * carried by a JAR that stays is never removed, and an upgraded module's project is removed and
     * immediately re-laid from the new JAR.
     *
     * @param added the arriving jars
     * @param removed the leaving jars
     * @param inspections the arriving jars' inspections
     */
    private void reconcileRegistryPayload(List<Path> added, List<Path> removed, Map<Path, Inspection> inspections) {
        Set<String> removedProjects = new LinkedHashSet<>();
        for (Path jar : removed) {
            try {
                removedProjects.addAll(ModuleJarInspector.inspect(jar)
                                                         .projects());
            } catch (IOException e) {
                // the immutable local-repo file was deleted externally - its payload cannot be
                // attributed any more; the per-artefact synchronizers will reap orphans over time
                LOGGER.warn("Cannot inspect the removed jar [{}] for its registry payload", jar, e);
            }
        }
        if (!removedProjects.isEmpty()) {
            // a project is only removed when NO remaining jar still carries it
            ModulesClassLoader current = loaderHolder.current();
            for (Path staying : current.jars()) {
                if (removed.contains(staying) || !Files.isRegularFile(staying)) {
                    continue;
                }
                try {
                    removedProjects.removeAll(ModuleJarInspector.inspect(staying)
                                                                .projects());
                } catch (IOException e) {
                    LOGGER.warn("Cannot inspect the staying jar [{}] while removing registry payload", staying, e);
                }
            }
            removedProjects.forEach(classpathExpander::remove);
        }
        for (Path jar : added) {
            if (!inspections.get(jar)
                            .projects()
                            .isEmpty()) {
                classpathExpander.expand(jar);
            }
        }
    }

    /**
     * WARN when an arriving artifact is also present on the platform classpath - parent-first
     * delegation resolves such classes to the platform's version, so the declared version is inert.
     *
     * @param inspections the arriving jars' inspections
     */
    private void warnOnPlatformShadowing(Map<Path, Inspection> inspections) {
        ClassLoader platform = getClass().getClassLoader();
        inspections.forEach((jar, inspection) -> {
            String probe = inspection.representativeClassResource();
            if (probe != null && platform.getResource(probe) != null) {
                LOGGER.warn("The resolved artifact [{}] is also present on the platform classpath - parent-first delegation serves "
                        + "the platform's version, not the declared one", jar.getFileName());
            }
        });
    }

    /**
     * Derives {@code groupId:artifactId:version} coordinates from local-repository paths; a path
     * outside the local repository (a launch-classpath jar) reports its file name.
     *
     * @param localRepository the local repository
     * @param jars the jars
     * @return the coordinates
     */
    private static Set<String> coordinates(Path localRepository, List<Path> jars) {
        Set<String> coordinates = new LinkedHashSet<>();
        for (Path jar : jars) {
            coordinates.add(coordinate(localRepository, jar));
        }
        return coordinates;
    }

    /**
     * Coordinate of one jar.
     *
     * @param localRepository the local repository
     * @param jar the jar
     * @return the coordinate, or the file name when the jar is not a local-repository artifact
     */
    private static String coordinate(Path localRepository, Path jar) {
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

    /**
     * The jars already on the launch classpath via {@code loader.path} / {@code LOADER_PATH} - they
     * seed every generation, so the drop-in {@code /modules} behavior stays intact (their classes
     * resolve parent-first from the application classloader anyway).
     *
     * @return the launch-classpath jars
     */
    private static List<Path> launchClasspathJars() {
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
                try (var stream = Files.list(path)) {
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

}
