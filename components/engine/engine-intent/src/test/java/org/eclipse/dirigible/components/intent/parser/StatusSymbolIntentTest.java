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
import org.junit.jupiter.api.Test;

/**
 * Statuses referenced by their seeded NAME resolve to the seed id at every site that names a
 * status, so a guard can no longer be silently retargeted by inserting a status mid-nomenclature
 * and a typo fails instead of quietly meaning another status (dirigible #6645, part 4).
 */
class StatusSymbolIntentTest {

    private static final String YAML = """
            name: billing
            entities:
              - name: InvoiceStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Invoice
                immutableWhen: "Status == ISSUED"
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, documentTitle: true }
                  - { name: paid, type: decimal }
                relations:
                  - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: DRAFT }
            processes:
              - name: InvoiceApproval
                trigger: { onCreate: Invoice }
                abortOn: { status: [VOIDED], then: end }
                steps:
                  - { name: approve, kind: userTask, args: { assignee: manager, form: ApproveInvoice } }
                  - { name: issue, kind: serviceTask, args: { setRelationField: Status, value: ISSUED } }
                  - { name: end, kind: end }
            forms:
              - { name: ApproveInvoice, forEntity: Invoice, fields: [number], actions: [approve] }
            transitions:
              - name: VoidInvoice
                forEntity: Invoice
                from: [ISSUED]
                setStatus: VOIDED
                when: "Paid == 0"
            reports:
              - name: OpenInvoices
                source: Invoice
                filter: "Status != VOIDED"
                measures: ["sum(paid)"]
            notifications:
              - name: issued-mail
                event: { onUpdate: Invoice, when: "Status == ISSUED" }
                to: ops@example.com
                subject: "Invoice {number} issued"
                body: "The invoice has been issued."
            schedules:
              - name: dunning
                cron: "0 0 8 * * ?"
                entity: Invoice
                where:
                  - { field: Status, op: eq, value: ISSUED }
                notify: { to: ops@example.com, subject: "Invoice overdue" }
            seeds:
              - name: invoice-statuses
                entity: InvoiceStatus
                rows:
                  - { id: 1, name: DRAFT, stage: draft }
                  - { id: 3, name: ISSUED, stage: live }
                  - { id: 9, name: VOIDED, stage: void }
            """;

    @Test
    void everySiteResolvesTheNameToItsSeedId() {
        IntentModel model = IntentParser.parse(YAML);
        assertEquals(List.of(3), model.getTransitions()
                                      .get(0)
                                      .getFrom(),
                "transition from");
        assertEquals(Integer.valueOf(9), model.getTransitions()
                                              .get(0)
                                              .getSetStatus(),
                "transition setStatus");
        assertEquals("Status == 3", model.getEntities()
                                         .get(1)
                                         .getImmutableWhen(),
                "immutableWhen");
        assertEquals("1", model.getEntities()
                               .get(1)
                               .getRelations()
                               .get(0)
                               .getInit(),
                "relation init");
        assertEquals("[9]", String.valueOf(model.getProcesses()
                                                .get(0)
                                                .getAbortOn()
                                                .get("status")),
                "abortOn status");
        assertEquals("3", String.valueOf(model.getProcesses()
                                              .get(0)
                                              .getSteps()
                                              .get(1)
                                              .getArgs()
                                              .get("value")),
                "setRelationField value");
        assertEquals("Status != 9", model.getReports()
                                         .get(0)
                                         .getFilter(),
                "report filter");
        assertEquals("3", String.valueOf(model.getSchedules()
                                              .get(0)
                                              .getWhere()
                                              .get(0)
                                              .getValue()),
                "schedule where status");
        assertEquals("Status == 3", String.valueOf(model.getNotifications()
                                                        .get(0)
                                                        .getEvent()
                                                        .get("when")),
                "notification event when");
    }

    /**
     * The {@code event.when} of the three declarative glue lists (issue #7289) - the guard that
     * qualifies the moment a mail goes out, a record is forwarded or a departure leaves. Left
     * unresolved, the name reached the generated listener as a string compared with the integer status
     * FK, so the guard could never hold and the message was never sent.
     */
    @Test
    void anUnknownStatusNameInAGlueEventGuardIsRejected() {
        assertIssue(YAML.replace("event: { onUpdate: Invoice, when: \"Status == ISSUED\" }",
                "event: { onUpdate: Invoice, when: \"Status == ISUED\" }"), "not a seeded status of [InvoiceStatus]");
    }

    /**
     * The row query of a cron schedule (issue #7251) - the site a status guard is written at most
     * often, and the one left behind when the sibling {@code items: where:} gained the rewrite: a name
     * there generated {@code .eq("Status", "OVERDUE")} into the job and matched nothing forever.
     */
    @Test
    void anUnknownStatusNameInAScheduleQueryIsRejected() {
        assertIssue(YAML.replace("field: Status, op: eq, value: ISSUED", "field: Status, op: eq, value: ISUED"),
                "not a seeded status of [InvoiceStatus]");
    }

    /** The point of the exercise: a mistyped status is a parse error, not another status. */
    @Test
    void anUnknownStatusNameIsRejected() {
        assertIssue(YAML.replace("setStatus: VOIDED", "setStatus: VOIDEED"), "not a seeded status of [InvoiceStatus]");
    }

    @Test
    void anUnknownStatusNameInAFilterIsRejected() {
        assertIssue(YAML.replace("Status != VOIDED", "Status != CANCELLED"), "not a seeded status of [InvoiceStatus]");
    }

    /** Names have no ordering, so the magic-number range idiom cannot be written symbolically. */
    @Test
    void anOrderingComparisonAgainstANameIsRejected() {
        assertIssue(YAML.replace("Status != VOIDED", "Status >= ISSUED"), "a status name has no ordering");
    }

    /** A cross-model nomenclature is seeded in its owner model; nothing here can resolve the name. */
    @Test
    void aCrossModelStatusNameIsRejected() {
        String yaml = """
                name: billing
                uses:
                  - { model: nomenclatures }
                entities:
                  - name: Invoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: paid, type: decimal }
                    relations:
                      - { name: Status, kind: manyToOne, to: InvoiceStatus, model: nomenclatures, function: EntityStatus }
                reports:
                  - name: OpenInvoices
                    source: Invoice
                    filter: "Status != VOIDED"
                    measures: ["sum(paid)"]
                """;
        assertIssue(yaml, "must be referenced by its numeric seed id");
    }

    /**
     * A posting's status guard is the enforcement-bearing site the motivating ledger bug lived in. Its
     * source is typically cross-model, so a name there cannot resolve - and must say so rather than
     * reach validation as a malformed guard.
     */
    @Test
    void aCrossModelPostingGuardCannotNameItsStatus() {
        String yaml = """
                name: ledger
                uses:
                  - { model: sales-invoices }
                entities:
                  - name: JournalEntry
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                postings:
                  - name: salesInvoiceStorno
                    event: { onTransition: SalesInvoice, model: sales-invoices, when: "Status == VOIDED" }
                    creates: JournalEntry
                    backReference: SalesInvoice
                """;
        assertIssue(yaml, "must be referenced by its numeric seed id");
    }

    /** Numeric ids keep working unchanged - this is additive, not a migration. */
    @Test
    void numericIdsAreLeftAlone() {
        IntentModel model = IntentParser.parse(YAML.replace("from: [ISSUED]", "from: [3]")
                                                   .replace("setStatus: VOIDED", "setStatus: 9")
                                                   .replace("Status != VOIDED", "Status != 9")
                                                   .replace("Status == ISSUED", "Status == 3")
                                                   .replace("init: DRAFT", "init: 1")
                                                   .replace("status: [VOIDED]", "status: [9]")
                                                   .replace("value: ISSUED", "value: 3"));
        assertEquals(List.of(3), model.getTransitions()
                                      .get(0)
                                      .getFrom());
        assertEquals("Status != 9", model.getReports()
                                         .get(0)
                                         .getFilter());
    }

    /**
     * The items of a cross-model source ({@code fromUses:}) are seeded in the owner model, so a status
     * NAME in their source-row rule is left in the numeric-id form for the generator to refuse against
     * the owner's {@code .model} (dirigible #7225) - and a LOCAL entity that merely shares the item's
     * name must not lend its own nomenclature to it: that id is positional in the wrong seed list.
     */
    @Test
    void aCrossModelItemSourceIsNotResolvedAgainstASameNamedLocalEntity() {
        String yaml = """
                name: delivery-notes
                uses:
                  - { model: inventory }
                entities:
                  - name: LineStatus
                    function: Setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: GoodsIssueItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: quantity, type: decimal }
                    relations:
                      - { name: Status, kind: manyToOne, to: LineStatus, function: EntityStatus, init: 1 }
                  - name: DeliveryNote
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string, documentTitle: true }
                  - name: DeliveryNoteItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: quantity, type: decimal }
                    relations:
                      - { name: DeliveryNote, kind: manyToOne, to: DeliveryNote, composition: true, required: true }
                generates:
                  - name: delivery-note-from-goods-issue
                    from: GoodsIssue
                    fromUses: inventory
                    to: DeliveryNote
                    forEntity: GoodsIssue
                    map:
                      Number: number
                    items:
                      from: GoodsIssueItem
                      to: DeliveryNoteItem
                      where:
                        - { field: Status, op: eq, value: APPROVED }
                      map:
                        Quantity: quantity
                seeds:
                  - name: line-statuses
                    entity: LineStatus
                    rows:
                      - { id: 1, name: DRAFT }
                      - { id: 2, name: APPROVED }
                """;
        IntentModel model = IntentParser.parse(yaml);
        assertEquals("APPROVED", model.getGenerates()
                                      .get(0)
                                      .getItems()
                                      .getWhere()
                                      .get(0)
                                      .getValue(),
                "the local LineStatus seed id 2 must not be taken for the inventory model's APPROVED");
    }

    private static void assertIssue(String yaml, String expected) {
        IntentValidationException thrown = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(thrown.getIssues()
                         .stream()
                         .anyMatch(issue -> issue.contains(expected)),
                "expected an issue containing [" + expected + "] but got " + thrown.getIssues());
    }
}
