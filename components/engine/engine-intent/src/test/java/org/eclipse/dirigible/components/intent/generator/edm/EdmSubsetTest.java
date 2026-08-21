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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The {@code subset} promise at the emission layer: the relation lowers to ONE plain VARCHAR
 * property carrying the MULTISELECT widget and its option source ({@code widgetOptionsEntityName} +
 * the dropdown key/value pair) - and deliberately NO {@code relationship*} attribute and NO
 * {@code <relation>} element, because the schema template emits a foreign key gated solely on
 * {@code relationshipEntityName} and a delimited value column must never grow an FK.
 */
class EdmSubsetTest {

    private static final String SCHEDULES = """
            name: schedules
            entities:
              - name: PayerType
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Schedule
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: title, type: string }
                relations:
                  - { name: payerTypes, kind: subset, to: PayerType, required: true }
            """;

    @Test
    void theRelationLowersToOnePlainValuePropertyWithTheMultiselectWidget() {
        Map<String, Object> schedule = entityByName(SCHEDULES, "Schedule");

        Map<String, Object> property = propertyByName(schedule, "PayerTypes");
        assertNotNull(property, "the subset relation must land as a property of the declaring entity");
        assertEquals("SCHEDULE_PAYER_TYPES", property.get("dataName"), "same column formula as a to-one FK, so seeds line up");
        assertEquals("VARCHAR", property.get("dataType"));
        assertEquals("512", property.get("dataLength"), "the length is not authorable, so headroom is the only safety valve");
        assertEquals("MULTISELECT", property.get("widgetType"));
        assertEquals("PayerType", property.get("widgetOptionsEntityName"), "the option source is the dedicated attribute");
        assertEquals("Id", property.get("widgetDropDownKey"));
        assertEquals("Name", property.get("widgetDropDownValue"));
        assertEquals("^\\d+(,\\d+)*$", property.get("widgetPattern"), "the server-side shape guard - the only one, no FK constrains it");
        assertEquals("false", property.get("dataNullable"), "required means at least one selected - empty selection stores null");
        assertEquals("true", property.get("isRequiredProperty"));
    }

    @Test
    void noRelationshipMetadataAndNoRelationElementAreEmitted() {
        Map<String, Object> schedule = entityByName(SCHEDULES, "Schedule");
        Map<String, Object> property = propertyByName(schedule, "PayerTypes");

        assertNull(property.get("relationshipType"), "a relationship attribute would make the downstream treat it as a to-one");
        assertNull(property.get("relationshipCardinality"));
        assertNull(property.get("relationshipName"));
        assertNull(property.get("relationshipEntityName"), "the schema template emits an FK gated solely on this attribute");
        assertNull(property.get("relationshipEntityPerspectiveName"));

        String edm = EdmIntentGenerator.buildEdmXmlForTest(IntentParser.parse(SCHEDULES), "schedules");
        assertFalse(edm.contains("<relation "), "a <relation> element would draw an FK edge and emit an FK constraint");
        assertTrue(edm.contains("widgetOptionsEntityName=\"PayerType\""), "the scalar attributes reach the .edm twin");
        assertTrue(edm.contains("widgetPattern=\"^\\d+(,\\d+)*$\""), "the regex survives into the XML verbatim");
    }

    @Test
    void anOptionalSubsetStaysNullable() {
        String yaml = SCHEDULES.replace(", required: true", "");
        Map<String, Object> property = propertyByName(entityByName(yaml, "Schedule"), "PayerTypes");
        assertEquals("true", property.get("dataNullable"));
        assertNull(property.get("isRequiredProperty"));
    }

    @Test
    void aWhereFilterRidesTheOptionsFilterScalars() {
        String yaml = """
                name: schedules
                entities:
                  - name: PayerType
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                      - { name: active, type: boolean }
                  - name: Schedule
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: payerTypes, kind: subset, to: PayerType, where: { active: true } }
                """;
        Map<String, Object> property = propertyByName(entityByName(yaml, "Schedule"), "PayerTypes");
        assertEquals("Active", property.get("widgetOptionsFilterBy"), "where: rides the same scalars the to-one dropdowns use");
        assertEquals("true", String.valueOf(property.get("widgetOptionsFilterValue")));
    }

    @Test
    void theControlOrderInterleavesIt() {
        String yaml = """
                name: schedules
                entities:
                  - name: PayerType
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: Schedule
                    order: [id, payerTypes, title]
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: title, type: string }
                    relations:
                      - { name: payerTypes, kind: subset, to: PayerType }
                """;
        Map<String, Object> schedule = entityByName(yaml, "Schedule");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> properties = (List<Map<String, Object>>) schedule.get("properties");
        assertEquals("PayerTypes", properties.get(1)
                                             .get("name"),
                "order: places the subset property between the fields like any other property");
    }

    @Test
    void aSettingTargetEmitsTheSameShape() {
        String yaml = """
                name: schedules
                entities:
                  - name: PayerType
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: Schedule
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: payerTypes, kind: subset, to: PayerType }
                """;
        Map<String, Object> property = propertyByName(entityByName(yaml, "Schedule"), "PayerTypes");
        assertEquals("MULTISELECT", property.get("widgetType"));
        assertEquals("PayerType", property.get("widgetOptionsEntityName"),
                "the property carries the entity name only - the SETTING perspective is resolved downstream, at generation time");
        assertNull(property.get("relationshipEntityPerspectiveName"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> entityByName(String yaml, String name) {
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "schedules");
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) model.get("model")).get("entities");
        return entities.stream()
                       .filter(entity -> name.equals(entity.get("name")))
                       .findFirst()
                       .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> propertyByName(Map<String, Object> entity, String name) {
        List<Map<String, Object>> properties = (List<Map<String, Object>>) entity.get("properties");
        return properties.stream()
                         .filter(property -> name.equals(property.get("name")))
                         .findFirst()
                         .orElse(null);
    }
}
