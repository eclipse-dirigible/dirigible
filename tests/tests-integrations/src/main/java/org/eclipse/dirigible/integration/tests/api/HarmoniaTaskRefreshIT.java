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
import java.util.List;

import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.Test;

/**
 * A page re-reads the record it shows once a BPM task of that record completes (dirigible #7612).
 *
 * <p>
 * Completing the Issue task from a document page left the page showing the pre-Issue number, status
 * and an empty Copies panel until the browser was reloaded: the task form closed, the
 * {@code processTasks} store re-fetched the inbox, and nothing told the page. Worse, a single
 * re-read on completion would race the chain behind the task (number, status, totals, the snapshot
 * copy), which runs behind Flowable's async boundaries. The store now announces the processes whose
 * task set changed ({@code harmonia:tasks-changed}), re-reads on a bounded schedule after a
 * completed form closed, and every process-aware page and the shared detail panel subscribe through
 * {@code basePage.onTasksChanged}, filtered by the record's own process.
 *
 * <p>
 * The store and the mixin are exercised by evaluating the shipped files in a polyglot context over
 * stubs of the browser surface they touch; the templates are then swept for the subscription and
 * the Refresh action.
 */
class HarmoniaTaskRefreshIT {

    private static final String SHELL = "/META-INF/dirigible/application-core/shell/js/";

    private static final String UI_BASE = "/META-INF/dirigible/template-application-ui-harmonia-java/ui/";

    /** Every generated page that surfaces a record's BPM tasks. */
    private static final List<String> PROCESS_PAGES = List.of("perspective/document/document-page.js.template",
            "perspective/manage/form-page.js.template", "perspective/manage/list-page.js.template");

    /** Every generated view rendering the shared detail panel. */
    private static final List<String> PANEL_VIEWS =
            List.of("perspective/document/document-view.html.template", "perspective/manage/form-view.html.template");

    private static final String ISSUE_TASK = "{ id: 't1', processInstanceId: 'p1', name: 'Issue', formKey: '/form', mine: true }";

    @Test
    void aReReadAnnouncesTheProcessesWhoseTaskSetChanged() {
        try (Context context = load()) {
            context.eval("js", "__inbox.assignee = [" + ISSUE_TASK + "]; store.init();");
            assertEquals("", announced(context), "the first reading is a baseline, not a change");
            context.eval("js", "store.load();");
            assertEquals("", announced(context), "a reading that changed nothing announces nothing");
            context.eval("js", "__inbox.assignee = []; store.load();");
            assertEquals("p1", announced(context), "the task completed elsewhere: its process is announced");
            context.eval("js", "__inbox.groups = [{ id: 't2', processInstanceId: 'p2', name: 'Approve' }]; store.load();");
            assertEquals("p1|p2", announced(context), "a task appearing announces its process");
        }
    }

    @Test
    void aCompletedTaskFormIsFollowedByABoundedSeriesOfReReads() {
        try (Context context = load()) {
            context.eval("js", "__inbox.assignee = [" + ISSUE_TASK + "]; store.init(); store.openTask(" + ISSUE_TASK + ");");
            assertEquals("p1", context.eval("js", "store.formProcessInstanceId")
                                      .asString());
            // The form completes the task (it is gone from the inbox) and asks to close.
            context.eval("js", "__inbox.assignee = []; store.closeForm(true);");
            assertEquals("p1", announced(context), "the re-read on close announces the process whose task vanished");
            assertEquals("2000", pendingDelays(context), "a follow-up is scheduled");
            for (String expected : List.of("3000", "5000", "10000")) {
                context.eval("js", "__runTimer();");
                assertEquals(expected, pendingDelays(context), "each tick schedules the next, later one");
            }
            context.eval("js", "__runTimer();");
            assertEquals("", pendingDelays(context), "the series is bounded");
            assertEquals("p1|p1|p1|p1|p1", announced(context),
                    "every tick tells the record's page to look again, so the chain's last writes are picked up");
        }
    }

    @Test
    void theSeriesStopsOnceTheProcessReachedItsNextWaitState() {
        try (Context context = load()) {
            context.eval("js", "__inbox.assignee = [" + ISSUE_TASK + "]; store.init(); store.openTask(" + ISSUE_TASK + ");");
            // Each step is its own evaluation: the follow-up is scheduled once the re-read on close settled,
            // which happens when the evaluation that closed the form returns.
            context.eval("js", "__inbox.assignee = []; store.closeForm(true);");
            context.eval("js", "__runTimer();");
            assertEquals("p1|p1", announced(context));
            // The chain ran through to the next user task of the same process.
            context.eval("js", "__inbox.assignee = [{ id: 't3', processInstanceId: 'p1', name: 'Confirm' }]; __runTimer();");
            assertEquals("p1|p1|p1", announced(context), "the new task is announced by the re-read itself");
            assertEquals("", pendingDelays(context), "nothing more to wait for: the chain up to the wait state has run");
        }
    }

    @Test
    void aDismissedTaskFormSchedulesNoFollowUp() {
        try (Context context = load()) {
            context.eval("js",
                    "__inbox.assignee = [" + ISSUE_TASK + "]; store.init(); store.openTask(" + ISSUE_TASK + "); store.closeForm();");
            assertEquals("", announced(context), "the task is still there: nothing changed");
            assertEquals("", pendingDelays(context), "only a completion has a chain to wait for");
        }
    }

    @Test
    void aPageReReadsOnlyForTheProcessOfItsOwnRecord() {
        try (Context context = load()) {
            context.eval("js", "var page = basePage(); var __reloads = 0; page.record = { ProcessId: 7 };"
                    + " page.onTasksChanged(function () { return this.record.ProcessId; }, function () { __reloads++; });");
            context.eval("js",
                    "window.dispatchEvent(new CustomEvent('harmonia:tasks-changed', { detail: { processInstanceIds: ['8'] } }));");
            assertEquals(0, reloads(context), "another record's process re-reads nothing");
            context.eval("js",
                    "window.dispatchEvent(new CustomEvent('harmonia:tasks-changed', { detail: { processInstanceIds: ['8', '7'] } }));");
            assertEquals(1, reloads(context), "the record's own process re-reads, compared as text (a numeric ProcessId)");
            context.eval("js", "page.record = {};"
                    + " window.dispatchEvent(new CustomEvent('harmonia:tasks-changed', { detail: { processInstanceIds: ['7'] } }));");
            assertEquals(1, reloads(context), "a page without a record re-reads nothing");
            context.eval("js", "page.record = { ProcessId: 7 }; page.destroy();"
                    + " window.dispatchEvent(new CustomEvent('harmonia:tasks-changed', { detail: { processInstanceIds: ['7'] } }));");
            assertEquals(1, reloads(context), "a page navigated away from must not keep reloading");
        }
    }

    @Test
    void everyProcessAwarePageAndTheSharedPanelSubscribe() throws Exception {
        for (String page : PROCESS_PAGES) {
            assertTrue(read(UI_BASE + page).contains("this.onTasksChanged("),
                    page + " does not re-read when a task of its record completes");
        }
        String panel = read(SHELL + "components/detailPanel.js");
        assertTrue(panel.contains("this.onTasksChanged(() => this.master && this.master.ProcessId"),
                "the shared detail panel does not re-read when a task of its master completes");
        assertTrue(panel.contains("async refresh()"), "the shared detail panel has no refresh action");
        for (String view : PANEL_VIEWS) {
            String content = read(UI_BASE + view);
            assertTrue(content.contains("@click=\"refresh()\"") && content.contains("data-lucide=\"refresh-cw\""),
                    view + " renders a detail panel without a Refresh action");
        }
        String form = read(UI_BASE + "perspective/manage/form-view.html.template");
        assertTrue(form.contains("+ (isPreview ? '/preview' : '/edit')}, id, form)"),
                "the form does not hand its record to its panels, so they cannot tell whose task completed");
        // A child row's own task is offered on the form's detail table, on Preview too - the row menu
        // is withheld there, and with the master-detail browse pane retired the form is where a
        // master's children are worked from (#7390).
        assertTrue(form.contains("<template x-for=\"task in $store.processTasks.getTasks(row)\" :key=\"task.id\">"),
                "the form's detail rows do not offer their own actionable tasks");
        assertTrue(read(UI_BASE + "perspective/manage/list-page.js.template").contains("await this.load(true)"),
                "the manage list's task-driven re-read is not quiet - it would blank the rows behind the open sheet");
    }

    private static String announced(Context context) {
        return context.eval("js", "__announced().join('|')")
                      .asString();
    }

    private static String pendingDelays(Context context) {
        return context.eval("js", "__timers.map(function (t) { return t.ms; }).join('|')")
                      .asString();
    }

    private static int reloads(Context context) {
        return context.eval("js", "__reloads")
                      .asInt();
    }

    /**
     * Evaluate the shipped processTasks.js and basePage.js over stubs of what they touch: a window to
     * publish on, Alpine's store registry, an inbox the API client answers from, and timers the test
     * fires by hand. Every promise settles synchronously, so a script's async work has completed by the
     * time its evaluation returns.
     */
    private static Context load() {
        Context context = Context.newBuilder("js")
                                 .allowAllAccess(true)
                                 // The interpreter-only notice is written straight to the native stream,
                                 // which the forked test JVM reports as a corrupted channel.
                                 .option("engine.WarnInterpreterOnly", "false")
                                 .build();
        context.eval("js",
                """
                        var window = this;
                        var __listeners = {};
                        var __events = [];
                        window.addEventListener = function (type, fn) { (__listeners[type] = __listeners[type] || []).push(fn); };
                        window.removeEventListener = function (type, fn) {
                          __listeners[type] = (__listeners[type] || []).filter(function (f) { return f !== fn; });
                        };
                        window.dispatchEvent = function (e) {
                          __events.push(e);
                          (__listeners[e.type] || []).forEach(function (fn) { fn(e); });
                          return true;
                        };
                        function CustomEvent(type, init) { this.type = type; this.detail = init && init.detail; }
                        var __announced = function () {
                          return __events.filter(function (e) { return e.type === 'harmonia:tasks-changed'; })
                                         .map(function (e) { return e.detail.processInstanceIds.join(','); });
                        };
                        var __documentListeners = {};
                        var document = { addEventListener: function (type, fn) { (__documentListeners[type] = __documentListeners[type] || []).push(fn); } };
                        var __stores = {};
                        var Alpine = { store: function (name, value) { if (value !== undefined) { __stores[name] = value; } return __stores[name]; } };
                        var __timers = [];
                        var __timerIds = 0;
                        function setTimeout(fn, ms) { var t = { id: ++__timerIds, fn: fn, ms: ms }; __timers.push(t); return t.id; }
                        function clearTimeout(id) { __timers = __timers.filter(function (t) { return t.id !== id; }); }
                        function setInterval() { return 0; }
                        function clearInterval() {}
                        var __runTimer = function () { var t = __timers.shift(); if (t) t.fn(); };
                        var console = { log: function () {}, warn: function () {}, error: function () {} };
                        var __inbox = { assignee: [], groups: [] };
                        var App = { config: {}, services: { api: {
                          get: function (url) { return Promise.resolve(__inbox[/type=assignee/.test(url) ? 'assignee' : 'groups']); },
                          post: function () { return Promise.resolve({}); }
                        } } };
                        """);
        for (String script : List.of("stores/processTasks.js", "components/pages/basePage.js")) {
            try {
                context.eval("js", read(SHELL + script));
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to evaluate " + script, ex);
            }
        }
        // Alpine would fire alpine:init and construct the store; the store's own init() is called by each
        // test.
        context.eval("js",
                "__documentListeners['alpine:init'].forEach(function (fn) { fn(); }); var store = Alpine.store('processTasks');");
        return context;
    }

    private static String read(String resource) throws Exception {
        try (InputStream in = HarmoniaTaskRefreshIT.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
