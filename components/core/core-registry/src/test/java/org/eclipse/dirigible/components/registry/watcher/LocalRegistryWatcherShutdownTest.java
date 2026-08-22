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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.dirigible.repository.api.IRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Shutting the watcher down must stop the watch loop, leave no thread of ours behind, and - above
 * all - come back.
 *
 * <p>
 * The watch loop blocks in {@code WatchService.take()}, so shutdown stops the loop before it closes
 * the service. That ordering is necessary but not sufficient: on macOS the JDK has no native
 * file-event source and falls back to {@code sun.nio.fs.PollingWatchService}, which deadlocks
 * against its OWN poller - {@code implClose()} takes the key map and then each key, while
 * {@code poll()} (synchronized on the key) takes the key and then the map when a vanished directory
 * sends it into {@code cancel()}. No ordering on our side can retire that poller, so whoever calls
 * {@code close()} can be parked forever and Spring's singleton teardown hangs, which took out every
 * {@code @DirtiesContext} integration test on a developer's Mac. Hence the close is offloaded to a
 * daemon thread nobody waits on for long.
 *
 * <p>
 * <b>What these tests do and do not prove.</b> The deadlock needs the poller to be inside a poll at
 * the moment of {@code close()} - a race a test cannot force (verified: the buggy versions still
 * pass a pure timeout assertion). So the two halves are asserted separately, and both hold
 * deterministically: {@link #destroyReturnsWhileTheWatchLoopIsBlockedInTake} covers the ordering's
 * observable effects against a real watch service, and
 * {@link #aCloseThatNeverReturnsDoesNotHoldUpTeardown} covers the close itself by handing shutdown
 * a close that behaves the way the wedged JDK one does. The deadlock was diagnosed from a thread
 * dump (lock-order inversion between {@code main} and {@code FileSystemWatcher}) and from the JDK
 * sources, not from these tests.
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
        // waits for the thread, so nothing of ours is left inside the service when it is closed.
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

    /**
     * The half no ordering can fix: a {@code close()} that never returns must not take teardown with
     * it. This is the deadlocked JDK close, stood in for by one that parks forever - the real one
     * cannot be provoked on demand.
     */
    @Test
    void aCloseThatNeverReturnsDoesNotHoldUpTeardown() throws Exception {
        ParkingWatchService service = new ParkingWatchService();

        boolean closed = assertTimeoutPreemptively(SHUTDOWN_BOUND, () -> LocalRegistryWatcher.closeOffThread(service, 1),
                "shutdown must walk away from a close that does not come back, not wait for it - waiting is the deadlock");

        assertFalse(closed, "a close that never finished must be reported as unfinished, not as a clean close");
        // Walking away is not skipping: the close is genuinely attempted, on a thread of its own.
        assertTrue(service.closeEntered.await(SHUTDOWN_BOUND.toSeconds(), TimeUnit.SECONDS), "the close must have been attempted");
        // ...and that thread must be a daemon, or the wedged close would keep the JVM from exiting -
        // trading a hung teardown for a hung process is no fix at all.
        assertTrue(service.closedOnDaemonThread.get(), "the close must run on a daemon thread so a wedged close cannot outlive the JVM");
    }

    /** Offloading the close must not mean losing it: a healthy service is still closed, and awaited. */
    @Test
    void aCloseThatCompletesIsAwaitedAndReported() throws Exception {
        WatchService service = FileSystems.getDefault()
                                          .newWatchService();

        assertTrue(LocalRegistryWatcher.closeOffThread(service, SHUTDOWN_BOUND.toSeconds()),
                "a close that completes must be reported as completed");
        assertThrows(ClosedWatchServiceException.class, service::poll, "the watch service must really have been closed");
    }

    /** Wait until the loop is actually inside take(), which is the state shutdown has to survive. */
    private static void awaitWatching(LocalRegistryWatcher watcher) throws InterruptedException {
        long deadline = System.nanoTime() + SETTLE.toNanos();
        while (!watcher.isWatching() && System.nanoTime() < deadline) {
            Thread.sleep(50);
        }
        assertTrue(watcher.isWatching(), "the watch loop should be running before we try to stop it");
    }

    /**
     * A watch service whose {@code close()} parks for good - what {@code PollingWatchService.close()}
     * effectively does once its own poller has the key monitor it wants. Nothing else is used: this
     * never gets registered with a path (a foreign watch service cannot be), it only gets closed.
     */
    private static final class ParkingWatchService implements WatchService {

        private final CountDownLatch closeEntered = new CountDownLatch(1);

        private final AtomicBoolean closedOnDaemonThread = new AtomicBoolean();

        /** Never counted down - the point is that this close has no way out. */
        private final CountDownLatch forever = new CountDownLatch(1);

        @Override
        public void close() {
            closedOnDaemonThread.set(Thread.currentThread()
                                           .isDaemon());
            closeEntered.countDown();
            try {
                forever.await();
            } catch (InterruptedException e) {
                Thread.currentThread()
                      .interrupt();
            }
        }

        @Override
        public WatchKey poll() {
            return null;
        }

        @Override
        public WatchKey poll(long timeout, TimeUnit unit) {
            return null;
        }

        @Override
        public WatchKey take() {
            throw new UnsupportedOperationException("not used: this service is only ever closed");
        }
    }
}
