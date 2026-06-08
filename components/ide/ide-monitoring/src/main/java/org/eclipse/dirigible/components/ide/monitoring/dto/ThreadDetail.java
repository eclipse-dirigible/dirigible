/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.monitoring.dto;

/**
 * Snapshot of a single live JVM thread, used by the Threads view for filtering and sorting.
 *
 * <p>
 * {@code cpuTimeNanos} and {@code userTimeNanos} are {@code -1} when thread CPU-time measurement is
 * disabled or unsupported on the running JVM. {@code lockOwnerId} is {@code null} when the thread
 * is not waiting on a lock owned by another thread.
 */
public record ThreadDetail(long id, String name, String state, boolean daemon, int priority, long cpuTimeNanos, long userTimeNanos,
        String lockName, String lockOwnerName, Long lockOwnerId) {
}
