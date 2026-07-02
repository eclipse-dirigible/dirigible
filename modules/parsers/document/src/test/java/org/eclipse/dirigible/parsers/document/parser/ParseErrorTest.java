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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Every syntax error must carry the exact 1-based line and column.
 */
public class ParseErrorTest {

    private final DocumentParser parser = new DocumentParser();

    private ParseException expectError(String source) {
        try {
            parser.parse(source);
        } catch (ParseException e) {
            return e;
        }
        fail("Expected ParseException");
        return null; // unreachable
    }

    @Test
    public void unclosedElementPointsAtTheOpeningTag() {
        ParseException e = expectError("<document>\n  <row>\n    <text>Hi</text>\n");
        assertTrue(e.getMessage()
                    .contains("Unclosed element <row>"));
        assertEquals(2, e.getLine());
        assertEquals(3, e.getColumn());
    }

    @Test
    public void mismatchedClosingTag() {
        ParseException e = expectError("<document>\n  <row>x</column>\n</document>");
        assertTrue(e.getMessage()
                    .contains("expected </row>, found </column>"));
        assertEquals(2, e.getLine());
        assertEquals(9, e.getColumn());
    }

    @Test
    public void missingEqualsAfterAttributeName() {
        ParseException e = expectError("<document><text align\"right\">x</text></document>");
        assertTrue(e.getMessage()
                    .contains("Expected '=' after attribute name 'align'"));
        assertEquals(1, e.getLine());
    }

    @Test
    public void unquotedAttributeValue() {
        ParseException e = expectError("<document><text align=right>x</text></document>");
        assertTrue(e.getMessage()
                    .contains("Attribute value must be quoted"));
        assertEquals(23, e.getColumn());
    }

    @Test
    public void unterminatedAttributeValuePointsAtTheOpeningQuote() {
        ParseException e = expectError("<document><text align=\"right>x</text></document>");
        assertTrue(e.getMessage()
                    .contains("Unterminated attribute value"));
        assertEquals(23, e.getColumn());
    }

    @Test
    public void unterminatedCommentPointsAtItsStart() {
        ParseException e = expectError("<document>\n<!-- oops\n</document>");
        assertTrue(e.getMessage()
                    .contains("Unterminated comment"));
        assertEquals(2, e.getLine());
        assertEquals(1, e.getColumn());
    }

    @Test
    public void duplicateAttribute() {
        ParseException e = expectError("<document><text align=\"left\" align=\"right\">x</text></document>");
        assertTrue(e.getMessage()
                    .contains("Duplicate attribute 'align'"));
        assertEquals(30, e.getColumn());
    }

    @Test
    public void contentAfterRoot() {
        ParseException e = expectError("<document/>\n<text>x</text>");
        assertTrue(e.getMessage()
                    .contains("Unexpected content after the root element"));
        assertEquals(2, e.getLine());
    }

    @Test
    public void textBeforeRoot() {
        ParseException e = expectError("hello <document/>");
        assertTrue(e.getMessage()
                    .contains("Expected '<' to start the root element"));
    }

    @Test
    public void unknownTag() {
        ParseException e = expectError("<document>\n  <colum width=\"45%\">x</colum>\n</document>");
        assertTrue(e.getMessage()
                    .contains("Unknown tag <colum>"));
        assertEquals(2, e.getLine());
        assertEquals(3, e.getColumn());
    }

    @Test
    public void invalidTagName() {
        ParseException e = expectError("<document><1text>x</1text></document>");
        assertTrue(e.getMessage()
                    .contains("Invalid tag name"));
    }

    @Test
    public void unterminatedProlog() {
        ParseException e = expectError("<?xml version=\"1.0\"");
        assertTrue(e.getMessage()
                    .contains("Unterminated XML prolog"));
        assertEquals(1, e.getLine());
        assertEquals(1, e.getColumn());
    }

    @Test
    public void emptySourceExpectsRootElement() {
        ParseException e = expectError("   \n  ");
        assertTrue(e.getMessage()
                    .contains("Expected '<' to start the root element"));
    }
}
