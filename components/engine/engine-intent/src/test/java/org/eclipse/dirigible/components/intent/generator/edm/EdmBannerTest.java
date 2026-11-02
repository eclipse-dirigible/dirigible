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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * {@code banners:} (dirigible #8371) reach the {@code .model} as the entity's {@code banners} list
 * - the conditions in the model's property names with the status names resolved to seed ids, the
 * placeholders PascalCase, the register a related banner reads by its foreign key and key - and
 * only the {@code .model}: the {@code .edm} carries no trace of them.
 */
class EdmBannerTest {

    private static final String YAML = """
            name: shipping
            seeds:
              - name: order-statuses
                entity: OrderStatus
                rows:
                  - { id: 1, name: IN_PROGRESS }
                  - { id: 2, name: ON_HOLD }
              - name: shipment-statuses
                entity: ShipmentStatus
                rows:
                  - { id: 1, name: PENDING }
                  - { id: 3, name: FAILED }
            entities:
              - name: OrderStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 50 }
              - name: ShipmentStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 50 }
              - name: Order
                related:
                  - { entity: Shipment }
            %s
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, length: 20 }
                relations:
                  - { name: status, kind: manyToOne, to: OrderStatus, function: EntityStatus }
              - name: Shipment
                fields:
                  - { name: id,         type: integer, primaryKey: true, generated: true }
                  - { name: failure,    type: string, length: 500, readOnly: true }
                  - { name: finishedAt, type: timestamp, readOnly: true }
                relations:
                  - { name: order,  kind: manyToOne, to: Order }
                  - { name: status, kind: manyToOne, to: ShipmentStatus, function: EntityStatus }
            transitions:
              - { name: RetryShipment, forEntity: Shipment, from: [3], setStatus: 1 }
            """;

    private static final String BANNERS = """
                banners:
                  - text: "The last shipment failed at {finishedAt}: {failure}"
                    variant: negative
                    related: { entity: Shipment, row: latest, when: "status == FAILED" }
                    action: RetryShipment
                  - text: "This order is on hold."
                    when: "status == ON_HOLD"
            """;

    @Test
    @SuppressWarnings("unchecked")
    void theModelCarriesTheBannersWithPascalCasePropertiesAndSeedIds() {
        List<Map<String, Object>> banners = (List<Map<String, Object>>) entity(YAML.formatted(BANNERS)).get("banners");
        assertEquals(2, banners.size());
        Map<String, Object> failed = banners.get(0);
        assertEquals("Order1", failed.get("key"));
        assertEquals("The last shipment failed at {FinishedAt}: {Failure}", failed.get("text"));
        assertEquals("negative", failed.get("variant"));
        assertNull(failed.get("when"));
        assertEquals("Shipment", failed.get("subject"));
        assertEquals("RetryShipment", failed.get("action"));
        Map<String, Object> related = (Map<String, Object>) failed.get("related");
        assertEquals("Shipment", related.get("entity"));
        assertEquals("Order", related.get("fkProperty"));
        assertEquals("Id", related.get("primaryKey"));
        assertEquals("latest", related.get("row"));
        assertEquals("Status == 3", related.get("when"));
        Map<String, Object> hold = banners.get(1);
        assertEquals("Order2", hold.get("key"));
        assertEquals("information", hold.get("variant"));
        assertEquals("Status == 2", hold.get("when"));
        assertEquals("Order", hold.get("subject"));
        assertNull(hold.get("related"));
    }

    @Test
    void noBannersLeaveTheModelAndTheEdmUnchanged() {
        String plain = YAML.formatted("");
        assertNull(entity(plain).get("banners"));
        String edm = EdmIntentGenerator.buildEdmXmlForTest(IntentParser.parse(YAML.formatted(BANNERS)), "shipping");
        assertFalse(edm.contains("banners") || edm.contains("The last shipment failed"), "the .edm carries no banners: " + edm);
        assertEquals(EdmIntentGenerator.buildEdmXmlForTest(IntentParser.parse(plain), "shipping"), edm,
                "a banner changes nothing in the .edm twin");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> entity(String yaml) {
        Map<String, Object> model = EdmIntentGenerator.buildModelJsonForTest(IntentParser.parse(yaml), "shipping");
        List<Map<String, Object>> entities = (List<Map<String, Object>>) ((Map<String, Object>) model.get("model")).get("entities");
        return entities.stream()
                       .filter(entity -> "Order".equals(entity.get("name")))
                       .findFirst()
                       .orElseThrow(() -> new AssertionError("no entity [Order]"));
    }
}
