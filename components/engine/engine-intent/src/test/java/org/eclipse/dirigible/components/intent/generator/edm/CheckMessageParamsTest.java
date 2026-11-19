/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.edm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.junit.jupiter.api.Test;

/**
 * A check message names the record's values (issue #8665): {@code {taxRateCap}} and
 * {@code {Category.code}} resolve against the record, the generator's own placeholders stay its
 * own, and an unknown path is a parse error.
 */
class CheckMessageParamsTest {

    private static String model(String message) {
        return """
                name: assets
                entities:
                  - name: Category
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: code, type: string, length: 20 }
                  - name: Asset
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: taxRate, type: decimal }
                      - { name: taxRateCap, type: decimal }
                    relations:
                      - { name: Category, kind: manyToOne, to: Category }
                    checks:
                      - { kind: compare, field: taxRate, op: le, than: taxRateCap, message: "%s" }
                """.formatted(message);
    }

    @Test
    @SuppressWarnings("unchecked")
    void anOwnAndAOneHopValueAreCarriedWithTheCheck() {
        List<Map<String, Object>> params = (List<Map<String, Object>>) check(
                "The tax depreciation rate exceeds the statutory maximum of {taxRateCap}% for category {Category.code}").get(
                        "messageParams");
        assertEquals(2, params.size());
        assertEquals("taxRateCap", params.get(0)
                                         .get("name"));
        assertEquals("TaxRateCap", params.get(0)
                                         .get("property"));
        assertNull(params.get(0)
                         .get("hop"));
        assertEquals("Category.code", params.get(1)
                                            .get("name"));
        assertEquals("Code", params.get(1)
                                   .get("property"));
        Map<String, Object> hop = (Map<String, Object>) params.get(1)
                                                              .get("hop");
        assertEquals("entity.Category", hop.get("sourceExpression"));
        assertEquals("Category", hop.get("entity"));
    }

    /** A message naming none of the record's values generates exactly what it did. */
    @Test
    void aPlainMessageAndTheGeneratorsOwnPlaceholdersCarryNoParams() {
        assertNull(check("The tax rate exceeds the cap").get("messageParams"));
        assertNull(check("{count} lines").get("messageParams"));
    }

    @Test
    void anUnknownPathIsAParseError() {
        assertIssue("over {taxRateMax}", "message names {taxRateMax}");
        assertIssue("for {Category.name}", "message names {Category.name}");
        assertIssue("for {Owner.code}", "message names {Owner.code}");
    }

    @Test
    void moreThanOneHopIsRefused() {
        assertIssue("for {Category.Parent.code}", "walks more than one hop");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> check(String message) {
        Map<String, Object> modelJson = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(model(message)), "assets");
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) modelJson.get("model")).get("entities");
        Map<String, Object> asset = entities.stream()
                                            .filter(e -> "Asset".equals(e.get("name")))
                                            .findFirst()
                                            .orElseThrow();
        return ((List<Map<String, Object>>) asset.get("checks")).get(0);
    }

    private static void assertIssue(String message, String expected) {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(model(message)));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains(expected)),
                "expected an issue containing [" + expected + "], got: " + ex.getIssues());
    }
}
