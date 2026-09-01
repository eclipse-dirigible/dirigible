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
 * inboxPage — the built-in Process Inbox, an Outlook-style master-detail (the Harmonia counterpart
 * of the dashboard Process Inbox, #6064/#6068). The current user's BPM tasks (assignee + candidate
 * groups) come from the shared processTasks store; selecting a task shows it on the right, where a
 * candidate task is claimed first and a claimed task's form is rendered inline in an <iframe>. When
 * the form completes it posts `harmonia.form.close`; we drop the selection and re-fetch.
 */
document.addEventListener('alpine:init', () => {
  Alpine.data('inboxPage', () => ({
    ...basePage(),
    searchTerm: '',
    // Task ordering is client-side (the house style — see the entity list views' sortedItems). The
    // backend returns tasks unordered, so default to newest-first and let the user flip the direction.
    sortDir: 'desc',
    selectedId: null,
    autoRefresh: false,
    lastUpdated: null,
    busy: false,
    _timer: null,
    _onMessage: null,

    // Act-as (delegated entry): while armed, this Inbox serves the ACTING identity's assigned
    // tasks, so the real user's own tasks are not in the list. Rendering that as a plain empty
    // list is indistinguishable from "nothing to do" — a forgotten arming looked exactly like a
    // broken process (#6694) — so the page states who is armed and how much it is hiding.
    actAs: { acting: null, hiddenTasks: 0 },

    init() {
      // The store self-loads at startup; refresh on entry so the list is current.
      this.refresh();
      this._onMessage = (e) => {
        if (e && e.data && e.data.type === 'harmonia.form.close') {
          this.selectedId = null;
          this.refresh();
        }
      };
      window.addEventListener('message', this._onMessage);
    },

    destroy() {
      if (this._onMessage) window.removeEventListener('message', this._onMessage);
      this.stopAuto();
    },

    get tasks() { return Alpine.store('processTasks').tasks; },

    get filtered() {
      const q = this.searchTerm.trim().toLowerCase();
      const store = Alpine.store('processTasks');
      // Filter on what the row actually reads — the translated names — as well as the raw ones, so
      // typing what is on screen finds it in any language.
      const base = q
        ? this.tasks.filter(t => [store.taskLabel(t), t.name, t.processDefinitionName, t.processInstanceBusinessKey, t.assignee]
            .some(v => v && String(v).toLowerCase().includes(q)))
        : this.tasks;
      return this.sortByTime(base);
    },

    // Order by task creation time (a copy — the store's list is shared with the notification bell).
    // A task missing a createTime sinks to the bottom either way, rather than jumping to the top.
    sortByTime(list) {
      const dir = this.sortDir === 'asc' ? 1 : -1;
      return [...list].sort((a, b) => {
        const ta = a.createTime ? new Date(a.createTime).getTime() : 0;
        const tb = b.createTime ? new Date(b.createTime).getTime() : 0;
        return (ta - tb) * dir;
      });
    },

    toggleSort() {
      this.sortDir = this.sortDir === 'desc' ? 'asc' : 'desc';
      this.refreshIcons();
    },

    get selected() { return this.tasks.find(t => t.id === this.selectedId) || null; },
    isSelected(task) { return task.id === this.selectedId; },
    select(task) { this.selectedId = task.id; this.refreshIcons(); },

    // The standalone form URL (task.formKey) + the task context it needs to complete itself.
    formSrc(task) {
      if (!task || !task.formKey) return '';
      const sep = task.formKey.indexOf('?') >= 0 ? '&' : '?';
      return task.formKey + sep + 'taskId=' + encodeURIComponent(task.id)
           + '&processInstanceId=' + encodeURIComponent(task.processInstanceId || '');
    },

    async claim(task) {
      this.busy = true;
      try {
        await App.services.api.post('/services/inbox/tasks/' + task.id, { action: 'CLAIM' }, { baseUrl: '' });
        await this.refresh();
        this.selectedId = task.id; // keep the selection after the list re-fetches
      } catch (e) {
        console.error('inbox: unable to claim task', e);
      } finally {
        this.busy = false;
      }
    },

    async loadActAs() {
      try {
        const state = await App.services.api.get('/services/inbox/act-as', { baseUrl: '' });
        this.actAs = { acting: state.actingAs || null, hiddenTasks: state.hiddenTasks || 0 };
      } catch (e) {
        console.error('inbox: unable to load the act-as state', e);
      }
    },

    // Exiting reloads: the whole shell (banner, personal pages, hosted apps) resolves under the
    // armed identity, so a partial in-place update would leave half the UI acting as someone else.
    async exitActAs() {
      const res = await fetch('/services/core/actas', { method: 'DELETE', credentials: 'same-origin' });
      if (res.ok) window.location.reload();
    },

    async refresh() {
      await this.loadActAs();
      await Alpine.store('processTasks').refresh();
      this.lastUpdated = new Date();
      // A completed task drops out of the list — clear a stale selection.
      if (this.selectedId && !this.tasks.some(t => t.id === this.selectedId)) this.selectedId = null;
      this.refreshIcons();
    },

    toggleAuto() {
      this.autoRefresh = !this.autoRefresh;
      if (this.autoRefresh) {
        this._timer = setInterval(() => {
          // Self-clean if the view was swapped out (no Alpine destroy hook for intervals).
          if (!this.$root || !this.$root.isConnected) return this.stopAuto();
          // Server gone — processTasks already stopped its own poll; stop auto-refresh too (refresh to resume).
          if (Alpine.store('processTasks').serverUnavailable) return this.stopAuto();
          this.refresh();
        }, 15000);
      } else {
        this.stopAuto();
      }
    },

    stopAuto() { if (this._timer) { clearInterval(this._timer); this._timer = null; } },

    fmtTime(t) {
      if (!t) return '';
      try { return new Date(t).toLocaleString(); } catch (_) { return String(t); }
    },
  }));
}, { once: true });
