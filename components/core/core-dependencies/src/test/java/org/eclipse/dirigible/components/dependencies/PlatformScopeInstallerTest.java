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
import org.eclipse.dirigible.components.dependencies.PlatformScopeInstaller.PlatformArtifactState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * The append-only contract of the platform tier: each artifact is appended once, a version change
 * is pending-restart with a WARN naming both versions, package-root overlap warns, and without
 * instrumentation everything is pending-restart.
 */
class PlatformScopeInstallerTest {

    @TempDir
    Path tempDir;

    private Path localRepository;
    private Instrumentation instrumentation;
    private PlatformScopeInstaller installer;

    @BeforeEach
    void setUp() throws IOException {
        localRepository = Files.createDirectories(tempDir.resolve("repo"));
        instrumentation = mock(Instrumentation.class);
        installer = new PlatformScopeInstaller(() -> instrumentation);
    }

    @Test
    void appends_each_artifact_once() throws IOException {
        Path jarA = artifact("com.example", "lib-a", "1.0.0", "com/example/liba/Klass.class");
        Path jarB = artifact("com.example", "lib-b", "1.0.0", "com/example/libb/Klass.class");

        List<PlatformArtifactState> first = installer.install(localRepository, List.of(jarA, jarB));
        List<PlatformArtifactState> second = installer.install(localRepository, List.of(jarA, jarB));

        assertThat(first).extracting(PlatformArtifactState::status)
                         .containsExactly("active", "active");
        assertThat(second).extracting(PlatformArtifactState::status)
                          .containsExactly("active", "active");
        // appended exactly once per artifact - re-installs are bookkeeping no-ops
        verify(instrumentation, times(2)).appendToSystemClassLoaderSearch(any(JarFile.class));
    }

    @Test
    void a_version_change_is_pending_restart_and_warns_with_both_versions() throws IOException {
        Path oldVersion = artifact("com.example", "lib-a", "1.0.0", "com/example/liba/Klass.class");
        Path newVersion = artifact("com.example", "lib-a", "2.0.0", "com/example/liba/Klass.class");
        installer.install(localRepository, List.of(oldVersion));

        try (LogCaptor logCaptor = LogCaptor.forClass(PlatformScopeInstaller.class)) {
            List<PlatformArtifactState> states = installer.install(localRepository, List.of(newVersion));

            assertThat(states).hasSize(1);
            assertThat(states.get(0)
                             .status()).isEqualTo("pending-restart");
            assertThat(states.get(0)
                             .message()).contains("1.0.0")
                                        .contains("2.0.0");
            assertThat(logCaptor.getWarnLogs()).anySatisfy(message -> assertThat(message).contains("1.0.0")
                                                                                         .contains("2.0.0")
                                                                                         .contains("append-only"));
        }
        // the new version was never appended
        verify(instrumentation, times(1)).appendToSystemClassLoaderSearch(any(JarFile.class));
    }

    @Test
    void overlapping_package_roots_warn() throws IOException {
        Path jarA = artifact("com.example", "lib-a", "1.0.0", "com/example/Shared.class");
        Path jarB = artifact("com.example", "lib-b", "1.0.0", "com/example/Other.class");
        installer.install(localRepository, List.of(jarA));

        try (LogCaptor logCaptor = LogCaptor.forClass(PlatformScopeInstaller.class)) {
            installer.install(localRepository, List.of(jarB));

            assertThat(logCaptor.getWarnLogs()).anySatisfy(message -> assertThat(message).contains("package root")
                                                                                         .contains("com/example"));
        }
    }

    @Test
    void without_instrumentation_everything_is_pending_restart() throws IOException {
        PlatformScopeInstaller agentless = new PlatformScopeInstaller(() -> null);
        Path jar = artifact("com.example", "lib-a", "1.0.0", "com/example/liba/Klass.class");

        List<PlatformArtifactState> states = agentless.install(localRepository, List.of(jar));

        assertThat(states).hasSize(1);
        assertThat(states.get(0)
                         .status()).isEqualTo("pending-restart");
        assertThat(states.get(0)
                         .message()).contains("next launch");
    }

    @Test
    void a_jar_seeded_on_the_launch_classpath_is_active_without_an_append() throws IOException {
        Path seedDir = Files.createDirectories(tempDir.resolve("seed"));
        Files.writeString(seedDir.resolve("com.example-lib-a-1.0.0.jar"), "seed link");
        Path jar = artifact("com.example", "lib-a", "1.0.0", "com/example/liba/Klass.class");

        String previous = System.getProperty("loader.path");
        System.setProperty("loader.path", seedDir.toString());
        try {
            PlatformScopeInstaller seeded = new PlatformScopeInstaller(() -> instrumentation);
            List<PlatformArtifactState> states = seeded.install(localRepository, List.of(jar));

            assertThat(states).hasSize(1);
            assertThat(states.get(0)
                             .status()).isEqualTo("active");
            assertThat(states.get(0)
                             .message()).contains("launch classpath");
            verify(instrumentation, times(0)).appendToSystemClassLoaderSearch(any(JarFile.class));
        } finally {
            if (previous == null) {
                System.clearProperty("loader.path");
            } else {
                System.setProperty("loader.path", previous);
            }
        }
    }

    /** Lays a fixture jar at its local-repository coordinates with the given class entries. */
    private Path artifact(String groupId, String artifactId, String version, String... classEntries) throws IOException {
        Path directory = Files.createDirectories(localRepository.resolve(groupId.replace('.', '/'))
                                                                .resolve(artifactId)
                                                                .resolve(version));
        Path jar = directory.resolve(artifactId + "-" + version + ".jar");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            for (String entry : classEntries) {
                out.putNextEntry(new JarEntry(entry));
                out.write("bytecode".getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        return jar;
    }

}
