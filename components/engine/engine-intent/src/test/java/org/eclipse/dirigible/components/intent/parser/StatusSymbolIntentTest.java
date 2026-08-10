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

    private static void assertIssue(String yaml, String expected) {
        IntentValidationException thrown = assertThrows(IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(thrown.getIssues()
                         .stream()
                         .anyMatch(issue -> issue.contains(expected)),
                "expected an issue containing [" + expected + "] but got " + thrown.getIssues());
    }
}
