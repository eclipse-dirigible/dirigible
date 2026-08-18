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

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The dependency lockfile ({@code project-lock.json}) - the reproducibility record of one union
 * resolution. Written after every fully clean resolution, verified before every activation, and in
 * frozen mode ({@code DIRIGIBLE_DEPENDENCIES_FROZEN=true}) it IS the resolution: the activated set
 * comes from here alone, checksum-verified, without consulting any remote repository.
 *
 * <p>
 * The lock is instance-level, not per-project: the union resolution mediates versions across all
 * registry projects, so only one file can faithfully record the outcome. Reviewability comes from
 * the per-artifact attribution instead - a root artifact carries the projects that requested it
 * ({@code requestedBy}), a transitive one the declared root that pulled it in ({@code via}) - and
 * from the deterministic, sorted serialization, which keeps the diff between two locks reviewable.
 *
 * @param resolvedAt when the recorded resolution completed, ISO-8601
 * @param platform the platform version the resolution ran on
 * @param artifacts the activated artifacts in sorted order
 * @param mediated the version mediations of the recorded resolution, in sorted order
 */
record Lockfile(String resolvedAt, String platform, List<LockedArtifact> artifacts, List<LockedMediation> mediated) {

    /**
     * Copies the collections defensively.
     *
     * @param resolvedAt when the recorded resolution completed
     * @param platform the platform version
     * @param artifacts the activated artifacts
     * @param mediated the version mediations
     */
    Lockfile {
        artifacts = artifacts == null ? List.of() : List.copyOf(artifacts);
        mediated = mediated == null ? List.of() : List.copyOf(mediated);
    }

    /**
     * One activated artifact.
     *
     * @param id the groupId:artifactId:version coordinate
     * @param sha256 the SHA-256 of the jar, hex
     * @param requestedBy the projects declaring the artifact, null for a transitive one
     * @param via the declared root that pulled the artifact in, null for a declared root
     * @param scope module or platform
     */
    record LockedArtifact(String id, String sha256, List<String> requestedBy, String via, String scope) {
    }

    /**
     * One version mediation.
     *
     * @param id the groupId:artifactId
     * @param chosen the version mediation chose
     * @param rejected the requested versions mediation rejected
     * @param requestedBy the projects directly declaring each version, keyed by version; empty for
     *        purely transitive requests
     */
    record LockedMediation(String id, String chosen, List<String> rejected, Map<String, List<String>> requestedBy) {
    }

    /**
     * The locked artifact with the given coordinate.
     *
     * @param coordinate the groupId:artifactId:version coordinate
     * @return the artifact, empty when the lock does not carry it
     */
    Optional<LockedArtifact> artifact(String coordinate) {
        return artifacts.stream()
                        .filter(artifact -> artifact.id()
                                                    .equals(coordinate))
                        .findFirst();
    }

}
