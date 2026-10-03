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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Every generated list keeps its column widths fixed (dirigible #7467).
 *
 * <p>
 * The policy - widths from the column's kind and the list's width, never from the values; a value
 * that does not fit truncates with its full text as a tooltip; a column can be dragged wider, not
 * persisted - lives once in the shared runtime ({@code App.listColumns}). A list template that does
 * not put {@code x-list-columns} on its table silently falls back to the browser's automatic
 * layout, where one long company name widens its column and pushes the rest of the list out of view
 * - the reviewer's observation. This is the sweep that keeps a list from being left behind.
 */
class HarmoniaListColumnsIT {

    private static final String UI_BASE = "/META-INF/dirigible/template-application-ui-harmonia-java/ui/";

    /** Every generated view whose main table is an entity list. */
    private static final List<String> LIST_VIEWS = List.of("perspective/manage/list-view.html.template",
            "perspective/list/view.html.template", "my/my-list-view.html.template", "partner/partner-list-view.html.template");

    private static final Pattern FIRST_TABLE = Pattern.compile("<table\\b[^>]*>");

    private static final Pattern NO_DATA_CELL = Pattern.compile("<td\\b[^>]*messages\\.noData[^>]*>");

    /**
     * The list's table opts in; the "no data" message inside it (a single cell under the first column)
     * is exempt from truncation, or the one sentence the user reads on an empty list is cut.
     */
    @Test
    void everyListTableUsesTheFixedColumnPolicy() throws Exception {
        for (String view : LIST_VIEWS) {
            String content = read(UI_BASE + view);
            Matcher table = FIRST_TABLE.matcher(content);
            assertTrue(table.find(), view + " renders no table");
            assertTrue(table.group()
                            .contains("x-list-columns"),
                    view + ": the list table does not use x-list-columns - its columns grow with their longest value");
            // Harmonia's table reference: data-fixed is incompatible with the scroll container the lists sit
            // in.
            assertTrue(!table.group()
                             .contains("data-fixed"),
                    view + ": the list table sets Harmonia's data-fixed, which Harmonia does not support in a scroll container");
            Matcher noData = NO_DATA_CELL.matcher(content);
            while (noData.find()) {
                assertTrue(noData.group()
                                 .contains("data-col-free"),
                        view + ": the no-data message cell is not data-col-free - it would be truncated to the first column's width");
            }
        }
    }

    /** A list with a row-actions column marks its header, so it stays narrow instead of stretching. */
    @Test
    void theRowActionsHeaderIsMarked() throws Exception {
        for (String view : List.of("perspective/manage/list-view.html.template")) {
            String content = read(UI_BASE + view);
            Matcher table = FIRST_TABLE.matcher(content);
            assertTrue(table.find());
            String head = content.substring(table.end(), content.indexOf("</tr>", table.end()));
            assertEquals(1, count(head, "data-col-actions"), view + ": the row-actions header is not marked data-col-actions");
        }
    }

    /** The shared half: the directive, the tooltip, the grip, and the stylesheet that truncates. */
    @Test
    void theSharedRuntimeCarriesThePolicy() throws Exception {
        String app = read("/META-INF/dirigible/application-core/shell/js/app.js");
        assertTrue(app.contains("App.listColumns"), "the shared runtime declares no list-column policy");
        assertTrue(app.contains("Alpine.directive('list-columns'"), "x-list-columns is not registered as an Alpine directive");
        assertTrue(app.contains("data-col-grip") && app.contains("pointerdown"), "the columns cannot be dragged wider");
        assertTrue(app.contains("'title'"), "a truncated value shows no tooltip");
        assertTrue(!app.contains("localStorage"), "column widths are not persisted in v1 (#7467)");
        assertTrue(!app.contains("'data-fixed'"),
                "the policy must not switch on Harmonia's data-fixed - it is declared incompatible with scroll mode");

        String css = read("/META-INF/dirigible/application-core/shell/css/app.css");
        assertTrue(css.contains("table[data-list-columns]") && css.contains("text-overflow: ellipsis"),
                "the shared stylesheet does not truncate list cells");
    }

    private static int count(String haystack, String needle) {
        int count = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + needle.length())) {
            count++;
        }
        return count;
    }

    private static String read(String resource) throws IOException {
        try (InputStream content = HarmoniaListColumnsIT.class.getResourceAsStream(resource)) {
            assertNotNull(content, "Missing resource " + resource);
            return new String(content.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
