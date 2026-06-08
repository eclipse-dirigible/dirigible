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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.ide.monitoring.dto.MonitoringSnapshot;
import org.eclipse.dirigible.components.ide.monitoring.dto.MonitoringSnapshot.CpuInfo;
import org.eclipse.dirigible.components.ide.monitoring.dto.MonitoringSnapshot.GcInfo;
import org.eclipse.dirigible.components.ide.monitoring.dto.MonitoringSnapshot.MemoryInfo;
import org.eclipse.dirigible.components.ide.monitoring.dto.MonitoringSnapshot.MemoryPoolInfo;
import org.eclipse.dirigible.components.ide.monitoring.dto.MonitoringSnapshot.MemoryUsageInfo;
import org.eclipse.dirigible.components.ide.monitoring.dto.MonitoringSnapshot.RuntimeInfo;
import org.eclipse.dirigible.components.ide.monitoring.dto.MonitoringSnapshot.ThreadsInfo;
import org.eclipse.dirigible.components.ide.monitoring.dto.ThreadDetail;
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

    /**
     * Returns a detailed view of every live thread in the JVM, suitable for the Threads view's
     * filtering and sorting. The Monitoring snapshot intentionally omits this list to keep its payload
     * small for the auto-refresh polling on the dashboard.
     */
    public List<ThreadDetail> threads() {
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        long[] ids = threads.getAllThreadIds();
        if (ids.length == 0) {
            return Collections.emptyList();
        }
        Map<Long, Thread> liveThreads = indexLiveThreads();
        boolean cpuTimeSupported = threads.isThreadCpuTimeSupported() && threads.isThreadCpuTimeEnabled();
        ThreadInfo[] infos = threads.getThreadInfo(ids, 0);
        List<ThreadDetail> details = new ArrayList<>(infos.length);
        for (ThreadInfo info : infos) {
            if (info == null) {
                continue;
            }
            Thread live = liveThreads.get(info.getThreadId());
            long cpuNanos = cpuTimeSupported ? threads.getThreadCpuTime(info.getThreadId()) : -1L;
            long userNanos = cpuTimeSupported ? threads.getThreadUserTime(info.getThreadId()) : -1L;
            details.add(new ThreadDetail(info.getThreadId(), info.getThreadName(), info.getThreadState()
                                                                                       .name(),
                    live != null && live.isDaemon(), live != null ? live.getPriority() : -1, cpuNanos, userNanos, info.getLockName(),
                    info.getLockOwnerName(), info.getLockOwnerId() == -1 ? null : info.getLockOwnerId()));
        }
        return details;
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
        long[] ids = threads.getAllThreadIds();
        ThreadInfo[] infos = threads.getThreadInfo(ids, 0);
        for (ThreadInfo info : infos) {
            if (info == null) {
                continue;
            }
            byState.merge(info.getThreadState()
                              .name(),
                    1, Integer::sum);
        }
        long[] deadlocked = threads.findDeadlockedThreads();
        int deadlockedCount = deadlocked == null ? 0 : deadlocked.length;
        return new ThreadsInfo(threads.getThreadCount(), threads.getDaemonThreadCount(), threads.getPeakThreadCount(),
                threads.getTotalStartedThreadCount(), deadlockedCount, byState);
    }

    private List<GcInfo> collectGc() {
        List<GcInfo> result = new ArrayList<>();
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            result.add(new GcInfo(gc.getName(), gc.getCollectionCount(), gc.getCollectionTime(), Arrays.asList(gc.getMemoryPoolNames())));
        }
        return result;
    }

    /**
     * Snapshots the live {@link Thread} set so we can read per-thread attributes (daemon, priority)
     * that are not exposed on {@link ThreadInfo}.
     */
    private static Map<Long, Thread> indexLiveThreads() {
        Thread[] all = new Thread[Thread.activeCount() * 2];
        int actual = Thread.enumerate(all);
        Map<Long, Thread> byId = new HashMap<>(actual);
        for (int i = 0; i < actual; i++) {
            Thread t = all[i];
            if (t != null) {
                byId.put(t.getId(), t);
            }
        }
        return byId;
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
