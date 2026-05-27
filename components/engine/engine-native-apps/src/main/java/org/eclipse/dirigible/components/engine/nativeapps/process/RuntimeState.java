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

import java.time.Instant;

/**
 * Live state for a spawned local native-app process. Not persisted — recreated each JVM lifetime.
 */
public final class RuntimeState {

    private final Process process;
    private final int port;
    private final Instant startedAt;

    public RuntimeState(Process process, int port, Instant startedAt) {
        this.process = process;
        this.port = port;
        this.startedAt = startedAt;
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

    public boolean isAlive() {
        return process != null && process.isAlive();
    }
}
