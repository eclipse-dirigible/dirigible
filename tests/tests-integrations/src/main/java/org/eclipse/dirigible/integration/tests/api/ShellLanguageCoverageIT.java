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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The Settings translation check reports what an application really leaves untranslated (dirigible
 * #8137, #8789).
 *
 * <p>
 * An app with no label catalog in a platform language has untranslated SCREENS. Its DATA is judged
 * by what its language tables hold: each multilingual entity's controller counts the records with
 * no translation per language, and the check reports those counts - not the {@code languages:} the
 * app declares, which hid an empty language table behind a declaration and reported a complete one
 * that declared nothing. The declaration is the judge only for an app whose coverage cannot be
 * read.
 *
 * <p>
 * The shared {@code languageCoverage} store is evaluated in a polyglot context over stubs of what
 * it touches - the store registry, the locale store, {@code T()} in the default language, the apps'
 * configs, their entities' coverage answers and the locales service. The four shells hand it their
 * perspectives and show its warnings.
 */
class ShellLanguageCoverageIT {

    /** The apps the shell hosts, as their perspectives point at them. */
    private static final String PERSPECTIVES = """
            [{ id: 'g', items: [
              { path: '/services/web/full/gen/full/index.html#/A' },
              { path: '/services/web/legacy/gen/legacy/index.html#/B' },
              { path: '/services/web/noscreens/gen/noscreens/index.html#/C' },
              { path: '/services/web/counted/gen/counted/index.html#/D' },
              { path: '/services/web/declared/gen/declared/index.html#/E' },
              { path: '/services/web/translated/gen/translated/index.html#/F' },
              { path: '/services/web/unreadable/gen/unreadable/index.html#/G' },
              { path: '/services/web/inbox/index.html#/H' }
            ]}]
            """;

    private static final String DECLARE_HINT =
            " does not provide bg data - its lists and documents show the default-language values (for example statuses and types)."
                    + " Declare languages: in its intent once the data is translated.";

    @Test
    void an_app_is_reported_for_what_its_screens_and_its_data_leave_untranslated() {
        try (Context context = load()) {
            context.eval("js", "var __result = 'pending'; Alpine.store('languageCoverage').load(" + PERSPECTIVES
                    + ").then(() => { __result = JSON.stringify(Alpine.store('languageCoverage').warnings()); });");

            assertEquals("[{\"app\":\"legacy\",\"lines\":[\"legacy" + DECLARE_HINT + "\"]},"
                    + "{\"app\":\"noscreens\",\"lines\":[\"noscreens has no bg translation of its screens - its labels show in the default language.\"]},"
                    + "{\"app\":\"counted\",\"lines\":[\"Currencies: 3 of 160 records have no Bulgarian translation.\","
                    + "\"Units: 1 of 5 records has no Bulgarian translation.\"]},"
                    + "{\"app\":\"declared\",\"lines\":[\"Countries: 4 of 4 records have no Bulgarian translation.\"]},"
                    + "{\"app\":\"unreadable\",\"lines\":[\"unreadable" + DECLARE_HINT + "\"]}]", //
                    context.eval("js", "__result")
                           .asString(),
                    "full (no translatable data), translated (every record translated, nothing declared), the entity the user may not"
                            + " read and the platform surface must not be reported; declared (bg declared, an empty language table) must");
        }
    }

    @Test
    void the_scan_waits_for_settings_and_runs_once() {
        try (Context context = load()) {
            // The perspectives arrive at boot; the scan is the Settings page's, so boot asks nothing (#8346).
            context.eval("js", "Alpine.store('languageCoverage').remember(" + PERSPECTIVES + ");");
            assertEquals(0, context.eval("js", "__requests.length")
                                   .asInt(),
                    "the coverage scan must not run before Settings is shown");

            context.eval("js", "Alpine.store('languageCoverage').scan(); Alpine.store('languageCoverage').scan();");
            assertEquals(1,
                    context.eval("js",
                            "__requests.filter(function (u) { return u.indexOf('/services/platform/extensions/locales?') === 0; }).length")
                           .asInt(),
                    "showing Settings again must not repeat the scan");
        }
    }

    /**
     * Each shell hands the store its perspectives at boot, starts the scan when Settings opens, loads
     * the store and shows its warnings - and keeps no copy of the check of its own.
     */
    @ParameterizedTest
    @ValueSource(strings = {"application", "admin", "partner", "personal"})
    void every_shell_shows_the_shared_check(String shell) {
        String appShell = read("/META-INF/dirigible/" + shell + "/js/appShell.js");
        assertTrue(appShell.contains("Alpine.store('languageCoverage').remember(all);"), shell + " must hand the store its perspectives");
        assertTrue(appShell.contains("Alpine.store('languageCoverage').scan();"), shell + " must start the scan when Settings opens");
        assertFalse(appShell.contains("translatableData"), shell + " must not judge the apps' configs itself");
        assertTrue(read("/META-INF/dirigible/" + shell + "/index.html").contains(
                "/services/web/application-core/shell/js/stores/languageCoverage.js"), shell + " must load the store");
        assertTrue(read("/META-INF/dirigible/" + shell + "/views/_settings.html").contains("$store.languageCoverage.warnings()"),
                shell + " must show the store's warnings");
    }

    private static Context load() {
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
                var __stores = { locale: { value: 'en', languages: function () { return ['en', 'bg']; } } };
                var Alpine = {
                  store: function (name, value) {
                    if (value === undefined) return __stores[name];
                    __stores[name] = value;
                  }
                };
                // T() in the default language: the baked fallback, interpolated.
                var T = function (key, fallback, options) {
                  return String(fallback).replace(/\\{\\{\\s*(\\w+)\\s*\\}\\}/g, function (match, name) {
                    return options && options[name] !== undefined ? options[name] : match;
                  });
                };
                function entities(list) { return 'translatableEntities: ' + JSON.stringify(list) + ','; }
                // Each app's js/config.js as the generator renders it: its declared data languages, whether
                // it has translatable data and its multilingual entities (one line).
                var __configs = {
                  full:       "App.config = { languages: ['en'], translatableData: false,\\n" + entities([]) + "\\n};",
                  legacy:     "App.config = { languages: ['en'] };",
                  noscreens:  "App.config = { languages: ['en'], translatableData: false,\\n" + entities([]) + "\\n};",
                  counted:    "App.config = { languages: ['en'], translatableData: true,\\n" + entities([
                                { key: 'counted:m.t.CURRENCY_plural', label: 'Currencies', url: '/cov/currency' },
                                { key: 'counted:m.t.UNIT_plural', label: 'Units', url: '/cov/unit' },
                                { key: 'counted:m.t.SECRET_plural', label: 'Secrets', url: '/cov/secret' }]) + "\\n};",
                  declared:   "App.config = { languages: ['en', 'bg'], translatableData: true,\\n" + entities([
                                { key: 'declared:m.t.COUNTRY_plural', label: 'Countries', url: '/cov/country' }]) + "\\n};",
                  translated: "App.config = { languages: ['en'], translatableData: true,\\n" + entities([
                                { key: 'translated:m.t.STATUS_plural', label: 'Statuses', url: '/cov/status' }]) + "\\n};",
                  unreadable: "App.config = { languages: ['en'], translatableData: true,\\n" + entities([
                                { key: 'unreadable:m.t.TYPE_plural', label: 'Types', url: '/cov/broken' }]) + "\\n};"
                };
                // The entities' controllers: how many records have no translation, per language.
                var __coverage = {
                  '/cov/currency': { total: 160, untranslated: { bg: 3 } },
                  '/cov/unit':     { total: 5,   untranslated: { bg: 1 } },
                  '/cov/country':  { total: 4,   untranslated: { bg: 4 } },
                  '/cov/status':   { total: 7,   untranslated: { bg: 0 } }
                };
                // The locales service: every app ships an en-US catalog; all but noscreens a bg-BG one.
                var __catalog = { t: { Label: 'x' } };
                var __bg = {};
                Object.keys(__configs).forEach(function (app) { if (app !== 'noscreens') __bg[app] = __catalog; });
                var __locales = {
                  locales: [{ id: 'en-US' }, { id: 'bg-BG' }],
                  translations: { 'en-US': Object.assign({ noscreens: __catalog }, __bg), 'bg-BG': __bg }
                };
                var __requests = [];
                function answer(status, body) {
                  return Promise.resolve({ ok: status === 200, status: status,
                    text: function () { return Promise.resolve(String(body)); },
                    json: function () { return Promise.resolve(body); } });
                }
                // The secret entity is not the user's to read (403); the broken one fails (500).
                var fetch = function (url) {
                  __requests.push(url);
                  var config = /^\\/services\\/web\\/([^\\/]+)\\/.*js\\/config\\.js$/.exec(url);
                  if (config) return __configs[config[1]] ? answer(200, __configs[config[1]]) : answer(404, '');
                  if (url === '/cov/secret') return answer(403, {});
                  if (__coverage[url]) return answer(200, __coverage[url]);
                  if (url.indexOf('/services/platform/extensions/locales?') === 0) return answer(200, __locales);
                  return answer(500, {});
                };
                """);
        context.eval("js", read("/META-INF/dirigible/application-core/shell/js/stores/languageCoverage.js"));
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
