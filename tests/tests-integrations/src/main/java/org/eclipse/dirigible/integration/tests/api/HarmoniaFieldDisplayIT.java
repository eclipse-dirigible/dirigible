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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.Test;

/**
 * The read-only cards of the generated forms and documents - the Details card beside the header and
 * the frozen header card of an immutable document - print each value through the shared
 * {@code basePage.fieldDisplay} (issue #7522). Before it the Details card printed the raw form
 * value (an ISO timestamp, {@code true}/{@code false}, a {@code 1,3} key list), and the frozen
 * card's per-page copies of the formatter threw on a DROPDOWN (an unqualified {@code options}
 * reference).
 *
 * <p>
 * The helper itself is exercised by evaluating the shipped {@code format.js} and
 * {@code basePage.js} in a polyglot context; every view template carrying such a card is then
 * checked to route its values through it.
 */
class HarmoniaFieldDisplayIT {

    private static final String SHELL = "/META-INF/dirigible/application-core/shell/js/";

    private static final String UI_BASE = "/META-INF/dirigible/template-application-ui-harmonia-java/ui/";

    /** Every generated view carrying a read-only card. */
    private static final List<String> VIEWS = List.of("perspective/manage/form-view.html.template",
            "perspective/document/document-view.html.template", "my/my-form-view.html.template", "my/my-document-view.html.template",
            "partner/partner-form-view.html.template", "partner/partner-document-view.html.template");

    /**
     * The record page prints its values through its own {@code fieldText} (issue #7491), which goes
     * through the shared helper since #8367 and adds the em dash for an empty value.
     */
    private static final String RECORD_PAGE = "perspective/manage/form-view.html.template";

    /** The record page's rail, from its heading to the history block (or the end of the rail). */
    private static final String RAIL_START = "x-text=\"T('$projectName:${tprefix}.defaults.details', 'Details')\"";

    private static final String OPTIONS = "[{ value: 1, text: 'Draft' }, { value: 3, text: 'Sent' }]";

    @Test
    void anOptionFieldShowsItsLabels() {
        try (Context context = load()) {
            assertEquals("Sent", display(context, "3, 'DROPDOWN', '', " + OPTIONS));
            assertEquals("Draft", display(context, "'1', 'DOCUMENT_STATUS', '', " + OPTIONS));
            assertEquals("Draft, Sent", display(context, "[1, 3], 'MULTISELECT', '', " + OPTIONS));
            assertEquals("Draft, Sent", display(context, "'1,3', 'MULTISELECT', '', " + OPTIONS));
            assertEquals("7", display(context, "7, 'DROPDOWN', '', null"), "an unresolved key falls back to itself");
        }
    }

    @Test
    void anEmptyValueIsEmptyTextSoTheCardHidesTheRow() {
        try (Context context = load()) {
            assertEquals("", display(context, "null, 'TEXTBOX', '', null"));
            assertEquals("", display(context, "undefined, 'DATE', '', null"));
            assertEquals("", display(context, "'', 'NUMBER', '#,##0.00', null"));
            assertEquals("", display(context, "[], 'MULTISELECT', '', " + OPTIONS),
                    "an empty MULTISELECT is an array - a raw !== '' test lets it through as a blank row");
        }
    }

    @Test
    void aDateGoesThroughTheInstanceFormat() {
        try (Context context = load()) {
            assertEquals("06.09.2026", display(context, "'2026-09-06', 'DATE', '', null"));
            assertEquals("06.09.2026 17:02", display(context, "'2026-09-06T17:02:31', 'DATETIME-LOCAL', '', null"));
            assertEquals("06.09.2026 17:02", display(context, "[2026, 9, 6, 17, 2, 31, 0], 'DATETIME-LOCAL', '', null"));
        }
    }

    /**
     * #8367: a value shown as stored keeps its seconds - the record page used to format the date
     * input's buffer, which keeps hours and minutes only, so the instance pattern padded them with
     * {@code :00}.
     */
    @Test
    void aTimestampKeepsItsSecondsUnderASecondsPattern() {
        try (Context context = load("dd.MM.yyyy HH:mm:ss")) {
            assertEquals("30.10.2026 13:49:11", display(context, "'2026-10-30T13:49:11', 'DATETIME-LOCAL', '', null"));
            assertEquals("30.10.2026 13:49:11", display(context, "[2026, 10, 30, 13, 49, 11, 0], 'DATETIME-LOCAL', '', null"));
            assertEquals("21", display(context, "21, 'TEXTBOX', '', null"), "an integer prints as stored, without decimals");
        }
    }

    @Test
    void aFloatUsesItsPatternAndAnIntegerIsLeftAlone() {
        try (Context context = load()) {
            assertEquals("1,234.50", display(context, "1234.5, 'NUMBER', '#,##0.00', null"));
            assertEquals("1235", display(context, "1234.5, 'NUMBER', '0', null"));
            assertEquals("42", display(context, "42, 'NUMBER', '', null"), "an integer carries no pattern and must not gain decimals");
        }
    }

    @Test
    void aCheckboxShowsTheLabelOfItsState() {
        try (Context context = load()) {
            String states = "[{ value: true, text: 'Ja' }, { value: false, text: 'Nein' }]";
            assertEquals("Ja", display(context, "true, 'CHECKBOX', '', " + states));
            assertEquals("Nein", display(context, "false, 'CHECKBOX', '', " + states),
                    "false is a value, not an empty one - the row shows the translated No");
            assertEquals("No", display(context, "false, 'CHECKBOX', '', null"));
        }
    }

    @Test
    void everyReadOnlyCardPrintsThroughTheSharedHelper() throws Exception {
        for (String view : VIEWS) {
            String content = read(UI_BASE + view);
            assertTrue(
                    content.contains(RECORD_PAGE.equals(view)
                            ? "#macro(valueDisplay $property)fieldText(loadedRecord?.${property.name}, #displayArgs($property))"
                            : "#macro(valueDisplay $property)fieldDisplay(record.${property.name}, #displayArgs($property))"),
                    view + " does not define its value display over the shared fieldDisplay()");
            assertTrue(content.contains("#valueGate($property)"), view + " gates a read-only card row outside the shared helper");
            assertTrue(content.contains("x-text=\"#valueDisplay($property)"),
                    view + " renders a read-only card that does not go through the shared helper");
            assertTrue(content.contains("#macro(valueGate $property)x-show=\"#if($property.roleRead)canSee('${property.name}') && #end"),
                    view + " shows a role-scoped read-only field without asking canSee()");
            assertFalse(content.contains("x-text=\"form."), view + " prints a raw form value");
            assertFalse(content.contains("headerDisplay("), view + " still calls a per-page copy of the formatter");
        }
    }

    /**
     * #8367: what a read-only card prints is the record as the server answered it, never the input
     * buffer - whose dates are cut to the shape a date input takes. The gates may read the form: they
     * only test for emptiness.
     */
    @Test
    void everyReadOnlyCardReadsTheServedRecord() throws Exception {
        for (String view : VIEWS) {
            String content = read(UI_BASE + view);
            String macro = lineStartingWith(content, "#macro(valueDisplay $property)");
            assertFalse(macro.contains("form."), view + " prints its read-only values from the input buffer: " + macro);
            assertTrue(macro.contains(RECORD_PAGE.equals(view) ? "loadedRecord?." : "record."),
                    view + " does not print its read-only values from the served record: " + macro);
        }
        String record = read(UI_BASE + RECORD_PAGE);
        int start = record.indexOf(RAIL_START);
        assertTrue(start > 0, "the record page has no Details rail");
        int end = record.indexOf("#if($history == \"true\")", start);
        String rail = record.substring(start, end > 0 ? end : record.length());
        for (String line : rail.split("\n")) {
            if (line.contains("x-text=")) {
                assertFalse(line.substring(line.indexOf("x-text="))
                                .contains("form."),
                        "a rail row of the record page prints the input buffer: " + line.trim());
            }
        }
    }

    private static String lineStartingWith(String content, String prefix) {
        for (String line : content.split("\n")) {
            if (line.startsWith(prefix)) {
                return line;
            }
        }
        throw new IllegalStateException("No line starts with " + prefix);
    }

    private static String display(Context context, String arguments) {
        return context.eval("js", "basePage().fieldDisplay(" + arguments + ")")
                      .asString();
    }

    /**
     * Evaluate the shipped format.js and basePage.js over the stubs they touch - a {@code window} to
     * publish on and a {@code localStorage} holding a day-first instance pattern and the English number
     * separators.
     */
    private static Context load() {
        return load("dd.MM.yyyy HH:mm");
    }

    private static Context load(String dateTimePattern) {
        Context context = Context.newBuilder("js")
                                 .allowAllAccess(true)
                                 // The interpreter-only notice is written straight to the native stream,
                                 // which the forked test JVM reports as a corrupted channel.
                                 .option("engine.WarnInterpreterOnly", "false")
                                 .build();
        context.eval("js",
                "var window = this; var __stored = { 'codbex.harmonia.format.date': 'dd.MM.yyyy'," + " 'codbex.harmonia.format.datetime': '"
                        + dateTimePattern + "', 'codbex.harmonia.format.number': '#,##0.00' };"
                        + " var localStorage = { getItem: function (k) { return __stored[k] || null; } };");
        for (String script : List.of("services/format.js", "components/pages/basePage.js")) {
            try {
                context.eval("js", read(SHELL + script));
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to evaluate " + script, ex);
            }
        }
        return context;
    }

    private static String read(String resource) throws Exception {
        try (InputStream in = HarmoniaFieldDisplayIT.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
