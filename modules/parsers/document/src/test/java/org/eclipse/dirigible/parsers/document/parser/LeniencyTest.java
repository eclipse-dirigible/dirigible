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
 * The business-user leniencies where the DSL deliberately diverges from strict XML.
 */
public class LeniencyTest {

    private final DocumentParser parser = new DocumentParser();

    @Test
    public void namedEntitiesDecodeInText() {
        Node root = parser.parse("<document><text>&lt;a&gt; &amp; &quot;b&quot; &apos;c&apos;</text></document>");
        assertEquals("<a> & \"b\" 'c'", root.children()
                                            .get(0)
                                            .text());
    }

    @Test
    public void namedEntitiesDecodeInAttributeValues() {
        Node root = parser.parse("<document><field label=\"Terms &amp; Conditions\"/></document>");
        assertEquals("Terms & Conditions", root.children()
                                               .get(0)
                                               .attributes()
                                               .get("label"));
    }

    @Test
    public void rawAmpersandIsLiteralText() {
        Node root = parser.parse("<document><text>Fruit & Veg</text></document>");
        assertEquals("Fruit & Veg", root.children()
                                        .get(0)
                                        .text());
    }

    @Test
    public void malformedEntityWithoutSemicolonIsLiteral() {
        Node root = parser.parse("<document><text>Fish &amp Chips</text></document>");
        assertEquals("Fish &amp Chips", root.children()
                                            .get(0)
                                            .text());
    }

    @Test
    public void numericCharacterReferencesAreUnsupportedAndLiteral() {
        Node root = parser.parse("<document><text>&#65;</text></document>");
        assertEquals("&#65;", root.children()
                                  .get(0)
                                  .text());
    }

    @Test
    public void commentsAreSkippedEverywhere() {
        Node root = parser.parse("""
                <!-- before root -->
                <document>
                    <!-- contains <tags> and -- dashes -->
                    <text>Hi</text>
                </document>
                <!-- after root -->
                """);
        assertEquals(1, root.children()
                            .size());
        assertEquals("Hi", root.children()
                               .get(0)
                               .text());
    }

    @Test
    public void prologIsSkipped() {
        Node root = parser.parse("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<document/>");
        assertEquals("document", root.tag());
    }

    @Test
    public void commentBetweenPrologAndRootIsSkipped() {
        Node root = parser.parse("<?xml version=\"1.0\"?>\n<!-- x -->\n<document/>");
        assertEquals("document", root.tag());
    }

    @Test
    public void bomIsSkipped() {
        Node root = parser.parse("\uFEFF<document/>");
        assertEquals("document", root.tag());
    }
}
