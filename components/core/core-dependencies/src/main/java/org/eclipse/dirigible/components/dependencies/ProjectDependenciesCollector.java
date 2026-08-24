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

import org.eclipse.dirigible.components.project.ProjectMetadata;
import org.eclipse.dirigible.components.project.ProjectMetadataDependency;
import org.eclipse.dirigible.components.project.ProjectMetadataUtils;
import org.eclipse.dirigible.repository.api.ICollection;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Collects the maven dependency declarations from the project.json files of all projects in the
 * registry.
 */
@Component
class ProjectDependenciesCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectDependenciesCollector.class);

    /** The repository. */
    private final IRepository repository;

    /** The diagnostics the previous collection reported, joined - see {@link #report(List)}. */
    private final AtomicReference<String> reported = new AtomicReference<>("");

    /**
     * Instantiates a new collector.
     *
     * @param repository the repository
     */
    ProjectDependenciesCollector(IRepository repository) {
        this.repository = repository;
    }

    /**
     * Collects the declarations of all registry projects.
     *
     * @return the declared dependencies and the declaration errors
     */
    DeclaredDependencies collect() {
        Set<MavenDependency> dependencies = new LinkedHashSet<>();
        Map<String, String> errors = new LinkedHashMap<>();
        Map<String, Set<String>> declaredBy = new LinkedHashMap<>();
        List<Notice> notices = new ArrayList<>();
        ICollection registry = repository.getCollection(IRepositoryStructure.PATH_REGISTRY_PUBLIC);
        if (registry.exists()) {
            for (ICollection project : registry.getCollections()) {
                collectProject(project, dependencies, errors, declaredBy, notices);
            }
        }
        report(notices);
        return new DeclaredDependencies(dependencies, errors, declaredBy);
    }

    /**
     * Collects one registry project's declarations.
     *
     * @param project the registry project
     * @param dependencies the collected dependencies to add to
     * @param errors the declaration errors to add to
     * @param declaredBy the declaring projects per coordinate to add to
     * @param notices the diagnostics to add to
     */
    private void collectProject(ICollection project, Set<MavenDependency> dependencies, Map<String, String> errors,
            Map<String, Set<String>> declaredBy, List<Notice> notices) {
        IResource descriptor = project.getResource(ProjectMetadata.PROJECT_METADATA_FILE_NAME);
        if (!descriptor.exists()) {
            return;
        }
        ProjectMetadata metadata;
        try {
            metadata = ProjectMetadataUtils.fromJson(new String(descriptor.getContent(), StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            notices.add(Notice.warning(
                    "Ignoring the unparseable [" + ProjectMetadata.PROJECT_METADATA_FILE_NAME + "] of project [" + project.getName() + "]",
                    e));
            return;
        }
        if (metadata != null) {
            notices.addAll(collectDeclared(project.getName(), metadata, dependencies, errors, declaredBy));
        }
    }

    /**
     * Collects one project's maven declarations - git and typeless project-to-project entries keep
     * their meaning elsewhere, and unknown types are tolerated with a warning so an older platform
     * accepts a newer descriptor.
     *
     * @param project the project name
     * @param metadata the parsed project.json
     * @param dependencies the collected dependencies to add to
     * @param errors the declaration errors to add to
     * @param declaredBy the declaring projects per coordinate to add to
     * @return the diagnostics of this project's declarations
     */
    static List<Notice> collectDeclared(String project, ProjectMetadata metadata, Set<MavenDependency> dependencies,
            Map<String, String> errors, Map<String, Set<String>> declaredBy) {
        List<Notice> notices = new ArrayList<>();
        for (ProjectMetadataDependency declared : metadata.getDependencies()) {
            String type = declared.getType();
            if (type == null || type.isBlank()) {
                continue; // the classic project-to-project (guid) dependency, consumed by the IDE workspace
            }
            if (ProjectMetadataDependency.TYPE_GIT.equalsIgnoreCase(type)) {
                continue; // consumed by the IDE workspace
            }
            if (!ProjectMetadataDependency.TYPE_MAVEN.equalsIgnoreCase(type)) {
                notices.add(Notice.warning("Ignoring the dependency of unknown type [" + type + "] declared by project [" + project + "]",
                        null));
                continue;
            }
            String id = declared.getId();
            if (id == null || id.isBlank()) {
                String message = "Project [" + project + "] declares a maven dependency without an id (groupId:artifactId:version)";
                errors.put(project, message);
                notices.add(Notice.error(message, null));
                continue;
            }
            try {
                MavenDependency.Scope scope = MavenDependency.Scope.parse(declared.getScope());
                List<String> exclusions = declared.getExclusions();
                dependencies.add(new MavenDependency(id, scope, exclusions == null ? List.of() : exclusions));
                declaredBy.computeIfAbsent(id, key -> new LinkedHashSet<>())
                          .add(project);
            } catch (IllegalArgumentException e) {
                errors.put(id, "Project [" + project + "]: " + e.getMessage());
                notices.add(Notice.error("Invalid maven dependency [" + id + "] declared by project [" + project + "]", e));
            }
        }
        return notices;
    }

    /**
     * Logs this collection's diagnostics, but only when they differ from the previous collection's. The
     * watcher re-collects the registry every few seconds, so a declaration this platform ignores or
     * cannot resolve would otherwise re-log forever on an unmodified instance - which trains operators
     * to ignore the log and buries the real dependency problems.
     *
     * @param notices the diagnostics of this collection
     */
    private void report(List<Notice> notices) {
        String signature = notices.stream()
                                  .map(Notice::message)
                                  .collect(Collectors.joining("\n"));
        if (signature.equals(reported.getAndSet(signature))) {
            return;
        }
        notices.forEach(notice -> notice.log(LOGGER));
    }

    /**
     * One declaration diagnostic. The message is materialized rather than left as an SLF4J format,
     * because it doubles as the change-detection key {@link #report(List)} compares.
     *
     * @param error whether the declaration will not resolve, as opposed to one this platform merely
     *        ignores
     * @param message the message
     * @param cause the throwable behind the diagnostic, null when there is none
     */
    record Notice(boolean error, String message, Throwable cause) {

        /**
         * A declaration this platform ignores.
         *
         * @param message the message
         * @param cause the throwable behind the diagnostic, null when there is none
         * @return the notice
         */
        static Notice warning(String message, Throwable cause) {
            return new Notice(false, message, cause);
        }

        /**
         * A declaration that will not resolve.
         *
         * @param message the message
         * @param cause the throwable behind the diagnostic, null when there is none
         * @return the notice
         */
        static Notice error(String message, Throwable cause) {
            return new Notice(true, message, cause);
        }

        /**
         * Logs the diagnostic.
         *
         * @param logger the logger
         */
        void log(Logger logger) {
            if (error) {
                logger.error(message, cause);
            } else {
                logger.warn(message, cause);
            }
        }
    }

}
