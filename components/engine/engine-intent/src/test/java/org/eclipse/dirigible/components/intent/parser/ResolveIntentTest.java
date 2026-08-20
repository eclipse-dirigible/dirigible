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

import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.ResolveIntent;
import org.junit.jupiter.api.Test;

/**
 * Parse + validation coverage for the {@code resolves} block - the effective-dated register lookup
 * that fills a to-one from the register row valid on a record's date.
 */
class ResolveIntentTest {

    /**
     * The canonical shape: a fine carries a vehicle and a violation date, and the driver comes from the
     * vehicle-assignment register row covering that date. The statuses are named, not numbered.
     */
    private static final String VALID = """
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
                  - { name: plate, type: string }
              - name: Driver
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
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

    @Test
    void parsesAValidLookup() {
        IntentModel model = IntentParser.parse(VALID);
        assertEquals(1, model.getResolves()
                             .size());
        ResolveIntent resolve = model.getResolves()
                                     .get(0);
        assertEquals("identifyDriver", resolve.getName());
        assertEquals("Fine", resolve.getEvent()
                                    .get("onCreate"));
        assertEquals("driver", resolve.getSet());
        assertEquals("VehicleAssignment", resolve.getFrom());
        assertEquals(Map.of("vehicle", "vehicle"), resolve.getMatch());
        assertEquals("validFrom", resolve.getBetween()
                                         .get("start"));
        assertEquals("validTo", resolve.getBetween()
                                       .get("end"));
        assertEquals("violationAt", resolve.getBetween()
                                           .get("value"));
        assertEquals("resolution", resolve.getOutcome());
    }

    /**
     * The seeded status names resolve to their ids before the typed mapping, so every outcome carries a
     * plain integer downstream (an id is positional - a name cannot be silently retargeted).
     */
    @Test
    void resolvesTheOutcomeStatusesByTheirSeededNames() {
        IntentModel model = IntentParser.parse(VALID);
        ResolveIntent resolve = model.getResolves()
                                     .get(0);
        assertEquals(2.0, ((Number) resolve.getFound()
                                           .get("setStatus")).doubleValue());
        assertEquals(3.0, ((Number) resolve.getNotFound()
                                           .get("setStatus")).doubleValue());
        assertEquals(3.0, ((Number) resolve.getAmbiguous()
                                           .get("setStatus")).doubleValue());
    }

    @Test
    void rejectsASetThatIsNotAToOneOfTheRecord() {
        IntentValidationException ex =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(VALID.replace("set: driver", "set: operator")));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains("set [operator] is not a to-one relation of [Fine]")),
                "got: " + ex.getIssues());
    }

    /** The register's own relations, isolated so a test can vary just them. */
    private static final String REGISTER_DRIVER_RELATION = "      - { name: driver, kind: manyToOne, to: Driver }\n  - name: Fine";

    @Test
    void rejectsARegisterThatHoldsNothingToResolve() {
        String yaml = VALID.replace(REGISTER_DRIVER_RELATION, "  - name: Fine");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains("register [VehicleAssignment] has no to-one relation to [Driver]")),
                "got: " + ex.getIssues());
    }

    /**
     * Two register columns pointing at the same target would make the copied value a coin toss - which
     * is exactly what this construct exists to refuse, so it is refused at Generate too.
     */
    @Test
    void rejectsARegisterWithTwoRelationsToTheSameTarget() {
        String yaml = VALID.replace(REGISTER_DRIVER_RELATION, "      - { name: driver, kind: manyToOne, to: Driver }\n"
                + "      - { name: standIn, kind: manyToOne, to: Driver }\n  - name: Fine");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains("has 2 to-one relations to [Driver]")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsANonDatePeriodBound() {
        IntentValidationException ex = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(VALID.replace("{ name: validFrom, type: date }", "{ name: validFrom, type: string }")));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains("between.start [validFrom] must be a date or timestamp field")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsAMatchKeyTheRegisterDoesNotHave() {
        IntentValidationException ex = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(VALID.replace("match: { vehicle: vehicle }", "match: { car: vehicle }")));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(
                             issue -> issue.contains("match key [car] is not a field or to-one relation of register [VehicleAssignment]")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsALookupWithoutMatchKeys() {
        IntentValidationException ex = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(VALID.replace("match: { vehicle: vehicle }", "match: {}")));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains("has no match keys")),
                "got: " + ex.getIssues());
    }

    /**
     * The outcome statuses as plain seed ids, for the variants that break the record binding - a symbol
     * needs a resolvable record to look its nomenclature up in, and would fail first with its own
     * error.
     */
    private static final String NUMBERED = VALID.replace("setStatus: IDENTIFIED", "setStatus: 2")
                                                .replace("setStatus: UNRESOLVED", "setStatus: 3");

    @Test
    void rejectsABindingToOnDelete() {
        IntentValidationException ex = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(NUMBERED.replace("event: { onCreate: Fine }", "event: { onDelete: Fine }")));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains("cannot bind to onDelete")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsAnOutcomeFieldThatIsNotAString() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(
                VALID.replace("{ name: resolution, type: string, readOnly: true }", "{ name: resolution, type: integer }")));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains("outcome [resolution] must be a string field")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsAStatusOutcomeWithoutAnEntityStatusRelation() {
        String yaml = NUMBERED.replace(", function: EntityStatus, init: 1 }", ", init: 1 }");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains("declares no function: EntityStatus relation")),
                "got: " + ex.getIssues());
    }

    /**
     * The trace exists to be read afterwards, and it is truncated at the DB where nothing reports it.
     * Routing widens the set the handler writes, because a status the record cannot take amends the
     * trace rather than losing the whole attempt.
     */
    @Test
    void rejectsAnOutcomeFieldTooShortForTheValuesWritten() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(
                VALID.replace("{ name: resolution, type: string, readOnly: true }", "{ name: resolution, type: string, length: 12 }")));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains("outcome [resolution] is length [12], too short") && issue.contains("at least [19]")
                             && issue.contains("routes by setStatus")),
                "got: " + ex.getIssues());
        // Without routing, the handler writes only the three plain outcomes, so 12 is ample.
        IntentParser.parse(
                VALID.replace("{ name: resolution, type: string, readOnly: true }", "{ name: resolution, type: string, length: 12 }")
                     .replace("found: { setStatus: IDENTIFIED }", "found: {}")
                     .replace("notFound: { setStatus: UNRESOLVED }", "notFound: {}")
                     .replace("ambiguous: { setStatus: UNRESOLVED }", "ambiguous: {}"));
    }

    @Test
    void rejectsAnUnknownRegister() {
        IntentValidationException ex = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(VALID.replace("from: VehicleAssignment", "from: Missing")));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains("from references unknown entity [Missing]")),
                "got: " + ex.getIssues());
    }
}
