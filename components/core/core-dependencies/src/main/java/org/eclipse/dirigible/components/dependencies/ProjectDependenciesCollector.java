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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Collects the maven dependency declarations from the project.json files of all projects in the
 * registry.
 */
@Component
class ProjectDependenciesCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectDependenciesCollector.class);

    /** The repository. */
    private final IRepository repository;

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
        ICollection registry = repository.getCollection(IRepositoryStructure.PATH_REGISTRY_PUBLIC);
        if (!registry.exists()) {
            return new DeclaredDependencies(dependencies, errors, declaredBy);
        }
        for (ICollection project : registry.getCollections()) {
            IResource descriptor = project.getResource(ProjectMetadata.PROJECT_METADATA_FILE_NAME);
            if (!descriptor.exists()) {
                continue;
            }
            ProjectMetadata metadata;
            try {
                metadata = ProjectMetadataUtils.fromJson(new String(descriptor.getContent(), StandardCharsets.UTF_8));
            } catch (RuntimeException e) {
                LOGGER.warn("Ignoring the unparseable [{}] of project [{}]", ProjectMetadata.PROJECT_METADATA_FILE_NAME, project.getName(),
                        e);
                continue;
            }
            if (metadata != null) {
                collectDeclared(project.getName(), metadata, dependencies, errors, declaredBy);
            }
        }
        return new DeclaredDependencies(dependencies, errors, declaredBy);
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
     */
    static void collectDeclared(String project, ProjectMetadata metadata, Set<MavenDependency> dependencies, Map<String, String> errors,
            Map<String, Set<String>> declaredBy) {
        for (ProjectMetadataDependency declared : metadata.getDependencies()) {
            String type = declared.getType();
            if (type == null || type.isBlank()) {
                continue; // the classic project-to-project (guid) dependency, consumed by the IDE workspace
            }
            if (ProjectMetadataDependency.TYPE_GIT.equalsIgnoreCase(type)) {
                continue; // consumed by the IDE workspace
            }
            if (!ProjectMetadataDependency.TYPE_MAVEN.equalsIgnoreCase(type)) {
                LOGGER.warn("Ignoring the dependency of unknown type [{}] declared by project [{}]", type, project);
                continue;
            }
            String id = declared.getId();
            if (id == null || id.isBlank()) {
                errors.put(project, "Project [" + project + "] declares a maven dependency without an id (groupId:artifactId:version)");
                LOGGER.error("Project [{}] declares a maven dependency without an id (groupId:artifactId:version)", project);
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
                LOGGER.error("Invalid maven dependency [{}] declared by project [{}]", id, project, e);
            }
        }
    }

}
