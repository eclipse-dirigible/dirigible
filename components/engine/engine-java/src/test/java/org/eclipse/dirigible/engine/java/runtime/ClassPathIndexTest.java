/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Resolution of the runtime image's drop-in modules location ({@code loader.path} /
 * {@code LOADER_PATH}) to {@code javac} classpath entries.
 */
class ClassPathIndexTest {

    @TempDir
    Path tempDir;

    @Test
    void a_directory_contributes_its_jars_in_a_stable_order() throws IOException {
        Path modules = Files.createDirectory(tempDir.resolve("modules"));
        Path second = Files.createFile(modules.resolve("b-module.jar"));
        Path first = Files.createFile(modules.resolve("a-module.jar"));
        Files.createFile(modules.resolve("notes.txt"));

        assertEquals(List.of(first, second), ClassPathIndex.loaderPathEntries(modules.toString()));
    }

    @Test
    void a_jar_contributes_itself_and_missing_segments_are_skipped() throws IOException {
        Path jar = Files.createFile(tempDir.resolve("module.jar"));

        List<Path> entries = ClassPathIndex.loaderPathEntries(jar + " , " + tempDir.resolve("absent") + ",");

        assertEquals(List.of(jar), entries);
    }

    @Test
    void an_empty_or_missing_loader_path_contributes_nothing() throws IOException {
        Files.createDirectory(tempDir.resolve("empty"));

        assertTrue(ClassPathIndex.loaderPathEntries(null)
                                 .isEmpty());
        assertTrue(ClassPathIndex.loaderPathEntries("   ")
                                 .isEmpty());
        assertTrue(ClassPathIndex.loaderPathEntries(tempDir.resolve("empty")
                                                           .toString())
                                 .isEmpty());
    }

}
