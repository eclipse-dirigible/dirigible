/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.typescript.transpilation;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Guards the registry folder creation against the race that used to abort application startup.
 * <p>
 * {@code Files.createDirectories} is not atomic: it calls {@code mkdir}, and when that reports the
 * directory already exists it re-checks with {@code Files.isDirectory} - a check that returns
 * {@code false} on any I/O error, including the path being removed in between. Anything else
 * touching the registry tree (the registry watcher walking it, a test harness wiping it) could
 * therefore turn "the folder is already there" into a fatal {@link FileAlreadyExistsException}, and
 * because the creation runs from the {@code ApplicationReadyEvent} listener it failed the whole
 * context.
 */
class TscWatcherServiceRegistryFolderTest {

    @Test
    void createsTheFolderWhenItIsMissing(@TempDir Path root) throws IOException {
        Path registry = root.resolve("repository/root/registry/public");

        TscWatcherService.createRegistryFolder(registry);

        assertTrue(Files.isDirectory(registry), "the registry folder and its parents must be created");
    }

    @Test
    void isIdempotentWhenTheFolderAlreadyExists(@TempDir Path root) throws IOException {
        Path registry = root.resolve("registry/public");
        Files.createDirectories(registry);

        TscWatcherService.createRegistryFolder(registry);
        TscWatcherService.createRegistryFolder(registry);

        assertTrue(Files.isDirectory(registry), "an existing folder must be accepted, not reported as a conflict");
    }

    /**
     * The retry must not paper over a genuinely unusable path. A regular file where the folder belongs
     * fails every attempt, so it is still reported rather than silently swallowed.
     *
     * @param root the temporary directory
     * @throws IOException if the fixture cannot be written
     */
    @Test
    void stillFailsWhenAFileOccupiesThePath(@TempDir Path root) throws IOException {
        Path registry = root.resolve("registry/public");
        Files.createDirectories(registry.getParent());
        Files.writeString(registry, "not a directory");

        assertThrows(FileAlreadyExistsException.class, () -> TscWatcherService.createRegistryFolder(registry));
    }
}
