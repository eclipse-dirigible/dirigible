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
    serverUnavailable: false,   // set once the backend is unreachable; stops re-fetching until a reload

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
      if (!project) return;
      const point = project + '-custom-action';
      try {
        const data = await App.services.api.get(
          '/services/js/platform-core/extension-services/views.js?extensionPoints=' + encodeURIComponent(point),
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

    // Open the contributed action page in the app-wide dialog. An entity action carries the selected
    // record's primary key as `id` (the contributed page reads it from its query string), mirroring the
    // AngularJS Dialogs.showWindow({ params: { id } }) dispatch. The view knows its own primary key, so
    // it passes the id value here rather than the whole row.
    trigger(action, id) {
      if (!action || !action.path) return;
      let url = action.path;
      if (id !== undefined && id !== null && id !== '') {
        url += (url.indexOf('?') >= 0 ? '&' : '?') + 'id=' + encodeURIComponent(id);
      }
      this.dialogUrl = url;
      this.dialogTitle = action.label || 'Action';
      this.dialogOpen = true;
    },

    closeDialog() {
      this.dialogOpen = false;
      this.dialogUrl = '';
      // Let the originating view refresh after a (possibly) mutating action.
      window.dispatchEvent(new CustomEvent('harmonia:action-done'));
    },
  });
}, { once: true });
