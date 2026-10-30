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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The status badge's colours (#8373): the {@code widgetStatusVariants} attribute of the
 * {@code DOCUMENT_STATUS} property maps each seed id to the Harmonia badge variant its row's
 * {@code variant:} or {@code stage:} names, so the pages stop guessing a colour from the status
 * name when the model says what the status means.
 */
class EdmStatusVariantsTest {

    private static final String YAML = """
            name: shipping
            entities:
              - name: ShipmentStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Carrier
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Shipment
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: Carrier, kind: manyToOne, to: Carrier }
                  - { name: Status, kind: manyToOne, to: ShipmentStatus, function: EntityStatus, init: 1 }
            seeds:
              - name: shipment-statuses
                entity: ShipmentStatus
                rows:
                  - { id: 1, name: BOOKED,     stage: draft }
                  - { id: 2, name: IN_TRANSIT }
                  - { id: 3, name: DELIVERED,  stage: live }
                  - { id: 4, name: RETURNED,   stage: cancelled }
                  - { id: 5, name: LOST,       stage: void }
            """;

    @Test
    void stagesBecomeTheBadgeMap() {
        assertEquals("{\"1\":\"outline\",\"3\":\"positive\",\"4\":\"negative\",\"5\":\"default\"}",
                property(YAML, "Status").get("widgetStatusVariants"));
        assertFalse(property(YAML, "Carrier").containsKey("widgetStatusVariants"), "a plain relation carries no badge map");
        String edm = EdmIntentGenerator.buildEdmXmlForTest(IntentParser.parse(YAML), "shipping");
        assertTrue(edm.contains("widgetStatusVariants=\"{&quot;1&quot;:&quot;outline&quot;"), edm);
    }

    @Test
    void aVariantWinsOverItsStage() {
        String yaml = YAML.replace("{ id: 3, name: DELIVERED,  stage: live }", "{ id: 3, name: DELIVERED,  stage: live, variant: info }")
                          .replace("{ id: 2, name: IN_TRANSIT }", "{ id: 2, name: IN_TRANSIT, variant: warning }");
        assertEquals("{\"1\":\"outline\",\"2\":\"warning\",\"3\":\"information\",\"4\":\"negative\",\"5\":\"default\"}",
                property(yaml, "Status").get("widgetStatusVariants"));
    }

    @Test
    void anUnclassifiedRowIsLeftToTheGuess() {
        Object variants = property(YAML, "Status").get("widgetStatusVariants");
        assertFalse(String.valueOf(variants)
                          .contains("\"2\""),
                "a row with neither stage nor variant must be absent, so the page guesses its colour from the name");
    }

    @Test
    void anUnclassifiedNomenclatureEmitsNothing() {
        String yaml = YAML.replaceAll(",\\s+stage: \\w+", "");
        assertFalse(property(yaml, "Status").containsKey("widgetStatusVariants"));
    }

    /** A cross-model nomenclature is seeded in its owner model - nothing here says what it means. */
    @Test
    void aCrossModelStatusEmitsNothing() {
        IntentModel shipping = IntentParser.parse("""
                name: shipping
                uses:
                  - { model: nomenclatures }
                entities:
                  - name: Shipment
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Status, kind: manyToOne, to: ShipmentStatus, model: nomenclatures, function: EntityStatus }
                """);
        assertFalse(property(shipping, "Shipment", "Status").containsKey("widgetStatusVariants"));
    }

    private static Map<String, Object> property(String yaml, String name) {
        return property(IntentParser.parse(yaml), "Shipment", name);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> property(IntentModel model, String entity, String name) {
        Map<String, Object> modelJson = EdmIntentGenerator.buildModelJsonForTest(model, model.getName());
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) modelJson.get("model")).get("entities");
        Map<String, Object> owner = entities.stream()
                                            .filter(e -> entity.equals(e.get("name")))
                                            .findFirst()
                                            .orElseThrow();
        return ((List<Map<String, Object>>) owner.get("properties")).stream()
                                                                    .filter(p -> name.equals(p.get("name")))
                                                                    .findFirst()
                                                                    .orElseThrow();
    }
}
