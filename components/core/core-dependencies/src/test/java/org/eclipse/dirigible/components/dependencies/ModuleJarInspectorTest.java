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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * A module jar's registry payload names become repository path segments, so the inspector must
 * refuse any name that could escape the registry: on removal the name is concatenated into
 * {@code /registry/public/<name>}, where a {@code ..} would resolve to the registry root and its
 * removal would delete everything.
 */
class ModuleJarInspectorTest {

    @TempDir
    Path tempDir;

    @Test
    void aPlainProjectNameIsAccepted() throws IOException {
        Path jar = jarWithEntries("META-INF/dirigible/my-project/file.txt", "META-INF/dirigible/my-project/sub/other.txt");

        ModuleJarInspector.Inspection inspection = ModuleJarInspector.inspect(jar);

        assertEquals(Set.of("my-project"), inspection.projects(), "the payload's project should be reported once");
    }

    @Test
    void aParentTraversalProjectNameIsRefused() throws IOException {
        Path jar = jarWithEntries("META-INF/dirigible/../x/file.txt");

        IOException refused = assertThrows(IOException.class, () -> ModuleJarInspector.inspect(jar),
                "a '..' project name resolves to the registry root on removal and must never pass inspection");
        assertTrue(refused.getMessage()
                          .contains("invalid registry project name"),
                "the refusal should name the problem: " + refused.getMessage());
    }

    @Test
    void aBackslashBearingProjectNameIsRefused() throws IOException {
        Path jar = jarWithEntries("META-INF/dirigible/..\\evil/file.txt");

        assertThrows(IOException.class, () -> ModuleJarInspector.inspect(jar),
                "a backslash-bearing name is a path on Windows and must never pass inspection");
    }

    @Test
    void theRegistryEntriesAreReportedPerFile() throws IOException {
        Path jar = jarWithEntries("META-INF/dirigible/my-project/file.txt", "META-INF/dirigible/my-project/sub/other.txt");

        ModuleJarInspector.Inspection inspection = ModuleJarInspector.inspect(jar);

        // the removal granularity: only what the jar laid down leaves again, never the whole project
        assertEquals(Set.of("/my-project/file.txt", "/my-project/sub/other.txt"), inspection.registryEntries(),
                "every payload entry should be reported");
    }

    @Test
    void aSkippedJarCarriesNoProjects() throws IOException {
        Path jar = jarWithEntries("META-INF/dirigible/.skip", "META-INF/dirigible/my-project/file.txt");

        ModuleJarInspector.Inspection inspection = ModuleJarInspector.inspect(jar);

        // the expansion honors .skip, so nothing of this jar is ever in the registry - reporting the
        // project would make its removal delete a collection the jar never created (possibly a
        // developer's own project of that name)
        assertTrue(inspection.projects()
                             .isEmpty(),
                "a .skip jar lays nothing down, so it carries no projects");
        assertTrue(inspection.registryEntries()
                             .isEmpty(),
                "a .skip jar lays nothing down, so it has no registry entries");
    }

    @Test
    void versionedAndAppleNativeLibrariesAreDetected() throws IOException {
        Path jar = jarWithEntries("lib/libfoo.so.1", "lib/libbar.jnilib", "lib/baz.DLL");

        ModuleJarInspector.Inspection inspection = ModuleJarInspector.inspect(jar);

        assertEquals(3, inspection.nativeLibraries()
                                  .size(),
                "the versioned, the Apple and the upper-case Windows form are all native libraries: " + inspection.nativeLibraries());
    }

    private Path jarWithEntries(String... entryNames) throws IOException {
        Path jar = tempDir.resolve("module.jar");
        try (OutputStream file = Files.newOutputStream(jar); JarOutputStream out = new JarOutputStream(file)) {
            for (String entryName : entryNames) {
                out.putNextEntry(new JarEntry(entryName));
                out.write("content".getBytes(StandardCharsets.UTF_8));
                out.closeEntry();
            }
        }
        return jar;
    }
}
