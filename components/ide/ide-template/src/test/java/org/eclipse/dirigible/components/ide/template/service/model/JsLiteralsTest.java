/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.ide.template.service.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests the JavaScript literals a model value is written into a generated source as.
 */
class JsLiteralsTest {

    @Test
    void leavesAnOrdinaryValueAlone() {
        assertEquals("DRAFT", JsLiterals.escape("DRAFT"));
        assertEquals("6\" wide", JsLiterals.escape("6\" wide"));
        assertEquals("Ц", JsLiterals.escape("Ц"));
    }

    /**
     * The defect: the apostrophe used to end the literal it was being written into, so one authored
     * possessive made the whole register a syntax error and the page failed to load (#7207).
     */
    @Test
    void escapesTheCharactersThatWouldEndTheLiteral() {
        assertEquals("Owner\\'s copy", JsLiterals.escape("Owner's copy"));
        assertEquals("C:\\\\tmp", JsLiterals.escape("C:\\tmp"));
        assertEquals("\\\\\\'", JsLiterals.escape("\\'"));
    }

    @Test
    void escapesTheControlCharactersThatWouldEndTheLine() {
        assertEquals("a\\nb", JsLiterals.escape("a\nb"));
        assertEquals("a\\rb", JsLiterals.escape("a\rb"));
        assertEquals("a\\tb", JsLiterals.escape("a\tb"));
        assertEquals("a\\bb", JsLiterals.escape("a\bb"));
        assertEquals("a\\fb", JsLiterals.escape("a\fb"));
        assertEquals("a\\u0000b", JsLiterals.escape("a\u0000b"));
        assertEquals("a\\u001bb", JsLiterals.escape("a\u001bb"));
        assertEquals("a\\u007fb", JsLiterals.escape("a\u007fb"));
    }

    /**
     * The seed is emitted in the shape the draft holds, so the dialog's value has the type the widget
     * binds to.
     */
    @Test
    void seedsEachWidgetInTheShapeTheDraftHolds() {
        assertEquals("true", JsLiterals.defaultValueExpression("CHECKBOX", false, "true"));
        assertEquals("true", JsLiterals.defaultValueExpression("CHECKBOX", false, "TRUE"));
        assertEquals("true", JsLiterals.defaultValueExpression("CHECKBOX", false, "1"));
        assertEquals("false", JsLiterals.defaultValueExpression("CHECKBOX", false, "false"));
        assertEquals("true", JsLiterals.defaultValueExpression("CHECKBOX", false, "'true'"));
        assertEquals("false", JsLiterals.defaultValueExpression("CHECKBOX", false, "'false'"));
        assertEquals("20.00", JsLiterals.defaultValueExpression("TEXTBOX", true, "20.00"));
        assertEquals("'DRAFT'", JsLiterals.defaultValueExpression("TEXTBOX", false, "DRAFT"));
        // A dropdown's FK stays a string even though it reads as a number, so it matches an option's
        // data-value.
        assertEquals("'7'", JsLiterals.defaultValueExpression("DROPDOWN", true, "7"));
    }

    /**
     * Both authoring shapes of a string default - bare, and SQL-quoted the way a working DB DEFAULT
     * needs - seed the string the column would hold, so the dialog offers what the repository applies.
     */
    @Test
    void readsADefaultInEitherAuthoringShape() {
        assertEquals("'DRAFT'", JsLiterals.defaultValueExpression("TEXTBOX", false, "'DRAFT'"));
        assertEquals("20", JsLiterals.defaultValueExpression("TEXTBOX", true, "'20'"));
    }

    /**
     * A numeric column's seed is written unquoted, so a default that is not a number has to fall back
     * to a string rather than being emitted as a bare identifier the register would choke on.
     */
    @Test
    void fallsBackToAStringWhereANumericDefaultIsNotANumber() {
        assertEquals("'N/A'", JsLiterals.defaultValueExpression("TEXTBOX", true, "N/A"));
        assertEquals("'1 or 2'", JsLiterals.defaultValueExpression("TEXTBOX", true, "1 or 2"));
        assertEquals("-1.5", JsLiterals.defaultValueExpression("TEXTBOX", true, "-1.5"));
        assertEquals("1e3", JsLiterals.defaultValueExpression("TEXTBOX", true, "1e3"));
    }

    @Test
    void escapesAValueThatWouldEndTheLiteral() {
        assertEquals("'Owner\\'s copy'", JsLiterals.defaultValueExpression("TEXTBOX", false, "Owner's copy"));
        assertEquals("'Owner\\'s copy'", JsLiterals.defaultValueExpression("DROPDOWN", false, "Owner's copy"));
        assertEquals("'C:\\\\tmp'", JsLiterals.defaultValueExpression("TEXTBOX", false, "C:\\tmp"));
        assertEquals("'a\\nb'", JsLiterals.defaultValueExpression("TEXTBOX", false, "a\nb"));
    }

    @Test
    void hasNoExpressionWithoutADefault() {
        assertNull(JsLiterals.defaultValueExpression("TEXTBOX", false, null));
        assertNull(JsLiterals.defaultValueExpression("TEXTBOX", false, ""));
        assertNull(JsLiterals.defaultValueExpression("CHECKBOX", false, null));
    }
}
