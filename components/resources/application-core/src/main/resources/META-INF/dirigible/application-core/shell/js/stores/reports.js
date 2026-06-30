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
        } catch (e) { /* keep defaults */ }
      }));
      // Reassign so Alpine re-renders the sidebar / dashboard with the enriched items.
      this.items = [...this.items];
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
          out.push({ name: m[1], label: humanizeReportName(m[1]), url: path.replace('/registry/public/', '/services/web/'), project: pm ? pm[1] : '' });
        }
        }
      });
      // Don't descend into a `bin` collection - it's a build-output mirror, not source of truth.
      (node.collections || []).forEach(c => { if (!c || c.name !== 'bin') this._walk(c, out); });
    },
  });
}, { once: true });
