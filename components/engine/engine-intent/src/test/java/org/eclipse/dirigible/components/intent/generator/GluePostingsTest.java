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

/** The postings glue: everything the shape-only template needs, pre-rendered. */
class GluePostingsTest {

    private static final String YAML = """
            name: ledger
            uses:
              - { model: kf-mod-sales-invoices }
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
                  - { name: VatAccount, kind: manyToOne, to: Account }
              - name: JournalEntry
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: entryDate, type: date }
                  - { name: reason, type: string, length: 400 }
                relations:
                  - { name: SalesInvoice, kind: manyToOne, to: SalesInvoice, model: kf-mod-sales-invoices }
              - name: JournalEntryItem
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: debit, type: decimal, precision: 18, scale: 2 }
                  - { name: credit, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: JournalEntry, kind: manyToOne, to: JournalEntry, composition: true, required: true }
                  - { name: Account, kind: manyToOne, to: Account, required: true }
            postings:
              - name: salesInvoicePosting
                event: { onTransition: SalesInvoice, model: kf-mod-sales-invoices, when: "Status == 3" }
                creates: JournalEntry
                backReference: SalesInvoice
                map: { entryDate: date, reason: "Sales invoice {number}" }
                rule: { entity: PostingRule, match: { documentType: "Sales Invoice" } }
                items:
                  - { Account: rule(receivableAccount), debit: "Net + Vat" }
                  - { Account: rule(revenueAccount), credit: "Net" }
                  - { Account: rule(vatAccount), credit: "Vat", when: "Vat != 0" }
            """;

    @Test
    void postingGlueIsFullyPreRendered() {
        List<Map<String, Object>> postings = GlueIntentGenerator.buildPostingsForTest(IntentParser.parse(YAML));
        assertEquals(1, postings.size());
        Map<String, Object> p = postings.get(0);
        assertEquals("SalesInvoicePosting", p.get("className"));
        assertEquals(true, p.get("crossModel"));
        assertEquals("kf-mod-sales-invoices", p.get("sourceProject"));
        assertEquals("SalesInvoice", p.get("sourceEntity"));
        assertEquals("Status", p.get("guardProperty"));
        assertEquals("3", p.get("guardValue"));
        assertEquals("JournalEntry", p.get("targetEntity"));
        assertEquals("JournalEntryItem", p.get("itemsEntity"));
        assertEquals("JournalEntry", p.get("itemsFk"));
        assertEquals("SalesInvoice", p.get("backRefProperty"));
        assertEquals("DocumentType", p.get("ruleMatchProperty"));
        assertEquals("\"Sales Invoice\"", p.get("ruleMatchValueJava"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> header = (List<Map<String, Object>>) p.get("headerAssignments");
        // copy + {placeholder} template, pre-rendered as Java expressions
        assertTrue(header.stream()
                         .anyMatch(a -> "EntryDate".equals(a.get("targetProp")) && "source.Date".equals(a.get("expr"))));
        assertTrue(header.stream()
                         .anyMatch(
                                 a -> "Reason".equals(a.get("targetProp")) && "\"Sales invoice \" + source.Number".equals(a.get("expr"))));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) p.get("itemRows");
        assertEquals(3, rows.size());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> firstAssigns = (List<Map<String, Object>>) rows.get(0)
                                                                                 .get("assigns");
        assertTrue(firstAssigns.stream()
                               .anyMatch(a -> "Account".equals(a.get("targetProp")) && "ruleRow.ReceivableAccount".equals(a.get("expr"))));
        assertTrue(firstAssigns.stream()
                               .anyMatch(a -> "Debit".equals(a.get("targetProp"))
                                       && "Calc.eval(\"Net + Vat\", source, 2)".equals(a.get("expr"))));
        // the third row carries a null-safe Calc guard
        assertEquals("Calc.eval(\"Vat\", source, 6).compareTo(new java.math.BigDecimal(\"0\")) != 0", rows.get(2)
                                                                                                          .get("guard"));
        assertEquals(List.of("ReceivableAccount", "RevenueAccount", "VatAccount"), p.get("usedRuleColumns"));
    }
}
