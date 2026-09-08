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
 * Tests the Java literals a model value is written into a generated source as.
 */
class JavaLiteralsTest {

    @Test
    void leavesAnOrdinaryValueAlone() {
        assertEquals("DRAFT", JavaLiterals.escape("DRAFT"));
        assertEquals("6 mm", JavaLiterals.escape("6 mm"));
        assertEquals("Ц", JavaLiterals.escape("Ц"));
    }

    /**
     * The defect: the quote used to end the literal it was being written into, so one authored inch
     * mark failed the compile of the whole generated module (#7154).
     */
    @Test
    void escapesTheCharactersThatWouldEndTheLiteral() {
        assertEquals("6\\\"", JavaLiterals.escape("6\""));
        assertEquals("C:\\\\tmp", JavaLiterals.escape("C:\\tmp"));
        assertEquals("\\\\\\\"", JavaLiterals.escape("\\\""));
    }

    @Test
    void escapesTheControlCharactersThatWouldEndTheLine() {
        assertEquals("a\\nb", JavaLiterals.escape("a\nb"));
        assertEquals("a\\rb", JavaLiterals.escape("a\rb"));
        assertEquals("a\\tb", JavaLiterals.escape("a\tb"));
        assertEquals("a\\bb", JavaLiterals.escape("a\bb"));
        assertEquals("a\\fb", JavaLiterals.escape("a\fb"));
        assertEquals("a\\u0000b", JavaLiterals.escape("a\u0000b"));
        assertEquals("a\\u001bb", JavaLiterals.escape("a\u001bb"));
        assertEquals("a\\u007fb", JavaLiterals.escape("a\u007fb"));
    }

    /**
     * A numeric default is parsed from its authored text rather than inlined, so an author's "8.0" on
     * an integer column fails that one create instead of failing the whole generated build.
     */
    @Test
    void parsesANumericDefaultFromItsAuthoredText() {
        assertEquals("new java.math.BigDecimal(\"20.00\")", JavaLiterals.defaultValueExpression("java.math.BigDecimal", "20.00"));
        assertEquals("Double.valueOf(\"1.5\")", JavaLiterals.defaultValueExpression("Double", "1.5"));
        assertEquals("Float.valueOf(\"1.5\")", JavaLiterals.defaultValueExpression("Float", "1.5"));
        assertEquals("Long.valueOf(\"7\")", JavaLiterals.defaultValueExpression("Long", "7"));
        assertEquals("Integer.valueOf(\"7\")", JavaLiterals.defaultValueExpression("Integer", "7"));
        assertEquals("Short.valueOf(\"7\")", JavaLiterals.defaultValueExpression("Short", "7"));
        assertEquals("Integer.valueOf(\"8.0\")", JavaLiterals.defaultValueExpression("Integer", "8.0"));
    }

    /**
     * A numeric default is the one place a malformed value cannot even be escaped into something that
     * parses - so it must still compile, and fail at that one create.
     */
    @Test
    void escapesAMalformedNumericDefaultTooRatherThanBreakingTheCompile() {
        assertEquals("Integer.valueOf(\"7\\\"\")", JavaLiterals.defaultValueExpression("Integer", "7\""));
    }

    @Test
    void readsABooleanDefaultInEveryAuthoredShape() {
        assertEquals("Boolean.TRUE", JavaLiterals.defaultValueExpression("Boolean", "true"));
        assertEquals("Boolean.TRUE", JavaLiterals.defaultValueExpression("Boolean", "TRUE"));
        assertEquals("Boolean.TRUE", JavaLiterals.defaultValueExpression("Boolean", "1"));
        assertEquals("Boolean.FALSE", JavaLiterals.defaultValueExpression("Boolean", "false"));
        assertEquals("Boolean.FALSE", JavaLiterals.defaultValueExpression("Boolean", "0"));
    }

    /**
     * Both authoring shapes of a string default - bare, and SQL-quoted the way a working DB DEFAULT
     * needs - yield the string the column would hold.
     */
    @Test
    void readsAStringDefaultInEitherAuthoringShape() {
        assertEquals("\"DRAFT\"", JavaLiterals.defaultValueExpression("String", "DRAFT"));
        assertEquals("\"DRAFT\"", JavaLiterals.defaultValueExpression("String", "'DRAFT'"));
        assertEquals("\"'\"", JavaLiterals.defaultValueExpression("String", "'"));
        assertEquals("\"6\\\"\"", JavaLiterals.defaultValueExpression("String", "6\""));
        assertEquals("\"6\\\"\"", JavaLiterals.defaultValueExpression("String", "'6\"'"));
    }

    @Test
    void hasNoExpressionForATypeWhoseDefaultIsASqlExpression() {
        assertNull(JavaLiterals.defaultValueExpression("java.time.LocalDate", "CURRENT_DATE"));
        assertNull(JavaLiterals.defaultValueExpression("java.time.Instant", "now()"));
        assertNull(JavaLiterals.defaultValueExpression("byte[]", "x"));
    }

    @Test
    void hasNoExpressionWithoutADefault() {
        assertNull(JavaLiterals.defaultValueExpression("String", null));
        assertNull(JavaLiterals.defaultValueExpression("String", ""));
        assertNull(JavaLiterals.defaultValueExpression(null, "DRAFT"));
    }
}
