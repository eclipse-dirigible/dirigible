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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.eclipse.dirigible.repository.api.IRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Shutting the watcher down must stop the watch loop and leave no thread behind.
 *
 * <p>
 * The watch loop blocks in {@code WatchService.take()}. Closing the service from another thread
 * while it sits there deadlocks on macOS, where the JDK has no native file-event source and falls
 * back to {@code sun.nio.fs.PollingWatchService}: its {@code close()} locks each registered key
 * while holding the key map, while the polling thread locks a key first and then the map. Spring's
 * singleton teardown then hangs for good, which took out {@code @DirtiesContext} integration tests
 * on a developer's Mac. The fix is the ORDER: stop the loop, then close the service.
 *
 * <p>
 * <b>What this test does and does not prove.</b> The deadlock needs the polling thread to be inside
 * a poll at the moment of {@code close()} - a race a unit test cannot force (verified: the old
 * order still passes a pure timeout assertion). So this asserts the ORDERING'S OBSERVABLE EFFECTS,
 * which hold deterministically: {@code destroy()} returns, the watch loop is gone afterwards (not
 * merely signalled), it is idempotent, and the watcher can be initialized again. A hang, a
 * surviving watch thread, or a wedged second call all fail here. The deadlock itself was diagnosed
 * from a thread dump (lock-order inversion between {@code main} and {@code FileSystemWatcher}), not
 * from this test.
 */
class LocalRegistryWatcherShutdownTest {

    /** How long to wait for the loop to finish the initial sync and enter take(). */
    private static final Duration SETTLE = Duration.ofSeconds(10);

    /**
     * Generous next to destroy()'s own 5s await - a deadlock never returns, so any bound catches it.
     */
    private static final Duration SHUTDOWN_BOUND = Duration.ofSeconds(30);

    @Test
    void destroyReturnsWhileTheWatchLoopIsBlockedInTake(@TempDir Path root) throws Exception {
        Path registryPublic = Files.createDirectories(root.resolve("registry")
                                                          .resolve("public"));
        Files.createDirectory(registryPublic.resolve("a-project"));
        IRepository repository = mock(IRepository.class);
        when(repository.getInternalResourcePath(anyString())).thenReturn(registryPublic.toString());

        LocalRegistryWatcher watcher = new LocalRegistryWatcher(repository, List.of());
        watcher.initialize();
        awaitWatching(watcher);

        assertTimeoutPreemptively(SHUTDOWN_BOUND, watcher::destroy,
                "destroy() must stop the watch loop before closing the service - closing it underneath a blocking take()"
                        + " deadlocks against the JDK's polling watch service");
        // The loop must be GONE, not just told to stop: destroy() interrupts the blocking take() and
        // waits for the thread, so nothing is left inside the service when it is closed.
        assertFalse(watcher.isWatching(), "the watch loop must have left before destroy() returned");
        // ...and it must have left because it was ASKED to (the interrupt), never because the service
        // was closed while it was blocked in take(). This is the ordering itself, asserted: the buggy
        // order can only ever produce "closed" here, and it produces it deterministically.
        assertNotEquals("closed", watcher.getLastExitCause(),
                "the watch service was closed underneath a blocking take() - that is the order that deadlocks");

        // Idempotent: Spring destroys a singleton once, but the re-initialize path closes in too.
        assertTimeoutPreemptively(SHUTDOWN_BOUND, watcher::destroy, "a second destroy() must be a no-op, not a hang");

        // ...and the watcher is reusable, i.e. destroy() left clean state behind, not a half-closed
        // service the next initialize() would trip over.
        watcher.initialize();
        awaitWatching(watcher);
        assertTimeoutPreemptively(SHUTDOWN_BOUND, watcher::destroy, "a re-initialized watcher must shut down the same way");
        assertFalse(watcher.isWatching(), "the re-initialized watch loop must also have left");
    }

    /** Wait until the loop is actually inside take(), which is the state shutdown has to survive. */
    private static void awaitWatching(LocalRegistryWatcher watcher) throws InterruptedException {
        long deadline = System.nanoTime() + SETTLE.toNanos();
        while (!watcher.isWatching() && System.nanoTime() < deadline) {
            Thread.sleep(50);
        }
        assertTrue(watcher.isWatching(), "the watch loop should be running before we try to stop it");
    }
}
