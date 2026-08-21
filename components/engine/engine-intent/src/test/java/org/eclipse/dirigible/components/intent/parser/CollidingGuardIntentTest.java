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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Two event-driven rules may not share one at-most-once guard.
 *
 * <p>
 * The guard the templates emit is {@code findAll(eq(<backReference>, sourceId))}: it asks whether
 * the source already has a row through that relation and cannot tell which rule wrote it. Two rules
 * sharing a target AND a back-reference therefore divide silently into a winner and a loser - the
 * first to fire claims the source forever, the other hands back that row instead of writing, for
 * this source and every future one. It parses, generates and compiles; the loser simply looks like
 * a rule whose condition never matched.
 */
class CollidingGuardIntentTest {

    /**
     * The reported shape: two transition-driven log rules onto one target through one back-reference.
     */
    private static final String YAML = """
            name: fines
            entities:
              - name: FineStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Fine
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, precision: 12, scale: 2 }
                relations:
                  - { name: status, kind: manyToOne, to: FineStatus, function: EntityStatus, init: 1 }
              - name: FineLog
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 200 }
                relations:
                  - { name: fine, kind: manyToOne, to: Fine }
            seeds:
              - name: fineStatuses
                entity: FineStatus
                rows:
                  - { id: 1, name: NEW }
                  - { id: 2, name: UNRESOLVED }
                  - { id: 3, name: DECLARED }
            generates:
              - name: log-identification-failed
                from: Fine
                to: FineLog
                event: { onTransition: Fine, when: "Status == UNRESOLVED" }
                map: { fine: id }
              - name: log-declaration-created
                from: Fine
                to: FineLog
                event: { onTransition: Fine, when: "Status == DECLARED" }
                map: { fine: id }
            """;

    @Test
    void rejectsTwoGeneratesSharingATargetAndABackReference() {
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(YAML));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains("shares its at-most-once guard on [FineLog] through back-reference [fine]")
                             && issue.contains("log-")),
                "got: " + ex.getIssues());
    }

    /**
     * Disjoint {@code when} guards do not make it safe, and that is the trap: the author reads two
     * mutually exclusive conditions and expects two independent rules. The collision is decided by the
     * target's EXISTENCE, so the second rule no-ops on a Fine whose condition it matched perfectly.
     */
    @Test
    void rejectsEvenWhenTheConditionsAreDisjoint() {
        assertThrows(IntentValidationException.class, () -> IntentParser.parse(YAML));
    }

    /**
     * The same model with a second entity, a second back-reference and a button variant, assembled from
     * one place: the fixtures differ only in the block under test, and string surgery on a text block
     * is how these went wrong the first time.
     */
    private static String model(String extraEntities, String logRelations, String generates) {
        return """
                name: fines
                entities:
                  - name: FineStatus
                    function: Setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: Fine
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: amount, type: decimal, precision: 12, scale: 2 }
                    relations:
                      - { name: status, kind: manyToOne, to: FineStatus, function: EntityStatus, init: 1 }
                %s  - name: FineLog
                    fields:
                      - { name: id,   type: integer, primaryKey: true, generated: true }
                      - { name: note, type: string, length: 200 }
                    relations:
                %s
                seeds:
                  - name: fineStatuses
                    entity: FineStatus
                    rows:
                      - { id: 1, name: NEW }
                      - { id: 2, name: UNRESOLVED }
                      - { id: 3, name: DECLARED }
                generates:
                %s
                """.formatted(extraEntities, logRelations, generates);
    }

    private static final String ONE_BACK_REF = "      - { name: fine, kind: manyToOne, to: Fine }";

    /** Separate back-references are separate guards, so two rules onto one target are fine. */
    @Test
    void acceptsTwoRulesWithSeparateBackReferences() {
        String yaml = model("", ONE_BACK_REF + "\n      - { name: declared, kind: manyToOne, to: Fine }", """
                - name: log-identification-failed
                  from: Fine
                  to: FineLog
                  event: { onTransition: Fine, when: "Status == UNRESOLVED" }
                  map: { fine: id }
                - name: log-declaration-created
                  from: Fine
                  to: FineLog
                  event: { onTransition: Fine, when: "Status == DECLARED" }
                  map: { declared: id }""");
        assertDoesNotThrow(() -> IntentParser.parse(yaml));
    }

    /** A different target is a different guard. */
    @Test
    void acceptsTwoRulesOntoDifferentTargets() {
        String yaml = model("""
                  - name: FineNote
                    fields:
                      - { name: id,   type: integer, primaryKey: true, generated: true }
                      - { name: note, type: string, length: 200 }
                    relations:
                      - { name: fine, kind: manyToOne, to: Fine }
                """, ONE_BACK_REF, """
                - name: log-identification-failed
                  from: Fine
                  to: FineLog
                  event: { onTransition: Fine, when: "Status == UNRESOLVED" }
                  map: { fine: id }
                - name: log-declaration-created
                  from: Fine
                  to: FineNote
                  event: { onTransition: Fine, when: "Status == DECLARED" }
                  map: { fine: id }""");
        assertDoesNotThrow(() -> IntentParser.parse(yaml));
    }

    /**
     * Two appending rules cannot collide - neither reads the other's rows, because {@code mode: append}
     * is the ABSENCE of the guard. This is the escape hatch the error message points at.
     */
    @Test
    void acceptsTwoAppendingRulesSharingEverything() {
        String yaml = model("", ONE_BACK_REF, """
                - name: log-identification-failed
                  from: Fine
                  to: FineLog
                  event: { onTransition: Fine, when: "Status == UNRESOLVED", mode: append }
                  map: { fine: id }
                - name: log-declaration-created
                  from: Fine
                  to: FineLog
                  event: { onTransition: Fine, when: "Status == DECLARED", mode: append }
                  map: { fine: id }""");
        assertDoesNotThrow(() -> IntentParser.parse(yaml));
    }

    /**
     * One appending and one guarded rule DO collide, which #6800 does not cover: the rows the appender
     * writes carry the back-reference, and that is all the guarded rule's lookup needs to be satisfied
     * forever.
     */
    @Test
    void rejectsAnAppendingRuleSharingWithAGuardedOne() {
        String yaml = model("", ONE_BACK_REF, """
                - name: log-identification-failed
                  from: Fine
                  to: FineLog
                  event: { onTransition: Fine, when: "Status == UNRESOLVED", mode: append }
                  map: { fine: id }
                - name: log-declaration-created
                  from: Fine
                  to: FineLog
                  event: { onTransition: Fine, when: "Status == DECLARED" }
                  map: { fine: id }""");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains("(mode: append)")),
                "the message should name the appending rule as such, got: " + ex.getIssues());
    }

    /**
     * A non-event-driven create-from is a BUTTON: a person decides when it runs, and the template emits
     * no guard at all, so two of them share nothing.
     */
    @Test
    void acceptsTwoButtonRulesSharingEverything() {
        String yaml = model("", ONE_BACK_REF, """
                - name: log-identification-failed
                  from: Fine
                  to: FineLog
                  map: { fine: id }
                - name: log-declaration-created
                  from: Fine
                  to: FineLog
                  map: { fine: id }""");
        assertDoesNotThrow(() -> IntentParser.parse(yaml));
    }
}
