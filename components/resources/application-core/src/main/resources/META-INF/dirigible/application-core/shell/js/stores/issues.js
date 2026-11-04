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
 * issues store - "Report an issue" and the user's own issues (#7948).
 *
 * report() opens the report dialog pre-filled with what the page knows: the last failed requests
 * (App.services.recentFailures, of this page and of any same-origin frame it hosts - a generated
 * application runs in one inside the application shell), their request ids, the URL, the build, the
 * browser and the tail of the console. The reporter sees the whole bundle - with the server-side log
 * excerpt of their own request ids, read from /services/issues/excerpt - before sending it. The
 * server redacts the bundle again before storing it.
 *
 * The same store serves the Issues page (shell/views/_issues.html): the user's issues, the selected
 * one's thread, answering, attaching a file, withdrawing. Every read goes to /services/issues, which
 * filters by the tenant and the caller server-side.
 *
 * The dialog is ONE fragment (shell/views/_report-issue.html) fetched into `markup` and mounted with
 * x-html by every shell, like the tenant chip. Shells generated before this store existed never load
 * it; shared code that reaches for it must check Alpine.store('issues') first.
 *
 * A distribution may ship without the issues engine (engine-issues), and then nothing answers
 * /services/issues (#7991). The branding script every shell loads states whether it is deployed
 * (top.PlatformIssues), so `available` is known before anything renders and costs no request - a
 * probe of /services/issues would print a 404 to the console on every shell load. While it is false
 * the shells leave out "Report an issue" and the Issues entry, the Issues page says the instance
 * does not offer it, and the store sends nothing.
 */
document.addEventListener('alpine:init', () => {
  const ISSUES_URL = '/services/issues';
  const MARKUP_URL = '/services/web/application-core/shell/views/_report-issue.html';
  const VERSION_URL = '/services/core/version';
  const HEADERS = { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' };
  const FAILURES_IN_REPORT = 5;
  const CONSOLE_TAIL = 30;
  const CONSOLE_LINE = 500;

  // The tail of this page's console errors and warnings, for the report. Wrapped once per window.
  const consoleTail = [];
  if (!window.__issuesConsoleTail) {
    window.__issuesConsoleTail = consoleTail;
    ['error', 'warn'].forEach((level) => {
      const original = console[level];
      console[level] = function (...args) {
        try {
          const text = args.map((arg) => (arg instanceof Error ? arg.stack || arg.message : typeof arg === 'string' ? arg : safeJson(arg))).join(' ');
          consoleTail.push({ at: new Date().toISOString(), level: level, text: text.slice(0, CONSOLE_LINE) });
          if (consoleTail.length > CONSOLE_TAIL) consoleTail.shift();
        } catch (ignored) {
          // the console must keep working whatever an argument does when stringified
        }
        return original.apply(console, args);
      };
    });
  }

  function safeJson(value) {
    try {
      return JSON.stringify(value);
    } catch (e) {
      return String(value);
    }
  }

  function failuresOf(win) {
    try {
      return (win.App && win.App.services && win.App.services.recentFailures && win.App.services.recentFailures.items) || [];
    } catch (e) {
      return []; // a cross-origin frame
    }
  }

  // Whether the issues engine is deployed, as the branding script states it. A page that did not load
  // the script says nothing, and the engine is assumed, as it was before the flag existed.
  function engineDeployed() {
    try {
      return top.PlatformIssues !== false;
    } catch (e) {
      return true; // a cross-origin top
    }
  }

  Alpine.store('issues', {
    /** Whether this instance answers /services/issues at all (#7991). */
    available: engineDeployed(),
    /** The report dialog's markup. */
    markup: '',
    /** Whether the report dialog is open. */
    dialogOpen: false,
    /** The report being written. */
    draft: { subject: '', description: '', severity: 'MEDIUM' },
    /** The bundle the report carries, as it will be sent. */
    context: {},
    /** The server-side log lines of the reporter's own request ids. */
    excerpt: [],
    /** Whether the dialog shows the bundle. */
    showBundle: false,
    busy: false,
    /** The last refusal, in the user's words; empty when there is none. */
    error: '',
    /** The build, read once. */
    build: null,

    /** The Issues page: the user's issues and the selected one. */
    items: [],
    loaded: false,
    selected: null,
    answerDraft: '',

    init() {
      this.loadMarkup();
    },

    async loadMarkup() {
      if (this.markup) return;
      try {
        const response = await fetch(MARKUP_URL, { credentials: 'same-origin' });
        if (response.ok) this.markup = await response.text();
      } catch (e) {
        console.error('issues: could not load the report dialog', e);
      }
    },

    /** The failed requests of this page and of its same-origin frames, newest first. */
    failures() {
      const all = failuresOf(window).slice();
      document.querySelectorAll('iframe').forEach((frame) => {
        try {
          if (frame.contentWindow) all.push(...failuresOf(frame.contentWindow));
        } catch (e) {
          // a cross-origin frame
        }
      });
      return all.sort((a, b) => String(b.at).localeCompare(String(a.at)));
    },

    /** The request id of the last failure the user saw, to quote. */
    get lastRequestId() {
      const last = this.failures()[0];
      return (last && last.requestId) || '';
    },

    /**
     * Opens the report dialog pre-filled.
     *
     * @param prefill what the caller knows: { subject, entity, recordId, requestId }
     */
    async report(prefill) {
      const given = prefill || {};
      const failures = this.failures().slice(0, FAILURES_IN_REPORT);
      const requestIds = [...new Set([given.requestId, ...failures.map((f) => f.requestId)].filter(Boolean))];
      this.draft = { subject: given.subject || '', description: '', severity: 'MEDIUM' };
      this.error = this.available ? '' : this.unavailableMessage();
      this.showBundle = false;
      this.excerpt = [];
      this.context = {
        requestIds: requestIds,
        url: window.location.href,
        shell: document.title,
        perspective: this.hostedPath(),
        entity: given.entity || undefined,
        recordId: given.recordId != null ? String(given.recordId) : undefined,
        build: await this.loadBuild(),
        browser: navigator.userAgent,
        language: navigator.language,
        failedRequest: failures[0] || undefined,
        failures: failures,
        consoleTail: (window.__issuesConsoleTail || consoleTail).slice(-CONSOLE_TAIL),
      };
      this.dialogOpen = true;
      this.loadExcerpt();
    },

    /** The page a shell hosts in its iframe, when it hosts one. */
    hostedPath() {
      const frame = document.querySelector('iframe');
      try {
        return frame && frame.contentWindow ? frame.contentWindow.location.pathname + frame.contentWindow.location.hash : undefined;
      } catch (e) {
        return undefined;
      }
    },

    async loadBuild() {
      if (this.build) return this.build;
      try {
        const response = await fetch(VERSION_URL, { headers: HEADERS, credentials: 'same-origin' });
        if (response.ok) {
          const version = await response.json();
          this.build = { product: version.productName, version: version.productVersion, commit: version.productCommitId };
        }
      } catch (e) {
        console.error('issues: could not read the build', e);
      }
      return this.build || undefined;
    },

    async loadExcerpt() {
      const requestIds = this.context.requestIds || [];
      if (requestIds.length === 0 || !this.available) return;
      const response = await this.call('POST', ISSUES_URL + '/excerpt', { requestIds: requestIds });
      if (response) this.excerpt = response;
    },

    /** The bundle as the reporter reviews it. */
    get bundleText() {
      const shown = Object.assign({}, this.context);
      if (this.excerpt.length) shown.logExcerpt = this.excerpt;
      return JSON.stringify(shown, null, 2);
    },

    closeReport() {
      this.dialogOpen = false;
    },

    /** Sends the report. */
    async submit() {
      if (!this.draft.subject.trim()) return;
      if (!this.available) {
        this.error = this.unavailableMessage();
        return;
      }
      const issue = await this.call('POST', ISSUES_URL, {
        subject: this.draft.subject,
        description: this.draft.description,
        severity: this.draft.severity,
        context: this.context,
      });
      if (!issue) return;
      this.dialogOpen = false;
      this.announce(this.t('shell.issues.filed', 'Issue #{{id}} reported. You can follow it under Issues.', { id: issue.id }));
      this.loadList();
    },

    // ---- the Issues page ----

    /** Entering the Issues page: the list, and the report dialog when the link asks for it (?report=1, the Workbench's entry). */
    enterPage() {
      if (!this.available) return;
      this.loadList();
      const query = new URLSearchParams(window.location.hash.split('?')[1] || '');
      if (query.get('report') === '1') this.report();
    },

    async loadList() {
      const items = await this.call('GET', ISSUES_URL);
      if (items) this.items = items;
      this.loaded = true;
    },

    async select(id) {
      this.answerDraft = '';
      const detail = await this.call('GET', ISSUES_URL + '/' + encodeURIComponent(id));
      if (detail) this.selected = detail;
    },

    isSelected(issue) {
      return !!(this.selected && issue && this.selected.issue.id === issue.id);
    },

    get selectedOpen() {
      const status = this.selected && this.selected.issue.status;
      return !!status && status !== 'RESOLVED' && status !== 'REJECTED' && status !== 'CANCELLED';
    },

    async answer() {
      const text = (this.answerDraft || '').trim();
      if (!text || !this.selected) return;
      const message = await this.call('POST', ISSUES_URL + '/' + this.selected.issue.id + '/messages', { body: text });
      if (!message) return;
      this.answerDraft = '';
      await this.refreshSelected();
    },

    async attach(file) {
      if (!file || !this.selected) return;
      const form = new FormData();
      form.append('file', file);
      const attachment = await this.call('POST', ISSUES_URL + '/' + this.selected.issue.id + '/attachments', form);
      if (attachment) await this.refreshSelected();
    },

    async withdraw() {
      if (!this.selected) return;
      const issue = await this.call('POST', ISSUES_URL + '/' + this.selected.issue.id + '/withdraw');
      if (issue) await this.refreshSelected();
    },

    async refreshSelected() {
      const id = this.selected.issue.id;
      await Promise.all([this.select(id), this.loadList()]);
    },

    downloadUrl(attachment) {
      return ISSUES_URL + '/' + this.selected.issue.id + '/attachments/' + attachment.id;
    },

    attachmentsOf(message) {
      return ((this.selected && this.selected.attachments) || []).filter((a) => a.messageId === (message ? message.id : null));
    },

    statusVariant(status) {
      return { NEW: 'information', OPEN: 'information', WAITING_FOR_REPORTER: 'warning', RESOLVED: 'positive', REJECTED: 'negative', CANCELLED: 'outline' }[status] || 'outline';
    },

    statusLabel(status) {
      return this.t('shell.issues.status.' + status, String(status || '').replace(/_/g, ' ').toLowerCase());
    },

    // ---- plumbing ----

    /** One request; answers the parsed body, or null after recording the refusal in `error`. */
    async call(method, url, body) {
      this.busy = true;
      this.error = '';
      try {
        const isForm = body instanceof FormData;
        const response = await fetch(url, {
          method: method,
          headers: isForm || body === undefined ? HEADERS : Object.assign({ 'Content-Type': 'application/json' }, HEADERS),
          credentials: 'same-origin',
          body: body === undefined ? undefined : isForm ? body : JSON.stringify(body),
        });
        const text = await response.text();
        const parsed = text ? safeParse(text) : null;
        if (!response.ok) {
          // Only an issue's own URLs answer 404 for a thread that does not exist or is not the caller's. A
          // 404 on the collection means nothing answers /services/issues: no issues engine (#7991).
          const noEngine = response.status === 404 && (url === ISSUES_URL || url === ISSUES_URL + '/excerpt');
          if (noEngine) this.available = false;
          this.error = noEngine ? this.unavailableMessage() : this.messageFor(response.status, parsed);
          return null;
        }
        return parsed;
      } catch (e) {
        console.error('issues: ' + method + ' ' + url + ' failed', e);
        this.error = this.messageFor(0, null);
        return null;
      } finally {
        this.busy = false;
      }
    },

    /** The words for a refusal: the server's own text only for a 400/409/413, short and without markup. */
    messageFor(status, refusal) {
      const reason = refusal && typeof refusal.message === 'string' ? refusal.message : '';
      if ((status === 400 || status === 409 || status === 413) && reason && reason.length <= 300 && reason.indexOf('<') < 0) return reason;
      if (status === 404) return this.t('shell.issues.notFound', 'This issue does not exist, or is not yours.');
      if (status === 0) return this.t('shell.issues.offline', 'The server could not be reached. Try again in a moment.');
      return this.t('shell.issues.failed', 'That did not work. Try again in a moment.');
    },

    unavailableMessage() {
      return this.t('shell.issues.unavailable', 'Issue reporting is not available on this instance.');
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

  function safeParse(text) {
    try {
      return JSON.parse(text);
    } catch (e) {
      return null;
    }
  }
}, { once: true });
