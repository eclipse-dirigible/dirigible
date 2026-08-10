/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * The System page: the build this instance runs and the JVM snapshot, restyled. Read-only - a thread
 * dump is diagnosis, not management.
 */
document.addEventListener('alpine:init', () => {
  Alpine.data('systemPage', () => ({
    init() {
      this.$store.system.start();
    },

    destroy() {
      this.$store.system.stop();
    },

    get state() {
      return this.$store.system;
    },

    /** The build - read once by its own store, so it survives a failed metrics poll. */
    get version() {
      return this.$store.version;
    },

    /** An absent value is shown as a dash, like every other unavailable figure on this page. */
    orDash(value) {
      return value ? value : '—';
    },

    /** Bytes as MB - every memory figure on this page is in the same unit. */
    mb(bytes) {
      if (!Number.isFinite(bytes) || bytes < 0) return '—';
      return Math.round(bytes / (1024 * 1024)).toLocaleString() + ' MB';
    },

    /** A JVM that cannot measure a load reports a negative value - and, on some platforms, NaN. */
    percent(fraction) {
      if (!Number.isFinite(fraction) || fraction < 0) return '—';
      return Math.round(fraction * 100) + '%';
    },

    /** Uptime as a compact "3d 4h 12m". */
    uptime(millis) {
      if (!millis) return '—';
      const minutes = Math.floor(millis / 60000);
      const days = Math.floor(minutes / 1440);
      const hours = Math.floor((minutes % 1440) / 60);
      const rest = minutes % 60;
      return [days ? days + 'd' : '', hours ? hours + 'h' : '', rest + 'm'].filter(Boolean)
                                                                          .join(' ');
    },

    /** A used-of-max ratio as a percentage, or null when the pool declares no maximum. */
    usedRatio(usage) {
      if (!usage || !usage.max || usage.max <= 0) return null;
      return Math.round((usage.used / usage.max) * 100);
    },

    /** The heap fills up long before it is a problem; only the top of the range is worth colouring. */
    ratioVariant(ratio) {
      if (ratio === null) return 'outline';
      if (ratio >= 90) return 'negative';
      return ratio >= 75 ? 'warning' : 'positive';
    },

    /** A counter's max, when the platform declared one (a pool size, a file-descriptor limit). */
    countText(item) {
      return item.max ? item.value.toLocaleString() + ' / ' + item.max.toLocaleString() : item.value.toLocaleString();
    },

    cpuTime(nanos) {
      return nanos === null || nanos === undefined || nanos < 0 ? '—' : Math.round(nanos / 1000000).toLocaleString() + ' ms';
    },
  }));
});
