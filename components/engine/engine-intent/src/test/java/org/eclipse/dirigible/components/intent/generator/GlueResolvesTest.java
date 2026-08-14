/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * Verifies the {@code resolves} glue the {@link GlueIntentGenerator} emits: the effective-dated
 * register lookup that fills a fine's driver from the vehicle-assignment row covering the violation
 * date. The descriptor carries the record's event coordinates, the register query (match keys +
 * period bounds), the value column to copy, and all three outcomes.
 */
class GlueResolvesTest {

    private static final String YAML = """
            name: fines
            entities:
              - name: FineStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Vehicle
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
              - name: Driver
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
              - name: VehicleAssignment
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: validFrom, type: date }
                  - { name: validTo, type: date }
                relations:
                  - { name: vehicle, kind: manyToOne, to: Vehicle }
                  - { name: driver, kind: manyToOne, to: Driver }
              - name: Fine
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: violationAt, type: timestamp }
                  - { name: resolution, type: string, readOnly: true }
                relations:
                  - { name: vehicle, kind: manyToOne, to: Vehicle }
                  - { name: driver, kind: manyToOne, to: Driver }
                  - { name: status, kind: manyToOne, to: FineStatus, function: EntityStatus, init: 1 }
            seeds:
              - name: fineStatuses
                entity: FineStatus
                rows:
                  - { id: 1, name: NEW }
                  - { id: 2, name: IDENTIFIED }
                  - { id: 3, name: UNRESOLVED }
            resolves:
              - name: identifyDriver
                event: { onCreate: Fine }
                set: driver
                from: VehicleAssignment
                match: { vehicle: vehicle }
                between: { start: validFrom, end: validTo, value: violationAt }
                outcome: resolution
                found: { setStatus: IDENTIFIED }
                notFound: { setStatus: UNRESOLVED }
                ambiguous: { setStatus: UNRESOLVED }
            """;

    @SuppressWarnings("unchecked")
    @Test
    void emitsTheRegisterLookupGlue() {
        IntentModel model = IntentParser.parse(YAML);
        List<Map<String, Object>> resolves = GlueIntentGenerator.buildResolvesForTest(model);
        assertEquals(1, resolves.size());
        Map<String, Object> resolve = resolves.get(0);

        assertEquals("identifyDriver", resolve.get("name"));
        assertEquals("IdentifyDriver", resolve.get("className"));
        assertEquals("Fine", resolve.get("entity"));
        assertEquals("Id", resolve.get("keyProperty"));
        // onCreate binds to the unsuffixed base topic; the guard is open.
        assertEquals("", resolve.get("topicSuffix"));
        assertEquals("true", resolve.get("guardExpression"));

        assertEquals("Driver", resolve.get("setProperty"));
        assertEquals("VehicleAssignment", resolve.get("registerEntity"));
        assertEquals("Driver", resolve.get("registerValueProperty"));
        assertEquals(List.of(Map.of("registerProperty", "Vehicle", "recordProperty", "Vehicle")), resolve.get("matches"));
        assertEquals("Vehicle = Vehicle", resolve.get("matchSummary"));

        assertEquals("ValidFrom", resolve.get("startProperty"));
        assertEquals("ValidTo", resolve.get("endProperty"));
        assertEquals("ViolationAt", resolve.get("valueProperty"));

        // All three outcomes are carried, plus the observable trace and the column each writes to.
        assertEquals("Resolution", resolve.get("outcomeProperty"));
        assertEquals("Status", resolve.get("statusProperty"));
        assertEquals("2", resolve.get("foundStatus"));
        assertEquals("3", resolve.get("notFoundStatus"));
        assertEquals("3", resolve.get("ambiguousStatus"));
        assertEquals("true", resolve.get("writesStatus"));
    }

    /**
     * An open-ended register - no {@code end} column at all - and no outcome routing: the optional
     * knobs come through blank rather than as a half-rendered expression the template would emit.
     */
    @Test
    void leavesTheOptionalKnobsBlank() {
        String yaml = YAML.replace(", end: validTo,", ",")
                          .replace("outcome: resolution", "")
                          .replace("found: { setStatus: IDENTIFIED }", "")
                          .replace("notFound: { setStatus: UNRESOLVED }", "")
                          .replace("ambiguous: { setStatus: UNRESOLVED }", "");
        Map<String, Object> resolve = GlueIntentGenerator.buildResolvesForTest(IntentParser.parse(yaml))
                                                         .get(0);
        assertEquals("ValidFrom", resolve.get("startProperty"));
        assertEquals("", resolve.get("endProperty"));
        assertEquals("", resolve.get("outcomeProperty"));
        assertEquals("", resolve.get("foundStatus"));
        assertEquals("", resolve.get("notFoundStatus"));
        assertEquals("", resolve.get("ambiguousStatus"));
        // No outcome sets a status, so the handler carries no status parameter and no status branch.
        assertEquals("false", resolve.get("writesStatus"));
    }

    /**
     * An {@code onUpdate} lookup binds to the entity's "-updated" topic and carries its guard - whose
     * status is named, not numbered, and resolved to the seed id before the guard is rendered.
     */
    @Test
    void bindsAnUpdateLookupToTheUpdatedTopic() {
        IntentModel model =
                IntentParser.parse(YAML.replace("event: { onCreate: Fine }", "event: { onUpdate: Fine, when: \"status == NEW\" }"));
        Map<String, Object> resolve = GlueIntentGenerator.buildResolvesForTest(model)
                                                         .get(0);
        assertEquals("-updated", resolve.get("topicSuffix"));
        assertEquals("java.util.Objects.equals(entity.Status, 1)", resolve.get("guardExpression"));
    }
}
