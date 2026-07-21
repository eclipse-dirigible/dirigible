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
 *
 * A def carrying `calendar: { start, end?, title?, color?, view, range }` (a composition child
 * declared `view: calendar` in the intent) renders as an embedded x-h-calendar instead of the
 * table: the same master-filtered rows become events; event-click edits the child, date-click
 * creates one with the master FK AND the clicked date preset. An empty month is meaningful, so a
 * calendar panel shows the calendar even with zero rows.
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
    // Reactive config for the embedded x-h-calendar (calendar defs only); rebuilt on every load.
    calCfg: { view: (def.calendar && def.calendar.view) || 'month', events: [] },

    lookups: {},   // relationship column name -> { fkValue: referencedRow }

    async init() {
      await this.load();
      this.loadLookups();
    },

    // Fetch the referenced rows for each relationship column once, keyed by FK -> the whole row (so both
    // the display label AND any `via` extra columns resolve off the same fetched record).
    async loadLookups() {
      const all = {};
      for (const col of (this.def.columns || [])) {
        if (!col.lookup) continue;
        try {
          // getAll (paged), NOT get: get returns only the controller's first page (default 20), so a
          // referenced row beyond it would leave the FK unresolved and the cell would show the raw id.
          const rows = await App.services.api.getAll(col.lookup.url, { baseUrl: '' });
          const m = {};
          (rows || []).forEach(e => { m[e[col.lookup.key]] = e; });
          all[col.name] = m;
        } catch (e) {
          console.error('detailPanel: failed to load lookup for ' + col.name, e);
        }
      }
      this.lookups = all;
      // A calendar def re-renders its events once the maps arrive - a title naming a relation
      // column resolves to the referenced label instead of the raw FK id.
      if (this.def.calendar) this.calCfg = { view: this.calCfg.view, events: this.buildEvents() };
      this.refreshIcons();
    },

    // Resolve a cell: a relationship column shows its referenced label; a `via` column shows a field of
    // that same referenced row (relation `show`); a date column is formatted.
    cellValue(col, row) {
      // A `via` column reads a field off another (lookup) column's referenced row.
      if (col.via) {
        const src = this.lookups[col.via.column];
        const ref = src ? src[row[col.via.column]] : undefined;
        return this.displayValue(ref ? ref[col.via.field] : undefined, col.date);
      }
      const v = row[col.name];
      if (col.lookup) {
        const m = this.lookups[col.name];
        const ref = m ? m[v] : undefined;
        const t = ref ? ref[col.lookup.text] : undefined;
        if (t !== undefined && t !== null && t !== '') return t;
      }
      if (col.float) return this.formatNumber(v, col.pattern);
      return this.displayValue(v, col.date);
    },

    // Render a date/datetime cell value through the instance Date/Timestamp patterns (services/format.js).
    displayValue(v, isDate) {
      return window.HarmoniaFormat.value(v, isDate);
    },

    async load() {
      // No master selected yet — don't fetch (avoids a ?<fk>=null call).
      if (this.masterId == null) { this.rows = []; this.state = 'empty'; return; }
      this.state = 'loading';
      this.error = null;
      try {
        // The detail controller filters by the master FK query param (apiPath is relative to restBase).
        // getAll (paged), NOT get: get returns only the controller's first page (default 20), which
        // would silently cap a detail with more rows and hide the ones past the first page.
        const q = '?' + encodeURIComponent(this.def.masterEntityId) + '=' + encodeURIComponent(this.masterId);
        this.rows = await App.services.api.getAll(this.def.apiPath + q);
        if (this.def.calendar) {
          this.calCfg = { view: this.def.calendar.view || 'month', events: this.buildEvents() };
          this.state = 'default';
        } else {
          this.state = this.rows.length === 0 ? 'empty' : 'default';
        }
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
    // Read-only view of the detail record (the routed form page in preview mode).
    previewRow(row) {
      const q = '?returnTo=' + encodeURIComponent(this.def.returnTo);
      window.PineconeRouter.navigate('/' + this.def.entity + '/' + encodeURIComponent(row[this.def.primaryKey]) + '/preview' + q);
    },

    // --- embedded calendar (calendar defs only) -------------------------------------------------
    // Row -> event mapping, the same conventions as the standalone calendar page: Jackson java.time
    // arrays / epoch seconds / ISO strings normalize via toISO; rows with no start are skipped.
    buildEvents() {
      const cal = this.def.calendar;
      return (this.rows || []).map(row => {
        const start = this.toISO(row[cal.start]);
        if (!start) return null;
        const ev = {
          id: String(row[this.def.primaryKey]),
          title: this.eventTitle(row),
          start: start,
          allDay: cal.range ? true : this.isDateOnly(row[cal.start]),
        };
        if (cal.end) {
          const end = this.toISO(row[cal.end]);
          if (end) ev.end = end;
        }
        if (cal.color) ev.color = this.colorFor(row[cal.color]);
        return ev;
      }).filter(Boolean);
    },
    eventTitle(row) {
      const cal = this.def.calendar;
      if (cal.title) {
        const v = row[cal.title];
        // A title naming a RELATION column resolves to its referenced label, exactly like the
        // table cell does; the raw value stays the fallback for dangling FKs / unloaded maps.
        const col = (this.def.columns || []).find(c => c.name === cal.title && c.lookup);
        if (col) {
          const m = this.lookups[cal.title];
          const ref = m ? m[v] : undefined;
          const t = ref ? ref[col.lookup.text] : undefined;
          if (t !== undefined && t !== null && String(t) !== '') return String(t);
        }
        if (v !== undefined && v !== null && String(v) !== '') return String(v);
      }
      return this.def.label + ' #' + row[this.def.primaryKey];
    },
    toISO(v) {
      if (v === undefined || v === null || v === '') return '';
      if (Array.isArray(v)) {
        const p = n => String(n).padStart(2, '0');
        const date = v[0] + '-' + p(v[1]) + '-' + p(v[2]);
        if (v.length <= 3) return date;
        return date + 'T' + p(v[3] || 0) + ':' + p(v[4] || 0) + ':' + p(v[5] || 0);
      }
      if (typeof v === 'number') {
        // Jackson serializes Instant/Timestamp as epoch SECONDS; JS Date wants millis.
        const ms = v < 1e12 ? v * 1000 : v;
        try { return new Date(ms).toISOString(); } catch (e) { return ''; }
      }
      return String(v);
    },
    isDateOnly(v) {
      return Array.isArray(v) ? v.length <= 3 : (typeof v === 'string' && v.length <= 10);
    },
    // Deterministic categorical colour from the Harmonia calendar palette.
    colorFor(v) {
      const palette = ['blue', 'green', 'purple', 'orange', 'teal', 'pink', 'indigo', 'yellow', 'red', 'gray'];
      const key = (v === undefined || v === null) ? '' : String(v);
      let h = 0;
      for (let i = 0; i < key.length; i++) h = (h * 31 + key.charCodeAt(i)) >>> 0;
      return palette[h % palette.length];
    },
    // Event click -> edit the child; empty-day click -> create one with the master FK AND the
    // clicked date preset (the shared form presets any create query param whose name matches).
    onEventClick(e) {
      const id = e && e.detail && e.detail.event ? e.detail.event.id : null;
      if (!id) return;
      const q = '?returnTo=' + encodeURIComponent(this.def.returnTo);
      window.PineconeRouter.navigate('/' + this.def.entity + '/' + encodeURIComponent(id) + '/edit' + q);
    },
    onDateClick(e) {
      const cal = this.def.calendar;
      let q = '?' + encodeURIComponent(this.def.masterEntityId) + '=' + encodeURIComponent(this.masterId)
            + '&returnTo=' + encodeURIComponent(this.def.returnTo);
      const d = e && e.detail ? e.detail.date : null;
      if (d instanceof Date && !isNaN(d.getTime())) {
        const p = n => String(n).padStart(2, '0');
        let val = d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate());
        if (e.detail.time) val += 'T' + e.detail.time;
        q += '&' + encodeURIComponent(cal.start) + '=' + encodeURIComponent(val);
      }
      window.PineconeRouter.navigate('/' + this.def.entity + '/create' + q);
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
