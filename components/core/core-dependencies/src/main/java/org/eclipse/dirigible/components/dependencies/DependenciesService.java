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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    /** The platform scope installer. */
    private final PlatformScopeInstaller platformScopeInstaller;

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
     * @param platformScopeInstaller the platform scope installer
     * @param loaderHolder the loader holder
     */
    DependenciesService(ProjectDependenciesCollector collector, DependencyResolver resolver, ResolvedModulesLinker linker,
            DependencySynchronizer dependencySynchronizer, PlatformScopeInstaller platformScopeInstaller,
            ModulesClassLoaderHolder loaderHolder) {
        this.collector = collector;
        this.resolver = resolver;
        this.linker = linker;
        this.dependencySynchronizer = dependencySynchronizer;
        this.platformScopeInstaller = platformScopeInstaller;
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
     * Runs the resolution of both dependency tiers and reconciles the results into the running system -
     * the platform tier (appended to the system classloader) first, then the module tier (the swappable
     * modules classloader). A failure on one tier never aborts the other; on any module-tier failure
     * the installed modules-classloader generation keeps serving.
     *
     * @return the resolved state
     */
    synchronized DependenciesState resolveAndActivate() {
        DeclaredDependencies declared = collector.collect();
        lastDeclaredFingerprint = declared.fingerprint();
        Set<MavenDependency> platformDeclared = scoped(declared, MavenDependency.Scope.PLATFORM);
        Set<MavenDependency> moduleDeclared = scoped(declared, MavenDependency.Scope.MODULE);
        Path localRepository = MavenResolverConfig.fromConfiguration()
                                                  .localRepository();

        // platform tier - append-only system-classloader additions; its failures never gate the
        // module swap
        ResolutionResult platformResult =
                platformDeclared.isEmpty() ? new ResolutionResult(List.of(), Map.of(), Map.of()) : resolver.resolve(platformDeclared);
        List<PlatformScopeInstaller.PlatformArtifactState> platformStates =
                platformDeclared.isEmpty() ? List.of() : platformScopeInstaller.install(localRepository, platformResult.artifacts());

        // module tier - gated on the declaration errors and its own resolution failures only
        ResolutionResult moduleResult = resolver.resolve(moduleDeclared);
        Map<String, String> moduleGate = new LinkedHashMap<>(declared.errors());
        moduleGate.putAll(moduleResult.failures());
        SwapOutcome outcome;
        if (moduleGate.isEmpty()) {
            outcome = dependencySynchronizer.swap(localRepository, moduleResult.artifacts(), platformResult.artifacts(),
                    moduleResult.mediated());
        } else {
            outcome = SwapOutcome.kept(null);
            LOGGER.error("Not swapping the dependency layer: [{}] declaration/resolution failure(s) - the installed generation keeps "
                    + "serving. Failures: {}", moduleGate.size(), moduleGate);
        }

        Map<String, String> failures = new LinkedHashMap<>(declared.errors());
        failures.putAll(platformResult.failures());
        failures.putAll(moduleResult.failures());
        if (outcome.error() != null) {
            failures.put(SWAP_FAILURE_KEY, outcome.error());
        }

        // both tiers seed the next launch's classpath through the resolved-modules directory;
        // stale links are removed only after a fully clean pass
        List<Path> allArtifacts = new ArrayList<>(moduleResult.artifacts());
        for (Path artifact : platformResult.artifacts()) {
            if (!allArtifacts.contains(artifact)) {
                allArtifacts.add(artifact);
            }
        }
        linker.sync(localRepository, allArtifacts, failures.isEmpty());

        Map<String, String> mediated = new LinkedHashMap<>(moduleResult.mediated());
        mediated.putAll(platformResult.mediated());

        DependenciesState state = new DependenciesState(isDynamicEnabled(), declared.dependencies()
                                                                                    .stream()
                                                                                    .map(MavenDependency::coordinate)
                                                                                    .toList(),
                allArtifacts.stream()
                            .map(Path::toString)
                            .toList(),
                mediated, failures, platformStates, localRepository.toString(), linker.directory()
                                                                                      .toString(),
                loaderHolder.generation(), loaderHolder.retiredGenerationsLive(), Instant.now());
        lastState.set(state);
        LOGGER.info(
                "Maven dependency resolution completed: [{}] declared, [{}] jar(s) {}, [{}] platform-scoped, [{}] mediated, "
                        + "[{}] failure(s), classloader generation [{}]",
                state.declared()
                     .size(),
                state.artifacts()
                     .size(),
                outcome.swapped() ? "active" : "resolved (generation kept)", platformStates.size(), state.mediated()
                                                                                                         .size(),
                state.failures()
                     .size(),
                state.classLoaderGeneration());
        return state;
    }

    /**
     * The declared dependencies of one scope.
     *
     * @param declared the declared dependencies
     * @param scope the scope
     * @return the dependencies of that scope, in declaration order
     */
    private static Set<MavenDependency> scoped(DeclaredDependencies declared, MavenDependency.Scope scope) {
        Set<MavenDependency> result = new LinkedHashSet<>();
        for (MavenDependency dependency : declared.dependencies()) {
            if (dependency.scope() == scope) {
                result.add(dependency);
            }
        }
        return result;
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
