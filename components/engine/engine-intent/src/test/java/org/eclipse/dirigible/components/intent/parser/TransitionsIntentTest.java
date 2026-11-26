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
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    /** {@code destructive: true} (#8364): opt-in, and kept on the model for the generator. */
    @Test
    void aDestructiveTransitionParses() {
        assertTrue(IntentParser.parse(VALID + "    destructive: true\n")
                               .getTransitions()
                               .get(0)
                               .isDestructive());
        assertFalse(IntentParser.parse(VALID)
                                .getTransitions()
                                .get(0)
                                .isDestructive(),
                "destructive is opt-in");
    }

    /** A destructive transition is never the record's filled button (#8364). */
    @Test
    void aDestructivePrimaryTransitionIsRefused() {
        IntentValidationException ex = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(VALID + "    destructive: true\n    primary: true\n"));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains(
                             "transition [VoidInvoice] declares destructive and primary - a destructive action is never the record's filled button")),
                ex.getIssues()
                  .toString());
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

    /** A leave request its employee owns through a personal relation (#8406). */
    private static final String PERSONAL = """
            name: vacations
            entities:
              - name: Employee
                identity: email
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: email, type: string, unique: true }
              - name: VacationStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: VacationRequest
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: Employee, kind: manyToOne, to: Employee, required: true, personal: true }
                  - { name: Status, kind: manyToOne, to: VacationStatus, function: EntityStatus, init: 1 }
            transitions:
              - { name: WithdrawVacationRequest, forEntity: VacationRequest, from: [2, 3], setStatus: 6, personal: true }
            """;

    /** {@code personal: true} (#8406): opt-in, and kept on the model for the generators. */
    @Test
    void aPersonalTransitionParses() {
        assertTrue(IntentParser.parse(PERSONAL)
                               .getTransitions()
                               .get(0)
                               .isPersonal());
        assertFalse(IntentParser.parse(PERSONAL.replace("setStatus: 6, personal: true }", "setStatus: 6 }"))
                                .getTransitions()
                                .get(0)
                                .isPersonal(),
                "personal is opt-in");
    }

    /**
     * The personal surface is scoped by the entity's own personal relation - without one there is none.
     */
    @Test
    void aPersonalTransitionOnAnEntityWithoutAPersonalRelationIsRefused() {
        String yaml = PERSONAL.replace("required: true, personal: true }", "required: true }");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("transition [WithdrawVacationRequest] declares personal but the entity [VacationRequest]"
                             + " has no personal: true relation")),
                "got: " + ex.getIssues());
    }

    /** A see-only personal surface never writes - a status change included. */
    @Test
    void aPersonalTransitionOnAReadOnlyPersonalSurfaceIsRefused() {
        String yaml = PERSONAL.replace("required: true, personal: true }", "required: true, personal: true, personalReadOnly: true }");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("transition [WithdrawVacationRequest] declares personal but the personal surface of"
                             + " [VacationRequest] is personalReadOnly")),
                "got: " + ex.getIssues());
    }

    /** A protocol issued by a button, numbered at the issue (#8493). */
    private static final String STAMP = """
            name: protocols
            entities:
              - name: ProtocolStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Protocol
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, number: { series: Protocol, stampOn: issue } }
                relations:
                  - { name: Status, kind: manyToOne, to: ProtocolStatus, function: EntityStatus, init: 1 }
            transitions:
              - { name: IssueProtocol, forEntity: Protocol, from: [1], setStatus: 2, stamp: number }
            """;

    /** {@code stamp: number} (#8493): kept on the model for the generators. */
    @Test
    void aStampingTransitionParses() {
        assertTrue(IntentParser.parse(STAMP)
                               .getTransitions()
                               .get(0)
                               .stampsNumber());
    }

    /** A move stamps a number and nothing else. */
    @Test
    void aStampOtherThanNumberIsRefused() {
        IntentValidationException ex =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(STAMP.replace("stamp: number", "stamp: date")));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("transition [IssueProtocol] stamp [date] must be `number`")),
                "got: " + ex.getIssues());
    }

    /** A number drawn at create has nothing left to issue - nor has an entity without a number. */
    @Test
    void aStampWithoutAnIssueNumberIsRefused() {
        IntentValidationException ex =
                assertThrows(IntentValidationException.class, () -> IntentParser.parse(STAMP.replace("stampOn: issue", "stampOn: create")));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains("transition [IssueProtocol] declares stamp: number but [Protocol] has no"
                             + " `number: { stampOn: issue }` field")),
                "got: " + ex.getIssues());
    }

    /** A Void that asks for its reason (#7909): the record's own fields and to-one relations. */
    private static final String PROMPT = """
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
                  - { name: number, type: string, number: { series: Invoice, stampOn: issue } }
                  - { name: voidReason, type: string, readOnly: true }
                  - { name: voidedAt, type: timestamp }
                  - { name: voidedOn, type: date, stampOn: { status: 8 } }
                relations:
                  - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
                  - { name: ReplacedBy, kind: manyToOne, to: Invoice, readOnly: true }
            transitions:
              - name: VoidInvoice
                forEntity: Invoice
                from: [3, 4]
                setStatus: 8
                prompt:
                  - { field: voidReason, required: true }
                  - { field: ReplacedBy }
            seeds:
              - name: invoice-statuses
                entity: InvoiceStatus
                rows:
                  - { id: 3, name: ISSUED }
                  - { id: 4, name: SENT }
                  - { id: 8, name: VOIDED }
            """;

    @Test
    void aPromptedTransitionParses() {
        TransitionIntent t = IntentParser.parse(PROMPT)
                                         .getTransitions()
                                         .get(0);
        assertTrue(t.hasPrompt());
        assertEquals(List.of("voidReason", "ReplacedBy"), t.getPrompt()
                                                           .stream()
                                                           .map(p -> p.getField())
                                                           .toList());
        assertTrue(t.getPrompt()
                    .get(0)
                    .isRequired());
        assertFalse(IntentParser.parse(VALID)
                                .getTransitions()
                                .get(0)
                                .hasPrompt());
    }

    /**
     * A prompt asks for what a person decides about the record: never a property it does not have, the
     * status the move writes, a column the platform writes, a timestamp the dialog cannot collect, or a
     * {@code default:} - the record it would default from is the one it writes.
     */
    @Test
    void aPromptForWhatNoPersonAnswersIsRefused() {
        assertPromptRefused("{ field: reason }",
                "transition [VoidInvoice] prompt field [reason] is not a field or to-one relation of [Invoice]");
        assertPromptRefused("{ field: Status }", "transition [VoidInvoice] prompt field [Status] is the status the transition writes");
        assertPromptRefused("{ field: number }", "transition [VoidInvoice] prompt field [number] is written by the platform");
        assertPromptRefused("{ field: voidedOn }", "transition [VoidInvoice] prompt field [voidedOn] is written by the platform");
        assertPromptRefused("{ field: voidedAt }", "transition [VoidInvoice] prompt field [voidedAt] has type timestamp");
        assertPromptRefused("{ field: voidReason, default: now }", "transition [VoidInvoice] prompt field [voidReason] declares default:");
        assertPromptRefused("{ field: ReplacedBy }", "transition [VoidInvoice] prompt names [ReplacedBy] more than once");
    }

    private static void assertPromptRefused(String entry, String issue) {
        String yaml = PROMPT.replace("      - { field: ReplacedBy }\n", "      - { field: ReplacedBy }\n      - " + entry + "\n");
        IntentValidationException ex = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(i -> i.contains(issue)),
                "got: " + ex.getIssues());
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
