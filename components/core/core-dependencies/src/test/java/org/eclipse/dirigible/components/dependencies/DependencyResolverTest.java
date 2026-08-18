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

import org.eclipse.dirigible.components.dependencies.MavenDependency.Scope;
import org.eclipse.dirigible.components.dependencies.MavenResolverConfig.MavenRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Resolves fixture artifacts from a file: repository laid out in a temporary directory - no test
 * touches the network.
 */
class DependencyResolverTest {

    @TempDir
    Path tempDir;

    private Path repositoryDir;
    private Path localRepository;
    private MavenDependencyResolver resolver;

    @BeforeEach
    void setUp() throws IOException {
        repositoryDir = Files.createDirectories(tempDir.resolve("fixture-repo"));
        localRepository = tempDir.resolve("local-repo");
        MavenResolverConfig config = new MavenResolverConfig(localRepository, List.of(new MavenRepository("fixture", repositoryDir.toUri()
                                                                                                                                  .toString(),
                null, null)), false, null);
        resolver = new MavenDependencyResolver(() -> config, () -> new ProvidedBom(Map.of()));
    }

    @AfterEach
    void tearDown() {
        resolver.shutdown();
    }

    @Test
    void resolves_the_transitive_graph_into_the_local_repository() throws IOException {
        deploy("leaf", "1.0.0", "");
        deploy("mid", "1.0.0", dependencyOn("leaf", "1.0.0"));

        ResolutionResult result = resolver.resolve(declared(module("com.example:mid:1.0.0")));

        assertThat(result.failures()).isEmpty();
        assertThat(result.mediated()).isEmpty();
        assertThat(result.artifacts()).containsExactlyInAnyOrder(localRepository.resolve("com/example/mid/1.0.0/mid-1.0.0.jar"),
                localRepository.resolve("com/example/leaf/1.0.0/leaf-1.0.0.jar"));
        assertThat(result.artifacts()).allSatisfy(artifact -> assertThat(artifact).exists());
    }

    @Test
    void applies_the_declared_exclusions() throws IOException {
        deploy("leaf", "1.0.0", "");
        deploy("mid", "1.0.0", dependencyOn("leaf", "1.0.0"));

        ResolutionResult exact =
                resolver.resolve(declared(new MavenDependency("com.example:mid:1.0.0", Scope.MODULE, List.of("com.example:leaf"))));
        assertThat(exact.failures()).isEmpty();
        assertThat(exact.artifacts()).containsExactly(localRepository.resolve("com/example/mid/1.0.0/mid-1.0.0.jar"));

        ResolutionResult wildcard =
                resolver.resolve(declared(new MavenDependency("com.example:mid:1.0.0", Scope.MODULE, List.of("com.example:*"))));
        assertThat(wildcard.failures()).isEmpty();
        assertThat(wildcard.artifacts()).containsExactly(localRepository.resolve("com/example/mid/1.0.0/mid-1.0.0.jar"));
    }

    @Test
    void reports_the_mediated_version_when_conflicting_versions_are_requested() throws IOException {
        deploy("leaf", "1.0.0", "");
        deploy("leaf", "1.1.0", "");

        ResolutionResult result = resolver.resolve(declared(module("com.example:leaf:1.0.0"), module("com.example:leaf:1.1.0")));

        assertThat(result.failures()).isEmpty();
        // directly declared conflicts are mediated first-declaration-wins
        assertThat(result.mediated()).containsEntry("com.example:leaf", "1.0.0");
        assertThat(result.artifacts()).containsExactly(localRepository.resolve("com/example/leaf/1.0.0/leaf-1.0.0.jar"));
    }

    @Test
    void reports_the_mediated_version_of_a_transitive_conflict() throws IOException {
        deploy("leaf", "1.0.0", "");
        deploy("leaf", "1.1.0", "");
        deploy("mid", "1.0.0", dependencyOn("leaf", "1.1.0"));

        // the direct declaration is nearer than mid's transitive request, so 1.0.0 wins
        ResolutionResult result = resolver.resolve(declared(module("com.example:leaf:1.0.0"), module("com.example:mid:1.0.0")));

        assertThat(result.failures()).isEmpty();
        assertThat(result.mediated()).containsEntry("com.example:leaf", "1.0.0");
        assertThat(result.artifacts()).containsExactlyInAnyOrder(localRepository.resolve("com/example/leaf/1.0.0/leaf-1.0.0.jar"),
                localRepository.resolve("com/example/mid/1.0.0/mid-1.0.0.jar"));
    }

    @Test
    void reports_an_unresolvable_coordinate_without_failing_the_rest() throws IOException {
        deploy("leaf", "1.0.0", "");
        deploy("mid", "1.0.0", dependencyOn("leaf", "1.0.0"));

        ResolutionResult result = resolver.resolve(declared(module("com.example:mid:1.0.0"), module("com.example:missing:9.9.9")));

        assertThat(result.artifacts()).containsExactlyInAnyOrder(localRepository.resolve("com/example/mid/1.0.0/mid-1.0.0.jar"),
                localRepository.resolve("com/example/leaf/1.0.0/leaf-1.0.0.jar"));
        assertThat(result.failures()).containsKey("com.example:missing:9.9.9");
        assertThat(result.failures()
                         .get("com.example:missing:9.9.9")).contains("missing")
                                                           .contains("fixture");
    }

    @Test
    void resolves_platform_scoped_declarations_like_any_other() throws IOException {
        deploy("leaf", "1.0.0", "");

        // scope is an activation concern (system classloader vs modules classloader) - the
        // resolver itself is scope-agnostic
        ResolutionResult result = resolver.resolve(declared(new MavenDependency("com.example:leaf:1.0.0", Scope.PLATFORM, List.of())));

        assertThat(result.failures()).isEmpty();
        assertThat(result.artifacts()).containsExactly(localRepository.resolve("com/example/leaf/1.0.0/leaf-1.0.0.jar"));
    }

    private static MavenDependency module(String coordinate) {
        return new MavenDependency(coordinate, Scope.MODULE, List.of());
    }

    private static Set<MavenDependency> declared(MavenDependency... dependencies) {
        return new LinkedHashSet<>(Arrays.asList(dependencies));
    }

    private void deploy(String artifactId, String version, String dependenciesXml) throws IOException {
        Path directory = repositoryDir.resolve("com/example")
                                      .resolve(artifactId)
                                      .resolve(version);
        Files.createDirectories(directory);
        Files.writeString(directory.resolve(artifactId + "-" + version + ".pom"), pom(artifactId, version, dependenciesXml));
        writeJar(directory.resolve(artifactId + "-" + version + ".jar"));
    }

    private static String pom(String artifactId, String version, String dependenciesXml) {
        return """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>%s</artifactId>
                    <version>%s</version>
                    %s
                </project>
                """.formatted(artifactId, version, dependenciesXml);
    }

    private static String dependencyOn(String artifactId, String version) {
        return """
                <dependencies>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>%s</artifactId>
                        <version>%s</version>
                    </dependency>
                </dependencies>
                """.formatted(artifactId, version);
    }

    private static void writeJar(Path path) throws IOException {
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(path))) {
            out.putNextEntry(new JarEntry("fixture.txt"));
            out.write("fixture".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
    }

}
