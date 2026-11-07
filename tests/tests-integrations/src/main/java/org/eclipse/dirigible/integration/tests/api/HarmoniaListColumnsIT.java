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

import org.graalvm.polyglot.Context;
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

    /** Every page whose tables carry a row-actions column (dirigible #8372). */
    private static final List<String> ROW_ACTIONS_PAGES = List.of(UI_BASE + "perspective/manage/list-view.html.template",
            UI_BASE + "perspective/manage/form-view.html.template", UI_BASE + "perspective/document/document-view.html.template",
            "/META-INF/dirigible/application-core/shell/views/_documents.html");

    /**
     * A cell's opening tag, quoted values skipped whole (an Alpine expression may hold a {@code >}).
     */
    private static final Pattern CELL_TAG = Pattern.compile("<(?:td|th)\\b(?:[^>\"]|\"[^\"]*\")*>");

    private static final Pattern ROW_ACTIONS_MARKER = Pattern.compile("\\sdata-(?:row|col)-actions[\\s>]");

    /**
     * A row's actions stay on screen while the columns scroll: every row-actions cell and its header
     * are pinned with Harmonia's {@code data-sticky="end"}, and each cell has its marked header.
     */
    @Test
    void everyRowActionsCellIsPinnedToTheEnd() throws Exception {
        for (String page : ROW_ACTIONS_PAGES) {
            String content = read(page);
            int cells = 0;
            int heads = 0;
            Matcher tag = CELL_TAG.matcher(content);
            while (tag.find()) {
                String element = tag.group();
                if (!ROW_ACTIONS_MARKER.matcher(element)
                                       .find()) {
                    continue;
                }
                assertTrue(element.contains("data-sticky=\"end\""), page + ": a row-actions cell is not data-sticky=\"end\": " + element);
                if (element.startsWith("<td")) {
                    cells++;
                } else {
                    heads++;
                }
            }
            assertTrue(cells > 0, page + " has no row-actions cell");
            assertEquals(cells, heads, page + ": every row-actions cell needs its data-col-actions header");
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

    /**
     * The truncation tooltip never fires on the row-actions cell (dirigible #8432). The open action
     * menu lives inside that cell, so the cell measured as cut and took "Preview Edit Delete" as a
     * native title - a second, smaller menu under the real one. The shipped {@code app.js} runs over
     * stub elements here, with every cell measuring as cut, so only the guard decides.
     */
    @Test
    void theTruncationTooltipSkipsRowActionsAndMenus() throws Exception {
        try (Context context = Context.newBuilder("js")
                                      .allowAllAccess(true)
                                      // The interpreter-only notice is written straight to the native stream,
                                      // which the forked test JVM reports as a corrupted channel.
                                      .option("engine.WarnInterpreterOnly", "false")
                                      .build()) {
            context.eval("js", """
                    var window = this;
                    var document = { addEventListener: function () {} };
                    function matches(el, selector) {
                      return selector.split(',').some(function (part) {
                        part = part.trim();
                        var attr = /^\\[([\\w-]+)(?:="?([^"\\]]*)"?)?\\]$/.exec(part);
                        if (!attr) return el.tagName === part.toUpperCase();
                        return el.attrs[attr[1]] !== undefined && (attr[2] === undefined || el.attrs[attr[1]] === attr[2]);
                      });
                    }
                    function element(tagName, attrs, parent, text) {
                      return { tagName: tagName, attrs: attrs, parentElement: parent, textContent: text,
                        hasAttribute: function (name) { return this.attrs[name] !== undefined; },
                        setAttribute: function (name, value) { this.attrs[name] = value; },
                        removeAttribute: function (name) { delete this.attrs[name]; },
                        querySelector: function () { return null; },
                        closest: function (selector) {
                          for (var el = this; el; el = el.parentElement) { if (matches(el, selector)) return el; }
                          return null;
                        } };
                    }
                    """);
            context.eval("js", read("/META-INF/dirigible/application-core/shell/js/app.js"));
            context.eval("js", """
                    App.listColumns.isCut = function () { return true; };
                    var hover = function (target) { App.listColumns.tooltip({ target: target }); };
                    var actions = element('TD', { 'data-row-actions': '' }, null, 'Preview Edit Delete');
                    var menu = element('UL', { role: 'menu' }, actions, 'Preview Edit Delete');
                    var item = element('LI', { role: 'menuitem' }, menu, 'Edit');
                    var value = element('TD', {}, null, 'A company name too long for its column');
                    """);
            context.eval("js", "hover(item); hover(menu); hover(actions);");
            assertEquals(false, context.eval("js", "actions.hasAttribute('title')")
                                       .asBoolean(),
                    "hovering the row-actions menu put its items' text on the cell as a native tooltip");
            context.eval("js", "var detached = element('LI', {}, element('UL', { role: 'menu' }, value, ''), 'Edit'); hover(detached);");
            assertEquals(false, context.eval("js", "value.hasAttribute('title')")
                                       .asBoolean(),
                    "hovering a menu inside a cell measured the cell for a tooltip");
            context.eval("js", "hover(value);");
            assertEquals("A company name too long for its column", context.eval("js", "value.attrs.title")
                                                                          .asString(),
                    "a cut value lost its tooltip");
        }
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
