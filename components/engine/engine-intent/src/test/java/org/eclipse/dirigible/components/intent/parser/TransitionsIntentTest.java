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

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.TransitionIntent;
import org.junit.jupiter.api.Test;

/**
 * Parse + validation coverage for the {@code transitions} block (the guarded on-demand status
 * flip).
 */
class TransitionsIntentTest {

    private static final String VALID = """
            name: billing
            entities:
              - name: InvoiceStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, documentTitle: true }
                  - { name: paid, type: decimal }
                relations:
                  - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
            transitions:
              - name: VoidInvoice
                forEntity: Invoice
                from: [3, 4]
                setStatus: 8
                when: "Paid == 0"
                label: Void
                icon: ban
            """;

    @Test
    void parsesAValidTransition() {
        IntentModel model = IntentParser.parse(VALID);
        assertEquals(1, model.getTransitions()
                             .size());
        TransitionIntent t = model.getTransitions()
                                  .get(0);
        assertEquals("VoidInvoice", t.getName());
        assertEquals("Invoice", t.getForEntity());
        assertEquals(List.of(3, 4), t.getFrom());
        assertEquals(Integer.valueOf(8), t.getSetStatus());
        assertEquals("Paid == 0", t.getWhen());
        assertEquals("Void", t.getLabel());
        assertEquals("ban", t.getIcon());
    }

    @Test
    void rejectsAnEntityWithoutAStatusRelation() {
        String yaml = """
                name: billing
                entities:
                  - name: Invoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                transitions:
                  - { name: VoidInvoice, forEntity: Invoice, from: [3], setStatus: 8 }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("requires the entity [Invoice] to declare a function: EntityStatus relation")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsAnUnknownForEntity() {
        String yaml = """
                name: billing
                entities:
                  - name: Invoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                transitions:
                  - { name: VoidInvoice, forEntity: Missing, from: [3], setStatus: 8 }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("forEntity references unknown entity [Missing]")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsMissingFromAndSetStatus() {
        String yaml = """
                name: billing
                entities:
                  - name: InvoiceStatus
                    function: Setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: Invoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus }
                transitions:
                  - { name: VoidInvoice, forEntity: Invoice }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("has no from statuses")),
                "got: " + ex.getIssues());
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("has no setStatus")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsASetStatusInsideFrom() {
        String yaml = """
                name: billing
                entities:
                  - name: InvoiceStatus
                    function: Setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: Invoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                    relations:
                      - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus }
                transitions:
                  - { name: VoidInvoice, forEntity: Invoice, from: [3, 8], setStatus: 8 }
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("setStatus [8] is also in from")),
                "got: " + ex.getIssues());
    }

    @Test
    void rejectsAMalformedOrUnresolvableWhenGuard() {
        String malformed = VALID.replace("when: \"Paid == 0\"", "when: \"Paid >= 0\"");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(malformed));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("must be `<Field> == <number>` or `<Field> != <number>`")),
                "got: " + ex.getIssues());

        String unresolvable = VALID.replace("when: \"Paid == 0\"", "when: \"Missing == 0\"");
        IntentValidationException ex2 = assertThrows(IntentValidationException.class, () -> IntentParser.parse(unresolvable));
        assertTrue(ex2.getIssues()
                      .stream()
                      .anyMatch(i -> i.contains("when references [Missing]")),
                "got: " + ex2.getIssues());
    }

    @Test
    void rejectsDuplicateNames() {
        String yaml = VALID + """
                  - name: VoidInvoice
                    forEntity: Invoice
                    from: [3]
                    setStatus: 5
                """;
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("duplicate transition [VoidInvoice]")),
                "got: " + ex.getIssues());
    }
}
