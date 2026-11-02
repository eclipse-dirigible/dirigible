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
 * A toast shows its whole message (dirigible #8365): the shared {@code notifications} store hands
 * the toast template the announcement's title and description apart - the title on its one line,
 * the description under it, wrapping - and keeps the joined {@code message} a page generated before
 * that layout renders. The store is evaluated in a polyglot context, so the test runs the code the
 * browser runs; the templates are read as every shell ships them.
 */
class HarmoniaToastIT {

    private static final String STORE = "/META-INF/dirigible/application-core/shell/js/stores/notifications.js";

    /** Every page that declares the {@code toast} template the store raises. */
    private static final List<String> TOAST_TEMPLATES = List.of(
            "/META-INF/dirigible/template-application-ui-harmonia-java/ui/shell/index.html.template",
            "/META-INF/dirigible/application/index.html", "/META-INF/dirigible/admin/index.html", "/META-INF/dirigible/partner/index.html",
            "/META-INF/dirigible/personal/index.html", "/META-INF/dirigible/builder/index.html");

    @Test
    void anAnnouncementCarriesTitleDescriptionAndTheJoinedMessage() throws Exception {
        try (Context context = load()) {
            String description = "This person is already a member of the team. Pick another address or change their roles instead.";
            context.eval("js", "store.announce({ title: 'Add member', description: '" + description + "', variant: 'negative' });");

            assertEquals("toast|Add member|" + description + "|Add member - " + description + "|negative|12000",
                    context.eval("js",
                            "var t = __toasts[0]; [t.template, t.data.title, t.data.description, t.data.message,"
                                    + " t.data.variant, t.timeout].join('|')")
                           .asString());
            assertEquals("Add member|" + description, context.eval("js", "store.items[0].title + '|' + store.items[0].description")
                                                             .asString(),
                    "the bell entry is recorded as before");

            context.eval("js", "store.toast('Saved');");
            assertEquals("||Saved|information", context
                                                       .eval("js",
                                                               "var t = __toasts[1]; [t.data.title, t.data.description, t.data.message,"
                                                                       + " t.data.variant].join('|')")
                                                       .asString(),
                    "a short confirmation keeps its one line");
        }
    }

    @Test
    void everyToastTemplateRendersTheDescription() throws Exception {
        for (String page : TOAST_TEMPLATES) {
            String markup = read(page);
            int start = markup.indexOf("<template id=\"toast\">");
            assertTrue(start >= 0, page + " declares no toast template");
            String toast = markup.substring(start, markup.indexOf("</template>\n", markup.indexOf("x-h-notification-actions", start)));
            assertTrue(toast.contains("<span x-h-notification-title x-show=\"title || !description\" x-text=\"title || message\"></span>"),
                    page + ": the title, on its one line, falling back to the message of a plain toast");
            assertTrue(toast.contains("<span x-h-notification-description x-show=\"description\" x-text=\"description\"></span>"),
                    page + ": the description under it, which wraps");
        }
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
                        var __documentListeners = {};
                        var document = { addEventListener: function (type, fn) { (__documentListeners[type] = __documentListeners[type] || []).push(fn); } };
                        var __stores = {};
                        var Alpine = { store: function (name, value) { if (value !== undefined) { __stores[name] = value; } return __stores[name]; } };
                        var console = { log: function () {}, warn: function () {}, error: function () {} };
                        var __toasts = [];
                        """);
        context.eval("js", read(STORE));
        context.eval("js", "__documentListeners['alpine:init'].forEach(function (fn) { fn(); }); var store = Alpine.store('notifications');"
                + " store.attachToaster({ add: function (n) { __toasts.push(n); } });");
        return context;
    }

    private static String read(String resource) throws Exception {
        try (InputStream in = HarmoniaToastIT.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
