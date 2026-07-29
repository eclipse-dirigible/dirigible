/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.commons.process.execution;

import jakarta.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteResultHandler;
import org.apache.commons.exec.ExecuteWatchdog;

/**
 * The Class ProcessExecutionFuture.
 */
public class ProcessExecutionFuture extends CompletableFuture<Integer> implements ExecuteResultHandler {

    /** The watchdog enforcing the process timeout, or null when the process is not time-limited. */
    private final ExecuteWatchdog watchdog;

    /**
     * Instantiates a new process execution future for a process that is not time-limited.
     */
    public ProcessExecutionFuture() {
        this(null);
    }

    /**
     * Instantiates a new process execution future.
     *
     * @param watchdog the watchdog enforcing the process timeout, or {@code null} when the process is
     *        not time-limited
     */
    public ProcessExecutionFuture(@Nullable ExecuteWatchdog watchdog) {
        this.watchdog = watchdog;
    }

    /**
     * On process complete.
     *
     * @param i the i
     */
    @Override
    public void onProcessComplete(int i) {
        complete(i);
    }

    /**
     * On process failed. A process destroyed by the watchdog exceeded its configured timeout and never
     * produced a meaningful exit code, so the failure is propagated instead of being reported as an
     * ordinary non-zero exit.
     *
     * @param e the e
     */
    @Override
    public void onProcessFailed(ExecuteException e) {
        if (watchdog != null && watchdog.killedProcess()) {
            completeExceptionally(new ProcessExecutionException("The process was destroyed after exceeding its configured timeout", e));
        } else {
            complete(e.getExitValue());
        }
    }
}
