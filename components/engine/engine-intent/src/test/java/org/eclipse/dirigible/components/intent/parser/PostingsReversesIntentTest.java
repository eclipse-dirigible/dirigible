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

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.junit.jupiter.api.Test;

/** Parse + validation coverage for the postings {@code reverses:} (red storno) mode. */
class PostingsReversesIntentTest {

    private static final String VALID = """
            name: ledger
            entities:
              - name: Doc
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: Status, kind: manyToOne, to: DocStatus, function: EntityStatus, init: 1 }
              - name: DocStatus
                kind: setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Entry
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: date, type: date }
                relations:
                  - { name: Doc, kind: manyToOne, to: Doc }
                  - { name: Storno, kind: manyToOne, to: Entry }
              - name: EntryLine
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: debit, type: decimal }
                  - { name: credit, type: decimal }
                relations:
                  - { name: Entry, kind: manyToOne, to: Entry, composition: true, required: true }
            postings:
              - name: docPosting
                event: { onTransition: Doc, when: "Status == 2" }
                creates: Entry
                backReference: Doc
                map: { date: date }
                items:
                  - { debit: "Amount" }
                  - { credit: "Amount" }
              - name: docStorno
                event: { onTransition: Doc, when: "Status == 3" }
                reverses: docPosting
                storno: Storno
            """;

    @Test
    void parsesAValidReversal() {
        IntentModel model = IntentParser.parse(VALID);
        assertEquals(2, model.getPostings()
                             .size());
        assertEquals("docPosting", model.getPostings()
                                        .get(1)
                                        .getReverses());
        assertEquals("Storno", model.getPostings()
                                    .get(1)
                                    .getStorno());
    }

    @Test
    void rejectsAnUnknownSibling() {
        String yaml = VALID.replace("reverses: docPosting", "reverses: missingPosting");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("reverses unknown posting [missingPosting]")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsInheritedKeysOnAReversal() {
        String yaml = VALID.replace("storno: Storno", "storno: Storno\n    creates: Entry");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("creates/backReference/rule/map/items are inherited")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsAMissingOrNonSelfStorno() {
        String missing = VALID.replace("\n    storno: Storno", "");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(missing));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("requires `storno: <self relation>`")),
                "got: " + ex.getIssues());

        String nonSelf = VALID.replace("storno: Storno", "storno: Doc");
        IntentValidationException ex2 = assertThrows(IntentValidationException.class, () -> IntentParser.parse(nonSelf));
        assertTrue(ex2.getIssues()
                      .stream()
                      .anyMatch(i -> i.contains("storno [Doc] must be a to-one SELF-relation of [Entry]")),
                "got: " + ex2.getIssues());
    }

    @Test
    void rejectsStornoWithoutReverses() {
        String yaml = VALID.replace("map: { date: date }", "map: { date: date }\n    storno: Storno");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("declares storno without reverses")),
                "got: " + ex.getIssues());
    }
}
