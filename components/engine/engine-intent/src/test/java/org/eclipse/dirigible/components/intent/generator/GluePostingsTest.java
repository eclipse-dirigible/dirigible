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
              - { model: sales-invoices }
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
                  - { name: SalesInvoice, kind: manyToOne, to: SalesInvoice, model: sales-invoices }
              - name: JournalEntryItem
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: debit, type: decimal, precision: 18, scale: 2 }
                  - { name: credit, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: JournalEntry, kind: manyToOne, to: JournalEntry, composition: true, required: true }
                  - { name: Account, kind: manyToOne, to: Account, required: true }
                  - { name: Customer, kind: manyToOne, to: Customer, model: sales-invoices }
            postings:
              - name: salesInvoicePosting
                event: { onTransition: SalesInvoice, model: sales-invoices, when: "Status == 3" }
                creates: JournalEntry
                backReference: SalesInvoice
                map: { entryDate: date, reason: "Sales invoice {number}" }
                rule: { entity: PostingRule, match: { documentType: "Sales Invoice" } }
                items:
                  - { Account: rule(receivableAccount), debit: "Net + Vat", Customer: Customer }
                  - { Account: rule(revenueAccount), credit: "Net" }
                  - { Account: rule(vatAccount), credit: "Vat", when: "Vat != 0" }
            """;

    @Test
    void postingGlueIsFullyPreRendered() {
        List<Map<String, Object>> postings = GlueIntentGenerator.buildPostingsForTest(IntentParser.parse(YAML));
        assertEquals(1, postings.size());
        Map<String, Object> p = postings.get(0);
        assertEquals("SalesInvoicePosting", p.get("className"));
        assertEquals(false, p.get("isCreate"));
        assertEquals(true, p.get("crossModel"));
        assertEquals("sales-invoices", p.get("sourceProject"));
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
        // source-FK copy (#6533): a to-one relation item cell copies the source FK verbatim - no Calc.
        assertTrue(firstAssigns.stream()
                               .anyMatch(a -> "Customer".equals(a.get("targetProp")) && "source.Customer".equals(a.get("expr"))),
                "a to-one relation item cell must pre-render as a source-FK copy");
        // the third row carries a null-safe Calc guard
        assertEquals("Calc.eval(\"Vat\", source, 6).compareTo(new java.math.BigDecimal(\"0\")) != 0", rows.get(2)
                                                                                                          .get("guard"));
        assertEquals(List.of("ReceivableAccount", "RevenueAccount", "VatAccount"), p.get("usedRuleColumns"));
    }

    /**
     * Conditional rule column (#6534): {@code rule(by: Method, cases: {...}, default: ...)} pre-renders
     * a null-safe classifier ternary over the rule row's columns - NOT static usedRuleColumns - and
     * registers the whole expression as a null guard so an undetermined account skips the posting.
     */
    @SuppressWarnings("unchecked")
    @Test
    void conditionalRuleColumnEmitsAClassifierTernaryAndAGuard() {
        String yaml =
                """
                        name: ledger
                        entities:
                          - name: Account
                            fields:
                              - { name: id, type: integer, primaryKey: true, generated: true }
                              - { name: number, type: string }
                          - name: PaymentMethodType
                            kind: setting
                            fields:
                              - { name: id, type: integer, primaryKey: true, generated: true }
                              - { name: name, type: string }
                          - name: PostingRule
                            kind: setting
                            fields:
                              - { name: id, type: integer, primaryKey: true, generated: true }
                              - { name: documentType, type: string }
                            relations:
                              - { name: BankAccount, kind: manyToOne, to: Account }
                              - { name: CashAccount, kind: manyToOne, to: Account }
                              - { name: SuspenseAccount, kind: manyToOne, to: Account }
                          - name: Payment
                            fields:
                              - { name: id, type: integer, primaryKey: true, generated: true }
                              - { name: amount, type: decimal, precision: 18, scale: 2 }
                            relations:
                              - { name: Method, kind: manyToOne, to: PaymentMethodType, required: true }
                          - name: JournalEntry
                            fields:
                              - { name: id, type: integer, primaryKey: true, generated: true }
                            relations:
                              - { name: Payment, kind: manyToOne, to: Payment }
                          - name: JournalEntryItem
                            fields:
                              - { name: id, type: integer, primaryKey: true, generated: true }
                              - { name: debit, type: decimal, precision: 18, scale: 2 }
                            relations:
                              - { name: JournalEntry, kind: manyToOne, to: JournalEntry, composition: true, required: true }
                              - { name: Account, kind: manyToOne, to: Account, required: true }
                        postings:
                          - name: paymentPosting
                            event: { onCreate: Payment }
                            creates: JournalEntry
                            backReference: Payment
                            rule: { entity: PostingRule, match: { documentType: "Payment" } }
                            items:
                              - { Account: "rule(by: Method, cases: { 1: BankAccount, 2: CashAccount }, default: SuspenseAccount)", debit: "Amount" }
                        """;
        Map<String, Object> p = GlueIntentGenerator.buildPostingsForTest(IntentParser.parse(yaml))
                                                   .get(0);
        String ternary = "(Calc.eval(\"Method\", source, 6).compareTo(new java.math.BigDecimal(\"1\")) == 0 ? ruleRow.BankAccount"
                + " : Calc.eval(\"Method\", source, 6).compareTo(new java.math.BigDecimal(\"2\")) == 0 ? ruleRow.CashAccount"
                + " : ruleRow.SuspenseAccount)";

        List<Map<String, Object>> assigns = (List<Map<String, Object>>) ((List<Map<String, Object>>) p.get("itemRows")).get(0)
                                                                                                                       .get("assigns");
        assertTrue(assigns.stream()
                          .anyMatch(a -> "Account".equals(a.get("targetProp")) && ternary.equals(a.get("expr"))),
                "the Account cell must be the classifier ternary: " + assigns);
        // the whole expression is a runtime null guard, NOT a static rule column
        assertEquals(List.of(ternary), p.get("conditionalRuleGuards"));
        assertEquals(List.of(), p.get("usedRuleColumns"));
    }

    /**
     * The onCreate trigger (#6421): a source with no status lifecycle - a booked payment - posts on its
     * INSERT. The glue flags the create event and carries no status guard.
     */
    @Test
    void onCreatePostingBindsTheCreateEventWithoutAGuard() {
        String yaml = """
                name: ledger
                uses:
                  - { model: payments }
                entities:
                  - name: JournalEntry
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: reason, type: string, length: 400 }
                    relations:
                      - { name: Payment, kind: manyToOne, to: Payment, model: payments }
                  - name: JournalEntryItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: debit, type: decimal, precision: 18, scale: 2 }
                    relations:
                      - { name: JournalEntry, kind: manyToOne, to: JournalEntry, composition: true, required: true }
                postings:
                  - name: paymentPosting
                    event: { onCreate: Payment, model: payments }
                    creates: JournalEntry
                    backReference: Payment
                    map: { reason: "Payment {number}" }
                    items:
                      - { debit: "Amount" }
                """;
        List<Map<String, Object>> postings = GlueIntentGenerator.buildPostingsForTest(IntentParser.parse(yaml));
        assertEquals(1, postings.size());
        Map<String, Object> p = postings.get(0);
        assertEquals("PaymentPosting", p.get("className"));
        assertEquals(true, p.get("isCreate"));
        assertEquals("Payment", p.get("sourceEntity"));
        assertEquals("", p.get("guardProperty"));
        assertEquals("", p.get("guardValue"));
    }
}
