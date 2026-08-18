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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Frozen-mode resolution ({@code DIRIGIBLE_DEPENDENCIES_FROZEN=true}): the activated set comes from
 * the lockfile alone. No remote repository is consulted, no version is re-mediated and no new
 * coordinate is accepted - a declaration the lock does not carry is rejected with a
 * {@code frozen-mismatch}, and every locked artifact is activated only after its SHA-256 matches
 * the recorded one. This is the boot gate immutable production images run: same lockfile, same
 * bytes, or a loud per-artifact failure.
 */
final class FrozenResolution {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrozenResolution.class);

    /**
     * Instantiates are not needed.
     */
    private FrozenResolution() {
        // utility
    }

    /**
     * The frozen activation plan.
     *
     * @param activations the checksum-verified locked artifacts and their local-repository paths
     * @param failures the per-coordinate failures (missing file, checksum mismatch, coordinate not in
     *        the lock), keyed by coordinate
     * @param mismatched the declared coordinates the lock does not carry - reported as
     *        {@code frozen-mismatch}
     */
    record FrozenPlan(List<LockedActivation> activations, Map<String, String> failures, Set<String> mismatched) {

        /**
         * The activations of one scope.
         *
         * @param scope the scope name (module or platform)
         * @return the jar paths in lock order
         */
        List<Path> artifacts(String scope) {
            return activations.stream()
                              .filter(activation -> scope.equals(activation.artifact()
                                                                           .scope()))
                              .map(LockedActivation::path)
                              .toList();
        }
    }

    /**
     * One verified locked artifact.
     *
     * @param artifact the lock entry
     * @param path the jar path inside the local repository
     */
    record LockedActivation(Lockfile.LockedArtifact artifact, Path path) {
    }

    /**
     * Plans the frozen activation - verifies every locked artifact against the local repository and
     * rejects every declaration the lock does not carry.
     *
     * @param lockfile the lockfile
     * @param declared the declared dependencies
     * @param localRepository the local repository the locked artifacts must already live in
     * @return the plan
     */
    static FrozenPlan plan(Lockfile lockfile, DeclaredDependencies declared, Path localRepository) {
        Map<String, String> failures = new LinkedHashMap<>();
        Set<String> mismatched = new LinkedHashSet<>();

        Set<String> lockedIds = new LinkedHashSet<>();
        lockfile.artifacts()
                .forEach(artifact -> lockedIds.add(artifact.id()));
        for (MavenDependency dependency : declared.dependencies()) {
            if (!lockedIds.contains(dependency.coordinate())) {
                String message = "Declared as [" + dependency.coordinate() + "] but not part of [project-lock.json] - frozen mode"
                        + " (DIRIGIBLE_DEPENDENCIES_FROZEN=true) activates the locked set only. Unfreeze the instance or rebuild the"
                        + " lock with a dynamic resolution to add or change dependencies.";
                mismatched.add(dependency.coordinate());
                failures.put(dependency.coordinate(), message);
                LOGGER.error("Frozen-mode mismatch: {}", message);
            }
        }

        List<LockedActivation> activations = new ArrayList<>();
        for (Lockfile.LockedArtifact artifact : lockfile.artifacts()) {
            Path jar = artifactPath(localRepository, artifact.id());
            if (jar == null) {
                failures.put(artifact.id(), "The locked coordinate [" + artifact.id() + "] is not a valid groupId:artifactId:version");
                continue;
            }
            if (!Files.isRegularFile(jar)) {
                failures.put(artifact.id(),
                        "The locked artifact [" + artifact.id() + "] is missing from the local repository [" + jar + "] - not activated");
                LOGGER.error("Frozen-mode activation failed: the locked artifact [{}] is missing from [{}]", artifact.id(), jar);
                continue;
            }
            try {
                String actual = LockfileStore.sha256(jar);
                if (!actual.equals(artifact.sha256())) {
                    failures.put(artifact.id(), "Checksum mismatch for the locked artifact [" + artifact.id() + "]: expected ["
                            + artifact.sha256() + "], found [" + actual + "] - not activated");
                    LOGGER.error("Frozen-mode activation failed: checksum mismatch for [{}] at [{}]", artifact.id(), jar);
                    continue;
                }
            } catch (IOException e) {
                failures.put(artifact.id(), "The locked artifact [" + artifact.id() + "] is unreadable: " + e.getMessage());
                LOGGER.error("Frozen-mode activation failed: the locked artifact [{}] at [{}] is unreadable", artifact.id(), jar, e);
                continue;
            }
            activations.add(new LockedActivation(artifact, jar));
        }
        return new FrozenPlan(activations, failures, mismatched);
    }

    /**
     * The local-repository jar path of a locked coordinate.
     *
     * @param localRepository the local repository
     * @param coordinate the groupId:artifactId:version coordinate
     * @return the path, null when the coordinate is malformed
     */
    private static Path artifactPath(Path localRepository, String coordinate) {
        String[] parts = coordinate.split(":", -1);
        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            return null;
        }
        return localRepository.resolve(parts[0].replace('.', '/'))
                              .resolve(parts[1])
                              .resolve(parts[2])
                              .resolve(parts[1] + "-" + parts[2] + ".jar");
    }

}
