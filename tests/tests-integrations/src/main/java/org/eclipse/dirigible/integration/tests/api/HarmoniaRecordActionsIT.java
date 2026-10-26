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

import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.Test;

/**
 * A document page offers no status-guarded action before its record has loaded (dirigible #7605).
 *
 * <p>
 * The page starts with an empty {@code record}, and the {@code customActions} store fails open on a
 * status it does not have - deliberately, for a caller with no row. So for the first moment every
 * guarded action was offered (Generate Invoice on an INVOICED proforma) and refused with a 409 when
 * clicked. The shipped store and the page's own {@code recordActions()} - lifted verbatim out of
 * the template - are evaluated in a polyglot context, so the test runs the code the browser runs.
 */
class HarmoniaRecordActionsIT {

    private static final String STORE = "/META-INF/dirigible/application-core/shell/js/stores/customActions.js";

    private static final String DOCUMENT = "/META-INF/dirigible/template-application-ui-harmonia-java/ui/perspective/document/";

    private static final String MANAGE = "/META-INF/dirigible/template-application-ui-harmonia-java/ui/perspective/manage/";

    /**
     * The proforma's three actions: Print-like (unguarded), a create-from guarded by {@code allowed},
     * and a transition guarded by {@code from}.
     */
    private static final String ACTIONS = """
            [
              { id: 'notes', view: 'ProformaInvoice', type: 'entity', label: 'Notes' },
              { id: 'invoice', view: 'ProformaInvoice', type: 'entity', label: 'Generate Invoice',
                guard: { property: 'Status', allowed: [2] } },
              { id: 'void', view: 'ProformaInvoice', type: 'entity', label: 'Void', statusProperty: 'Status', from: [1, 2] }
            ]""";

    @Test
    void aLoadingRecordIsOfferedOnlyTheUnguardedActions() throws Exception {
        try (Context context = load()) {
            assertEquals("notes", ids(context, "page.recordActions()"), "nothing guarded may be offered before the record has loaded");

            context.eval("js", "page.record = { Id: 12, Status: 3 }; page.recordLoaded = true;");
            assertEquals("notes", ids(context, "page.recordActions()"), "an INVOICED proforma offers neither Generate Invoice nor Void");

            context.eval("js", "page.record = { Id: 12, Status: 2 };");
            assertEquals("notes,invoice,void", ids(context, "page.recordActions()"), "an ISSUED proforma offers all three");
        }
    }

    /** The store's own answer for a caller with no row is unchanged: it still fails open. */
    @Test
    void theStoreStillFailsOpenWithoutARecord() throws Exception {
        try (Context context = load()) {
            assertEquals("notes,invoice,void", ids(context, "store.getActions('ProformaInvoice', 'entity')"));
            assertEquals("false,true,true", context.eval("js", "store.actions.map(function (a) { return store.isGuarded(a); }).join(',')")
                                                   .asString());
        }
    }

    /** The view reads the page's method, and the page flips the flag where the record arrives. */
    @Test
    void theDocumentPageWiresTheGate() throws Exception {
        String view = read(DOCUMENT + "document-view.html.template");
        assertTrue(view.contains("x-for=\"action in recordActions()\""), "the More actions menu must ask the page, not the store directly");
        String page = read(DOCUMENT + "document-page.js.template");
        assertTrue(page.contains("this.record = record || {};\n      this.recordLoaded = true;"),
                "loadHeader must mark the record loaded where it assigns it");
    }

    /**
     * The manage list's row menu offers each row the actions its own status allows (#8116) - the
     * sheet's rule, applied to the row it is opened on.
     */
    @Test
    void aRowOffersTheActionsItsStatusAllows() throws Exception {
        try (Context context = load()) {
            context.eval("js",
                    "var list = { caView: 'ProformaInvoice', " + method(MANAGE + "list-page.js.template", "    rowActions(row) {") + " };");
            assertEquals("notes,invoice,void", ids(context, "list.rowActions({ Id: 1, Status: 2 })"), "an ISSUED row offers all three");
            assertEquals("notes", ids(context, "list.rowActions({ Id: 2, Status: 3 })"), "an INVOICED row offers neither guarded action");
        }
    }

    /**
     * The manage record page offers its record's actions in preview only, and only once the record has
     * loaded (#8116): an action acts on the stored record, and the store fails open on a record without
     * its status (#7605).
     */
    @Test
    void theRecordPageOffersItsActionsInPreviewOnceLoaded() throws Exception {
        try (Context context = load()) {
            String getter = method(MANAGE + "form-page.js.template", "    get recordActions() {").replace("'${name}'", "'ProformaInvoice'");
            context.eval("js", "var form = { mode: 'preview', loadedRecord: null, get isPreview() { return this.mode === 'preview'; }, "
                    + getter + " };");
            assertEquals("", ids(context, "form.recordActions"), "nothing before the record has loaded");

            context.eval("js", "form.loadedRecord = { Id: 12, Status: 2 };");
            assertEquals("notes,invoice,void", ids(context, "form.recordActions"), "an ISSUED proforma offers all three");

            context.eval("js", "form.loadedRecord = { Id: 12, Status: 3 };");
            assertEquals("notes", ids(context, "form.recordActions"), "an INVOICED proforma offers only the unguarded one");

            context.eval("js", "form.mode = 'edit';");
            assertEquals("", ids(context, "form.recordActions"), "nothing while editing: an action acts on the stored record");
            context.eval("js", "form.mode = 'create'; form.loadedRecord = null;");
            assertEquals("", ids(context, "form.recordActions"), "nothing on a record not yet created");
        }
    }

    /** The two manage menus read the page's answer, and the record page keeps the record it loaded. */
    @Test
    void theManagePagesWireTheActions() throws Exception {
        assertTrue(read(MANAGE + "list-view.html.template").contains("x-for=\"action in rowActions(row)\""),
                "the row menu must offer the row's actions");
        assertTrue(read(MANAGE + "form-view.html.template").contains("x-for=\"action in recordActions\""),
                "the record page's More actions menu must offer the record's actions");
        String page = read(MANAGE + "form-page.js.template");
        assertTrue(page.contains("this.loadedRecord = record;"), "loadRecord must keep the record the guards read");
        assertTrue(page.contains("this.onActionDone("), "the record page must re-read its record after an action");
        assertTrue(page.contains("return this.recordActions.length > 0 ||"), "the overflow menu must count the actions");
    }

    private static String ids(Context context, String expression) {
        return context.eval("js", expression + ".map(function (a) { return a.id; }).join(',')")
                      .asString();
    }

    private static Context load() throws Exception {
        Context context = Context.newBuilder("js")
                                 .allowAllAccess(true)
                                 // The interpreter-only notice is written straight to the native stream,
                                 // which the forked test JVM reports as a corrupted channel.
                                 .option("engine.WarnInterpreterOnly", "false")
                                 .build();
        context.eval("js",
                """
                        var window = this;
                        var __documentListeners = {};
                        var document = { addEventListener: function (type, fn) { (__documentListeners[type] = __documentListeners[type] || []).push(fn); } };
                        var __stores = {};
                        var Alpine = { store: function (name, value) { if (value !== undefined) { __stores[name] = value; } return __stores[name]; }, data: function () {} };
                        var console = { log: function () {}, warn: function () {}, error: function () {} };
                        var App = { config: {}, services: {} };
                        """);
        context.eval("js", read(STORE));
        context.eval("js", "__documentListeners['alpine:init'].forEach(function (fn) { fn(); }); var store = Alpine.store('customActions');"
                + " store.actions = " + ACTIONS + ";");
        context.eval("js", "var page = { caView: 'ProformaInvoice', record: {}, recordLoaded: false, " + recordActionsMethod() + " };");
        return context;
    }

    /** The page's {@code recordActions()} exactly as the template ships it. */
    private static String recordActionsMethod() throws Exception {
        String page = read(DOCUMENT + "document-page.js.template");
        int start = page.indexOf("    recordActions() {");
        assertTrue(start >= 0, "the document page declares no recordActions()");
        int end = page.indexOf("\n    },", start);
        return page.substring(start, end + "\n    }".length());
    }

    /** A method of a page template, from its first line to its closing brace, exactly as shipped. */
    private static String method(String template, String firstLine) throws Exception {
        String page = read(template);
        int start = page.indexOf(firstLine);
        assertTrue(start >= 0, template + " declares no " + firstLine.trim());
        int end = page.indexOf("\n    },", start);
        return page.substring(start, end + "\n    }".length());
    }

    private static String read(String resource) throws Exception {
        try (InputStream in = HarmoniaRecordActionsIT.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
