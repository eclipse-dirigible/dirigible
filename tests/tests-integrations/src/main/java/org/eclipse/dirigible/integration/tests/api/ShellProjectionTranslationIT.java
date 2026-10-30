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
 * A projection's name in the product switcher translates (dirigible #8133).
 *
 * <p>
 * The switcher always bound {@code p.tkey ? T(p.tkey, p.label) : p.label}, but nothing loaded the
 * catalog the key lives in: the shells added the i18n namespaces of the perspectives' keys only,
 * and a lens project contributes no perspective, so the name stayed English in every language. A
 * projection now declares {@code translation: { key }} like a navigation group (#7610), and the
 * shell adopts it as the projection's key and loads its project's catalog.
 *
 * <p>
 * Each shell's shipped {@code appShell.js} is evaluated in a polyglot context over stubs of what
 * {@code loadProjections} touches - the {@code alpine:init} listener, the component registry, the
 * projections service, the namespace loader and {@code T} - and the component it registers is asked
 * to load the projections.
 */
class ShellProjectionTranslationIT {

    @ParameterizedTest
    @ValueSource(strings = {"application", "admin", "partner", "personal"})
    void aProjectionTranslatesThroughItsDeclaredKey(String shell) {
        try (Context context = load(shell)) {
            context.eval("js", "var shell = __factory(); shell.loadProjections();");

            assertEquals("nav-lens-store:projections.store", eval(context, "shell.projections[0].tkey"),
                    "the declared translation key is adopted as the projection's key");
            assertEquals("legacy:projections.legacy", eval(context, "shell.projections[1].tkey"),
                    "a projection that already declares a raw key keeps it");
            assertEquals("nav-lens-store,legacy", eval(context, "__namespaces.join(',')"),
                    "the catalogs of the projections' projects are loaded - never the shell's own, never a key without a project");
            assertEquals("Склад", eval(context, "shell.projectionLabel()"));
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
        context.eval("js", """
                var window = this;
                var __listeners = [];
                var document = {
                  addEventListener: function (type, listener) { if (type === 'alpine:init') __listeners.push(listener); }
                };
                var localStorage = { getItem: function () { return null; }, setItem: function () {} };
                var App = { config: {} };
                window.PineconeRouter = { settings: function () {} };
                var __factory;
                var Alpine = {
                  data: function (name, factory) { if (name === 'app') __factory = factory; },
                  store: function () { return {}; }
                };
                var __projections = [
                  { id: 'store', label: 'Store', translation: { key: 'nav-lens-store:projections.store' } },
                  { id: 'legacy', label: 'Legacy', tkey: 'legacy:projections.legacy' },
                  { id: 'core', label: 'Core', translation: { key: 'application-core:shell.projections.core' } },
                  { id: 'bare', label: 'Bare', translation: { key: 'no-namespace' } }
                ];
                var fetch = function () {
                  return Promise.resolve({ ok: true, json: function () { return Promise.resolve({ projections: __projections }); } });
                };
                var __namespaces = [];
                window.AppI18nAddNamespaces = function (namespaces) { __namespaces.push.apply(__namespaces, namespaces); };
                var T = function (key, fallback) { return key === 'nav-lens-store:projections.store' ? 'Склад' : fallback; };
                """);
        context.eval("js", read("/META-INF/dirigible/" + shell + "/js/appShell.js"));
        context.eval("js", "__listeners.forEach(function (listener) { listener(); });");
        return context;
    }

    private static String read(String resource) {
        try (InputStream in = ShellProjectionTranslationIT.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read " + resource, ex);
        }
    }
}
