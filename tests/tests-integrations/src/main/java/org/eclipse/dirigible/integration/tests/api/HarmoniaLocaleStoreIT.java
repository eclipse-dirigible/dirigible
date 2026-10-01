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
import org.junit.jupiter.api.Test;

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
 * the settled language.
 */
class HarmoniaLocaleStoreIT {

    private static final String SHELL_JS = "/META-INF/dirigible/application-core/shell/js/";

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

    private static int count(Context context) {
        return context.eval("js", "__requests.length")
                      .asInt();
    }

    private static String eval(Context context, String expression) {
        return context.eval("js", expression)
                      .toString();
    }

    /**
     * Evaluate the shipped store and fetch client over the stubs they touch - a document that records
     * the {@code alpine:init} listener, an Alpine whose {@code store(name, value)} calls {@code init()}
     * as Alpine does, a localStorage holding the saved language, and a fetch that records every data
     * request and holds the languages response until released - then fire {@code alpine:init}.
     */
    private static Context load(String savedLanguage) {
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
                          addEventListener: function (type, listener) { if (type === 'alpine:init') __listeners.push(listener); }
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
                        var __requests = [];
                        var __releaseLanguages;
                        var __failLanguages;
                        var fetch = function (url, init) {
                          if (url.indexOf('application-languages') >= 0) {
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
        evalResource(context, SHELL_JS + "stores/locale.js");
        evalResource(context, SHELL_JS + "services/api.js");
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
