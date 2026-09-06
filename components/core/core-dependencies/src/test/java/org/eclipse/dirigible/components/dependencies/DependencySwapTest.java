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

import org.eclipse.dirigible.components.dependencies.DependencySynchronizer.SwapOutcome;
import org.eclipse.dirigible.components.initializers.classpath.ClasspathExpander;
import org.eclipse.dirigible.engine.java.runtime.ModulesClassLoaderHolder;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.local.LocalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What the swap does to the running system beyond installing a generation: which declarations it
 * refuses, and how much of the registry a leaving or upgraded module takes with it.
 */
class DependencySwapTest {

    @TempDir
    Path tempDir;

    private IRepository repository;
    private ModulesClassLoaderHolder loaderHolder;
    private DependencySynchronizer synchronizer;
    private Path localRepository;

    @BeforeEach
    void setUp() throws IOException {
        repository = new LocalRepository(tempDir.resolve("repository")
                                                .toString(),
                true);
        loaderHolder = new ModulesClassLoaderHolder();
        synchronizer = new DependencySynchronizer(loaderHolder, new ClasspathExpander(repository), event -> {
        });
        localRepository = Files.createDirectories(tempDir.resolve("m2"));
    }

    @Test
    void aNativeLibraryRejectsOnlyItsOwnDeclaration() throws IOException {
        Path plain = artifact("com.example", "plain", "1.0.0", Map.of("com/example/Plain.class", "bytes"));
        Path nativeBearing = artifact("com.example", "sqlite", "1.0.0", Map.of("org/sqlite/native/libsqlite.so", "bytes"));

        SwapOutcome outcome = synchronizer.swap(localRepository, List.of(plain, nativeBearing), List.of(), Map.of());

        // the offending coordinate is refused; every other project's dependency still activates -
        // one library shipping a native resource must not block the whole instance
        assertThat(outcome.swapped()).isTrue();
        assertThat(outcome.error()).isNull();
        assertThat(outcome.rejected()).containsOnlyKeys("com.example:sqlite:1.0.0");
        assertThat(outcome.rejected()
                          .get("com.example:sqlite:1.0.0")).contains("platform");
        assertThat(loaderHolder.current()
                               .jars()).containsExactly(plain);
    }

    @Test
    void anUpgradeKeepsWhatTheModuleDidNotPublish() throws IOException {
        Path version1 = artifact("com.example", "mod", "1.0.0", Map.of("META-INF/dirigible/mod/greeting.txt", "v1"));
        Path version2 = artifact("com.example", "mod", "1.1.0", Map.of("META-INF/dirigible/mod/greeting.txt", "v2"));

        synchronizer.swap(localRepository, List.of(version1), List.of(), Map.of());
        // a customization published into the module's project folder, by a developer or another tool
        repository.createResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/mod/mine.txt", "mine".getBytes(StandardCharsets.UTF_8));

        synchronizer.swap(localRepository, List.of(version2), List.of(), Map.of());

        assertThat(content("/mod/greeting.txt")).isEqualTo("v2");
        assertThat(content("/mod/mine.txt")).isEqualTo("mine");
    }

    @Test
    void aRemovedModuleTakesOnlyItsOwnPayload() throws IOException {
        Path mod = artifact("com.example", "mod", "1.0.0", Map.of("META-INF/dirigible/mod/greeting.txt", "v1"));
        synchronizer.swap(localRepository, List.of(mod), List.of(), Map.of());
        repository.createResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/mod/mine.txt", "mine".getBytes(StandardCharsets.UTF_8));

        synchronizer.swap(localRepository, List.of(), List.of(), Map.of());

        assertThat(repository.hasResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/mod/greeting.txt")).isFalse();
        assertThat(content("/mod/mine.txt")).isEqualTo("mine");
    }

    @Test
    void aSkippedJarNeverRemovesAProjectOfTheSameName() throws IOException {
        Path skipped = artifact("com.example", "skipped", "1.0.0",
                Map.of("META-INF/dirigible/.skip", "", "META-INF/dirigible/mine/hidden.txt", "never laid down"));
        synchronizer.swap(localRepository, List.of(skipped), List.of(), Map.of());
        // a developer's own project that happens to carry the same name as the jar's skipped payload
        repository.createResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/mine/file.txt", "mine".getBytes(StandardCharsets.UTF_8));

        synchronizer.swap(localRepository, List.of(), List.of(), Map.of());

        assertThat(content("/mine/file.txt")).isEqualTo("mine");
    }

    private String content(String registryRelativePath) {
        return new String(repository.getResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + registryRelativePath)
                                    .getContent(),
                StandardCharsets.UTF_8);
    }

    private Path artifact(String groupId, String artifactId, String version, Map<String, String> entries) throws IOException {
        Path directory = Files.createDirectories(localRepository.resolve(groupId.replace('.', '/'))
                                                                .resolve(artifactId)
                                                                .resolve(version));
        Path jar = directory.resolve(artifactId + "-" + version + ".jar");
        try (OutputStream file = Files.newOutputStream(jar); JarOutputStream out = new JarOutputStream(file)) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                out.putNextEntry(new JarEntry(entry.getKey()));
                out.write(entry.getValue()
                               .getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        return jar;
    }

}
