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
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.base.helpers.JsonHelper;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The {@code .glue}'s expression keys carry READINGS, never Java (issue #7425): a mapped value, a
 * row or event guard, a transition's guard, a timer's due moment travel as a {@code kind}-tagged
 * map of the authored facts, the template layer rendering the Java from them. The rendered Java
 * itself is pinned by the sibling glue tests through {@link GlueRendering}; this test pins the
 * SHAPE, and that the serialized glue holds no Java and survives the JSON round-trip a regeneration
 * reads it back through (every number as text, no null entry to drop).
 */
class GlueExpressionReadingsTest {

    private static final String POSTINGS = """
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
            postings:
              - name: salesInvoicePosting
                event: { onTransition: SalesInvoice, model: sales-invoices, when: "Status == 3" }
                creates: JournalEntry
                backReference: SalesInvoice
                map: { entryDate: date, reason: "Sales invoice {number}" }
                rule: { entity: PostingRule, match: { documentType: "Sales Invoice" } }
                items:
                  - { Account: rule(receivableAccount), debit: "Net + Vat" }
                  - { Account: rule(revenueAccount), credit: "Vat", when: "Vat != 0" }
            """;

    private static final String TRANSITIONS = """
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

    private static final String PROCESSES = """
            name: services
            entities:
              - name: Case
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: subject, type: string }
                  - { name: validUntil, type: date }
                relations:
                  - { name: messages, kind: oneToMany, to: CaseMessage }
              - name: CaseMessage
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: text, type: string }
                  - { name: internal, type: integer }
                relations:
                  - { name: case, kind: manyToOne, to: Case, composition: true }
            processes:
              - name: CaseHandling
                trigger: { onCreate: Case }
                steps:
                  - name: work
                    kind: userTask
                    args:
                      assignee: agent
                      expire: { until: validUntil, then: markExpired }
                      next: done
                  - { name: markExpired, kind: serviceTask, args: { setField: subject, value: EXPIRED, next: end } }
                  - { name: awaitReply,  kind: wait, args: { onCreate: CaseMessage, via: case, when: "internal == 0", next: done } }
                  - { name: done,        kind: end }
            """;

    @Test
    void aPostingsCellsAndGuardsAreReadings() {
        Map<String, Object> posting = GlueIntentGenerator.buildPostingsForTest(IntentParser.parse(POSTINGS))
                                                         .get(0);

        List<Map<String, Object>> header = maps(posting.get("headerAssignments"));
        assertEquals(Map.of("kind", "read", "owner", "source", "property", "Date"), reading(header, "EntryDate"));
        assertEquals(List.of(Map.of("kind", "string", "text", "Sales invoice "),
                Map.of("kind", "read", "owner", "source", "property", "Number")), reading(header, "Reason").get("parts"));

        List<Map<String, Object>> rows = maps(posting.get("itemRows"));
        List<Map<String, Object>> first = maps(rows.get(0)
                                                   .get("assigns"));
        assertEquals(Map.of("kind", "read", "owner", "ruleRow", "property", "ReceivableAccount"), reading(first, "Account"));
        // The scale travels as TEXT: the glue is read back through a parser that types every number as
        // a Double, and a `2.0` in the generated call would not compile.
        assertEquals(Map.of("kind", "calc", "text", "Net + Vat", "owner", "source", "scale", "2"), reading(first, "Debit"));
        assertEquals(Map.of(), rows.get(0)
                                   .get("guardReading"),
                "a row without a `when` carries the empty reading");
        assertEquals(Map.of("kind", "calcCompare", "owner", "source", "property", "Vat", "equal", false, "text", "0"), rows.get(1)
                                                                                                                           .get("guardReading"));
        for (String rendered : List.of("conditionalRuleGuards", "expr", "guard")) {
            assertFalse(JsonHelper.toJson(posting)
                                  .contains("\"" + rendered + "\""),
                    "the rendered [" + rendered + "] key must not be carried any more");
        }
    }

    @Test
    void aTransitionsGuardIsAReading() {
        Map<String, Object> transition = GlueIntentGenerator.buildTransitionsForTest(IntentParser.parse(TRANSITIONS))
                                                            .get(0);

        assertEquals(Map.of("kind", "calcCompare", "owner", "source", "property", "Paid", "equal", true, "text", "0"),
                transition.get("guardReading"));
        assertEquals("Paid == 0", transition.get("guardText"));
        assertFalse(transition.containsKey("guardExpr"));
    }

    @Test
    void aWaitsGuardAndATimersDueMomentAreReadings() {
        IntentModel model = IntentParser.parse(PROCESSES);

        Map<String, Object> wait = GlueIntentGenerator.buildWaitsForTest(model)
                                                      .get(0);
        // An untyped guard infers the term's type from the literal's spelling - the same terms a typed
        // guard carries, so the template layer renders both through one rule.
        Map<String, Object> term = maps(wait.get("guardTerms")).get(0);
        assertEquals("entity", term.get("owner"));
        assertEquals("Internal", term.get("property"));
        assertEquals(true, term.get("equal"));
        assertEquals("number", term.get("type"));
        assertEquals("0", term.get("value"));
        assertEquals(List.of("owner", "property", "equal", "type", "value", "numericKey"), List.copyOf(term.keySet()));

        Map<String, Object> loader = GlueIntentGenerator.buildTimerLoadersForTest(model)
                                                        .get(0);
        assertEquals(Map.of("kind", "due", "property", "ValidUntil", "shape", "date"), loader.get("due"));
        assertFalse(loader.containsKey("dueExpression"));
    }

    /**
     * What a reading is FOR: the serialized glue is the process description every template reads, and
     * none of it is Java any more.
     */
    @Test
    void theSerializedGlueCarriesNoJava() {
        for (Object collection : List.of(GlueIntentGenerator.buildPostingsForTest(IntentParser.parse(POSTINGS)),
                GlueIntentGenerator.buildTransitionsForTest(IntentParser.parse(TRANSITIONS)),
                GlueIntentGenerator.buildWaitsForTest(IntentParser.parse(PROCESSES)),
                GlueIntentGenerator.buildTimerLoadersForTest(IntentParser.parse(PROCESSES)))) {
            String json = JsonHelper.toJson(collection);
            assertFalse(json.contains("Calc.eval"), json);
            assertFalse(json.contains("java."), json);
            assertFalse(json.contains("org.eclipse.dirigible.sdk"), json);
        }
    }

    private static Map<String, Object> reading(List<Map<String, Object>> assignments, String targetProp) {
        for (Map<String, Object> assignment : assignments) {
            if (targetProp.equals(assignment.get("targetProp"))) {
                assertFalse(assignment.containsKey("expr"), "an assignment carries its reading, not the rendered Java");
                return map(assignment.get("reading"));
            }
        }
        throw new AssertionError("no assignment writes [" + targetProp + "] in " + assignments);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> maps(Object raw) {
        return (List<Map<String, Object>>) raw;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object raw) {
        return (Map<String, Object>) raw;
    }
}
