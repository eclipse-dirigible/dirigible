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

/**
 * Tests the Java a {@code .glue} expression reading renders as (issue #7425) - every kind, spelled
 * exactly as the generator used to write it into the glue, so the generated handlers stay
 * byte-identical.
 */
class JavaExpressionsTest {

    @Test
    void rendersTheReads() {
        assertEquals("source.Number", JavaExpressions.expression(reading("read", "owner", "source", "property", "Number")));
        assertEquals("(Customer == null ? null : Customer.Name)",
                JavaExpressions.expression(reading("hop", "owner", "Customer", "property", "Name")));
        assertEquals("version", JavaExpressions.expression(reading("local", "name", "version")));
        assertEquals("item.Quantity == null ? null : item.Quantity.negate()",
                JavaExpressions.expression(reading("negate", "owner", "item", "property", "Quantity")));
    }

    @Test
    void rendersTheLiterals() {
        assertEquals("\"from \\\"quote\\\"\"", JavaExpressions.expression(reading("string", "text", "from \"quote\"")));
        assertEquals("true", JavaExpressions.expression(reading("boolean", "text", "true")));
        assertEquals("null", JavaExpressions.expression(reading("null")));
        // A number in its bare spelling, or typed to the column it is written into.
        assertEquals("2", JavaExpressions.expression(reading("number", "text", "2")));
        assertEquals("-3.5", JavaExpressions.expression(reading("number", "text", "-3.5")));
        assertEquals("2", JavaExpressions.expression(reading("number", "text", "2", "type", "integer")));
        assertEquals("2L", JavaExpressions.expression(reading("number", "text", "2", "type", "long")));
        assertEquals("new java.math.BigDecimal(\"1.5\")", JavaExpressions.expression(reading("number", "text", "1.5", "type", "decimal")));
        assertEquals("new java.math.BigDecimal(\"1\")", JavaExpressions.expression(reading("number", "text", "1", "type", "decimal")));
        // A fraction into a whole-number column still compiles (the parser refuses the shape).
        assertEquals("new java.math.BigDecimal(\"1.5\").intValue()",
                JavaExpressions.expression(reading("number", "text", "1.5", "type", "integer")));
        assertEquals("new java.math.BigDecimal(\"1.5\").longValue()",
                JavaExpressions.expression(reading("number", "text", "1.5", "type", "long")));
    }

    @Test
    void rendersNowInTheTargetFieldsOwnShape() {
        assertEquals("java.time.LocalDate.now()", JavaExpressions.expression(reading("now", "shape", "date")));
        assertEquals("java.time.Instant.now()", JavaExpressions.expression(reading("now", "shape", "timestamp")));
        assertEquals("java.time.YearMonth.now().toString()", JavaExpressions.expression(reading("now", "shape", "month")));
        assertEquals(
                "String.format(\"%04d-W%02d\", java.time.LocalDate.now().get(java.time.temporal.IsoFields.WEEK_BASED_YEAR), "
                        + "java.time.LocalDate.now().get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR))",
                JavaExpressions.expression(reading("now", "shape", "week")));
    }

    @Test
    void rendersACalcCellRoundedAndNarrowed() {
        assertEquals("Calc.eval(\"Net - Discount\", source, 2)",
                JavaExpressions.expression(reading("calc", "text", "Net - Discount", "owner", "source", "scale", "2")));
        assertEquals("Calc.eval(\"Hours * 2\", source, 2).doubleValue()",
                JavaExpressions.expression(reading("calc", "text", "Hours * 2", "owner", "source", "scale", "2", "narrow", "double")));
        assertEquals("Calc.eval(\"Count\", source, 0).intValue()",
                JavaExpressions.expression(reading("calc", "text", "Count", "owner", "source", "scale", "0", "narrow", "int")));
        // The transition controller imports no Calc, so its guard spells the evaluator fully qualified.
        assertEquals("org.eclipse.dirigible.sdk.utils.Calc.eval(\"Paid\", source, 6).compareTo(new java.math.BigDecimal(\"0\")) == 0",
                JavaExpressions.qualifiedExpression(
                        reading("calcCompare", "owner", "source", "property", "Paid", "equal", true, "text", "0")));
        assertEquals("Calc.eval(\"Vat\", source, 6).compareTo(new java.math.BigDecimal(\"0\")) != 0",
                JavaExpressions.expression(reading("calcCompare", "owner", "source", "property", "Vat", "equal", false, "text", "0")));
    }

    @Test
    void rendersAConcatenation() {
        Map<String, Object> read = reading("read", "owner", "source", "property", "Number");
        assertEquals("\"Credit note \" + source.Number",
                JavaExpressions.expression(reading("concat", "parts", List.of(reading("string", "text", "Credit note "), read))));
        assertEquals("\"Services for \" + String.valueOf(source.Period)",
                JavaExpressions.expression(reading("concat", "parts", List.of(reading("string", "text", "Services for "),
                        reading("text", "of", reading("read", "owner", "source", "property", "Period"))))));
        // A lone value keeps its own type unless the concatenation is forced to a String.
        assertEquals("source.Number", JavaExpressions.expression(reading("concat", "parts", List.of(read))));
        assertEquals("\"\" + source.Number", JavaExpressions.expression(reading("concat", "parts", List.of(read), "forceString", true)));
        assertEquals("\"\"", JavaExpressions.expression(reading("concat", "parts", List.of())));
        // A nested concatenation flattens: the pattern's own terms, then the suffixes appended to it.
        assertEquals("\"\" + source.Number + \"_v\" + version + \".pdf\"",
                JavaExpressions.expression(
                        reading("concat", "parts", List.of(reading("concat", "parts", List.of(read), "forceString", true),
                                reading("string", "text", "_v"), reading("local", "name", "version"), reading("string", "text", ".pdf")))));
    }

    @Test
    void rendersAPromptedValuesConversion() {
        assertEquals("Integer.valueOf(new java.math.BigDecimal(String.valueOf(raw)).intValue())",
                JavaExpressions.expression(reading("convert", "type", "relation")));
        assertEquals("new java.math.BigDecimal(String.valueOf(raw))", JavaExpressions.expression(reading("convert", "type", "decimal")));
        assertEquals("java.time.LocalDate.parse(String.valueOf(raw))", JavaExpressions.expression(reading("convert", "type", "date")));
        assertEquals("String.valueOf(raw)", JavaExpressions.expression(reading("convert", "type", "string")));
    }

    @Test
    void rendersTheClassifierTernary() {
        Map<String, Object> reading = reading("ruleCase", "by", "Method", "owner", "source", "cases",
                List.of(reading(null, "value", "1", "column", "CashAccount"), reading(null, "value", "2", "column", "CardAccount")),
                "otherwise", "BankAccount");
        assertEquals("(Calc.eval(\"Method\", source, 6).compareTo(new java.math.BigDecimal(\"1\")) == 0 ? ruleRow.CashAccount"
                + " : Calc.eval(\"Method\", source, 6).compareTo(new java.math.BigDecimal(\"2\")) == 0 ? ruleRow.CardAccount"
                + " : ruleRow.BankAccount)", JavaExpressions.expression(reading));
        // No default column: an unmatched value falls to null, which the handler null-guards.
        assertEquals("(Calc.eval(\"Method\", source, 6).compareTo(new java.math.BigDecimal(\"1\")) == 0 ? ruleRow.CashAccount : null)",
                JavaExpressions.expression(reading("ruleCase", "by", "Method", "owner", "source", "cases",
                        List.of(reading(null, "value", "1", "column", "CashAccount")))));
    }

    @Test
    void rendersThePrintLanguageAndFileNameHelpers() {
        assertEquals("org.eclipse.dirigible.sdk.print.Print.defaultLanguage()", JavaExpressions.expression(reading("defaultLanguage")));
        assertEquals(
                "attachLanguageSource == null || attachLanguageSource.Code == null || attachLanguageSource.Code.isBlank()"
                        + " ? org.eclipse.dirigible.sdk.print.Print.defaultLanguage() : attachLanguageSource.Code.trim()",
                JavaExpressions.expression(reading("languageFrom", "owner", "attachLanguageSource", "property", "Code")));
        Map<String, Object> date = reading("read", "owner", "entity", "property", "Date");
        assertEquals("org.eclipse.dirigible.sdk.print.FileNames.part(entity.Date, \"yyyyMMdd\")",
                JavaExpressions.expression(reading("part", "of", date, "format", "yyyyMMdd")));
        assertEquals("org.eclipse.dirigible.sdk.print.FileNames.part(entity.Date)",
                JavaExpressions.expression(reading("part", "of", date)));
        assertEquals("org.eclipse.dirigible.sdk.print.FileNames.first(entity.Date, (Customer == null ? null : Customer.Name))",
                JavaExpressions.expression(
                        reading("first", "parts", List.of(date, reading("hop", "owner", "Customer", "property", "Name")))));
        assertEquals("(entity.Number == null || entity.Number.isBlank() ? \"SalesInvoice \" + entity.Id : entity.Number)",
                JavaExpressions.expression(
                        reading("numberOrId", "owner", "entity", "entity", "SalesInvoice", "key", "Id", "number", "Number")));
        assertEquals("\"Invoice \" + document.Id",
                JavaExpressions.expression(reading("numberOrId", "owner", "document", "entity", "Invoice", "key", "Id")));
    }

    @Test
    void rendersATimersDueMoment() {
        assertEquals("entity.ValidUntil == null ? java.util.Date.from(java.time.Instant.parse(\"9999-12-31T00:00:00Z\"))"
                + " : java.util.Date.from(entity.ValidUntil.plusDays(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())",
                JavaExpressions.expression(reading("due", "property", "ValidUntil", "shape", "date")));
        assertEquals(
                "entity.DueAt == null ? java.util.Date.from(java.time.Instant.parse(\"9999-12-31T00:00:00Z\"))"
                        + " : java.util.Date.from(entity.DueAt)",
                JavaExpressions.expression(reading("due", "property", "DueAt", "shape", "timestamp")));
    }

    /**
     * A reading this build did not write cannot be narrowed into a guess: it renders as null, which the
     * binder reads as "nothing to render".
     */
    @Test
    void anUnknownReadingRendersNothing() {
        assertNull(JavaExpressions.expression(reading("ternary", "text", "a ? b : c")));
        assertNull(JavaExpressions.expression(Map.of()));
        assertNull(JavaExpressions.expression("source.Number"));
        assertNull(JavaExpressions.expression(null));
        // ... and so does a concatenation one of whose parts does not.
        assertNull(JavaExpressions.expression(reading("concat", "parts", List.of(reading("string", "text", "a"), reading("what")))));
    }

    private static Map<String, Object> reading(String kind, Object... keysAndValues) {
        Map<String, Object> reading = new LinkedHashMap<>();
        if (kind != null) {
            reading.put("kind", kind);
        }
        for (int index = 0; index < keysAndValues.length; index += 2) {
            reading.put(String.valueOf(keysAndValues[index]), keysAndValues[index + 1]);
        }
        return reading;
    }
}
