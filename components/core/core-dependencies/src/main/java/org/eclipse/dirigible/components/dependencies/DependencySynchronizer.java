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
        this.launchClasspathJars = DependencyPaths.launchClasspathJars();
    }

    /**
     * The outcome of a swap attempt.
     *
     * @param swapped whether a new generation was installed
     * @param error the abort reason, null when the swap succeeded or nothing changed
     * @param added the arrived coordinates
     * @param removed the left coordinates
     * @param rejected the coordinates the module tier refused, with the reason - the rest of the
     *        resolution still activated
     * @param shadowed the arrived coordinates the launch classpath already provides, so parent-first
     *        delegation keeps serving the launch-classpath version and the arrival is inert
     * @param payloadError the registry-payload reconciliation failure, null when it succeeded
     */
    record SwapOutcome(boolean swapped, String error, Set<String> added, Set<String> removed, Map<String, String> rejected,
            Set<String> shadowed, String payloadError) {

        /**
         * Kept the current generation.
         *
         * @param error the reason, null when nothing changed
         * @return the outcome
         */
        static SwapOutcome kept(String error) {
            return new SwapOutcome(false, error, Set.of(), Set.of(), Map.of(), Set.of(), null);
        }

        /**
         * Kept the current generation, with per-declaration rejections to report.
         *
         * @param rejected the rejected coordinates and their reasons
         * @return the outcome
         */
        static SwapOutcome kept(Map<String, String> rejected) {
            return new SwapOutcome(false, null, Set.of(), Set.of(), rejected, Set.of(), null);
        }
    }

    /**
     * Reconciles the resolved JAR sets into a new modules-classloader generation. The platform-tier
     * JARs ride in the generation too - that puts them on the compile classpath for registry
     * {@code .java} sources and fires the rebuild event; at runtime parent-first delegation serves them
     * from the system classloader once appended. They skip the module-tier validation (native libraries
     * are exactly what the platform tier exists for) and never carry registry payload.
     *
     * @param localRepository the local repository the artifacts live in, for coordinate derivation
     * @param moduleJars the resolved module-tier JAR set
     * @param platformJars the resolved platform-tier JAR set
     * @param mediated the mediated versions, forwarded to the change event
     * @return the outcome
     */
    synchronized SwapOutcome swap(Path localRepository, List<Path> moduleJars, List<Path> platformJars, Map<String, String> mediated) {
        List<Path> target = new ArrayList<>(launchClasspathJars);
        for (Path jar : platformJars) {
            if (!target.contains(jar)) {
                target.add(jar);
            }
        }
        for (Path jar : moduleJars) {
            if (!target.contains(jar)) {
                target.add(jar);
            }
        }

        ModulesClassLoader current = loaderHolder.current();
        Set<Path> currentJars = new LinkedHashSet<>(current.jars());
        Set<Path> platformTier = new LinkedHashSet<>(platformJars);
        Set<Path> launchTier = new LinkedHashSet<>(launchClasspathJars);

        // validate everything BEFORE the first side effect - an aborted swap leaves generation N
        // installed and serving, with no partial state anywhere. Only arriving module-tier jars are
        // validated: a launch-classpath jar is already loaded by the application classloader, so
        // vetoing it here would abort every swap for the lifetime of the process over a drop-in
        // nobody declared.
        Map<Path, Inspection> inspections = new LinkedHashMap<>();
        Map<String, String> rejected = new LinkedHashMap<>();
        Set<Path> rejectedJars = new LinkedHashSet<>();
        for (Path jar : target) {
            if (currentJars.contains(jar) || platformTier.contains(jar) || launchTier.contains(jar)) {
                continue;
            }
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
                // per-declaration, not per-swap: one library shipping a native resource must not keep
                // every other project's dependencies from ever activating
                String reason = "Contains native libraries " + inspection.nativeLibraries()
                        + " which the swappable module tier cannot host (a native library binds to exactly one classloader)."
                        + " Declare it with scope \"platform\" instead, or bake it into the image.";
                LOGGER.error("Rejecting the module-tier dependency [{}]: {}", jar.getFileName(), reason);
                rejected.put(coordinate(localRepository, jar), reason);
                rejectedJars.add(jar);
                continue;
            }
            inspections.put(jar, inspection);
        }
        if (!rejectedJars.isEmpty()) {
            target.removeAll(rejectedJars);
            inspections.keySet()
                       .removeAll(rejectedJars);
        }

        if (loaderHolder.generation() > 0 && currentJars.equals(new LinkedHashSet<>(target))) {
            return SwapOutcome.kept(rejected);
        }

        List<Path> added = target.stream()
                                 .filter(jar -> !currentJars.contains(jar))
                                 .toList();
        List<Path> removed = currentJars.stream()
                                        .filter(jar -> !target.contains(jar))
                                        .toList();
        List<Path> addedModuleTier = added.stream()
                                          .filter(jar -> !platformTier.contains(jar) && !launchTier.contains(jar))
                                          .toList();
        List<Path> removedModuleTier = removed.stream()
                                              .filter(jar -> !platformTier.contains(jar) && !launchTier.contains(jar))
                                              .toList();

        Set<Path> shadowedJars = shadowedByLaunchClasspath(inspections);

        // the classloader generation is the authoritative half of the swap and is installed first;
        // the registry payload follows, guarded - a repository write failure must not leave the
        // pipeline reporting a 500 with a half-removed payload and no new generation
        loaderHolder.swap(target);
        int generation = loaderHolder.generation();
        String payloadError = null;
        try {
            reconcileRegistryPayload(addedModuleTier, removedModuleTier, inspections);
        } catch (RuntimeException e) {
            payloadError = "The dependency layer swapped to generation [" + generation
                    + "], but reconciling the registry payload failed - some module content may be missing or stale. Cause: "
                    + e.getMessage();
            LOGGER.error("Registry payload reconciliation failed after the swap to generation [{}]", generation, e);
        }

        Set<String> addedCoordinates = coordinates(localRepository, added);
        Set<String> removedCoordinates = coordinates(localRepository, removed);
        if (!addedCoordinates.isEmpty() || !removedCoordinates.isEmpty()) {
            LOGGER.info("Dependency layer swapped to generation [{}]: added {}, removed {}", generation, addedCoordinates,
                    removedCoordinates);
            eventPublisher.publishEvent(new DependenciesChangedEvent(this, addedCoordinates, removedCoordinates, mediated, generation));
        }
        return new SwapOutcome(true, null, addedCoordinates, removedCoordinates, rejected,
                coordinates(localRepository, List.copyOf(shadowedJars)), payloadError);
    }

    /**
     * Removes the registry payload of leaving JARs and lays the payload of arriving ones. Only the
     * entries a leaving JAR actually carried are removed, and only those no remaining JAR still carries
     * - an upgrade is a remove followed by a re-expand, so removing the whole project collection would
     * destroy whatever else was published under it.
     *
     * @param added the arriving jars
     * @param removed the leaving jars
     * @param inspections the arriving jars' inspections
     */
    private void reconcileRegistryPayload(List<Path> added, List<Path> removed, Map<Path, Inspection> inspections) {
        Set<String> removedEntries = new LinkedHashSet<>();
        for (Path jar : removed) {
            try {
                removedEntries.addAll(ModuleJarInspector.inspect(jar)
                                                        .registryEntries());
            } catch (IOException e) {
                // the immutable local-repo file was deleted externally - its payload cannot be
                // attributed any more; the per-artefact synchronizers will reap orphans over time
                LOGGER.warn("Cannot inspect the removed jar [{}] for its registry payload", jar, e);
            }
        }
        if (!removedEntries.isEmpty()) {
            // an entry is only removed when NO remaining jar still carries it
            ModulesClassLoader current = loaderHolder.current();
            for (Path staying : current.jars()) {
                if (removed.contains(staying) || !Files.isRegularFile(staying)) {
                    continue;
                }
                try {
                    removedEntries.removeAll(ModuleJarInspector.inspect(staying)
                                                               .registryEntries());
                } catch (IOException e) {
                    LOGGER.warn("Cannot inspect the staying jar [{}] while removing registry payload", staying, e);
                }
            }
            classpathExpander.remove(removedEntries);
        }
        for (Path jar : added) {
            Inspection inspection = inspections.get(jar);
            if (inspection != null && !inspection.projects()
                                                 .isEmpty()) {
                classpathExpander.expand(jar);
            }
        }
    }

    /**
     * The arriving artifacts the launch classpath already provides - parent-first delegation resolves
     * such classes to the launch-classpath version, so the declared one never serves. Reported as
     * {@code shadowed} rather than active: a swap that silently changed nothing is the one outcome an
     * operator cannot debug.
     *
     * @param inspections the arriving jars' inspections
     * @return the shadowed jars
     */
    private Set<Path> shadowedByLaunchClasspath(Map<Path, Inspection> inspections) {
        Set<Path> shadowed = new LinkedHashSet<>();
        ClassLoader platform = getClass().getClassLoader();
        inspections.forEach((jar, inspection) -> {
            String probe = inspection.representativeClassResource();
            if (probe != null && platform.getResource(probe) != null) {
                shadowed.add(jar);
                LOGGER.warn("The resolved artifact [{}] is also present on the launch classpath - parent-first delegation serves "
                        + "that version, not the declared one; the arrival is inert", jar.getFileName());
            }
        });
        return shadowed;
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
        return DependencyPaths.coordinate(localRepository, jar);
    }


}
