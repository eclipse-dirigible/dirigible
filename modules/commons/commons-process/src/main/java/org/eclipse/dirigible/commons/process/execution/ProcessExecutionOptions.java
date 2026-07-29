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

public class ProcessExecutionOptions {
    private String workingDirectory;
    private Long timeoutMillis;

    public void setWorkingDirectory(@Nullable String workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    @Nullable
    public String getWorkingDirectory() {
        return workingDirectory;
    }

    /**
     * Sets the maximum time the process is allowed to run. A process that is still running when the
     * timeout elapses is destroyed, and the execution fails with a {@link ProcessExecutionException}
     * instead of returning an exit code.
     *
     * @param timeoutMillis the timeout in milliseconds, or {@code null} to let the process run for as
     *        long as it needs
     * @throws IllegalArgumentException if the timeout is not a positive number of milliseconds
     */
    public void setTimeoutMillis(@Nullable Long timeoutMillis) {
        if (timeoutMillis != null && timeoutMillis <= 0) {
            throw new IllegalArgumentException("Process timeout must be a positive number of milliseconds but was: " + timeoutMillis);
        }
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Gets the maximum time the process is allowed to run.
     *
     * @return the timeout in milliseconds, or {@code null} when the process is not time-limited
     */
    @Nullable
    public Long getTimeoutMillis() {
        return timeoutMillis;
    }
}
