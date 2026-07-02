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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.eclipse.dirigible.parsers.document.DocumentNode;
import org.eclipse.dirigible.parsers.document.FieldNode;
import org.eclipse.dirigible.parsers.document.HeaderNode;
import org.eclipse.dirigible.parsers.document.Node;
import org.eclipse.dirigible.parsers.document.SectionNode;
import org.eclipse.dirigible.parsers.document.TextNode;
import org.junit.Test;

public class DocumentParserTest {

    private final DocumentParser parser = new DocumentParser();

    @Test
    public void parsesNestedStructureInDocumentOrder() {
        Node root = parser.parse("""
                <document>
                    <header>
                        <text align="right" style="title">Sales Invoice</text>
                    </header>
                    <section>
                        <field label="Invoice No">{{invoice.number}}</field>
                        <field label="Date">{{invoice.date}}</field>
                    </section>
                </document>
                """);
        assertTrue(root instanceof DocumentNode);
        assertEquals(2, root.children()
                            .size());
        assertTrue(root.children()
                       .get(0) instanceof HeaderNode);
        assertTrue(root.children()
                       .get(1) instanceof SectionNode);
        Node section = root.children()
                           .get(1);
        assertEquals(2, section.children()
                               .size());
        FieldNode first = (FieldNode) section.children()
                                             .get(0);
        assertEquals("Invoice No", first.attributes()
                                        .get("label"));
        assertEquals("{{invoice.number}}", first.text());
    }

    @Test
    public void parsesAttributesWithSingleAndDoubleQuotes() {
        Node root = parser.parse("<document><text align='right' style=\"title caption\">Hi</text></document>");
        Node text = root.children()
                        .get(0);
        assertEquals("right", text.attributes()
                                  .get("align"));
        assertEquals("title caption", text.attributes()
                                          .get("style"));
    }

    @Test
    public void selfClosingTagHasNoChildrenAndEmptyText() {
        Node root = parser.parse("<document><line/><space height=\"20\" /></document>");
        assertEquals(2, root.children()
                            .size());
        Node space = root.children()
                         .get(1);
        assertEquals("space", space.tag());
        assertEquals(0, space.children()
                             .size());
        assertEquals("", space.text());
        assertEquals("20", space.attributes()
                                .get("height"));
    }

    @Test
    public void textIsTrimmedAndWhitespaceCollapsed() {
        Node root = parser.parse("<document><text>  Hello \n\t  World  </text></document>");
        assertEquals("Hello World", root.children()
                                        .get(0)
                                        .text());
    }

    @Test
    public void whitespaceOnlyContentIsEmptyText() {
        Node root = parser.parse("<document><section>\n   \n</section></document>");
        assertEquals("", root.children()
                             .get(0)
                             .text());
    }

    @Test
    public void mixedContentConcatenatesTextAroundChildren() {
        Node root = parser.parse("<document><text>Total: <line/> due now</text></document>");
        TextNode text = (TextNode) root.children()
                                       .get(0);
        assertEquals("Total: due now", text.text());
        assertEquals(1, text.children()
                            .size());
        assertEquals("line", text.children()
                                 .get(0)
                                 .tag());
    }

    @Test
    public void attributeValuesKeepInnerWhitespaceVerbatim() {
        Node root = parser.parse("<document><field label=\"Invoice   No\"/></document>");
        assertEquals("Invoice   No", root.children()
                                         .get(0)
                                         .attributes()
                                         .get("label"));
    }

    @Test
    public void nodePositionsPointAtTheOpeningTag() {
        Node root = parser.parse("<document>\n  <header>\n    <text>Hi</text>\n  </header>\n</document>");
        assertEquals(1, root.position()
                            .line());
        assertEquals(1, root.position()
                            .column());
        Node header = root.children()
                          .get(0);
        assertEquals(2, header.position()
                              .line());
        assertEquals(3, header.position()
                              .column());
        Node text = header.children()
                          .get(0);
        assertEquals(3, text.position()
                            .line());
        assertEquals(5, text.position()
                            .column());
    }

    @Test
    public void attributePositionIsTracked() {
        Node root = parser.parse("<document>\n  <text align=\"right\">Hi</text>\n</document>");
        assertEquals(2, root.children()
                            .get(0)
                            .attributes()
                            .positionOf("align")
                            .line());
        assertEquals(9, root.children()
                            .get(0)
                            .attributes()
                            .positionOf("align")
                            .column());
    }

    @Test
    public void parseDocumentReturnsTypedRoot() {
        DocumentNode document = parser.parseDocument("<document id=\"x\"/>");
        assertEquals("x", document.attributes()
                                  .get("id"));
    }

    @Test
    public void parseDocumentRejectsOtherRoots() {
        try {
            parser.parseDocument("<page/>");
        } catch (ParseException e) {
            assertTrue(e.getMessage()
                        .contains("Root element must be <document>"));
            return;
        }
        throw new AssertionError("Expected ParseException");
    }

    @Test
    public void parserIsReusable() {
        Node first = parser.parse("<document/>");
        Node second = parser.parse("<document/>");
        assertEquals(first.tag(), second.tag());
        assertSame(first.getClass(), second.getClass());
    }
}
