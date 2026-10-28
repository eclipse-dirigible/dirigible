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
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.graalvm.polyglot.Context;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A projection that includes a navigation group keeps the group's settings (dirigible #8138).
 *
 * <p>
 * The shell moves every SETTING perspective out of its group into the Settings footer and used to
 * forget the group on the way. A projection then kept a setting only when it cherry-picked its id
 * or when the setting's project contributed a visible sidebar item - so the Store lens, which lists
 * the {@code commons} group, showed none of Units of Measure, Tax Rates or Currencies: their
 * projects have no sidebar item at all. Listing a group now means everything in it, sidebar items
 * and settings.
 *
 * <p>
 * Each shell's shipped {@code appShell.js} is evaluated in a polyglot context over stubs of what
 * its {@code init} touches - the {@code alpine:init} listener, the component registry, permissive
 * stores and a fetch answering the perspectives and projections services - and the component it
 * registers is initialized and asked for its visible settings.
 */
class ShellProjectionSettingsIT {

    private static final String VISIBLE = "shell.visibleSettingsItems().map(function (it) { return it.id; }).sort().join(',')";

    @ParameterizedTest
    @ValueSource(strings = {"application", "admin", "partner", "personal"})
    void aProjectionThatListsAGroupKeepsTheGroupsSettings(String shell) {
        try (Context context = load(shell)) {
            context.eval("js", "var shell = __factory(); shell.init().catch(function () {});");

            assertEquals("invoices-InvoiceType,stores-StoreType,tax-rates-TaxRate,uoms-UoM", eval(context, VISIBLE),
                    "the Store projection keeps the settings of its groups (commons: UoM, Tax Rate - whose projects have no sidebar"
                            + " item), its cherry-picked setting (Invoice Type) and its own project's (Store Type) - and not an"
                            + " unrelated group's (Department)");

            context.eval("js", "shell.projectionId = 'all';");
            assertEquals("employees-Department,invoices-InvoiceType,stores-StoreType,tax-rates-TaxRate,uoms-UoM", eval(context, VISIBLE),
                    "the everything projection filters nothing");
        }
    }

    private static String eval(Context context, String expression) {
        return context.eval("js", expression)
                      .toString();
    }

    private static Context load(String shell) {
        Context context = Context.newBuilder("js")
                                 .allowAllAccess(true)
                                 // See HarmoniaDateFormatIT: the interpreter-only notice corrupts the forked JVM's channel.
                                 .option("engine.WarnInterpreterOnly", "false")
                                 .build();
        context.eval("js",
                """
                        var window = this;
                        var __listeners = [];
                        var document = {
                          addEventListener: function (type, listener) { if (type === 'alpine:init') __listeners.push(listener); },
                          querySelector: function () { return null; },
                          querySelectorAll: function () { return []; }
                        };
                        window.addEventListener = function () {};
                        window.location = { hash: '', pathname: '/', search: '' };
                        window.matchMedia = function () { return { matches: false, addEventListener: function () {} }; };
                        var localStorage = { getItem: function () { return null; }, setItem: function () {} };
                        var App = { config: {} };
                        window.PineconeRouter = { settings: function () {}, context: { path: '/' } };
                        var __factory;
                        // Any store, any method: init() touches several, and only the settings split is under test.
                        var __anything = new Proxy(function () {}, {
                          get: function (target, key) { return key === 'then' ? undefined : __anything; },
                          apply: function () { return __anything; }
                        });
                        var Alpine = {
                          data: function (name, factory) { if (name === 'app') __factory = factory; },
                          store: function () { return __anything; }
                        };
                        function item(project, entity, kind) {
                          return { id: project + '-' + entity, kind: kind, label: entity, path: '/services/web/' + project + '/gen/app/index.html' };
                        }
                        var __perspectives = [
                          { id: 'inventory', label: 'Inventory', items: [item('stores', 'Store', 'PRIMARY'), item('stores', 'StoreType', 'SETTING')] },
                          { id: 'commons', label: 'Commons', items: [item('uoms', 'UoM', 'SETTING'), item('tax-rates', 'TaxRate', 'SETTING')] },
                          { id: 'sales', label: 'Sales', items: [item('invoices', 'Invoice', 'PRIMARY'), item('invoices', 'InvoiceType', 'SETTING')] },
                          { id: 'hr', label: 'People', items: [item('employees', 'Employee', 'PRIMARY'), item('employees', 'Department', 'SETTING')] }
                        ];
                        var __projections = [
                          { id: 'store', label: 'Store', groups: ['inventory', 'commons'], items: ['invoices-InvoiceType'] },
                          { id: 'all', label: 'All', all: true }
                        ];
                        var fetch = function (url) {
                          var body = String(url).indexOf('/projections') >= 0 ? { projections: __projections }
                              : String(url).indexOf('/perspectives') >= 0 ? { perspectives: __perspectives } : null;
                          if (!body) return Promise.resolve({ ok: false, json: function () { return Promise.resolve({}); } });
                          return Promise.resolve({ ok: true, json: function () { return Promise.resolve(body); } });
                        };
                        var T = function (key, fallback) { return fallback; };
                        """);
        context.eval("js", read("/META-INF/dirigible/" + shell + "/js/appShell.js"));
        context.eval("js", "__listeners.forEach(function (listener) { listener(); });");
        return context;
    }

    private static String read(String resource) {
        try (InputStream in = ShellProjectionSettingsIT.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read " + resource, ex);
        }
    }
}
