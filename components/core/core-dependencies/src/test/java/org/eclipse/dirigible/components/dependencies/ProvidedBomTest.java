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

import java.io.ByteArrayInputStream;
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
 * The provided-BOM contract at the resolver: a coordinate the platform provides at the declared
 * version is satisfied without a download and without a warning, a different-version request is
 * reported as shadowed with both versions (and never downloaded - parent-first delegation would
 * serve the platform's copy anyway), and a platform-provided transitive is pruned from the graph.
 * All repositories are file: fixtures - no test touches the network.
 */
class ProvidedBomTest {

    @TempDir
    Path tempDir;

    private Path repositoryDir;
    private Path localRepository;
    private ProvidedBom bom;
    private MavenDependencyResolver resolver;

    @BeforeEach
    void setUp() throws IOException {
        repositoryDir = Files.createDirectories(tempDir.resolve("fixture-repo"));
        localRepository = tempDir.resolve("local-repo");
        MavenResolverConfig config = new MavenResolverConfig(localRepository, List.of(new MavenRepository("fixture", repositoryDir.toUri()
                                                                                                                                  .toString(),
                null, null)), false, null);
        bom = new ProvidedBom(Map.of());
        resolver = new MavenDependencyResolver(() -> config, () -> bom);
    }

    @AfterEach
    void tearDown() {
        resolver.shutdown();
    }

    @Test
    void a_provided_coordinate_at_the_platform_version_is_satisfied_without_a_download() throws IOException {
        deploy("widget", "1.0.0", "");
        bom = new ProvidedBom(Map.of("com.example:widget", "1.0.0"));

        ResolutionResult result = resolver.resolve(declared(module("com.example:widget:1.0.0")));

        assertThat(result.provided()).containsExactly("com.example:widget:1.0.0");
        assertThat(result.shadowed()).isEmpty();
        assertThat(result.failures()).isEmpty();
        assertThat(result.artifacts()).isEmpty();
        // never downloaded - the platform already carries it
        assertThat(localRepository.resolve("com/example/widget/1.0.0/widget-1.0.0.jar")).doesNotExist();
    }

    @Test
    void a_different_version_request_is_shadowed_with_both_versions() {
        bom = new ProvidedBom(Map.of("com.example:widget", "1.0.0"));

        ResolutionResult result = resolver.resolve(declared(module("com.example:widget:2.0.0")));

        assertThat(result.shadowed()).hasSize(1);
        ResolutionResult.Shadowed shadowed = result.shadowed()
                                                   .get(0);
        assertThat(shadowed.groupArtifact()).isEqualTo("com.example:widget");
        assertThat(shadowed.requested()).isEqualTo("2.0.0");
        assertThat(shadowed.providedVersion()).isEqualTo("1.0.0");
        assertThat(result.provided()).isEmpty();
        assertThat(result.failures()).isEmpty();
        // the inert version is not downloaded either - parent-first delegation would never serve it
        assertThat(localRepository.resolve("com/example/widget/2.0.0/widget-2.0.0.jar")).doesNotExist();
    }

    @Test
    void a_provided_transitive_is_pruned_from_the_graph() throws IOException {
        deploy("leaf", "1.0.0", "");
        deploy("mid", "1.0.0", dependencyOn("leaf", "1.0.0"));
        bom = new ProvidedBom(Map.of("com.example:leaf", "1.0.0"));

        ResolutionResult result = resolver.resolve(declared(module("com.example:mid:1.0.0")));

        assertThat(result.failures()).isEmpty();
        assertThat(result.artifacts()).containsExactly(localRepository.resolve("com/example/mid/1.0.0/mid-1.0.0.jar"));
        assertThat(localRepository.resolve("com/example/leaf/1.0.0/leaf-1.0.0.jar")).doesNotExist();
    }

    @Test
    void the_generated_bom_round_trips_through_the_parser() throws Exception {
        Map<String, String> provided = ProvidedBomGenerator.parseDependencyList(List.of("The following files have been resolved:",
                "   com.google.gson:gson:jar:2.13.2:compile -- module com.google.gson", "   com.example:widget:jar:1.0.0:runtime",
                "   io.netty:netty-transport-native-epoll:jar:linux-x86_64:4.1.100.Final:runtime",
                "   com.example:pom-only:pom:1.0.0:compile", "", "   none"));
        assertThat(provided).containsEntry("com.google.gson:gson", "2.13.2")
                            .containsEntry("com.example:widget", "1.0.0")
                            .containsEntry("io.netty:netty-transport-native-epoll", "4.1.100.Final")
                            .doesNotContainKey("com.example:pom-only");

        String xml = ProvidedBomGenerator.bom(provided, "15.0.0-SNAPSHOT");
        ProvidedBom parsed = ProvidedBom.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        assertThat(parsed.providedVersion("com.google.gson:gson")).isEqualTo("2.13.2");
        assertThat(parsed.providedVersion("com.example:widget")).isEqualTo("1.0.0");
        assertThat(parsed.providedVersion("com.example:absent")).isNull();
        assertThat(xml).contains("<artifactId>dirigible-provided-bom</artifactId>")
                       .contains("<version>15.0.0-SNAPSHOT</version>");
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
        Files.writeString(directory.resolve(artifactId + "-" + version + ".pom"), """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>%s</artifactId>
                    <version>%s</version>
                    %s
                </project>
                """.formatted(artifactId, version, dependenciesXml));
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(directory.resolve(artifactId + "-" + version + ".jar")))) {
            out.putNextEntry(new JarEntry("fixture.txt"));
            out.write("fixture".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
        }
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

}
