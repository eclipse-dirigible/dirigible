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

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.dependencies.DependencySynchronizer.SwapOutcome;
import org.eclipse.dirigible.components.dependencies.FrozenResolution.FrozenPlan;
import org.eclipse.dirigible.components.dependencies.ResolutionResult.ResolvedArtifact;
import org.eclipse.dirigible.engine.java.runtime.ModulesClassLoaderHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Orchestrates the declare - resolve - verify - activate flow: collects the maven declarations of
 * all registry projects, resolves their union into the local repository, verifies every artifact
 * against the lockfile's recorded SHA-256 before anything activates, and reconciles the verified
 * JARs into the running system through the {@link DependencySynchronizer} - a dependency change
 * takes effect without restarting the platform. Every fully clean resolution rewrites the lockfile;
 * in frozen mode ({@code DIRIGIBLE_DEPENDENCIES_FROZEN=true}) the lockfile IS the resolution and no
 * remote repository is ever consulted. The resolved-modules directory is still maintained as the
 * launch-time seed.
 */
@Component
class DependenciesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DependenciesService.class);

    /** The failures key carrying a swap abort. */
    private static final String SWAP_FAILURE_KEY = "modules-swap";

    /** The failures key carrying a frozen boot without a lockfile. */
    private static final String LOCKFILE_FAILURE_KEY = "project-lock.json";

    /** The collector. */
    private final ProjectDependenciesCollector collector;

    /** The resolver. */
    private final DependencyResolver resolver;

    /** The linker. */
    private final ResolvedModulesLinker linker;

    /** The lockfile store. */
    private final LockfileStore lockfileStore;

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
     * @param lockfileStore the lockfile store
     * @param dependencySynchronizer the dependency synchronizer
     * @param platformScopeInstaller the platform scope installer
     * @param loaderHolder the loader holder
     */
    DependenciesService(ProjectDependenciesCollector collector, DependencyResolver resolver, ResolvedModulesLinker linker,
            LockfileStore lockfileStore, DependencySynchronizer dependencySynchronizer, PlatformScopeInstaller platformScopeInstaller,
            ModulesClassLoaderHolder loaderHolder) {
        this.collector = collector;
        this.resolver = resolver;
        this.linker = linker;
        this.lockfileStore = lockfileStore;
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
     * Whether the instance runs in frozen mode.
     *
     * @return true when frozen
     */
    boolean isFrozen() {
        return DirigibleConfig.DEPENDENCIES_FROZEN.getBooleanValue();
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
     * Runs the resolution of both dependency tiers and reconciles the results into the running system.
     * In frozen mode the activated set comes from the lockfile alone; otherwise the union is resolved,
     * verified against the lockfile and activated - the platform tier (appended to the system
     * classloader) first, then the module tier (the swappable modules classloader). A failure on one
     * tier never aborts the other; on any module-tier failure the installed modules-classloader
     * generation keeps serving.
     *
     * @return the resolved state
     */
    synchronized DependenciesState resolveAndActivate() {
        DeclaredDependencies declared = collector.collect();
        lastDeclaredFingerprint = declared.fingerprint();
        Path localRepository = MavenResolverConfig.fromConfiguration()
                                                  .localRepository();
        DependenciesState state = isFrozen() ? activateFrozen(declared, localRepository) : resolveDynamic(declared, localRepository);
        lastState.set(state);
        LOGGER.info(
                "Maven dependency {} completed: [{}] declared, [{}] jar(s) activated, [{}] platform-scoped, [{}] mediated,"
                        + " [{}] failure(s), classloader generation [{}]",
                state.frozen() ? "frozen activation" : "resolution", state.declared()
                                                                          .size(),
                state.artifacts()
                     .size(),
                state.platform()
                     .size(),
                state.mediated()
                     .size(),
                state.failures()
                     .size(),
                state.classLoaderGeneration());
        return state;
    }

    /**
     * The dynamic flow: resolve both tiers, verify every resolved artifact against the lockfile's
     * recorded checksum, activate the verified set and - only after a fully clean pass - rewrite the
     * lockfile.
     *
     * @param declared the declared dependencies
     * @param localRepository the local repository
     * @return the state
     */
    private DependenciesState resolveDynamic(DeclaredDependencies declared, Path localRepository) {
        Set<MavenDependency> platformDeclared = scoped(declared, MavenDependency.Scope.PLATFORM);
        Set<MavenDependency> moduleDeclared = scoped(declared, MavenDependency.Scope.MODULE);

        // platform tier - append-only system-classloader additions; its failures never gate the
        // module swap
        ResolutionResult platformResult = platformDeclared.isEmpty() ? ResolutionResult.empty() : resolver.resolve(platformDeclared);
        ResolutionResult moduleResult = resolver.resolve(moduleDeclared);

        // integrity: every resolved artifact is hashed, and one the lockfile already records must
        // match the recorded SHA-256 - a mismatch is a hard per-artifact failure, the artifact is
        // not activated and its seed link is evicted so no later launch picks it up either
        Optional<Lockfile> lockfile = lockfileStore.read();
        Map<String, String> hashes = new LinkedHashMap<>();
        Map<String, String> integrityFailures = new LinkedHashMap<>();
        List<ResolvedArtifact> verifiedModule = verify(moduleResult.resolved(), lockfile, hashes, integrityFailures);
        List<ResolvedArtifact> verifiedPlatform = verify(platformResult.resolved(), lockfile, hashes, integrityFailures);
        moduleResult.resolved()
                    .stream()
                    .filter(artifact -> integrityFailures.containsKey(artifact.coordinate()))
                    .forEach(artifact -> linker.remove(localRepository, artifact.path()));
        platformResult.resolved()
                      .stream()
                      .filter(artifact -> integrityFailures.containsKey(artifact.coordinate()))
                      .forEach(artifact -> linker.remove(localRepository, artifact.path()));

        List<PlatformScopeInstaller.PlatformArtifactState> platformStates =
                verifiedPlatform.isEmpty() ? List.of() : platformScopeInstaller.install(localRepository, paths(verifiedPlatform));

        // module tier - gated on the declaration errors and its own resolution failures only; an
        // integrity failure is per-artifact by design (the verified rest still activates)
        Map<String, String> moduleGate = new LinkedHashMap<>(declared.errors());
        moduleGate.putAll(moduleResult.failures());
        SwapOutcome outcome;
        if (moduleGate.isEmpty()) {
            outcome = dependencySynchronizer.swap(localRepository, paths(verifiedModule), paths(verifiedPlatform), moduleResult.mediated());
        } else {
            outcome = SwapOutcome.kept(null);
            LOGGER.error("Not swapping the dependency layer: [{}] declaration/resolution failure(s) - the installed generation keeps "
                    + "serving. Failures: {}", moduleGate.size(), moduleGate);
        }

        Map<String, String> failures = new LinkedHashMap<>(declared.errors());
        failures.putAll(platformResult.failures());
        failures.putAll(moduleResult.failures());
        failures.putAll(integrityFailures);
        if (outcome.error() != null) {
            failures.put(SWAP_FAILURE_KEY, outcome.error());
        }

        // both tiers seed the next launch's classpath through the resolved-modules directory;
        // stale links are removed only after a fully clean pass
        List<ResolvedArtifact> allArtifacts = union(verifiedModule, verifiedPlatform);
        linker.sync(localRepository, paths(allArtifacts), failures.isEmpty());

        // the lockfile records only fully clean resolutions - a partial pass must never launder a
        // failed or tampered artifact into the trusted set
        if (failures.isEmpty()) {
            lockfileStore.write(buildLockfile(declared, moduleResult, platformResult, verifiedModule, verifiedPlatform, hashes));
        }

        Map<String, String> mediated = new LinkedHashMap<>(moduleResult.mediated());
        mediated.putAll(platformResult.mediated());

        List<ArtifactStatus> report = new ArrayList<>();
        reportDeclarationErrors(declared, report);
        reportResolution(moduleResult, verifiedModule, integrityFailures, outcome.swapped(), ArtifactStatus.SCOPE_MODULE, report);
        reportResolution(platformResult, List.of(), integrityFailures, outcome.swapped(), ArtifactStatus.SCOPE_PLATFORM, report);
        platformStates.forEach(state -> report.add(
                new ArtifactStatus(state.coordinate(), ArtifactStatus.SCOPE_PLATFORM, state.status(), state.message())));

        return state(false, declared, paths(allArtifacts), mediated, failures, platformStates, report, localRepository);
    }

    /**
     * The frozen flow: the lockfile is the resolution - every locked artifact is checksum-verified from
     * the local repository and activated, every declaration the lock does not carry is rejected, and no
     * remote repository is ever consulted.
     *
     * @param declared the declared dependencies
     * @param localRepository the local repository
     * @return the state
     */
    private DependenciesState activateFrozen(DeclaredDependencies declared, Path localRepository) {
        Optional<Lockfile> lockfile = lockfileStore.read();
        if (lockfile.isEmpty()) {
            String message = "DIRIGIBLE_DEPENDENCIES_FROZEN=true but there is no lockfile at [" + lockfileStore.path()
                    + "] - nothing is activated in frozen mode without a lock. Run one dynamic resolution to produce it.";
            LOGGER.error("Frozen-mode activation failed: {}", message);
            List<ArtifactStatus> report = new ArrayList<>();
            declared.dependencies()
                    .forEach(dependency -> report.add(new ArtifactStatus(dependency.coordinate(), scopeName(dependency),
                            ArtifactStatus.STATUS_FROZEN_MISMATCH, message)));
            return state(true, declared, List.of(), Map.of(), Map.of(LOCKFILE_FAILURE_KEY, message), List.of(), report, localRepository);
        }

        FrozenPlan plan = FrozenResolution.plan(lockfile.get(), declared, localRepository, ProvidedBom.fromClasspath());
        List<Path> platformArtifacts = plan.artifacts(ArtifactStatus.SCOPE_PLATFORM);
        List<PlatformScopeInstaller.PlatformArtifactState> platformStates =
                platformArtifacts.isEmpty() ? List.of() : platformScopeInstaller.install(localRepository, platformArtifacts);

        Map<String, String> moduleGate = new LinkedHashMap<>(declared.errors());
        SwapOutcome outcome;
        if (moduleGate.isEmpty()) {
            outcome =
                    dependencySynchronizer.swap(localRepository, plan.artifacts(ArtifactStatus.SCOPE_MODULE), platformArtifacts, Map.of());
        } else {
            outcome = SwapOutcome.kept(null);
            LOGGER.error("Not swapping the dependency layer: [{}] declaration failure(s) - the installed generation keeps serving."
                    + " Failures: {}", moduleGate.size(), moduleGate);
        }

        Map<String, String> failures = new LinkedHashMap<>(declared.errors());
        failures.putAll(plan.failures());
        if (outcome.error() != null) {
            failures.put(SWAP_FAILURE_KEY, outcome.error());
        }

        List<Path> allArtifacts = new ArrayList<>(plan.activations()
                                                      .stream()
                                                      .map(FrozenResolution.LockedActivation::path)
                                                      .distinct()
                                                      .toList());
        linker.sync(localRepository, allArtifacts, failures.isEmpty());

        List<ArtifactStatus> report = new ArrayList<>();
        reportDeclarationErrors(declared, report);
        plan.shadowed()
            .forEach(shadowed -> report.add(shadowedStatus(shadowed, null)));
        plan.provided()
            .forEach(coordinate -> report.add(new ArtifactStatus(coordinate, null, ArtifactStatus.STATUS_ACTIVE,
                    "Provided by the platform - satisfied without a download")));
        plan.mismatched()
            .forEach(coordinate -> report.add(new ArtifactStatus(coordinate, null, ArtifactStatus.STATUS_FROZEN_MISMATCH, plan.failures()
                                                                                                                              .get(coordinate))));
        plan.failures()
            .entrySet()
            .stream()
            .filter(failure -> !plan.mismatched()
                                    .contains(failure.getKey()))
            .forEach(failure -> report.add(new ArtifactStatus(failure.getKey(), null, ArtifactStatus.STATUS_FAILED, failure.getValue())));
        for (FrozenResolution.LockedActivation activation : plan.activations()) {
            if (ArtifactStatus.SCOPE_PLATFORM.equals(activation.artifact()
                                                               .scope())) {
                continue; // reported through the platform installer's own state
            }
            report.add(activated(activation.artifact()
                                           .id(),
                    ArtifactStatus.SCOPE_MODULE, activation.path(), outcome.swapped(), "Activated from the lockfile, checksum verified"));
        }
        platformStates.forEach(state -> report.add(
                new ArtifactStatus(state.coordinate(), ArtifactStatus.SCOPE_PLATFORM, state.status(), state.message())));

        return state(true, declared, allArtifacts, Map.of(), failures, platformStates, report, localRepository);
    }

    /**
     * Verifies resolved artifacts against the lockfile - every artifact is hashed (the hash also feeds
     * the next lockfile write), and one the lock records must match the recorded SHA-256.
     *
     * @param resolved the resolved artifacts
     * @param lockfile the lockfile, when present
     * @param hashes the computed hashes per coordinate, added to
     * @param failures the integrity failures per coordinate, added to
     * @return the verified artifacts
     */
    private List<ResolvedArtifact> verify(List<ResolvedArtifact> resolved, Optional<Lockfile> lockfile, Map<String, String> hashes,
            Map<String, String> failures) {
        List<ResolvedArtifact> verified = new ArrayList<>();
        for (ResolvedArtifact artifact : resolved) {
            String hash;
            try {
                hash = LockfileStore.sha256(artifact.path());
            } catch (IOException e) {
                failures.put(artifact.coordinate(),
                        "The resolved artifact [" + artifact.coordinate() + "] is unreadable: " + e.getMessage());
                LOGGER.error("Integrity verification failed: the resolved artifact [{}] at [{}] is unreadable", artifact.coordinate(),
                        artifact.path(), e);
                continue;
            }
            hashes.put(artifact.coordinate(), hash);
            Optional<Lockfile.LockedArtifact> locked = lockfile.flatMap(lock -> lock.artifact(artifact.coordinate()));
            if (locked.isPresent() && !locked.get()
                                             .sha256()
                                             .equals(hash)) {
                failures.put(artifact.coordinate(), "Checksum mismatch for [" + artifact.coordinate() + "]: expected [" + locked.get()
                                                                                                                                .sha256()
                        + "], found [" + hash + "] - the artifact changed since it was locked; not activated");
                LOGGER.error("Integrity verification failed: checksum mismatch for [{}] at [{}] - the artifact is not activated",
                        artifact.coordinate(), artifact.path());
                continue;
            }
            verified.add(artifact);
        }
        return verified;
    }

    /**
     * Builds the lockfile of a fully clean resolution - artifacts and mediations sorted, so two locks
     * diff line by line.
     *
     * @param declared the declared dependencies
     * @param moduleResult the module-tier resolution
     * @param platformResult the platform-tier resolution
     * @param verifiedModule the verified module-tier artifacts
     * @param verifiedPlatform the verified platform-tier artifacts
     * @param hashes the computed hashes per coordinate
     * @return the lockfile
     */
    private Lockfile buildLockfile(DeclaredDependencies declared, ResolutionResult moduleResult, ResolutionResult platformResult,
            List<ResolvedArtifact> verifiedModule, List<ResolvedArtifact> verifiedPlatform, Map<String, String> hashes) {
        List<Lockfile.LockedArtifact> artifacts = new ArrayList<>();
        lockArtifacts(verifiedModule, ArtifactStatus.SCOPE_MODULE, declared, hashes, artifacts);
        lockArtifacts(verifiedPlatform, ArtifactStatus.SCOPE_PLATFORM, declared, hashes, artifacts);
        artifacts.sort(Comparator.comparing(Lockfile.LockedArtifact::id)
                                 .thenComparing(Lockfile.LockedArtifact::scope));

        Map<String, Set<String>> requestedVersions = new TreeMap<>(moduleResult.requestedVersions());
        requestedVersions.putAll(platformResult.requestedVersions());
        Map<String, String> mediated = new LinkedHashMap<>(moduleResult.mediated());
        mediated.putAll(platformResult.mediated());
        List<Lockfile.LockedMediation> mediations = new ArrayList<>();
        requestedVersions.forEach((groupArtifact, versions) -> {
            String chosen = mediated.get(groupArtifact);
            if (chosen == null) {
                return;
            }
            List<String> rejected = versions.stream()
                                            .filter(version -> !version.equals(chosen))
                                            .sorted()
                                            .toList();
            Map<String, List<String>> requestedBy = new TreeMap<>();
            versions.forEach(version -> {
                Set<String> projects = declared.declaredBy()
                                               .get(groupArtifact + ":" + version);
                if (projects != null) {
                    requestedBy.put(version, projects.stream()
                                                     .sorted()
                                                     .toList());
                }
            });
            mediations.add(new Lockfile.LockedMediation(groupArtifact, chosen, rejected, requestedBy));
        });

        String platformVersion = Configuration.get("DIRIGIBLE_PRODUCT_VERSION", "unknown");
        return new Lockfile(Instant.now()
                                   .toString(),
                platformVersion, artifacts, mediations);
    }

    /**
     * Locks one tier's verified artifacts.
     *
     * @param verified the verified artifacts
     * @param scope the tier's scope name
     * @param declared the declared dependencies, for the requestedBy attribution
     * @param hashes the computed hashes per coordinate
     * @param artifacts the lock entries to add to
     */
    private void lockArtifacts(List<ResolvedArtifact> verified, String scope, DeclaredDependencies declared, Map<String, String> hashes,
            List<Lockfile.LockedArtifact> artifacts) {
        for (ResolvedArtifact artifact : verified) {
            List<String> requestedBy = null;
            if (artifact.via() == null) {
                Set<String> projects = declared.declaredBy()
                                               .get(artifact.coordinate());
                requestedBy = projects == null ? List.of()
                        : projects.stream()
                                  .sorted()
                                  .toList();
            }
            artifacts.add(new Lockfile.LockedArtifact(artifact.coordinate(), hashes.get(artifact.coordinate()), requestedBy, artifact.via(),
                    scope));
        }
    }

    /**
     * The status of one shadowed declaration.
     *
     * @param shadowed the shadowed declaration
     * @param scope the scope name, null when unknown
     * @return the status
     */
    private static ArtifactStatus shadowedStatus(ResolutionResult.Shadowed shadowed, String scope) {
        return new ArtifactStatus(shadowed.groupArtifact() + ":" + shadowed.requested(), scope, ArtifactStatus.STATUS_SHADOWED,
                "requested: " + shadowed.requested() + ", provided: " + shadowed.providedVersion()
                        + " - parent-first delegation serves the platform's version; the declared version is inert");
    }

    /**
     * Reports the declaration errors.
     *
     * @param declared the declared dependencies
     * @param report the report to add to
     */
    private void reportDeclarationErrors(DeclaredDependencies declared, List<ArtifactStatus> report) {
        declared.errors()
                .forEach((coordinate, message) -> report.add(new ArtifactStatus(coordinate, null, ArtifactStatus.STATUS_FAILED, message)));
    }

    /**
     * Reports one tier's resolution: shadowed and provided declarations, resolution failures, and - for
     * the module tier - the per-artifact activation outcome.
     *
     * @param result the resolution result
     * @param verified the verified artifacts to report activation for (empty for the platform tier,
     *        whose activation the installer reports itself)
     * @param integrityFailures the integrity failures per coordinate
     * @param swapped whether the swap installed a new generation
     * @param scope the tier's scope name
     * @param report the report to add to
     */
    private void reportResolution(ResolutionResult result, List<ResolvedArtifact> verified, Map<String, String> integrityFailures,
            boolean swapped, String scope, List<ArtifactStatus> report) {
        result.shadowed()
              .forEach(shadowed -> report.add(shadowedStatus(shadowed, scope)));
        result.provided()
              .forEach(coordinate -> report.add(new ArtifactStatus(coordinate, scope, ArtifactStatus.STATUS_ACTIVE,
                      "Provided by the platform - satisfied without a download")));
        result.failures()
              .forEach((coordinate, message) -> report.add(new ArtifactStatus(coordinate, scope, ArtifactStatus.STATUS_FAILED, message)));
        result.resolved()
              .stream()
              .filter(artifact -> integrityFailures.containsKey(artifact.coordinate()))
              .forEach(artifact -> report.add(new ArtifactStatus(artifact.coordinate(), scope, ArtifactStatus.STATUS_FAILED,
                      integrityFailures.get(artifact.coordinate()))));
        for (ResolvedArtifact artifact : verified) {
            String groupArtifact = artifact.coordinate()
                                           .substring(0, artifact.coordinate()
                                                                 .lastIndexOf(':'));
            String origin = artifact.via() == null ? "declared" : "via " + artifact.via();
            if (result.mediated()
                      .containsKey(groupArtifact)) {
                report.add(new ArtifactStatus(artifact.coordinate(), scope, ArtifactStatus.STATUS_MEDIATED,
                        "Version mediation chose [" + result.mediated()
                                                            .get(groupArtifact)
                                + "] out of the requested versions " + result.requestedVersions()
                                                                             .get(groupArtifact)
                                + "; " + origin));
                continue;
            }
            report.add(activated(artifact.coordinate(), scope, artifact.path(), swapped, capitalize(origin)));
        }
    }

    /**
     * The activation status of one module-tier artifact - active when it is part of the currently
     * installed generation, failed otherwise (the swap that would have activated it was aborted).
     *
     * @param coordinate the coordinate
     * @param scope the scope name
     * @param path the jar path
     * @param swapped whether the swap installed a new generation
     * @param activeMessage the message when active
     * @return the status
     */
    private ArtifactStatus activated(String coordinate, String scope, Path path, boolean swapped, String activeMessage) {
        if (swapped || loaderHolder.current()
                                   .jars()
                                   .contains(path)) {
            return new ArtifactStatus(coordinate, scope, ArtifactStatus.STATUS_ACTIVE, activeMessage);
        }
        return new ArtifactStatus(coordinate, scope, ArtifactStatus.STATUS_FAILED,
                "Resolved but not activated - the swap was aborted; see failures");
    }

    /**
     * Assembles the reported state.
     *
     * @param frozen whether the state comes from a frozen activation
     * @param declared the declared dependencies
     * @param artifacts the activated jar paths
     * @param mediated the mediated versions
     * @param failures the failures
     * @param platformStates the platform-tier activation states
     * @param report the per-artifact report
     * @param localRepository the local repository
     * @return the state
     */
    private DependenciesState state(boolean frozen, DeclaredDependencies declared, List<Path> artifacts, Map<String, String> mediated,
            Map<String, String> failures, List<PlatformScopeInstaller.PlatformArtifactState> platformStates, List<ArtifactStatus> report,
            Path localRepository) {
        Map<String, List<String>> declaredBy = new LinkedHashMap<>();
        declared.declaredBy()
                .forEach((coordinate, projects) -> declaredBy.put(coordinate, projects.stream()
                                                                                      .sorted()
                                                                                      .toList()));
        return new DependenciesState(isDynamicEnabled(), frozen, declared.dependencies()
                                                                         .stream()
                                                                         .map(MavenDependency::coordinate)
                                                                         .toList(),
                declaredBy, artifacts.stream()
                                     .map(Path::toString)
                                     .toList(),
                mediated, failures, platformStates, report, localRepository.toString(), linker.directory()
                                                                                              .toString(),
                lockfileStore.path()
                             .toString(),
                loaderHolder.generation(), loaderHolder.retiredGenerationsLive(), Instant.now());
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
     * The reported scope name of one declaration.
     *
     * @param dependency the declaration
     * @return the scope name
     */
    private static String scopeName(MavenDependency dependency) {
        return dependency.scope() == MavenDependency.Scope.PLATFORM ? ArtifactStatus.SCOPE_PLATFORM : ArtifactStatus.SCOPE_MODULE;
    }

    /**
     * The union of both tiers' artifacts, first occurrence wins.
     *
     * @param module the module-tier artifacts
     * @param platform the platform-tier artifacts
     * @return the union
     */
    private static List<ResolvedArtifact> union(List<ResolvedArtifact> module, List<ResolvedArtifact> platform) {
        List<ResolvedArtifact> union = new ArrayList<>(module);
        Set<Path> seen = new LinkedHashSet<>(paths(module));
        for (ResolvedArtifact artifact : platform) {
            if (seen.add(artifact.path())) {
                union.add(artifact);
            }
        }
        return union;
    }

    /**
     * The paths of the artifacts.
     *
     * @param artifacts the artifacts
     * @return the paths
     */
    private static List<Path> paths(List<ResolvedArtifact> artifacts) {
        return artifacts.stream()
                        .map(ResolvedArtifact::path)
                        .toList();
    }

    /**
     * Capitalizes the first character.
     *
     * @param value the value
     * @return the capitalized value
     */
    private static String capitalize(String value) {
        return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    /**
     * The current declared / resolved state.
     *
     * @return the state
     */
    DependenciesState getState() {
        DependenciesState state = lastState.get();
        if (state == null) {
            return DependenciesState.empty(isDynamicEnabled(), isFrozen(), linker.directory()
                                                                                 .toString(),
                    lockfileStore.path()
                                 .toString());
        }
        return state.refreshed(isDynamicEnabled(), isFrozen(), loaderHolder.generation(), loaderHolder.retiredGenerationsLive());
    }

}
