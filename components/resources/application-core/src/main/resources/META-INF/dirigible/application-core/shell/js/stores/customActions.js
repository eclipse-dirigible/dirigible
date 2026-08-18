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
 * customActions - surfaces developer-contributed, per-view on-demand action buttons in the generated
 * Harmonia views. It is the Alpine counterpart of the AngularJS templates' `<project>-custom-action`
 * extension mechanism: any extension contributed to the `<project>-custom-action` extension point (a
 * view descriptor exporting at least { id, label, path } plus { perspective, view, type, order }) is
 * fetched once from the shared extension-services endpoint and surfaced on the view it targets.
 *
 * A descriptor's `type` decides where its button renders:
 *   'page' (or omitted) - a toolbar button acting on the whole view
 *   'entity'            - a per-record button that needs a selected row (its id is passed to the page)
 *
 * A descriptor's action decides what the button does: an `endpoint` descriptor POSTs the selected id to
 * a server controller (a create-from / generate action) and toasts the result; a `path` descriptor opens
 * the page in the app-wide dialog.
 *
 * It is a global Alpine store so every generated view reads it the same way:
 *   $store.customActions.getActions(perspective, view, 'page')    -> the view's toolbar actions
 *   $store.customActions.getActions(perspective, view, 'entity')  -> the view's per-record actions
 *   $store.customActions.trigger(action, id)                      -> open the action page in the
 *                                                                    app-wide dialog (an entity action
 *                                                                    passes the record id as ?id=)
 * The action page opens in the dialog wired in index.html; on its `harmonia.form.close` message (or an
 * explicit close) the store closes it and raises a `harmonia:action-done` window event, so a view that
 * a mutating action changed (duplicate / create-from / aggregate) can refresh without a manual reload.
 */
document.addEventListener('alpine:init', () => {
  Alpine.store('customActions', {
    actions: [],        // the flat list of contributed action descriptors for this app
    loaded: false,
    dialogOpen: false,
    dialogUrl: '',
    dialogTitle: '',
    // Confirm-before-run state for `endpoint` actions: a generate/create-from mutates data the moment
    // it is POSTed, so the shell interposes an explicit confirmation the user can cancel - a bare
    // button click must never be the point of no return.
    confirmOpen: false,
    confirmAction: null,
    confirmId: null,
    confirmBusy: false,
    // Prompt-before-run state (issue #6685): a generate action may declare a small input form
    // (`prompt` on the descriptor) - the values the target needs that cannot be derived from the
    // source (which payment, how much). The dialog's controls are resolved AT RUNTIME from the
    // target entity's own detail registration (App.detailsFor), so the target's dropdown lookups,
    // dependsOn cascades and patterns apply unchanged - the descriptor carries only the authored
    // property names. The dialog replaces the plain confirm: submitting it IS the confirmation.
    promptOpen: false,
    promptAction: null,
    promptId: null,
    promptReg: null,      // the target's detail registration (master FK name, editColumns)
    promptCols: [],       // the prompted subset of the registration's editColumns
    promptValues: {},     // col.name -> the user's input
    promptOptions: {},    // col.name -> dropdown options (dependsOn-filtered when a cascade is active)
    promptBusy: false,
    promptError: '',
    serverUnavailable: false,   // set once the backend is unreachable; stops re-fetching until a reload
    extraPoints: [],            // extension points added via addProjects (the shared shell's hosted apps)

    init() {
      this.load();
      // An action page (opened in the app-wide dialog) asks its host to close when it is done.
      window.addEventListener('message', (e) => {
        if (e && e.data && e.data.type === 'harmonia.form.close' && this.dialogOpen) this.closeDialog();
      });
      // Re-read the contributions on navigation so a newly published action shows without a full reload.
      document.addEventListener('pinecone:end', () => { if (this.loaded) this.load(); });
    },

    async load() {
      // Once the server has gone away we stop re-fetching; a browser refresh recreates this store
      // (serverUnavailable back to false) and resumes.
      if (this.serverUnavailable) return;
      const project = (window.App && App.config && App.config.projectName) || '';
      // The hosting app's own point, plus every point added via addProjects: the SHARED application
      // shell hosts OTHER apps' views, so it must ask for THEIR `<project>-custom-action` points -
      // exactly as it adds their i18n namespaces - or contributed actions surface only in each
      // app's standalone shell and silently vanish from the shared one.
      const points = new Set(this.extraPoints);
      if (project) points.add(project + '-custom-action');
      if (!points.size) return;
      try {
        const query = [...points].map((p) => 'extensionPoints=' + encodeURIComponent(p)).join('&');
        const data = await App.services.api.get(
          '/services/js/platform-core/extension-services/views.js?' + query,
          { baseUrl: '' });
        this.actions = Array.isArray(data) ? data : [];
        this.loaded = true;
      } catch (e) {
        // A transport failure (httpStatus 0 -> dead server) or an auth failure (401/403) can't be helped
        // by retrying; stop until a reload. Other errors just leave the actions empty (buttons hidden).
        if (e && e.isApiError && (e.httpStatus === 0 || e.httpStatus === 401 || e.httpStatus === 403)) {
          this.serverUnavailable = true;
          console.warn('customActions: ' + (e.httpStatus === 0 ? 'server unavailable' : 'not authenticated (' + e.httpStatus + ')')
            + ' - custom action buttons unavailable until the page is reloaded');
          return;
        }
        this.actions = [];
        console.error('customActions: unable to load custom actions', e);
      }
    },

    refresh() { return this.load(); },

    // Add hosted apps' custom-action points and re-read the contributions. The shared application
    // shell calls this with the project names of the perspectives it aggregates (the same list it
    // feeds AppI18nAddNamespaces); each app's standalone shell never needs it. Idempotent: already
    // known projects are skipped, and load() always fetches the full accumulated point set, so a
    // navigation-triggered reload keeps the merged actions instead of dropping back to one app's.
    addProjects(projects) {
      const added = [...new Set((projects || [])
        .filter((p) => p && p !== 'application' && p !== 'application-core')
        .map((p) => p + '-custom-action'))]
        .filter((p) => !this.extraPoints.includes(p));
      if (!added.length) return Promise.resolve();
      this.extraPoints.push(...added);
      return this.load();
    },

    // The actions targeted at a given view. `view` is the entity name; the app-scoped extension point
    // (`<project>-custom-action`) already narrows to this app, so entity + type is unambiguous. `type`
    // is 'page' (a toolbar action on the whole view; matches page or an unset type) or 'entity' (a
    // per-record action). Contributors set `view`/`type` on the descriptor (a `perspective` field, if
    // present, stays informational). The list arrives already order-sorted from the endpoint.
    getActions(view, type) {
      return (this.actions || []).filter((a) =>
        a && a.view === view &&
        (type === 'entity' ? a.type === 'entity'
                           : (a.type === 'page' || a.type === undefined || a.type === null)));
    },

    // Trigger a contributed action. Two flavours, decided by the descriptor:
    //   - `endpoint` present: POST the selected record's id to a server endpoint (a create-from /
    //     generate action) AFTER an explicit confirmation, then toast the result and raise
    //     `harmonia:action-done`. No iframe dialog opens.
    //   - otherwise (`path`): open the contributed page in the app-wide dialog.
    // An entity action carries the selected record's primary key as `id`; the view knows its own primary
    // key so it passes the id value here rather than the whole row.
    trigger(action, id) {
      if (!action) return;
      if (action.endpoint) {
        // A declared input form (issue #6685) opens the prompt dialog instead of the plain
        // confirm; when the target's detail registration is unavailable (e.g. the shared shell,
        // which does not load the hosted apps' registrations) it degrades to the confirm below.
        if (Array.isArray(action.prompt) && action.prompt.length && this.openPrompt(action, id)) return;
        this.confirmAction = action;
        this.confirmId = (id !== undefined && id !== null && id !== '') ? id : null;
        this.confirmOpen = true;
        return;
      }
      if (!action.path) return;
      let url = action.path;
      if (id !== undefined && id !== null && id !== '') {
        url += (url.indexOf('?') >= 0 ? '&' : '?') + 'id=' + encodeURIComponent(id);
      }
      this.dialogUrl = url;
      this.dialogTitle = action.label || 'Action';
      this.dialogOpen = true;
    },

    // The confirm dialog's Run/Cancel pair. Run stays disabled while the POST is in flight so a
    // double-click cannot fire the mutation twice.
    async confirmRun() {
      if (!this.confirmAction || this.confirmBusy) return;
      this.confirmBusy = true;
      try {
        await this.runEndpoint(this.confirmAction, this.confirmId);
      } finally {
        this.confirmBusy = false;
        this.confirmOpen = false;
        this.confirmAction = null;
        this.confirmId = null;
      }
    },
    cancelConfirm() {
      if (this.confirmBusy) return;
      this.confirmOpen = false;
      this.confirmAction = null;
      this.confirmId = null;
    },

    // ----- the prompt dialog (issue #6685) -----

    // Open the input dialog for a prompted action. Returns false (so trigger() falls back to the
    // plain confirm) when the target's detail registration is not loaded in this shell - the
    // registration is what types the controls, so without it there is nothing to render.
    openPrompt(action, id) {
      const reg = (window.App && typeof App.detailsFor === 'function')
        ? (App.detailsFor(action.view) || []).find((d) => d.entity === action.promptEntity)
        : null;
      if (!reg || !Array.isArray(reg.editColumns)) {
        console.warn('customActions: no detail registration for prompt target [' + action.promptEntity
          + '] on view [' + action.view + '] - falling back to a plain confirm');
        return false;
      }
      this.promptReg = reg;
      this.promptCols = action.prompt.map((p) => {
        const col = reg.editColumns.find((c) => c.name === p.name);
        // An unregistered property still renders (as a plain text input) rather than vanishing -
        // the authored-but-silently-unconsumed failure mode is worse than an untyped control.
        return col ? { ...col, required: !!(p.required || col.required) }
                   : { name: p.name, label: p.name, widget: 'TEXT', required: !!p.required };
      });
      this.promptValues = {};
      this.promptOptions = {};
      this.promptError = '';
      this.promptAction = action;
      this.promptId = (id !== undefined && id !== null && id !== '') ? id : null;
      this.promptOpen = true;
      this.loadPromptOptions().then(() => this.seedPromptCascade());
      return true;
    },

    cancelPrompt() {
      if (this.promptBusy) return;
      this.promptOpen = false;
      this.promptAction = null;
      this.promptId = null;
      this.promptReg = null;
      this.promptCols = [];
      this.promptValues = {};
      this.promptOptions = {};
      this.promptError = '';
    },

    promptOptionsFor(name) { return this.promptOptions[name] || []; },

    // Load each prompted dropdown's option list - the full target set, narrowed by the column's
    // static `where:` filter when it declares one (the same semantics as the item dialog).
    async loadPromptOptions() {
      for (const col of this.promptCols) {
        if (col.widget !== 'DROPDOWN' || !col.lookup) continue;
        try {
          let rows;
          if (col.filter) {
            rows = await App.services.api.post(col.lookup.url + '/search', {
              conditions: [{ propertyName: col.filter.by, operator: 'EQ', value: col.filter.value }]
            }, { baseUrl: '' });
          } else {
            rows = await App.services.api.getAll(col.lookup.url, { baseUrl: '' });
          }
          this.promptOptions[col.name] = (rows || []).map((e) => ({ value: e[col.lookup.key], text: e[col.lookup.text] }));
        } catch (e) {
          console.error('customActions: failed to load prompt options for ' + col.name, e);
        }
      }
    },

    // Seed the dependsOn cascade once the dialog opens: every prompted column whose trigger value
    // is already determined (the clicked record itself, or a value reachable through the target's
    // UNPROMPTED dependsOn chain - e.g. the invoice's Customer) gets its options filtered / its
    // value defaulted before the user touches anything.
    async seedPromptCascade() {
      for (const col of this.promptCols) {
        if (!col.dependsOn || col.dependsOn.header || col.dependsOn.valueBy) continue;
        const triggerValue = await this.resolvePromptTriggerValue(col.dependsOn.property, 3);
        if (triggerValue == null || triggerValue === '') continue;
        await this.applyPromptDependsOn(col, triggerValue, true);
      }
    },

    // The current value of a dependsOn trigger property inside the prompt dialog:
    //   - a PROMPTED column's live value;
    //   - the master FK (the clicked record IS the master - a generate button is per-record);
    //   - an unprompted column reachable through its own dependsOn (valueFrom) chain, resolved by
    //     fetching the upstream record - e.g. Customer defaulting from the invoice the id points at.
    async resolvePromptTriggerValue(property, depth) {
      if (!property || depth <= 0) return null;
      const prompted = this.promptCols.find((c) => c.name === property);
      if (prompted) {
        const v = this.promptValues[property];
        return (v == null || v === '') ? null : v;
      }
      if (this.promptReg && property === this.promptReg.masterEntityId) return this.promptId;
      const chained = (this.promptReg.editColumns || []).find((c) => c.name === property);
      if (!chained || !chained.dependsOn || chained.dependsOn.header || chained.dependsOn.valueBy
          || !chained.dependsOn.valueFrom) return null;
      const upstream = await this.resolvePromptTriggerValue(chained.dependsOn.property, depth - 1);
      if (upstream == null || upstream === '') return null;
      try {
        const rec = await App.services.api.get(chained.dependsOn.url + '/' + encodeURIComponent(upstream), { baseUrl: '' });
        const v = rec == null ? null : rec[chained.dependsOn.valueFrom];
        return (v == null || v === '') ? null : v;
      } catch (e) {
        console.error('customActions: failed to resolve prompt cascade value for ' + property, e);
        return null;
      }
    },

    // Apply one dependsOn edge to a prompted column (the item dialog's applyDraftDependsOn,
    // scoped to the prompt): load the trigger's record, read valueFrom (default: its key), then
    // re-filter a dropdown's options by filterBy (auto-selecting a single match) or copy the
    // scalar as an editable default.
    async applyPromptDependsOn(col, triggerValue, adjust) {
      try {
        const rec = await App.services.api.get(col.dependsOn.url + '/' + encodeURIComponent(triggerValue), { baseUrl: '' });
        const prop = col.dependsOn.valueFrom;
        if (prop == null) return;
        const from = rec == null ? null : rec[prop];
        if (col.widget === 'DROPDOWN' && col.lookup) {
          if (from == null || from === '') return;
          const conditions = [{ propertyName: col.dependsOn.filterBy, operator: 'EQ', value: from }];
          if (col.filter) conditions.push({ propertyName: col.filter.by, operator: 'EQ', value: col.filter.value });
          const rows = await App.services.api.post(col.lookup.url + '/search', { conditions }, { baseUrl: '' });
          this.promptOptions[col.name] = (rows || []).map((e) => ({ value: e[col.lookup.key], text: e[col.lookup.text] }));
          if (adjust) {
            this.promptValues[col.name] = this.promptOptions[col.name].length === 1
              ? String(this.promptOptions[col.name][0].value) : '';
          }
        } else if (from != null) {
          this.promptValues[col.name] = from;
        }
      } catch (e) {
        console.error('customActions: prompt dependsOn refresh failed for ' + col.name, e);
      }
    },

    // A prompted control changed: re-run the cascade for every prompted column depending on it.
    async promptChanged(name) {
      const value = this.promptValues[name];
      if (value == null || value === '') return;
      for (const col of this.promptCols) {
        if (!col.dependsOn || col.dependsOn.header || col.dependsOn.valueBy) continue;
        if (col.dependsOn.property !== name) continue;
        await this.applyPromptDependsOn(col, value, true);
      }
    },

    // Validate + run: required inputs must be present (the generated controller enforces the same
    // with a 400 - the dialog check just keeps the failure local); the values are POSTed together
    // with the source id.
    async promptRun() {
      if (!this.promptAction || this.promptBusy) return;
      const missing = this.promptCols.filter((c) => c.required
        && (this.promptValues[c.name] == null || this.promptValues[c.name] === ''));
      if (missing.length) {
        this.promptError = missing.map((c) => c.label || c.name).join(', ');
        return;
      }
      this.promptError = '';
      this.promptBusy = true;
      try {
        await this.runEndpoint(this.promptAction, this.promptId, this.promptValues);
      } finally {
        this.promptBusy = false;
        this.cancelPrompt();
      }
    },

    // POST { id } to the action's endpoint (a generated create-from controller). The server clones the
    // source record into a new target record and returns it; we surface a success/error notification and
    // raise `harmonia:action-done` so the originating view can refresh. The endpoint is an absolute
    // same-origin path (it targets gen/events, not the entity api base), so we prepend no baseUrl.
    async runEndpoint(action, id, values) {
      const label = action.label || 'Action';
      const body = {};
      if (id !== undefined && id !== null && id !== '') body.id = id;
      if (values && Object.keys(values).length) body.values = values;
      try {
        const created = await App.services.api.post(action.endpoint, body, { baseUrl: '' });
        const ref = created && (created.Number || created.Name || created.Id || created.id);
        this.notify(label, ref ? 'Created ' + ref : 'Completed', 'positive');
        window.dispatchEvent(new CustomEvent('harmonia:action-done'));
      } catch (e) {
        const msg = (App.services.apiErrors && App.services.apiErrors.messageFor)
          ? App.services.apiErrors.messageFor(e, 'Action failed')
          : 'Action failed';
        this.notify(label, msg, 'negative');
      }
    },

    // Surface a notification through the shared notifications store (the shell's notification centre),
    // degrading to the console when it is unavailable so an action never fails silently.
    notify(title, description, variant) {
      try {
        const store = window.Alpine && Alpine.store('notifications');
        if (store && typeof store.add === 'function') {
          store.add({ title: title, description: description, variant: variant });
          return;
        }
      } catch (e) { /* fall through to the console */ }
      console.log('customActions: ' + title + (description ? ' - ' + description : ''));
    },

    closeDialog() {
      this.dialogOpen = false;
      this.dialogUrl = '';
      // Let the originating view refresh after a (possibly) mutating action.
      window.dispatchEvent(new CustomEvent('harmonia:action-done'));
    },
  });

  // The prompt dialog's markup wrapper (issue #6685). Two reasons it exists: the per-project shell
  // is a VELOCITY template, where any `$store.x(...)` call with arguments breaks generation - the
  // component's `s` getter lets the markup stay `$`-free; and the dependsOn cascade needs a watcher
  // on the prompt values, which a store cannot register on itself.
  Alpine.data('customActionPrompt', () => ({
    _last: {},
    get s() { return Alpine.store('customActions'); },
    init() {
      this.$watch('s.promptOpen', (open) => { if (!open) this._last = {}; });
      // Deep-watch the values: a change to any prompted control re-runs the cascade for its
      // dependents (payment picked -> amount defaults). The _last snapshot keeps programmatic
      // cascade writes from re-firing endlessly - an unchanged value never re-triggers.
      this.$watch('s.promptValues', (values) => {
        const store = this.s;
        if (!store.promptOpen) return;
        for (const col of store.promptCols) {
          const v = values ? values[col.name] : undefined;
          if (this._last[col.name] !== v) {
            this._last[col.name] = v;
            store.promptChanged(col.name);
          }
        }
      });
    },
  }));
}, { once: true });
