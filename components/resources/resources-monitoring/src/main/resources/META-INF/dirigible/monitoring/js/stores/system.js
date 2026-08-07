/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * State behind the System page: the JVM snapshot (CPU, memory, threads, GC), the platform's own
 * counters, and the live thread list.
 *
 * The heap and CPU histories are a client-side ring buffer filled while the page is open - there is
 * no server-side metric history, and this shell does not invent one. Long-term dashboards remain the
 * OpenTelemetry stack's job.
 */
document.addEventListener('alpine:init', () => {
  const REFRESH_MS = 10000;

  /** How many samples the sparklines keep - about five minutes at the refresh cadence above. */
  const HISTORY_SIZE = 30;

  Alpine.store('system', {
    loading: true,
    metrics: null,
    counts: null,
    threads: [],
    error: null,
    threadFilter: '',
    /** { labels: [...], series: [{ name, data }] } - the shape Harmonia's chart directive takes. */
    heapHistory: { labels: [], series: [{ name: 'Heap MB', data: [] }] },
    cpuHistory: { labels: [], series: [{ name: 'CPU %', data: [] }] },

    _timer: null,

    start() {
      if (this._timer === null) {
        this._timer = setInterval(() => this.refresh(), REFRESH_MS);
      }
      this.refresh();
    },

    stop() {
      if (this._timer !== null) {
        clearInterval(this._timer);
        this._timer = null;
      }
    },

    async refresh() {
      const ops = window.MonitoringOps;
      const errors = {};
      const [metrics, counts, threads] = await Promise.all([
        ops.soft('metrics', errors, ops.metrics, null),
        ops.soft('counts', errors, ops.counts, null),
        ops.soft('threads', errors, ops.threads, []),
      ]);
      this.metrics = metrics;
      this.counts = counts;
      this.threads = threads || [];
      this.error = errors.metrics || null;
      this.sample(metrics);
      this.loading = false;
    },

    /** Append one sample to the session-local histories. */
    sample(metrics) {
      if (!metrics) return;
      const label = new Date(metrics.timestamp).toLocaleTimeString();
      // A JVM that cannot measure the load reports a negative value or NaN - both plot as zero
      // rather than breaking the line.
      const load = Number.isFinite(metrics.cpu.processCpuLoad) ? metrics.cpu.processCpuLoad : 0;
      const heapMb = Math.round((metrics.memory.heap.used || 0) / (1024 * 1024));
      const cpuPercent = Math.round(Math.max(0, load) * 100);
      this.push(this.heapHistory, label, heapMb);
      this.push(this.cpuHistory, label, cpuPercent);
    },

    push(history, label, value) {
      const labels = [...history.labels, label];
      const data = [...history.series[0].data, value];
      if (labels.length > HISTORY_SIZE) {
        labels.shift();
        data.shift();
      }
      // Replaced wholesale, not mutated: the chart directive re-renders on a new value.
      history.labels = labels;
      history.series = [{ name: history.series[0].name, data }];
    },

    get visibleThreads() {
      const needle = this.threadFilter.trim()
                         .toLowerCase();
      if (!needle) return this.threads;
      return this.threads.filter((thread) => (thread.name || '').toLowerCase()
                                                                .includes(needle)
        || (thread.state || '').toLowerCase()
                               .includes(needle));
    },

    get deadlockedCount() {
      return (this.metrics && this.metrics.threads && this.metrics.threads.deadlockedCount) || 0;
    },
  });
});
