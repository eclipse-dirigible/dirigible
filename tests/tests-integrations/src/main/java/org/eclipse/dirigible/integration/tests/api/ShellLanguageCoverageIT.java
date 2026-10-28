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
 * The Settings translation check reports what an application really leaves untranslated (dirigible
 * #8137).
 *
 * <p>
 * It read only the {@code languages:} declaration of each app's {@code js/config.js} - the intent's
 * DATA languages - so every module whose screens are fully in Bulgarian, but which declares no
 * Bulgarian data, was listed under "Missing translations" and reported by testers as untranslated.
 * The check now tells the two apart: an app with no label catalog in a platform language has
 * untranslated SCREENS; an app with labels whose translatable (multilingual) data is not provided
 * in it has untranslated DATA; an app with labels and no translatable data is not reported at all.
 *
 * <p>
 * Each shell's shipped {@code appShell.js} is evaluated in a polyglot context over stubs of what
 * the check touches - the component registry, the locale store, the apps' configs and the locales
 * service - and the component it registers is asked to classify a set of apps.
 */
class ShellLanguageCoverageIT {

    /** The apps the shell hosts, as their perspectives point at them. */
    private static final String PERSPECTIVES = """
            [{ id: 'g', items: [
              { path: '/services/web/full/gen/full/index.html#/A' },
              { path: '/services/web/datamissing/gen/datamissing/index.html#/B' },
              { path: '/services/web/legacy/gen/legacy/index.html#/C' },
              { path: '/services/web/noscreens/gen/noscreens/index.html#/D' },
              { path: '/services/web/complete/gen/complete/index.html#/E' },
              { path: '/services/web/inbox/index.html#/F' }
            ]}]
            """;

    @ParameterizedTest
    @ValueSource(strings = {"application", "admin", "partner", "personal"})
    void an_app_is_reported_for_what_it_does_not_translate(String shell) {
        try (Context context = load(shell)) {
            context.eval("js", "var shell = __factory(); var __result = 'pending'; shell.loadLanguageCoverage(" + PERSPECTIVES
                    + ").then(() => { __result = JSON.stringify(shell.languageWarnings()); });");

            assertEquals("[{\"app\":\"datamissing\",\"screens\":[],\"data\":[\"bg\"]}," //
                    + "{\"app\":\"legacy\",\"screens\":[],\"data\":[\"bg\"]}," //
                    + "{\"app\":\"noscreens\",\"screens\":[\"bg\"],\"data\":[]}]", //
                    context.eval("js", "__result")
                           .asString(),
                    "full (labels, no translatable data), complete (labels and data) and the platform surface must not be reported");
        }
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
                var __locale = { languages: function () { return ['en', 'bg']; } };
                var Alpine = {
                  data: function (name, factory) { if (name === 'app') __factory = factory; },
                  store: function (name) { return name === 'locale' ? __locale : {}; }
                };
                // Each app's js/config.js: its declared data languages and whether it has translatable data.
                var __configs = {
                  full:        "export default { languages: ['en'], translatableData: false };",
                  datamissing: "export default { languages: ['en'], translatableData: true };",
                  legacy:      "export default { languages: ['en'] };",
                  noscreens:   "export default { languages: ['en'], translatableData: false };",
                  complete:    "export default { languages: ['en', 'bg'], translatableData: true };"
                };
                // The locales service: every app ships an en-US catalog; all but noscreens a bg-BG one.
                var __catalog = { t: { Label: 'x' } };
                var __locales = {
                  locales: [{ id: 'en-US' }, { id: 'bg-BG' }],
                  translations: {
                    'en-US': { full: __catalog, datamissing: __catalog, legacy: __catalog, noscreens: __catalog, complete: __catalog },
                    'bg-BG': { full: __catalog, datamissing: __catalog, legacy: __catalog, complete: __catalog }
                  }
                };
                var fetch = function (url) {
                  var config = /^\\/services\\/web\\/([^\\/]+)\\/.*js\\/config\\.js$/.exec(url);
                  if (config) {
                    var text = __configs[config[1]];
                    return Promise.resolve(text
                        ? { ok: true, text: function () { return Promise.resolve(text); } }
                        : { ok: false, text: function () { return Promise.resolve(''); } });
                  }
                  if (url.indexOf('/services/platform/extensions/locales?') === 0) {
                    return Promise.resolve({ ok: true, json: function () { return Promise.resolve(__locales); } });
                  }
                  return Promise.resolve({ ok: false, json: function () { return Promise.resolve({}); } });
                };
                """);
        context.eval("js", read("/META-INF/dirigible/" + shell + "/js/appShell.js"));
        context.eval("js", "__listeners.forEach(function (listener) { listener(); });");
        return context;
    }

    private static String read(String resource) {
        try (InputStream in = ShellLanguageCoverageIT.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read " + resource, ex);
        }
    }
}
