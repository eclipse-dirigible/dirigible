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
 * {@code systemProperties: { processId: { visibleTo: [...] } }} (dirigible #8692) reaches the model
 * as a display-only {@code roleDisplay} on the generated {@code ProcessId} / {@code ProcessIds} -
 * never as the {@code roleRead} that would strip the value from the responses the page correlates
 * its tasks on. Without it the property maps are unchanged.
 */
class EdmProcessIdDisplayTest {

    private static String intent(String systemProperties) {
        return """
                name: stock
                %s
                permissions:
                  - { role: Support, description: Support staff, can: [Receipt:read] }
                  - { role: Auditor, description: Auditors,      can: [Receipt:read] }
                entities:
                  - name: Receipt
                    fields:
                      - { name: id,   type: integer, primaryKey: true, generated: true }
                      - { name: note, type: string }
                processes:
                  - name: PostReceipt
                    trigger: { onCreate: Receipt }
                    steps:
                      - { name: done, kind: end }
                """.formatted(systemProperties);
    }

    @Test
    void visibleToEmitsADisplayOnlyRoleOnBothProcessProperties() {
        Map<String, Object> receipt = entity(intent("systemProperties: { processId: { visibleTo: [Support, Auditor] } }"));
        for (String name : List.of("ProcessId", "ProcessIds")) {
            Map<String, Object> property = property(receipt, name);
            assertEquals("Support,Auditor", property.get("roleDisplay"), name);
            assertNull(property.get("roleRead"), name + " must stay in every response - the page correlates its tasks on it");
            assertNull(property.get("roleWrite"), name);
        }
        assertNull(property(receipt, "Note").get("roleDisplay"), "an authored field is not touched");
    }

    @Test
    void withoutTheSettingThePropertiesAreUnchanged() {
        Map<String, Object> receipt = entity(intent(""));
        assertNull(property(receipt, "ProcessId").get("roleDisplay"));
        assertNull(property(receipt, "ProcessIds").get("roleDisplay"));
    }

    @Test
    void anUndeclaredOrAnEmptyRoleListIsRefused() {
        IntentValidationException undeclared = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(intent("systemProperties: { processId: { visibleTo: [Storekeeper] } }")));
        assertTrue(undeclared.getMessage()
                             .contains("systemProperties.processId visibleTo names role [Storekeeper]"),
                undeclared.getMessage());
        IntentValidationException empty =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(intent("systemProperties: { processId: {} }")));
        assertTrue(empty.getMessage()
                        .contains("systemProperties.processId visibleTo lists no role"),
                empty.getMessage());
        IntentValidationException unknown = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(intent("systemProperties: { uuid: { visibleTo: [Support] } }")));
        assertTrue(unknown.getMessage()
                          .contains("uuid"),
                unknown.getMessage());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> entity(String yaml) {
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "stock");
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) model.get("model")).get("entities");
        return entities.stream()
                       .filter(entity -> "Receipt".equals(entity.get("name")))
                       .findFirst()
                       .orElseThrow(() -> new AssertionError("no entity [Receipt]"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> property(Map<String, Object> entity, String name) {
        return ((List<Map<String, Object>>) entity.get("properties")).stream()
                                                                     .filter(property -> name.equals(property.get("name")))
                                                                     .findFirst()
                                                                     .orElseThrow(() -> new AssertionError("no property [" + name + "]"));
    }
}
