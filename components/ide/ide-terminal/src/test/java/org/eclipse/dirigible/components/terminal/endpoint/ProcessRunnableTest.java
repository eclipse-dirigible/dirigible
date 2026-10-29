/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.terminal.endpoint;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Verifies that a running ttyd never keeps the JVM alive: the stream reader threads are daemon
 * threads and the process is destroyed on demand.
 */
class ProcessRunnableTest {

    private static final String[] READER_THREAD_NAMES = {"dirigible-ttyd-stdout-reader", "dirigible-ttyd-stderr-reader"};

    @Test
    void destroy_is_safe_before_the_process_has_started() {
        ProcessRunnable runnable = new ProcessRunnable("noop", Map.of());

        assertDoesNotThrow(runnable::destroy);
        assertNull(runnable.getProcess(), "no process should have been started");
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void reader_threads_are_daemon_and_the_process_is_destroyed() throws Exception {
        ProcessRunnable runnable = new ProcessRunnable("sleep 30", Map.of());
        try {
            runnable.run();

            Process process = awaitStartedProcess(runnable);
            assertNotNull(process, "the process should have been started");
            assertTrue(process.isAlive(), "the process should be alive");

            for (String name : READER_THREAD_NAMES) {
                Thread reader = awaitThread(name);
                assertNotNull(reader, "reader thread [" + name + "] should have been started");
                assertTrue(reader.isDaemon(), "reader thread [" + name + "] must be a daemon thread");
            }

            runnable.destroy();
            assertTrue(process.waitFor(10, TimeUnit.SECONDS), "destroy() should terminate the process");
            assertFalse(process.isAlive(), "the process should no longer be alive");
        } finally {
            Process process = runnable.getProcess();
            if (process != null) {
                process.destroyForcibly();
            }
        }
    }

    private static Process awaitStartedProcess(ProcessRunnable runnable) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(10)
                                                    .toNanos();
        while (System.nanoTime() < deadline) {
            Process process = runnable.getProcess();
            if (process != null) {
                return process;
            }
            Thread.sleep(50);
        }
        return runnable.getProcess();
    }

    private static Thread awaitThread(String name) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(10)
                                                    .toNanos();
        while (System.nanoTime() < deadline) {
            Optional<Thread> thread = findThread(name);
            if (thread.isPresent()) {
                return thread.get();
            }
            Thread.sleep(50);
        }
        return findThread(name).orElse(null);
    }

    private static Optional<Thread> findThread(String name) {
        return Thread.getAllStackTraces()
                     .keySet()
                     .stream()
                     .filter(thread -> name.equals(thread.getName()))
                     .findFirst();
    }
}
