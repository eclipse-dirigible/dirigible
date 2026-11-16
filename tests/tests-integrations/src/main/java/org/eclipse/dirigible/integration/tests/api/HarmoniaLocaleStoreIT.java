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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * The language a data request carries is the one the locale store settles on, never the saved
 * candidate it starts from (dirigible #7558).
 *
 * <p>
 * The store starts from the saved {@code codbex.harmonia.language} and only learns the platform's
 * supported set from a fetch; a saved code the instance does not offer then falls back to the first
 * entry. A request the fetch client sent in between carried the saved code, so on an instance
 * serving only {@code en} the same multilingual list rendered translated on one load and
 * untranslated on the next, depending on which response arrived first.
 *
 * <p>
 * The race is made deterministic by evaluating the shipped {@code locale.js} and {@code api.js} in
 * a polyglot context over stubs of what they touch, with the languages response held back until the
 * test releases it: a data request issued before that must not leave before it, and must then carry
 * the settled language. The labels {@code i18n.js} renders wait for the same settled language
 * (#8632).
 */
class HarmoniaLocaleStoreIT {

    private static final String SHELL_JS = "/META-INF/dirigible/application-core/shell/js/";

    private static final String I18N_SCRIPT = "src=\"/services/web/application-core/shell/js/services/i18n.js\"";

    private static final String LOCALE_SCRIPT = "src=\"/services/web/application-core/shell/js/stores/locale.js\"";

    private static final String ALPINE_SCRIPT = "src=\"/webjars/alpinejs/dist/cdn.min.js\"";

    @Test
    void aSavedLanguageTheInstanceDoesNotOfferIsNeverSent() {
        try (Context context = load("bg")) {
            eval(context, "App.services.api.request('GET', '/Items');");
            assertEquals(0, count(context), "a data request left before the supported set was known");

            eval(context, "__releaseLanguages(['en']);");
            assertEquals(1, count(context));
            assertEquals("en", eval(context, "__requests[0].language"));
            assertEquals("en", eval(context, "Alpine.store('locale').value"));
        }
    }

    @Test
    void aSavedLanguageTheInstanceOffersIsKept() {
        try (Context context = load("bg")) {
            eval(context, "App.services.api.request('GET', '/Items');");
            eval(context, "__releaseLanguages(['en', 'bg']);");
            assertEquals(1, count(context));
            assertEquals("bg", eval(context, "__requests[0].language"));
        }
    }

    @Test
    void anUnreachableLanguageServiceDoesNotHoldRequestsBack() {
        try (Context context = load("bg")) {
            eval(context, "App.services.api.request('GET', '/Items');");
            eval(context, "__failLanguages();");
            assertEquals(1, count(context));
            assertEquals("bg", eval(context, "__requests[0].language"));
        }
    }

    @Test
    void anExplicitRequestLanguageIsSentWithoutWaiting() {
        try (Context context = load("bg")) {
            eval(context, "App.services.api.request('GET', '/Print', null, { language: 'de' });");
            assertEquals(1, count(context));
            assertEquals("de", eval(context, "__requests[0].language"));
        }
    }

    /**
     * The picker names each language in that language, whatever the UI speaks (#8133): a Bulgarian user
     * looks for "Български", not "Bulgarian".
     */
    @Test
    void aLanguageIsNamedInItself() {
        try (Context context = load("en")) {
            assertEquals("Български", eval(context, "Alpine.store('locale').displayName('bg')"));
            assertEquals("English", eval(context, "Alpine.store('locale').displayName('en')"));
            assertEquals("Deutsch", eval(context, "Alpine.store('locale').displayName('de')"));
        }
    }

    /**
     * The labels follow the language the store settles on, not the saved code (#8632): the shipped
     * catalogs carry a language whether or not the instance offers it, so {@code i18n.js} resolving the
     * saved code against them rendered Bulgarian labels over English data and messages.
     */
    @Test
    void labelsIgnoreASavedLanguageTheInstanceDoesNotOffer() {
        try (Context context = load("bg", "stores/locale.js", "services/i18n.js")) {
            assertEquals(0, catalogRequests(context), "the catalogs were requested before the supported set was known");

            eval(context, "__releaseLanguages(['en']);");
            assertEquals(0, catalogRequests(context), "the default language renders from the baked labels");
            assertEquals("false", eval(context, "Alpine.store('i18n').ready"));
            assertEquals("Create", eval(context, "T('application-core:shell.nav.create', 'Create')"));
        }
    }

    @Test
    void labelsFollowASavedLanguageTheInstanceOffers() {
        try (Context context = load("bg", "stores/locale.js", "services/i18n.js")) {
            eval(context, "__releaseLanguages(['en', 'bg']);");
            assertEquals(1, catalogRequests(context));
            assertEquals("bg-BG", eval(context, "i18next.language"));
            assertEquals("Създаване", eval(context, "T('application-core:shell.nav.create', 'Create')"));
        }
    }

    /**
     * Every page that renders its labels through {@code i18n.js} also loads the locale store before
     * Alpine starts (#8632): without the store {@code i18n.js} can only take the saved code, and the
     * standalone pages (builder, capture, report file, form) rendered a language the instance no longer
     * offered.
     */
    @Test
    void everyPageTranslatingItsLabelsLoadsTheLocaleStore() {
        List<String> pages = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        for (Resource resource : resources("classpath*:META-INF/dirigible/**/*.html", "classpath*:META-INF/dirigible/**/*.html.template")) {
            String page = read(resource);
            int i18n = page.indexOf(I18N_SCRIPT);
            if (i18n < 0) {
                continue;
            }
            pages.add(resource.getFilename());
            int locale = page.indexOf(LOCALE_SCRIPT);
            int alpine = page.indexOf(ALPINE_SCRIPT);
            if (locale < 0 || alpine < 0 || locale > alpine) {
                problems.add(describe(resource));
            }
        }
        assertTrue(pages.size() >= 8, "found too few pages loading i18n.js to trust the scan: " + pages);
        assertTrue(problems.isEmpty(), "load i18n.js without the locale store before Alpine:\n  " + String.join("\n  ", problems));
    }

    private static int catalogRequests(Context context) {
        return context.eval("js", "__catalogRequests")
                      .asInt();
    }

    private static int count(Context context) {
        return context.eval("js", "__requests.length")
                      .asInt();
    }

    private static String eval(Context context, String expression) {
        return context.eval("js", expression)
                      .toString();
    }

    private static Context load(String savedLanguage) {
        return load(savedLanguage, "stores/locale.js", "services/api.js");
    }

    /**
     * Evaluate the given shipped shell scripts over the stubs they touch - a document that records the
     * {@code alpine:init} listeners and loads a script at once, an Alpine whose
     * {@code store(name, value)} calls {@code init()} as Alpine does, a localStorage holding the saved
     * language, an i18next serving flat catalogs, and a fetch that answers the locales service with one
     * Bulgarian label, records every data request and holds the languages response until released -
     * then fire {@code alpine:init}.
     */
    private static Context load(String savedLanguage, String... scripts) {
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
                          documentElement: { lang: '' },
                          addEventListener: function (type, listener) { if (type === 'alpine:init') __listeners.push(listener); },
                          createElement: function () { return {}; },
                          head: { appendChild: function (script) { Promise.resolve().then(function () { script.onload(); }); } }
                        };
                        var __stored = { 'codbex.harmonia.language': '%s' };
                        var localStorage = {
                          getItem: function (key) { return __stored[key] || null; },
                          setItem: function (key, value) { __stored[key] = value; }
                        };
                        var __stores = {};
                        var Alpine = {
                          store: function (name, value) {
                            if (value === undefined) return __stores[name];
                            __stores[name] = value;
                            if (typeof value.init === 'function') value.init();
                          }
                        };
                        var App = { services: {}, config: { restBase: '/api' } };
                        var i18next = {
                          init: function (options) { i18next.language = options.lng; i18next.catalogs = options.resources; return Promise.resolve(); },
                          lookup: function (key) {
                            var parts = key.split(':');
                            return ((i18next.catalogs[i18next.language] || {})[parts[0]] || {})[parts[1]];
                          },
                          exists: function (key) { return i18next.lookup(key) !== undefined; },
                          t: function (key) { return i18next.lookup(key); },
                          services: { formatter: { add: function () {} } }
                        };
                        var __catalogRequests = 0;
                        var __requests = [];
                        var __releaseLanguages;
                        var __failLanguages;
                        var fetch = function (url, init) {
                          if (url.indexOf('/services/platform/extensions/locales') >= 0) {
                            __catalogRequests++;
                            var catalogs = { locales: [{ id: 'en-US' }, { id: 'bg-BG' }],
                              translations: { 'bg-BG': { 'application-core': { 'shell.nav.create': 'Създаване' } } } };
                            return Promise.resolve({ ok: true, json: function () { return Promise.resolve(catalogs); } });
                          }
                          if (url.indexOf('/services/platform/languages') >= 0) {
                            return new Promise(function (resolve, reject) {
                              __releaseLanguages = function (codes) { resolve({ ok: true, json: function () { return Promise.resolve(codes); } }); };
                              __failLanguages = function () { reject(new TypeError('Failed to fetch')); };
                            });
                          }
                          __requests.push({ url: url, language: (init.headers || {})['Accept-Language'] || null });
                          return Promise.resolve({ ok: true, status: 200, text: function () { return Promise.resolve('[]'); } });
                        };
                        """.formatted(
                        savedLanguage));
        for (String script : scripts) {
            evalResource(context, SHELL_JS + script);
        }
        context.eval("js", "__listeners.forEach(function (listener) { listener(); });");
        return context;
    }

    private static void evalResource(Context context, String resource) {
        String source = read(resource);
        try {
            context.eval("js", source);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to evaluate " + resource, ex);
        }
    }

    private static List<Resource> resources(String... patterns) {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        List<Resource> resources = new ArrayList<>();
        for (String pattern : patterns) {
            try {
                resources.addAll(List.of(resolver.getResources(pattern)));
            } catch (IOException ex) {
                throw new UncheckedIOException("Cannot resolve " + pattern, ex);
            }
        }
        return resources;
    }

    private static String describe(Resource resource) {
        try {
            return resource.getURL()
                           .toString();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static String read(Resource resource) {
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read " + describe(resource), ex);
        }
    }

    private static String read(String resource) {
        try (InputStream in = HarmoniaLocaleStoreIT.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read " + resource, ex);
        }
    }
}
