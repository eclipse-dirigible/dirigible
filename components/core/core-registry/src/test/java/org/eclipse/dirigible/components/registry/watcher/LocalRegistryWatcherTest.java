/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.registry.watcher;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizationWatcher;
import org.eclipse.dirigible.repository.api.IRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The registry watcher that everything depends on registers the registry root and nothing below it,
 * so a write several folders deep schedules no synchronization pass at all - the shape of #7192.
 * This watcher is the one that sees deep writes, and its whole job is to report them.
 */
class LocalRegistryWatcherTest {

    /** A change picked up by the watch service: up to ~10s where the JDK falls back to polling. */
    private static final long WATCH_TIMEOUT_MILLIS = 60_000;

    /** How long to wait for the watch loop to be up before changing anything under it. */
    private static final long START_TIMEOUT_MILLIS = 30_000;

    private final SynchronizationWatcher synchronizationWatcher = mock(SynchronizationWatcher.class);

    private LocalRegistryWatcher watcher;

    @AfterEach
    void stopWatching() {
        Configuration.remove("DIRIGIBLE_REGISTRY_LOCAL_IGNORED_FOLDERS");
        if (watcher != null) {
            // the watch service holds handles on the temp folders - Windows refuses to delete those
            watcher.destroy();
        }
    }

    @Test
    void aFileWrittenDeepInTheRegistrySchedulesASynchronizationPass(@TempDir Path root) throws IOException {
        Path registry = startWatching(root, "project/deep");

        Files.writeString(registry.resolve("project")
                                  .resolve("deep")
                                  .resolve("artefact.txt"),
                "content");

        verify(synchronizationWatcher, timeout(WATCH_TIMEOUT_MILLIS)).force();
    }

    /**
     * A folder is registered as it appears, or the project a publish or a copy drops in would be the
     * one thing the watcher never watches.
     */
    @Test
    void aFolderCreatedAfterStartupIsWatchedTooAndItsContentSchedulesAPass(@TempDir Path root) throws IOException {
        Path registry = startWatching(root, "existing");

        Path fresh = Files.createDirectories(registry.resolve("fresh-project")
                                                     .resolve("deep"));
        verify(synchronizationWatcher, timeout(WATCH_TIMEOUT_MILLIS)).force();

        Files.writeString(fresh.resolve("artefact.txt"), "content");
        verify(synchronizationWatcher, timeout(WATCH_TIMEOUT_MILLIS).atLeast(2)).force();
    }

    /** A deletion leaves runtime state behind just as a creation leaves it missing. */
    @Test
    void aDeletedArtefactSchedulesASynchronizationPass(@TempDir Path root) throws IOException {
        Path registry = startWatching(root, "project");
        Path artefact = registry.resolve("project")
                                .resolve("artefact.txt");
        Files.writeString(artefact, "content");
        verify(synchronizationWatcher, timeout(WATCH_TIMEOUT_MILLIS)).force();

        Files.delete(artefact);

        verify(synchronizationWatcher, timeout(WATCH_TIMEOUT_MILLIS).atLeast(2)).force();
    }

    /** An ignored top-level folder is neither watched nor reported - that is what the key is for. */
    @Test
    void aChangeInAnIgnoredFolderSchedulesNothing(@TempDir Path root) throws IOException, InterruptedException {
        Configuration.set("DIRIGIBLE_REGISTRY_LOCAL_IGNORED_FOLDERS", "ignored");
        Path registry = startWatching(root, "ignored/deep");

        Files.writeString(registry.resolve("ignored")
                                  .resolve("deep")
                                  .resolve("artefact.txt"),
                "content");
        // nothing to wait for, so give the watch service the time it would have needed to report it
        Thread.sleep(5_000);

        verifyNoInteractions(synchronizationWatcher);
    }

    private Path startWatching(Path root, String existingFolder) throws IOException {
        Path registry = root.resolve("registry")
                            .resolve("public");
        Files.createDirectories(registry.resolve(existingFolder));

        IRepository repository = mock(IRepository.class);
        when(repository.getInternalResourcePath(anyString())).thenReturn(registry.toString());

        watcher = new LocalRegistryWatcher(repository, synchronizationWatcher);
        watcher.initialize();
        awaitWatching();
        return registry;
    }

    private void awaitWatching() {
        long deadline = System.currentTimeMillis() + START_TIMEOUT_MILLIS;
        while (!watcher.isWatching()) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("The watcher did not start watching within " + START_TIMEOUT_MILLIS + " ms");
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread()
                      .interrupt();
                throw new AssertionError("Interrupted while waiting for the watcher to start", e);
            }
        }
        assertTrue(watcher.isWatching());
    }
}
