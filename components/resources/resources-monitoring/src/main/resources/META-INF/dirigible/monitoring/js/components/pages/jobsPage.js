/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * The Jobs page: the scheduled jobs on the left, the selected job's execution log on the right.
 * The store owns the data and the actions; this component only formats what they hold.
 */
document.addEventListener('alpine:init', () => {
  Alpine.data('jobsPage', () => ({
    init() {
      this.$store.jobs.load();
    },

    get state() {
      return this.$store.jobs;
    },

    /** The Harmonia variant for a job or log status. */
    statusVariant(status) {
      switch (status) {
        case 'FINISHED':
          return 'positive';
        case 'FAILED':
        case 'ERROR':
          return 'negative';
        case 'TRIGGRED':
          return 'information';
        case 'WARN':
          return 'warning';
        default:
          return 'outline';
      }
    },

    timeText(value) {
      if (!value) return '—';
      const at = new Date(value);
      return isNaN(at.getTime()) ? '—' : at.toLocaleString();
    },

    shorten(text, limit = 200) {
      const single = (text || '').replace(/\s+/g, ' ').trim();
      return single.length > limit ? single.slice(0, limit) + '…' : single;
    },

    /** The log entries newest first - the platform returns them in insertion order. */
    get logEntries() {
      return [...this.state.logs].sort((left, right) =>
        new Date(right.triggeredAt || 0) - new Date(left.triggeredAt || 0));
    },
  }));
});
