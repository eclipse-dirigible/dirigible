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
 * A page re-reads itself while a record it shows is in a moving status (dirigible #8375):
 * {@code basePage.pollWhile} - the one implementation every generated page wires to its entity's
 * intent {@code refresh:} - exercised by evaluating the shipped {@code basePage.js} in a polyglot
 * context over fake timers, a fake clock and a fake {@code document}.
 */
class HarmoniaPollWhileIT {

    private static final String SHELL = "/META-INF/dirigible/application-core/shell/js/";

    /**
     * A page polling every 10 s while {@code __moving}; each re-read counts itself and, as the
     * generated pages do, calls {@code pollCheck()} after it.
     */
    private static final String POLLING_PAGE = "var page = basePage(); page.items = ['IN_TRANSIT'];"
            + " page.pollWhile(() => __moving, function () { __reads++; if (__fail) return Promise.reject(new Error('down'));"
            + " this.pollCheck(); return Promise.resolve(); }, 10000); page.pollCheck();";

    @Test
    void pollsOnlyWhileMoving() {
        try (Context context = load()) {
            context.eval("js", "__moving = false; " + POLLING_PAGE);
            assertEquals("", pending(context), "a settled page schedules nothing");
            context.eval("js", "__moving = true; page.pollCheck();");
            assertEquals("10000", pending(context), "a moving record schedules one re-read, never an interval");
            context.eval("js", "__tick();");
            assertEquals(1, reads(context));
            assertEquals("10000", pending(context), "still moving: the next re-read is scheduled");
            context.eval("js", "__moving = false; __tick();");
            assertEquals(2, reads(context));
            assertEquals("", pending(context), "settled: polling ends");
            assertEquals(0, context.eval("js", "__intervals")
                                   .asInt(),
                    "no interval is ever started");
        }
    }

    @Test
    void skipsAHiddenTabAndCatchesUpOnShow() {
        try (Context context = load()) {
            context.eval("js", "__moving = true; " + POLLING_PAGE);
            context.eval("js", "document.hidden = true; __tick();");
            assertEquals(0, reads(context), "a hidden tab sends no request");
            assertEquals("", pending(context), "and keeps no timer running");
            context.eval("js", "document.hidden = false; __fire('visibilitychange');");
            assertEquals(1, reads(context), "the poll that fell due runs as soon as the tab shows");
            assertEquals("10000", pending(context));
        }
    }

    @Test
    void stopsAfterTenMinutes() {
        try (Context context = load()) {
            context.eval("js", "__moving = true; " + POLLING_PAGE);
            context.eval("js", "for (var i = 0; i < 100 && __timers.length; i++) __tick();");
            assertEquals("", pending(context), "polling stops by itself");
            assertEquals(59, reads(context), "nothing is sent ten minutes after polling began");
            context.eval("js", "page.pollCheck(true);");
            assertEquals("10000", pending(context), "a Refresh or a re-entry restarts the window");
        }
    }

    @Test
    void aFailedPollKeepsTheRowsAndStops() {
        try (Context context = load()) {
            context.eval("js", "__moving = true; __fail = true; " + POLLING_PAGE);
            context.eval("js", "__tick();");
            assertEquals(1, reads(context));
            assertEquals("", pending(context), "a failed re-read stops polling");
            assertEquals("IN_TRANSIT", context.eval("js", "page.items.join(',')")
                                              .asString(),
                    "the rows on screen stay");
            assertEquals(1, context.eval("js", "__errors.length")
                                   .asInt(),
                    "the failure is logged");
            context.eval("js", "page.pollCheck();");
            assertEquals("", pending(context), "a read alone does not resume a failed poll");
            context.eval("js", "__fail = false; page.pollCheck(true);");
            assertEquals("10000", pending(context), "a Refresh resumes it");
        }
    }

    @Test
    void destroyClearsTheTimer() {
        try (Context context = load()) {
            context.eval("js", "__moving = true; " + POLLING_PAGE);
            assertEquals("10000", pending(context));
            context.eval("js", "page.destroy();");
            assertEquals("", pending(context), "a page navigated away from keeps no timer");
            context.eval("js", "document.hidden = false; __fire('visibilitychange'); page.pollCheck();");
            assertEquals(0, reads(context), "nor reads again");
        }
    }

    private static String pending(Context context) {
        return context.eval("js", "__timers.map(t => t.ms).join(',')")
                      .asString();
    }

    private static int reads(Context context) {
        return context.eval("js", "__reads")
                      .asInt();
    }

    /**
     * A polyglot context with the shipped {@code basePage.js} over stubs: timers the test fires by hand
     * ({@code __tick} also moves the clock by the timer's delay), a document whose visibility the test
     * sets, and a console that records errors. Every promise settles before the evaluation that started
     * it returns.
     */
    private static Context load() {
        Context context = Context.newBuilder("js")
                                 .allowAllAccess(true)
                                 .option("engine.WarnInterpreterOnly", "false")
                                 .build();
        context.eval("js", """
                var window = this;
                window.addEventListener = function () {};
                window.removeEventListener = function () {};
                var __documentListeners = {};
                var document = {
                  hidden: false,
                  addEventListener: function (type, fn) { (__documentListeners[type] = __documentListeners[type] || []).push(fn); },
                  removeEventListener: function (type, fn) {
                    __documentListeners[type] = (__documentListeners[type] || []).filter(function (f) { return f !== fn; });
                  }
                };
                var __fire = function (type) { (__documentListeners[type] || []).slice().forEach(function (fn) { fn({ type: type }); }); };
                var __now = 1000000;
                Date.now = function () { return __now; };
                var __timers = [];
                var __timerIds = 0;
                function setTimeout(fn, ms) { var t = { id: ++__timerIds, fn: fn, ms: ms }; __timers.push(t); return t.id; }
                function clearTimeout(id) { __timers = __timers.filter(function (t) { return t.id !== id; }); }
                var __intervals = 0;
                function setInterval() { __intervals++; return 0; }
                var __tick = function () { var t = __timers.shift(); if (t) { __now += t.ms; t.fn(); } };
                var __errors = [];
                var console = { log: function () {}, warn: function () {}, error: function () { __errors.push(arguments); } };
                var App = {};
                var __moving = false;
                var __fail = false;
                var __reads = 0;
                """);
        context.eval("js", read(SHELL + "components/pages/basePage.js"));
        return context;
    }

    private static String read(String resource) {
        try (InputStream in = HarmoniaPollWhileIT.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
