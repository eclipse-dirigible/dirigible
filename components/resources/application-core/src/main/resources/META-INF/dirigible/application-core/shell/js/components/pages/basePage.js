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
     * Format a floating-point value for display: decimals from the field's DecimalFormat pattern, the
     * grouping/decimal separators from the instance-wide Number setting (services/format.js).
     */
    formatNumber(value, pattern) {
      return window.HarmoniaFormat.number(value, pattern);
    },
  };
}
