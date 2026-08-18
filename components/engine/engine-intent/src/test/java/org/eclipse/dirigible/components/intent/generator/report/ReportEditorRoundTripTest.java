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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.generator.TestContexts;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The round-trip guard of dirigible #6675: a generated {@code .report} must carry a structured
 * model the report editor's visual builder can rebuild the stored {@code query} from.
 *
 * <p>
 * When it cannot, the editor falls back to free-style (the query string is the source of truth and
 * is never regenerated) - safe, but the builder panels go away, so every report that <i>can</i>
 * round-trip should. Before the guard existed the editor simply assumed the structured model was
 * authoritative and rewrote the query from it on the load digest: quoting was lost,
 * {@code COUNT(*)} became {@code COUNT(alias.*)}, the joins the generator never emitted
 * disappeared, an empty {@code conditions} produced a bare {@code WHERE}, and a computed dimension
 * degraded to its raw column - a file the user only opened came back corrupted.
 *
 * <p>
 * The oracle below is a port of {@code buildQuery()} in {@code editor-report/js/editor.js}. It is
 * the contract between the two modules; keep it in step with the editor when either side changes.
 */
class ReportEditorRoundTripTest {

    private static final String MODEL = """
            name: sales
            entities:
              - name: InvoiceStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Customer
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                  - { name: country, type: string }
              - name: Currency
                function: Setting
                multilingual: true
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: issuedOn, type: date }
                  - { name: due, type: date }
                  - { name: total, type: decimal }
                  - { name: balance, type: decimal }
                relations:
                  - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
                  - { name: Customer, kind: manyToOne, to: Customer }
                  - { name: Currency, kind: manyToOne, to: Currency }
            reports:
            #REPORT#
            seeds:
              - name: invoice-statuses
                entity: InvoiceStatus
                rows:
                  - { id: 1, name: DRAFT, stage: draft }
                  - { id: 3, name: ISSUED, stage: live }
                  - { id: 9, name: VOIDED, stage: void }
            """;

    private static Map<String, Object> document(String reportBlock) {
        IntentModel model = IntentParser.parse(MODEL.replace("#REPORT#", reportBlock));
        return ReportIntentGenerator.buildForTest(TestContexts.context(model), model.getReports()
                                                                                    .get(0));
    }

    /** The report opens in the editor's structured mode: the builder reproduces the stored query. */
    private static void assertRoundTrips(Map<String, Object> document) {
        assertEquals(document.get("query"), editorBuildQuery(document),
                "the visual builder must rebuild the stored query, else the editor opens the report free-style");
    }

    @Test
    void aJoinedAggregationRoundTrips() {
        Map<String, Object> document = document("""
                  - name: InvoicesByCustomer
                    source: Invoice
                    dimensions: [Customer.name]
                    measures: ["count(*)", "sum(total)"]
                """);
        assertRoundTrips(document);

        // The join the generator resolves is now part of the model, not only of the query string -
        // the editor rebuilds joins solely from `joins`, so an absent one used to be deleted on save.
        List<Map<String, Object>> joins = joins(document);
        assertEquals(1, joins.size());
        Map<String, Object> join = joins.get(0);
        assertEquals("Customer", join.get("alias"));
        assertEquals("SALES_CUSTOMER", join.get("name"));
        assertEquals("INNER", join.get("type"));
        assertEquals("Invoice.\"INVOICE_CUSTOMER\" = Customer.\"CUSTOMER_ID\"", join.get("condition"));
        assertTrue(String.valueOf(document.get("query"))
                         .contains("INNER JOIN \"SALES_CUSTOMER\" as Customer ON Invoice.\"INVOICE_CUSTOMER\" = Customer.\"CUSTOMER_ID\""),
                document.get("query")
                        .toString());

        // A bare count() is the star itself, so the builder emits COUNT(*) - qualifying it as
        // COUNT(Invoice.*) is rejected by H2.
        Map<String, Object> count = columns(document).get(1);
        assertEquals("*", count.get("name"));
        assertEquals("COUNT", count.get("aggregate"));
    }

    @Test
    void aTranslatedDimensionRoundTrips() {
        Map<String, Object> document = document("""
                  - name: InvoicesByCurrency
                    source: Invoice
                    dimensions: [Currency]
                    measures: ["sum(total)"]
                """);
        assertRoundTrips(document);

        // The overlay rides on the two things the editor's builder already rebuilds from: a LEFT join
        // row and the column's verbatim expression. Neither needed a new concept in the editor - but
        // both must be in the model, or opening the report would drop the translation on save.
        Map<String, Object> language = joins(document).get(1);
        assertEquals("Currency_LANG", language.get("alias"));
        assertEquals("SALES_CURRENCY_LANG", language.get("name"));
        assertEquals("LEFT", language.get("type"));
        assertEquals("Currency_LANG.\"Id\" = Currency.\"CURRENCY_ID\" AND Currency_LANG.\"Language\" = :language",
                language.get("condition"));
        assertEquals("COALESCE(Currency_LANG.\"Name\", Currency.\"CURRENCY_NAME\")", columns(document).get(0)
                                                                                                      .get("expression"));
    }

    /** A computed dimension lives only in the query string unless the column carries its expression. */
    @Test
    void computedDimensionsCarryTheirExpression() {
        Map<String, Object> monthly = document("""
                  - name: RevenueByMonth
                    source: Invoice
                    dimensions: ["month(issuedOn)"]
                    measures: ["sum(total)"]
                """);
        assertRoundTrips(monthly);
        assertEquals("(EXTRACT(YEAR FROM Invoice.\"INVOICE_ISSUED_ON\") * 100 + EXTRACT(MONTH FROM Invoice.\"INVOICE_ISSUED_ON\"))",
                columns(monthly).get(0)
                                .get("expression"));

        Map<String, Object> ageing = document("""
                  - name: Receivables
                    source: Invoice
                    dimensions: ["ageing(due, [30, 60, 90])"]
                    measures: ["sum(balance)"]
                """);
        assertRoundTrips(ageing);
        assertTrue(String.valueOf(columns(ageing).get(0)
                                                 .get("expression"))
                         .startsWith("CASE WHEN Invoice.\"INVOICE_DUE\" IS NULL THEN 'n/a'"),
                String.valueOf(columns(ageing).get(0)));
    }

    /** A plain listing: no aggregate, no WHERE, nothing computed - the simplest structured report. */
    @Test
    void aPlainListingRoundTrips() {
        assertRoundTrips(document("""
                  - name: InvoiceList
                    source: Invoice
                    dimensions: [issuedOn, total]
                """));
    }

    /** The filter and the lifecycle scope both become conditions the builder owns. */
    @Test
    void aFilteredAndScopedAggregationRoundTrips() {
        Map<String, Object> document = document("""
                  - name: RevenueThisYear
                    source: Invoice
                    scope: live
                    filter: "issuedOn >= CURRENT_DATE AND Customer.country != 'XX'"
                    measures: ["sum(total)"]
                """);
        assertRoundTrips(document);

        List<Map<String, Object>> conditions = conditions(document);
        assertEquals(3, conditions.size());
        assertEquals("Invoice.\"INVOICE_ISSUED_ON\"", conditions.get(0)
                                                                .get("left"));
        assertEquals(">=", conditions.get(0)
                                     .get("operation"));
        assertEquals("CURRENT_DATE", conditions.get(0)
                                               .get("right"));
        // The condition tokens are the query's own quoted physical columns - an unquoted token would
        // have been re-emitted unquoted and stopped matching the UPPER_SNAKE objects on PostgreSQL.
        assertEquals("Customer.\"CUSTOMER_COUNTRY\"", conditions.get(1)
                                                                .get("left"));
        assertEquals("Invoice.\"INVOICE_STATUS\"", conditions.get(2)
                                                             .get("left"));
        assertEquals("IN", conditions.get(2)
                                     .get("operation"));
        assertEquals("(3)", conditions.get(2)
                                      .get("right"));
    }

    /** No filter and no scope: no `conditions` key at all - an empty one emitted a bare `WHERE`. */
    @Test
    void anUnfilteredReportEmitsNoConditions() {
        Map<String, Object> document = document("""
                  - name: InvoicesByStatus
                    source: Invoice
                    dimensions: [Status]
                    measures: ["count(*)"]
                """);
        assertRoundTrips(document);
        assertFalse(document.containsKey("conditions"), "an empty conditions array makes the editor emit a bare WHERE");
        assertFalse(String.valueOf(document.get("query"))
                          .contains("WHERE"),
                document.get("query")
                        .toString());
    }

    /** The balance windows are expressions too, so the accounting reports round-trip as well. */
    @Test
    void aBalanceReportRoundTrips() {
        IntentModel model = IntentParser.parse("""
                name: ledger
                entities:
                  - name: Account
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: code, type: string }
                  - name: JournalEntry
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: entryDate, type: date }
                    relations:
                      - { name: items, kind: oneToMany, to: JournalEntryItem }
                  - name: JournalEntryItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: debit, type: decimal }
                      - { name: credit, type: decimal }
                    relations:
                      - { name: journalEntry, kind: manyToOne, to: JournalEntry, composition: true }
                      - { name: account, kind: manyToOne, to: Account, required: true }
                reports:
                  - name: TrialBalance
                    kind: balance
                    source: JournalEntryItem
                    date: journalEntry.entryDate
                    debit: debit
                    credit: credit
                    dimensions: [account.code]
                """);
        Map<String, Object> document = ReportIntentGenerator.buildForTest(TestContexts.context(model), model.getReports()
                                                                                                            .get(0));
        assertRoundTrips(document);
        assertEquals(2, joins(document).size());
    }

    /**
     * A predicate the builder cannot represent must NOT be half-emitted: the conditions are dropped
     * whole, the query keeps the filter verbatim, and the editor opens the report free-style. An OR is
     * the case that matters - splitting it into AND-ed condition rows would rebind it against the scope
     * and silently change which rows the report counts.
     */
    @Test
    void anIrreducibleFilterFallsBackToFreeStyle() {
        Map<String, Object> document = document("""
                  - name: Flagged
                    source: Invoice
                    scope: live
                    filter: "total > 1000 OR balance > 500"
                    measures: ["count(*)"]
                """);
        String query = String.valueOf(document.get("query"));
        assertFalse(document.containsKey("conditions"), "a partial decomposition would silently drop the rest of the filter");
        // Bracketed, so the appended scope cannot rebind the OR.
        assertTrue(query.contains(
                "WHERE (Invoice.\"INVOICE_TOTAL\" > 1000 OR Invoice.\"INVOICE_BALANCE\" > 500) AND Invoice.\"INVOICE_STATUS\" IN (3)"),
                query);
        assertNotEquals(query, editorBuildQuery(document), "the report must open free-style, so the builder must not reproduce its query");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> columns(Map<String, Object> document) {
        return (List<Map<String, Object>>) document.get("columns");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> joins(Map<String, Object> document) {
        return (List<Map<String, Object>>) document.getOrDefault("joins", List.of());
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> conditions(Map<String, Object> document) {
        return (List<Map<String, Object>>) document.getOrDefault("conditions", List.of());
    }

    // --- The oracle: a port of buildQuery() in editor-report/js/editor.js -------------------------

    private static String quoteIdentifier(String name) {
        return name == null || name.isEmpty() || "*".equals(name) || name.startsWith("\"") ? name : "\"" + name + "\"";
    }

    private static String columnTerm(Map<String, Object> column) {
        if (column.get("expression") != null) {
            return (String) column.get("expression");
        }
        if ("*".equals(column.get("name"))) {
            return "*";
        }
        return column.get("table") + "." + quoteIdentifier((String) column.get("name"));
    }

    private static String editorBuildQuery(Map<String, Object> report) {
        StringBuilder query = new StringBuilder("SELECT ");
        List<String> selectParts = new ArrayList<>();
        for (Map<String, Object> column : columns(report)) {
            if (Boolean.TRUE.equals(column.get("select"))) {
                String part = columnTerm(column);
                Object aggregate = column.get("aggregate");
                if (aggregate != null && !"NONE".equals(aggregate)) {
                    part = aggregate + "(" + part + ")";
                }
                selectParts.add(part + " as \"" + column.get("alias") + "\"");
            }
        }
        query.append(String.join(", ", selectParts));

        String table = (String) report.get("table");
        if (table != null && !table.isEmpty() && report.get("alias") != null) {
            query.append("\nFROM ")
                 .append(quoteIdentifier(table))
                 .append(" as ")
                 .append(report.get("alias"));
        }
        for (Map<String, Object> join : joins(report)) {
            query.append('\n')
                 .append(join.get("type"))
                 .append(" JOIN ")
                 .append(quoteIdentifier((String) join.get("name")))
                 .append(" as ")
                 .append(join.get("alias"))
                 .append(" ON ")
                 .append(join.get("condition"));
        }
        List<Map<String, Object>> conditions = conditions(report);
        if (!conditions.isEmpty()) {
            query.append("\nWHERE ");
            for (int i = 0; i < conditions.size(); i++) {
                if (i > 0) {
                    query.append(" AND ");
                }
                query.append(conditions.get(i)
                                       .get("left"))
                     .append(' ')
                     .append(conditions.get(i)
                                       .get("operation"))
                     .append(' ')
                     .append(conditions.get(i)
                                       .get("right"));
            }
        }
        List<String> groupParts = new ArrayList<>();
        for (Map<String, Object> column : columns(report)) {
            if (Boolean.TRUE.equals(column.get("grouping"))) {
                groupParts.add(columnTerm(column));
            }
        }
        if (!groupParts.isEmpty()) {
            query.append("\nGROUP BY ")
                 .append(String.join(", ", groupParts));
        }
        // The editor also appends HAVING and ORDER BY; no generated report declares either.
        return query.toString();
    }
}
