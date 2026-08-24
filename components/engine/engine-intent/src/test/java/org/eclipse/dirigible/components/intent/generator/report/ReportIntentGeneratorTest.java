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

    @Test
    @SuppressWarnings("unchecked")
    void correspondenceBucketsTheCounterSideOfTheSameDocumentAndAllocatesProportionally() {
        IntentModel model = IntentParser.parse(GENERAL_LEDGER_INTENT);
        Map<String, Object> document = ReportIntentGenerator.buildForTest(TestContexts.context(model), model.getReports()
                                                                                                            .get(0));
        String query = (String) document.get("query");

        // The counter-side line is a LEFT self-join on the document (the first hop of `date`), with the
        // line itself excluded by key and the pairing restricted to the opposite side - so no bucket is
        // emitted for a same-side sibling, and a document with no counter side keeps its row.
        assertTrue(query.contains(
                "LEFT JOIN \"LEDGER_JOURNAL_ENTRY_ITEM\" as JournalEntryItemCorrespondent ON JournalEntryItemCorrespondent.\"JOURNAL_ENTRY_ITEM_JOURNAL_ENTRY\" = JournalEntryItem.\"JOURNAL_ENTRY_ITEM_JOURNAL_ENTRY\""
                        + " AND JournalEntryItemCorrespondent.\"JOURNAL_ENTRY_ITEM_ID\" <> JournalEntryItem.\"JOURNAL_ENTRY_ITEM_ID\""
                        + " AND ((COALESCE(JournalEntryItem.\"JOURNAL_ENTRY_ITEM_DEBIT\", 0) <> 0 AND COALESCE(JournalEntryItemCorrespondent.\"JOURNAL_ENTRY_ITEM_CREDIT\", 0) <> 0)"
                        + " OR (COALESCE(JournalEntryItem.\"JOURNAL_ENTRY_ITEM_CREDIT\", 0) <> 0 AND COALESCE(JournalEntryItemCorrespondent.\"JOURNAL_ENTRY_ITEM_DEBIT\", 0) <> 0))"),
                query);
        // The bucket path is resolved a SECOND time, against that line - so the account it joins must
        // carry its own alias, or it would collide with the dimension's account and be dropped.
        assertTrue(query.contains(
                "LEFT JOIN \"LEDGER_ACCOUNT\" as AccountCorrespondent ON JournalEntryItemCorrespondent.\"JOURNAL_ENTRY_ITEM_ACCOUNT\" = AccountCorrespondent.\"ACCOUNT_ID\""),
                query);
        assertTrue(query.contains("AccountCorrespondent.\"ACCOUNT_CODE\" as \"Correspondent Account Code\""), query);
        assertTrue(query.contains("GROUP BY Account.\"ACCOUNT_CODE\", Account.\"ACCOUNT_NAME\", AccountCorrespondent.\"ACCOUNT_CODE\""),
                query);

        // A debit line's share of a bucket is that bucket's CREDIT over the document's total credit
        // (excluding the line itself); the cast makes the share a fraction rather than integer division,
        // and the outer COALESCE gives a line with no counter side its full amount in the empty bucket.
        String documentCredit =
                "(SELECT SUM(COALESCE(JournalEntryItemDocumentTotal.\"JOURNAL_ENTRY_ITEM_CREDIT\", 0)) FROM \"LEDGER_JOURNAL_ENTRY_ITEM\" as JournalEntryItemDocumentTotal"
                        + " WHERE JournalEntryItemDocumentTotal.\"JOURNAL_ENTRY_ITEM_JOURNAL_ENTRY\" = JournalEntryItem.\"JOURNAL_ENTRY_ITEM_JOURNAL_ENTRY\""
                        + " AND JournalEntryItemDocumentTotal.\"JOURNAL_ENTRY_ITEM_ID\" <> JournalEntryItem.\"JOURNAL_ENTRY_ITEM_ID\")";
        assertTrue(query.contains(
                "SUM(CASE WHEN JournalEntry.\"JOURNAL_ENTRY_ENTRY_DATE\" >= :fromDate AND JournalEntry.\"JOURNAL_ENTRY_ENTRY_DATE\" <= :toDate"
                        + " THEN COALESCE(CAST(COALESCE(JournalEntryItem.\"JOURNAL_ENTRY_ITEM_DEBIT\", 0) AS DECIMAL(34,12))"
                        + " * COALESCE(JournalEntryItemCorrespondent.\"JOURNAL_ENTRY_ITEM_CREDIT\", 0) / NULLIF(" + documentCredit + ", 0),"
                        + " COALESCE(JournalEntryItem.\"JOURNAL_ENTRY_ITEM_DEBIT\", 0)) ELSE 0 END) as \"Debit\""),
                query);
        // ... and a credit line's share is the mirror image - the bucket's DEBIT over the total debit.
        assertTrue(query.contains(
                "* COALESCE(JournalEntryItemCorrespondent.\"JOURNAL_ENTRY_ITEM_DEBIT\", 0) / NULLIF((SELECT SUM(COALESCE(JournalEntryItemDocumentTotal.\"JOURNAL_ENTRY_ITEM_DEBIT\", 0))"),
                query);

        // The correspondence bucket is a dimension: it groups, and it sits before the six totals.
        List<Map<String, Object>> columns = (List<Map<String, Object>>) document.get("columns");
        assertEquals(9, columns.size());
        Map<String, Object> bucket = columns.get(2);
        assertEquals("Correspondent Account Code", bucket.get("alias"));
        assertEquals("AccountCorrespondent", bucket.get("table"));
        assertEquals("NONE", bucket.get("aggregate"));
        assertEquals(Boolean.TRUE, bucket.get("grouping"));
        assertEquals("Debit", columns.get(5)
                                     .get("alias"));
    }

    @Test
    void correspondenceNeedsADocumentReachingDateAndAResolvablePath() {
        IntentValidationException error = assertThrows(IntentValidationException.class, () -> IntentParser.parse("""
                name: ledger
                entities:
                  - name: JournalEntryItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: entryDate, type: date }
                      - { name: debit, type: decimal }
                      - { name: credit, type: decimal }
                reports:
                  - name: GeneralLedger
                    kind: balance
                    source: JournalEntryItem
                    date: entryDate
                    debit: debit
                    credit: credit
                    dimensions: [debit]
                    correspondence: account.code
                """));
        String message = error.getMessage();
        // The document the lines share IS the first hop of `date`, so a line-local date has none.
        assertTrue(message.contains("correspondence needs the document its lines share"), message);
        assertTrue(message.contains("correspondence [account.code] does not start with a to-one relation of [JournalEntryItem]"), message);
    }

    /**
     * A statement's rows are its declared lines, so it has no account axis to bucket - declaring the
     * general ledger's correspondence there is a mistake, not a silently ignored key.
     */
    @Test
    void correspondenceOnAStatementIsRejected() {
        IntentValidationException error = assertThrows(IntentValidationException.class,
                () -> IntentParser.parse(GENERAL_LEDGER_INTENT.replace("kind: balance", "kind: statement")));
        assertTrue(error.getMessage()
                        .contains("must not declare correspondence - the general ledger axis belongs to kind: balance"),
                error.getMessage());
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

    private static final String GENERAL_LEDGER_INTENT = LEDGER_INTENT.replace("""
                dimensions: [account.code, account.name]
                filter: "credit == 0"
            """, """
                dimensions: [account.code, account.name]
                correspondence: account.code
            """);

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
        assertTrue(message.contains("declares date/debit/credit/correspondence but is not kind: balance"), message);
        assertTrue(message.contains("unknown kind [pivot]"), message);
    }

    private static final String STATEMENT_INTENT = """
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
              - name: BalanceSheet
                kind: statement
                source: JournalEntryItem
                date: journalEntry.entryDate
                debit: debit
                credit: credit
                account: account.code
                lines:
                  - { code: A.I,  label: Fixed assets, accounts: "20*,21*", measure: closingNetDebit }
                  - { code: A.II, label: Receivables,  accounts: "41*",     measure: closingNetDebit }
                  - { code: A,    label: Total assets, sum: [A.I, A.II] }
            """;

    @Test
    @SuppressWarnings("unchecked")
    void statementReportEmitsOneRowPerLineOverThePerAccountBalances() {
        IntentModel model = IntentParser.parse(STATEMENT_INTENT);
        Map<String, Object> document = ReportIntentGenerator.buildForTest(TestContexts.context(model), model.getReports()
                                                                                                            .get(0));

        assertEquals("statement", document.get("kind"));

        String query = (String) document.get("query");
        // The ledger is reduced to one balance per account FIRST: a net measure nets an account's two
        // sides before a line sums it, so the reduction cannot happen per line.
        assertTrue(query.contains("WITH \"ACCOUNT_BALANCES\" as (\nSELECT Account.\"ACCOUNT_CODE\" as \"ACCOUNT_CODE\""), query);
        assertTrue(query.contains("GROUP BY Account.\"ACCOUNT_CODE\""), query);
        // The windows are the balance report's, to the token.
        assertTrue(query.contains(
                "SUM(CASE WHEN JournalEntry.\"JOURNAL_ENTRY_ENTRY_DATE\" <= :toDate THEN COALESCE(JournalEntryItem.\"JOURNAL_ENTRY_ITEM_DEBIT\", 0) ELSE 0 END) as \"CLOSING_DEBIT\""),
                query);
        // A comma-separated selector is an OR of prefixes; a net measure floors the account at zero.
        assertTrue(query.contains(
                "COALESCE(SUM(CASE WHEN (\"ACCOUNT_CODE\" LIKE '20%' OR \"ACCOUNT_CODE\" LIKE '21%') THEN CASE WHEN \"CLOSING_DEBIT\" - \"CLOSING_CREDIT\" > 0 THEN \"CLOSING_DEBIT\" - \"CLOSING_CREDIT\" ELSE 0 END ELSE 0 END), 0)"),
                query);
        // A computed line is FLATTENED into its leaves' own terms, so no line waits for another.
        assertTrue(query.contains(
                "CAST('A' AS VARCHAR(255)) as \"Code\", CAST('Total assets' AS VARCHAR(4000)) as \"Label\", COALESCE(SUM(CASE WHEN (\"ACCOUNT_CODE\" LIKE '20%'"),
                query);
        assertTrue(query.contains("ELSE 0 END), 0) + COALESCE(SUM(CASE WHEN \"ACCOUNT_CODE\" LIKE '41%'"), query);
        assertTrue(query.endsWith("ORDER BY \"STATEMENT_LINES\".\"Ordinal\""), query);

        // Code / Label / Amount - the amount right-aligned and money-formatted like every decimal.
        List<Map<String, Object>> columns = (List<Map<String, Object>>) document.get("columns");
        assertEquals(3, columns.size());
        assertEquals(List.of("Code", "Label", "Amount"), columns.stream()
                                                                .map(column -> column.get("alias"))
                                                                .toList());
        assertEquals("DECIMAL", columns.get(2)
                                       .get("type"));
        assertEquals("### ### ### ##0.00", columns.get(2)
                                                  .get("pattern"));

        // The window parameters are the balance report's, so a statement is queried the same way.
        List<Map<String, Object>> parameters = (List<Map<String, Object>>) document.get("parameters");
        assertEquals(2, parameters.size());
        assertEquals("fromDate", parameters.get(0)
                                           .get("name"));
        assertEquals("toDate", parameters.get(1)
                                         .get("name"));

        // A statement's joins live inside its own subquery, so the builder-owned model is deliberately
        // absent and the report editor opens it free-style rather than rewriting it into a flat SELECT.
        assertFalse(document.containsKey("joins"), "a statement must not claim builder-owned joins");
        assertFalse(document.containsKey("conditions"), "a statement must not claim builder-owned conditions");
    }

    @Test
    void statementLinesMustBeWellFormed() {
        IntentValidationException error = assertThrows(IntentValidationException.class, () -> IntentParser.parse("""
                name: ledger
                entities:
                  - name: Account
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: code, type: integer }
                  - name: JournalEntryItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: debit, type: decimal }
                      - { name: credit, type: decimal }
                      - { name: entryDate, type: date }
                    relations:
                      - { name: account, kind: manyToOne, to: Account, required: true }
                reports:
                  - name: BalanceSheet
                    kind: statement
                    source: JournalEntryItem
                    date: entryDate
                    debit: debit
                    credit: credit
                    account: account.code
                    dimensions: [debit]
                    lines:
                      - { code: A, label: Assets, accounts: "20*", measure: closingBalance }
                      - { code: A, label: Repeat, accounts: "21*", measure: closingNetDebit }
                      - { code: B, label: Both,   accounts: "22*", measure: closingNetDebit, sum: [A] }
                      - { code: C, label: Neither }
                      - { code: D, label: Missing, sum: [Nope] }
                      - { code: E, label: Bad range, accounts: "60-699", measure: closingNetDebit }
                      - { code: F, label: Injected, accounts: "20';DROP", measure: closingNetDebit }
                """));
        String message = error.getMessage();
        assertTrue(message.contains("must not declare dimensions"), message);
        assertTrue(message.contains("account [account.code] must be a string field"), message);
        assertTrue(message.contains("unknown measure [closingBalance]"), message);
        assertTrue(message.contains("declares the line code [A] twice"), message);
        assertTrue(message.contains("both selects accounts and sums other lines"), message);
        assertTrue(message.contains("line [C] neither selects accounts"), message);
        assertTrue(message.contains("references the line [Nope], which the statement does not declare"), message);
        assertTrue(message.contains("bounds are of different length"), message);
        assertTrue(message.contains("contains [']"), message);
    }

    @Test
    void statementLineArithmeticMustNotFormACycle() {
        IntentValidationException error = assertThrows(IntentValidationException.class, () -> IntentParser.parse("""
                name: ledger
                entities:
                  - name: Account
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: code, type: string }
                  - name: JournalEntryItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: debit, type: decimal }
                      - { name: credit, type: decimal }
                      - { name: entryDate, type: date }
                    relations:
                      - { name: account, kind: manyToOne, to: Account, required: true }
                reports:
                  - name: BalanceSheet
                    kind: statement
                    source: JournalEntryItem
                    date: entryDate
                    debit: debit
                    credit: credit
                    account: account.code
                    lines:
                      - { code: A, label: A, sum: [B] }
                      - { code: B, label: B, sum: [A] }
                """));
        assertTrue(error.getMessage()
                        .contains("cycle in its line arithmetic"),
                error.getMessage());
    }

    @Test
    void statementInputsWithoutTheKindAreRejected() {
        IntentValidationException error = assertThrows(IntentValidationException.class, () -> IntentParser.parse("""
                name: ledger
                entities:
                  - name: JournalEntryItem
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: code, type: string }
                reports:
                  - name: Totals
                    source: JournalEntryItem
                    account: code
                """));
        assertTrue(error.getMessage()
                        .contains("declares account/lines but is not kind: statement"),
                error.getMessage());
    }

    private static final String AGEING_INTENT = """
            name: billing
            entities:
              - name: Customer
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: due, type: date }
                  - { name: balance, type: decimal }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }
            reports:
              - name: Receivables
                source: Invoice
                dimensions: ["ageing(due, [30, 60, 90])"]
                measures: ["sum(balance)"]
            """;

    /**
     * The ageing bucket must be emitted as a CASE over DATE BOUNDARIES. Day-count arithmetic
     * (`CURRENT_DATE - due`) is not portable - PostgreSQL yields an integer, H2 an INTERVAL - and the
     * .report query is a static string with no dialect to switch on.
     */
    @Test
    void ageingDimensionEmitsPortableDateBoundaryBuckets() {
        IntentModel model = IntentParser.parse(AGEING_INTENT);
        Map<String, Object> document = ReportIntentGenerator.buildForTest(TestContexts.context(model), model.getReports()
                                                                                                            .get(0));
        String query = (String) document.get("query");
        assertTrue(query.contains("CASE WHEN Invoice.\"INVOICE_DUE\" IS NULL THEN 'n/a'"),
                "a null date must bucket as n/a, not as maximally overdue: " + query);
        assertTrue(query.contains("Invoice.\"INVOICE_DUE\" > CURRENT_DATE - INTERVAL '30' DAY THEN '0-30'"), query);
        assertTrue(query.contains("Invoice.\"INVOICE_DUE\" > CURRENT_DATE - INTERVAL '60' DAY THEN '31-60'"), query);
        assertTrue(query.contains("Invoice.\"INVOICE_DUE\" > CURRENT_DATE - INTERVAL '90' DAY THEN '61-90'"), query);
        assertTrue(query.contains("ELSE '90+' END"), query);
        // Never day-count arithmetic - that is the non-portable form.
        assertTrue(!query.contains("CURRENT_DATE - Invoice"), query);
        // An aggregated report must GROUP BY the whole bucket expression, not the raw column.
        assertTrue(query.contains("GROUP BY CASE WHEN"), query);
    }

    @Test
    void ageingThresholdsMustAscendAndBePositive() {
        String yaml = AGEING_INTENT.replace("[30, 60, 90]", "[60, 30]");
        org.eclipse.dirigible.components.intent.parser.IntentValidationException ex = org.junit.jupiter.api.Assertions.assertThrows(
                org.eclipse.dirigible.components.intent.parser.IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains("thresholds must ascend")),
                "expected an ascending-thresholds issue, got: " + ex.getIssues());
    }

    /**
     * A report has no field-level scoping, so one that sums a {@code visibleTo:} field serves that
     * restricted figure to everyone who may open the report. Legitimate to author, so it is reported as
     * a generation warning naming the report, the field and the roles - never silently.
     */
    @Test
    void aReportOverARestrictedFieldIsReported() {
        String yaml = """
                name: hr
                permissions:
                  - { role: Payroll }
                entities:
                  - name: Department
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string, length: 100 }
                  - name: Employee
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: dailyRate, type: decimal, visibleTo: [Payroll] }
                      - { name: headcount, type: integer }
                    relations:
                      - { name: Department, kind: manyToOne, to: Department }
                reports:
                  - name: PayrollByDepartment
                    source: Employee
                    dimensions: ["Department.name"]
                    measures: ["sum(dailyRate)"]
                  - name: HeadcountByDepartment
                    source: Employee
                    dimensions: ["Department.name"]
                    measures: ["sum(headcount)"]
                """;
        IntentModel model = IntentParser.parse(yaml);
        org.eclipse.dirigible.components.intent.generator.IntentGenerationContext context = TestContexts.context(model);
        ReportIntentGenerator.buildForTest(context, model.getReports()
                                                         .get(0));
        assertTrue(context.getIssues()
                          .stream()
                          .anyMatch(issue -> issue.contains("PayrollByDepartment") && issue.contains("Employee.dailyRate")
                                  && issue.contains("[Payroll]")),
                "the re-exposing report should be reported, got: " + context.getIssues());

        org.eclipse.dirigible.components.intent.generator.IntentGenerationContext plain = TestContexts.context(model);
        ReportIntentGenerator.buildForTest(plain, model.getReports()
                                                       .get(1));
        assertTrue(plain.getIssues()
                        .isEmpty(),
                "a report touching no restricted field has nothing to report, got: " + plain.getIssues());
    }

    @Test
    void ageingRequiresATemporalField() {
        String yaml = AGEING_INTENT.replace("ageing(due, [30, 60, 90])", "ageing(balance, [30, 60, 90])");
        org.eclipse.dirigible.components.intent.parser.IntentValidationException ex = org.junit.jupiter.api.Assertions.assertThrows(
                org.eclipse.dirigible.components.intent.parser.IntentValidationException.class, () -> IntentParser.parse(yaml));
        assertTrue(ex.getIssues()
                     .stream()
                     .anyMatch(issue -> issue.contains("must be a date/timestamp field")),
                "expected a temporal-field issue, got: " + ex.getIssues());
    }

    /**
     * A multilingual nomenclature reported on from both sides: as a related label and as the source.
     */
    private static final String MULTILINGUAL_INTENT = """
            name: shop
            entities:
              - name: Unit
                kind: setting
                multilingual: true
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
                  - { name: code, type: string }
                  - { name: factor, type: decimal }
              - name: Line
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string }
                  - { name: quantity, type: decimal }
                relations:
                  - { name: Unit, kind: manyToOne, to: Unit }
            reports:
              - name: QuantityByUnit
                source: Line
                dimensions: [Unit, Unit.code, Unit.factor, note]
                measures: ["sum(quantity)"]
                filter: "Unit.code != 'X'"
              - name: UnitFactors
                source: Unit
                dimensions: [name, factor]
            """;

    @Test
    void translatableColumnsOfAMultilingualEntityAreOverlaidForTheRequestLanguage() {
        IntentModel model = IntentParser.parse(MULTILINGUAL_INTENT);
        String query = (String) ReportIntentGenerator.buildForTest(TestContexts.context(model), model.getReports()
                                                                                                     .get(0))
                                                     .get("query");

        // One LEFT join to the language table, keyed on the base row and the request language - LEFT so
        // an untranslated row (or a caller with no language) still shows, with its base value.
        assertEquals(1, query.split("LEFT JOIN", -1).length - 1, query);
        assertTrue(query.contains(
                "LEFT JOIN \"SHOP_UNIT_LANG\" as Unit_LANG ON Unit_LANG.\"Id\" = Unit.\"UNIT_ID\" AND Unit_LANG.\"Language\" = :language"),
                query);
        // The related nomenclature's label - the column the issue was raised about.
        assertTrue(query.contains("COALESCE(Unit_LANG.\"Name\", Unit.\"UNIT_NAME\") as \"Unit\""), query);
        // ... and any other translatable property reached through the relation.
        assertTrue(query.contains("COALESCE(Unit_LANG.\"Code\", Unit.\"UNIT_CODE\") as \"Unit Code\""), query);
        // A non-translatable property has no language column, so it stays on the base table.
        assertTrue(query.contains("Unit.\"UNIT_FACTOR\" as \"Unit Factor\""), query);
        assertFalse(query.contains("Unit_LANG.\"Factor\""), query);
        // The source entity is not multilingual, so its own columns are untouched.
        assertTrue(query.contains("Line.\"LINE_NOTE\" as \"Note\""), query);
        // An aggregated report groups by what it selects, or the translated column is not grouped at all.
        assertTrue(query.contains(
                "GROUP BY COALESCE(Unit_LANG.\"Name\", Unit.\"UNIT_NAME\"), COALESCE(Unit_LANG.\"Code\", Unit.\"UNIT_CODE\"), Unit.\"UNIT_FACTOR\", Line.\"LINE_NOTE\""),
                query);
        // The overlay belongs to the SELECT list only: the filter still compiles against the BASE
        // table, which is why translating a nomenclature can never change what a report matches.
        assertTrue(query.contains("WHERE Unit.\"UNIT_CODE\" != 'X'"), query);
    }

    @Test
    void aPlainFieldOfAMultilingualSourceIsOverlaidToo() {
        IntentModel model = IntentParser.parse(MULTILINGUAL_INTENT);
        String query = (String) ReportIntentGenerator.buildForTest(TestContexts.context(model), model.getReports()
                                                                                                     .get(1))
                                                     .get("query");

        assertTrue(query.contains(
                "LEFT JOIN \"SHOP_UNIT_LANG\" as Unit_LANG ON Unit_LANG.\"Id\" = Unit.\"UNIT_ID\" AND Unit_LANG.\"Language\" = :language"),
                query);
        assertTrue(query.contains("COALESCE(Unit_LANG.\"Name\", Unit.\"UNIT_NAME\") as \"Name\""), query);
        assertTrue(query.contains("Unit.\"UNIT_FACTOR\" as \"Factor\""), query);
    }

    @Test
    void aReportOverAPlainEntityBindsNoLanguageParameter() {
        IntentModel model = IntentParser.parse(MULTILINGUAL_INTENT.replace("    multilingual: true\n", ""));
        String query = (String) ReportIntentGenerator.buildForTest(TestContexts.context(model), model.getReports()
                                                                                                     .get(0))
                                                     .get("query");

        // Nothing to overlay, nothing to bind: the generated repository keys its language binding off
        // the query itself, so an unchanged model must stay byte-identical to what it emitted before.
        assertFalse(query.contains(":language"), query);
        assertFalse(query.contains("LEFT JOIN"), query);
    }
}
