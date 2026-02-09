/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.web.watcher;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HtmlPlatformLinksInjectorTest {

    private HtmlPlatformLinksInjector injector;

    @BeforeEach
    void setup() {
        // Example assets (could be loaded from JSON)
        List<PlatformAsset> assets =
                List.of(new PlatformAsset(PlatformAsset.Type.CSS, "/services/platform/css/platform.css", "head-css", false, false),
                        new PlatformAsset(PlatformAsset.Type.CSS, "/services/platform/css/theme.css", "head-css", false, false),
                        new PlatformAsset(PlatformAsset.Type.SCRIPT, "/services/platform/js/platform.js", "body-js", false, true),
                        new PlatformAsset(PlatformAsset.Type.SCRIPT, "/services/platform/js/components.js", "body-js", true, true));

        injector = new HtmlPlatformLinksInjector(assets);
    }

    @Test
    void testCssInjectionInHead() {
        String html = """
                <html>
                    <head>
                        <meta name="platform-links" category="head-css">
                    </head>
                    <body></body>
                </html>
                """;

        String result = injector.processHtml(html);

        Document doc = Jsoup.parse(result);
        Element head = doc.head();

        assertEquals(2, head.select("link[rel=stylesheet]")
                            .size());
        assertTrue(head.select("link[href=/services/platform/css/platform.css]")
                       .size() == 1);
        assertTrue(head.select("link[href=/services/platform/css/theme.css]")
                       .size() == 1);
        // placeholder removed
        assertEquals(0, head.select("meta[name=platform-links]")
                            .size());
    }

    @Test
    void testScriptInjectionInBody() {
        String html = """
                <html>
                    <head></head>
                    <body>
                        <meta name="platform-links" category="body-js">
                    </body>
                </html>
                """;

        String result = injector.processHtml(html);
        Document doc = Jsoup.parse(result);
        Element body = doc.body();

        assertEquals(2, body.select("script")
                            .size());
        assertNotNull(body.select("script[src=/services/platform/js/platform.js]")
                          .first());
        assertNotNull(body.select("script[src=/services/platform/js/components.js]")
                          .first());

        // Check attributes
        assertEquals("module", body.select("script[src=/services/platform/js/components.js]")
                                   .attr("type"));
        // assertEquals("defer", body.select("script[src=/services/platform/js/components.js]")
        // .attr("defer"));
        // assertEquals("defer", body.select("script[src=/services/platform/js/platform.js]")
        // .attr("defer"));

        // placeholder removed
        assertEquals(0, body.select("meta[name=platform-links]")
                            .size());
    }

    @Test
    void testMultipleCategories() {
        String html = """
                <html>
                    <head>
                        <meta name="platform-links" category="head-css,body-js">
                    </head>
                    <body></body>
                </html>
                """;

        String result = injector.processHtml(html);
        Document doc = Jsoup.parse(result);

        // CSS injected in head
        assertEquals(2, doc.head()
                           .select("link[rel=stylesheet]")
                           .size());

        // Scripts injected before placeholder in head (for simplicity, that's how injector works now)
        assertEquals(2, doc.head()
                           .select("script")
                           .size());

        // placeholder removed
        assertEquals(0, doc.head()
                           .select("meta[name=platform-links]")
                           .size());
    }

    @Test
    void testUnknownCategoryLogsError() {
        String html = """
                <html>
                    <head>
                        <meta name="platform-links" category="unknown-category">
                    </head>
                    <body></body>
                </html>
                """;

        // We just verify it does not throw
        String result = injector.processHtml(html);
        Document doc = Jsoup.parse(result);

        assertEquals(0, doc.head()
                           .select("link, script")
                           .size());
        assertEquals(0, doc.head()
                           .select("meta[name=platform-links]")
                           .size());
    }
}

