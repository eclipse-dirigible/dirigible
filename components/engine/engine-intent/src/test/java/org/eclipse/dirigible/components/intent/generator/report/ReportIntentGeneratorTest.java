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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.generator.TestContexts;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.ReportIntent;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.junit.jupiter.api.Test;

/**
 * The widget block a report-attached KPI emits into the {@code .report}: authored expressions are
 * resolved to the report's own column aliases (the tracking maps {@code build} assembles), the
 * {@code now} token stays symbolic, defaults apply. The full build path (alias tracking inside the
 * dimension/measure loops) is covered end-to-end by {@code IntentEngineIT}.
 */
class ReportIntentGeneratorTest {

    private static final String INTENT = """
            name: sales
            entities:
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: issuedOn, type: date }
                  - { name: total, type: decimal }
            reports:
              - name: RevenueByMonth
                source: Invoice
                dimensions: ["month(issuedOn)"]
                measures: ["sum(total)"]
                widget:
                  value: "sum(total)"
                  at: { "month(issuedOn)": now }
                  label: Revenue (this month)
                  icon: banknote
            """;

    private static ReportIntent report() {
        IntentModel model = IntentParser.parse(INTENT);
        return model.getReports()
                    .get(0);
    }

    /** The column maps as {@code build} tracks them for the widget resolution. */
    private static Map<String, Object> column(String alias, String type, String pattern) {
        Map<String, Object> column = new LinkedHashMap<>();
        column.put("alias", alias);
        column.put("type", type);
        if (pattern != null) {
            column.put("pattern", pattern);
        }
        return column;
    }

    @Test
    void chartKindParsesOntoTheReport() {
        IntentModel model = IntentParser.parse("""
                name: sales
                entities:
                  - name: Invoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: issuedOn, type: date }
                      - { name: total, type: decimal }
                reports:
                  - name: MonthlyRevenue
                    source: Invoice
                    dimensions: ["month(issuedOn)"]
                    measures: ["sum(total)"]
                    chart: bar
                """);
        assertEquals("bar", model.getReports()
                                 .get(0)
                                 .getChart());
    }

    @Test
    void unknownChartKindIsRejected() {
        assertThrows(IntentValidationException.class, () -> IntentParser.parse("""
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
                    chart: bogus
                """));
    }

    @Test
    void valueWidgetResolvesMeasureColumnAndPinsTheBucketDimension() {
        Map<String, ReportIntentGenerator.WidgetDimension> dimensions = new LinkedHashMap<>();
        dimensions.put("month(issuedon)", new ReportIntentGenerator.WidgetDimension(column("Month Issued On", "INTEGER", null), "month"));
        Map<String, Map<String, Object>> measures = new LinkedHashMap<>();
        measures.put("sum(total)", column("Sum Total", "DECIMAL", "### ### ### ##0.00"));

        Map<String, Object> widget = ReportIntentGenerator.widget(report(), dimensions, measures);

        assertEquals("value", widget.get("kind"));
        assertEquals("Revenue (this month)", widget.get("label"));
        assertEquals("banknote", widget.get("icon"));
        assertEquals("widgetRevenueByMonth", widget.get("tId"));
        assertEquals("Sum Total", widget.get("valueColumn"));
        assertEquals("DECIMAL", widget.get("valueType"));
        assertEquals("### ### ### ##0.00", widget.get("pattern"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pins = (List<Map<String, Object>>) widget.get("at");
        assertEquals(1, pins.size());
        Map<String, Object> pin = pins.get(0);
        assertEquals("Month Issued On", pin.get("column"));
        assertEquals("INTEGER", pin.get("type"));
        assertEquals("month", pin.get("bucket"));
        assertEquals("now", pin.get("token"));
        assertNull(pin.get("value"));
    }

    @Test
    void countWidgetDefaultsKindLabelAndIcon() {
        ReportIntent report = report();
        report.getWidget()
              .setValue(null);
        report.getWidget()
              .setKind(null);
        report.getWidget()
              .setLabel(null);
        report.getWidget()
              .setIcon(null);
        report.getWidget()
              .setAt(null);

        Map<String, Object> widget = ReportIntentGenerator.widget(report, new LinkedHashMap<>(), new LinkedHashMap<>());

        assertEquals("count", widget.get("kind"));
        assertEquals("Revenue By Month", widget.get("label"));
        assertEquals("gauge", widget.get("icon"));
        assertFalse(widget.containsKey("valueColumn"));
        assertFalse(widget.containsKey("at"));
        assertFalse(widget.containsKey("limit"));
    }

    @Test
    void listWidgetCarriesTheLimitAndLiteralPinsKeepTheirValue() {
        ReportIntent report = report();
        report.getWidget()
              .setValue(null);
        report.getWidget()
              .setKind("list");
        report.getWidget()
              .setLimit(3);
        report.getWidget()
              .getAt()
              .put("month(issuedOn)", 202601L);

        Map<String, ReportIntentGenerator.WidgetDimension> dimensions = new LinkedHashMap<>();
        dimensions.put("month(issuedon)", new ReportIntentGenerator.WidgetDimension(column("Month Issued On", "INTEGER", null), "month"));

        Map<String, Object> widget = ReportIntentGenerator.widget(report, dimensions, new LinkedHashMap<>());

        assertEquals("list", widget.get("kind"));
        assertEquals(3, widget.get("limit"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pins = (List<Map<String, Object>>) widget.get("at");
        // A non-`now` pin is a literal: it keeps its value instead of becoming a token.
        assertEquals(1, pins.size());
        assertEquals(202601L, pins.get(0)
                                  .get("value"));
        assertNull(pins.get(0)
                       .get("token"));
    }

    private static final String STATUS_FILTER_INTENT = """
            name: billing
            entities:
              - name: InvoiceStatus
                kind: setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Customer
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string }
                  - { name: due, type: date }
                  - { name: balance, type: decimal }
                relations:
                  - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
                  - { name: Customer, kind: manyToOne, to: Customer }
            reports:
              - name: OverdueInvoices
                source: Invoice
                dimensions: [number, due, Customer.name]
                filter: "due <= CURRENT_DATE AND Customer.name != 'X' AND Status != 8"
            """;

    @Test
    void filterTranslatesABareToOneRelationToItsFkColumn() {
        IntentModel model = IntentParser.parse(STATUS_FILTER_INTENT);
        Map<String, Object> document = ReportIntentGenerator.buildForTest(TestContexts.context(model), model.getReports()
                                                                                                            .get(0));
        String query = (String) document.get("query");
        // A bare to-one relation name filters by its FK column - previously it passed through
        // untranslated (`AND Status != 8`) and broke the generated SQL.
        assertTrue(query.contains("Invoice.\"INVOICE_STATUS\" != 8"), query);
        // The dotted ref keeps its join-alias form - the bare-relation pass must not mangle the
        // alias token it produced.
        assertTrue(query.contains("Customer.\"CUSTOMER_NAME\" != 'X'"), query);
        assertTrue(!query.contains(" Status "), query);
    }

    private static final String LEDGER_INTENT = """
            name: ledger
            entities:
              - name: Account
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: code, type: string }
                  - { name: name, type: string }
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
                dimensions: [account.code, account.name]
                filter: "credit == 0"
            """;

    @Test
    @SuppressWarnings("unchecked")
    void balanceReportEmitsTheWindowedTotalsQueryAndTheDateParameters() {
        IntentModel model = IntentParser.parse(LEDGER_INTENT);
        Map<String, Object> document = ReportIntentGenerator.buildForTest(TestContexts.context(model), model.getReports()
                                                                                                            .get(0));

        assertEquals("balance", document.get("kind"));

        String query = (String) document.get("query");
        // The window: opening strictly before :fromDate, period inclusive, closing up to :toDate.
        assertTrue(query.contains(
                "SUM(CASE WHEN JournalEntry.\"JOURNAL_ENTRY_ENTRY_DATE\" < :fromDate THEN COALESCE(JournalEntryItem.\"JOURNAL_ENTRY_ITEM_DEBIT\", 0) ELSE 0 END) as \"Opening Debit\""),
                query);
        assertTrue(query.contains(
                "SUM(CASE WHEN JournalEntry.\"JOURNAL_ENTRY_ENTRY_DATE\" >= :fromDate AND JournalEntry.\"JOURNAL_ENTRY_ENTRY_DATE\" <= :toDate THEN COALESCE(JournalEntryItem.\"JOURNAL_ENTRY_ITEM_CREDIT\", 0) ELSE 0 END) as \"Credit\""),
                query);
        assertTrue(query.contains(
                "SUM(CASE WHEN JournalEntry.\"JOURNAL_ENTRY_ENTRY_DATE\" <= :toDate THEN COALESCE(JournalEntryItem.\"JOURNAL_ENTRY_ITEM_DEBIT\", 0) ELSE 0 END) as \"Closing Debit\""),
                query);
        // The date rides in over the composition join; the dimensions join the account.
        assertTrue(query.contains("INNER JOIN \"LEDGER_JOURNAL_ENTRY\" as JournalEntry"), query);
        assertTrue(query.contains("INNER JOIN \"LEDGER_ACCOUNT\" as Account"), query);
        assertTrue(query.contains("GROUP BY Account.\"ACCOUNT_CODE\", Account.\"ACCOUNT_NAME\""), query);
        // The intent-guard-style `==` is normalized to SQL's single `=` (PostgreSQL rejects `==`).
        assertTrue(query.contains("WHERE JournalEntryItem.\"JOURNAL_ENTRY_ITEM_CREDIT\" = 0"), query);

        List<Map<String, Object>> parameters = (List<Map<String, Object>>) document.get("parameters");
        assertEquals(2, parameters.size());
        assertEquals("fromDate", parameters.get(0)
                                           .get("name"));
        assertEquals("DATE", parameters.get(0)
                                       .get("type"));
        assertEquals("1900-01-01", parameters.get(0)
                                             .get("initial"));
        assertEquals("toDate", parameters.get(1)
                                         .get("name"));
        assertEquals("9999-12-31", parameters.get(1)
                                             .get("initial"));

        // Two dimensions + the six totals, all SUM DECIMAL with the money pattern.
        List<Map<String, Object>> columns = (List<Map<String, Object>>) document.get("columns");
        assertEquals(8, columns.size());
        List<String> totals = List.of("Opening Debit", "Opening Credit", "Debit", "Credit", "Closing Debit", "Closing Credit");
        for (int i = 0; i < totals.size(); i++) {
            Map<String, Object> column = columns.get(2 + i);
            assertEquals(totals.get(i), column.get("alias"));
            assertEquals("SUM", column.get("aggregate"));
            assertEquals("DECIMAL", column.get("type"));
            assertEquals("### ### ### ##0.00", column.get("pattern"));
        }
    }

    @Test
    void balanceReportRequiresItsInputsAndForbidsMeasures() {
        IntentValidationException error = assertThrows(IntentValidationException.class, () -> IntentParser.parse("""
                name: ledger
                entities:
                  - name: JournalEntryItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: debit, type: decimal }
                      - { name: postedAt, type: timestamp }
                reports:
                  - name: TrialBalance
                    kind: balance
                    source: JournalEntryItem
                    date: postedAt
                    debit: debit
                    measures: ["sum(debit)"]
                """));
        String message = error.getMessage();
        assertTrue(message.contains("must not declare measures"), message);
        assertTrue(message.contains("needs at least one dimension"), message);
        assertTrue(message.contains("date [postedAt] must be a date field (found [timestamp])"), message);
        assertTrue(message.contains("needs credit"), message);
    }

    @Test
    void balanceInputsWithoutTheKindAndAnUnknownKindAreRejected() {
        IntentValidationException error = assertThrows(IntentValidationException.class, () -> IntentParser.parse("""
                name: ledger
                entities:
                  - name: JournalEntryItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: debit, type: decimal }
                reports:
                  - name: Totals
                    source: JournalEntryItem
                    debit: debit
                  - name: Weird
                    kind: pivot
                    source: JournalEntryItem
                """));
        String message = error.getMessage();
        assertTrue(message.contains("declares date/debit/credit but is not kind: balance"), message);
        assertTrue(message.contains("unknown kind [pivot]"), message);
    }
}
