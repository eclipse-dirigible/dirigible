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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizationWatcher;
import org.eclipse.dirigible.repository.api.IRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * #7367: {@code registerAll} used to register the folder it was called with UNCONDITIONALLY, before
 * the {@code isIgnored} guard that only protects the folder's own children. Harmless at boot (the
 * root is registered once, and an already-existing ignored folder is only ever reached through the
 * guarded walk), but the watch loop calls {@code registerAll} again for every directory created at
 * runtime - so an ignored top-level folder created while the process is running was still watched,
 * and every write directly inside it kept forcing a synchronization pass.
 */
class LocalRegistryWatcherIgnoredFolderTest {

    private static final Duration SETTLE = Duration.ofSeconds(10);

    @Test
    void anIgnoredTopLevelFolderCreatedAtRuntimeIsNeverRegistered(@TempDir Path root) throws Exception {
        Path registryPublic = Files.createDirectories(root.resolve("registry")
                                                          .resolve("public"));
        IRepository repository = mock(IRepository.class);
        when(repository.getInternalResourcePath(anyString())).thenReturn(registryPublic.toString());

        String key = DirigibleConfig.REGISTRY_LOCAL_IGNORED_FOLDERS.getKey();
        Configuration.set(key, "ignored");
        LocalRegistryWatcher watcher = new LocalRegistryWatcher(repository, mock(SynchronizationWatcher.class));
        try {
            watcher.initialize();
            awaitWatching(watcher);

            // What the watch loop does when ENTRY_CREATE reports a brand-new top-level directory -
            // simulated directly (real OS watch events are too slow/OS-dependent to assert on).
            Path ignoredDir = Files.createDirectory(registryPublic.resolve("ignored"));
            invokeRegisterAll(watcher, ignoredDir);

            assertFalse(watchedPaths(watcher).contains(ignoredDir),
                    "an ignored top-level folder created at runtime must never be registered for watching");

            // Regression guard: a NON-ignored runtime folder must still be registered as before.
            Path keptDir = Files.createDirectory(registryPublic.resolve("kept"));
            invokeRegisterAll(watcher, keptDir);
            assertTrue(watchedPaths(watcher).contains(keptDir), "a non-ignored runtime folder must still be watched");
        } finally {
            watcher.destroy();
            Configuration.remove(key);
        }
    }

    /**
     * Wait until the loop is actually inside take() - the state a directory creation reacts against.
     */
    private static void awaitWatching(LocalRegistryWatcher watcher) throws InterruptedException {
        long deadline = System.nanoTime() + SETTLE.toNanos();
        while (!watcher.isWatching() && System.nanoTime() < deadline) {
            Thread.sleep(50);
        }
        assertTrue(watcher.isWatching(), "the watch loop should be running before we try to register a new directory");
    }

    private static void invokeRegisterAll(LocalRegistryWatcher watcher, Path path) throws Exception {
        Method registerAll = LocalRegistryWatcher.class.getDeclaredMethod("registerAll", Path.class);
        registerAll.setAccessible(true);
        registerAll.invoke(watcher, path);
    }

    @SuppressWarnings("unchecked")
    private static List<Path> watchedPaths(LocalRegistryWatcher watcher) throws Exception {
        Field field = LocalRegistryWatcher.class.getDeclaredField("keyToPathMap");
        field.setAccessible(true);
        Map<WatchKey, Path> keyToPathMap = (Map<WatchKey, Path>) field.get(watcher);
        return List.copyOf(keyToPathMap.values());
    }
}
