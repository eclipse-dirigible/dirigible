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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.Test;

/**
 * Editing a HAVING row in the report editor saves into that HAVING row (dirigible #8742).
 *
 * <p>
 * The edit dialog was prefilled from {@code report.havings[index]}, but its submit handler wrote
 * the values into {@code report.conditions[editConditionIndex]}: the HAVING row kept its old value
 * and the WHERE row last edited was overwritten, or the handler threw when there was no such row.
 *
 * <p>
 * The shipped controller is evaluated in a polyglot context over stubs of the AngularJS and hub
 * surface it touches; the form dialog resolves at once with the values the test submits.
 */
class ReportEditorHavingEditIT {

    private static final String EDITOR = "/META-INF/dirigible/editor-report/js/editor.js";

    private static final String REPORT = """
            $scope.report = {
              conditions: [ { left: 'a.STATUS', operation: '=', right: '1' }, { left: 'a.TOTAL', operation: '>', right: '0' } ],
              havings: [ { left: 'SUM(a.TOTAL)', operation: '>', right: '100' } ]
            };
            """;

    private static final String SUBMIT = "__form = { teiLeft: 'COUNT(*)', tedOperation: '>=', teiRight: '5' };";

    @Test
    void editingAHavingRowSavesIntoThatHavingRow() {
        try (Context context = load()) {
            context.eval("js", REPORT + "$scope.editConditionIndex = 1; " + SUBMIT + " $scope.editHaving(0);");
            assertEquals("COUNT(*)|>=|5", row(context, "havings[0]"), "the edited HAVING row carries the submitted values");
            assertEquals("a.STATUS|=|1", row(context, "conditions[0]"), "the WHERE rows are untouched");
            assertEquals("a.TOTAL|>|0", row(context, "conditions[1]"), "the last edited WHERE row is untouched");
        }
    }

    @Test
    void editingAHavingRowOfAReportWithoutConditionsSucceeds() {
        try (Context context = load()) {
            context.eval("js", "$scope.report = { havings: [ { left: 'SUM(a.TOTAL)', operation: '>', right: '100' } ] }; " + SUBMIT
                    + " $scope.editHaving(0);");
            assertEquals("COUNT(*)|>=|5", row(context, "havings[0]"), "the edited HAVING row carries the submitted values");
        }
    }

    private static String row(Context context, String path) {
        return context.eval("js", "var r = $scope.report." + path + "; r.left + '|' + r.operation + '|' + r.right;")
                      .asString();
    }

    private static Context load() {
        Context context = Context.newBuilder("js")
                                 .allowAllAccess(true)
                                 // The interpreter-only notice is written straight to the native stream,
                                 // which the forked test JVM reports as a corrupted channel.
                                 .option("engine.WarnInterpreterOnly", "false")
                                 .build();
        context.eval("js", """
                var __controller;
                var __form;
                var angular = {
                  module: function () { return { controller: function (name, fn) { __controller = fn; } }; },
                  element: function () { return { bind: function () {} }; },
                  toJson: function (value) { return JSON.stringify(value); }
                };
                function StatusBarHub() { this.showLabel = function () {}; }
                function WorkspaceHub() { this.onSaveAll = function () {}; this.onSaveFile = function () {}; }
                function LayoutHub() {
                  this.onFocusEditor = function () {}; this.onReloadEditorParams = function () {};
                  this.setEditorDirty = function () {}; this.closeEditor = function () {};
                }
                function DialogHub() {
                  this.showFormDialog = function () { return { then: function (ok) { ok(__form); } }; };
                  this.showAlert = function () {};
                }
                var AlertTypes = { Error: 'error' };
                var $scope = { $watch: function () {}, $evalAsync: function (fn) { fn(); } };
                """);
        try {
            context.eval("js", read(EDITOR));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to evaluate " + EDITOR, ex);
        }
        // No filePath view parameter: the controller stops before it loads a file.
        context.eval("js", "__controller($scope, {}, {}, {}, {}, { get: function () { return {}; } }, {});");
        return context;
    }

    private static String read(String resource) throws Exception {
        try (InputStream in = ReportEditorHavingEditIT.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
