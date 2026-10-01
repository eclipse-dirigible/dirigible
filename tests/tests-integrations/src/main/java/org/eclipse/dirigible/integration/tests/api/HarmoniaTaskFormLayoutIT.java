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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * No generated Harmonia form squeezes its fields, and the Inbox keeps the inline task form readable
 * (issue #7602).
 *
 * <p>
 * A numeric column span wider than the grid it sits in does not clamp: the browser adds implicit
 * columns to fit it, and the remaining cells auto-flow into them. With {@code col-span-12} on the
 * full-width elements of the two-column BPM task form, the six read-only fields of a journal entry
 * rendered side by side, one twelfth of the width each. The generated forms are one column below a
 * breakpoint and wider above it ({@code grid-cols-1 sm:grid-cols-12}), so an unprefixed
 * {@code col-span-12} does the same on a narrow screen. A span therefore carries the prefix of the
 * grid it spans ({@code sm:col-span-2}, {@code sm:col-span-12}) - the pinned Harmonia ships no
 * {@code col-span-full} ({@link HarmoniaContractIT}).
 */
class HarmoniaTaskFormLayoutIT {

    private static final List<String> TEMPLATE_PATTERNS =
            List.of("classpath*:META-INF/dirigible/template-application-ui-harmonia-java/**/*.template",
                    "classpath*:META-INF/dirigible/template-form-builder-harmonia/**/*.template");

    private static final String INBOX = "/META-INF/dirigible/application-core/shell/views/_inbox.html";

    /**
     * A grid's column count, with its breakpoint prefix if any ({@code grid-cols-1},
     * {@code sm:grid-cols-2}).
     */
    private static final Pattern GRID_COLUMNS = Pattern.compile("(?<![\\w:-])(?:([a-z]+):)?grid-cols-(\\d+)\\b");

    /** A numeric column span, with its breakpoint prefix if any. */
    private static final Pattern NUMERIC_SPAN = Pattern.compile("(?<![\\w:-])(?:([a-z]+):)?col-span-(\\d+)\\b");

    /** The Inbox detail panel: the split panel beside the task list's 20% one. */
    private static final Pattern DETAIL_PANEL = Pattern.compile("<div x-h-split-panel data-size=\"80%\"([^>]*)>");

    private static final Pattern MINIMUM = Pattern.compile("data-min=\"(\\d+)\"");

    /** The narrowest width a one-column task form still reads at. */
    private static final int READABLE_FORM_WIDTH = 320;

    @Test
    void noSpanIsWiderThanTheGridItSpansAtItsBreakpoint() throws IOException {
        List<String> problems = new ArrayList<>();
        int templates = 0;
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        for (String pattern : TEMPLATE_PATTERNS) {
            for (Resource template : resolver.getResources(pattern)) {
                templates++;
                problems.addAll(tooWideSpans(template.getFilename(), template.getContentAsString(StandardCharsets.UTF_8)));
            }
        }
        assertTrue(templates > 20, "only " + templates + " Harmonia templates found on the classpath");
        assertTrue(problems.isEmpty(), "a span wider than its grid adds implicit columns and squeezes the fields into slivers - "
                + "prefix it with the breakpoint of the grid it spans: " + problems);
    }

    @Test
    void theInboxStopsTheSplitterWhereTheInlineFormStillReads() throws IOException {
        Matcher panel = DETAIL_PANEL.matcher(read(INBOX));
        assertTrue(panel.find(), "the Inbox detail panel is not where this test expects it");
        Matcher minimum = MINIMUM.matcher(panel.group(1));
        assertTrue(minimum.find(), "the Inbox detail panel has no data-min, so the splitter can squeeze the inline form");
        assertFalse(Integer.parseInt(minimum.group(1)) < READABLE_FORM_WIDTH,
                "the Inbox detail panel minimum is below the narrowest width a one-column task form reads at: " + minimum.group());
    }

    /**
     * The spans wider than the widest grid declared at the same breakpoint. An unprefixed span is
     * measured against the unprefixed grids, which are what a narrow screen lays out; a prefixed one
     * against the grids of its prefix, or the widest grid at all when its prefix declares none.
     */
    private static List<String> tooWideSpans(String template, String content) {
        Map<String, Integer> columns = new HashMap<>();
        Matcher grid = GRID_COLUMNS.matcher(content);
        while (grid.find()) {
            columns.merge(prefixOf(grid.group(1)), Integer.parseInt(grid.group(2)), Math::max);
        }
        List<String> problems = new ArrayList<>();
        if (columns.isEmpty()) {
            return problems;
        }
        int widest = columns.values()
                            .stream()
                            .max(Integer::compare)
                            .orElseThrow();
        Matcher span = NUMERIC_SPAN.matcher(content);
        while (span.find()) {
            String prefix = prefixOf(span.group(1));
            Integer limit = columns.get(prefix);
            if (limit == null) {
                limit = prefix.isEmpty() ? null : widest;
            }
            if (limit != null && Integer.parseInt(span.group(2)) > limit) {
                problems.add(template + ": " + span.group() + " in a " + limit + "-column grid");
            }
        }
        return problems;
    }

    private static String prefixOf(String breakpoint) {
        return breakpoint == null ? "" : breakpoint;
    }

    private static String read(String resource) throws IOException {
        try (InputStream in = HarmoniaTaskFormLayoutIT.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
