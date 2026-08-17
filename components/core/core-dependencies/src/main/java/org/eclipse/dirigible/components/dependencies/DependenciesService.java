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

import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Orchestrates the declare - resolve - activate flow: collects the maven declarations of all
 * registry projects, resolves their union into the local repository and links the resolved jars
 * into the resolved-modules directory. The contract of this phase: declare, resolve (at boot or on
 * demand), restart to activate.
 */
@Component
class DependenciesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DependenciesService.class);

    /** The collector. */
    private final ProjectDependenciesCollector collector;

    /** The resolver. */
    private final DependencyResolver resolver;

    /** The linker. */
    private final ResolvedModulesLinker linker;

    /** The last resolved state, null before the first resolution. */
    private final AtomicReference<DependenciesState> lastState = new AtomicReference<>();

    /**
     * Instantiates a new dependencies service.
     *
     * @param collector the collector
     * @param resolver the resolver
     * @param linker the linker
     */
    DependenciesService(ProjectDependenciesCollector collector, DependencyResolver resolver, ResolvedModulesLinker linker) {
        this.collector = collector;
        this.resolver = resolver;
        this.linker = linker;
    }

    /**
     * Whether dynamic dependency resolution is enabled on this instance.
     *
     * @return true when enabled
     */
    boolean isDynamicEnabled() {
        return DirigibleConfig.DEPENDENCIES_DYNAMIC_ENABLED.getBooleanValue();
    }

    /**
     * Runs the union resolution and links the resolved jars into the resolved-modules directory.
     *
     * @return the resolved state
     */
    synchronized DependenciesState resolveAndActivate() {
        DeclaredDependencies declared = collector.collect();
        ResolutionResult result = resolver.resolve(declared.dependencies());
        Map<String, String> failures = new LinkedHashMap<>(declared.errors());
        failures.putAll(result.failures());
        Path localRepository = MavenResolverConfig.fromConfiguration()
                                                  .localRepository();
        linker.sync(localRepository, result.artifacts(), failures.isEmpty());
        DependenciesState state = new DependenciesState(isDynamicEnabled(), declared.dependencies()
                                                                                    .stream()
                                                                                    .map(MavenDependency::coordinate)
                                                                                    .toList(),
                result.artifacts()
                      .stream()
                      .map(Path::toString)
                      .toList(),
                result.mediated(), failures, localRepository.toString(), linker.directory()
                                                                               .toString(),
                Instant.now());
        lastState.set(state);
        LOGGER.info(
                "Maven dependency resolution completed: [{}] declared, [{}] jar(s) activated for the next restart, [{}] mediated, [{}] failure(s)",
                state.declared()
                     .size(),
                state.artifacts()
                     .size(),
                state.mediated()
                     .size(),
                state.failures()
                     .size());
        return state;
    }

    /**
     * The current declared / resolved state.
     *
     * @return the state
     */
    DependenciesState getState() {
        DependenciesState state = lastState.get();
        if (state == null) {
            return DependenciesState.empty(isDynamicEnabled(), linker.directory()
                                                                     .toString());
        }
        return state.withEnabled(isDynamicEnabled());
    }

}
