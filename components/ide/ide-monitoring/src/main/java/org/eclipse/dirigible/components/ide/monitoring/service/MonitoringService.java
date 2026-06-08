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

import java.lang.management.ClassLoadingMXBean;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.eclipse.dirigible.components.data.sources.manager.DataSourceInitializer;
import org.eclipse.dirigible.components.database.DirigibleDataSource;
import org.eclipse.dirigible.components.ide.monitoring.dto.CountMetrics;
import org.eclipse.dirigible.components.ide.monitoring.dto.CountMetrics.MetricGroup;
import org.eclipse.dirigible.components.ide.monitoring.dto.CountMetrics.NamedCount;
import org.eclipse.dirigible.components.ide.monitoring.dto.MonitoringSnapshot;
import org.eclipse.dirigible.components.ide.monitoring.dto.MonitoringSnapshot.CpuInfo;
import org.eclipse.dirigible.components.ide.monitoring.dto.MonitoringSnapshot.GcInfo;
import org.eclipse.dirigible.components.ide.monitoring.dto.MonitoringSnapshot.MemoryInfo;
import org.eclipse.dirigible.components.ide.monitoring.dto.MonitoringSnapshot.MemoryPoolInfo;
import org.eclipse.dirigible.components.ide.monitoring.dto.MonitoringSnapshot.MemoryUsageInfo;
import org.eclipse.dirigible.components.ide.monitoring.dto.MonitoringSnapshot.RuntimeInfo;
import org.eclipse.dirigible.components.ide.monitoring.dto.MonitoringSnapshot.ThreadsInfo;
import org.eclipse.dirigible.components.ide.monitoring.dto.ThreadDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Builds an immutable {@link MonitoringSnapshot} from the platform JMX MXBeans. Stateless — every
 * call samples the live JVM.
 */
@Service
public class MonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringService.class);

    private final DataSourceInitializer dataSourceInitializer;

    public MonitoringService(DataSourceInitializer dataSourceInitializer) {
        this.dataSourceInitializer = dataSourceInitializer;
    }

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

    /**
     * Collects count-only runtime metrics grouped by subsystem (threads, classloader, database pools,
     * OS). Designed for the Counters view, which renders a compact vertical list of name/value pairs
     * and auto-refreshes alongside the Monitoring dashboard.
     */
    public CountMetrics counts() {
        List<MetricGroup> groups = new ArrayList<>();
        groups.add(collectThreadCounts());
        groups.add(collectClassLoadingCounts());
        MetricGroup os = collectOsCounts();
        if (os != null) {
            groups.add(os);
        }
        MetricGroup pools = collectDataSourcePoolCounts();
        if (pools != null) {
            groups.add(pools);
        }
        return new CountMetrics(System.currentTimeMillis(), groups);
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

    private MetricGroup collectThreadCounts() {
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
        List<NamedCount> items = new ArrayList<>();
        items.add(new NamedCount("Live", threads.getThreadCount(), null));
        items.add(new NamedCount("Daemon", threads.getDaemonThreadCount(), null));
        items.add(new NamedCount("Peak", threads.getPeakThreadCount(), null));
        items.add(new NamedCount("Started total", threads.getTotalStartedThreadCount(), null));
        items.add(new NamedCount("Deadlocked", deadlockedCount, null));
        for (Map.Entry<String, Integer> entry : byState.entrySet()) {
            items.add(new NamedCount(entry.getKey(), entry.getValue(), null));
        }
        return new MetricGroup("Threads", items);
    }

    private MetricGroup collectClassLoadingCounts() {
        ClassLoadingMXBean classLoading = ManagementFactory.getClassLoadingMXBean();
        List<NamedCount> items = List.of(new NamedCount("Loaded", classLoading.getLoadedClassCount(), null),
                new NamedCount("Loaded total", classLoading.getTotalLoadedClassCount(), null),
                new NamedCount("Unloaded total", classLoading.getUnloadedClassCount(), null));
        return new MetricGroup("Classes", items);
    }

    /**
     * Open-file-descriptor counts come from {@code com.sun.management.UnixOperatingSystemMXBean}, which
     * is only present on Unix-family JVMs. Returns {@code null} on platforms (Windows) where neither
     * accessor exists, so the UI simply omits the group.
     */
    private MetricGroup collectOsCounts() {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        long openFds = readLong(os, "getOpenFileDescriptorCount");
        long maxFds = readLong(os, "getMaxFileDescriptorCount");
        if (openFds < 0 && maxFds < 0) {
            return null;
        }
        List<NamedCount> items = new ArrayList<>();
        if (openFds >= 0) {
            items.add(new NamedCount("Open file descriptors", openFds, maxFds >= 0 ? maxFds : null));
        }
        items.add(new NamedCount("Available processors", os.getAvailableProcessors(), null));
        return new MetricGroup("Operating system", items);
    }

    /**
     * Iterates every initialized Dirigible data source and reads its HikariCP pool MX bean. The
     * resulting {@link MetricGroup} carries one named-count per (data-source, metric) pair so the UI
     * can render them under a single "Database pools" heading; returns {@code null} when no pool is
     * initialized so the group is omitted entirely.
     */
    private MetricGroup collectDataSourcePoolCounts() {
        Collection<DirigibleDataSource> dataSources = dataSourceInitializer.getInitializedDataSources();
        if (dataSources.isEmpty()) {
            return null;
        }
        List<NamedCount> items = new ArrayList<>();
        for (DirigibleDataSource dataSource : dataSources) {
            HikariPoolStats stats = readHikariPoolStats(dataSource);
            if (stats == null) {
                continue;
            }
            String name = dataSource.getName();
            Long maxPoolSize = stats.maxPoolSize >= 0 ? (long) stats.maxPoolSize : null;
            items.add(new NamedCount(name + " · active", stats.active, maxPoolSize));
            items.add(new NamedCount(name + " · idle", stats.idle, null));
            items.add(new NamedCount(name + " · total", stats.total, maxPoolSize));
            items.add(new NamedCount(name + " · awaiting", stats.awaiting, null));
        }
        if (items.isEmpty()) {
            return null;
        }
        return new MetricGroup("Database pools", items);
    }

    private static HikariPoolStats readHikariPoolStats(DirigibleDataSource dataSource) {
        try {
            HikariDataSource hikari = dataSource.unwrap(HikariDataSource.class);
            HikariPoolMXBean mx = hikari.getHikariPoolMXBean();
            if (mx == null) {
                return null;
            }
            return new HikariPoolStats(mx.getActiveConnections(), mx.getIdleConnections(), mx.getTotalConnections(),
                    mx.getThreadsAwaitingConnection(), hikari.getMaximumPoolSize());
        } catch (Exception ex) {
            logger.debug("Skipping pool stats for data source [{}] — pool is not a HikariDataSource or is closed", dataSource.getName(),
                    ex);
            return null;
        }
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

    /**
     * Reflective accessor for long-returning MXBean methods. Returns {@code -1} if the method is
     * missing or the call throws — the caller treats negative values as "unsupported".
     */
    private static long readLong(Object bean, String methodName) {
        try {
            Object value = bean.getClass()
                               .getMethod(methodName)
                               .invoke(bean);
            return value instanceof Number number ? number.longValue() : -1L;
        } catch (ReflectiveOperationException ex) {
            return -1L;
        }
    }

    private record HikariPoolStats(int active, int idle, int total, int awaiting, int maxPoolSize) {
    }
}
