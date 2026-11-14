/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors
 * SPDX-License-Identifier: EPL-2.0
 */
/*
 * tenantExport store - the exports of the current tenant, for the "Data export" section of Settings.
 *
 * A tenant owner starts an export of everything the tenant holds - the application's tables and
 * every stored document - follows it while it runs, downloads the one archive it ends in, and deletes
 * it. The export runs in the background: starting one is ONE request to /services/data/tenant-export,
 * answered 202 at once, and the list shows where it stands. The endpoint family exists only when the
 * platform enables the feature and answers only those who may export - so anything but a 200 on the
 * context means "not offered here" and the section stays hidden.
 *
 * The markup is ONE fragment (shell/views/_tenant-export.html), mounted by the application shell's
 * Settings and by every generated application shell through x-html, like the Users section. The
 * mounts use only `visible`, `markup` and `load()`. Shells generated before this store existed never
 * load it; shared code that reaches for it must check Alpine.store('tenantExport') first.
 */
document.addEventListener('alpine:init', () => {
  const EXPORT_URL = '/services/data/tenant-export';
  const CONTEXT_URL = EXPORT_URL + '/context';
  const MARKUP_URL = '/services/web/application-core/shell/views/_tenant-export.html';
  const HEADERS = { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' };
  /** How often a running export is re-read. */
  const POLL_MS = 3 * 1000;

  Alpine.store('tenantExport', {
    /** What the platform says: { enabled, maxArchives }; null until loaded. */
    context: null,
    /** The exports of the tenant, newest first. */
    items: [],
    loaded: false,
    busy: false,
    /** The export the delete dialog asks about; null while it is closed. */
    removeTarget: null,
    /** The last refusal, in the user's words; empty when there is none. */
    error: '',
    /** The fragment the section renders from. */
    markup: '',
    /** A load asked for before the context arrived - e.g. the page opened on the section's route. */
    loadRequested: false,
    pollTimer: null,

    /** The section is offered where the platform enables it, to those it answers. */
    get visible() {
      return !!(this.context && this.context.enabled);
    },

    /** How many archives the tenant may keep at a time. */
    get maxArchives() {
      return (this.context && this.context.maxArchives) || 3;
    },

    /** The tenant keeps as many archives as it may - a further export is refused until one is deleted. */
    get atLimit() {
      return this.items.filter((item) => this.isCompleted(item)).length >= this.maxArchives;
    },

    /** An export is requested or running - a second one is refused until it ends. */
    get running() {
      return this.items.some((item) => this.isRunning(item));
    },

    init() {
      this.loadContext();
      document.addEventListener('visibilitychange', () => {
        if (!document.hidden && this.running) this.load();
      });
    },

    async loadContext() {
      try {
        const response = await fetch(CONTEXT_URL, { headers: HEADERS, credentials: 'same-origin' });
        if (!response.ok) {
          this.context = { enabled: false };
          return;
        }
        this.context = Object.assign({ enabled: true }, await response.json());
        this.loadMarkup();
        if (this.loadRequested) this.load();
      } catch (e) {
        console.error('tenantExport: could not read the context', e);
        this.context = { enabled: false };
      }
    },

    async loadMarkup() {
      if (this.markup) return;
      try {
        const response = await fetch(MARKUP_URL, { credentials: 'same-origin' });
        if (response.ok) this.markup = await response.text();
      } catch (e) {
        console.error('tenantExport: could not load the section', e);
      }
    },

    /** Reads the exports of the tenant - called when the section is opened, after every change, and while one runs. */
    async load() {
      if (this.context === null) {
        this.loadRequested = true;
        return;
      }
      this.loadRequested = false;
      if (!this.visible) return;
      this.loadMarkup();
      try {
        const response = await fetch(EXPORT_URL, { headers: HEADERS, credentials: 'same-origin' });
        if (!response.ok) {
          this.error = this.messageFor(response.status);
          return;
        }
        this.items = await response.json();
      } catch (e) {
        console.error('tenantExport: could not read the exports', e);
        this.error = this.messageFor(0);
      } finally {
        this.loaded = true;
        this.schedulePoll();
      }
    },

    /** Starts an export of the tenant - one request, answered 202; the list follows it from there. */
    async start() {
      if (this.busy || this.running || this.atLimit) return;
      await this.send('POST', EXPORT_URL, () => {
        this.announce(this.t('shell.tenantExport.started', 'The export has started'));
      });
    },

    /** Asks before an export is deleted - its archive cannot be brought back. */
    askRemove(item) {
      if (this.busy || !item || this.isRunning(item)) return;
      this.removeTarget = item;
    },

    closeRemove() {
      this.removeTarget = null;
    },

    /** Deletes the export the dialog asks about, with its archive. */
    async remove() {
      const item = this.removeTarget;
      if (this.busy || !item) return;
      await this.send('DELETE', EXPORT_URL + '/' + encodeURIComponent(item.id), () => {});
      this.removeTarget = null;
    },

    async send(method, url, onSuccess) {
      this.busy = true;
      this.error = '';
      try {
        const response = await fetch(url, { method: method, headers: HEADERS, credentials: 'same-origin' });
        if (!response.ok) {
          this.error = this.messageFor(response.status);
        } else {
          onSuccess();
        }
        const refused = !response.ok && response.status === 409;
        await this.load();
        // 409 is "one is running" or "the limit is reached" - the list just read says which, and the
        // section already says the limit is reached.
        if (refused && !this.running && this.atLimit) this.error = '';
      } catch (e) {
        console.error('tenantExport: the request failed', e);
        this.error = this.messageFor(0);
      } finally {
        this.busy = false;
      }
    },

    // ---- what a row shows ----

    isRunning(item) {
      return item.status === 'REQUESTED' || item.status === 'RUNNING';
    },

    isCompleted(item) {
      return item.status === 'COMPLETED';
    },

    isFailed(item) {
      return item.status === 'FAILED';
    },

    /** Where the browser downloads the archive from - a plain link, so the browser streams it to disk. */
    downloadUrl(item) {
      return EXPORT_URL + '/' + encodeURIComponent(item.id) + '/archive';
    },

    statusVariant(item) {
      switch (item.status) {
        case 'COMPLETED': return 'positive';
        case 'FAILED': return 'negative';
        default: return 'information';
      }
    },

    statusLabel(item) {
      const fallback = { REQUESTED: 'Requested', RUNNING: 'Running', COMPLETED: 'Ready', FAILED: 'Failed' }[item.status] || item.status;
      return this.t('shell.tenantExport.status.' + item.status, fallback);
    },

    /** A size in the unit a person reads it in; empty when there is none. */
    size(bytes) {
      if (bytes === null || bytes === undefined) return '';
      const units = ['B', 'KB', 'MB', 'GB', 'TB'];
      let value = bytes;
      let unit = 0;
      while (value >= 1024 && unit < units.length - 1) {
        value = value / 1024;
        unit++;
      }
      return (unit === 0 ? String(value) : value.toFixed(1)) + ' ' + units[unit];
    },

    /** A timestamp in the user's locale; empty when there is none. */
    time(iso) {
      if (!iso) return '';
      const date = new Date(iso);
      return isNaN(date.getTime()) ? iso : date.toLocaleString();
    },

    // ---- internals ----

    /** The section is on screen: mounted, shown and in a visible tab. */
    onScreen() {
      const page = document.getElementById('tenant-export-page');
      return !document.hidden && !!page && page.offsetParent !== null;
    },

    /** Re-reads the list while an export runs and the section is on screen; stops when none does. */
    schedulePoll() {
      if (this.pollTimer) {
        clearTimeout(this.pollTimer);
        this.pollTimer = null;
      }
      if (!this.running || !this.onScreen()) return;
      this.pollTimer = setTimeout(() => this.load(), POLL_MS);
    },

    /** The words for a refusal - from its status, never its raw text. */
    messageFor(status) {
      if (status === 409) return this.t('shell.tenantExport.error.RUNNING', 'An export is still running. Wait for it to finish.');
      if (status === 410) return this.t('shell.tenantExport.error.EXPIRED', 'This export has expired. Start a new one.');
      if (status === 404) return this.t('shell.tenantExport.error.GONE', 'This export is no longer in the list. Reload it.');
      if (status === 403) return this.t('shell.tenantExport.error.FORBIDDEN', 'Only an owner of this tenant can export its data.');
      if (status === 401) return this.t('shell.tenantExport.error.SESSION', 'Your session has expired. Reload the page to sign in again.');
      return this.t('shell.tenantExport.error.FAILED', 'Something went wrong. Please try again.');
    },

    limitMessage() {
      return this.t('shell.tenantExport.limit', 'This tenant keeps up to {{count}} exports. Delete one to make a new one.', { count: this.maxArchives });
    },

    announce(message) {
      const notifications = Alpine.store('notifications');
      if (notifications && typeof notifications.announce === 'function') {
        notifications.announce({ title: message, variant: 'positive' });
      }
    },

    /** Translate a shell key, with {{name}} placeholders; the fallback where the page carries no i18n service. */
    t(key, fallback, options) {
      if (typeof window.T === 'function') return T('application-core:' + key, fallback, options);
      return String(fallback).replace(/\{\{(\w+)\}\}/g, (match, name) => (options && name in options ? options[name] : match));
    },
  });
}, { once: true });
