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
 * The request a {@code call:} action sends (issue #8090), built by the shipped
 * {@code customActions} store evaluated in a polyglot context: {@code {id}} URL-encoded into the
 * path, a whole-string placeholder typed by its control, a placeholder inside a longer string
 * interpolated as text, constants sent as written, and no body at all when none is declared. The
 * browser journey is {@code IntentCallActionSampleIT}.
 */
class CustomActionsCallIT {

    private static final String STORE = "/META-INF/dirigible/application-core/shell/js/stores/customActions.js";

    private static final String ESCALATE = """
            { label: 'Escalate', success: 'Escalation requested',
              call: { method: 'POST', path: '/services/java/helpdesk/custom/api/TicketApi/tickets/{id}/escalations',
                      body: { kind: 'MANUAL', level: '{level}', note: '{note}', urgent: '{urgent}', due: '{due}',
                              summary: 'level {level}: {note}', tags: ['{note}', 7] } },
              promptColumns: [ { name: 'level', widget: 'NUMBER' }, { name: 'note', widget: 'TEXT' },
                               { name: 'urgent', widget: 'CHECKBOX' }, { name: 'due', widget: 'DATE' } ] }""";

    @Test
    void theBodyIsTypedByThePromptControls() throws Exception {
        try (Context context = load()) {
            context.eval("js", "store.runCall(" + ESCALATE + ", 'A/1', { level: '2', note: 'VIP', due: '' });");

            assertEquals("POST /services/java/helpdesk/custom/api/TicketApi/tickets/A%2F1/escalations "
                    + "{\"kind\":\"MANUAL\",\"level\":2,\"note\":\"VIP\",\"urgent\":false,\"due\":null,\"summary\":\"level 2: VIP\","
                    + "\"tags\":[\"VIP\",7]}", sent(context));
            assertEquals("Escalate: Escalation requested", announced(context));
        }
    }

    @Test
    void anEmptyOptionalInputIsSentAsNull() throws Exception {
        try (Context context = load()) {
            context.eval("js", "store.runCall(" + ESCALATE + ", 1, { level: 3 });");

            assertEquals("POST /services/java/helpdesk/custom/api/TicketApi/tickets/1/escalations "
                    + "{\"kind\":\"MANUAL\",\"level\":3,\"note\":null,\"urgent\":false,\"due\":null,\"summary\":\"level 3: \","
                    + "\"tags\":[null,7]}", sent(context));
        }
    }

    @Test
    void aCallWithoutBodySendsNone() throws Exception {
        try (Context context = load()) {
            context.eval("js",
                    "store.runCall({ label: 'Retry', call: { method: 'DELETE', path: '/services/x/escalations/{id}/retry' } }, 4);");

            assertEquals("DELETE /services/x/escalations/4/retry undefined", sent(context));
            assertEquals("Retry: Retry completed", announced(context));
        }
    }

    @Test
    void aRefusalShowsTheEndpointsMessage() throws Exception {
        try (Context context = load()) {
            context.eval("js", "__answer = Promise.reject({ httpStatus: 409, errorMessage: 'Ticket [2] is CLOSED' });" + " store.runCall("
                    + ESCALATE + ", 2, { level: 1 });");

            assertEquals("Escalate: Ticket [2] is CLOSED", announced(context));
        }
    }

    private static String sent(Context context) {
        return context.eval("js", "__sent")
                      .asString();
    }

    private static String announced(Context context) {
        return context.eval("js", "__announced")
                      .asString();
    }

    private static Context load() throws Exception {
        Context context = Context.newBuilder("js")
                                 .allowAllAccess(true)
                                 // The interpreter-only notice is written straight to the native stream,
                                 // which the forked test JVM reports as a corrupted channel.
                                 .option("engine.WarnInterpreterOnly", "false")
                                 .build();
        context.eval("js",
                """
                        var window = this;
                        window.dispatchEvent = function () {};
                        var CustomEvent = function (type) { this.type = type; };
                        var __documentListeners = {};
                        var document = { addEventListener: function (type, fn) { (__documentListeners[type] = __documentListeners[type] || []).push(fn); } };
                        var __stores = {};
                        var Alpine = { store: function (name, value) { if (value !== undefined) { __stores[name] = value; } return __stores[name]; }, data: function () {} };
                        var console = { log: function () {}, warn: function () {}, error: function () {} };
                        var __sent = '', __announced = '', __answer = null;
                        Alpine.store('notifications', { announce: function (n) { __announced = n.title + ': ' + n.description; } });
                        var App = { config: {}, services: {
                          api: { request: function (method, url, body, opts) {
                            __sent = method + ' ' + opts.baseUrl + url + ' ' + (body === undefined ? 'undefined' : JSON.stringify(body));
                            return __answer || Promise.resolve('');
                          } },
                          apiErrors: { refusalMessageFor: function (e, fallback) { return (e && e.errorMessage) || fallback; } }
                        } };
                        """);
        context.eval("js", read(STORE));
        context.eval("js",
                "__documentListeners['alpine:init'].forEach(function (fn) { fn(); }); var store = Alpine.store('customActions');");
        return context;
    }

    private static String read(String resource) throws Exception {
        try (InputStream in = CustomActionsCallIT.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
