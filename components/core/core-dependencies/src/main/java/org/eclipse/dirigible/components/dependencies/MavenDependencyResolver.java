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

import jakarta.annotation.PreDestroy;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.AuthenticationSelector;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.MirrorSelector;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.eclipse.dirigible.components.dependencies.MavenResolverConfig.MavenRepository;
import org.eclipse.dirigible.components.dependencies.ResolutionResult.ResolvedArtifact;
import org.eclipse.dirigible.components.dependencies.ResolutionResult.Shadowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * The {@link DependencyResolver} over the Apache Maven Artifact Resolver - coordinates are taken
 * programmatically (no POM generation, no external mvn binary) and resolved in one union collect
 * request so Maven's standard version mediation applies globally. Every coordinate the
 * {@link ProvidedBom} lists is treated as provided - never downloaded, pruned from the graph - and
 * a declared coordinate the platform provides at a different version is reported as shadowed (see
 * {@link ProvidedBom} for why the report, not child-first loading, is the feature).
 */
@Component
class MavenDependencyResolver implements DependencyResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenDependencyResolver.class);

    /** The configuration source - read on every resolution so runtime changes take effect. */
    private final Supplier<MavenResolverConfig> configSupplier;

    /** The provided-BOM source - what the platform image already ships. */
    private final Supplier<ProvidedBom> bomSupplier;

    /** The repository system - thread-safe and configuration-independent, created once. */
    private final RepositorySystem repositorySystem;

    /**
     * Instantiates the resolver reading its configuration from the environment and the provided-BOM
     * from the classpath.
     */
    MavenDependencyResolver() {
        this(MavenResolverConfig::fromConfiguration, ProvidedBom::fromClasspath);
    }

    /**
     * Instantiates the resolver with explicit configuration and provided-BOM sources - the constructor
     * the unit tests use to point at file-based fixture repositories.
     *
     * @param configSupplier the configuration source
     * @param bomSupplier the provided-BOM source
     */
    MavenDependencyResolver(Supplier<MavenResolverConfig> configSupplier, Supplier<ProvidedBom> bomSupplier) {
        this.configSupplier = configSupplier;
        this.bomSupplier = bomSupplier;
        this.repositorySystem = new RepositorySystemSupplier().get();
    }

    /**
     * Shuts the repository system down.
     */
    @PreDestroy
    void shutdown() {
        repositorySystem.shutdown();
    }

    /**
     * Resolve.
     *
     * @param declared the union of all declared dependencies
     * @return the resolution result
     */
    @Override
    public ResolutionResult resolve(Set<MavenDependency> declared) {
        Map<String, String> failures = new LinkedHashMap<>();
        ProvidedBom bom = bomSupplier.get();
        List<String> provided = new ArrayList<>();
        List<Shadowed> shadowed = new ArrayList<>();
        List<MavenDependency> resolvable = partition(declared, bom, provided, shadowed);
        if (resolvable.isEmpty()) {
            return new ResolutionResult(List.of(), Map.of(), Map.of(), failures, provided, shadowed);
        }

        // the collect request merges duplicate groupId:artifactId root dependencies before conflict
        // resolution ever sees them, so direct conflicts are mediated here - first declaration wins -
        // and every declared version is seeded into the mediation report
        Map<String, Set<String>> requestedVersions = new LinkedHashMap<>();
        Map<String, MavenDependency> roots = new LinkedHashMap<>();
        for (MavenDependency dependency : resolvable) {
            String[] parts = dependency.coordinate()
                                       .split(":", -1);
            requestedVersions.computeIfAbsent(parts[0] + ":" + parts[1], key -> new LinkedHashSet<>())
                             .add(parts[2]);
            roots.putIfAbsent(parts[0] + ":" + parts[1], dependency);
        }

        MavenResolverConfig config = configSupplier.get();
        DefaultRepositorySystemSession session = newSession(config, bom);
        List<RemoteRepository> repositories = remoteRepositories(session, config);
        String repositoriesDescription = describe(repositories);
        LOGGER.info("Resolving [{}] declared maven dependency(ies) from repositories [{}] into local repository [{}]", resolvable.size(),
                repositoriesDescription, config.localRepository());

        List<MavenDependency> rootDependencies = List.copyOf(roots.values());
        CollectResult collectResult;
        try {
            collectResult = repositorySystem.collectDependencies(session, collectRequest(rootDependencies, repositories));
        } catch (DependencyCollectionException e) {
            LOGGER.error("Maven dependency graph collection failed for [{}] from repositories [{}]", coordinates(rootDependencies),
                    repositoriesDescription, e);
            resolvable.forEach(
                    dependency -> failures.putIfAbsent(dependency.coordinate(), "Dependency graph collection failed: " + e.getMessage()));
            return new ResolutionResult(List.of(), Map.of(), Map.of(), failures, provided, shadowed);
        }

        Map<String, String> winnerVersions = new LinkedHashMap<>();
        List<DependencyNode> winners = new ArrayList<>();
        Map<DependencyNode, String> winnerVia = new IdentityHashMap<>();
        walk(collectResult.getRoot(), null, requestedVersions, winnerVersions, winners, winnerVia);

        List<ResolvedArtifact> resolved = resolveArtifacts(session, winners, winnerVia, failures, repositoriesDescription);
        Map<String, String> mediated = mediated(requestedVersions, winnerVersions);
        Map<String, Set<String>> contested = new LinkedHashMap<>();
        mediated.forEach((ga, version) -> {
            contested.put(ga, requestedVersions.get(ga));
            LOGGER.info("Version mediation chose [{}:{}] out of the requested versions {}", ga, version, requestedVersions.get(ga));
        });
        return new ResolutionResult(resolved, mediated, contested, failures, provided, shadowed);
    }

    /**
     * Partitions the declared dependencies against the provided-BOM: a coordinate the platform provides
     * at the declared version is satisfied without a download, one it provides at a different version
     * is reported as shadowed (parent-first delegation would serve the platform's version anyway), and
     * everything else proceeds to resolution.
     *
     * @param declared the declared dependencies
     * @param bom the provided-BOM
     * @param provided the platform-satisfied coordinates to add to
     * @param shadowed the shadowed declarations to add to
     * @return the dependencies to resolve
     */
    private List<MavenDependency> partition(Set<MavenDependency> declared, ProvidedBom bom, List<String> provided,
            List<Shadowed> shadowed) {
        List<MavenDependency> resolvable = new ArrayList<>();
        for (MavenDependency dependency : declared) {
            String[] parts = dependency.coordinate()
                                       .split(":", -1);
            String groupArtifact = parts[0] + ":" + parts[1];
            String providedVersion = bom.providedVersion(groupArtifact);
            if (providedVersion == null) {
                resolvable.add(dependency);
            } else if (providedVersion.equals(parts[2])) {
                provided.add(dependency.coordinate());
                LOGGER.info("Declared [{}] is provided by the platform - satisfied without a download", dependency.coordinate());
            } else {
                shadowed.add(new Shadowed(groupArtifact, parts[2], providedVersion));
                LOGGER.warn(
                        "Declared [{}] is SHADOWED: requested [{}], the platform provides [{}] - parent-first delegation serves"
                                + " the platform's version, so the declared version is inert and is not downloaded",
                        groupArtifact, parts[2], providedVersion);
            }
        }
        return resolvable;
    }

    /**
     * Creates the session - local repository, offline mode, verbose conflict resolution (so version
     * mediation is reportable), the settings.xml mirror / proxy / server selectors and the provided-BOM
     * pruning selector.
     *
     * @param config the configuration
     * @param bom the provided-BOM
     * @return the session
     */
    private DefaultRepositorySystemSession newSession(MavenResolverConfig config, ProvidedBom bom) {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        session.setLocalRepositoryManager(repositorySystem.newLocalRepositoryManager(session, new LocalRepository(config.localRepository()
                                                                                                                        .toFile())));
        session.setOffline(config.offline());
        session.setSystemProperties(System.getProperties());
        // keep conflict losers in the graph so the mediation report can name the requested versions
        session.setConfigProperty(ConflictResolver.CONFIG_PROP_VERBOSE, Boolean.TRUE);
        // remote repositories are instance-level operator configuration - a repository declared
        // inside a dependency's POM must not redirect where artifacts are downloaded from
        session.setIgnoreArtifactDescriptorRepositories(true);
        if (!bom.isEmpty()) {
            // a platform-provided transitive is pruned with its subtree - the platform already
            // carries what its own version needs, exactly like Maven's provided scope
            session.setDependencySelector(new AndDependencySelector(session.getDependencySelector(), new ProvidedPruningSelector(bom)));
        }
        Settings settings = readSettings(config.settingsXml());
        if (settings != null) {
            session.setMirrorSelector(mirrorSelector(settings));
            session.setProxySelector(proxySelector(settings));
            session.setAuthenticationSelector(authenticationSelector(settings));
        }
        return session;
    }

    /**
     * Prunes platform-provided coordinates from the dependency graph - they are never downloaded and
     * never enter the modules tier; at runtime parent-first delegation serves the platform's copy.
     */
    private static final class ProvidedPruningSelector implements DependencySelector {

        /** The provided-BOM. */
        private final ProvidedBom bom;

        /**
         * Instantiates a new selector.
         *
         * @param bom the provided-BOM
         */
        private ProvidedPruningSelector(ProvidedBom bom) {
            this.bom = bom;
        }

        /**
         * Select dependency.
         *
         * @param dependency the dependency
         * @return false when the platform provides the coordinate
         */
        @Override
        public boolean selectDependency(Dependency dependency) {
            Artifact artifact = dependency.getArtifact();
            String providedVersion = bom.providedVersion(artifact.getGroupId() + ":" + artifact.getArtifactId());
            if (providedVersion == null) {
                return true;
            }
            LOGGER.debug("Transitive [{}:{}:{}] is provided by the platform as [{}] - pruned from the graph", artifact.getGroupId(),
                    artifact.getArtifactId(), artifact.getVersion(), providedVersion);
            return false;
        }

        /**
         * Derive child selector.
         *
         * @param context the context
         * @return this selector - the decision is context-free
         */
        @Override
        public DependencySelector deriveChildSelector(DependencyCollectionContext context) {
            return this;
        }
    }

    /**
     * Builds the union collect request - every declared dependency becomes a root dependency of one
     * request, so one mediation covers the whole flat classpath.
     *
     * @param resolvable the declared dependencies
     * @param repositories the remote repositories
     * @return the collect request
     */
    private CollectRequest collectRequest(List<MavenDependency> resolvable, List<RemoteRepository> repositories) {
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRepositories(repositories);
        for (MavenDependency dependency : resolvable) {
            List<Exclusion> exclusions = dependency.exclusions()
                                                   .stream()
                                                   .map(exclusion -> {
                                                       String[] parts = exclusion.split(":", -1);
                                                       return new Exclusion(parts[0], parts[1], "*", "*");
                                                   })
                                                   .toList();
            collectRequest.addDependency(
                    new Dependency(new DefaultArtifact(dependency.coordinate()), JavaScopes.RUNTIME, false, exclusions));
        }
        return collectRequest;
    }

    /**
     * Walks the verbose dependency graph - conflict losers are retained as leaves carrying the winner
     * reference, so both the winners (the classpath) and every requested version (the mediation report)
     * come out of one walk. Each winner is attributed to the declared root whose subtree it was first
     * seen in, so the lockfile can record how a transitive artifact entered the classpath.
     *
     * @param node the node whose children are walked
     * @param via the declared root owning this subtree, null at the graph root
     * @param requestedVersions all requested versions per groupId:artifactId
     * @param winnerVersions the winning version per groupId:artifactId
     * @param winners the winner nodes in graph order
     * @param winnerVia the declared root per winner node, absent for the roots themselves
     */
    private void walk(DependencyNode node, String via, Map<String, Set<String>> requestedVersions, Map<String, String> winnerVersions,
            List<DependencyNode> winners, Map<DependencyNode, String> winnerVia) {
        for (DependencyNode child : node.getChildren()) {
            Artifact artifact = child.getArtifact();
            if (artifact == null) {
                continue;
            }
            String ga = artifact.getGroupId() + ":" + artifact.getArtifactId();
            requestedVersions.computeIfAbsent(ga, key -> new LinkedHashSet<>())
                             .add(artifact.getVersion());
            if (child.getData()
                     .get(ConflictResolver.NODE_DATA_WINNER) != null) {
                continue; // a conflict loser - a leaf by definition of the verbose graph
            }
            if (winnerVersions.putIfAbsent(ga, artifact.getVersion()) == null && isClasspathScope(child)) {
                winners.add(child);
                if (via != null) {
                    winnerVia.put(child, via);
                }
            }
            String childVia = via != null ? via : ga + ":" + artifact.getVersion();
            walk(child, childVia, requestedVersions, winnerVersions, winners, winnerVia);
        }
    }

    /**
     * Checks if is classpath scope.
     *
     * @param node the node
     * @return true, if is classpath scope
     */
    private boolean isClasspathScope(DependencyNode node) {
        String scope = node.getDependency()
                           .getScope();
        return scope == null || scope.isEmpty() || JavaScopes.COMPILE.equals(scope) || JavaScopes.RUNTIME.equals(scope);
    }

    /**
     * Resolves the winner nodes' artifacts into the local repository - a failing artifact is reported
     * per coordinate and never fails the rest.
     *
     * @param session the session
     * @param winners the winner nodes
     * @param winnerVia the declared root per winner node
     * @param failures the per-coordinate failures to add to
     * @param repositoriesDescription the repositories, for the error log
     * @return the resolved artifacts
     */
    private List<ResolvedArtifact> resolveArtifacts(RepositorySystemSession session, List<DependencyNode> winners,
            Map<DependencyNode, String> winnerVia, Map<String, String> failures, String repositoriesDescription) {
        List<ArtifactRequest> requests = winners.stream()
                                                .map(ArtifactRequest::new)
                                                .toList();
        List<ArtifactResult> results;
        try {
            results = repositorySystem.resolveArtifacts(session, requests);
        } catch (ArtifactResolutionException e) {
            results = e.getResults();
        }
        List<ResolvedArtifact> artifacts = new ArrayList<>();
        for (ArtifactResult result : results) {
            if (result.isResolved()) {
                Artifact resolved = result.getArtifact();
                String coordinate = resolved.getGroupId() + ":" + resolved.getArtifactId() + ":" + resolved.getVersion();
                artifacts.add(new ResolvedArtifact(coordinate, resolved.getFile()
                                                                       .toPath(),
                        winnerVia.get(result.getRequest()
                                            .getDependencyNode())));
            } else {
                Artifact artifact = result.getRequest()
                                          .getArtifact();
                String coordinate = artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion();
                String message = result.getExceptions()
                                       .stream()
                                       .map(Throwable::getMessage)
                                       .filter(m -> m != null && !m.isBlank())
                                       .collect(Collectors.joining("; "));
                if (message.isEmpty()) {
                    message = "Could not resolve the artifact";
                }
                failures.put(coordinate, message);
                Exception cause = result.getExceptions()
                                        .isEmpty() ? null
                                                : result.getExceptions()
                                                        .get(0);
                LOGGER.error("Could not resolve maven dependency [{}] from repositories [{}]: {}", coordinate, repositoriesDescription,
                        message, cause);
            }
        }
        return artifacts;
    }

    /**
     * The mediation report - the groupId:artifactId entries the graph requested in more than one
     * version, mapped to the version mediation chose.
     *
     * @param requestedVersions all requested versions per groupId:artifactId
     * @param winnerVersions the winning version per groupId:artifactId
     * @return the mediated versions
     */
    private Map<String, String> mediated(Map<String, Set<String>> requestedVersions, Map<String, String> winnerVersions) {
        Map<String, String> mediated = new LinkedHashMap<>();
        requestedVersions.forEach((ga, versions) -> {
            String winner = winnerVersions.get(ga);
            if (versions.size() > 1 && winner != null) {
                mediated.put(ga, winner);
            }
        });
        return mediated;
    }

    /**
     * Builds the remote repositories - the configured credentials are applied first, then the session's
     * settings.xml mirror / proxy / server selectors, so corporate setups work unmodified.
     *
     * @param session the session
     * @param config the configuration
     * @return the remote repositories
     */
    private List<RemoteRepository> remoteRepositories(RepositorySystemSession session, MavenResolverConfig config) {
        List<RemoteRepository> repositories = new ArrayList<>();
        for (MavenRepository definition : config.repositories()) {
            RemoteRepository.Builder builder = new RemoteRepository.Builder(definition.id(), "default", definition.url());
            if (definition.username() != null && !definition.username()
                                                            .isBlank()) {
                builder.setAuthentication(new AuthenticationBuilder().addUsername(definition.username())
                                                                     .addPassword(definition.password())
                                                                     .build());
            }
            repositories.add(applySessionSelectors(session, builder.build()));
        }
        return repositories;
    }

    /**
     * Applies the session's mirror, proxy and authentication selectors to an explicitly configured
     * repository - the resolver only applies them to repositories discovered during collection, so the
     * top-level ones have to be processed here.
     *
     * @param session the session
     * @param repository the repository
     * @return the effective repository
     */
    private RemoteRepository applySessionSelectors(RepositorySystemSession session, RemoteRepository repository) {
        RemoteRepository effective = repository;
        MirrorSelector mirrorSelector = session.getMirrorSelector();
        if (mirrorSelector != null) {
            RemoteRepository mirror = mirrorSelector.getMirror(effective);
            if (mirror != null) {
                effective = mirror;
            }
        }
        RemoteRepository.Builder builder = new RemoteRepository.Builder(effective);
        boolean changed = false;
        ProxySelector proxySelector = session.getProxySelector();
        if (effective.getProxy() == null && proxySelector != null) {
            Proxy proxy = proxySelector.getProxy(effective);
            if (proxy != null) {
                builder.setProxy(proxy);
                changed = true;
            }
        }
        AuthenticationSelector authenticationSelector = session.getAuthenticationSelector();
        if (effective.getAuthentication() == null && authenticationSelector != null) {
            Authentication authentication = authenticationSelector.getAuthentication(effective);
            if (authentication != null) {
                builder.setAuthentication(authentication);
                changed = true;
            }
        }
        return changed ? builder.build() : effective;
    }

    /**
     * Reads the effective settings.
     *
     * @param settingsXml the settings.xml path, null when absent
     * @return the settings, null when absent or unreadable
     */
    private Settings readSettings(Path settingsXml) {
        if (settingsXml == null) {
            return null;
        }
        try {
            DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
            request.setUserSettingsFile(settingsXml.toFile());
            return new DefaultSettingsBuilderFactory().newInstance()
                                                      .build(request)
                                                      .getEffectiveSettings();
        } catch (SettingsBuildingException e) {
            LOGGER.warn("Ignoring the unreadable maven settings [{}]", settingsXml, e);
            return null;
        }
    }

    /**
     * Mirror selector from the settings.
     *
     * @param settings the settings
     * @return the mirror selector
     */
    private MirrorSelector mirrorSelector(Settings settings) {
        DefaultMirrorSelector selector = new DefaultMirrorSelector();
        for (Mirror mirror : settings.getMirrors()) {
            selector.add(mirror.getId(), mirror.getUrl(), mirror.getLayout(), false, mirror.isBlocked(), mirror.getMirrorOf(),
                    mirror.getMirrorOfLayouts());
        }
        return selector;
    }

    /**
     * Proxy selector from the settings.
     *
     * @param settings the settings
     * @return the proxy selector
     */
    private ProxySelector proxySelector(Settings settings) {
        DefaultProxySelector selector = new DefaultProxySelector();
        for (org.apache.maven.settings.Proxy proxy : settings.getProxies()) {
            if (!proxy.isActive()) {
                continue;
            }
            Authentication authentication = null;
            if (proxy.getUsername() != null && !proxy.getUsername()
                                                     .isBlank()) {
                authentication = new AuthenticationBuilder().addUsername(proxy.getUsername())
                                                            .addPassword(proxy.getPassword())
                                                            .build();
            }
            selector.add(new Proxy(proxy.getProtocol(), proxy.getHost(), proxy.getPort(), authentication), proxy.getNonProxyHosts());
        }
        return selector;
    }

    /**
     * Authentication selector from the settings' servers, so a mirrored corporate repository
     * authenticates with the credentials its settings.xml already carries.
     *
     * @param settings the settings
     * @return the authentication selector
     */
    private AuthenticationSelector authenticationSelector(Settings settings) {
        DefaultAuthenticationSelector selector = new DefaultAuthenticationSelector();
        for (Server server : settings.getServers()) {
            if (server.getUsername() == null && server.getPrivateKey() == null) {
                continue;
            }
            AuthenticationBuilder builder = new AuthenticationBuilder().addUsername(server.getUsername())
                                                                       .addPassword(server.getPassword());
            if (server.getPrivateKey() != null) {
                builder.addPrivateKey(server.getPrivateKey(), server.getPassphrase());
            }
            selector.add(server.getId(), builder.build());
        }
        return selector;
    }

    /**
     * Describes the repositories for logging - ids and URLs, never credentials.
     *
     * @param repositories the repositories
     * @return the description
     */
    private String describe(List<RemoteRepository> repositories) {
        return repositories.stream()
                           .map(repository -> repository.getId() + " (" + repository.getUrl() + ")")
                           .collect(Collectors.joining(", "));
    }

    /**
     * Coordinates.
     *
     * @param dependencies the dependencies
     * @return the string
     */
    private String coordinates(List<MavenDependency> dependencies) {
        return dependencies.stream()
                           .map(MavenDependency::coordinate)
                           .collect(Collectors.joining(", "));
    }

}
