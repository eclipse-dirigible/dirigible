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
 * detailPanel — generic master-detail child panel (metadata-driven, no per-detail code).
 *
 * A master page renders one of these per registered detail (App.detailsFor(master)),
 * passing the panel definition and the selected master id. The panel lists that detail's
 * rows filtered to the master via the controller's `?<masterEntityId>=<id>` query (which
 * the generated REST controller supports for MANAGE_DETAILS/LIST_DETAILS), supports delete,
 * and routes create/edit to the detail's own generated form page (FK + returnTo preset).
 *
 * `def` shape (from App.registerDetail): { entity, apiPath, masterEntityId, label,
 *   columns: [{ name }], returnTo }. apiPath is relative to App.config.restBase (the api client
 *   prepends it), so detail calls pass no baseUrl override.
 */
function detailPanel(def, masterId) {
  return {
    ...basePage(),
    def,
    masterId,
    rows: [],
    state: 'loading',     // loading | error | empty | default
    error: null,
    deleteOpen: false,
    deleteTarget: null,
    deleteBusy: false,

    lookups: {},   // relationship column name -> { fkValue: label }

    async init() {
      await this.load();
      this.loadLookups();
    },

    // Fetch the referenced rows for each relationship column once, mapping FK -> display label.
    async loadLookups() {
      const all = {};
      for (const col of (this.def.columns || [])) {
        if (!col.lookup) continue;
        try {
          const rows = await App.services.api.get(col.lookup.url, { baseUrl: '' });
          const m = {};
          (rows || []).forEach(e => { m[e[col.lookup.key]] = e[col.lookup.text]; });
          all[col.name] = m;
        } catch (e) {
          console.error('detailPanel: failed to load lookup for ' + col.name, e);
        }
      }
      this.lookups = all;
      this.refreshIcons();
    },

    // Resolve a cell: a relationship column shows its referenced label; a date column is formatted.
    cellValue(col, row) {
      const v = row[col.name];
      if (col.lookup) {
        const m = this.lookups[col.name];
        const t = m ? m[v] : undefined;
        if (t !== undefined && t !== null && t !== '') return t;
      }
      if (col.float) return this.formatNumber(v, col.pattern);
      return this.displayValue(v, col.date);
    },

    // Render a cell value. Jackson serializes java.time as arrays (LocalDate [y,m,d],
    // LocalDateTime [y,m,d,h,mi,s,ns]); show those as readable date strings.
    displayValue(v, isDate) {
      if (v === null || v === undefined || v === '') return '—';
      const p = (n) => String(n).padStart(2, '0');
      if (Array.isArray(v)) {
        if (v.length === 3) return v[0] + '-' + p(v[1]) + '-' + p(v[2]);
        if (v.length >= 5) return v[0] + '-' + p(v[1]) + '-' + p(v[2]) + ' ' + p(v[3]) + ':' + p(v[4]);
        return v.join(', ');
      }
      // A date/time-typed numeric is an epoch Instant/Timestamp (seconds or millis).
      if (isDate && typeof v === 'number') {
        const d = new Date(v < 1e11 ? v * 1000 : v);
        if (!isNaN(d.getTime())) {
          return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes());
        }
      }
      return v;
    },

    async load() {
      // No master selected yet — don't fetch (avoids a ?<fk>=null call).
      if (this.masterId == null) { this.rows = []; this.state = 'empty'; return; }
      this.state = 'loading';
      this.error = null;
      try {
        // The detail controller filters by the master FK query param (apiPath is relative to restBase).
        const q = '?' + encodeURIComponent(this.def.masterEntityId) + '=' + encodeURIComponent(this.masterId);
        this.rows = await App.services.api.get(this.def.apiPath + q);
        this.state = this.rows.length === 0 ? 'empty' : 'default';
      } catch (e) {
        this.error = App.services.apiErrors.messageFor(e, 'Could not load ' + this.def.label + '.');
        this.state = 'error';
      }
      this.refreshIcons();
    },

    // Create/edit route to the detail's own form page, carrying the FK + a returnTo back here.
    addRow() {
      const q = '?' + encodeURIComponent(this.def.masterEntityId) + '=' + encodeURIComponent(this.masterId)
              + '&returnTo=' + encodeURIComponent(this.def.returnTo);
      window.PineconeRouter.navigate('/' + this.def.entity + '/create' + q);
    },
    editRow(row) {
      const q = '?returnTo=' + encodeURIComponent(this.def.returnTo);
      window.PineconeRouter.navigate('/' + this.def.entity + '/' + encodeURIComponent(row[this.def.primaryKey]) + '/edit' + q);
    },

    askDelete(row) { this.deleteTarget = row; this.deleteOpen = true; },
    async confirmDelete() {
      if (!this.deleteTarget) return;
      this.deleteBusy = true;
      try {
        await App.services.api.delete(this.def.apiPath + '/' + encodeURIComponent(this.deleteTarget[this.def.primaryKey]));
        this.deleteOpen = false;
        this.deleteTarget = null;
        await this.load();
      } catch (e) {
        this.error = App.services.apiErrors.messageFor(e, 'Could not delete.');
      } finally {
        this.deleteBusy = false;
      }
    },
  };
}
