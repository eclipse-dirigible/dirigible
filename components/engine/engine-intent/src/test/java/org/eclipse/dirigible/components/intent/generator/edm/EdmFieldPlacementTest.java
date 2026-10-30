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

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * {@code placement:} (dirigible #8369) reaches the model as {@code widgetPlacement: MAIN} for
 * {@code main}, and for {@code hidden} as the three flags the generator gives its own bookkeeping
 * columns. Without it the property map is unchanged.
 */
class EdmFieldPlacementTest {

    private static String shipment(String notePlacement, String tokenPlacement) {
        return """
                name: logistics
                entities:
                  - name: Shipment
                    fields:
                      - { name: id,          type: integer, primaryKey: true, generated: true }
                      - { name: reference,   type: string, length: 40 }
                      - { name: carrierNote, type: text, readOnly: true%s }
                      - { name: syncToken,   type: string, length: 64, readOnly: true%s }
                """.formatted(notePlacement, tokenPlacement);
    }

    @Test
    void mainEmitsTheMainPlacement() {
        Map<String, Object> note = property(entity(shipment(", placement: main", "")), "CarrierNote");
        assertEquals("MAIN", note.get("widgetPlacement"));
        assertEquals("true", note.get("isReadOnlyProperty"));
        assertNull(note.get("isHiddenProperty"));
    }

    @Test
    void hiddenEmitsTheBookkeepingFlags() {
        Map<String, Object> token = property(entity(shipment("", ", placement: hidden")), "SyncToken");
        assertEquals("true", token.get("isHiddenProperty"));
        assertEquals("true", token.get("isReadOnlyProperty"));
        assertEquals("false", token.get("widgetIsMajor"));
        assertNull(token.get("widgetPlacement"));
    }

    @Test
    void detailsAndAnAbsentPlacementLeaveThePropertyMapUnchanged() {
        Map<String, Object> plain = entity(shipment("", ""));
        assertEquals(plain, entity(shipment(", placement: details", ", placement: details")),
                "details is the default for a read-only field - it must change nothing");
        assertNull(property(plain, "CarrierNote").get("widgetPlacement"));
        assertNull(property(plain, "SyncToken").get("isHiddenProperty"));
        assertEquals("true", property(plain, "SyncToken").get("widgetIsMajor"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> entity(String yaml) {
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "logistics");
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) model.get("model")).get("entities");
        return entities.stream()
                       .filter(entity -> "Shipment".equals(entity.get("name")))
                       .findFirst()
                       .orElseThrow(() -> new AssertionError("no entity [Shipment]"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> property(Map<String, Object> entity, String name) {
        return ((List<Map<String, Object>>) entity.get("properties")).stream()
                                                                     .filter(property -> name.equals(property.get("name")))
                                                                     .findFirst()
                                                                     .orElseThrow(() -> new AssertionError("no property [" + name + "]"));
    }
}
