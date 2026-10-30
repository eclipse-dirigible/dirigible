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
 * The cash basis of a {@code kind: statement} ({@code cashAccounts:}, dirigible #7857) - the
 * direct-method cash-flow statement - RUN against a real little ledger.
 *
 * <p>
 * The promise of the shape is a reconciliation: operating + investing + financing equals the change
 * in the cash accounts' balance over the window. So the same model carries a second, accrual-basis
 * statement reading that change straight off the cash accounts, and the two are compared - over a
 * ledger that holds the cases a naive counter-account sum gets wrong: a compound entry whose cash
 * covers only part of the counter side, an entry with cash on both sides, a transfer between two
 * cash accounts, an entry touching no cash at all, and one before the window.
 */
class CashFlowStatementSqlTest {

    private static final String INTENT = """
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
              - name: CashFlow
                kind: statement
                source: JournalEntryItem
                date: journalEntry.entryDate
                debit: debit
                credit: credit
                account: account.code
                cashAccounts: "50*"
                lines:
                  - { code: OP.IN,    label: Receipts from customers, accounts: "41*", measure: periodCredit }
                  - { code: OP.SALES, label: Cash sales,              accounts: "70*", measure: periodCredit }
                  - { code: OP.OUT,   label: Payments to suppliers,   accounts: "40*", measure: periodDebit }
                  - { code: OP,       label: Operating activities,    sum: [OP.IN, OP.SALES], less: [OP.OUT] }
                  - { code: INV.OUT,  label: Purchases of equipment,  accounts: "20*", measure: periodDebit }
                  - { code: INV,      label: Investing activities,    less: [INV.OUT] }
                  - { code: FIN.IN,   label: Borrowings,              accounts: "15*", measure: periodCredit }
                  - { code: FIN,      label: Financing activities,    sum: [FIN.IN] }
                  - { code: NET,      label: Net cash flow,           sum: [OP, INV, FIN] }
                  - { code: CASH,     label: Cash to cash,            accounts: "50*", measure: periodDebit }
              - name: CashMovement
                kind: statement
                source: JournalEntryItem
                date: journalEntry.entryDate
                debit: debit
                credit: credit
                account: account.code
                lines:
                  - { code: IN,  label: Cash debited,   accounts: "50*", measure: periodDebit }
                  - { code: OUT, label: Cash credited,  accounts: "50*", measure: periodCredit }
                  - { code: CHG, label: Change in cash, sum: [IN], less: [OUT] }
            """;

    private static final String[] SCHEMA = {
            "CREATE TABLE \"LEDGER_ACCOUNT\" (\"ACCOUNT_ID\" INT PRIMARY KEY, \"ACCOUNT_CODE\" VARCHAR(20), \"ACCOUNT_NAME\" VARCHAR(100))",
            "CREATE TABLE \"LEDGER_JOURNAL_ENTRY\" (\"JOURNAL_ENTRY_ID\" INT PRIMARY KEY, \"JOURNAL_ENTRY_ENTRY_DATE\" DATE)",
            "CREATE TABLE \"LEDGER_JOURNAL_ENTRY_ITEM\" (\"JOURNAL_ENTRY_ITEM_ID\" INT PRIMARY KEY,"
                    + " \"JOURNAL_ENTRY_ITEM_DEBIT\" DECIMAL(19,2), \"JOURNAL_ENTRY_ITEM_CREDIT\" DECIMAL(19,2),"
                    + " \"JOURNAL_ENTRY_ITEM_JOURNAL_ENTRY\" INT, \"JOURNAL_ENTRY_ITEM_ACCOUNT\" INT)",
            "INSERT INTO \"LEDGER_ACCOUNT\" VALUES (1,'5010','Cash desk'), (2,'5030','Bank'), (3,'4110','Customers'),"
                    + " (4,'4010','Suppliers'), (5,'2040','Equipment'), (6,'1510','Loans'), (7,'6010','Materials'),"
                    + " (8,'7010','Sales')",
            "INSERT INTO \"LEDGER_JOURNAL_ENTRY\" VALUES (1, DATE '2025-12-20'), (2, DATE '2026-02-01'), (3, DATE '2026-02-05'),"
                    + " (4, DATE '2026-02-10'), (5, DATE '2026-02-12'), (6, DATE '2026-02-15'), (7, DATE '2026-02-20'),"
                    + " (8, DATE '2026-02-25'), (9, DATE '2026-03-01')",
            "INSERT INTO \"LEDGER_JOURNAL_ENTRY_ITEM\" VALUES"
                    // 1: a borrowing BEFORE the window - no flow of this period.
                    + " (11, 500, NULL, 1, 1), (12, NULL, 500, 1, 6),"
                    // 2: a customer pays 300 in cash.
                    + " (21, 300, NULL, 2, 1), (22, NULL, 300, 2, 3),"
                    // 3: a supplier is paid 120 from the bank.
                    + " (31, 120, NULL, 3, 4), (32, NULL, 120, 3, 2),"
                    // 4: equipment bought for 200 cash.
                    + " (41, 200, NULL, 4, 5), (42, NULL, 200, 4, 1),"
                    // 5: a 1000 loan lands in the bank.
                    + " (51, 1000, NULL, 5, 2), (52, NULL, 1000, 5, 6),"
                    // 6: 50 moved from the desk to the bank - no cash flow at all.
                    + " (61, 50, NULL, 6, 2), (62, NULL, 50, 6, 1),"
                    // 7: compound - 100 cash and 50 of materials against 90 receivable and 60 sales: the
                    // cash is spread over the credit side by amount, 60 to the customer and 40 to sales.
                    + " (71, 100, NULL, 7, 1), (72, 50, NULL, 7, 7), (73, NULL, 90, 7, 3), (74, NULL, 60, 7, 8),"
                    // 8: an invoice - no cash touched.
                    + " (81, 999, NULL, 8, 3), (82, NULL, 999, 8, 8),"
                    // 9: cash on both sides - 80 into the bank against 60 receivable and 20 out of the desk.
                    + " (91, 80, NULL, 9, 2), (92, NULL, 60, 9, 3), (93, NULL, 20, 9, 1)"};

    @Test
    void theCashFlowStatementClassifiesEveryCashMovementByItsCounterAccount() throws SQLException {
        Map<String, BigDecimal> flows = run(report("CashFlow"));

        assertAmount("420", flows, "OP.IN", "receipts are the customer credits of cash entries, the compound shares included");
        assertAmount("40", flows, "OP.SALES", "a compound entry's cash is spread over its credit side by amount");
        assertAmount("120", flows, "OP.OUT", "a payment is the supplier debit of an entry crediting cash");
        assertAmount("340", flows, "OP", "the section is the line arithmetic over the flows");
        assertAmount("-200", flows, "INV", "an outflow-only section is negative");
        assertAmount("1000", flows, "FIN", "a borrowing before the window is not a flow of the window");
        assertAmount("1140", flows, "NET", "the net cash flow adds the three sections");
        assertAmount("0", flows, "CASH", "a transfer between cash accounts is no cash flow");
    }

    /** The acceptance of #7857: the three sections reconcile with the change in the cash balance. */
    @Test
    void theSectionsReconcileWithTheChangeInTheCashAccounts() throws SQLException {
        Map<String, BigDecimal> flows = run(report("CashFlow"));
        Map<String, BigDecimal> movement = run(report("CashMovement"));

        BigDecimal sections = flows.get("OP")
                                   .add(flows.get("INV"))
                                   .add(flows.get("FIN"));
        assertEquals(0, sections.compareTo(movement.get("CHG")),
                "operating + investing + financing should equal the change in the cash accounts: " + sections + " vs " + movement);
    }

    @Test
    void theEntryTotalsAreASecondStructuralView() {
        Map<String, String> views = views("CashFlow");
        assertEquals(List.of("LEDGER_CASH_FLOW_LINES", "LEDGER_CASH_FLOW_CASH_TOTALS"), List.copyOf(views.keySet()));
        String totals = views.get("LEDGER_CASH_FLOW_CASH_TOTALS");
        assertTrue(totals.contains("GROUP BY JournalEntryItem.\"JOURNAL_ENTRY_ITEM_JOURNAL_ENTRY\""), totals);
        assertTrue(totals.contains("HAVING "), "only entries touching cash belong in the totals: " + totals);
        String query = query("CashFlow");
        assertTrue(query.contains("INNER JOIN \"LEDGER_CASH_FLOW_CASH_TOTALS\" as CashTotals"), query);
        assertTrue(query.contains("AND NOT Account_.\"ACCOUNT_CODE\" LIKE '50%'"), query);
    }

    @Test
    void aCashBasisIsRefusedWhereItCannotBeComputed() {
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
                      - { name: entryDate, type: date }
                      - { name: debit, type: decimal }
                      - { name: credit, type: decimal }
                    relations:
                      - { name: account, kind: manyToOne, to: Account, required: true }
                reports:
                  - name: CashFlow
                    kind: statement
                    source: JournalEntryItem
                    date: entryDate
                    debit: debit
                    credit: credit
                    account: account.code
                    cashAccounts: "50-5"
                    lines:
                      - { code: A, label: Receipts, accounts: "41*", measure: closingNetCredit }
                  - name: Trial
                    kind: balance
                    source: JournalEntryItem
                    date: entryDate
                    debit: debit
                    credit: credit
                    dimensions: [account.code]
                    cashAccounts: "50*"
                """));
        String message = error.getMessage();
        assertTrue(message.contains("cashAccounts: accounts term [50-5] is a range whose bounds are of different length"), message);
        assertTrue(message.contains("cashAccounts needs the entry its lines share"), message);
        assertTrue(message.contains("line [A] takes [closingNetCredit], but a cash-basis statement reads movements within the window"),
                message);
        assertTrue(message.contains("balance report [Trial] declares account/lines/cashAccounts - those belong to kind: statement"),
                message);
    }

    private static void assertAmount(String expected, Map<String, BigDecimal> amounts, String code, String message) {
        assertEquals(0, new BigDecimal(expected).compareTo(amounts.get(code)), message + " - line [" + code + "] read " + amounts);
    }

    private static ReportIntent reportIntent(IntentModel model, String name) {
        return model.getReports()
                    .stream()
                    .filter(report -> report.getName()
                                            .equals(name))
                    .findFirst()
                    .orElseThrow();
    }

    private static String query(String report) {
        IntentModel model = IntentParser.parse(INTENT);
        return (String) ReportIntentGenerator.buildForTest(TestContexts.context(model), reportIntent(model, report))
                                             .get("query");
    }

    private static Map<String, String> views(String report) {
        IntentModel model = IntentParser.parse(INTENT);
        return ReportIntentGenerator.buildViewsForTest(TestContexts.context(model), reportIntent(model, report));
    }

    /** The report's query with the window the generated repository would bind, as literals. */
    private static String report(String name) {
        return query(name).replace(":fromDate", "DATE '2026-01-01'")
                          .replace(":toDate", "DATE '2026-12-31'");
    }

    /** Run a statement and read its lines back, in the order the query returns them. */
    private static Map<String, BigDecimal> run(String query) throws SQLException {
        Map<String, BigDecimal> amounts = new LinkedHashMap<>();
        try (Connection connection = database();
                Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(query)) {
            while (rows.next()) {
                amounts.put(rows.getString("Code"), rows.getBigDecimal("Amount"));
            }
        }
        return amounts;
    }

    /** A private in-memory ledger: the tables, then every structural view both statements read. */
    private static Connection database() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:h2:mem:cashflow;DB_CLOSE_DELAY=-1;INIT=SET SCHEMA PUBLIC");
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
            for (String ddl : SCHEMA) {
                statement.execute(ddl);
            }
            for (String report : new String[] {"CashFlow", "CashMovement"}) {
                for (Map.Entry<String, String> view : views(report).entrySet()) {
                    statement.execute("CREATE VIEW \"" + view.getKey() + "\" AS " + view.getValue());
                }
            }
        }
        return connection;
    }
}
