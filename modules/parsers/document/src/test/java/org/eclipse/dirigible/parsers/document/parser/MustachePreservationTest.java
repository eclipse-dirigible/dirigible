/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.parsers.document.parser;

import static org.junit.Assert.assertEquals;

import org.eclipse.dirigible.parsers.document.Node;
import org.junit.Test;

/**
 * The parser must never evaluate Mustache placeholders — they are plain text for a later
 * data-binding layer.
 */
public class MustachePreservationTest {

    private final DocumentParser parser = new DocumentParser();

    @Test
    public void placeholderInTextSurvivesVerbatim() {
        Node root = parser.parse("<document><total align=\"right\">{{invoice.total}}</total></document>");
        assertEquals("{{invoice.total}}", root.children()
                                              .get(0)
                                              .text());
    }

    @Test
    public void placeholderInAttributeSurvivesVerbatim() {
        Node root = parser.parse("<document><image src=\"{{company.logo}}\"/></document>");
        assertEquals("{{company.logo}}", root.children()
                                             .get(0)
                                             .attributes()
                                             .get("src"));
    }

    @Test
    public void tripleStacheSurvivesVerbatim() {
        Node root = parser.parse("<document><text>{{{raw.html}}}</text></document>");
        assertEquals("{{{raw.html}}}", root.children()
                                           .get(0)
                                           .text());
    }

    @Test
    public void mustacheSectionsAreJustText() {
        Node root = parser.parse("<document><text>{{#items}} {{name}} {{/items}}</text></document>");
        assertEquals("{{#items}} {{name}} {{/items}}", root.children()
                                                           .get(0)
                                                           .text());
    }

    @Test
    public void placeholderAdjacentToEntityKeepsBoth() {
        Node root = parser.parse("<document><text>{{a}} &amp; {{b}}</text></document>");
        assertEquals("{{a}} & {{b}}", root.children()
                                          .get(0)
                                          .text());
    }
}
