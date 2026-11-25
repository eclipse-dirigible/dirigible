/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.Test;

/**
 * The dashboard's column spans come from {@code dashboardLayout.js}, two pure functions: the number
 * tiles fill rows of up to four, and the content tiles (wide 8, narrow 4) are packed into full rows
 * of twelve columns, a chart beside a chart and a table beside a table. The shipped file is
 * evaluated in a polyglot context, the way {@link HarmoniaDateFormatIT} does for {@code format.js}.
 */
class HarmoniaDashboardLayoutIT {

    private static final String LAYOUT_JS = "/META-INF/dirigible/application-core/shell/js/services/dashboardLayout.js";

    @Test
    void numberTilesFillEveryRowOfTheGrid() {
        assertEquals("[]", kpiSpans(0));
        assertEquals("[12]", cols(kpiSpans(1)));
        assertEquals("[6,6]", cols(kpiSpans(2)));
        assertEquals("[4,4,4]", cols(kpiSpans(3)));
        assertEquals("[3,3,3,3]", cols(kpiSpans(4)));
        assertEquals("[4,4,4,6,6]", cols(kpiSpans(5)));
        assertEquals("[4,4,4,4,4,4]", cols(kpiSpans(6)));
        assertEquals("[3,3,3,3,4,4,4]", cols(kpiSpans(7)));
        assertEquals("[3,3,3,3,3,3,3,3]", cols(kpiSpans(8)));
        assertEquals("[4,4,4,4,4,4,4,4,4]", cols(kpiSpans(9)));
    }

    @Test
    void everyRowIsFullAndAWideTileTakesANarrowPartner() {
        assertEquals("[12]", pack("W"));
        assertEquals("[8,4]", pack("WN"));
        assertEquals("[4,8]", pack("NW"), "a row keeps the given order of its tiles");
        assertEquals("[8,4,12]", pack("WWN"), "a wide tile without a narrow partner and no other to share with fills its row");
        assertEquals("[6,6]", pack("WW"), "two wide tiles without a narrow partner share a row");
        assertEquals("[6,6,12]", pack("WWW"));
        assertEquals("[6,6,6,6]", pack("WWWW"));
        assertEquals("[8,4,6,6]", pack("WWWN"), "the narrow tile pairs with the first wide one, the other two share a row");
        assertEquals("[4,8,4,4,4]", pack("NNNNW"));
    }

    @Test
    void narrowTilesShareRowsAndAreNeverLeftAloneBesideOthers() {
        assertEquals("[12]", pack("N"), "a lone narrow tile only when it is the only content tile");
        assertEquals("[6,6]", pack("NN"));
        assertEquals("[4,4,4]", pack("NNN"));
        assertEquals("[6,6,6,6]", pack("NNNN"), "three plus one becomes two plus two");
        assertEquals("[4,4,4,6,6]", pack("NNNNN"));
        assertEquals("[12,6,6]", pack("WNN"), "the wide tile fills a row so the two narrow ones can share one");
    }

    @Test
    void aChartPairsWithAChartAndATableWithATable() {
        assertEquals("[8,4]", pack("LP"), "a line chart and a pie chart share a row");
        assertEquals("L:0 P:2 W:1 N:3", order("LWPN"), "the pie joins the line chart, the narrow table the wide table");
        assertEquals("W:0 N:2 L:1", order("WLN"), "a narrow table goes to the wide table, not the line chart");
        assertEquals("L:0 P:2 W:1", order("LWP"));
        assertEquals("[8,4]", pack("WP"), "across kinds when nothing of the same kind is left");
    }

    @Test
    void aSingleNarrowTileLeftOverTakesTheNarrowTableOfAPairNotThePieChart() {
        // The dashboard of a list (N), a wide table (W), a narrow table (N), a bar chart (L), a doughnut
        // (P).
        assertEquals("N:0 N:2 W:1 L:3 P:4", order("NWNLP"));
        assertEquals("[6,6,12,8,4]", pack("NWNLP"));
        assertEquals("W:0 N:2 W:1 N:3 N:4", order("WWNNN"), "the freed wide tile is alone, as it has no wide one to share with");
        assertEquals("[8,4,12,6,6]", pack("WWNNN"));
    }

    @Test
    void aWeightOtherThanNarrowIsPackedAsWide() {
        try (Context context = load()) {
            assertEquals("[\"lg:col-span-8\",\"lg:col-span-4\",\"lg:col-span-12\"]",
                    context.eval("js", "JSON.stringify(DashboardLayout.pack([{weight: 20}, {weight: 4}, {}]).map(p => p.span))")
                           .asString(),
                    "an unknown weight neither stalls the packing nor loses its span");
        }
    }

    @Test
    void aTileWeighsWhatItShows() {
        try (Context context = load()) {
            assertEquals(
                    "4,8,4,8,8", context
                                        .eval("js",
                                                "[{chart: 'Doughnut'}, {chart: 'line'}, {columnCount: 3}, {columnCount: 7}, {}]"
                                                        + ".map(r => DashboardLayout.reportWeight(r)).join()")
                                        .asString(),
                    "a part-to-whole chart and a table of up to three columns are narrow, an unknown count wide");
            assertEquals("8,4,8", context.eval("js", "[0, 2, 4].map(n => DashboardLayout.tableWeight(n)).join()")
                                         .asString());
        }
    }

    @Test
    void theSpansAreLiteralClassStrings() {
        try (Context context = load()) {
            assertEquals("lg:col-span-4", context.eval("js", "DashboardLayout.kpiSpans(3)[0]")
                                                 .asString());
            assertEquals("lg:col-span-12", context.eval("js", "DashboardLayout.pack([{weight: 4}])[0].span")
                                                  .asString());
        }
    }

    private static String kpiSpans(int count) {
        try (Context context = load()) {
            return context.eval("js", "JSON.stringify(DashboardLayout.kpiSpans(" + count + "))")
                          .asString();
        }
    }

    /** Spans as plain column numbers, so the expectations read as the algorithm states them. */
    private static String cols(String spans) {
        return spans.replace("\"lg:col-span-", "")
                    .replace("\"", "");
    }

    /**
     * Packs tiles written as W (wide table), N (narrow table), L (wide chart, a line) and P (narrow
     * chart, a pie) and returns the column numbers in packed order.
     */
    private static String pack(String shapes) {
        try (Context context = load()) {
            return cols(context.eval("js", tiles(shapes) + "JSON.stringify(DashboardLayout.pack(t).map(p => p.span))")
                               .asString());
        }
    }

    /** The packed render order: shape and original index. */
    private static String order(String shapes) {
        try (Context context = load()) {
            return context.eval("js", tiles(shapes) + "DashboardLayout.pack(t).map(p => p.tile.id).join(' ')")
                          .asString();
        }
    }

    private static String tiles(String shapes) {
        StringBuilder script = new StringBuilder("var t = [");
        for (int i = 0; i < shapes.length(); i++) {
            char shape = shapes.charAt(i);
            script.append("{id: '")
                  .append(shape)
                  .append(':')
                  .append(i)
                  .append("', weight: ")
                  .append(shape == 'W' || shape == 'L' ? "DashboardLayout.WIDE" : "DashboardLayout.NARROW")
                  .append(", chart: ")
                  .append(shape == 'L' || shape == 'P')
                  .append("},");
        }
        return script.append("];")
                     .toString();
    }

    private static Context load() {
        Context context = Context.newBuilder("js")
                                 .allowAllAccess(true)
                                 .option("engine.WarnInterpreterOnly", "false")
                                 .build();
        context.eval("js", "var window = this;");
        try (InputStream in = HarmoniaDashboardLayoutIT.class.getResourceAsStream(LAYOUT_JS)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + LAYOUT_JS);
            }
            context.eval("js", new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to evaluate " + LAYOUT_JS, ex);
        }
        return context;
    }
}
