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
 *                      NARROW (4); a wide tile takes a narrow partner of its kind (chart or table),
 *                      the other narrow tiles share rows, and no row is left short.
 *   reportWeight(r)  - a report preview's weight (chart type, else column count); tableWeight(n) a table's.
 *
 * Used by the generated application's dashboard and the application shell's, so both size a tile alike.
 */
(function () {
  'use strict';

  const WIDE = 8;
  const NARROW = 4;
  const ROW = 12;
  const MAX_KPIS_PER_ROW = 4;
  const COMPACT_CHARTS = ['pie', 'doughnut', 'polararea', 'radar'];

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
   * Places content tiles into full rows of 12 columns:
   *
   * 1. each WIDE tile takes a NARROW one beside it (8 + 4) - a chart beside a chart and a table beside
   *    a table first (a line chart with its pie chart), then across kinds;
   * 2. the NARROW tiles left over share rows of three (4 + 4 + 4);
   * 3. a short last row is never stretched from one narrow tile: two narrow tiles split a row (6 + 6),
   *    and a single one left over takes a partner - from the last row of three (making 6 + 6 twice) or
   *    from a wide tile's pair (a table's before a chart's), the wide tile then filling a row alone.
   *    A wide tile with no narrow partner fills a row alone (12).
   *
   * A wide tile grows by half when it fills a row alone and uses the width; a narrow one would triple
   * and a pie chart or a two-column table would only gain margins. Rows keep the order of their first
   * tile, and the tiles in a row keep their given order. A weight other than NARROW counts as WIDE.
   *
   * @param {{weight: number, chart?: boolean}[]} tiles `weight` WIDE (8) or NARROW (4), anything else
   *        is WIDE; `chart` marks a chart (pairing prefers the same kind)
   * @returns {{tile: object, span: string}[]} the tiles in render order with their span class
   */
  function pack(tiles) {
    const entries = tiles.map((tile, index) => ({ tile, index }));
    const wides = entries.filter(e => weightOf(e.tile) === WIDE);
    let narrows = entries.filter(e => weightOf(e.tile) === NARROW);

    const pairs = [];
    const lone = [];
    const pairWide = (wide, accept) => {
      const at = narrows.findIndex(n => accept(n, wide));
      if (at < 0) return false;
      pairs.push([wide, narrows.splice(at, 1)[0]]);
      return true;
    };
    const unpaired = wides.filter(w => !pairWide(w, (n, wide) => isChart(n) === isChart(wide)));
    for (const wide of unpaired) {
      if (!pairWide(wide, () => true)) lone.push(wide);
    }

    const rows = pairs.map(pair => pair.map(e => ({ entry: e, span: weightOf(e.tile) })));
    for (const wide of lone) rows.push([{ entry: wide, span: ROW }]);

    const narrowRows = [];
    for (let i = 0; i < narrows.length; i += 3) narrowRows.push(narrows.slice(i, i + 3));
    const last = narrowRows[narrowRows.length - 1];
    if (last && last.length === 1) {
      if (narrowRows.length > 1) {
        const four = narrowRows.splice(-2).flat();
        narrowRows.push(four.slice(0, 2), four.slice(2));
      } else {
        const breakable = pairs.filter(([, narrow]) => !isChart(narrow)).pop() || pairs[pairs.length - 1];
        if (breakable) {
          rows.splice(pairs.indexOf(breakable), 1, [{ entry: breakable[0], span: ROW }]);
          narrowRows[0] = [breakable[1], last[0]];
        }
      }
    }
    for (const row of narrowRows) {
      const span = row.length === 3 ? NARROW : ROW / row.length;
      rows.push(row.map(e => ({ entry: e, span })));
    }

    return rows
      .map(row => row.sort((a, b) => a.entry.index - b.entry.index))
      .sort((a, b) => a[0].entry.index - b[0].entry.index)
      .flat()
      .map(cell => ({ tile: cell.entry.tile, span: SPAN[cell.span] }));
  }

  /**
   * The weight of a table tile: up to three columns read in a third of the row, more need two thirds.
   * An unknown count (a manifest written before it carried one) is WIDE, because a wide table in a
   * narrow cell scrolls sideways.
   *
   * @param {number} [columnCount] the columns the table shows
   * @returns {number} WIDE or NARROW
   */
  function tableWeight(columnCount) {
    return typeof columnCount === 'number' && columnCount >= 1 && columnCount <= 3 ? NARROW : WIDE;
  }

  /**
   * The weight of a report preview tile: a part-to-whole chart (pie, doughnut, polar area, radar) is
   * compact, any other chart needs width, and a table goes by its column count.
   *
   * @param {{chart?: string, columnCount?: number}} report a reports-store item
   * @returns {number} WIDE or NARROW
   */
  function reportWeight(report) {
    if (report.chart) return COMPACT_CHARTS.includes(String(report.chart).toLowerCase()) ? NARROW : WIDE;
    return tableWeight(report.columnCount);
  }

  function weightOf(tile) {
    return tile.weight === NARROW ? NARROW : WIDE;
  }

  function isChart(entry) {
    return entry.tile.chart === true;
  }

  window.DashboardLayout = { WIDE, NARROW, kpiSpans, pack, tableWeight, reportWeight };
})();
