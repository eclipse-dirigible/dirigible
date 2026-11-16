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
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * {@code patternMessage:} (dirigible #7749) reaches the model as the trimmed scalar
 * {@code widgetPatternMessage} beside {@code widgetPattern}. Without it the property map is
 * unchanged.
 */
class EdmFieldPatternMessageTest {

    private static String customers(String extra) {
        return """
                name: customers
                entities:
                  - name: Customer
                    fields:
                      - { name: id,   type: integer, primaryKey: true, generated: true }
                      - { name: code, type: string, length: 13, pattern: '^[0-9]{9}$'%s }
                """.formatted(extra);
    }

    @Test
    void aPatternMessageIsEmittedTrimmedBesideThePattern() {
        Map<String, Object> code = code(customers(", patternMessage: '  EIK: 9 digits  '"));
        assertEquals("^[0-9]{9}$", code.get("widgetPattern"));
        assertEquals("EIK: 9 digits", code.get("widgetPatternMessage"));
    }

    @Test
    void noPatternMessageLeavesThePropertyMapUnchanged() {
        Map<String, Object> with = code(customers(", patternMessage: EIK"));
        Map<String, Object> without = code(customers(""));
        assertFalse(without.containsKey("widgetPatternMessage"));
        with.remove("widgetPatternMessage");
        assertEquals(without, with, "the key adds widgetPatternMessage and nothing else");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> code(String yaml) {
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "customers");
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) model.get("model")).get("entities");
        Map<String, Object> customer = entities.stream()
                                               .filter(entity -> "Customer".equals(entity.get("name")))
                                               .findFirst()
                                               .orElseThrow();
        return ((List<Map<String, Object>>) customer.get("properties")).stream()
                                                                       .filter(property -> "Code".equals(property.get("name")))
                                                                       .findFirst()
                                                                       .orElseThrow();
    }
}
