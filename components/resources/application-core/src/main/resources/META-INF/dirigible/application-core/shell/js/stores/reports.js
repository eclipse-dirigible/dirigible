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
 * reports store — discovers the project's standalone reports at runtime.
 *
 * Intent-generated reports are standalone `.report` files rendered (by the Harmonia report-file
 * template) as self-contained pages under gen/<reportGenFolder>/reports/<Name>/index.html — a
 * DIFFERENT gen folder than the SPA shell (gen/<modelGenFolder>/), so the shell cannot enumerate
 * them at generation time. Instead we walk the project's registry tree once and collect every
 * `.../reports/<Name>/index.html` page, exposing them as { name, url } for the sidebar Reports
 * entry (shown only when non-empty) and the Reports landing page.
 *
 * (Pragmatic discovery via the core repository API. A cleaner long-term option is for the intent to
 * surface its reports in the model the shell is generated from.)
 */
// Mirror of service-generate/template/parameterUtils.js sanitizeJavaIdentifier — the rule that maps
// a raw gen-folder name to the Java package folder the generated report controller lives under.
// Keep the two in sync: lowercase, every non-[a-z0-9_] char -> '_', digit-prefixed -> '_'-prefixed.
function sanitizeJavaIdentifier(name) {
  if (name === undefined || name === null || name === '') return '_';
  let s = String(name).toLowerCase().replace(/[^a-z0-9_]/g, '_');
  if (/^[0-9]/.test(s)) s = '_' + s;
  return s === '' ? '_' : s;
}

// "MembersWithLoansDue" -> "Members With Loans Due" (PascalCase/camelCase -> spaced Title Case).
function humanizeReportName(s) {
  return String(s || '')
    .replace(/[_-]+/g, ' ')
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/\s+/g, ' ')
    .trim()
    .replace(/^./, c => c.toUpperCase());
}

document.addEventListener('alpine:init', () => {
  Alpine.store('reports', {
    items: [],      // [{ name, label, url }]
    selected: null, // the report shown embedded on the /reports page (chosen from the sidebar)
    loaded: false,
    widgetData: {}, // KPI widget value cache, keyed by report name: { loaded, value | rows | forbidden | error }

    init() { this.load(); },

    async load() {
      try {
        // The application shell sets aggregateReports to collect reports from EVERY published app
        // (walk the whole registry); a generated app walks only its own project's gen tree.
        const aggregate = !!(App.config && App.config.aggregateReports);
        const project = (App.config && App.config.projectName) || '';
        const base = aggregate
          ? '/services/core/repository/registry/public'
          : (project ? '/services/core/repository/registry/public/' + project + '/gen' : '');
        if (!base) { this.loaded = true; return; }
        const r = await fetch(base, {
          headers: { 'Accept': 'application/json' }, credentials: 'same-origin',
        });
        if (!r.ok) { this.loaded = true; return; }
        const tree = await r.json();
        const found = [];
        this._walk(tree, found);
        const seen = new Set();
        this.items = found
          .filter(x => { if (seen.has(x.url)) return false; seen.add(x.url); return true; })
          .sort((a, b) => a.label.localeCompare(b.label));
        this.loaded = true;
        this._enrich();
      } catch (e) {
        console.error('reports: discovery failed', e);
        this.loaded = true;
      }
    },

    // Enrich each discovered report from its .report model file (project root): the human label, the
    // description (shown on the dashboard tile), and the dashboard exclude flag. Best-effort — a
    // missing/unreadable .report just leaves the defaults (label = humanized name, shown on dashboard).
    async _enrich() {
      await Promise.all(this.items.map(async (it) => {
        if (it.dashboard === undefined) it.dashboard = true;
        if (!it.project) return;
        try {
          const r = await fetch('/services/core/repository/registry/public/' + it.project + '/' + it.name + '.report', {
            headers: { 'Accept': 'application/json' }, credentials: 'same-origin',
          });
          if (!r.ok) return;
          const def = await r.json();
          if (!def) return;
          if (def.label) it.label = def.label;
          if (def.description) it.description = def.description;
          if (def.dashboard === false) it.dashboard = false;
          if (def.tId) it.tId = def.tId;
          if (def.descriptionTId) it.descriptionTId = def.descriptionTId;
          // A report-attached KPI widget: the dashboard shows a compact KPI tile (count / single
          // aggregate value / top-N list) instead of the report's iframe preview tile.
          if (def.widget) {
            it.widget = def.widget;
            it.columns = def.columns || [];
          }
        } catch (e) { /* keep defaults */ }
      }));
      // Reassign so Alpine re-renders the sidebar / dashboard with the enriched items.
      this.items = [...this.items];
      // The application shell dashboard renders report-attached KPI widgets directly from the store
      // (a generated per-app dashboard drives its own KPI loading instead — see dashboardPage), so
      // eagerly load the widget values only when aggregating across every published app.
      if (App.config && App.config.aggregateReports) this.loadWidgets();
    },

    // --- Report-attached KPI widgets on the dashboard (reports[].widget). The value is loaded once
    //     from the report's generated controller and formatted for display; shared by the standalone
    //     per-app dashboard and the application shell so both render identical KPI tiles. ---

    // Reports carrying a KPI widget and allowed on the dashboard; a role-guarded report the user may
    // not read (403 on the value fetch) is hidden, not shown as an error tile.
    kpiReports() {
      return this.items.filter(r => r.widget && r.dashboard !== false)
        .filter(r => !(this.widgetData[r.name] && this.widgetData[r.name].forbidden));
    },

    // Non-widget reports shown on the dashboard as an iframe preview tile.
    previewReports() { return this.items.filter(r => r.dashboard !== false && !r.widget); },

    // Load each KPI widget's value (memoized in widgetData); tiles render independently, an error on
    // one never blocks the rest. Pass force=true to re-pull already-loaded widgets — used when the
    // dashboard is re-entered so freshly entered records show without a full browser refresh; the
    // current tile value is kept until the new one arrives (no skeleton flash on refresh).
    async loadWidgets(force) {
      await Promise.all(this.items.filter(r => r.widget).map(async (r) => {
        if (this.widgetData[r.name] && !force) return;
        if (!this.widgetData[r.name]) this.widgetData[r.name] = { loaded: false };
        const result = await this.loadWidgetValue(r);
        this.widgetData[r.name] = { loaded: true, ...result };
      }));
    },

    widgetState(it) { return this.widgetData[it.name] || { loaded: false }; },

    // The KPI number, formatted: a value-kind widget with a decimal pattern gets the platform money
    // format; counts and pattern-less numbers render as clean integers (the DB may return 12.0).
    widgetValueText(it) {
      const d = this.widgetState(it);
      if (!d.loaded || d.error) return '';
      return this.displayNumber(d.value, (it.widget && it.widget.pattern) || '0');
    },

    // list-kind helpers: the tile shows the report's own selected columns (from the .report metadata).
    widgetColumns(it) { return (it.columns || []).filter(c => c.select === true || c.select === 'true'); },
    widgetRows(it) { return this.widgetState(it).rows || []; },
    widgetCellText(it, row, c) {
      const v = row[c.alias];
      if (c.pattern) return this.displayNumber(v, c.pattern);
      const numeric = c.type === 'INTEGER' || c.type === 'BIGINT' || c.type === 'DECIMAL';
      return numeric ? this.displayNumber(v, '0') : v;
    },
    widgetAlignClass(c) { return c.align === 'right' ? 'text-right' : ''; },

    displayNumber(v, pattern) {
      if (v === null || v === undefined || v === '') return '';
      const n = Number(v);
      if (isNaN(n)) return v;
      let decimals = 2;
      let groupSep = ' ';
      if (pattern) {
        const dot = pattern.lastIndexOf('.');
        decimals = dot >= 0 ? (pattern.length - dot - 1) : 0;
        groupSep = pattern.indexOf(' ') >= 0 ? ' ' : (pattern.indexOf(',') >= 0 ? ',' : '');
      }
      const parts = n.toFixed(decimals).split('.');
      if (groupSep) parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, groupSep);
      return parts.length > 1 ? parts[0] + '.' + parts[1] : parts[0];
    },

    // Translated display labels. Reports ship per-project catalogs under the '<Name>-report'
    // translation prefix (the report template's translate action); the fully-qualified i18next key
    // is '<project>:<Name>-report.t.<tId>'. In the default language the baked label wins (window.T's
    // contract), and without the i18n service (or a tId off an older .report) the raw label renders.
    tkey(it, tId) { return it.project + ':' + it.name + '-report.t.' + tId; },
    displayLabel(it) {
      return (window.T && it.tId) ? T(this.tkey(it, it.tId), it.label) : it.label;
    },
    displayDescription(it) {
      return (window.T && it.descriptionTId) ? T(this.tkey(it, it.descriptionTId), it.description) : it.description;
    },
    widgetLabel(it) {
      const w = it.widget || {};
      return (window.T && w.tId) ? T(this.tkey(it, w.tId), w.label) : w.label;
    },

    // Load a KPI widget's data from the report's generated controller. Returns
    //   { value }               for kind count/value (missing data coalesces to 0),
    //   { rows }                for kind list,
    //   { forbidden: true }     when the report is role-guarded and the user lacks the role
    //                           (the tile should be hidden, not shown as an error),
    //   { error: true }         on any other failure.
    // `at` pins become typed EQ conditions over the report's output columns — the same
    // per-column filter contract the report page's filter panel uses. The `now` token is
    // resolved here, type-aware: month(x) bucket -> current YYYYMM, year(x) -> current year,
    // date column -> today ISO.
    async loadWidgetValue(it) {
      const w = it.widget;
      if (!w || !it.apiBase) return { error: true };
      try {
        const conditions = (w.at || []).map(pin => ({ column: pin.column, operator: 'EQ', value: this._pinValue(pin) }));
        if (w.kind === 'value') {
          const rows = await this._fetchJson(it.apiBase + '/search', { conditions, $limit: 1 });
          const v = rows && rows.length ? rows[0][w.valueColumn] : 0;
          return { value: v === null || v === undefined ? 0 : v };
        }
        if (w.kind === 'list') {
          const rows = await this._fetchJson(it.apiBase + '/search', { conditions, $limit: w.limit || 5 });
          return { rows: rows || [] };
        }
        // kind: count (the default) — the number of records the report yields.
        const r = conditions.length
          ? await this._fetchJson(it.apiBase + '/count', { conditions })
          : await this._fetchJson(it.apiBase + '/count');
        return { value: r && typeof r.count === 'number' ? r.count : 0 };
      } catch (e) {
        if (e && e.status === 403) return { forbidden: true };
        console.error('reports: widget load failed for ' + it.name, e);
        return { error: true };
      }
    },

    _pinValue(pin) {
      if (pin.token === 'now') {
        const d = new Date();
        if (pin.bucket === 'month') return d.getFullYear() * 100 + (d.getMonth() + 1);
        if (pin.bucket === 'year') return d.getFullYear();
        // A plain date column: today as the ISO date the typed condition parameter expects.
        return d.toISOString().slice(0, 10);
      }
      return pin.value;
    },

    // GET when no body, POST with a JSON body otherwise; throws { status } on a non-OK response.
    async _fetchJson(url, body) {
      const r = await fetch(url, {
        method: body === undefined ? 'GET' : 'POST',
        headers: body === undefined
          ? { 'Accept': 'application/json' }
          : { 'Accept': 'application/json', 'Content-Type': 'application/json' },
        credentials: 'same-origin',
        body: body === undefined ? undefined : JSON.stringify(body),
      });
      if (!r.ok) { const err = new Error('HTTP ' + r.status + ' for ' + url); err.status = r.status; throw err; }
      return r.json();
    },

    // Walk the registry tree (collections + resources); a report page is a `reports/<Name>/index.html`.
    _walk(node, out) {
      if (!node) return;
      (node.resources || []).forEach(res => {
        const path = res && res.path;
        // Skip the `bin/` build-output mirror (client-Java build copies gen/ under bin/), or the same
        // report page is discovered twice and listed twice.
        if (res && res.name === 'index.html' && path && path.indexOf('/reports/') >= 0 && path.indexOf('/bin/') < 0) {
          const m = path.match(/\/reports\/([^/]+)\/index\.html$/);
          if (m) {
          const pm = path.match(/\/registry\/public\/([^/]+)\//);
          // The generated report controller URL, derived from the page path: the Java package folder
          // is the SANITIZED gen-folder name and the perspective folder is the sanitized constant
          // 'reports' (mirrors report.js.template's apiBase). Absent when the path doesn't match the
          // gen/<folder>/reports/<Name> convention — then a KPI widget on this report can't load.
          const gm = path.match(/\/registry\/public\/([^/]+)\/gen\/([^/]+)\/reports\/([^/]+)\/index\.html$/);
          const apiBase = gm
            ? '/services/java/' + gm[1] + '/gen/' + sanitizeJavaIdentifier(gm[2]) + '/api/reports/' + gm[3] + 'Controller'
            : '';
          out.push({ name: m[1], label: humanizeReportName(m[1]), url: path.replace('/registry/public/', '/services/web/'), project: pm ? pm[1] : '', apiBase });
        }
        }
      });
      // Don't descend into a `bin` collection - it's a build-output mirror, not source of truth.
      (node.collections || []).forEach(c => { if (!c || c.name !== 'bin') this._walk(c, out); });
    },
  });
}, { once: true });
