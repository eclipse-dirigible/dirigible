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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.eclipse.dirigible.components.base.registry.RegistryMutationTracker;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizationWatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The external folder is copied onto the registry's file system path behind every mechanism that
 * would otherwise notice - so the copy has to report itself, or nothing schedules the
 * synchronization pass that turns the copied files into runtime state (issue #7192).
 */
class RecursiveFolderWatcherTest {

    /** The initial copy is synchronous once the watcher's thread gets going. */
    private static final long SYNC_TIMEOUT_MILLIS = 30_000;

    /** A change picked up by the watch service: up to ~10s where the JDK falls back to polling. */
    private static final long WATCH_TIMEOUT_MILLIS = 60_000;

    @TempDir
    Path source;

    @TempDir
    Path target;

    private final RegistryMutationTracker mutationTracker = new RegistryMutationTracker();

    private final SynchronizationWatcher synchronizationWatcher = mock(SynchronizationWatcher.class);

    private RecursiveFolderWatcher watcher;

    @AfterEach
    void stopWatching() {
        if (watcher != null) {
            // the watch service holds handles on the temp folders - Windows refuses to delete those
            watcher.destroy();
        }
    }

    @Test
    void the_initial_copy_reports_itself_as_a_registry_mutation_and_schedules_a_pass() throws IOException {
        Files.createDirectories(source.resolve("project")
                                      .resolve("deep"));
        Files.writeString(source.resolve("project")
                                .resolve("deep")
                                .resolve("artefact.txt"),
                "content");

        startWatching();

        verify(synchronizationWatcher, timeout(SYNC_TIMEOUT_MILLIS)).force();
        assertTrue(Files.exists(target.resolve("project")
                                      .resolve("deep")
                                      .resolve("artefact.txt")));
        // the window a pass must not clean up in is opened and closed, not left hanging
        assertTrue(mutationTracker.completedMutations() > 0);
        assertFalse(mutationTracker.isMutating());
    }

    @Test
    void a_file_added_deep_in_the_tree_afterwards_schedules_another_pass() throws IOException {
        Files.createDirectories(source.resolve("project")
                                      .resolve("deep"));
        Files.writeString(source.resolve("project")
                                .resolve("deep")
                                .resolve("first.txt"),
                "content");
        startWatching();
        verify(synchronizationWatcher, timeout(SYNC_TIMEOUT_MILLIS)).force();
        // the initial copy runs BEFORE the directories are registered - a change made in between is
        // legitimately missed, so wait for the mirroring to be live
        awaitWatching();

        Files.writeString(source.resolve("project")
                                .resolve("deep")
                                .resolve("second.txt"),
                "more content");

        verify(synchronizationWatcher, timeout(WATCH_TIMEOUT_MILLIS).atLeast(2)).force();
        assertTrue(Files.exists(target.resolve("project")
                                      .resolve("deep")
                                      .resolve("second.txt")));
    }

    private void awaitWatching() {
        long deadline = System.currentTimeMillis() + SYNC_TIMEOUT_MILLIS;
        while (!watcher.isWatching()) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("The watcher did not start watching within " + SYNC_TIMEOUT_MILLIS + " ms");
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread()
                      .interrupt();
                throw new AssertionError("Interrupted while waiting for the watcher to start", e);
            }
        }
    }

    private void startWatching() {
        watcher = new RecursiveFolderWatcher(mutationTracker, synchronizationWatcher);
        watcher.initialize(source.toString(), target.toString(), false, Set.of());
    }

}
