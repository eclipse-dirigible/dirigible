/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.initializers.classpath;

import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.local.LocalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The per-jar expand / remove round-trip of {@link ClasspathExpander} - the runtime counterpart of
 * the startup sweep - including the {@code .skip} marker.
 */
class ClasspathExpanderTest {

    @TempDir
    Path tempDir;

    private IRepository repository;
    private ClasspathExpander expander;

    @BeforeEach
    void setUp() {
        repository = new LocalRepository(tempDir.resolve("repository")
                                                .toString(),
                true);
        expander = new ClasspathExpander(repository);
    }

    @Test
    void expands_a_single_jar_into_the_registry_and_removes_its_project() throws IOException {
        Path jar = jarWithEntries("module.jar",
                Map.of("META-INF/dirigible/my-module/hello.txt", "payload", "META-INF/dirigible/my-module/sub/nested.txt", "nested"));

        expander.expand(jar);

        assertThat(resourceContent("/my-module/hello.txt")).isEqualTo("payload");
        assertThat(resourceContent("/my-module/sub/nested.txt")).isEqualTo("nested");

        expander.remove("my-module");

        assertThat(repository.hasCollection(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/my-module")).isFalse();
        // removing an absent project is a no-op, not an error
        expander.remove("my-module");
    }

    @Test
    void honors_the_skip_marker() throws IOException {
        Path jar = jarWithEntries("skipped.jar",
                Map.of("META-INF/dirigible/.skip", "", "META-INF/dirigible/skipped-module/hello.txt", "payload"));

        expander.expand(jar);

        assertThat(repository.hasCollection(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/skipped-module")).isFalse();
    }

    private String resourceContent(String registryRelativePath) {
        return new String(repository.getResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + registryRelativePath)
                                    .getContent(),
                StandardCharsets.UTF_8);
    }

    private Path jarWithEntries(String name, Map<String, String> entries) throws IOException {
        Path jar = tempDir.resolve(name);
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
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
