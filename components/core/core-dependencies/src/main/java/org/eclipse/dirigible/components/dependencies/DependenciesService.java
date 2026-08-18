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
import org.eclipse.dirigible.components.dependencies.DependencySynchronizer.SwapOutcome;
import org.eclipse.dirigible.engine.java.runtime.ModulesClassLoaderHolder;
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
 * registry projects, resolves their union into the local repository and reconciles the resolved
 * JARs into the running system through the {@link DependencySynchronizer} - a dependency change
 * takes effect without restarting the platform. The resolved-modules directory is still maintained
 * as the launch-time seed.
 */
@Component
class DependenciesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DependenciesService.class);

    /** The failures key carrying a swap abort. */
    private static final String SWAP_FAILURE_KEY = "modules-swap";

    /** The collector. */
    private final ProjectDependenciesCollector collector;

    /** The resolver. */
    private final DependencyResolver resolver;

    /** The linker. */
    private final ResolvedModulesLinker linker;

    /** The dependency synchronizer. */
    private final DependencySynchronizer dependencySynchronizer;

    /** The loader holder. */
    private final ModulesClassLoaderHolder loaderHolder;

    /** The last resolved state, null before the first resolution. */
    private final AtomicReference<DependenciesState> lastState = new AtomicReference<>();

    /** The fingerprint of the declarations the last resolution processed, null before it. */
    private volatile String lastDeclaredFingerprint;

    /**
     * Instantiates a new dependencies service.
     *
     * @param collector the collector
     * @param resolver the resolver
     * @param linker the linker
     * @param dependencySynchronizer the dependency synchronizer
     * @param loaderHolder the loader holder
     */
    DependenciesService(ProjectDependenciesCollector collector, DependencyResolver resolver, ResolvedModulesLinker linker,
            DependencySynchronizer dependencySynchronizer, ModulesClassLoaderHolder loaderHolder) {
        this.collector = collector;
        this.resolver = resolver;
        this.linker = linker;
        this.dependencySynchronizer = dependencySynchronizer;
        this.loaderHolder = loaderHolder;
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
     * The fingerprint of the declarations the last resolution processed - the watcher compares the
     * registry against it.
     *
     * @return the fingerprint, null before the first resolution
     */
    String lastDeclaredFingerprint() {
        return lastDeclaredFingerprint;
    }

    /**
     * Runs the union resolution and reconciles the result into the running system. On any failure -
     * declaration, resolution or swap validation - the installed modules-classloader generation keeps
     * serving and the failure is reported in the returned state.
     *
     * @return the resolved state
     */
    synchronized DependenciesState resolveAndActivate() {
        DeclaredDependencies declared = collector.collect();
        lastDeclaredFingerprint = declared.fingerprint();
        ResolutionResult result = resolver.resolve(declared.dependencies());
        Map<String, String> failures = new LinkedHashMap<>(declared.errors());
        failures.putAll(result.failures());
        Path localRepository = MavenResolverConfig.fromConfiguration()
                                                  .localRepository();

        SwapOutcome outcome;
        if (failures.isEmpty()) {
            outcome = dependencySynchronizer.swap(localRepository, result.artifacts(), result.mediated());
        } else {
            outcome = SwapOutcome.kept(null);
            LOGGER.error("Not swapping the dependency layer: [{}] declaration/resolution failure(s) - the installed generation keeps "
                    + "serving. Failures: {}", failures.size(), failures);
        }
        if (outcome.error() != null) {
            failures.put(SWAP_FAILURE_KEY, outcome.error());
        }
        // the resolved-modules directory stays maintained as the seed of the next launch's
        // classpath; stale links are removed only after a fully clean pass
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
                loaderHolder.generation(), loaderHolder.retiredGenerationsLive(), Instant.now());
        lastState.set(state);
        LOGGER.info(
                "Maven dependency resolution completed: [{}] declared, [{}] jar(s) {}, [{}] mediated, [{}] failure(s), "
                        + "classloader generation [{}]",
                state.declared()
                     .size(),
                state.artifacts()
                     .size(),
                outcome.swapped() ? "active" : "resolved (generation kept)", state.mediated()
                                                                                  .size(),
                state.failures()
                     .size(),
                state.classLoaderGeneration());
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
        return state.refreshed(isDynamicEnabled(), loaderHolder.generation(), loaderHolder.retiredGenerationsLive());
    }

}
