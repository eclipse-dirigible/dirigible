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

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
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
 * A report's {@code union:} (dirigible #7946): several entities read as one source. The generated
 * view and the report over it are RUN on H2 over a small receivables book - invoices, credit notes
 * and migrated opening open items - so the assertions are the figures the report shows.
 */
class UnionReportTest {

    private static final String MODEL = """
            name: billing
            entities:
              - name: Customer
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: InvoiceStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: issuedOn, type: date }
                  - { name: due, type: date }
                  - { name: total, type: decimal }
                  - { name: paid, type: decimal }
                relations:
                  - { name: customer, kind: manyToOne, to: Customer, required: true }
                  - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
              - name: CreditNote
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: issuedOn, type: date }
                  - { name: total, type: decimal }
                  - { name: allocated, type: decimal }
                relations:
                  - { name: customer, kind: manyToOne, to: Customer, required: true }
              - name: OpeningBalance
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: documentDate, type: date }
                  - { name: dueDate, type: timestamp }
                  - { name: debit, type: decimal }
                  - { name: credit, type: decimal }
                relations:
                  - { name: customer, kind: manyToOne, to: Customer }
            reports:
            #REPORT#
            seeds:
              - name: invoice-statuses
                entity: InvoiceStatus
                rows:
                  - { id: 1, name: DRAFT, stage: draft }
                  - { id: 3, name: ISSUED, stage: live }
            """;

    private static final String AGED_RECEIVABLES = """
              - name: AgedReceivables
                union:
                  - { source: Invoice, counterparty: customer, date: issuedOn, due: due, amount: "total - paid" }
                  - { source: CreditNote, counterparty: customer, date: issuedOn, due: issuedOn, amount: "allocated - total" }
                  - { source: OpeningBalance, counterparty: customer, date: documentDate, due: dueDate, amount: "debit - credit",
                      filter: "customer IS NOT NULL" }
                dimensions: [counterparty]
                aging: { buckets: [30, 60, 90] }
            """;

    /** The receivables book, aged as of 2026-06-30. */
    private static final String[] SCHEMA = {
            "CREATE TABLE \"BILLING_CUSTOMER\" (\"CUSTOMER_ID\" INT PRIMARY KEY, \"CUSTOMER_NAME\" VARCHAR(100))",
            "CREATE TABLE \"BILLING_INVOICE\" (\"INVOICE_ID\" INT PRIMARY KEY, \"INVOICE_ISSUED_ON\" DATE, \"INVOICE_DUE\" DATE,"
                    + " \"INVOICE_TOTAL\" DECIMAL(19,2), \"INVOICE_PAID\" DECIMAL(19,2), \"INVOICE_CUSTOMER\" INT, \"INVOICE_STATUS\" INT)",
            "CREATE TABLE \"BILLING_CREDIT_NOTE\" (\"CREDIT_NOTE_ID\" INT PRIMARY KEY, \"CREDIT_NOTE_ISSUED_ON\" DATE,"
                    + " \"CREDIT_NOTE_TOTAL\" DECIMAL(19,2), \"CREDIT_NOTE_ALLOCATED\" DECIMAL(19,2), \"CREDIT_NOTE_CUSTOMER\" INT)",
            "CREATE TABLE \"BILLING_OPENING_BALANCE\" (\"OPENING_BALANCE_ID\" INT PRIMARY KEY, \"OPENING_BALANCE_DOCUMENT_DATE\" DATE,"
                    + " \"OPENING_BALANCE_DUE_DATE\" TIMESTAMP, \"OPENING_BALANCE_DEBIT\" DECIMAL(19,2),"
                    + " \"OPENING_BALANCE_CREDIT\" DECIMAL(19,2), \"OPENING_BALANCE_CUSTOMER\" INT)",
            "INSERT INTO \"BILLING_CUSTOMER\" VALUES (1, 'Acme'), (2, 'Beta')", "INSERT INTO \"BILLING_INVOICE\" VALUES"
                    // Acme: due tomorrow -> not yet due.
                    + " (1, DATE '2026-06-01', DATE '2026-07-01', 100, 0, 1, 3),"
                    // due today, partially paid -> its remaining 150 in 0-30.
                    + " (2, DATE '2026-06-01', DATE '2026-06-30', 200, 50, 1, 3),"
                    // a draft is not a receivable, however overdue.
                    + " (3, DATE '2026-01-01', DATE '2026-01-01', 999, 0, 1, 1),"
                    // Beta: 46 days past due -> 31-60.
                    + " (4, DATE '2026-05-01', DATE '2026-05-15', 60, NULL, 2, 3)",
            "INSERT INTO \"BILLING_CREDIT_NOTE\" VALUES"
                    // Acme: 30 of a 40 credit still unallocated, 20 days old -> -30 in 0-30.
                    + " (1, DATE '2026-06-10', 40, 10, 1),"
                    // Beta: fully allocated -> nothing open.
                    + " (2, DATE '2026-06-10', 60, 60, 2)",
            "INSERT INTO \"BILLING_OPENING_BALANCE\" VALUES"
                    // Acme: a migrated open invoice, aged by its OWN due date (135 days) -> 90+.
                    + " (1, DATE '2025-12-31', TIMESTAMP '2026-02-15 10:00:00', 500, 0, 1),"
                    // a supplier's opening item - no customer, so the member's filter drops it.
                    + " (2, DATE '2025-12-31', TIMESTAMP '2026-02-15 10:00:00', 0, 70, NULL)"};

    private static IntentModel parse(String report) {
        return IntentParser.parse(MODEL.replace("#REPORT#", report));
    }

    private static Map<String, Object> document(IntentModel model) {
        return ReportIntentGenerator.buildForTest(TestContexts.context(model), model.getReports()
                                                                                    .get(0));
    }

    private static Map<String, String> views(IntentModel model) {
        return ReportIntentGenerator.buildViewsForTest(TestContexts.context(model), model.getReports()
                                                                                         .get(0));
    }

    @Test
    void theAgedReceivablesAddUpEveryOpenItemOfEveryMember() throws SQLException {
        IntentModel model = parse(AGED_RECEIVABLES);
        Map<String, Map<String, BigDecimal>> report = run(model, "DATE '2026-06-30'");

        assertEquals(List.of("Acme", "Beta"), List.copyOf(report.keySet()));
        Map<String, BigDecimal> acme = report.get("Acme");
        assertAmount("100.00", acme, "Not Yet Due"); // due tomorrow
        assertAmount("120.00", acme, "Overdue 0-30"); // partially paid 150 - unallocated credit 30
        assertAmount("0.00", acme, "Overdue 31-60");
        assertAmount("0.00", acme, "Overdue 61-90");
        assertAmount("500.00", acme, "Overdue 90+"); // the opening item, by its own due date
        assertAmount("720.00", acme, "Open Amount");
        assertBucketsAddUp(acme);

        Map<String, BigDecimal> beta = report.get("Beta");
        assertAmount("60.00", beta, "Overdue 31-60");
        assertAmount("60.00", beta, "Open Amount");
        assertBucketsAddUp(beta);
    }

    @Test
    void theUnionIsAViewTheReportReadsAsItsTable() {
        IntentModel model = parse(AGED_RECEIVABLES);
        Map<String, String> views = views(model);
        assertEquals(List.of("BILLING_AGED_RECEIVABLES_UNION"), List.copyOf(views.keySet()));
        String view = views.get("BILLING_AGED_RECEIVABLES_UNION");
        assertEquals(2, view.split("UNION ALL", -1).length - 1, view);
        assertTrue(view.contains("Invoice.\"INVOICE_STATUS\" IN (3)"), "each member keeps its own lifecycle scope: " + view);
        assertTrue(view.contains("WHERE OpeningBalance.\"OPENING_BALANCE_CUSTOMER\" IS NOT NULL"), view);
        assertTrue(view.contains("CAST(OpeningBalance.\"OPENING_BALANCE_DUE_DATE\" AS DATE) as \"AGED_RECEIVABLES_DUE\""), view);
        assertFalse(view.contains(":"), "the view binds nothing - the as-of date stays on the report: " + view);

        Map<String, Object> document = document(model);
        assertEquals("BILLING_AGED_RECEIVABLES_UNION", document.get("table"));
        String query = (String) document.get("query");
        assertTrue(query.contains("FROM \"BILLING_AGED_RECEIVABLES_UNION\" as AgedReceivables"), query);
        assertTrue(query.contains("LEFT JOIN \"BILLING_CUSTOMER\" as Customer ON AgedReceivables.\"AGED_RECEIVABLES_COUNTERPARTY\""),
                query);
        assertTrue(query.contains("AgedReceivables.\"AGED_RECEIVABLES_DATE\" <= CAST(:asOf AS DATE)"), query);
        assertNull(document.get("drill"), "a union row counts several entities, so no single list can repeat it");
    }

    @Test
    void aUnionIsNotOnlyForAging() throws SQLException {
        // Sales revenue nets the issued credit notes against the invoices (base-sales-invoices#229).
        IntentModel model = parse("""
                  - name: Revenue
                    union:
                      - { source: Invoice, date: issuedOn, amount: total }
                      - { source: CreditNote, date: issuedOn, amount: "-total" }
                    dimensions: [source]
                    measures: ["sum(amount)"]
                """);
        Map<String, Map<String, BigDecimal>> report = run(model, null);

        assertAmount("360.00", report.get("Invoice"), null); // the draft is not revenue
        assertAmount("-100.00", report.get("CreditNote"), null);
    }

    @Test
    void aMemberOfAnotherModelIsReadFromItsOwnersTable() {
        IntentModel model = IntentParser.parse(MODEL.replace("name: billing\n", "name: billing\nuses:\n  - { model: sales }\n")
                                                    .replace("#REPORT#",
                                                            """
                                                                      - name: AgedReceivables
                                                                        union:
                                                                          - { source: SalesInvoice, model: sales, counterparty: customer, due: due, amount: "payable - paid" }
                                                                          - { source: OpeningBalance, counterparty: customer, due: dueDate, amount: "debit - credit" }
                                                                        dimensions: [counterparty]
                                                                        aging: { buckets: [30] }
                                                                    """));
        String view = views(model).get("BILLING_AGED_RECEIVABLES_UNION");
        assertTrue(view.contains("FROM \"SALES_SALES_INVOICE\" as SalesInvoice"), view);
        assertTrue(view.contains("COALESCE(SalesInvoice.\"SALES_INVOICE_PAYABLE\", 0) - COALESCE(SalesInvoice.\"SALES_INVOICE_PAID\", 0)"),
                view);
        assertTrue(view.contains("SalesInvoice.\"SALES_INVOICE_CUSTOMER\" as \"AGED_RECEIVABLES_COUNTERPARTY\""), view);
    }

    @Test
    void anIllFormedUnionIsRefusedAtParse() {
        assertRefused("""
                  - name: R
                    source: Invoice
                    union:
                      - { source: Invoice, amount: total }
                """, "union replaces source:");
        assertRefused("""
                  - name: R
                    union:
                      - { source: Invoice, due: due, amount: total }
                      - { source: CreditNote, amount: total }
                """, "union due is mapped by 1 of 2 members");
        assertRefused("""
                  - name: R
                    union:
                      - { source: Invoice, counterparty: customer, amount: total }
                      - { source: CreditNote, counterparty: issuedOn, amount: total }
                """, "member [CreditNote] counterparty [issuedOn] is not a to-one relation");
        assertRefused("""
                  - name: R
                    union:
                      - { source: Invoice, counterparty: Status, amount: total }
                      - { source: CreditNote, counterparty: customer, amount: total }
                """, "every member must name the same entity");
        assertRefused("""
                  - name: R
                    union:
                      - { source: Invoice, amount: "total * 2" }
                """, "member [Invoice] amount [total * 2] must be a numeric field");
        assertRefused("""
                  - name: R
                    union:
                      - { source: Invoice, amount: customer }
                """, "member [Invoice] amount field [customer] is not a field of [Invoice]");
        assertRefused("""
                  - name: R
                    union:
                      - { source: Invoice, due: total, amount: total }
                """, "member [Invoice] due [total] must be a date/timestamp field");
        assertRefused("""
                  - name: R
                    union:
                      - { source: Nowhere, amount: total }
                """, "member [Nowhere] is not an entity of this model");
        assertRefused("""
                  - name: R
                    union:
                      - { source: SalesInvoice, model: sales, amount: total }
                """, "member [SalesInvoice] uses unknown model alias [sales]");
        assertRefused("""
                  - name: R
                    union:
                      - { source: Invoice, amount: total }
                    dimensions: [customer]
                    aging: { buckets: [30] }
                """, "aging needs every union member to map its due date");
        assertRefused("""
                  - name: R
                    union:
                      - { source: Invoice, due: due, amount: total }
                    aging: { due: due, amount: total, buckets: [30] }
                """, "aging over a union reads each member's due, amount and date");
        assertRefused("""
                  - name: R
                    union:
                      - { source: Invoice, amount: total }
                    parameters:
                      - { name: from, target: issuedOn, op: ge }
                """, "parameter [from] field [issuedOn] is not a field of [R]");
    }

    @Test
    void aLeadingSignNegatesTheFirstTerm() {
        ReportIntent report = parse("""
                  - name: R
                    union:
                      - { source: CreditNote, amount: "-total + allocated" }
                """).getReports()
                    .get(0);
        assertEquals(2, org.eclipse.dirigible.components.intent.model.ReportAgingIntent.amountTerms(report.getUnion()
                                                                                                          .get(0)
                                                                                                          .getAmount())
                                                                                       .size());
        assertNull(org.eclipse.dirigible.components.intent.model.ReportAgingIntent.amountTerms("-"));
        assertNull(org.eclipse.dirigible.components.intent.model.ReportAgingIntent.amountTerms("total - - paid"));
    }

    private static void assertRefused(String report, String expected) {
        IntentValidationException refused = assertThrows(IntentValidationException.class, () -> parse(report));
        assertTrue(refused.getMessage()
                          .contains(expected),
                "expected [" + expected + "] in: " + refused.getMessage());
    }

    private static void assertBucketsAddUp(Map<String, BigDecimal> row) {
        BigDecimal buckets = row.entrySet()
                                .stream()
                                .filter(entry -> !"Open Amount".equals(entry.getKey()))
                                .map(Map.Entry::getValue)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, buckets.compareTo(row.get("Open Amount")), "the buckets must add up to the open amount: " + row);
    }

    /** {@code column} null reads the row's only value column. */
    private static void assertAmount(String expected, Map<String, BigDecimal> row, String column) {
        BigDecimal actual = column == null ? row.values()
                                                .iterator()
                                                .next()
                : row.get(column);
        assertEquals(0, new BigDecimal(expected).compareTo(actual), column + " in " + row);
    }

    /**
     * Create the book and the generated views, run the report with the as-of date bound as a literal,
     * and read every row back as first column -> column -> amount.
     */
    private static Map<String, Map<String, BigDecimal>> run(IntentModel model, String asOf) throws SQLException {
        String query = (String) document(model).get("query");
        if (asOf != null) {
            query = query.replace(":asOf", asOf);
        }
        Map<String, Map<String, BigDecimal>> report = new LinkedHashMap<>();
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:union;DB_CLOSE_DELAY=-1");
                Statement statement = connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
            for (String ddl : SCHEMA) {
                statement.execute(ddl);
            }
            for (Map.Entry<String, String> view : views(model).entrySet()) {
                statement.execute("CREATE VIEW \"" + view.getKey() + "\" AS " + view.getValue());
            }
            try (ResultSet rows = statement.executeQuery(query)) {
                ResultSetMetaData meta = rows.getMetaData();
                while (rows.next()) {
                    Map<String, BigDecimal> row = new LinkedHashMap<>();
                    for (int i = 2; i <= meta.getColumnCount(); i++) {
                        row.put(meta.getColumnLabel(i), rows.getBigDecimal(i));
                    }
                    report.put(rows.getString(1), row);
                }
            }
        }
        return report;
    }
}
