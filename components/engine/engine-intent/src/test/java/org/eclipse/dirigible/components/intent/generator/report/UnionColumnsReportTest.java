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
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.junit.jupiter.api.Test;

/**
 * An as-of aging over a {@code union:} (dirigible #8619): an allocation row is a member - its
 * counterparty and currency one hop away through its invoice, its date the date of a payment
 * another model owns - and the union carries the currency as a column of its own, so the report
 * ages each invoice minus only the payments dated on or before the as-of date, per customer AND
 * currency. The generated view and report are RUN on H2.
 */
class UnionColumnsReportTest {

    private static final String MODEL = """
            name: billing
            uses:
              - { model: payments }
            entities:
              - name: Customer
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Currency
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: code, type: string }
              - name: InvoiceStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: SalesInvoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: date, type: date }
                  - { name: due, type: date }
                  - { name: payable, type: decimal }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer, required: true }
                  - { name: Currency, kind: manyToOne, to: Currency, required: true }
                  - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
              - name: CreditNote
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: date, type: date }
                  - { name: total, type: decimal }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer, required: true }
                  - { name: Currency, kind: manyToOne, to: Currency, required: true }
                  - { name: SalesInvoice, kind: manyToOne, to: SalesInvoice }
              - name: SalesInvoiceCustomerPayment
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: SalesInvoice, kind: manyToOne, to: SalesInvoice, composition: true, required: true }
                  - { name: CustomerPayment, kind: manyToOne, to: CustomerPayment, model: payments, required: true }
            reports:
            #REPORT#
            seeds:
              - name: invoice-statuses
                entity: InvoiceStatus
                rows:
                  - { id: 1, name: DRAFT, stage: draft }
                  - { id: 3, name: ISSUED, stage: live }
                  - { id: 8, name: CANCELLED, stage: cancelled }
            """;

    private static final String AGED_RECEIVABLES = """
              - name: AgedReceivables
                union:
                  - { source: SalesInvoice, counterparty: Customer, columns: { currency: Currency }, date: date, due: due,
                      amount: payable }
                  - { source: CreditNote, counterparty: Customer, columns: { currency: Currency }, date: date,
                      due: SalesInvoice.due, amount: "-total" }
                  - { source: SalesInvoiceCustomerPayment, counterparty: SalesInvoice.Customer,
                      columns: { currency: SalesInvoice.Currency }, date: CustomerPayment.date, due: SalesInvoice.due,
                      amount: "-amount", filter: "SalesInvoice.Status >= 3 AND SalesInvoice.Status != 8" }
                dimensions: [counterparty.name, currency.code]
                aging: { buckets: [30, 60, 90] }
            """;

    private static final String[] SCHEMA = {
            "CREATE TABLE \"BILLING_CUSTOMER\" (\"CUSTOMER_ID\" INT PRIMARY KEY, \"CUSTOMER_NAME\" VARCHAR(100))",
            "CREATE TABLE \"BILLING_CURRENCY\" (\"CURRENCY_ID\" INT PRIMARY KEY, \"CURRENCY_CODE\" VARCHAR(3))",
            "CREATE TABLE \"BILLING_SALES_INVOICE\" (\"SALES_INVOICE_ID\" INT PRIMARY KEY, \"SALES_INVOICE_DATE\" DATE,"
                    + " \"SALES_INVOICE_DUE\" DATE, \"SALES_INVOICE_PAYABLE\" DECIMAL(19,2), \"SALES_INVOICE_CUSTOMER\" INT,"
                    + " \"SALES_INVOICE_CURRENCY\" INT, \"SALES_INVOICE_STATUS\" INT)",
            "CREATE TABLE \"BILLING_CREDIT_NOTE\" (\"CREDIT_NOTE_ID\" INT PRIMARY KEY, \"CREDIT_NOTE_DATE\" DATE,"
                    + " \"CREDIT_NOTE_TOTAL\" DECIMAL(19,2), \"CREDIT_NOTE_CUSTOMER\" INT, \"CREDIT_NOTE_CURRENCY\" INT,"
                    + " \"CREDIT_NOTE_SALES_INVOICE\" INT)",
            "CREATE TABLE \"BILLING_SALES_INVOICE_CUSTOMER_PAYMENT\" (\"SALES_INVOICE_CUSTOMER_PAYMENT_ID\" INT PRIMARY KEY,"
                    + " \"SALES_INVOICE_CUSTOMER_PAYMENT_AMOUNT\" DECIMAL(19,2), \"SALES_INVOICE_CUSTOMER_PAYMENT_SALES_INVOICE\" INT,"
                    + " \"SALES_INVOICE_CUSTOMER_PAYMENT_CUSTOMER_PAYMENT\" INT)",
            // The payments model's own table.
            "CREATE TABLE \"PAYMENTS_CUSTOMER_PAYMENT\" (\"CUSTOMER_PAYMENT_ID\" INT PRIMARY KEY, \"CUSTOMER_PAYMENT_DATE\" DATE)",
            "INSERT INTO \"BILLING_CUSTOMER\" VALUES (1, 'Acme')", "INSERT INTO \"BILLING_CURRENCY\" VALUES (1, 'EUR'), (2, 'USD')",
            "INSERT INTO \"BILLING_SALES_INVOICE\" VALUES"
                    // The issue's acceptance case: 120.00 dated 10-01, due 10-31, fully paid on 10-20.
                    + " (1, DATE '2026-10-01', DATE '2026-10-31', 120, 1, 1, 3),"
                    // Another currency is its own row: 50.00 USD, overdue since 09-30.
                    + " (2, DATE '2026-09-01', DATE '2026-09-30', 50, 1, 2, 3),"
                    // A cancelled invoice and its payment are no receivable.
                    + " (3, DATE '2026-09-01', DATE '2026-09-30', 70, 1, 1, 8)",
            // 10 USD credited on 10-05 against the USD invoice - aged in its invoice's bucket.
            "INSERT INTO \"BILLING_CREDIT_NOTE\" VALUES (1, DATE '2026-10-05', 10, 1, 2, 2)",
            "INSERT INTO \"PAYMENTS_CUSTOMER_PAYMENT\" VALUES (1, DATE '2026-10-20'), (2, DATE '2026-09-15')",
            "INSERT INTO \"BILLING_SALES_INVOICE_CUSTOMER_PAYMENT\" VALUES (1, 120, 1, 1), (2, 70, 3, 2)"};

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
    void anInvoicePaidAfterTheAsOfDateIsStillOpenOnIt() throws SQLException {
        Map<String, Map<String, BigDecimal>> report = run(parse(AGED_RECEIVABLES), "2026-10-15");

        assertEquals(List.of("Acme EUR", "Acme USD"), List.copyOf(report.keySet()));
        // The payment dated 10-20 is not in yet: 120.00 Not Yet Due, in the invoice's currency.
        assertAmount("120.00", report.get("Acme EUR"), "Not Yet Due");
        assertAmount("120.00", report.get("Acme EUR"), "Open Amount");
        // The USD invoice less its credit note, 15 days overdue - never added to the EUR figure.
        assertAmount("40.00", report.get("Acme USD"), "Overdue 0-30");
        assertAmount("40.00", report.get("Acme USD"), "Open Amount");
    }

    @Test
    void onceThePaymentIsDatedTheInvoiceHasNoOpenAmount() throws SQLException {
        Map<String, Map<String, BigDecimal>> report = run(parse(AGED_RECEIVABLES), "2026-10-25");

        // The invoice and its payment net to nothing in every bucket.
        Map<String, BigDecimal> eur = report.get("Acme EUR");
        if (eur != null) {
            eur.values()
               .forEach(amount -> assertEquals(0, amount.signum(), "no open EUR amount as of 10-25: " + eur));
        }
        assertAmount("40.00", report.get("Acme USD"), "Open Amount");
    }

    @Test
    void theAllocationReachesItsCounterpartyCurrencyAndDueOverItsInvoice() {
        IntentModel model = parse(AGED_RECEIVABLES);
        String view = views(model).get("BILLING_AGED_RECEIVABLES_UNION");
        String allocationArm = view.substring(view.lastIndexOf("UNION ALL"));
        assertTrue(allocationArm.contains("SalesInvoice.\"SALES_INVOICE_CUSTOMER\" as \"AGED_RECEIVABLES_COUNTERPARTY\""), view);
        assertTrue(allocationArm.contains("SalesInvoice.\"SALES_INVOICE_CURRENCY\" as \"AGED_RECEIVABLES_CURRENCY\""), view);
        // The payment's date lives in another model: read over a join to its owner's table.
        assertTrue(allocationArm.contains("CAST(CustomerPayment.\"CUSTOMER_PAYMENT_DATE\" AS DATE) as \"AGED_RECEIVABLES_DATE\""), view);
        assertTrue(allocationArm.contains("JOIN \"PAYMENTS_CUSTOMER_PAYMENT\" as CustomerPayment"), view);
        // Every arm selects the columns in one order: source, counterparty, currency, date, due, amount.
        String firstArm = view.substring(0, view.indexOf("UNION ALL"));
        assertTrue(firstArm.indexOf("AGED_RECEIVABLES_COUNTERPARTY") < firstArm.indexOf("AGED_RECEIVABLES_CURRENCY")
                && firstArm.indexOf("AGED_RECEIVABLES_CURRENCY") < firstArm.indexOf("AGED_RECEIVABLES_DATE"), view);

        String query = (String) document(model).get("query");
        assertTrue(query.contains("JOIN \"BILLING_CURRENCY\" as Currency ON AgedReceivables.\"AGED_RECEIVABLES_CURRENCY\""), query);
    }

    @Test
    void aColumnIsAFilterAndAParameterTargetLikeAnyRelation() throws SQLException {
        IntentModel model = parse(AGED_RECEIVABLES.replace("aging: { buckets: [30, 60, 90] }",
                "aging: { buckets: [30, 60, 90] }\n" + "    filter: \"currency.code = 'USD'\""));
        assertEquals(List.of("Acme USD"), List.copyOf(run(model, "2026-10-15").keySet()));
    }

    @Test
    void anIllFormedColumnIsRefusedAtParse() {
        assertRefused(AGED_RECEIVABLES.replace("columns: { currency: SalesInvoice.Currency }, ", ""),
                "union currency is mapped by 2 of 3 members");
        assertRefused(
                AGED_RECEIVABLES.replace("columns: { currency: SalesInvoice.Currency }", "columns: { currency: SalesInvoice.Customer }"),
                "member [SalesInvoiceCustomerPayment] currency points at [Customer], but another member's points at [Currency]");
        assertRefused(AGED_RECEIVABLES.replace("counterparty: SalesInvoice.Customer", "counterparty: SalesInvoice.date"),
                "member [SalesInvoiceCustomerPayment] counterparty [SalesInvoice.date] is not a to-one relation");
        assertRefused(AGED_RECEIVABLES.replace("counterparty: SalesInvoice.Customer", "counterparty: CustomerPayment.Customer"),
                "counterparty [CustomerPayment.Customer] is not a to-one relation");
        assertRefused(AGED_RECEIVABLES.replace("columns: { currency: Currency }, date: date, due: due",
                "columns: { amount: Currency }, date: date, due: due"), "union column [amount] must be a plain name other than");
    }

    private static void assertRefused(String report, String expected) {
        IntentValidationException refused = assertThrows(IntentValidationException.class, () -> parse(report));
        assertTrue(refused.getMessage()
                          .contains(expected),
                "expected [" + expected + "] in: " + refused.getMessage());
    }

    private static void assertAmount(String expected, Map<String, BigDecimal> row, String column) {
        assertEquals(0, new BigDecimal(expected).compareTo(row.get(column)), column + " in " + row);
    }

    /**
     * Create the book and the generated views, run the report as of the date, and read every row back
     * as "customer currency" -> column -> amount.
     */
    private static Map<String, Map<String, BigDecimal>> run(IntentModel model, String asOf) throws SQLException {
        String query = ((String) document(model).get("query")).replace(":asOf", "DATE '" + asOf + "'");
        Map<String, Map<String, BigDecimal>> report = new LinkedHashMap<>();
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:unioncolumns;DB_CLOSE_DELAY=-1");
                Statement statement = connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
            for (String ddl : SCHEMA) {
                statement.execute(ddl);
            }
            for (Map.Entry<String, String> view : views(model).entrySet()) {
                statement.execute("CREATE VIEW \"" + view.getKey() + "\" AS " + view.getValue());
            }
            try (ResultSet rows = statement.executeQuery(query + "\nORDER BY 1, 2")) {
                ResultSetMetaData meta = rows.getMetaData();
                while (rows.next()) {
                    Map<String, BigDecimal> row = new LinkedHashMap<>();
                    for (int i = 3; i <= meta.getColumnCount(); i++) {
                        row.put(meta.getColumnLabel(i), rows.getBigDecimal(i));
                    }
                    report.put(rows.getString(1) + " " + rows.getString(2), row);
                }
            }
        }
        return report;
    }
}
