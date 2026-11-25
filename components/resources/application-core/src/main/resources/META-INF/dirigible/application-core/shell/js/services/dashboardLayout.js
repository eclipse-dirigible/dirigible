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
/*
 * dashboardLayout - the column spans of the generated home dashboard (pure functions, no DOM).
 *
 * The dashboard is a 12-column grid at the `lg` breakpoint. Tiles are given as spans from a fixed
 * table of literal class strings, so the class names appear verbatim in source (the stylesheet only
 * ships what is written out; nothing is assembled from a number at run time).
 *
 *   kpiSpans(count)  - the number tiles: rows of at most four, filled evenly, so a row has no hole.
 *   pack(tiles)      - the content tiles (list, page, report preview): each weighs WIDE (8 columns) or
 *                      NARROW (4), and is placed first-fit in order into rows of 12.
 */
(function () {
  'use strict';

  const WIDE = 8;
  const NARROW = 4;
  const ROW = 12;
  const MAX_KPIS_PER_ROW = 4;

  // Span class by number of columns.
  const SPAN = {
    3: 'lg:col-span-3',
    4: 'lg:col-span-4',
    6: 'lg:col-span-6',
    8: 'lg:col-span-8',
    12: 'lg:col-span-12',
  };
  // Span class of a KPI tile in a row of k tiles (12 / k columns each).
  const KPI_SPAN_BY_ROW_SIZE = { 1: SPAN[12], 2: SPAN[6], 3: SPAN[4], 4: SPAN[3] };

  /**
   * Spans for `count` number tiles. They are split into ceil(count / 4) rows, distributed evenly with
   * the earlier rows taking the extra tile (5 -> 3 + 2, 7 -> 4 + 3, 9 -> 3 + 3 + 3), and a tile in a
   * row of k gets 12 / k columns.
   *
   * @param {number} count how many tiles
   * @returns {string[]} one `lg:col-span-N` class per tile, in order
   */
  function kpiSpans(count) {
    const spans = [];
    if (!(count > 0)) return spans;
    const rows = Math.ceil(count / MAX_KPIS_PER_ROW);
    const base = Math.floor(count / rows);
    const extra = count % rows;
    for (let row = 0; row < rows; row++) {
      const size = base + (row < extra ? 1 : 0);
      for (let i = 0; i < size; i++) spans.push(KPI_SPAN_BY_ROW_SIZE[size]);
    }
    return spans;
  }

  /**
   * Places content tiles into rows of 12 columns, first fit: the next tile in order starts a row and
   * the row then takes the first later tile that still fits, until none does. A row that closes with
   * free space is stretched: a lone tile takes the whole row, two NARROW tiles take half each. (8 + 4,
   * 4 + 8 and 4 + 4 + 4 are the only other row shapes, and they are already full.) A weight other than
   * NARROW counts as WIDE, so every tile fits an empty row and the packing always ends.
   *
   * @param {{weight: number}[]} tiles each with `weight` WIDE (8) or NARROW (4); anything else is WIDE
   * @returns {{tile: object, span: string}[]} the tiles in packed (render) order with their span class
   */
  function pack(tiles) {
    const queue = tiles.slice();
    const packed = [];
    while (queue.length) {
      const row = [];
      let used = 0;
      for (;;) {
        const at = queue.findIndex(t => used + weightOf(t) <= ROW);
        if (at < 0) break;
        const tile = queue.splice(at, 1)[0];
        row.push(tile);
        used += weightOf(tile);
      }
      for (const tile of row) packed.push({ tile, span: SPAN[spanOf(row, tile)] });
    }
    return packed;
  }

  function weightOf(tile) {
    return tile.weight === NARROW ? NARROW : WIDE;
  }

  function spanOf(row, tile) {
    if (row.length === 1) return ROW;
    if (row.length === 2 && row.every(t => weightOf(t) === NARROW)) return ROW / 2;
    return weightOf(tile);
  }

  window.DashboardLayout = { WIDE, NARROW, kpiSpans, pack };
})();
