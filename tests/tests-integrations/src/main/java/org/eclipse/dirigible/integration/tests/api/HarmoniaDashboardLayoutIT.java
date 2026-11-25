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
 * tiles fill rows of up to four, and the content tiles are packed first-fit into rows of twelve
 * columns (wide 8, narrow 4). The shipped file is evaluated in a polyglot context, the way
 * {@link HarmoniaDateFormatIT} does for {@code format.js}.
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
    void contentTilesArePackedFirstFitInRowsOfTwelve() {
        assertEquals("[12]", pack("N"));
        assertEquals("[6,6]", pack("NN"));
        assertEquals("[12]", pack("W"));
        assertEquals("[8,4,12]", pack("WNN"), "the second narrow tile is alone in its row and stretched");
        assertEquals("[8,4,12]", pack("WWN"), "a narrow tile joins the first wide one; the second wide one is alone");
        assertEquals("[8,4,4,8]", pack("WNNW"));
        assertEquals("[8,4,4,4,4]", pack("WNNNN"));
        assertEquals("[4,4,4,12]", pack("NNNW"));
        assertEquals("[8,4,8,4]", pack("WWNN"), "each wide tile takes the next narrow one beside it");
        assertEquals("[6,6,12]", pack("NNW"), "two narrow tiles that cannot take the wide one are stretched");
    }

    @Test
    void aLaterTileThatFitsTakesTheGapBeforeAnEarlierOneThatDoesNot() {
        assertEquals("[8,4,12]", pack("WWN"));
        assertEquals("W:0 N:2 W:1", order("WWN"), "the narrow tile is rendered second, beside the first wide one");
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

    /** Packs tiles written as W / N and returns the column numbers in packed order. */
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
                  .append(shape == 'W' ? "DashboardLayout.WIDE" : "DashboardLayout.NARROW")
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
