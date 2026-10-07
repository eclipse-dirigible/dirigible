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
 *
 * Every re-fetch compares the task set per process with the previous one and raises a window event
 * `harmonia:tasks-changed` naming the processes that differ (basePage.onTasksChanged is the consumer),
 * and a completed task form is followed by a bounded series of re-fetches, because the chain behind the
 * task keeps writing the record after the completion returned (#7612).
 */
document.addEventListener('alpine:init', () => {
  // Records already fetched while building subject lines, keyed by '<controller url>/<id>'. Deliberately
  // outside the store: it is a request cache, not view state, and several tasks of the same document (or
  // several documents of the same customer) must share one fetch rather than one each.
  //
  // It is BOUNDED in both directions, because a shell stays open for a working day: an entry older than
  // the TTL is re-fetched (so a record edited elsewhere stops answering with the value it had this
  // morning), and the map never holds more than MAX entries (least recently used dropped first), so the
  // memory a long session holds is a function of the cap, not of how many documents were ever inspected.
  const RECORD_CACHE_TTL_MS = 60000;
  const RECORD_CACHE_MAX = 200;
  const recordCache = new Map();

  const cacheKey = (url, id) => url + '/' + id;

  const fetchRecord = (url, id) => {
    const key = cacheKey(url, id);
    const cached = recordCache.get(key);
    if (cached && (Date.now() - cached.at) < RECORD_CACHE_TTL_MS) {
      // A Map iterates in insertion order, so re-inserting a hit makes it the most recently used and the
      // eviction below always drops the coldest entry.
      recordCache.delete(key);
      recordCache.set(key, cached);
      return cached.promise;
    }
    const promise = App.services.api.get(url + '/' + encodeURIComponent(id), { baseUrl: '' })
      .catch(() => null);   // unreachable or not permitted: the subject simply omits what it cannot read
    recordCache.delete(key);
    recordCache.set(key, { at: Date.now(), promise });
    while (recordCache.size > RECORD_CACHE_MAX) recordCache.delete(recordCache.keys().next().value);
    return promise;
  };

  // The cache keys each task's subject line was built from — the record itself plus every relation target
  // a label was read from — keyed by task id, since the task objects themselves are replaced on every
  // load. Dropping exactly those keys is what lets a task whose record just changed re-read it while
  // every other task keeps its warm entry.
  const subjectKeys = new Map();

  const forgetTask = (taskId) => {
    (subjectKeys.get(taskId) || []).forEach((key) => recordCache.delete(key));
    subjectKeys.delete(taskId);
  };

  // The process instances whose task set differs between two readings of the inbox - a task completed
  // (gone), the next wait state reached (a new one appeared) - compared by task id, so a re-read that
  // changed nothing announces nothing.
  const changedProcesses = (before, after) => {
    const ids = (tasks) => (tasks || []).map((t) => t.id).sort().join(',');
    const changed = [];
    new Set([...Object.keys(before), ...Object.keys(after)]).forEach((processInstanceId) => {
      if (ids(before[processInstanceId]) !== ids(after[processInstanceId])) changed.push(processInstanceId);
    });
    return changed;
  };

  // Tell the open page that these processes' records may have changed (basePage.onTasksChanged listens,
  // filtered by the record's own ProcessId, so a change in someone else's documents re-reads nothing).
  const announceTasksChanged = (processInstanceIds) => {
    window.dispatchEvent(new CustomEvent('harmonia:tasks-changed', { detail: { processInstanceIds } }));
  };

  // How long after a completed task form closed the inbox is re-read and the record's page told to look
  // again (cumulative: 2 s, 5 s, 10 s, 20 s). The chain behind a user task - number stamping, the status,
  // the totals, the snapshot copy - runs behind Flowable's async boundaries, so the re-read the completion
  // itself triggers can run before the chain has written (#7612). Bounded: a process that ends without a
  // further task never reports "done", so this is the only way its last writes are picked up.
  const SETTLE_DELAYS_MS = [2000, 3000, 5000, 10000];

  Alpine.store('processTasks', {
    byProcessId: {},
    tasks: [],          // flat list of all the user's tasks (assignee + groups) — the Inbox view
    loaded: false,
    formOpen: false,
    formUrl: '',
    formTitle: '',
    formTitleKey: '',   // the open task's translation key; '' for a process that declares no catalog
    formTaskId: '',     // the open task, so closing the form re-reads exactly the record it wrote
    formProcessInstanceId: '',   // the open task's process, so the record's page is told to re-read once the form closed
    subjects: {},       // taskId -> the resolved business-identity line; '' while unresolved or when there is none
    serverUnavailable: false,   // set once the backend is unreachable; stops the poll until a reload
    _poll: null,
    _settle: null,      // the pending follow-up re-read after a completed task form closed (see settle)

    init() {
      this.load();
      // A task form (opened in the app-wide dialog) asks its host to close when it completes.
      window.addEventListener('message', (e) => {
        if (e && e.data && e.data.type === 'harmonia.form.close' && this.formOpen) this.closeForm(true);
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
      if (this.serverUnavailable) return [];
      try {
        // A personal shell (`taskScope: 'assignee'`) serves strictly the person's own work, so it asks
        // only for the tasks assigned to them. The back-office group queues a user's ROLES make them a
        // candidate for - Credit Note Confirm, Journal Entry Post - belong to the back-office shell;
        // listing them next to an employee's own Submit tasks made the Personal Inbox a second, wider
        // back office (#7077). The scope is a display choice: the server gates every task either way.
        const personalOnly = (window.App && App.config && App.config.taskScope) === 'assignee';
        const [mine, groups] = await Promise.all([
          App.services.api.get('/services/inbox/tasks?type=assignee&limit=100', { baseUrl: '' }),
          personalOnly ? Promise.resolve([])
            : App.services.api.get('/services/inbox/tasks?type=groups&limit=100', { baseUrl: '' }),
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
        // The first reading is a baseline, not a change: the page that is open loaded its own record.
        const changed = this.loaded ? changedProcesses(this.byProcessId, map) : [];
        this.byProcessId = map;
        this.tasks = flat;
        this.loaded = true;
        this.loadTaskLabelCatalogs(flat);
        this.resolveSubjects(flat);
        // Surface the user's actionable tasks in the shell's notification bell.
        const notifications = Alpine.store('notifications');
        if (notifications && notifications.syncTasks) notifications.syncTasks(flat);
        if (changed.length) announceTasksChanged(changed);
        return changed;
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
          return [];
        }
        this.byProcessId = {};
        this.tasks = [];
        console.error('processTasks: unable to load inbox tasks', e);
        return [];
      }
    },

    // An explicit refresh — the Inbox's Refresh button — re-reads the records the subject lines are built
    // from, which is what makes it authoritative. Everything automatic (the 30s poll, the Inbox's 15s
    // auto-refresh) goes through load() instead and only resolves the tasks it has not seen yet, so a
    // shell sitting open does not re-fetch every document, and the subject lines do not blank out and
    // repopulate on every cycle (#7157).
    refresh() {
      recordCache.clear();
      subjectKeys.clear();
      this.subjects = {};
      return this.load();
    },

    // The targeted counterpart: the record behind ONE task has just been written (its form completed), so
    // that task alone re-resolves and every other subject stays warm.
    invalidate(task) {
      if (!task) return;
      forgetTask(task.id);
      delete this.subjects[task.id];
    },

    /**
     * The business identity of the record a task is about - the document's number, its counterparty and
     * its total - for the row that lists the task away from its own application. Before this, a row
     * carried only the BPM business key ('Ref 6', the record id), so the approver had to open every task
     * to learn what they were approving (#7077).
     *
     * The task carries LOCATORS, not values (see the TaskSubject DTO): the record's REST URL and the
     * properties that identify it. They are resolved here, live, the same way the task form resolves the
     * record it edits - a subject stamped when the process started would state the total of a document
     * whose lines are added afterwards.
     */
    subject(task) {
      return (task && this.subjects[task.id]) || '';
    },

    async resolveSubjects(tasks) {
      const current = new Set(tasks.map((t) => t.id));
      Object.keys(this.subjects).forEach((id) => {
        if (current.has(id)) return;
        delete this.subjects[id];
        subjectKeys.delete(id);   // the records stay cached (another task may share them); they age out on TTL
      });
      const pending = tasks.filter((t) => t.subject && this.subjects[t.id] === undefined);
      if (!pending.length) return;
      await Promise.all(pending.map((t) => this.resolveSubject(t)));
      // The bell bakes an item's text in when it arrives, so it is re-titled once the subjects are in.
      const notifications = Alpine.store('notifications');
      if (notifications && notifications.syncTasks) notifications.syncTasks(this.tasks);
    },

    async resolveSubject(task) {
      this.subjects[task.id] = '';   // claim it, so a concurrent load does not resolve the same task twice
      try {
        const declared = task.subject;
        const keys = [cacheKey(declared.url, declared.id)];
        const record = await fetchRecord(declared.url, declared.id);
        subjectKeys.set(task.id, keys);
        if (!record) return;
        const parts = [];
        for (const field of declared.fields) {
          const value = await this.subjectPart(field, record[field.property], keys);
          if (value) parts.push(value);
        }
        this.subjects[task.id] = parts.join(' · ');
      } catch (e) {
        console.warn('processTasks: unable to resolve the subject of task ' + task.id, e);
      }
    },

    // One property of a subject line, rendered the way the record's own application renders it.
    async subjectPart(field, value, keys) {
      if (value === null || value === undefined || value === '') return '';
      if (field.kind === 'relation') {
        if (keys) keys.push(cacheKey(field.url, value));
        const related = await fetchRecord(field.url, value);
        const label = related && related[field.label];
        return label === null || label === undefined || label === '' ? '' : String(label);
      }
      if (!window.HarmoniaFormat) return String(value);
      // A number: { stampOn: issue } reads as the draft marker until it is stamped (#7722).
      if (field.kind === 'docnumber') return String(HarmoniaFormat.documentNumber(value));
      if (field.kind === 'number') return HarmoniaFormat.number(value, null, '');
      if (field.kind === 'integer') return HarmoniaFormat.number(value, '0', '');
      if (field.kind === 'date') return HarmoniaFormat.value(value, true);
      return String(value);
    },

    // A task names its own translation key ('<project>:<model>-model.processes.<task>', minted into
    // the process definition at generation time), and the module that raised it is not necessarily
    // the one hosting this shell — the Inbox and the bell aggregate every deployed app. So the
    // namespaces the tasks actually reference are added to the translator on arrival; already
    // rendered T() bindings re-evaluate when they land.
    loadTaskLabelCatalogs(tasks) {
      if (!window.AppI18nAddNamespaces) return;
      const namespaces = [];
      tasks.forEach((t) => {
        const separator = t.nameKey ? t.nameKey.indexOf(':') : -1;
        if (separator > 0) {
          const namespace = t.nameKey.substring(0, separator);
          if (namespaces.indexOf(namespace) < 0) namespaces.push(namespace);
        }
      });
      if (namespaces.length) AppI18nAddNamespaces(namespaces);
    },

    // The display names, in the user's language. A generated process carries the i18n key of every
    // name it shows (see loadTaskLabelCatalogs); anything else — a hand-authored BPMN, an older
    // deployment — carries none and keeps rendering the raw BPMN name it always did. One rule here so
    // the Inbox, the bell, the task dialog and the in-record buttons cannot drift apart.
    taskName(task) {
      const name = (task && task.name) || 'Task';
      return window.T ? T(task && task.nameKey, name) : name;
    },

    processName(task) {
      const name = task && task.processDefinitionName;
      if (!name) return '';
      return window.T ? T(task.processDefinitionNameKey, name) : name;
    },

    // How a task reads where it is listed on its own, away from the record it belongs to.
    taskLabel(task) {
      const process = this.processName(task);
      const name = this.taskName(task);
      return process ? process + ' — ' + name : name;
    },

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
      // The dialog title is bound as T(formTitleKey, formTitle), so it keeps following the language
      // (and the catalogs, which may still be loading) rather than being resolved once here.
      this.formTitle = task.name || 'Task';
      this.formTitleKey = task.nameKey || '';
      this.formTaskId = task.id;
      this.formProcessInstanceId = task.processInstanceId || '';
      this.formOpen = true;
    },

    // Called when the task-form dialog closes; the generated task form completes the task itself,
    // so a re-fetch drops the finished task from the originating view's badge. `completed` is true when
    // the form itself asked to close (its `harmonia.form.close`, posted once the task is completed) and
    // absent when the user dismissed the dialog - only a completion has a chain to wait for.
    closeForm(completed) {
      const processInstanceId = this.formProcessInstanceId;
      // The form just wrote the record this task is about, so that one subject is re-read; the rest of
      // the inbox is untouched and keeps its cache.
      this.invalidate({ id: this.formTaskId });
      this.formOpen = false;
      this.formUrl = '';
      this.formTitleKey = '';
      this.formTaskId = '';
      this.formProcessInstanceId = '';
      // The re-read itself announces the process whose task just vanished, so the record's page re-reads
      // at once; the follow-up below catches what the chain behind the task writes after that.
      const reread = this.load();
      if (completed) reread.then(() => this.settle(processInstanceId));
    },

    // The record a task was just completed on keeps changing AFTER the completion returned (#7612): see
    // SETTLE_DELAYS_MS. Each tick re-reads the inbox and tells the record's page to look again, and stops
    // early once the process reached its next wait state - a new task of that process appeared, which the
    // re-read announces itself - because the chain up to that state has then run. A second completion
    // restarts the schedule for its own process.
    settle(processInstanceId) {
      this.cancelSettle();
      if (!processInstanceId) return;
      const tick = (index) => {
        if (index >= SETTLE_DELAYS_MS.length) return;
        this._settle = setTimeout(async () => {
          this._settle = null;
          if (this.serverUnavailable) return;
          const changed = await this.load();
          if (changed.indexOf(processInstanceId) >= 0) return;
          announceTasksChanged([processInstanceId]);
          tick(index + 1);
        }, SETTLE_DELAYS_MS[index]);
      };
      tick(0);
    },

    cancelSettle() {
      if (this._settle) {
        clearTimeout(this._settle);
        this._settle = null;
      }
    },
  });
}, { once: true });
