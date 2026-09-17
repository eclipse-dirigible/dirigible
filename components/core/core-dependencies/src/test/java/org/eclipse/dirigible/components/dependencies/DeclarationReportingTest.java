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
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.local.LocalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A declaration diagnostic is logged once per declaration change, not once per watcher tick
 * (dirigible #6827). The watcher re-collects the registry every five seconds purely to compare the
 * declaration fingerprint, so a diagnostic emitted by the collection itself repeats forever on an
 * unmodified instance - which trains operators to ignore the log and buries the real dependency
 * problems.
 */
class DeclarationReportingTest {

    private IRepository repository;

    private ProjectDependenciesCollector collector;

    @BeforeEach
    void setUp(@TempDir Path root) {
        repository = new LocalRepository(root.toString(), true);
        collector = new ProjectDependenciesCollector(repository);
    }

    @Test
    void an_unknown_dependency_type_is_logged_once_across_repeated_collections() {
        declare("my-project", """
                { "guid": "my-project", "dependencies": [ { "type": "npm", "id": "left-pad@1.3.0" } ] }
                """);

        try (LogCaptor logCaptor = LogCaptor.forClass(ProjectDependenciesCollector.class)) {
            collector.collect();
            collector.collect();
            collector.collect();

            assertThat(logCaptor.getWarnLogs()).singleElement()
                                               .satisfies(message -> assertThat(message).contains("npm")
                                                                                        .contains("my-project"));
        }
    }

    @Test
    void a_changed_declaration_is_logged_again() {
        declare("my-project", """
                { "guid": "my-project", "dependencies": [ { "type": "npm", "id": "left-pad@1.3.0" } ] }
                """);

        try (LogCaptor logCaptor = LogCaptor.forClass(ProjectDependenciesCollector.class)) {
            collector.collect();
            declare("my-project", """
                    { "guid": "my-project", "dependencies": [ { "type": "gem", "id": "rails" } ] }
                    """);
            collector.collect();

            assertThat(logCaptor.getWarnLogs()).hasSize(2);
            assertThat(logCaptor.getWarnLogs()
                                .get(1)).contains("gem");
        }
    }

    @Test
    void a_declaration_error_is_logged_once_across_repeated_collections() {
        declare("my-project", """
                { "guid": "my-project", "dependencies": [ { "type": "maven", "id": "com.example:widget:[1.0,2.0)" } ] }
                """);

        try (LogCaptor logCaptor = LogCaptor.forClass(ProjectDependenciesCollector.class)) {
            collector.collect();
            collector.collect();

            assertThat(logCaptor.getErrorLogs()).singleElement()
                                                .satisfies(message -> assertThat(message).contains("com.example:widget:[1.0,2.0)"));
        }
    }

    @Test
    void a_typeless_project_dependency_never_reports() {
        // the form the shipped platform templates carry - a plain project reference that predates
        // typed dependencies
        declare("template-application-rest-java", """
                { "guid": "template-application-rest-java",
                  "dependencies": [ { "guid": "template-application-dao-java" } ], "actions": [] }
                """);

        try (LogCaptor logCaptor = LogCaptor.forClass(ProjectDependenciesCollector.class)) {
            collector.collect();
            collector.collect();

            assertThat(logCaptor.getWarnLogs()).isEmpty();
            assertThat(logCaptor.getErrorLogs()).isEmpty();
        }
    }

    /**
     * Writes a project.json into the registry.
     *
     * @param project the project name
     * @param descriptor the project.json content
     */
    private void declare(String project, String descriptor) {
        repository.createResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + project + "/project.json",
                descriptor.getBytes(StandardCharsets.UTF_8), false, "application/json", true);
    }
}
