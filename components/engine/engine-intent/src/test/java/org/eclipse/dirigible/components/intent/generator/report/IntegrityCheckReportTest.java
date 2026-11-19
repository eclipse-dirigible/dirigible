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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.generator.TestContexts;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.junit.jupiter.api.Test;

/**
 * The integrity-check conditions of a report (dirigible #7914): {@code having:} keeps the groups an
 * aggregate condition holds for, {@code notExists:} keeps the rows no row of another entity
 * matches. The generated queries are RUN on H2 over a small book, so the assertions are the rows an
 * accountant would be shown before a period close.
 */
class IntegrityCheckReportTest {

    private static final String MODEL = """
            name: books
            uses:
              - { model: journal }
            entities:
              - name: Supplier
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Company
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: PurchaseInvoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: date, type: date }
                  - { name: supplierNumber, type: string }
                  - { name: total, type: decimal }
                relations:
                  - { name: Supplier, kind: manyToOne, to: Supplier, required: true }
              - name: Payment
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string }
                  - { name: amount, type: decimal }
              - name: Allocation
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: Payment, kind: manyToOne, to: Payment, required: true }
                  - { name: PurchaseInvoice, kind: manyToOne, to: PurchaseInvoice }
              - name: FiscalYear
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: startDate, type: date }
                  - { name: endDate, type: date }
                relations:
                  - { name: Company, kind: manyToOne, to: Company, required: true }
              - name: Posting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string }
                  - { name: postedOn, type: timestamp }
                relations:
                  - { name: Company, kind: manyToOne, to: Company }
            reports:
            #REPORT#
            """;

    private static final String[] SCHEMA = {
            "CREATE TABLE \"BOOKS_SUPPLIER\" (\"SUPPLIER_ID\" INT PRIMARY KEY, \"SUPPLIER_NAME\" VARCHAR(100))",
            "CREATE TABLE \"BOOKS_COMPANY\" (\"COMPANY_ID\" INT PRIMARY KEY, \"COMPANY_NAME\" VARCHAR(100))",
            "CREATE TABLE \"BOOKS_PURCHASE_INVOICE\" (\"PURCHASE_INVOICE_ID\" INT PRIMARY KEY, \"PURCHASE_INVOICE_DATE\" DATE,"
                    + " \"PURCHASE_INVOICE_SUPPLIER_NUMBER\" VARCHAR(50), \"PURCHASE_INVOICE_TOTAL\" DECIMAL(19,2),"
                    + " \"PURCHASE_INVOICE_SUPPLIER\" INT)",
            "CREATE TABLE \"BOOKS_PAYMENT\" (\"PAYMENT_ID\" INT PRIMARY KEY, \"PAYMENT_NUMBER\" VARCHAR(50), \"PAYMENT_AMOUNT\" DECIMAL(19,2))",
            "CREATE TABLE \"BOOKS_ALLOCATION\" (\"ALLOCATION_ID\" INT PRIMARY KEY, \"ALLOCATION_AMOUNT\" DECIMAL(19,2),"
                    + " \"ALLOCATION_PAYMENT\" INT, \"ALLOCATION_PURCHASE_INVOICE\" INT)",
            "CREATE TABLE \"BOOKS_FISCAL_YEAR\" (\"FISCAL_YEAR_ID\" INT PRIMARY KEY, \"FISCAL_YEAR_START_DATE\" DATE,"
                    + " \"FISCAL_YEAR_END_DATE\" DATE, \"FISCAL_YEAR_COMPANY\" INT)",
            "CREATE TABLE \"BOOKS_POSTING\" (\"POSTING_ID\" INT PRIMARY KEY, \"POSTING_NUMBER\" VARCHAR(50), \"POSTING_POSTED_ON\" TIMESTAMP,"
                    + " \"POSTING_COMPANY\" INT)",
            // The journal model's entries, each pointing back at the invoice it books.
            "CREATE TABLE \"JOURNAL_JOURNAL_ENTRY\" (\"JOURNAL_ENTRY_ID\" INT PRIMARY KEY, \"JOURNAL_ENTRY_PURCHASE_INVOICE\" INT,"
                    + " \"JOURNAL_ENTRY_REVERSED\" BOOLEAN)",
            "INSERT INTO \"BOOKS_SUPPLIER\" VALUES (1, 'Acme'), (2, 'Beta')",
            "INSERT INTO \"BOOKS_COMPANY\" VALUES (1, 'North'), (2, 'South')", "INSERT INTO \"BOOKS_PURCHASE_INVOICE\" VALUES"
                    // Acme's A-1 twice in 2026: the duplicate.
                    + " (1, DATE '2026-02-01', 'A-1', 100, 1), (2, DATE '2026-03-01', 'A-1', 100, 1),"
                    // ...and once again in 2025: another year, no duplicate.
                    + " (3, DATE '2025-03-01', 'A-1', 50, 1),"
                    // Beta's A-1 is another supplier's number.
                    + " (4, DATE '2026-02-01', 'A-1', 70, 2), (5, DATE '2026-02-01', 'B-7', 30, 2)",
            "INSERT INTO \"BOOKS_PAYMENT\" VALUES (1, 'P-1', 100), (2, 'P-2', 50), (3, 'P-3', 20)",
            // P-1 is allocated; P-3 only by a zero allocation, which the filtered check does not count.
            "INSERT INTO \"BOOKS_ALLOCATION\" VALUES (1, 100, 1, 1), (2, 0, 3, 2)",
            "INSERT INTO \"BOOKS_FISCAL_YEAR\" VALUES" + " (1, DATE '2026-01-01', DATE '2026-12-31', 1),"
            // South's current year is still open-ended.
                    + " (2, DATE '2026-01-01', NULL, 2)",
            "INSERT INTO \"BOOKS_POSTING\" VALUES"
                    // inside North's year - on its last day, at noon.
                    + " (1, 'E-1', TIMESTAMP '2026-12-31 12:00:00', 1),"
                    // North has no 2025 year.
                    + " (2, 'E-2', TIMESTAMP '2025-12-31 12:00:00', 1),"
                    // inside South's open year.
                    + " (3, 'E-3', TIMESTAMP '2027-06-01 08:00:00', 2),"
                    // no company - it lies in no year of its company.
                    + " (4, 'E-4', TIMESTAMP '2026-05-01 08:00:00', NULL)",
            // Invoice 1 is booked; invoice 2's entry is reversed; the others have none.
            "INSERT INTO \"JOURNAL_JOURNAL_ENTRY\" VALUES (1, 1, FALSE), (2, 2, TRUE)"};

    private static IntentModel parse(String report) {
        return IntentParser.parse(MODEL.replace("#REPORT#", report));
    }

    private static Map<String, Object> document(IntentModel model) {
        return ReportIntentGenerator.buildForTest(TestContexts.context(model), model.getReports()
                                                                                    .get(0));
    }

    @Test
    void havingKeepsOnlyTheDuplicatedSupplierNumbers() throws SQLException {
        IntentModel model = parse("""
                  - name: DuplicateSupplierNumbers
                    source: PurchaseInvoice
                    dimensions: [Supplier, "year(date)", supplierNumber]
                    measures: ["count(*)"]
                    having: "count(*) > 1"
                """);
        Map<String, Object> document = document(model);
        String query = (String) document.get("query");
        assertTrue(
                query.contains("\nGROUP BY Supplier.\"SUPPLIER_NAME\", EXTRACT(YEAR FROM PurchaseInvoice.\"PURCHASE_INVOICE_DATE\"),"
                        + " PurchaseInvoice.\"PURCHASE_INVOICE_SUPPLIER_NUMBER\", Supplier.\"SUPPLIER_ID\"\nHAVING COUNT(*) > 1\nORDER BY"),
                query);
        assertEquals(List.of(Map.of("left", "COUNT(*)", "operation", ">", "right", "1")), document.get("havings"),
                "the editor's own HAVING rows, so its builder rebuilds the same query");

        assertEquals(List.of(List.of("Acme", "2026", "A-1", "2")), run(model));
    }

    @Test
    void havingAggregatesAFieldThroughItsQualifiedColumn() throws SQLException {
        IntentModel model = parse("""
                  - name: LargeSuppliers
                    source: PurchaseInvoice
                    dimensions: [Supplier]
                    measures: ["sum(total)"]
                    having: "sum(total) >= 150 and count(*) > 1"
                """);
        String query = (String) document(model).get("query");
        assertTrue(query.endsWith("\nHAVING SUM(PurchaseInvoice.\"PURCHASE_INVOICE_TOTAL\") >= 150 and COUNT(*) > 1"), query);
        assertEquals(List.of(List.of("Acme", "250.00")), run(model));
    }

    @Test
    void notExistsListsThePaymentsWithNoAllocation() throws SQLException {
        IntentModel model = parse("""
                  - name: UnallocatedPayments
                    source: Payment
                    dimensions: [number]
                    notExists: { entity: Allocation, match: { Payment: id } }
                """);
        Map<String, Object> document = document(model);
        String query = (String) document.get("query");
        assertTrue(query.endsWith("\nWHERE NOT EXISTS (SELECT 1 FROM \"BOOKS_ALLOCATION\" as AllocationAbsent"
                + " WHERE AllocationAbsent.\"ALLOCATION_PAYMENT\" = Payment.\"PAYMENT_ID\")"), query);
        assertNull(document.get("conditions"), "a NOT EXISTS is no condition row - the report opens free-style");
        assertNull(document.get("drill"), "the source's list cannot repeat the anti-join");
        // P-3's zero allocation is an allocation all the same - see the filtered variant below.
        assertEquals(List.of(List.of("P-2")), run(model));
    }

    @Test
    void theTargetsOwnFilterDecidesWhichOfItsRowsCount() throws SQLException {
        IntentModel model = parse("""
                  - name: UnallocatedPayments
                    source: Payment
                    dimensions: [number]
                    filter: "amount > 10 OR number = 'P-1'"
                    notExists: { entity: Allocation, match: { Payment: id }, filter: "amount <> 0" }
                """);
        String query = (String) document(model).get("query");
        assertTrue(query.contains("WHERE (Payment.\"PAYMENT_AMOUNT\" > 10 OR Payment.\"PAYMENT_NUMBER\" = 'P-1') AND NOT EXISTS ("), query);
        assertTrue(query.contains("AND (AllocationAbsent.\"ALLOCATION_AMOUNT\" <> 0))"), query);
        assertEquals(List.of(List.of("P-2"), List.of("P-3")), run(model));
    }

    @Test
    void notExistsWithAWindowListsThePostingsOutsideEveryFiscalYearOfTheirCompany() throws SQLException {
        IntentModel model = parse("""
                  - name: PostingsOutsideTheirYear
                    source: Posting
                    dimensions: [number]
                    notExists:
                      entity: FiscalYear
                      match: { Company: Company }
                      between: { start: startDate, end: endDate, value: postedOn }
                """);
        String query = (String) document(model).get("query");
        assertTrue(query.contains("FiscalYearAbsent.\"FISCAL_YEAR_COMPANY\" = Posting.\"POSTING_COMPANY\""), query);
        assertTrue(query.contains("(FiscalYearAbsent.\"FISCAL_YEAR_END_DATE\" IS NULL OR CAST(FiscalYearAbsent.\"FISCAL_YEAR_END_DATE\""
                + " AS DATE) >= CAST(Posting.\"POSTING_POSTED_ON\" AS DATE))"), query);
        assertEquals(List.of(List.of("E-2"), List.of("E-4")), run(model));
    }

    @Test
    void aTargetOfAnotherModelIsReadFromItsOwnersTable() throws SQLException {
        IntentModel model = parse("""
                  - name: InvoicesWithoutEntry
                    source: PurchaseInvoice
                    dimensions: [id]
                    notExists: { entity: JournalEntry, model: journal, match: { PurchaseInvoice: id } }
                """);
        String query = (String) document(model).get("query");
        assertTrue(query.contains("NOT EXISTS (SELECT 1 FROM \"JOURNAL_JOURNAL_ENTRY\" as JournalEntryAbsent WHERE"
                + " JournalEntryAbsent.\"JOURNAL_ENTRY_PURCHASE_INVOICE\" = PurchaseInvoice.\"PURCHASE_INVOICE_ID\""), query);
        // Invoices 1 and 2 have an entry. (The owner's property list - which a target filter is rewritten
        // against - needs the owner's published .model, so the filter is exercised end to end in the IT.)
        assertEquals(List.of(List.of("3"), List.of("4"), List.of("5")), run(model));
    }

    @Test
    void bothConditionsCombineInOneAggregatingReport() throws SQLException {
        // Per supplier: how many invoices no allocation pays, listed only when more than one.
        IntentModel model = parse("""
                  - name: UnpaidPerSupplier
                    source: PurchaseInvoice
                    dimensions: [Supplier]
                    measures: ["count(*)"]
                    notExists: { entity: Allocation, match: { PurchaseInvoice: id } }
                    having: "count(*) > 1"
                """);
        String query = (String) document(model).get("query");
        assertTrue(query.indexOf("NOT EXISTS") < query.indexOf("GROUP BY") && query.indexOf("GROUP BY") < query.indexOf("HAVING"), query);
        assertEquals(List.of(List.of("Beta", "2")), run(model));
    }

    @Test
    void aReportWithoutEitherKeyIsUnchanged() {
        String query = (String) document(parse("""
                  - name: Plain
                    source: Payment
                    dimensions: [number]
                    filter: "amount > 10"
                """)).get("query");
        assertFalse(query.contains("HAVING") || query.contains("EXISTS"), query);
        assertEquals(
                "SELECT Payment.\"PAYMENT_NUMBER\" as \"Number\"\nFROM \"BOOKS_PAYMENT\" as Payment\nWHERE Payment.\"PAYMENT_AMOUNT\" > 10",
                query);
    }

    @Test
    void anIllFormedConditionIsRefusedAtParse() {
        assertRefused("""
                  - name: R
                    source: Payment
                    dimensions: [number]
                    having: "count(*) > 1"
                """, "having [count(*) > 1] filters the report's groups, so the report must aggregate");
        assertRefused("""
                  - name: R
                    source: Payment
                    dimensions: [number]
                    measures: ["count(*)"]
                    having: "amount > 1"
                """, "names [amount] outside an aggregate call");
        assertRefused("""
                  - name: R
                    source: Payment
                    dimensions: [number]
                    measures: ["count(*)"]
                    having: "sum(nowhere) > 1"
                """, "aggregate field [nowhere] is not a field of [Payment]");
        assertRefused("""
                  - name: R
                    source: Payment
                    notExists: { entity: Nowhere, match: { Payment: id } }
                """, "notExists entity [Nowhere] is not an entity of this model");
        assertRefused("""
                  - name: R
                    source: Payment
                    notExists: { entity: Allocation, match: { Invoice: id } }
                """, "notExists match [Invoice] is not a field or to-one relation of [Allocation]");
        assertRefused("""
                  - name: R
                    source: Payment
                    notExists: { entity: Allocation, match: { Payment: identifier } }
                """, "notExists match [Payment] [identifier] is not a field or to-one relation of [Payment]");
        assertRefused("""
                  - name: R
                    source: Payment
                    notExists: { entity: Allocation }
                """, "notExists declares neither match: nor between:");
        assertRefused("""
                  - name: R
                    source: Posting
                    notExists: { entity: FiscalYear, between: { start: startDate, value: postedOn } }
                """, "notExists between has no end");
        assertRefused("""
                  - name: R
                    source: Posting
                    notExists: { entity: FiscalYear, between: { start: Company, end: endDate, value: number } }
                """, "notExists between value [number] must be a date/timestamp field");
        assertRefused("""
                  - name: R
                    source: Posting
                    notExists: { entity: FiscalYear, between: { from: startDate, end: endDate, value: postedOn } }
                """, "unknown key [from]");
        assertRefused("""
                  - name: R
                    source: Payment
                    notExists: { entity: JournalEntry, model: ledger, match: { Payment: id } }
                """, "notExists uses unknown model alias [ledger]");
        assertRefused("""
                  - name: R
                    union:
                      - { source: Payment, amount: amount }
                    notExists: { entity: Allocation, match: { Payment: source } }
                """, "notExists cannot be combined with union:");
    }

    private static void assertRefused(String report, String expected) {
        IntentValidationException refused = assertThrows(IntentValidationException.class, () -> parse(report));
        assertTrue(refused.getMessage()
                          .contains(expected),
                "expected [" + expected + "] in: " + refused.getMessage());
    }

    /** Create the book, run the report, and read every row back as its columns' text, in order. */
    private static List<List<String>> run(IntentModel model) throws SQLException {
        String query = (String) document(model).get("query");
        List<List<String>> rows = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:integrity;DB_CLOSE_DELAY=-1");
                Statement statement = connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
            for (String ddl : SCHEMA) {
                statement.execute(ddl);
            }
            String ordered = query.contains("ORDER BY") ? query : query + "\nORDER BY 1";
            try (ResultSet result = statement.executeQuery(ordered)) {
                ResultSetMetaData meta = result.getMetaData();
                while (result.next()) {
                    List<String> row = new ArrayList<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        row.add(result.getString(i));
                    }
                    rows.add(row);
                }
            }
        }
        return rows;
    }
}
