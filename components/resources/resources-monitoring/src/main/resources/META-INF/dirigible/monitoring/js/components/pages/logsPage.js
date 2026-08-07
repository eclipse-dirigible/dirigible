/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * The Logs page: the live stream on one tab, the files on disk on the other. The socket is opened
 * when the page is entered and closed when it is left - a background stream nobody is watching is
 * just traffic.
 */
document.addEventListener('alpine:init', () => {
  Alpine.data('logsPage', () => ({
    tab: 'live',
    autoScroll: true,

    init() {
      this.$store.logs.connect();
      this.$store.logs.loadFiles();
    },

    destroy() {
      this.$store.logs.disconnect();
    },

    get state() {
      return this.$store.logs;
    },

    selectTab(tab) {
      this.tab = tab;
    },

    /** Keep the newest record in view unless the operator has scrolled away deliberately. */
    scrollToBottom() {
      if (!this.autoScroll) return;
      this.$nextTick(() => {
        const stream = this.$refs.stream;
        if (stream) stream.scrollTop = stream.scrollHeight;
      });
    },

    levelVariant(level) {
      switch (level) {
        case 'ERROR':
          return 'negative';
        case 'WARN':
          return 'warning';
        case 'INFO':
          return 'information';
        default:
          return 'outline';
      }
    },

    timeText(timestamp) {
      if (!timestamp) return '';
      const at = new Date(timestamp);
      return isNaN(at.getTime()) ? '' : at.toLocaleTimeString();
    },

    /** The download URL of a log file - the same endpoint the page reads, opened directly. */
    downloadUrl(file) {
      return '/services/ide/logs/' + encodeURIComponent(file);
    },
  }));
});
