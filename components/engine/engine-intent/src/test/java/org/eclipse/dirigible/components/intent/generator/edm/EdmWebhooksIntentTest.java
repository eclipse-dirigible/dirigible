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
 * Entity {@code webhooks:} (#8689): the entity events a tenant's external system may subscribe to.
 * Only declared events are exposed, so an internal channel never becomes a public contract, and a
 * model that declares none generates exactly as before.
 */
class EdmWebhooksIntentTest {

    private static final String HR = """
            name: hr
            entities:
              - name: LeaveStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 40 }
              - name: Leave
                webhooks: [created, transitioned]
                fields:
                  - { name: leaveId, type: integer, primaryKey: true, generated: true }
                  - { name: uuid,    type: string, length: 36 }
                  - { name: note,    type: string, length: 100 }
                relations:
                  - { name: Phase, kind: manyToOne, to: LeaveStatus, function: EntityStatus, init: 1 }
              - name: Holiday
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 40 }
            """;

    @Test
    void theDeclaredEventsReachTheModelWithTheThreePropertiesAThinEventCarries() {
        Map<String, Object> leave = entity(model(HR), "Leave");

        assertEquals("created,transitioned", leave.get("webhooks"));
        assertEquals("LeaveId", leave.get("webhookIdProperty"));
        assertEquals("Uuid", leave.get("webhookUuidProperty"));
        assertEquals("Phase", leave.get("webhookStatusProperty"));
    }

    @Test
    void anEntityThatExposesNothingCarriesNothing() {
        Map<String, Object> holiday = entity(model(HR), "Holiday");

        for (String key : List.of("webhooks", "webhookIdProperty", "webhookUuidProperty", "webhookStatusProperty")) {
            assertNull(holiday.get(key), key);
        }
    }

    @Test
    void anUnknownEventIsRejected() {
        assertIssue(HR.replace("[created, transitioned]", "[created, rekeyed]"), "webhooks names [rekeyed]");
    }

    @Test
    void anEventNamedTwiceIsRejected() {
        assertIssue(HR.replace("[created, transitioned]", "[created, created]"), "webhooks names [created] more than once");
    }

    @Test
    void transitionedNeedsAStatusRelation() {
        assertIssue(HR.replace("  - name: Holiday\n", "  - name: Holiday\n    webhooks: [transitioned]\n"),
                "entity [Holiday] webhooks names [transitioned], but the entity has no `function: EntityStatus` relation");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> entity(Map<String, Object> model, String name) {
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) model.get("model")).get("entities");
        return entities.stream()
                       .filter(entity -> name.equals(entity.get("name")))
                       .findFirst()
                       .orElseThrow(() -> new AssertionError("no entity [" + name + "]"));
    }

    private static Map<String, Object> model(String yaml) {
        return EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "hr");
    }

    private static void assertIssue(String yaml, String expected) {
        IntentValidationException exception = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));

        assertTrue(exception.getMessage()
                            .contains(expected),
                () -> "expected an issue containing [" + expected + "] but got: " + exception.getMessage());
    }
}
