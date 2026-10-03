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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.Test;

/**
 * The shared Harmonia runtime formats every date it PRINTS against the instance Date / Timestamp
 * patterns; this covers the other half - what a date INPUT shows and accepts.
 *
 * <p>
 * A Harmonia date picker left unconfigured displays and parses in the document's language, or
 * absent one the browser's, which is a silent corruption rather than a cosmetic mismatch:
 * {@code 06.09.2026} typed into an m/d/yyyy field is a perfectly valid 9 June, saved without a
 * warning. So the picker is handed {@code HarmoniaFormat.pickerConfig()} - the instance pattern
 * translated into the order/delimiter/width triple the widget takes - everywhere a date is entered.
 *
 * <p>
 * The derivation itself is exercised by evaluating the shipped {@code format.js} in a polyglot
 * context (it is deliberately dependency-free, so it needs only a {@code window} to attach to), and
 * every generated surface that carries a date input is checked to pass the configuration on - a new
 * picker added without it reopens the bug on that one screen only.
 */
class HarmoniaDateFormatIT {

    private static final String FORMAT_JS = "/META-INF/dirigible/application-core/shell/js/services/format.js";

    /** The generated / shipped surfaces where a date is entered. */
    private static final List<String> PICKER_SURFACES =
            List.of("/META-INF/dirigible/template-application-ui-harmonia-java/ui/perspective/manage/form-view.html.template",
                    "/META-INF/dirigible/template-application-ui-harmonia-java/ui/perspective/manage/list-view.html.template",
                    "/META-INF/dirigible/template-application-ui-harmonia-java/ui/perspective/document/document-view.html.template",
                    "/META-INF/dirigible/template-application-ui-harmonia-java/ui/perspective/report-file/index.html.template",
                    "/META-INF/dirigible/template-application-ui-harmonia-java/ui/shell/index.html.template",
                    "/META-INF/dirigible/template-application-ui-harmonia-java/ui/my/my-form-view.html.template",
                    "/META-INF/dirigible/template-application-ui-harmonia-java/ui/my/my-document-view.html.template",
                    "/META-INF/dirigible/template-application-ui-harmonia-java/ui/partner/partner-form-view.html.template",
                    "/META-INF/dirigible/template-application-ui-harmonia-java/ui/partner/partner-document-view.html.template",
                    "/META-INF/dirigible/template-form-builder-harmonia/ui/index.html.template");

    @Test
    void theDefaultIsoPatternConfiguresThePickerYearFirst() {
        try (Context context = load(null)) {
            Value config = eval(context, "HarmoniaFormat.pickerConfig()");

            assertEquals("YMD", config.getMember("order")
                                      .asString());
            assertEquals("-", config.getMember("delimiter")
                                    .asString());
            assertEquals("numeric", config.getMember("options")
                                          .getMember("year")
                                          .asString());
            assertEquals("2-digit", config.getMember("options")
                                          .getMember("month")
                                          .asString());
        }
    }

    @Test
    void aDayFirstPatternConfiguresThePickerDayFirst() {
        try (Context context = load("dd.MM.yyyy")) {
            Value config = eval(context, "HarmoniaFormat.pickerConfig()");

            assertEquals("DMY", config.getMember("order")
                                      .asString());
            assertEquals(".", config.getMember("delimiter")
                                    .asString());
            assertEquals("2-digit", config.getMember("options")
                                          .getMember("day")
                                          .asString());
            assertEquals("dd.mm.yyyy", eval(context, "HarmoniaFormat.dateHint()").asString());
        }
    }

    @Test
    void anUnpaddedMonthFirstPatternKeepsItsWidths() {
        try (Context context = load("M/d/yy")) {
            Value config = eval(context, "HarmoniaFormat.pickerConfig()");

            assertEquals("MDY", config.getMember("order")
                                      .asString());
            assertEquals("/", config.getMember("delimiter")
                                    .asString());
            assertEquals("numeric", config.getMember("options")
                                          .getMember("month")
                                          .asString());
            assertEquals("2-digit", config.getMember("options")
                                          .getMember("year")
                                          .asString());
        }
    }

    /**
     * A pattern the widget cannot be configured from must leave it alone rather than half-applied: a
     * missing field, a repeated one, or two different separators all yield an empty configuration.
     */
    @Test
    void aPatternThatIsNotAPlainDateLeavesThePickerAlone() {
        for (String pattern : List.of("yyyy-MM", "dd/MM-yyyy", "dd.MM.dd", "MMMM d, yyyy")) {
            try (Context context = load(pattern)) {
                assertEquals(0, eval(context, "Object.keys(HarmoniaFormat.pickerConfig()).length").asInt(),
                        "expected no picker configuration from [" + pattern + "]");
            }
        }
    }

    /**
     * A value typed in a shape other than the configured one is handed by the picker to the browser's
     * Date constructor, which reads {@code 06.09.2026} month first: 9 June under the default ISO
     * pattern (#7591). A dotted date is read day first and respelled in the instance pattern, which the
     * picker then reads as configured.
     */
    @Test
    void aDottedDateIsReadDayFirstWhateverThePattern() {
        try (Context context = load(null)) {
            assertEquals("2026-09-06", typed(context, "06.09.2026"));
            assertEquals("2026-09-06", typed(context, "6.9.2026"));
            assertEquals("2026-09-06", typed(context, " 06. 09. 26 "));
            assertEquals("2026-09-06", typed(context, "2026.09.06."));
            assertEquals("2026-09-06", typed(context, "2026/9/6"));
        }
        try (Context context = load("MM/dd/yyyy")) {
            assertEquals("09/06/2026", typed(context, "06.09.2026"));
        }
        // No picker configuration: the picker keeps its locale default, and ISO is what it always reads.
        try (Context context = load("MMMM d, yyyy")) {
            assertEquals("2026-09-06", typed(context, "06.09.2026"));
            assertEquals("null", typed(context, "6/9/2026"));
        }
    }

    /**
     * A value already in the configured shape, or no numeric date at all, is the picker's own to read.
     */
    @Test
    void aValueInTheConfiguredShapeIsLeftToThePicker() {
        try (Context context = load(null)) {
            assertEquals("null", typed(context, "2026-09-06"));
            assertEquals("null", typed(context, "Sep 6, 2026"));
            assertEquals("null", typed(context, ""));
        }
        try (Context context = load("dd.MM.yyyy")) {
            assertEquals("null", typed(context, "06.09.2026"));
            assertEquals("null", typed(context, "6.9.26"));
        }
        try (Context context = load("MM/dd/yyyy")) {
            assertEquals("null", typed(context, "09/06/2026"));
        }
    }

    /**
     * A numeric date whose separator implies no order is refused rather than guessed, and so is a day
     * that does not exist - the picker would roll 31.02 over into March.
     */
    @Test
    void anAmbiguousOrImpossibleDateIsRefused() {
        try (Context context = load(null)) {
            assertEquals("refused", typed(context, "6/9/2026"));
            assertEquals("refused", typed(context, "06-09-2026"));
            assertEquals("refused", typed(context, "31.02.2026"));
            assertEquals("refused", typed(context, "2026-02-31"));
        }
        try (Context context = load("dd.MM.yyyy")) {
            assertEquals("refused", typed(context, "31.02.2026"));
            assertEquals("refused", typed(context, "6/9/2026"));
        }
    }

    /**
     * The Inbox prints a task's timestamp - a {@code java.util.Date} on the wire, a {@code Date} object
     * for its own "updated" stamp - and used to hand both to {@code toLocaleString()}.
     */
    @Test
    void aTimestampPrintsThroughTheInstancePatternWhateverShapeItArrivesIn() {
        try (Context context = load("dd.MM.yyyy", "dd.MM.yyyy HH:mm")) {
            assertEquals("06.09.2026 17:02", eval(context, "HarmoniaFormat.value(new Date(2026, 8, 6, 17, 2, 31), true)").asString());
            assertEquals("06.09.2026 17:02", eval(context, "HarmoniaFormat.value('2026-09-06T17:02:31', true)").asString());
            assertEquals("06.09.2026", eval(context, "HarmoniaFormat.value('2026-09-06', true)").asString());
            assertEquals("06.09.2026 17:02", eval(context, "HarmoniaFormat.value(new Date(2026, 8, 6, 17, 2).getTime(), true)").asString());
        }
    }

    @Test
    void everyDatePickerIsConfiguredFromTheInstancePattern() throws Exception {
        for (String surface : PICKER_SURFACES) {
            String content = read(surface);

            assertTrue(
                    content.contains("x-h-date-picker-popup=\"HarmoniaFormat.pickerConfig()\"")
                            || content.contains("x-h-datetime-picker-popup=\"HarmoniaFormat.pickerConfig('dateTime')\""),
                    surface + " declares no configured picker at all - has the markup moved?");
            assertTrue(!content.contains("x-h-date-picker-popup ") && !content.contains("x-h-datetime-picker-popup "),
                    surface + " carries a picker with no configuration, which parses in the browser's locale");
        }
    }

    /** A date column filters through the same picker, not a native input in the browser's locale. */
    @Test
    void theListColumnFilterIsThePickerToo() throws Exception {
        String content = read("/META-INF/dirigible/template-application-ui-harmonia-java/ui/perspective/manage/list-view.html.template");

        // The filter moved out of the table into the Filter menu (#7491), so the picker now edits that
        // menu's draft - applyFilterDraft writes it into columnFilters. Same picker, same instance
        // pattern; only the model it binds moved.
        assertTrue(content.contains("x-h-date-picker-popup=\"HarmoniaFormat.pickerConfig()\" x-model=\"filterDraft\""),
                "the date column filter must bind the picker to the filter draft");
        assertTrue(!content.contains("'date' : 'text'"), "the native date input must be gone: " + content);
    }

    private static Value eval(Context context, String expression) {
        return context.eval("js", expression);
    }

    /** What readTypedDate makes of a typed value: the respelled value, "refused" or "null". */
    private static String typed(Context context, String text) {
        context.getBindings("js")
               .putMember("__typed", text);
        return eval(context, "(function (r) { return r === null ? 'null' : r.refused ? 'refused' : r.value; })"
                + "(HarmoniaFormat.readTypedDate(__typed))").asString();
    }

    private static Context load(String datePattern) {
        return load(datePattern, null);
    }

    /**
     * Evaluate the shipped format.js over the two stubs it touches - a {@code window} to publish itself
     * on and a {@code localStorage} holding the instance patterns (absent ones fall back to the file's
     * own defaults, exactly as in a browser with nothing stored).
     */
    private static Context load(String datePattern, String dateTimePattern) {
        Context context = Context.newBuilder("js")
                                 .allowAllAccess(true)
                                 // Nothing here is performance sensitive, and the interpreter-only notice
                                 // is written straight to the native stream, which the forked test JVM reports
                                 // as a corrupted channel.
                                 .option("engine.WarnInterpreterOnly", "false")
                                 .build();
        StringBuilder stored = new StringBuilder("var __stored = {};");
        if (datePattern != null) {
            stored.append("__stored['codbex.harmonia.format.date'] = '")
                  .append(datePattern)
                  .append("';");
        }
        if (dateTimePattern != null) {
            stored.append("__stored['codbex.harmonia.format.datetime'] = '")
                  .append(dateTimePattern)
                  .append("';");
        }
        context.eval("js",
                "var window = this; " + stored + " var localStorage = { getItem: function (k) { return __stored[k] || null; } };");
        try {
            context.eval("js", read(FORMAT_JS));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to evaluate " + FORMAT_JS, ex);
        }
        return context;
    }

    private static String read(String resource) throws Exception {
        try (InputStream in = HarmoniaDateFormatIT.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
