/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * The Messaging page: the broker's destinations and a read-only browse of a queue's pending
 * messages.
 */
document.addEventListener('alpine:init', () => {
  Alpine.data('messagingPage', () => ({
    init() {
      this.$store.messaging.load();
    },

    get state() {
      return this.$store.messaging;
    },

    /** A queue holding messages with nothing consuming them is the case worth colouring. */
    queueVariant(queue) {
      if (queue.size > 0 && !queue.consumerCount) return 'negative';
      return queue.size > 0 ? 'warning' : 'positive';
    },

    timeText(timestamp) {
      if (!timestamp) return '';
      const at = new Date(timestamp);
      return isNaN(at.getTime()) ? '' : at.toLocaleString();
    },

    shorten(text, limit = 300) {
      const single = (text || '').replace(/\s+/g, ' ').trim();
      return single.length > limit ? single.slice(0, limit) + '…' : single;
    },
  }));
});
