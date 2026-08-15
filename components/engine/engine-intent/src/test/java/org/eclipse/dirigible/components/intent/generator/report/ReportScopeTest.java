/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.dirigible.components.intent.generator.IntentGenerationContext;
import org.eclipse.dirigible.components.intent.generator.TestContexts;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The lifecycle predicate a report's {@code scope} emits into the {@code .report} query - the fix
 * for "a voided or draft document silently counts in every sum" (dirigible #6645).
 */
class ReportScopeTest {

    /** Report block placeholder - each case swaps in the report it exercises. */
    private static final String YAML = """
            name: sales
            entities:
              - name: InvoiceStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: issuedOn, type: date }
                  - { name: total, type: decimal }
                relations:
                  - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
            reports:
            #REPORT#
            seeds:
              - name: invoice-statuses
                entity: InvoiceStatus
                rows:
                  - { id: 1, name: DRAFT, stage: draft }
                  - { id: 3, name: ISSUED, stage: live }
                  - { id: 7, name: PAID, stage: live }
                  - { id: 8, name: CANCELLED, stage: cancelled }
                  - { id: 9, name: VOIDED, stage: void }
            """;

    /** The generated SQL of the single report the given block declares. */
    private static String queryOf(String reportBlock) {
        return String.valueOf(document(reportBlock).get("query"));
    }

    private static java.util.Map<String, Object> document(String reportBlock) {
        IntentModel model = IntentParser.parse(YAML.replace("#REPORT#", reportBlock));
        return ReportIntentGenerator.buildForTest(TestContexts.context(model), model.getReports()
                                                                                    .get(0));
    }

    /**
     * The motivating case: dimensions and measures, no filter, no scope - and previously no
     * {@code WHERE} at all, so every draft invoice inflated the monthly revenue.
     */
    @Test
    void anAggregateOverAStageClassifiedLifecycleDefaultsToLive() {
        String query = queryOf("""
                  - name: RevenueByMonth
                    source: Invoice
                    dimensions: ["month(issuedOn)"]
                    measures: ["sum(total)"]
                """);
        assertTrue(query.contains("WHERE Invoice.\"INVOICE_STATUS\" IN (3, 7)"),
                "an unscoped aggregation should count the live statuses only, got: " + query);
    }

    @Test
    void anExplicitStageScopeSelectsThatStage() {
        String query = queryOf("""
                  - name: VoidedInvoices
                    source: Invoice
                    scope: void
                    measures: ["sum(total)"]
                """);
        assertTrue(query.contains("WHERE Invoice.\"INVOICE_STATUS\" IN (9)"), "scope: void should select the voided status, got: " + query);
    }

    @Test
    void scopeAllCountsEveryRow() {
        String query = queryOf("""
                  - name: AllInvoices
                    source: Invoice
                    scope: all
                    measures: ["sum(total)"]
                """);
        assertFalse(query.contains("WHERE"), "scope: all is the explicit opt-out, got: " + query);
    }

    /** A breakdown BY status is about the lifecycle, so it must keep its draft and voided rows. */
    @Test
    void aReportGroupingByTheStatusKeepsEveryRow() {
        String query = queryOf("""
                  - name: InvoicesByStatus
                    source: Invoice
                    dimensions: [Status]
                    measures: ["count(*)"]
                """);
        assertFalse(query.contains("INVOICE_STATUS\" IN"), "a status dimension is the report's subject, got: " + query);
    }

    /** An authored status predicate is authoritative - the default must not double up on it. */
    @Test
    void aFilterOnTheStatusWins() {
        String query = queryOf("""
                  - name: OpenInvoices
                    source: Invoice
                    filter: "Status != 9"
                    measures: ["sum(total)"]
                """);
        assertEquals(1, query.split("INVOICE_STATUS", -1).length - 1, "the authored predicate should be the only status clause: " + query);
        assertTrue(query.contains("WHERE Invoice.\"INVOICE_STATUS\" != 9"), query);
    }

    /** A listing report aggregates nothing, so there is no total to protect. */
    @Test
    void aNonAggregatingReportIsUntouched() {
        String query = queryOf("""
                  - name: InvoiceList
                    source: Invoice
                    dimensions: [issuedOn, total]
                """);
        assertFalse(query.contains("WHERE"), "a plain listing keeps every row, got: " + query);
    }

    /**
     * A scope combines with an authored filter rather than replacing it. The filter is not
     * parenthesised because it decomposes into plain AND-ed comparisons (a filter that does not is left
     * bracketed - see {@code ReportEditorRoundTripTest}), which is what lets the report editor's
     * builder own the predicate instead of opening the report free-style.
     */
    @Test
    void anExplicitScopeAndsWithTheFilter() {
        String query = queryOf("""
                  - name: RevenueThisYear
                    source: Invoice
                    scope: live
                    filter: "issuedOn >= CURRENT_DATE"
                    measures: ["sum(total)"]
                """);
        assertTrue(query.contains("WHERE Invoice.\"INVOICE_ISSUED_ON\" >= CURRENT_DATE AND Invoice.\"INVOICE_STATUS\" IN (3, 7)"),
                "the filter and the scope should be ANDed, got: " + query);
    }

    /**
     * Part 3 - the cheap half: with no stage classification there is nothing to default to, so the
     * omission is reported instead of silently producing an inflated total.
     */
    @Test
    void anUnclassifiedNomenclatureWarnsInsteadOfGuessing() {
        IntentModel model = IntentParser.parse(YAML.replace("#REPORT#", """
                  - name: RevenueByMonth
                    source: Invoice
                    measures: ["sum(total)"]
                """)
                                                   .replaceAll(", stage: \\w+", ""));
        IntentGenerationContext context = TestContexts.context(model);
        String query = String.valueOf(ReportIntentGenerator.buildForTest(context, model.getReports()
                                                                                       .get(0))
                                                           .get("query"));
        assertFalse(query.contains("WHERE"), "nothing is resolvable, so the query stays as authored: " + query);
        assertTrue(context.getIssues()
                          .stream()
                          .anyMatch(issue -> issue.contains("neither declares `scope:` nor filters on that status")),
                "the lifecycle-blind aggregate should be reported, got: " + context.getIssues());
    }

    /** An entity with no lifecycle at all is not this feature's business. */
    @Test
    void aSourceWithoutALifecycleIsUntouched() {
        IntentModel model = IntentParser.parse("""
                name: sales
                entities:
                  - name: Invoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: total, type: decimal }
                reports:
                  - name: Revenue
                    source: Invoice
                    measures: ["sum(total)"]
                """);
        IntentGenerationContext context = TestContexts.context(model);
        String query = String.valueOf(ReportIntentGenerator.buildForTest(context, model.getReports()
                                                                                       .get(0))
                                                           .get("query"));
        assertFalse(query.contains("WHERE"), query);
        assertTrue(context.getIssues()
                          .isEmpty(),
                "no lifecycle means nothing to warn about, got: " + context.getIssues());
    }
}
