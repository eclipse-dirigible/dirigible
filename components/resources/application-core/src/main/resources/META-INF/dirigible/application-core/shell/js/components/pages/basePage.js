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
 * Adopted from codbex-athena-app (js/components/pages/basePage.js).
 *
 * basePage — mixin for every page component.
 *
 * Usage:
 *   Alpine.data('myPage', () => ({
 *     ...basePage(),
 *     // page-specific members
 *   }));
 */
function basePage() {
  return {
    // No-op: Lucide icons now render via the x-h-lucide directive (harmonia-lucide bundle), which
    // upgrades icons on init and inside dynamically loaded views - no manual createIcons scan needed.
    refreshIcons() {},

    /**
     * Format a floating-point value for display: decimals from the field's DecimalFormat pattern, the
     * grouping/decimal separators from the instance-wide Number setting (services/format.js).
     */
    formatNumber(value, pattern) {
      return window.HarmoniaFormat.number(value, pattern);
    },

    /**
     * Translate a column header: columns carry an optional i18next key (tkey) next to the
     * design-time label, exactly like the table headers render them.
     */
    columnHeader(col) {
      return (window.T && col.tkey) ? window.T(col.tkey, col.label) : col.label;
    },

    /**
     * Download rows as a CSV file. Values come from cellText(row, col) - the SAME resolver the
     * table cells use, so FK columns carry their referenced labels and dates their formatted
     * form, never raw ids or serialized arrays. The BOM makes Excel decode UTF-8 (Cyrillic
     * included) without an import wizard.
     */
    exportRowsCsv(rows, columns, cellText, filename) {
      const esc = (v) => {
        const s = String(v == null ? '' : v);
        return /[",\n\r]/.test(s) ? '"' + s.replace(/"/g, '""') + '"' : s;
      };
      const head = columns.map((c) => esc(this.columnHeader(c))).join(',');
      const lines = rows.map((r) => columns.map((c) => esc(cellText(r, c))).join(','));
      const blob = new Blob(['\ufeff' + [head].concat(lines).join('\r\n')], { type: 'text/csv;charset=utf-8' });
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = filename;
      a.click();
      URL.revokeObjectURL(a.href);
    },

    /**
     * Print rows as a minimal table document in a new window (the browser dialog covers paper and
     * Save as PDF). Same data path as the CSV export: the full filtered set through cellText.
     */
    printRows(rows, columns, cellText, title) {
      const esc = (v) => String(v == null ? '' : v)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
      const th = columns.map((c) =>
        '<th class="' + (c.number ? 'text-right' : '') + '">' + esc(this.columnHeader(c)) + '</th>').join('');
      const body = rows.map((r) => '<tr>' + columns.map((c) =>
        '<td class="' + (c.number ? 'text-right' : '') + '">' + esc(cellText(r, c)) + '</td>').join('') + '</tr>').join('');
      const html = '<!doctype html><html><head><title>' + esc(title) + '</title><style>'
        + 'body{font-family:system-ui,-apple-system,sans-serif;margin:24px;color:#111}'
        + 'h1{font-size:18px;margin:0 0 4px}'
        + '.meta{font-size:11px;color:#555;margin:0 0 16px}'
        + 'table{border-collapse:collapse;width:100%;font-size:12px}'
        + 'th,td{border:1px solid #999;padding:4px 8px;text-align:left}'
        + 'th{background:#eee}.text-right{text-align:right}'
        + '</style></head><body><h1>' + esc(title) + '</h1>'
        + '<p class="meta">' + rows.length + ' rows - ' + esc(new Date().toLocaleString()) + '</p>'
        + '<table><thead><tr>' + th + '</tr></thead><tbody>' + body + '</tbody></table></body></html>';
      const w = window.open('', '_blank');
      if (!w) return;
      w.document.write(html);
      w.document.close();
      w.focus();
      w.print();
    },
  };
}
