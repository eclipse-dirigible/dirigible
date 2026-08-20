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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.junit.jupiter.api.Test;

/**
 * The static register filter on a {@code resolves:} lookup ({@code where:}).
 *
 * <p>
 * The defect it closes: every {@code match:} pair binds a register column to a column of the
 * RECORD, so "and only the rows that are still valid" was inexpressible. A register accumulates
 * cancelled and superseded rows, and each one keeps covering its old period forever - so a lookup
 * with exactly one right answer reports {@code ambiguous} and routes to a human, quietly and worse
 * over time.
 *
 * <p>
 * The fixture is the reported one: an assignment register whose corrections are kept as CANCELLED
 * rows beside the ACTIVE one, with the two nomenclatures deliberately numbered differently so a
 * symbol resolved against the wrong entity's lifecycle cannot pass as the right id.
 */
class GlueResolveWhereTest {

    private static final String YAML = """
            name: fines
            entities:
              - name: FineStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: AssignmentStatus
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
                  - { name: kind, type: string, length: 20 }
                  - { name: primary, type: boolean }
                relations:
                  - { name: vehicle, kind: manyToOne, to: Vehicle }
                  - { name: driver, kind: manyToOne, to: Driver }
                  - { name: status, kind: manyToOne, to: AssignmentStatus, function: EntityStatus, init: 7 }
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
                  - { id: 2, name: ACTIVE }
                  - { id: 3, name: UNRESOLVED }
              - name: assignmentStatuses
                entity: AssignmentStatus
                rows:
                  - { id: 7, name: ACTIVE }
                  - { id: 8, name: CANCELLED }
            resolves:
              - name: identifyDriver
                event: { onCreate: Fine }
                set: driver
                from: VehicleAssignment
                match: { vehicle: vehicle }
                where: { status: ACTIVE }
                between: { start: validFrom, end: validTo, value: violationAt }
                outcome: resolution
                found: { setStatus: UNRESOLVED }
            """;

    @SuppressWarnings("unchecked")
    private static List<Map<String, String>> filtersOf(String yaml) {
        Map<String, Object> resolve = GlueIntentGenerator.buildResolvesForTest(IntentParser.parse(yaml))
                                                         .get(0);
        return (List<Map<String, String>>) resolve.get("filters");
    }

    /**
     * The filter reaches the glue as a property plus a ready Java literal, so the template only has to
     * chain it - nothing about a value's type is decided in Velocity.
     */
    @Test
    void emitsTheFilterAsAPropertyAndAJavaLiteral() {
        List<Map<String, String>> filters = filtersOf(YAML);
        assertEquals(1, filters.size());
        assertEquals("Status", filters.get(0)
                                      .get("property"));
        assertEquals("7", filters.get(0)
                                 .get("literal"));
    }

    /**
     * The whole point of the design: {@code where} narrows the REGISTER, so a symbolic status resolves
     * on the register's nomenclature. Both entities seed a status called ACTIVE - the record's is 2,
     * the register's is 7 - so resolving against the record would produce a plausible id for the wrong
     * lifecycle and filter the register on a value it never holds.
     */
    @Test
    void resolvesASymbolicStatusOnTheRegistersOwnNomenclature() {
        assertEquals("7", filtersOf(YAML).get(0)
                                         .get("literal"));
    }

    /** A plain string column is quoted, and is NOT run through the status resolver. */
    @Test
    void quotesAStringLiteralOnAnOrdinaryColumn() {
        List<Map<String, String>> filters = filtersOf(YAML.replace("where: { status: ACTIVE }", "where: { kind: PRIMARY }"));
        assertEquals("Kind", filters.get(0)
                                    .get("property"));
        assertEquals("\"PRIMARY\"", filters.get(0)
                                           .get("literal"));
    }

    /** A boolean is written bare - quoted, it would filter a boolean column with the text "true". */
    @Test
    void writesABooleanBare() {
        List<Map<String, String>> filters = filtersOf(YAML.replace("where: { status: ACTIVE }", "where: { primary: true }"));
        assertEquals("true", filters.get(0)
                                    .get("literal"));
    }

    /**
     * Several pairs are ANDed - unlike the relation-level where, which two EDM attributes cap at one.
     */
    @Test
    void andsSeveralPairs() {
        List<Map<String, String>> filters =
                filtersOf(YAML.replace("where: { status: ACTIVE }", "where: { status: ACTIVE, kind: PRIMARY }"));
        assertEquals(2, filters.size());
        assertEquals("7", filters.get(0)
                                 .get("literal"));
        assertEquals("\"PRIMARY\"", filters.get(1)
                                           .get("literal"));
    }

    /** A lookup that declares no filter carries an empty list, not a null the template would print. */
    @Test
    void aLookupWithoutAFilterCarriesAnEmptyList() {
        List<Map<String, String>> filters = filtersOf(YAML.replace("where: { status: ACTIVE }", ""));
        assertTrue(filters.isEmpty(), "got: " + filters);
        Map<String, Object> resolve =
                GlueIntentGenerator.buildResolvesForTest(IntentParser.parse(YAML.replace("where: { status: ACTIVE }", "")))
                                   .get(0);
        assertEquals("", resolve.get("filterSummary"));
    }

    @Test
    void rejectsAKeyThatIsNotAPropertyOfTheRegister() {
        IntentValidationException ex = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(YAML.replace("where: { status: ACTIVE }", "where: { archived: 1 }")));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains("where key [archived] is not a field or to-one relation of register")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsANonScalarValue() {
        IntentValidationException ex = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(YAML.replace("where: { status: ACTIVE }", "where: { kind: [A, B] }")));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains("where [kind] value must be a scalar literal")),
                "got: " + ex.getIssues());
    }

    /**
     * A literal on a column {@code match} already binds to the record either repeats it or contradicts
     * it into matching nothing, and which one depends on data the parser cannot see.
     */
    @Test
    void rejectsAKeyThatIsAlreadyAMatchKey() {
        IntentValidationException ex = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(YAML.replace("where: { status: ACTIVE }", "where: { vehicle: 4 }")));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains("where [vehicle] is already a match key")),
                "got: " + ex.getIssues());
    }
}
