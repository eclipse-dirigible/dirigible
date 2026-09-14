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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
     * A numeric default is parsed from its authored text rather than inlined as a numeric literal -
     * which keeps BigDecimal exact and needs no per-type suffix.
     */
    @Test
    void parsesANumericDefaultFromItsAuthoredText() {
        assertEquals("new java.math.BigDecimal(\"20.00\")", JavaLiterals.defaultValueExpression("java.math.BigDecimal", "20.00", "E.P"));
        assertEquals("Double.valueOf(\"1.5\")", JavaLiterals.defaultValueExpression("Double", "1.5", "E.P"));
        assertEquals("Float.valueOf(\"1.5\")", JavaLiterals.defaultValueExpression("Float", "1.5", "E.P"));
        assertEquals("Long.valueOf(\"7\")", JavaLiterals.defaultValueExpression("Long", "7", "E.P"));
        assertEquals("Integer.valueOf(\"7\")", JavaLiterals.defaultValueExpression("Integer", "7", "E.P"));
        assertEquals("Short.valueOf(\"7\")", JavaLiterals.defaultValueExpression("Short", "7", "E.P"));
        assertEquals("Integer.valueOf(\"-7\")", JavaLiterals.defaultValueExpression("Integer", "-7", "E.P"));
    }

    /**
     * The defect: the SQL-quoted shape was read in the String arm only, so an integer column's
     * {@code '20'} seeded 20 in the item dialog and emitted {@code Integer.valueOf("'20'")} in the
     * repository - a NumberFormatException on every create that relied on the default (#7293).
     */
    @Test
    void readsANumericDefaultInEitherAuthoringShape() {
        assertEquals("Integer.valueOf(\"20\")", JavaLiterals.defaultValueExpression("Integer", "'20'", "E.P"));
        assertEquals("Long.valueOf(\"20\")", JavaLiterals.defaultValueExpression("Long", "'20'", "E.P"));
        assertEquals("new java.math.BigDecimal(\"20.00\")", JavaLiterals.defaultValueExpression("java.math.BigDecimal", "'20.00'", "E.P"));
    }

    /**
     * A text the property's own factory cannot read is refused while the author is generating, naming
     * the property - it used to compile into an expression that threw on every create of that entity.
     */
    @Test
    void refusesANumericDefaultThatIsNotAValueOfItsType() {
        IllegalArgumentException refusal =
                assertThrows(IllegalArgumentException.class, () -> JavaLiterals.defaultValueExpression("Integer", "8.0", "Order.Lines"));
        assertTrue(refusal.getMessage()
                          .contains("Order.Lines"),
                "the refusal must name the property, got: " + refusal.getMessage());
        assertThrows(IllegalArgumentException.class, () -> JavaLiterals.defaultValueExpression("Integer", "7\"", "E.P"));
        assertThrows(IllegalArgumentException.class, () -> JavaLiterals.defaultValueExpression("Integer", "N/A", "E.P"));
        assertThrows(IllegalArgumentException.class, () -> JavaLiterals.defaultValueExpression("java.math.BigDecimal", "1 or 2", "E.P"));
        assertThrows(IllegalArgumentException.class, () -> JavaLiterals.defaultValueExpression("Long", "nextval('s')", "E.P"));
    }

    @Test
    void readsABooleanDefaultInEveryAuthoredShape() {
        assertEquals("Boolean.TRUE", JavaLiterals.defaultValueExpression("Boolean", "true", "E.P"));
        assertEquals("Boolean.TRUE", JavaLiterals.defaultValueExpression("Boolean", "TRUE", "E.P"));
        assertEquals("Boolean.TRUE", JavaLiterals.defaultValueExpression("Boolean", "1", "E.P"));
        assertEquals("Boolean.FALSE", JavaLiterals.defaultValueExpression("Boolean", "false", "E.P"));
        assertEquals("Boolean.FALSE", JavaLiterals.defaultValueExpression("Boolean", "0", "E.P"));
        assertEquals("Boolean.TRUE", JavaLiterals.defaultValueExpression("Boolean", "'true'", "E.P"));
        assertEquals("Boolean.FALSE", JavaLiterals.defaultValueExpression("Boolean", "'false'", "E.P"));
    }

    /**
     * Both authoring shapes of a string default - bare, and SQL-quoted the way a working DB DEFAULT
     * needs - yield the string the column would hold.
     */
    @Test
    void readsAStringDefaultInEitherAuthoringShape() {
        assertEquals("\"DRAFT\"", JavaLiterals.defaultValueExpression("String", "DRAFT", "E.P"));
        assertEquals("\"DRAFT\"", JavaLiterals.defaultValueExpression("String", "'DRAFT'", "E.P"));
        assertEquals("\"'\"", JavaLiterals.defaultValueExpression("String", "'", "E.P"));
        assertEquals("\"6\\\"\"", JavaLiterals.defaultValueExpression("String", "6\"", "E.P"));
        assertEquals("\"6\\\"\"", JavaLiterals.defaultValueExpression("String", "'6\"'", "E.P"));
    }

    @Test
    void hasNoExpressionForATypeWhoseDefaultIsASqlExpression() {
        assertNull(JavaLiterals.defaultValueExpression("java.time.LocalDate", "CURRENT_DATE", "E.P"));
        assertNull(JavaLiterals.defaultValueExpression("java.time.Instant", "now()", "E.P"));
        assertNull(JavaLiterals.defaultValueExpression("byte[]", "x", "E.P"));
    }

    @Test
    void hasNoExpressionWithoutADefault() {
        assertNull(JavaLiterals.defaultValueExpression("String", null, "E.P"));
        assertNull(JavaLiterals.defaultValueExpression("String", "", "E.P"));
        assertNull(JavaLiterals.defaultValueExpression(null, "DRAFT", "E.P"));
    }
}
