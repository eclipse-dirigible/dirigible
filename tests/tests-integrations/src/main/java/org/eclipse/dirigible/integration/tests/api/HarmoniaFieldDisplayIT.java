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
     * The record page formats its values through its own preview helpers ({@code fieldText} and friends
     * in {@code form-page.js}, issue #7491), so only its role gate goes through the shared one.
     */
    private static final String RECORD_PAGE = "perspective/manage/form-view.html.template";

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
            assertTrue(content.contains("#macro(valueDisplay $property)fieldDisplay(form.${property.name}, "),
                    view + " does not define its value display over the shared fieldDisplay()");
            assertTrue(content.contains("#valueGate($property)"), view + " gates a read-only card row outside the shared helper");
            if (!RECORD_PAGE.equals(view)) {
                assertTrue(content.contains("x-text=\"#valueDisplay($property)\""),
                        view + " renders a read-only card that does not go through the shared helper");
            }
            assertTrue(content.contains("#macro(valueGate $property)x-show=\"#if($property.roleRead)canSee('${property.name}') && #end"),
                    view + " shows a role-scoped read-only field without asking canSee()");
            assertFalse(content.contains("x-text=\"form."), view + " prints a raw form value");
            assertFalse(content.contains("headerDisplay("), view + " still calls a per-page copy of the formatter");
        }
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
        Context context = Context.newBuilder("js")
                                 .allowAllAccess(true)
                                 // The interpreter-only notice is written straight to the native stream,
                                 // which the forked test JVM reports as a corrupted channel.
                                 .option("engine.WarnInterpreterOnly", "false")
                                 .build();
        context.eval("js",
                "var window = this; var __stored = { 'codbex.harmonia.format.date': 'dd.MM.yyyy',"
                        + " 'codbex.harmonia.format.datetime': 'dd.MM.yyyy HH:mm', 'codbex.harmonia.format.number': '#,##0.00' };"
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
