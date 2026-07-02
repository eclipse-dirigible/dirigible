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
/**
 * processTasks — surfaces a record's BPM user tasks as in-context actions, the Harmonia/Alpine
 * counterpart of the dashboard's ProcessTasks module. A process-aware entity carries a
 * system-managed ProcessId (the started process-instance id, written back by the intent process
 * trigger); the current user's actionable inbox tasks are fetched once and bucketed by
 * processInstanceId, so any view — list, manage, or a master-detail pane — surfaces the tasks for a
 * given record by matching entity.ProcessId === task.processInstanceId.
 *
 * It is a global Alpine store so every generated view reads it the same way:
 *   $store.processTasks.getTasks(row)        -> the record's actionable tasks
 *   $store.processTasks.openTask(task)       -> claim if needed, then open the task form
 * The task form is opened in the app-wide dialog wired in index.html; on close the store re-fetches.
 */
document.addEventListener('alpine:init', () => {
  Alpine.store('processTasks', {
    byProcessId: {},
    tasks: [],          // flat list of all the user's tasks (assignee + groups) — the Inbox view
    loaded: false,
    formOpen: false,
    formUrl: '',
    formTitle: '',
    serverUnavailable: false,   // set once the backend is unreachable; stops the poll until a reload
    _poll: null,

    init() {
      this.load();
      // A task form (opened in the app-wide dialog) asks its host to close when it completes.
      window.addEventListener('message', (e) => {
        if (e && e.data && e.data.type === 'harmonia.form.close' && this.formOpen) this.closeForm();
      });
      // Keep every consumer fresh without a manual page refresh — the bell notifications AND the
      // in-record task buttons (which read this store) — by re-checking the inbox on navigation and
      // on a periodic poll. So a task raised by a just-created record (e.g. a new Loan starting a
      // process) shows up on its own, not only after a full refresh or visiting the Inbox.
      document.addEventListener('pinecone:end', () => { if (this.loaded) this.load(); });
      this._poll = setInterval(() => this.load(), 30000);
    },

    async load() {
      // Once the server has gone away we stop polling entirely; a browser refresh recreates this
      // store (serverUnavailable back to false) and resumes.
      if (this.serverUnavailable) return;
      try {
        const [mine, groups] = await Promise.all([
          App.services.api.get('/services/inbox/tasks?type=assignee&limit=100', { baseUrl: '' }),
          App.services.api.get('/services/inbox/tasks?type=groups&limit=100', { baseUrl: '' }),
        ]);
        const map = {};
        const flat = [];
        const seen = new Set();
        const collect = (tasks, isMine) => (tasks || []).forEach((t) => {
          if (seen.has(t.id)) return;
          seen.add(t.id);
          t.mine = isMine;
          flat.push(t);
          // Only process-bound tasks bucket for in-record surfacing; all tasks show in the Inbox.
          if (t.processInstanceId) (map[t.processInstanceId] = map[t.processInstanceId] || []).push(t);
        });
        collect(mine, true);
        collect(groups, false);
        this.byProcessId = map;
        this.tasks = flat;
        this.loaded = true;
        // Surface the user's actionable tasks in the shell's notification bell.
        const notifications = Alpine.store('notifications');
        if (notifications && notifications.syncTasks) notifications.syncTasks(flat);
      } catch (e) {
        // Stop the loop when retrying can't help: a transport failure (httpStatus 0 → dead server) or
        // an auth failure (401 not-authenticated / 403 forbidden → the session expired or lacks the
        // role). Polling every 30s in those cases just spams the console/network. A browser refresh
        // recreates this store (serverUnavailable back to false) and resumes; a 401 typically means the
        // user must re-login. Other 4xx/5xx are transient app errors, so keep polling for those.
        if (e && e.isApiError && (e.httpStatus === 0 || e.httpStatus === 401 || e.httpStatus === 403)) {
          this.serverUnavailable = true;
          if (this._poll) { clearInterval(this._poll); this._poll = null; }
          console.warn('processTasks: ' + (e.httpStatus === 0 ? 'server unavailable' : 'not authenticated (' + e.httpStatus + ')')
            + ' — stopped polling for tasks; refresh the page to resume');
          return;
        }
        this.byProcessId = {};
        this.tasks = [];
        console.error('processTasks: unable to load inbox tasks', e);
      }
    },

    refresh() { return this.load(); },

    getTasks(entity) {
      return (entity && entity.ProcessId && this.byProcessId[entity.ProcessId]) || [];
    },

    async openTask(task) {
      // A candidate (group) task must be claimed for the user before its form opens.
      if (!task.mine) {
        try {
          await App.services.api.post('/services/inbox/tasks/' + task.id, { action: 'CLAIM' }, { baseUrl: '' });
          task.mine = true;
          this.load();
        } catch (e) {
          console.error('processTasks: unable to claim task', e);
          return;
        }
      }
      if (!task.formKey) {
        console.warn('processTasks: task has no formKey', task);
        return;
      }
      const sep = task.formKey.indexOf('?') >= 0 ? '&' : '?';
      this.formUrl = task.formKey + sep + 'taskId=' + encodeURIComponent(task.id)
                   + '&processInstanceId=' + encodeURIComponent(task.processInstanceId);
      this.formTitle = task.name || 'Task';
      this.formOpen = true;
    },

    // Called when the task-form dialog closes; the generated task form completes the task itself,
    // so a re-fetch drops the finished task from the originating view's badge.
    closeForm() {
      this.formOpen = false;
      this.formUrl = '';
      this.refresh();
    },
  });
}, { once: true });
