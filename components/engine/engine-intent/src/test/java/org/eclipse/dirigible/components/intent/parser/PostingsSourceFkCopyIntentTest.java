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

/**
 * Parse + validation coverage for the postings source-FK-copy item cell (issue #6533): a to-one
 * relation item cell copies a source to-one FK onto the generated line (the counterparty
 * dimension). The source here is LOCAL, so the validator can deep-check the copied relation's
 * target entity.
 */
class PostingsSourceFkCopyIntentTest {

    // Doc.Party (source) and EntryLine.Party (item) are both to-one to Party - a valid copy. The
    // trailing comments keep the two otherwise-identical relation lines individually replaceable.
    private static final String VALID = """
            name: ledger
            entities:
              - name: Party
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Doc
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: Status, kind: manyToOne, to: DocStatus, function: EntityStatus, init: 1 }
                  - { name: Party, kind: manyToOne, to: Party }   # source dimension
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
              - name: EntryLine
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: debit, type: decimal }
                  - { name: credit, type: decimal }
                relations:
                  - { name: Entry, kind: manyToOne, to: Entry, composition: true, required: true }
                  - { name: Party, kind: manyToOne, to: Party }   # item dimension
            postings:
              - name: docPosting
                event: { onTransition: Doc, when: "Status == 2" }
                creates: Entry
                backReference: Doc
                map: { date: date }
                items:
                  - { debit: "Amount", Party: Party }
                  - { credit: "Amount" }
            """;

    @Test
    void parsesAValidSourceFkCopy() {
        IntentModel model = IntentParser.parse(VALID);
        assertEquals(1, model.getPostings()
                             .size());
        // The item row still carries the copy cell verbatim - the generator turns it into source.Party.
        assertEquals("Party", model.getPostings()
                                   .get(0)
                                   .getItems()
                                   .get(0)
                                   .get("Party"));
    }

    @Test
    void rejectsAnExpressionValuedRelationCell() {
        // A to-one relation cell copies a FK - you cannot arithmetic-evaluate it.
        String yaml = VALID.replace("Party: Party }", "Party: \"Amount + 1\" }");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("item [Party]") && i.contains("not an expression")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsAnUnknownSourceRelation() {
        String yaml = VALID.replace("Party: Party }", "Party: Ghost }");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("copies [Ghost] which is not a to-one relation of the source entity [Doc]")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsATargetEntityMismatch() {
        // Point the SOURCE relation at a different entity than the item relation - the copy would put a
        // DocStatus FK into a Party column.
        String yaml = VALID.replace("- { name: Party, kind: manyToOne, to: Party }   # source dimension",
                "- { name: Party, kind: manyToOne, to: DocStatus }   # source dimension");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("item [Party]") && i.contains("must be to-one to the same entity")),
                "got: " + ex.getIssues());
    }
}
