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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The period lock of a posting's TARGET, carried onto the posting (issue #7703).
 *
 * <p>
 * The lock is enforced in the target's own repository (#7590), but a posting runs on a listener -
 * after its source committed the transition that raised it - so a closed period dropped the post
 * where nobody was listening: the issuer saw a 200 and the document stayed unposted for good,
 * because reopening the period publishes on the REGISTER's topic. The register is an entity of the
 * target's own model, so every fact needed to ask it before writing, and to sweep the refused
 * documents when it reopens, travels with the posting itself.
 */
class GluePostingPeriodTest {

    private static final String YAML = """
            name: ledger
            entities:
              - name: PeriodStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: InvoiceStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: AccountingPeriod
                period:
                  start: startDate
                  end: endDate
                  closedWhen: "Status == CLOSED"
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: startDate, type: date }
                  - { name: endDate,   type: date }
                relations:
                  - { name: Status, kind: manyToOne, to: PeriodStatus, function: EntityStatus, init: OPEN }
              - name: SalesInvoice
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: date, type: date }
                  - { name: net,  type: decimal }
                relations:
                  - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
              - name: JournalEntry
                immutableInPeriod: { period: AccountingPeriod, date: entryDate }
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: entryDate, type: date }
                relations:
                  - { name: SalesInvoice, kind: manyToOne, to: SalesInvoice }
              - name: JournalEntryItem
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: debit,  type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: JournalEntry, kind: manyToOne, to: JournalEntry, composition: true, required: true }
            seeds:
              - name: period-statuses
                entity: PeriodStatus
                rows:
                  - { id: 1, name: OPEN }
                  - { id: 2, name: CLOSED }
              - name: invoice-statuses
                entity: InvoiceStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: ISSUED }
            postings:
              - name: salesInvoicePosting
                event: { onTransition: SalesInvoice, when: "Status == 2" }
                creates: JournalEntry
                backReference: SalesInvoice
                map: { entryDate: date }
                items:
                  - { debit: "Net" }
            """;

    @Test
    void thePostingCarriesTheRegisterItsTargetIsLockedBy() {
        Map<String, Object> posting = GlueIntentGenerator.buildPostingsForTest(IntentParser.parse(YAML))
                                                         .get(0);

        assertEquals("AccountingPeriod", posting.get("periodRegisterEntity"));
        assertEquals("StartDate", posting.get("periodStartProperty"));
        assertEquals("EndDate", posting.get("periodEndProperty"));
        assertEquals("Status", posting.get("periodStatusProperty"));
        assertEquals("2", posting.get("periodClosedValues"), "the seeded CLOSED name, resolved to its id");
        assertEquals("EntryDate", posting.get("periodDateProperty"), "the TARGET column the lock tests");
    }

    /**
     * The one thing the sweep cannot derive at runtime: which SOURCE column the locked date is copied
     * from. With it, the sweep asks only for the documents the reopened period covers.
     */
    @Test
    void aPlainMapCellNamesTheSourceColumnTheLockedDateComesFrom() {
        Map<String, Object> posting = GlueIntentGenerator.buildPostingsForTest(IntentParser.parse(YAML))
                                                         .get(0);

        assertEquals("Date", posting.get("periodSourceDateProperty"));
    }

    /**
     * An expression cannot be pushed into a query, so the sweep falls back to the status guard alone.
     */
    @Test
    void anExpressionLeavesTheSourceColumnUnnamed() {
        String yaml = YAML.replace("map: { entryDate: date }", "map: { entryDate: \"{date}\" }");
        Map<String, Object> posting = GlueIntentGenerator.buildPostingsForTest(IntentParser.parse(yaml))
                                                         .get(0);

        assertEquals("AccountingPeriod", posting.get("periodRegisterEntity"), "the lock still applies");
        assertEquals("", posting.get("periodSourceDateProperty"));
    }

    @Test
    void aPostingWhoseTargetCarriesNoLockIsUnchanged() {
        // the text block strips its common indent, so the replacement is the STRIPPED line
        String yaml = YAML.replace("    immutableInPeriod: { period: AccountingPeriod, date: entryDate }\n", "");
        Map<String, Object> posting = GlueIntentGenerator.buildPostingsForTest(IntentParser.parse(yaml))
                                                         .get(0);

        assertNull(posting.get("periodRegisterEntity"), "an unlocked target must generate byte-identically");
        assertTrue(GlueIntentGenerator.buildPostingReopensForTest(IntentParser.parse(yaml))
                                      .isEmpty(),
                "and contribute no sweep");
    }

    @Test
    void aLockedTargetRendersOneSweepPerPosting() {
        List<Map<String, Object>> reopens = GlueIntentGenerator.buildPostingReopensForTest(IntentParser.parse(YAML));

        // One per channel a register's status moves on: the two are disjoint, and a period is
        // legitimately reopened by a transitions: button or by a plain REST update.
        assertEquals(2, reopens.size());
        assertEquals("SalesInvoicePostingPostingReopen", reopens.get(0)
                                                                .get("reopenClassName"));
        assertEquals("-transitioned", reopens.get(0)
                                             .get("reopenTopicSuffix"));
        assertEquals("SalesInvoicePostingPostingReopenOnUpdate", reopens.get(1)
                                                                        .get("reopenClassName"));
        assertEquals("-updated", reopens.get(1)
                                        .get("reopenTopicSuffix"));
        assertEquals("SalesInvoicePosting", reopens.get(0)
                                                   .get("className"),
                "the sweep renders from the posting it re-runs, so the two cannot disagree");
    }
}
