/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * The Overview page: lifecycle for the polled `overview` store plus the presentation helpers its
 * tiles bind to. All state lives in the store (the sidebar reads the same counts) - this component
 * only starts and stops the poll with the page and formats what the store holds.
 */
document.addEventListener('alpine:init', () => {
  Alpine.data('overviewPage', () => ({
    init() {
      this.$store.overview.start();
    },

    destroy() {
      this.$store.overview.stop();
    },

    /** The store, as a short name for the bindings. */
    get state() {
      return this.$store.overview;
    },

    /** "14:32:05" - a poll timestamp, not a date; the page shows only when it last refreshed. */
    lastUpdatedText() {
      const at = this.state.lastUpdated;
      return at ? at.toLocaleTimeString() : '';
    },

    /** A tile's headline: the source's own value, or a dash when the source could not be read. */
    valueText(source, value) {
      return this.state.errors[source] ? '—' : String(value);
    },

    /** A short, human explanation of a failed read, shown under the tile it belongs to. */
    errorText(source) {
      const error = this.state.errors[source];
      if (!error) return '';
      return error === 'forbidden'
        ? T('monitoring:overview.errors.forbidden', 'Not permitted for your roles')
        : T('monitoring:overview.errors.unavailable', 'Currently unavailable');
    },

    /** A count tile turns red as soon as it counts anything - these tiles only count problems. */
    countVariant(count) {
      return count > 0 ? 'negative' : 'positive';
    },

    /** The artefact's project - the first segment of its registry-relative location. */
    projectOf(location) {
      const segments = (location || '').split('/').filter(Boolean);
      return segments.length ? segments[0] : '';
    },

    /** Trim a stack-trace-ish message to something a tile row can carry. */
    shorten(text, limit = 160) {
      const single = (text || '').replace(/\s+/g, ' ').trim();
      return single.length > limit ? single.slice(0, limit) + '…' : single;
    },

    /** A job's last execution time, or a dash when it has never run. */
    executedAtText(job) {
      if (!job.executedAt) return '—';
      const at = new Date(job.executedAt);
      return isNaN(at.getTime()) ? '—' : at.toLocaleString();
    },
  }));
});
