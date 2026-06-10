/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.platform;

/**
 * Read-only snapshot of the host JVM — operating system, architecture, processor count, and the
 * current memory budget. Useful for diagnostics endpoints and for jobs that want to gate their
 * batch size by available memory.
 * <p>
 * "Available memory" follows the conventional JVM definition:
 * {@code maxMemory - (totalMemory - freeMemory)} — i.e. how much more the heap can grow before
 * hitting {@code -Xmx}, not how much physical RAM the OS has free. For OS-level numbers you need a
 * JMX-based view (see {@code com.sun.management.OperatingSystemMXBean}).
 */
public final class Os {

    private Os() {}

    public static String getOS() {
        return System.getProperty("os.name");
    }

    public static String getArch() {
        return System.getProperty("os.arch");
    }

    public static int getProcessors() {
        return Runtime.getRuntime()
                      .availableProcessors();
    }

    public static long getFreeMemory() {
        return Runtime.getRuntime()
                      .freeMemory();
    }

    public static long getTotalMemory() {
        return Runtime.getRuntime()
                      .totalMemory();
    }

    public static long getMaxMemory() {
        return Runtime.getRuntime()
                      .maxMemory();
    }

    public static long getAvailableMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
    }
}
