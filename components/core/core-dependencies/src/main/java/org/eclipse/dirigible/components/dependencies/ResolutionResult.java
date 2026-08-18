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

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The outcome of a {@link DependencyResolver#resolve(java.util.Set)} call.
 *
 * @param resolved the resolved artifacts - immutable, versioned local-repository locations that are
 *        never copied out or overwritten in place, each attributed to the declared root that pulled
 *        it in
 * @param mediated the versions chosen where the dependency graph requested more than one, keyed by
 *        {@code groupId:artifactId}
 * @param requestedVersions every requested version of the mediated {@code groupId:artifactId}
 *        entries
 * @param failures the per-coordinate failure messages (unresolvable, bad checksum, unsupported
 *        scope, ...), keyed by the failing coordinate
 * @param provided the declared coordinates the platform provides at exactly the declared version -
 *        satisfied without a download
 * @param shadowed the declared {@code groupId:artifactId} entries the platform provides at a
 *        different version - the declared version is inert (parent-first delegation serves the
 *        platform's), reported loudly instead of silently
 */
public record ResolutionResult(List<ResolvedArtifact> resolved, Map<String, String> mediated, Map<String, Set<String>> requestedVersions,
        Map<String, String> failures, List<String> provided, List<Shadowed> shadowed) {

    /**
     * One resolved artifact.
     *
     * @param coordinate the groupId:artifactId:version coordinate
     * @param path the jar path inside the local repository
     * @param via the declared root that pulled the artifact in transitively, null for a declared root
     *        itself
     */
    public record ResolvedArtifact(String coordinate, Path path, String via) {
    }

    /**
     * One shadowed declaration.
     *
     * @param groupArtifact the groupId:artifactId
     * @param requested the declared version
     * @param providedVersion the version the platform provides
     */
    public record Shadowed(String groupArtifact, String requested, String providedVersion) {
    }

    /**
     * Copies the collections defensively, keeping their order.
     *
     * @param resolved the resolved artifacts
     * @param mediated the mediated versions
     * @param requestedVersions the requested versions of the mediated entries
     * @param failures the per-coordinate failures
     * @param provided the platform-provided declared coordinates
     * @param shadowed the shadowed declarations
     */
    public ResolutionResult {
        resolved = List.copyOf(resolved);
        mediated = Collections.unmodifiableMap(new LinkedHashMap<>(mediated));
        requestedVersions = Collections.unmodifiableMap(new LinkedHashMap<>(requestedVersions));
        failures = Collections.unmodifiableMap(new LinkedHashMap<>(failures));
        provided = List.copyOf(provided);
        shadowed = List.copyOf(shadowed);
    }

    /**
     * The result of resolving nothing.
     *
     * @return the empty result
     */
    public static ResolutionResult empty() {
        return new ResolutionResult(List.of(), Map.of(), Map.of(), Map.of(), List.of(), List.of());
    }

    /**
     * The resolved jar paths in graph order.
     *
     * @return the paths
     */
    public List<Path> artifacts() {
        return resolved.stream()
                       .map(ResolvedArtifact::path)
                       .toList();
    }

}
