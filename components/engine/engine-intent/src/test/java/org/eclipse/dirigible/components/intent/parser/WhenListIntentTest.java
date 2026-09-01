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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.junit.jupiter.api.Test;

/**
 * The list form of a create-from's {@code when} guard (issue #6957): an implicit AND of the status
 * comparison plus comparisons against the source's own string fields, which is what lets a consumer
 * tell apart two paths converging on one status - the automatic {@code resolves:} route stamped by
 * its {@code outcome:} trace field versus a manual task. AND-only, equality-only, one comparison
 * per property - the restriction is encoded in the SHAPE, so there is no expression grammar to
 * grow.
 */
class WhenListIntentTest {

    /** The fines shape of the motivating case: a status lifecycle plus a readOnly trace field. */
    private static final String HEAD = """
            name: fines
            entities:
              - name: FineStatus
                function: Setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Fine
                fields:
                  - { name: id,         type: integer, primaryKey: true, generated: true }
                  - { name: note,       type: string }
                  - { name: amount,     type: decimal }
                  - { name: resolution, type: string, readOnly: true }
                relations:
                  - { name: Status, kind: manyToOne, to: FineStatus, function: EntityStatus, init: 1 }
              - name: FineLog
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string }
                relations:
                  - { name: Fine, kind: manyToOne, to: Fine }
            seeds:
              - name: fine-statuses
                entity: FineStatus
                rows:
                  - { id: 1, name: NEW }
                  - { id: 2, name: IDENTIFIED }
            generates:
              - name: log-identified
                from: Fine
                to: FineLog
                forEntity: Fine
            """;

    @Test
    void aListWhenParsesAndItsStatusNameResolvesToTheSeedId() {
        IntentModel model = IntentParser.parse(HEAD + """
                    event:
                      onTransition: Fine
                      when:
                        - "Status == IDENTIFIED"
                        - "resolution == found"
                    map:
                      Fine: id
                """);

        Object when = model.getGenerates()
                           .get(0)
                           .getEvent()
                           .get("when");
        assertTrue(when instanceof List<?>, "the list survives into the typed model, got: " + when);
        List<?> terms = (List<?>) when;
        assertEquals("Status == 2", terms.get(0), "the status NAME resolves to its seed id inside the list");
        assertEquals("resolution == found", terms.get(1), "the string term passes through the symbol resolver untouched");
    }

    @Test
    void quotedLiteralsParseTheSameAsBareWords() {
        IntentParser.parse(HEAD + """
                    event:
                      onTransition: Fine
                      when:
                        - "Status == 2"
                        - "resolution == 'notFound-notRouted'"
                    map:
                      Fine: id
                """);
    }

    @Test
    void aStringTermMayAlsoExclude() {
        IntentParser.parse(HEAD + """
                    event:
                      onTransition: Fine
                      when:
                        - "Status == 2"
                        - "resolution != ambiguous"
                    map:
                      Fine: id
                """);
    }

    @Test
    void anOnTransitionListWithoutTheStatusGuardIsRefused() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(HEAD + """
                    event:
                      onTransition: Fine
                      when:
                        - "resolution == found"
                    map:
                      Fine: id
                """));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains("must include the status guard")),
                "got: " + ex.getIssues());
    }

    @Test
    void anEmptyListIsRefused() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(HEAD + """
                    event:
                      onTransition: Fine
                      when: []
                    map:
                      Fine: id
                """));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains("when list must not be empty")),
                "got: " + ex.getIssues());
    }

    @Test
    void aSecondComparisonOnTheSamePropertyIsRefused() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(HEAD + """
                    event:
                      onTransition: Fine
                      when:
                        - "Status == 2"
                        - "resolution == found"
                        - "resolution != ambiguous"
                    map:
                      Fine: id
                """));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains("guards [resolution] twice")),
                "got: " + ex.getIssues());
    }

    @Test
    void twoNumericComparisonsAreRefused() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(HEAD + """
                    event:
                      onTransition: Fine
                      when:
                        - "Status == 2"
                        - "id == 7"
                    map:
                      Fine: id
                """));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains("more than one numeric comparison")),
                "got: " + ex.getIssues());
    }

    @Test
    void aStringTermOnANonStringFieldIsRefused() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(HEAD + """
                    event:
                      onTransition: Fine
                      when:
                        - "Status == 2"
                        - "amount == found"
                    map:
                      Fine: id
                """));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains("[amount]") && issue.contains("decimal")),
                "got: " + ex.getIssues());
    }

    @Test
    void aStringTermOnAnUnknownPropertyIsRefused() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(HEAD + """
                    event:
                      onTransition: Fine
                      when:
                        - "Status == 2"
                        - "verdict == found"
                    map:
                      Fine: id
                """));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains("[verdict], which is not a field of [Fine]")),
                "got: " + ex.getIssues());
    }

    @Test
    void aProcessTriggerAcceptsAListWhen() {
        IntentParser.parse(HEAD + """
                    event: { onCreate: Fine }
                    map:
                      Fine: id
                processes:
                  - name: ManualIdentification
                    trigger:
                      onTransition: Fine
                      when:
                        - "Status == IDENTIFIED"
                        - "resolution == found"
                    steps:
                      - { name: done, kind: end }
                """);
    }

    @Test
    void aResolveRefusesAListWhen() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse("""
                name: fines
                entities:
                  - name: Driver
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: Vehicle
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                  - name: Assignment
                    fields:
                      - { name: id,        type: integer, primaryKey: true, generated: true }
                      - { name: validFrom, type: timestamp }
                      - { name: validTo,   type: timestamp }
                    relations:
                      - { name: vehicle, kind: manyToOne, to: Vehicle }
                      - { name: driver,  kind: manyToOne, to: Driver }
                  - name: Fine
                    fields:
                      - { name: id,   type: integer, primaryKey: true, generated: true }
                      - { name: kind, type: string }
                      - { name: at,   type: timestamp }
                    relations:
                      - { name: vehicle, kind: manyToOne, to: Vehicle }
                      - { name: driver,  kind: manyToOne, to: Driver }
                resolves:
                  - name: lookup
                    event:
                      onCreate: Fine
                      when: ["kind == speeding"]
                    set: driver
                    from: Assignment
                    match: { vehicle: vehicle }
                    between: { start: validFrom, end: validTo, value: at }
                """));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains("does not take a list here")),
                "got: " + ex.getIssues());
    }
}
