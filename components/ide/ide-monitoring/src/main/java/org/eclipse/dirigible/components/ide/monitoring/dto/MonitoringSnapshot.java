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

import java.util.List;
import java.util.Map;

/**
 * Aggregated JVM runtime metrics snapshot returned by the monitoring endpoint.
 */
public record MonitoringSnapshot(long timestamp, RuntimeInfo runtime, CpuInfo cpu, MemoryInfo memory, ThreadsInfo threads,
        List<GcInfo> gc) {

    public record RuntimeInfo(String vmName, String vmVendor, String vmVersion, String javaVersion, long pid, long startTime,
            long uptimeMillis, int availableProcessors, String osName, String osArch, String osVersion) {
    }

    public record CpuInfo(double systemCpuLoad, double processCpuLoad, double systemLoadAverage) {
    }

    public record MemoryInfo(MemoryUsageInfo heap, MemoryUsageInfo nonHeap, List<MemoryPoolInfo> pools) {
    }

    public record MemoryUsageInfo(long init, long used, long committed, long max) {
    }

    public record MemoryPoolInfo(String name, String type, MemoryUsageInfo usage, MemoryUsageInfo peakUsage) {
    }

    public record ThreadsInfo(int liveCount, int daemonCount, int peakCount, long totalStartedCount, int deadlockedCount,
            Map<String, Integer> countsByState) {
    }

    public record GcInfo(String name, long collectionCount, long collectionTimeMillis, List<String> memoryPoolNames) {
    }
}
