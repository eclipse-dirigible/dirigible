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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The IDE is not translated: its {@code LocaleService} initializes in {@code en-US} whatever an
 * earlier version left in the browser's storage (dirigible #8736, #8793).
 *
 * <p>
 * A language saved by the removed IDE language picker - one the instance still has catalogs for, or
 * one it never had - used to be sent to the locales service, which refuses a language it does not
 * register; i18next then never initialized and every {@code onInit} callback stayed queued, so the
 * Workbench never rendered its translated texts. The shipped {@code locale.js} is evaluated in a
 * polyglot context over stubs of what it touches - AngularJS's module registration, a
 * {@code localStorage} holding the stale language, an i18next and an {@code Extensions} service
 * whose translations response is held back until the test settles it.
 */
class IdeLocaleServiceIT {

    private static final String LOCALE_SERVICE = "/META-INF/dirigible/platform-core/ui/platform/locale.js";

    /**
     * The key the removed picker saved the IDE's language under, for the stubbed branding and config.
     */
    private static final String STORAGE_KEY = "dirigible.ide.locale.language";

    @ParameterizedTest
    @ValueSource(strings = {"bg-BG", "xx-XX"})
    void aSavedLanguageIsIgnoredAndTheQueuedCallbacksFire(String savedLanguage) {
        try (Context context = load(savedLanguage)) {
            eval(context, "__service.onInit(function () { __fired.push('queued'); });");
            assertEquals("0", eval(context, "__fired.length"), "a callback fired before i18next initialized");

            eval(context, "__settle(true);");
            assertEquals("en-US", eval(context, "String(__translationRequests[0].langs)"));
            assertEquals("en-US", eval(context, "i18next.options.lng"));
            assertEquals("queued", eval(context, "__fired.join(',')"));

            eval(context, "__service.onInit(function () { __fired.push('late'); });");
            assertEquals("queued,late", eval(context, "__fired.join(',')"));
            assertEquals("en-US", eval(context, "__service.getLanguage()"));
            assertEquals("0", eval(context, "__storageAccesses"), "the IDE read or wrote a saved language");
            assertEquals(savedLanguage, eval(context, "__stored['" + STORAGE_KEY + "']"));
        }
    }

    @Test
    void aRefusedTranslationRequestStillFiresTheQueuedCallbacks() {
        try (Context context = load("en-US")) {
            eval(context, "__service.onInit(function () { __fired.push('queued'); });");
            eval(context, "__settle(false);");
            assertEquals("queued", eval(context, "__fired.join(',')"));
            assertEquals("en-US", eval(context, "i18next.options.lng"));
        }
    }

    private static String eval(Context context, String expression) {
        return context.eval("js", expression)
                      .toString();
    }

    /**
     * Evaluate the shipped {@code locale.js} over its stubs and build the service the way AngularJS's
     * injector does: the provider is constructed, then its {@code $get} runs with the provider as
     * {@code this}.
     */
    private static Context load(String savedLanguage) {
        Context context = Context.newBuilder("js")
                                 .allowAllAccess(true)
                                 // See HarmoniaDateFormatIT: the interpreter-only notice corrupts the forked JVM's channel.
                                 .option("engine.WarnInterpreterOnly", "false")
                                 .build();
        context.eval("js", """
                var window = this;
                var top = this;
                var getBrandingInfo = function () { return { prefix: 'dirigible' }; };
                var getConfigData = function () { return { id: 'ide' }; };
                var __stored = { '%s': '%s' };
                var __storageAccesses = 0;
                var localStorage = {
                  getItem: function (key) { __storageAccesses++; return __stored[key] || null; },
                  setItem: function (key, value) { __storageAccesses++; __stored[key] = value; }
                };
                var i18next = {
                  isInitialized: false,
                  init: function (options) { i18next.options = options; i18next.isInitialized = true; return Promise.resolve(); },
                  t: function (key) { return key; }
                };
                var __provider;
                var angular = {
                  isDefined: function (value) { return value !== undefined; },
                  module: function () {
                    var module = {
                      provider: function (name, constructor) { __provider = constructor; return module; },
                      filter: function () { return module; }
                    };
                    return module;
                  }
                };
                var __translationRequests = [];
                var __settle;
                var Extensions = {
                  getTranslations: function (request) {
                    __translationRequests.push(request);
                    return new Promise(function (resolve, reject) {
                      __settle = function (ok) {
                        if (ok) resolve({ data: { locales: [{ id: 'en-US' }], translations: { 'en-US': {} } } });
                        else reject({ status: 500 });
                      };
                    });
                  }
                };
                var $rootScope = { $applyAsync: function (callback) { callback(); } };
                var __fired = [];
                """.formatted(STORAGE_KEY, savedLanguage));
        context.eval("js", read(LOCALE_SERVICE));
        context.eval("js", """
                var __providerInstance = new __provider();
                var __factory = __providerInstance.$get;
                var __service = __factory[__factory.length - 1].call(__providerInstance, $rootScope, Extensions);
                """);
        return context;
    }

    private static String read(String resource) {
        try (InputStream in = IdeLocaleServiceIT.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read " + resource, ex);
        }
    }
}
