/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 *
 * State behind the Messaging page - read-only on purpose. The broker summary and a non-destructive
 * browse of a queue's pending messages; purging a queue, deleting a message or removing a
 * destination stay in the Workbench's Messaging perspective.
 */
document.addEventListener('alpine:init', () => {
  /** How many messages a browse asks for. The endpoint caps at 200 anyway. */
  const BROWSE_LIMIT = 100;

  Alpine.store('messaging', {
    loading: false,
    summary: null,
    error: null,

    selectedQueue: '',
    messages: [],
    messagesLoading: false,

    async load() {
      this.loading = true;
      this.error = null;
      try {
        this.summary = await window.MonitoringOps.brokerSummary();
      } catch (e) {
        this.summary = null;
        this.error = e;
        console.error('monitoring: could not read the broker summary', e);
      } finally {
        this.loading = false;
      }
    },

    get queues() {
      return (this.summary && this.summary.queues) || [];
    },

    get topics() {
      return (this.summary && this.summary.topics) || [];
    },

    /** Browse a queue without consuming: the broker returns the pending set as it stands. */
    async selectQueue(queue) {
      this.selectedQueue = queue.name;
      this.messagesLoading = true;
      try {
        this.messages = await window.MonitoringOps.queueMessages(queue.name, BROWSE_LIMIT);
      } catch (e) {
        this.messages = [];
        console.error('monitoring: could not browse queue ' + queue.name, e);
      } finally {
        this.messagesLoading = false;
      }
    },

    clearSelection() {
      this.selectedQueue = '';
      this.messages = [];
    },
  });
});
