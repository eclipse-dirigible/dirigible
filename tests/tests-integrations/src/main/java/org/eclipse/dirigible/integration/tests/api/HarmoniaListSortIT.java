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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.Test;

/**
 * A generated list sorted by a column that is not a relation orders its rows through the shared
 * {@code basePage.compareSortValues} (dirigible #8433). A Java {@code LocalDate} the controller
 * serialised as the array {@code [2025, 10, 6]} compared as the string "2025,10,6", so a Date
 * column sorted 2025-10-16, 2025-10-24, 2025-10-06, ... - the comparator now compares a temporal
 * value by its zero-padded ISO text, whichever shape it arrived in.
 *
 * <p>
 * The comparator is exercised by evaluating the shipped {@code basePage.js} in a polyglot context,
 * sorting the rows as the generated {@code sortedItems} does.
 */
class HarmoniaListSortIT {

    private static final String BASE_PAGE = "/META-INF/dirigible/application-core/shell/js/components/pages/basePage.js";

    @Test
    void localDateArraysSortChronologically() {
        String dates =
                "[[2025,10,16],[2025,10,24],[2025,10,6],[2025,11,16],[2025,11,6],[2025,12,16],[2025,12,6],[2025,7,16],[2025,7,6],[2025,8,16]]";
        String expected =
                "2025,7,6 | 2025,7,16 | 2025,8,16 | 2025,10,6 | 2025,10,16 | 2025,10,24 | 2025,11,6 | 2025,11,16 | 2025,12,6 | 2025,12,16";
        assertEquals(expected, sorted(dates, "asc"));
        assertEquals(reversed(expected), sorted(dates, "desc"));
    }

    @Test
    void isoStringsAndDateObjectsSortChronologically() {
        assertEquals("2025-07-06 | 2025-10-06 | 2025-10-16", sorted("['2025-10-16','2025-07-06','2025-10-06']", "asc"));
        assertEquals("2025-07-06T00:00:00.000Z | 2025-10-06T00:00:00.000Z | 2025-10-16T00:00:00.000Z",
                sorted("[new Date(Date.UTC(2025,9,16)),new Date(Date.UTC(2025,6,6)),new Date(Date.UTC(2025,9,6))]", "asc"));
    }

    @Test
    void anArrayAndItsIsoTextShareOneKey() {
        try (Context context = load()) {
            assertEquals(0, context.eval("js", "page.compareSortValues([2025,10,6], '2025-10-06', 'asc')")
                                   .asInt());
            assertEquals(0, context.eval("js", "page.compareSortValues([2025,10,6,10,0], '2025-10-06T10:00:00.000000000', 'asc')")
                                   .asInt());
        }
    }

    /** Jackson drops a trailing zero second and nano, so one column mixes array lengths. */
    @Test
    void localDateTimeArraysWithDroppedSecondsSortChronologically() {
        assertEquals("2025,10,6,9,59,59 | 2025,10,6,10,0 | 2025,10,6,10,0,15 | 2025,10,6,10,0,15,500000000 | 2025,10,7,0,0",
                sorted("[[2025,10,6,10,0,15],[2025,10,7,0,0],[2025,10,6,10,0],[2025,10,6,10,0,15,500000000],[2025,10,6,9,59,59]]", "asc"));
    }

    /** A three-part LocalTime is told from a three-part LocalDate by its first part, an hour. */
    @Test
    void localTimeArraysSortChronologically() {
        assertEquals("0,30 | 9,5 | 10,0 | 10,0,15 | 10,30 | 23,59,59,1",
                sorted("[[10,30],[23,59,59,1],[10,0,15],[9,5],[10,0],[0,30]]", "asc"));
    }

    @Test
    void otherValuesKeepTheirOrder() {
        assertEquals(" | 2 | 9 | 10", sorted("[10,null,9,2]", "asc"));
        assertEquals("alpha | Beta | gamma", sorted("['gamma','alpha','Beta']", "asc"));
        assertEquals("gamma | Beta | alpha | ", sorted("['gamma',undefined,'alpha','Beta']", "desc"));
    }

    /** The values of {@code literal} sorted as the generated sortedItems sorts the rows' column. */
    private static String sorted(String literal, String dir) {
        try (Context context = load()) {
            return context.eval("js",
                    "(" + literal + ").map(v => ({ D: v }))" + ".sort((a, b) => page.compareSortValues(a.D, b.D, '" + dir + "'))"
                            + ".map(r => r.D == null ? '' : (r.D instanceof Date ? r.D.toISOString() : String(r.D))).join(' | ')")
                          .asString();
        }
    }

    private static String reversed(String joined) {
        List<String> parts = new ArrayList<>(Arrays.asList(joined.split(" \\| ")));
        Collections.reverse(parts);
        return String.join(" | ", parts);
    }

    private static Context load() {
        Context context = Context.newBuilder("js")
                                 .allowAllAccess(true)
                                 // The interpreter-only notice is written straight to the native stream,
                                 // which the forked test JVM reports as a corrupted channel.
                                 .option("engine.WarnInterpreterOnly", "false")
                                 .build();
        context.eval("js", "var window = this;");
        context.eval("js", read(BASE_PAGE));
        context.eval("js", "var page = basePage();");
        return context;
    }

    private static String read(String resource) {
        try (InputStream in = HarmoniaListSortIT.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read " + resource, ex);
        }
    }
}
