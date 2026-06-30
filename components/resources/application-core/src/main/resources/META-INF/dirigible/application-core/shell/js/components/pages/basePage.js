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
    refreshIcons() {
      this.$nextTick(() => {
        if (window.lucide && typeof window.lucide.createIcons === 'function') {
          window.lucide.createIcons();
        }
      });
    },

    /**
     * Format a floating-point value for display using a DecimalFormat-style pattern. The number of
     * fraction digits is taken from the pattern's part after the decimal point and grouping uses the
     * pattern's separator (a space for "### ### ### ##0.00"). Empty/non-numeric values pass through.
     */
    formatNumber(value, pattern) {
      if (value === null || value === undefined || value === '') return '—';
      const n = Number(value);
      if (!isFinite(n)) return value;
      const p = pattern || '### ### ### ##0.00';
      const dot = p.lastIndexOf('.');
      const decimals = dot >= 0 ? (p.length - dot - 1) : 0;
      const groupSep = p.indexOf(' ') >= 0 ? ' ' : (p.indexOf(',') >= 0 ? ',' : '');
      const parts = n.toFixed(decimals).split('.');
      if (groupSep) parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, groupSep);
      return parts.length > 1 ? parts[0] + '.' + parts[1] : parts[0];
    },
  };
}
