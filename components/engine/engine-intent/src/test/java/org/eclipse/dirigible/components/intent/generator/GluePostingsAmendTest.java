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
 *
 * <p>
 * The comparison is over the values as they will be STORED, not as the derived rows stand (#7131):
 * the repository applies each column's authored default before the insert (#7104/#7115), so a
 * defaulted column read back off a stored row is not a change - and every header expression is
 * evaluated once, into a local both the comparison and the assignment read.
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
                      - { name: issueDate, type: date }
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
                      - { name: debit, type: decimal, precision: 18, scale: 2, defaultValue: 0 }
                      - { name: credit, type: decimal, precision: 18, scale: 2, defaultValue: 0 }
                    relations:
                      - { name: JournalEntry, kind: manyToOne, to: JournalEntry, composition: true, required: true }
                      - { name: Account, kind: manyToOne, to: Account, required: true }
                seeds:
                  - name: journalEntryStatuses
                    entity: JournalEntryStatus
                    rows:
                      - { id: 1, name: Draft }
                      - { id: 2, name: Posted }
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
        return postingOf(yaml(journalEntryStatus));
    }

    private static Map<String, Object> postingOf(String yaml) {
        List<Map<String, Object>> postings = GlueIntentGenerator.buildPostingsForTest(IntentParser.parse(yaml));
        assertEquals(1, postings.size());
        return postings.get(0);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> comparedProperties(Map<String, Object> posting) {
        return (List<Map<String, Object>>) posting.get("itemComparedProps");
    }

    private static Map<String, Object> comparedProperty(Map<String, Object> posting, String property) {
        for (Map<String, Object> compared : comparedProperties(posting)) {
            if (property.equals(compared.get("name"))) {
                return compared;
            }
        }
        throw new AssertionError(property + " is not compared");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> headerAssignments(Map<String, Object> posting) {
        return (List<Map<String, Object>>) posting.get("headerAssignments");
    }

    @Test
    void comparedPropertiesAreTheUnionOfEveryAssignedItemCell() {
        // What a stored row is compared on: every cell any row writes, and nothing else - a property
        // no row assigns is null on the derived side and says nothing about the stored one.
        assertEquals(List.of("Account", "Debit", "Credit"), comparedProperties(posting(WITH_STATUS)).stream()
                                                                                                    .map(compared -> compared.get("name"))
                                                                                                    .toList());
    }

    @Test
    void aComparedColumnCarryingADefaultIsComparedAgainstThatDefault() {
        // The regression this pins (#7131): the debit row assigns Debit and the credit row Credit,
        // both `default: 0`. A stored credit row reads Debit = 0 back - save() applies the default
        // before the insert (#7104/#7115) - while the derived one still holds null, so comparing the
        // two raw made `unchanged` unreachable and EVERY redelivery rewrote the whole post.
        Map<String, Object> posting = posting(WITH_STATUS);
        assertEquals("new java.math.BigDecimal(\"0\")", comparedProperty(posting, "Debit").get("derivedDefault"));
        assertEquals("new java.math.BigDecimal(\"0\")", comparedProperty(posting, "Credit").get("derivedDefault"));
        assertEquals(Boolean.FALSE, comparedProperty(posting, "Debit").get("expressionDefault"));
    }

    @Test
    void aComparedColumnWithNoDefaultIsComparedAsItStands() {
        // Nothing to apply: a null on the derived side genuinely means the column is left empty.
        Map<String, Object> posting = posting(WITH_STATUS);
        assertEquals("", comparedProperty(posting, "Account").get("derivedDefault"));
        assertEquals(Boolean.FALSE, comparedProperty(posting, "Account").get("expressionDefault"));
    }

    @Test
    void aDefaultOnlyTheDatabaseCanApplyIsReportedAsAnExpression() {
        // A date column's DEFAULT reaches the DDL verbatim as a SQL expression (CURRENT_DATE) with no
        // Java stand-in - the DAO's #applyDefaults() skips it too, so the DATABASE fills it. What the
        // stored row holds cannot be derived at all, and the template compares the column only for the
        // rows that do assign it.
        Map<String, Object> posting =
                postingOf(yaml(WITH_STATUS)
                                           .replace("      - { name: credit, type: decimal, precision: 18, scale: 2, defaultValue: 0 }",
                                                   "      - { name: credit, type: decimal, precision: 18, scale: 2, defaultValue: 0 }\n"
                                                           + "      - { name: valueDate, type: date, defaultValue: CURRENT_DATE }")
                                           .replace("- { Account: rule(revenueAccount), credit: \"Net\" }",
                                                   "- { Account: rule(revenueAccount), credit: \"Net\", valueDate: \"IssueDate\" }"));
        assertEquals(Boolean.TRUE, comparedProperty(posting, "ValueDate").get("expressionDefault"));
        assertEquals("", comparedProperty(posting, "ValueDate").get("derivedDefault"));
    }

    @Test
    void eachHeaderExpressionIsEvaluatedIntoOneNumberedLocal() {
        // Evaluated ONCE: the comparison and the assignment must read the same value, or an expression
        // reading the clock would make every redelivery look like an amendment (#7131).
        List<Map<String, Object>> header = headerAssignments(posting(WITH_STATUS));
        assertEquals(1, header.size());
        assertEquals("Reason", header.get(0)
                                     .get("targetProp"));
        assertEquals("header1", header.get(0)
                                      .get("local"));
        assertEquals("", header.get(0)
                               .get("derivedDefault")); // reason declares none
    }

    @Test
    void aMappedHeaderColumnCarryingADefaultIsComparedAgainstThatDefault() {
        // The same asymmetry one level up: a mapped header column left null by the source is stored
        // carrying the target column's default.
        Map<String, Object> posting = postingOf(yaml(WITH_STATUS).replace("      - { name: reason, type: string, length: 400 }",
                "      - { name: reason, type: string, length: 400, defaultValue: 'automatic' }"));
        assertEquals("\"automatic\"", headerAssignments(posting).get(0)
                                                                .get("derivedDefault"));
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

    @Test
    void anInitWrittenAsTheSeededStatusNameResolvesToItsId() {
        // The readable spelling an author reaches for reaches the guard as the seed id it names, not as
        // a word no guard can compare against: StatusSymbolResolver rewrites it before the typed
        // mapping. The guard's own non-numeric fallback is therefore unreachable through the parser -
        // it warns and leaves the target rewritable rather than refusing every amendment (#7131).
        assertEquals("target.Status != null && target.Status == 2",
                posting("      - { name: Status, kind: manyToOne, to: JournalEntryStatus, function: EntityStatus, init: Posted }").get(
                        "amendableGuard"));
    }
}
