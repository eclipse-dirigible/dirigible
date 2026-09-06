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

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The amendment half of the postings glue (#7071): what an existing post is compared against when
 * the source reaches the moment again, and how far the created document's own lifecycle lets that
 * post be rewritten.
 */
class GluePostingsAmendTest {

    /** The created document's own status lifecycle, spliced into the target entity's relations. */
    private static final String WITH_STATUS =
            "      - { name: Status, kind: manyToOne, to: JournalEntryStatus, function: EntityStatus, init: 1 }";

    private static String yaml(String journalEntryStatus) {
        return """
                name: ledger
                entities:
                  - name: Invoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: net, type: decimal, precision: 18, scale: 2 }
                      - { name: vat, type: decimal, precision: 18, scale: 2 }
                    relations:
                      - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
                  - name: InvoiceStatus
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: Account
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: number, type: string }
                  - name: PostingRule
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: documentType, type: string }
                    relations:
                      - { name: ReceivableAccount, kind: manyToOne, to: Account }
                      - { name: RevenueAccount, kind: manyToOne, to: Account }
                  - name: JournalEntryStatus
                    kind: setting
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: JournalEntry
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: reason, type: string, length: 400 }
                    relations:
                      - { name: Invoice, kind: manyToOne, to: Invoice }
                %s
                  - name: JournalEntryItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: debit, type: decimal, precision: 18, scale: 2 }
                      - { name: credit, type: decimal, precision: 18, scale: 2 }
                    relations:
                      - { name: JournalEntry, kind: manyToOne, to: JournalEntry, composition: true, required: true }
                      - { name: Account, kind: manyToOne, to: Account, required: true }
                postings:
                  - name: invoicePosting
                    event: { onTransition: Invoice, when: "Status == 3" }
                    creates: JournalEntry
                    backReference: Invoice
                    map: { reason: "Invoice {id}" }
                    rule: { entity: PostingRule, match: { documentType: "Invoice" } }
                    items:
                      - { Account: rule(receivableAccount), debit: "Net + Vat" }
                      - { Account: rule(revenueAccount), credit: "Net" }
                """.formatted(journalEntryStatus);
    }

    private static Map<String, Object> posting(String journalEntryStatus) {
        List<Map<String, Object>> postings = GlueIntentGenerator.buildPostingsForTest(IntentParser.parse(yaml(journalEntryStatus)));
        assertEquals(1, postings.size());
        return postings.get(0);
    }

    @Test
    void comparedPropertiesAreTheUnionOfEveryAssignedItemCell() {
        // What a stored row is compared on: every cell any row writes, and nothing else - a property
        // no row assigns is null on both sides and could only ever say "unchanged".
        assertEquals(List.of("Account", "Debit", "Credit"), posting(WITH_STATUS).get("itemComparedProps"));
    }

    @Test
    void aTargetCarryingAStatusIsRewritableOnlyWhileItStillHoldsTheStatusItWasCreatedIn() {
        assertEquals("target.Status != null && target.Status == 1", posting(WITH_STATUS).get("amendableGuard"));
    }

    @Test
    void aTargetWithNoStatusLifecycleIsAlwaysRewritable() {
        // Nothing to act on, so nothing to protect: the post always follows the source.
        assertEquals("", posting("").get("amendableGuard"));
    }
}
