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

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * The declared / resolved dependency state the endpoint reports.
 *
 * @param enabled whether dynamic dependency resolution is enabled on this instance
 * @param frozen whether the instance runs in frozen mode (the lockfile is the resolution)
 * @param declared the declared coordinates
 * @param declaredBy the declaring projects per declared coordinate
 * @param artifacts the resolved jar paths inside the local repository
 * @param mediated the versions chosen where more than one was requested, keyed by
 *        groupId:artifactId
 * @param failures the per-coordinate failure messages
 * @param platform the per-artifact activation states of the platform-scoped dependencies
 * @param report the per-artifact status report - every artifact carries one of active,
 *        pending-restart, shadowed, mediated, failed or frozen-mismatch
 * @param localRepository the local repository, null before the first resolution
 * @param resolvedModulesDirectory the directory the resolved jars are linked into (the launch-time
 *        seed; at runtime the jars are served by the modules classloader directly)
 * @param lockfile the lockfile path
 * @param classLoaderGeneration the installed modules-classloader generation number
 * @param retiredClassLoaders how many retired generations are still pinned by live references
 * @param resolvedAt when the state was resolved, null before the first resolution
 */
record DependenciesState(boolean enabled, boolean frozen, List<String> declared, Map<String, List<String>> declaredBy,
        List<String> artifacts, Map<String, String> mediated, Map<String, String> failures,
        List<PlatformScopeInstaller.PlatformArtifactState> platform, List<ArtifactStatus> report, String localRepository,
        String resolvedModulesDirectory, String lockfile, int classLoaderGeneration, int retiredClassLoaders, Instant resolvedAt) {

    /**
     * The state before the first resolution.
     *
     * @param enabled whether dynamic dependency resolution is enabled
     * @param frozen whether the instance runs in frozen mode
     * @param resolvedModulesDirectory the resolved-modules directory
     * @param lockfile the lockfile path
     * @return the empty state
     */
    static DependenciesState empty(boolean enabled, boolean frozen, String resolvedModulesDirectory, String lockfile) {
        return new DependenciesState(enabled, frozen, List.of(), Map.of(), List.of(), Map.of(), Map.of(), List.of(), List.of(), null,
                resolvedModulesDirectory, lockfile, 0, 0, null);
    }

    /**
     * The same state with the mode flags and the classloader counters re-read.
     *
     * @param enabled the current enabled flag value
     * @param frozen the current frozen flag value
     * @param classLoaderGeneration the current generation number
     * @param retiredClassLoaders the current pinned retired-generation count
     * @return the state
     */
    DependenciesState refreshed(boolean enabled, boolean frozen, int classLoaderGeneration, int retiredClassLoaders) {
        return new DependenciesState(enabled, frozen, declared, declaredBy, artifacts, mediated, failures, platform, report,
                localRepository, resolvedModulesDirectory, lockfile, classLoaderGeneration, retiredClassLoaders, resolvedAt);
    }

}
