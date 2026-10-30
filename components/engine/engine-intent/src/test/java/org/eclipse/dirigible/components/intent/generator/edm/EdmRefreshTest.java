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
 * An entity's {@code refresh:} (dirigible #8375) reaches its pages as two {@code .model} entity
 * attributes: {@code refreshWhile} (the status ids, comma-joined) and {@code refreshEvery}
 * (seconds).
 */
class EdmRefreshTest {

    private static final String YAML = """
            name: shipping
            entities:
              - name: ShipmentStatus
                kind: setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Shipment
                refresh:
                  while: [BOOKED, IN_TRANSIT]
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: status, kind: manyToOne, to: ShipmentStatus, function: EntityStatus }
            seeds:
              - name: shipment-statuses
                entity: ShipmentStatus
                rows:
                  - { id: 1, name: BOOKED }
                  - { id: 2, name: IN_TRANSIT }
                  - { id: 3, name: DELIVERED }
            """;

    @Test
    void theStatusesAndTheIntervalRideTheEntity() {
        Map<String, Object> shipment = entity(YAML, "Shipment");
        assertEquals("1,2", shipment.get("refreshWhile"));
        assertEquals("10", shipment.get("refreshEvery"));
        assertEquals("45",
                entity(YAML.replace("while: [BOOKED, IN_TRANSIT]", "while: [BOOKED, IN_TRANSIT]\n      every: PT45S"), "Shipment").get(
                        "refreshEvery"));
    }

    @Test
    void anEntityWithoutRefreshCarriesNeither() {
        Map<String, Object> shipment = entity(YAML.replace("    refresh:\n      while: [BOOKED, IN_TRANSIT]\n", ""), "Shipment");
        assertFalse(shipment.containsKey("refreshWhile"));
        assertFalse(shipment.containsKey("refreshEvery"));
        assertFalse(entity(YAML, "ShipmentStatus").containsKey("refreshWhile"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> entity(String yaml, String name) {
        Map<String, Object> modelJson = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "shipping");
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) modelJson.get("model")).get("entities");
        return entities.stream()
                       .filter(e -> name.equals(e.get("name")))
                       .findFirst()
                       .orElseThrow();
    }
}
