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

import nl.altindag.log.LogCaptor;
import org.eclipse.dirigible.components.dependencies.MavenDependency.Scope;
import org.eclipse.dirigible.components.project.ProjectMetadata;
import org.eclipse.dirigible.components.project.ProjectMetadataDependency;
import org.eclipse.dirigible.components.project.ProjectMetadataUtils;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The project.json parsing contract of the maven dependency type - maven entries parse, git entries
 * keep their exact current meaning, and unknown types are tolerated with a warning so an older
 * platform accepts a newer descriptor.
 */
class ProjectMetadataParsingTest {

    private static final String PROJECT_JSON = """
            {
                "guid": "my-project",
                "dependencies": [
                    { "type": "git", "guid": "employees",
                      "url": "https://github.com/example/employees.git", "branch": "main" },
                    { "type": "maven", "id": "com.businessintents:employees:1.4.0" },
                    { "type": "maven", "id": "com.example:widget:2.1.0",
                      "scope": "module",
                      "exclusions": ["com.fasterxml.jackson.core:*"] }
                ],
                "actions": []
            }
            """;

    @Test
    void maven_entries_parse() {
        ProjectMetadata metadata = ProjectMetadataUtils.fromJson(PROJECT_JSON);

        ProjectMetadataDependency[] dependencies = metadata.getDependencies();
        assertThat(dependencies).hasSize(3);
        assertThat(dependencies[1].getType()).isEqualTo(ProjectMetadataDependency.TYPE_MAVEN);
        assertThat(dependencies[1].getId()).isEqualTo("com.businessintents:employees:1.4.0");
        assertThat(dependencies[1].getScope()).isNull();
        assertThat(dependencies[1].getExclusions()).isNull();
        assertThat(dependencies[2].getScope()).isEqualTo("module");
        assertThat(dependencies[2].getExclusions()).containsExactly("com.fasterxml.jackson.core:*");
    }

    @Test
    void git_entries_are_unaffected() {
        ProjectMetadata metadata = ProjectMetadataUtils.fromJson(PROJECT_JSON);

        ProjectMetadataDependency git = metadata.getDependencies()[0];
        assertThat(git.getType()).isEqualTo(ProjectMetadataDependency.TYPE_GIT);
        assertThat(git.getGuid()).isEqualTo("employees");
        assertThat(git.getUrl()).isEqualTo("https://github.com/example/employees.git");
        assertThat(git.getBranch()).isEqualTo("main");
    }

    @Test
    void collects_the_maven_declarations_and_skips_the_git_ones() {
        Set<MavenDependency> dependencies = new LinkedHashSet<>();
        Map<String, String> errors = new LinkedHashMap<>();

        ProjectDependenciesCollector.collectDeclared("my-project", ProjectMetadataUtils.fromJson(PROJECT_JSON), dependencies, errors);

        assertThat(errors).isEmpty();
        assertThat(dependencies).containsExactly(new MavenDependency("com.businessintents:employees:1.4.0", Scope.MODULE, List.of()),
                new MavenDependency("com.example:widget:2.1.0", Scope.MODULE, List.of("com.fasterxml.jackson.core:*")));
    }

    @Test
    void an_unknown_dependency_type_warns_and_is_ignored() {
        String json = """
                {
                    "guid": "my-project",
                    "dependencies": [
                        { "type": "npm", "id": "left-pad@1.3.0" }
                    ]
                }
                """;
        Set<MavenDependency> dependencies = new LinkedHashSet<>();
        Map<String, String> errors = new LinkedHashMap<>();

        try (LogCaptor logCaptor = LogCaptor.forClass(ProjectDependenciesCollector.class)) {
            ProjectDependenciesCollector.collectDeclared("my-project", ProjectMetadataUtils.fromJson(json), dependencies, errors);

            assertThat(logCaptor.getWarnLogs()).anySatisfy(message -> assertThat(message).contains("npm")
                                                                                         .contains("my-project"));
        }
        assertThat(dependencies).isEmpty();
        assertThat(errors).isEmpty();
    }

    @Test
    void a_version_range_becomes_a_declaration_error() {
        String json = """
                {
                    "guid": "my-project",
                    "dependencies": [
                        { "type": "maven", "id": "com.example:widget:[1.0,2.0)" }
                    ]
                }
                """;
        Set<MavenDependency> dependencies = new LinkedHashSet<>();
        Map<String, String> errors = new LinkedHashMap<>();

        ProjectDependenciesCollector.collectDeclared("my-project", ProjectMetadataUtils.fromJson(json), dependencies, errors);

        assertThat(dependencies).isEmpty();
        assertThat(errors).containsKey("com.example:widget:[1.0,2.0)");
        assertThat(errors.get("com.example:widget:[1.0,2.0)")).contains("exact version");
    }

    @Test
    void a_maven_entry_without_an_id_becomes_a_declaration_error() {
        String json = """
                {
                    "guid": "my-project",
                    "dependencies": [
                        { "type": "maven" }
                    ]
                }
                """;
        Set<MavenDependency> dependencies = new LinkedHashSet<>();
        Map<String, String> errors = new LinkedHashMap<>();

        ProjectDependenciesCollector.collectDeclared("my-project", ProjectMetadataUtils.fromJson(json), dependencies, errors);

        assertThat(dependencies).isEmpty();
        assertThat(errors).containsKey("my-project");
    }

    @Test
    void the_platform_scope_is_parsed() {
        String json = """
                {
                    "guid": "my-project",
                    "dependencies": [
                        { "type": "maven", "id": "com.example:widget:2.1.0", "scope": "platform" }
                    ]
                }
                """;
        Set<MavenDependency> dependencies = new LinkedHashSet<>();
        Map<String, String> errors = new LinkedHashMap<>();

        ProjectDependenciesCollector.collectDeclared("my-project", ProjectMetadataUtils.fromJson(json), dependencies, errors);

        // parsed here - rejected as unsupported by the resolver until phase 3
        assertThat(errors).isEmpty();
        assertThat(dependencies).containsExactly(new MavenDependency("com.example:widget:2.1.0", Scope.PLATFORM, List.of()));
    }

}
