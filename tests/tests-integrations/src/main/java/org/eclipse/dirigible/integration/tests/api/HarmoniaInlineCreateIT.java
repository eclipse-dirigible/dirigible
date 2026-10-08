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
import org.junit.jupiter.api.Test;

/**
 * A row created through a picker's inline "New" dialog commits on its own, before the record that
 * picks it (issue #7725). The shared {@code baseFormPage} keeps that row listed and selected when
 * the picker's reloaded list is narrowed past it ({@code adoptCreatedOption}), and names it in the
 * error of a failed save ({@code savedAlongside}), so the user corrects the record instead of
 * creating the row a second time.
 *
 * <p>
 * The helpers are exercised by evaluating the shipped {@code basePage.js} and
 * {@code baseFormPage.js} in a polyglot context; the generated pages that call them are run by
 * {@code IntentEmissionCoverageIT}, and every template carrying an inline create is checked here to
 * route it through them.
 */
class HarmoniaInlineCreateIT {

    private static final String SHELL = "/META-INF/dirigible/application-core/shell/js/";

    private static final String UI_BASE = "/META-INF/dirigible/template-application-ui-harmonia-java/ui/perspective/";

    private static final String LOOKUP = "{ url: '/units', key: 'Id', text: 'Name' }";

    @Test
    void aRowOutsideTheNarrowedListIsSplicedInByItsId() {
        try (Context context = load()) {
            context.eval("js", "var list = [{ value: 1, text: 'Kg' }]; page.adoptCreatedOption(list, 43, " + LOOKUP
                    + ").then(o => { __result = o.text + '|' + list.map(x => x.value).join(','); });");
            assertEquals("Pallet|43,1", result(context));
            assertEquals("/units/43", context.eval("js", "__fetched.join(',')")
                                             .asString());
        }
    }

    @Test
    void aRowAlreadyListedIsReturnedWithoutAFetch() {
        try (Context context = load()) {
            context.eval("js", "var list = [{ value: 43, text: 'Pallet' }]; page.adoptCreatedOption(list, '43', " + LOOKUP
                    + ").then(o => { __result = o.text + '|' + list.length; });");
            assertEquals("Pallet|1", result(context));
            assertEquals("", context.eval("js", "__fetched.join(',')")
                                    .asString(),
                    "a listed row must not cost a round trip");
        }
    }

    @Test
    void aRowThatCannotBeReadLeavesTheListAlone() {
        try (Context context = load()) {
            context.eval("js", "var list = []; page.adoptCreatedOption(list, 99, " + LOOKUP
                    + ").then(o => { __result = String(o) + '|' + list.length; });");
            assertEquals("null|0", result(context));
        }
    }

    @Test
    void aFailedSaveNamesWhatWasAlreadySaved() {
        try (Context context = load()) {
            assertEquals("", context.eval("js", "page.savedAlongside([])")
                                    .asString(),
                    "nothing created inline, nothing to say");
            context.eval("js",
                    "var one = []; page.rememberInlineCreated(one, 'Supplier Payment', 42, { value: 42, text: '2026-10-01 57.24' });");
            assertTrue(context.eval("js", "page.savedAlongside(one)")
                              .asString()
                              .startsWith("Supplier Payment '2026-10-01 57.24' was saved; this record was not."));
            context.eval("js", "var two = []; page.rememberInlineCreated(two, 'Unit', 43, { value: 43, text: 'Pallet' });"
                    + " page.rememberInlineCreated(two, 'Unit', 44, null);");
            assertTrue(context.eval("js", "page.savedAlongside(two)")
                              .asString()
                              .startsWith("Unit 'Pallet', Unit '#44' were saved; this record was not."),
                    "a row whose label is unknown is named by its id");
        }
    }

    @Test
    void everyInlineCreateGoesThroughTheSharedHelpers() throws Exception {
        String formPage = read(UI_BASE + "manage/form-page.js.template");
        String documentPage = read(UI_BASE + "document/document-page.js.template");
        assertEquals(1, count(formPage, "this.adoptCreatedOption("), "the form page's addRelated does not adopt the created row");
        assertEquals(2, count(documentPage, "this.adoptCreatedOption("),
                "the document page's addRelated and addRelatedItem must both adopt the created row");
        assertEquals(2, count(formPage, "this.inlineCreated = [];"), "the form page must forget its inline rows once its write lands");
        assertEquals(2, count(documentPage, "this.inlineCreated = [];"),
                "the document header must forget its inline rows once its write lands");
        assertTrue(count(documentPage, "this.draftInlineCreated = [];") >= 4,
                "the line dialog must forget its inline rows on open, close and every landed write");
        assertTrue(read(UI_BASE + "manage/form-view.html.template").contains("x-text=\"savedAlongside(inlineCreated)\""),
                "the form's error banner does not say what was already saved");
        String documentView = read(UI_BASE + "document/document-view.html.template");
        assertTrue(documentView.contains("x-text=\"savedAlongside(inlineCreated)\""),
                "the document's error banner does not say what was already saved");
        assertTrue(documentView.contains("x-text=\"savedAlongside(draftInlineCreated)\""),
                "the line dialog's error does not say what was already saved");
    }

    private static int count(String content, String token) {
        int count = 0;
        for (int at = content.indexOf(token); at >= 0; at = content.indexOf(token, at + token.length())) {
            count++;
        }
        return count;
    }

    private static String result(Context context) {
        return context.eval("js", "__result")
                      .asString();
    }

    /**
     * Evaluate the shipped basePage.js and baseFormPage.js over a REST stub that knows one row,
     * {@code /units/43}, and records every fetch; promises settle between evaluations.
     */
    private static Context load() {
        Context context = Context.newBuilder("js")
                                 .allowAllAccess(true)
                                 // The interpreter-only notice is written straight to the native stream,
                                 // which the forked test JVM reports as a corrupted channel.
                                 .option("engine.WarnInterpreterOnly", "false")
                                 .build();
        context.eval("js", "var window = this; var __result = null; var __fetched = [];"
                + " var T = (key, fallback, options) => String(fallback).replace(/\\{\\{(\\w+)\\}\\}/g, (m, n) => options && options[n] != null ? options[n] : m);"
                + " var App = { services: { api: { get: (url) => { __fetched.push(url);"
                + "   return url === '/units/43' ? Promise.resolve({ Id: 43, Name: 'Pallet' }) : Promise.reject(new Error('not found')); } } } };"
                + " var console = { error: () => {} };");
        for (String script : List.of("components/pages/basePage.js", "components/pages/baseFormPage.js")) {
            try {
                context.eval("js", read(SHELL + script));
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to evaluate " + script, ex);
            }
        }
        context.eval("js", "var page = baseFormPage();");
        return context;
    }

    private static String read(String resource) throws Exception {
        try (InputStream in = HarmoniaInlineCreateIT.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
