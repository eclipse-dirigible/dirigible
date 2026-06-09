/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.nativeapps.process;

import java.io.File;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Live state for a spawned local native-app process. Not persisted — recreated each JVM lifetime.
 *
 * <p>
 * Carries a bounded ring buffer of the most recent stderr lines from the child so the platform can
 * inline them into failure messages when the process exits prematurely (see
 * {@link NativeAppProcessManager#startAndAwaitReady}). The buffer is populated by
 * {@link ProcessLogPump} as the child streams output; reads happen from the awaitReady thread.
 */
public final class RuntimeState {

    private static final int RECENT_STDERR_LINES = 30;

    private final Process process;
    private final int port;
    private final Instant startedAt;
    private final File workingDir;
    private final Deque<String> recentStderr = new ArrayDeque<>(RECENT_STDERR_LINES);

    public RuntimeState(Process process, int port, Instant startedAt, File workingDir) {
        this.process = process;
        this.port = port;
        this.startedAt = startedAt;
        this.workingDir = workingDir;
    }

    public Process getProcess() {
        return process;
    }

    public int getPort() {
        return port;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public File getWorkingDir() {
        return workingDir;
    }

    public boolean isAlive() {
        return process != null && process.isAlive();
    }

    void recordStderrLine(String line) {
        synchronized (recentStderr) {
            if (recentStderr.size() >= RECENT_STDERR_LINES) {
                recentStderr.removeFirst();
            }
            recentStderr.addLast(line);
        }
    }

    /** Snapshot of the last {@value #RECENT_STDERR_LINES} stderr lines, newline-joined. */
    String snapshotRecentStderr() {
        synchronized (recentStderr) {
            return String.join("\n", recentStderr);
        }
    }
}
