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
 * @param declared the declared coordinates
 * @param artifacts the resolved jar paths inside the local repository
 * @param mediated the versions chosen where more than one was requested, keyed by
 *        groupId:artifactId
 * @param failures the per-coordinate failure messages
 * @param localRepository the local repository, null before the first resolution
 * @param resolvedModulesDirectory the directory the resolved jars are linked into (the launch-time
 *        seed; at runtime the jars are served by the modules classloader directly)
 * @param classLoaderGeneration the installed modules-classloader generation number
 * @param retiredClassLoaders how many retired generations are still pinned by live references
 * @param resolvedAt when the state was resolved, null before the first resolution
 */
record DependenciesState(boolean enabled, List<String> declared, List<String> artifacts, Map<String, String> mediated,
        Map<String, String> failures, String localRepository, String resolvedModulesDirectory, int classLoaderGeneration,
        int retiredClassLoaders, Instant resolvedAt) {

    /**
     * The state before the first resolution.
     *
     * @param enabled whether dynamic dependency resolution is enabled
     * @param resolvedModulesDirectory the resolved-modules directory
     * @return the empty state
     */
    static DependenciesState empty(boolean enabled, String resolvedModulesDirectory) {
        return new DependenciesState(enabled, List.of(), List.of(), Map.of(), Map.of(), null, resolvedModulesDirectory, 0, 0, null);
    }

    /**
     * The same state with the enabled flag and the classloader counters re-read.
     *
     * @param enabled the current flag value
     * @param classLoaderGeneration the current generation number
     * @param retiredClassLoaders the current pinned retired-generation count
     * @return the state
     */
    DependenciesState refreshed(boolean enabled, int classLoaderGeneration, int retiredClassLoaders) {
        return new DependenciesState(enabled, declared, artifacts, mediated, failures, localRepository, resolvedModulesDirectory,
                classLoaderGeneration, retiredClassLoaders, resolvedAt);
    }

}
