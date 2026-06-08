/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.monitoring.service;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.ide.monitoring.dto.MonitoringSnapshot;
import org.eclipse.dirigible.components.ide.monitoring.dto.MonitoringSnapshot.BlockedThreadInfo;
import org.eclipse.dirigible.components.ide.monitoring.dto.MonitoringSnapshot.CpuInfo;
import org.eclipse.dirigible.components.ide.monitoring.dto.MonitoringSnapshot.GcInfo;
import org.eclipse.dirigible.components.ide.monitoring.dto.MonitoringSnapshot.MemoryInfo;
import org.eclipse.dirigible.components.ide.monitoring.dto.MonitoringSnapshot.MemoryPoolInfo;
import org.eclipse.dirigible.components.ide.monitoring.dto.MonitoringSnapshot.MemoryUsageInfo;
import org.eclipse.dirigible.components.ide.monitoring.dto.MonitoringSnapshot.RuntimeInfo;
import org.eclipse.dirigible.components.ide.monitoring.dto.MonitoringSnapshot.ThreadsInfo;
import org.springframework.stereotype.Service;

/**
 * Builds an immutable {@link MonitoringSnapshot} from the platform JMX MXBeans. Stateless — every
 * call samples the live JVM.
 */
@Service
public class MonitoringService {

    // CPU-load accessors live on com.sun.management.OperatingSystemMXBean rather than the platform
    // interface; we invoke them reflectively so the module has no hard dependency on the Sun bean.

    public MonitoringSnapshot snapshot() {
        return new MonitoringSnapshot(System.currentTimeMillis(), collectRuntime(), collectCpu(), collectMemory(), collectThreads(),
                collectGc());
    }

    private RuntimeInfo collectRuntime() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        return new RuntimeInfo(runtime.getVmName(), runtime.getVmVendor(), runtime.getVmVersion(), System.getProperty("java.version"),
                runtime.getPid(), runtime.getStartTime(), runtime.getUptime(), os.getAvailableProcessors(), os.getName(), os.getArch(),
                os.getVersion());
    }

    private CpuInfo collectCpu() {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        double system = readDouble(os, "getCpuLoad");
        if (Double.isNaN(system)) {
            system = readDouble(os, "getSystemCpuLoad");
        }
        double process = readDouble(os, "getProcessCpuLoad");
        return new CpuInfo(system, process, os.getSystemLoadAverage());
    }

    private MemoryInfo collectMemory() {
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        List<MemoryPoolInfo> pools = new ArrayList<>();
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            pools.add(new MemoryPoolInfo(pool.getName(), pool.getType()
                                                             .name(),
                    toUsage(pool.getUsage()), toUsage(pool.getPeakUsage())));
        }
        return new MemoryInfo(toUsage(memory.getHeapMemoryUsage()), toUsage(memory.getNonHeapMemoryUsage()), pools);
    }

    private ThreadsInfo collectThreads() {
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        Map<String, Integer> byState = emptyStateCounts();
        List<BlockedThreadInfo> blockedOrWaiting = new ArrayList<>();
        long[] ids = threads.getAllThreadIds();
        ThreadInfo[] infos = threads.getThreadInfo(ids, 0);
        for (ThreadInfo info : infos) {
            if (info == null) {
                continue;
            }
            Thread.State state = info.getThreadState();
            byState.merge(state.name(), 1, Integer::sum);
            if (state == Thread.State.BLOCKED || state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING) {
                blockedOrWaiting.add(new BlockedThreadInfo(info.getThreadId(), info.getThreadName(), state.name(), info.getLockName(),
                        info.getLockOwnerName(), info.getLockOwnerId() == -1 ? null : info.getLockOwnerId()));
            }
        }
        long[] deadlocked = threads.findDeadlockedThreads();
        int deadlockedCount = deadlocked == null ? 0 : deadlocked.length;
        return new ThreadsInfo(threads.getThreadCount(), threads.getDaemonThreadCount(), threads.getPeakThreadCount(),
                threads.getTotalStartedThreadCount(), deadlockedCount, byState, blockedOrWaiting);
    }

    private List<GcInfo> collectGc() {
        List<GcInfo> result = new ArrayList<>();
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            result.add(new GcInfo(gc.getName(), gc.getCollectionCount(), gc.getCollectionTime(), Arrays.asList(gc.getMemoryPoolNames())));
        }
        return result;
    }

    private static MemoryUsageInfo toUsage(MemoryUsage usage) {
        if (usage == null) {
            return new MemoryUsageInfo(-1, -1, -1, -1);
        }
        return new MemoryUsageInfo(usage.getInit(), usage.getUsed(), usage.getCommitted(), usage.getMax());
    }

    private static Map<String, Integer> emptyStateCounts() {
        // Seed every Thread.State so the JSON always carries the full set, including zero-count states.
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Thread.State state : Thread.State.values()) {
            counts.put(state.name(), 0);
        }
        return counts;
    }

    /**
     * Reflective accessor for double-returning MXBean methods. Returns {@link Double#NaN} if the method
     * is missing or the call throws — the caller decides whether to fall back to another accessor.
     */
    private static double readDouble(Object bean, String methodName) {
        try {
            Object value = bean.getClass()
                               .getMethod(methodName)
                               .invoke(bean);
            return value instanceof Number number ? number.doubleValue() : Double.NaN;
        } catch (ReflectiveOperationException ex) {
            return Double.NaN;
        }
    }
}
