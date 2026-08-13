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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the compilation of a mapping model into the Java expressions its mapper renders. This is
 * the one template whose preparation derives code, so these are the assertions that the JavaScript
 * generator's semantics survived the port: source precedence, the typed criteria comparisons, and
 * the loud failures.
 */
class MappingCompilerTest {

    /** Wraps target columns in the mapping file shape the compiler reads. */
    private static String mappingOf(String columnsJson) {
        return """
                { "mapping": { "structures": [
                    { "name": "SOURCE_TABLE", "type": "SOURCE", "columns": [] },
                    { "name": "TARGET_TABLE", "type": "TARGET", "columns": [%s] }
                ] } }
                """.formatted(columnsJson);
    }

    private static List<Map<String, Object>> columnsOf(Map<String, Object> compiled) {
        return ModelValues.asMaps(compiled.get("mappingColumns"));
    }

    @Test
    void compilesEachSourceKindWithModuleFirstAndDirectLast() {
        Map<String, Object> compiled = MappingCompiler.compile(mappingOf("""
                { "name": "PRICE",   "formula": "AMOUNT * 2", "scale": 4 },
                { "name": "LABEL",   "constant": "fixed" },
                { "name": "CODE",    "direct": "SOURCE_CODE" },
                { "name": "MAPPED",  "module": "custom.PriceMapper", "direct": "IGNORED" },
                { "name": "NOTHING" }
                """));

        List<Map<String, Object>> columns = columnsOf(compiled);
        assertEquals("TARGET_TABLE", compiled.get("mappingTarget"));
        assertEquals(5, columns.size());
        assertEquals("org.eclipse.dirigible.sdk.utils.Calc.eval(\"AMOUNT * 2\", source, 4)", columns.get(0)
                                                                                                    .get("expression"));
        assertEquals("\"fixed\"", columns.get(1)
                                         .get("expression"));
        assertEquals("source.get(\"SOURCE_CODE\")", columns.get(2)
                                                           .get("expression"));
        // A module wins over every other source it is declared next to.
        assertEquals("org.eclipse.dirigible.sdk.component.Beans.get(custom.PriceMapper.class).map(source, target, \"MAPPED\")",
                columns.get(3)
                       .get("expression"));
        assertEquals("null", columns.get(4)
                                    .get("expression"));
    }

    @Test
    void defaultsAFormulaScaleToTwoDecimals() {
        Map<String, Object> compiled = MappingCompiler.compile(mappingOf("""
                { "name": "PRICE", "formula": "AMOUNT" }
                """));

        assertEquals("org.eclipse.dirigible.sdk.utils.Calc.eval(\"AMOUNT\", source, 2)", columnsOf(compiled).get(0)
                                                                                                            .get("expression"));
    }

    @Test
    void compilesANumericCriteriaThroughTheNumericReader() {
        Map<String, Object> compiled = MappingCompiler.compile(mappingOf("""
                { "name": "AMOUNT", "direct": "AMOUNT", "criteria": "> 100" }
                """));

        assertEquals("number(target.get(\"AMOUNT\")) > 100d", columnsOf(compiled).get(0)
                                                                                 .get("criteria"));
        // Only a numeric criteria needs the reader, and the template emits it on this flag alone.
        assertTrue((Boolean) compiled.get("usesNumber"));
    }

    @Test
    void compilesAStringCriteriaAsAnEqualityAndANullCriteriaAsAComparison() {
        Map<String, Object> compiled = MappingCompiler.compile(mappingOf("""
                { "name": "NAME",  "direct": "NAME",  "criteria": "== 'skip'" },
                { "name": "OTHER", "direct": "OTHER", "criteria": "!== \\"keep\\"" },
                { "name": "EMPTY", "direct": "EMPTY", "criteria": "!= null" }
                """));

        List<Map<String, Object>> columns = columnsOf(compiled);
        assertEquals("java.util.Objects.equals(target.get(\"NAME\"), \"skip\")", columns.get(0)
                                                                                        .get("criteria"));
        assertEquals("!java.util.Objects.equals(target.get(\"OTHER\"), \"keep\")", columns.get(1)
                                                                                          .get("criteria"));
        assertEquals("target.get(\"EMPTY\") != null", columns.get(2)
                                                             .get("criteria"));
        assertFalse((Boolean) compiled.get("usesNumber"));
    }

    @Test
    void escapesJavaStringLiterals() {
        Map<String, Object> compiled = MappingCompiler.compile(mappingOf("""
                { "name": "QUOTED", "constant": "a \\"quote\\" and a \\\\ and a \\n newline" }
                """));

        assertEquals("\"a \\\"quote\\\" and a \\\\ and a \\n newline\"", columnsOf(compiled).get(0)
                                                                                            .get("expression"));
    }

    @Test
    void refusesAMappingWithoutATargetAndAnUnparseableCriteria() {
        assertThrows(IllegalArgumentException.class,
                () -> MappingCompiler.compile("{ \"mapping\": { \"structures\": [ { \"name\": \"S\", \"type\": \"SOURCE\" } ] } }"),
                "a mapping with no TARGET has nothing to map to and must fail the generation");
        assertThrows(IllegalArgumentException.class, () -> MappingCompiler.compile(mappingOf("""
                { "name": "AMOUNT", "direct": "AMOUNT", "criteria": "totally bogus" }
                """)), "a criteria that is not a comparison must fail the generation, not emit misbehaving code");
        assertThrows(IllegalArgumentException.class, () -> MappingCompiler.compile(mappingOf("""
                { "name": "AMOUNT", "direct": "AMOUNT", "criteria": "> unquoted" }
                """)), "a criteria operand that is neither a number, a quoted string nor null must fail the generation");
    }

    @Test
    void buildsAValidClassNameFromAnyFileName() {
        assertEquals("PriceMapping", MappingCompiler.className("price-mapping"));
        assertEquals("PriceMapping", MappingCompiler.className("price mapping"));
        assertEquals("_2024Prices", MappingCompiler.className("2024-prices"));
        assertEquals("Mapper", MappingCompiler.className("---"));
        assertEquals("Mapper", MappingCompiler.className(null));
    }

}
