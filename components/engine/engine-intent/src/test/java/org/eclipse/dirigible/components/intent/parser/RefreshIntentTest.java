/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.dirigible.components.intent.model.EntityIntent;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.junit.jupiter.api.Test;

/**
 * An entity's {@code refresh:} (dirigible #8375): its pages re-read themselves while a record is in
 * one of the listed statuses, which name the entity's own seeded nomenclature.
 */
class RefreshIntentTest {

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
                  every: PT20S
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
                  - { id: 3, name: DELIVERED, stage: live }
                  - { id: 4, name: RETURNED,  stage: cancelled }
            """;

    @Test
    void refreshWhileResolvesSeededNames() {
        EntityIntent shipment = shipment(IntentParser.parse(YAML));
        assertEquals(List.of(1, 2), shipment.getRefresh()
                                            .statusIds());
        assertEquals(20, shipment.getRefresh()
                                 .everySeconds());
        // An id works as well as a name, and the two mix.
        assertEquals(List.of(1, 2), shipment(IntentParser.parse(YAML.replace("[BOOKED, IN_TRANSIT]", "[1, IN_TRANSIT]"))).getRefresh()
                                                                                                                         .statusIds());
    }

    @Test
    void refreshWithoutAStatusIsRejected() {
        assertIssue(YAML.replace("function: EntityStatus", "required: false")
                        .replace("[BOOKED, IN_TRANSIT]", "[1, 2]"),
                "entity [Shipment] refresh polls while a record is in a status, but [Shipment] has no `function: EntityStatus` relation");
    }

    @Test
    void refreshOverACrossModelStatusIsRejected() {
        String yaml = """
                name: shipping
                uses:
                  - { model: nomenclatures }
                entities:
                  - name: Shipment
                    refresh:
                      while: [IN_TRANSIT]
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: status, kind: manyToOne, to: ShipmentStatus, model: nomenclatures, function: EntityStatus }
                """;
        assertIssue(yaml, "entity [Shipment] refresh polls on the status [ShipmentStatus], which belongs to model [nomenclatures]");
        assertIssue(yaml.replace("[IN_TRANSIT]", "[2]"), "belongs to model [nomenclatures]");
    }

    @Test
    void anUnknownStatusNameIsRejected() {
        assertIssue(YAML.replace("[BOOKED, IN_TRANSIT]", "[BOOKED, LOST]"), "entity [Shipment] refresh while names [LOST]");
        assertIssue(YAML.replace("[BOOKED, IN_TRANSIT]", "[9]"), "entity [Shipment] refresh `while` names [9]");
        assertIssue(YAML.replace("[BOOKED, IN_TRANSIT]", "[]"), "entity [Shipment] refresh `while` lists no status");
        assertIssue(YAML.replace("  every: PT20S", "  every: PT20S\n      pause: true"), "pause");
    }

    @Test
    void anEveryOutsideTheBoundsIsRejected() {
        assertIssue(YAML.replace("PT20S", "PT2S"), "entity [Shipment] refresh `every` [PT2S] must lie between PT5S and PT5M");
        assertIssue(YAML.replace("PT20S", "PT10M"), "must lie between PT5S and PT5M");
        assertIssue(YAML.replace("PT20S", "often"), "entity [Shipment] refresh `every` [often] is not an ISO-8601 duration");
    }

    @Test
    void everyDefaultsToTenSeconds() {
        EntityIntent shipment = shipment(IntentParser.parse(YAML.replace("      every: PT20S\n", "")));
        assertEquals(10, shipment.getRefresh()
                                 .everySeconds());
    }

    private static EntityIntent shipment(IntentModel model) {
        return model.getEntities()
                    .stream()
                    .filter(entity -> "Shipment".equals(entity.getName()))
                    .findFirst()
                    .orElseThrow();
    }

    private static void assertIssue(String yaml, String expected) {
        IntentValidationException thrown = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(thrown.getIssues()
                         .stream()
                         .anyMatch(issue -> issue.contains(expected)),
                "expected an issue containing [" + expected + "] but got " + thrown.getIssues());
    }
}
