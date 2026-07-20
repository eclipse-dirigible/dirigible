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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The reversal (red storno) posting glue: the reversal inherits the reversed sibling's
 * creates/backReference/rule/map/items, negates every item amount expression on the SAME side, and
 * both entries carry the storno coordinates their handlers' idempotency guards discriminate by.
 */
class GluePostingsReversesTest {

    private static final String YAML = """
            name: ledger
            uses:
              - { model: acme-billing }
            entities:
              - name: Account
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
              - name: JournalEntry
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: entryDate, type: date }
                relations:
                  - { name: Invoice, kind: manyToOne, to: Invoice, model: acme-billing }
                  - { name: Storno, kind: manyToOne, to: JournalEntry }
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
                event: { onTransition: Invoice, model: acme-billing, when: "Status == 3" }
                creates: JournalEntry
                backReference: Invoice
                map: { entryDate: date }
                rule: { entity: PostingRule, match: { documentType: "Invoice" } }
                items:
                  - { Account: rule(receivableAccount), debit: "Net + Vat" }
                  - { Account: rule(revenueAccount), credit: "Net + Vat" }
              - name: invoiceStorno
                event: { onTransition: Invoice, model: acme-billing, when: "Status == 8" }
                reverses: invoicePosting
                storno: Storno
            """;

    @SuppressWarnings("unchecked")
    @Test
    void reversalInheritsTheSiblingAndNegatesTheAmounts() {
        List<Map<String, Object>> postings = GlueIntentGenerator.buildPostingsForTest(IntentParser.parse(YAML));
        assertEquals(2, postings.size());

        Map<String, Object> base = postings.get(0);
        assertEquals("InvoicePosting", base.get("className"));
        // The reversed sibling's guard must exclude reversal rows (they share its back-reference).
        assertEquals("", base.get("stornoProperty"));
        assertEquals("Storno", base.get("stornoFilterProperty"));

        Map<String, Object> storno = postings.get(1);
        assertEquals("InvoiceStorno", storno.get("className"));
        assertEquals("8", storno.get("guardValue"));
        // Inherited coordinates.
        assertEquals("JournalEntry", storno.get("targetEntity"));
        assertEquals("JournalEntryItem", storno.get("itemsEntity"));
        assertEquals("Invoice", storno.get("backRefProperty"));
        assertEquals("\"Invoice\"", storno.get("ruleMatchValueJava"));
        assertEquals("Storno", storno.get("stornoProperty"));
        assertEquals("", storno.get("stornoFilterProperty"));

        // The header is the sibling's map; the amounts are the sibling's expressions NEGATED on the
        // SAME side (red storno - never swapped sides).
        List<Map<String, Object>> header = (List<Map<String, Object>>) storno.get("headerAssignments");
        assertTrue(header.stream()
                         .anyMatch(a -> "EntryDate".equals(a.get("targetProp")) && "source.Date".equals(a.get("expr"))));
        List<Map<String, Object>> rows = (List<Map<String, Object>>) storno.get("itemRows");
        assertEquals(2, rows.size());
        List<Map<String, Object>> firstAssigns = (List<Map<String, Object>>) rows.get(0)
                                                                                 .get("assigns");
        assertTrue(firstAssigns.stream()
                               .anyMatch(a -> "Account".equals(a.get("targetProp")) && "ruleRow.ReceivableAccount".equals(a.get("expr"))));
        assertTrue(firstAssigns.stream()
                               .anyMatch(a -> "Debit".equals(a.get("targetProp"))
                                       && "Calc.eval(\"-(Net + Vat)\", source, 2)".equals(a.get("expr"))));
    }
}
