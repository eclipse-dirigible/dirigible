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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * The #7405 half: the model carries the READING of a compare literal, and the Java appears only
     * here. A number compares by value through BigDecimal, so a decimal column and a long one still
     * compare exactly.
     */
    @Test
    void rendersACompareLiteralFromItsNeutralReading() {
        assertEquals("new java.math.BigDecimal(\"0\")", JavaLiterals.compareLiteralExpression(Map.of("kind", "number", "text", "0")));
        assertEquals("new java.math.BigDecimal(\"100.50\")",
                JavaLiterals.compareLiteralExpression(Map.of("kind", "number", "text", "100.50")));
    }

    /**
     * A moment is resolved against the clock of the WRITE, and in the shape the generated column
     * carries - a comparison across LocalDate and Instant does not compile.
     */
    @Test
    void rendersAMomentInTheShapeItsColumnCarries() {
        assertEquals("java.time.LocalDate.now()", JavaLiterals.compareLiteralExpression(Map.of("kind", "moment", "shape", "date")));
        assertEquals("java.time.Instant.now()", JavaLiterals.compareLiteralExpression(Map.of("kind", "moment", "shape", "timestamp")));
        assertEquals("java.time.LocalDate.now().plus(java.time.Period.parse(\"P1D\"))",
                JavaLiterals.compareLiteralExpression(Map.of("kind", "moment", "shape", "date", "offset", "P1D", "forward", "true")));
        assertEquals("java.time.Instant.now().minus(java.time.Duration.parse(\"PT1H\"))", JavaLiterals.compareLiteralExpression(
                Map.of("kind", "moment", "shape", "timestamp", "offset", "PT1H", "forward", "false")));
    }

    @Test
    void rendersATemporalLiteralInTheShapeItsColumnCarries() {
        assertEquals("java.time.LocalDate.parse(\"2026-01-01\")",
                JavaLiterals.compareLiteralExpression(Map.of("kind", "temporal", "shape", "date", "text", "2026-01-01")));
        assertEquals("java.time.Instant.parse(\"2026-01-01T00:00:00Z\")",
                JavaLiterals.compareLiteralExpression(Map.of("kind", "temporal", "shape", "timestamp", "text", "2026-01-01T00:00:00Z")));
    }

    @Test
    void hasNoCompareExpressionForAReadingItDoesNotRecognise() {
        assertNull(JavaLiterals.compareLiteralExpression(null));
        assertNull(JavaLiterals.compareLiteralExpression(Map.of()));
        assertNull(JavaLiterals.compareLiteralExpression(Map.of("kind", "number")));
        assertNull(JavaLiterals.compareLiteralExpression(Map.of("kind", "colour", "text", "red")));
    }

    /**
     * The other #7405 half: a condition reaches the model as typed terms, each rendered against the
     * type the generator resolved for it - a string quoted, an integer bare, a long suffixed.
     */
    @Test
    void rendersAConditionFromItsNeutralTerms() {
        assertEquals("java.util.Objects.equals(entity.SentMethod, 1)",
                JavaLiterals.conditionExpression(List.of(term("entity", "SentMethod", true, "integer", "1", false))));
        assertEquals("!java.util.Objects.equals(entity.Kind, \"export\")",
                JavaLiterals.conditionExpression(List.of(term("entity", "Kind", false, "string", "export", false))));
        assertEquals("java.util.Objects.equals(entity.SentMethod, 1) && java.util.Objects.equals(entity.Kind, \"export\")",
                JavaLiterals.conditionExpression(List.of(term("entity", "SentMethod", true, "integer", "1", false),
                        term("entity", "Kind", true, "string", "export", false))));
    }

    /**
     * A term reading a loaded hop goes through that hop's null guard - the hop may not have resolved.
     */
    @Test
    void readsALoadedHopThroughItsNullGuard() {
        assertEquals("java.util.Objects.equals((hop0 == null ? null : hop0.Status), 7)",
                JavaLiterals.conditionExpression(List.of(term("hop0", "Status", true, "integer", "7", false))));
    }

    /**
     * A foreign key of an unknown width is compared by VALUE: Objects.equals(Long, Integer) never
     * holds, and the boxed form would switch the guard off while looking authored (#7237).
     */
    @Test
    void comparesAKeyOfUnknownWidthByValue() {
        assertEquals("(entity.Status != null && entity.Status.longValue() == 4L)",
                JavaLiterals.conditionExpression(List.of(term("entity", "Status", true, "long", "4", true))));
        assertEquals("!(entity.Status != null && entity.Status.longValue() == 4L)",
                JavaLiterals.conditionExpression(List.of(term("entity", "Status", false, "long", "4", true))));
    }

    /**
     * A term the generator did not type yields NO expression rather than a weaker guard - a condition
     * degraded to something that always holds is the failure the whole check exists to refuse.
     */
    @Test
    void hasNoConditionForATermItCannotType() {
        assertNull(JavaLiterals.conditionExpression(null));
        assertNull(JavaLiterals.conditionExpression(List.of()));
        assertNull(JavaLiterals.conditionExpression(List.of(term("entity", "Days", true, "integer", "many", false))));
        assertNull(JavaLiterals.conditionExpression(List.of(term("entity", "Filed", true, "date", "2026-01-01", false))));
    }

    /** The escape applies inside a rendered literal exactly as it does inside a default. */
    @Test
    void escapesTheValueItRendersIntoALiteral() {
        assertEquals("java.util.Objects.equals(entity.Size, \"6\\\"\")",
                JavaLiterals.conditionExpression(List.of(term("entity", "Size", true, "string", "6\"", false))));
    }

    private static Map<String, Object> term(String owner, String property, boolean equal, String type, String value, boolean numericKey) {
        Map<String, Object> term = new LinkedHashMap<>();
        term.put("owner", owner);
        term.put("property", property);
        term.put("equal", equal);
        term.put("type", type);
        term.put("value", value);
        term.put("numericKey", numericKey);
        return term;
    }
}
