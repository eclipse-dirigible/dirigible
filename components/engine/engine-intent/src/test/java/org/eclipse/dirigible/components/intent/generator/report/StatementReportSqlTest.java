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

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.generator.TestContexts;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The generated {@code kind: statement} query, RUN - against a real database holding a real little
 * ledger, not asserted as a string.
 *
 * <p>
 * A statement is arithmetic: a selector that matches nothing, a netting that happens after the sum
 * instead of before it, or a subtotal that double-counts all produce well-formed SQL and a
 * plausible-looking wrong number. Only executing it and reading the figures off proves the
 * semantics, so this test builds the tables the generator names, posts entries, and checks each
 * line's amount. It also runs the two shapes the generated report repository wraps the query in -
 * the {@code COUNT(*)} wrap and the appended {@code LIMIT} - because a statement query is a
 * {@code WITH} and those wraps are where a common table expression would break if the database
 * refused it there.
 */
class StatementReportSqlTest {

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
                  - { name: posted, type: integer }
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
                filter: "journalEntry.posted == 1"
                lines:
                  - { code: A.I,  label: Fixed assets,    accounts: "20*,21*", measure: closingNetDebit }
                  - { code: A.II, label: Receivables,     accounts: "41*",     measure: closingNetDebit }
                  - { code: A,    label: Total assets,    sum: [A.I, A.II] }
                  - { code: B.I,  label: Payables,        accounts: "40-49",   measure: closingNetCredit }
                  - { code: C,    label: Net assets,      sum: [A], less: [B.I] }
                  - { code: D,    label: Opening assets,  accounts: "2*",      measure: openingNetDebit }
                  - { code: E,    label: Period additions, accounts: "2*",     measure: periodNetDebit }
            """;

    /** The ledger the assertions below are read off. */
    private static final String[] SCHEMA = {
            "CREATE TABLE \"LEDGER_ACCOUNT\" (\"ACCOUNT_ID\" INT PRIMARY KEY, \"ACCOUNT_CODE\" VARCHAR(20), \"ACCOUNT_NAME\" VARCHAR(100))",
            "CREATE TABLE \"LEDGER_JOURNAL_ENTRY\" (\"JOURNAL_ENTRY_ID\" INT PRIMARY KEY, \"JOURNAL_ENTRY_ENTRY_DATE\" DATE,"
                    + " \"JOURNAL_ENTRY_POSTED\" INT)",
            "CREATE TABLE \"LEDGER_JOURNAL_ENTRY_ITEM\" (\"JOURNAL_ENTRY_ITEM_ID\" INT PRIMARY KEY,"
                    + " \"JOURNAL_ENTRY_ITEM_DEBIT\" DECIMAL(19,2), \"JOURNAL_ENTRY_ITEM_CREDIT\" DECIMAL(19,2),"
                    + " \"JOURNAL_ENTRY_ITEM_JOURNAL_ENTRY\" INT, \"JOURNAL_ENTRY_ITEM_ACCOUNT\" INT)",
            // 2010 / 2110 fixed assets, 4110 receivable, 4010 payable, 4510 a both-type account.
            "INSERT INTO \"LEDGER_ACCOUNT\" VALUES (1,'2010','Land'), (2,'2110','Equipment'), (3,'4110','Trade receivables'),"
                    + " (4,'4010','Trade payables'), (5,'4510','VAT settlement'), (6,'5010','Cash')",
            // One entry before the window, one inside it, one unposted - which the filter must drop.
            "INSERT INTO \"LEDGER_JOURNAL_ENTRY\" VALUES (1, DATE '2025-12-31', 1), (2, DATE '2026-03-15', 1), (3, DATE '2026-06-01', 0)",
            "INSERT INTO \"LEDGER_JOURNAL_ENTRY_ITEM\" VALUES (1, 1000, NULL, 1, 1), (2, NULL, 1000, 1, 4), (3, 500, NULL, 2, 3),"
                    + " (4, NULL, 200, 2, 3), (5, 250, NULL, 2, 2), (6, NULL, 700, 2, 5), (7, 100, NULL, 2, 5),"
                    + " (8, 9999, NULL, 3, 1)"};

    @Test
    void theStatementQueryComputesEveryLineOverARealLedger() throws SQLException {
        Map<String, BigDecimal> amounts = run(bound(query()));

        // 2010 (1000 debit) + 2110 (250 debit) - the unposted 9999 is filtered out.
        assertEquals(new BigDecimal("1250.00"), amounts.get("A.I"), "fixed assets should net each account, posted entries only");
        // 4110 holds 500 debit and 200 credit: netting BEFORE the sum leaves 300 on the debit side.
        assertEquals(new BigDecimal("300.00"), amounts.get("A.II"), "a receivable with a credit note should report its net balance");
        assertEquals(new BigDecimal("1550.00"), amounts.get("A"), "a sum line should add exactly its referenced lines");
        // 4010 (1000 credit) + 4510 (700 credit against 100 debit = 600) + 4110, whose net is on the
        // debit side and so contributes nothing to a net-credit line.
        assertEquals(new BigDecimal("1600.00"), amounts.get("B.I"),
                "a range selector should take the whole 40-49 block, netted per account");
        assertEquals(new BigDecimal("-50.00"), amounts.get("C"),
                "a line may be negative - the arithmetic is not floored, only each account's netting is");
        // The window: only the 2025-12-31 entry is opening, only the 2026-03-15 one is in the period.
        assertEquals(new BigDecimal("1000.00"), amounts.get("D"), "the opening measure should see only entries before :fromDate");
        assertEquals(new BigDecimal("250.00"), amounts.get("E"), "the period measure should see only entries inside the window");
    }

    /** The lines are the statement's structure, so they come back in the authored order. */
    @Test
    void theStatementRendersItsLinesInTheAuthoredOrder() throws SQLException {
        assertEquals(List.of("A.I", "A.II", "A", "B.I", "C", "D", "E"), new ArrayList<>(run(bound(query())).keySet()),
                "the statement should render its lines in the order they are declared");
    }

    /**
     * The generated report repository counts through {@code SELECT COUNT(*) FROM (<query>)} and pages
     * by appending {@code LIMIT}/{@code OFFSET}. A statement query is a {@code WITH}, so both wraps are
     * worth proving rather than assuming.
     */
    @Test
    void theQuerySurvivesTheWrapsTheGeneratedRepositoryPutsItIn() throws SQLException {
        String query = bound(query());
        try (Connection connection = database(); Statement statement = connection.createStatement()) {
            try (ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM (" + query + ") AS \"REPORT_TOTAL\"")) {
                rows.next();
                assertEquals(7, rows.getInt(1), "the count wrap should see every line");
            }
            try (ResultSet rows = statement.executeQuery(query + " LIMIT 3 OFFSET 1")) {
                List<String> codes = new ArrayList<>();
                while (rows.next()) {
                    codes.add(rows.getString("Code"));
                }
                assertEquals(List.of("A.II", "A", "B.I"), codes, "paging should walk the ordered lines");
            }
        }
    }

    /** The emitted statement query. */
    private static String query() {
        IntentModel model = IntentParser.parse(INTENT);
        return (String) ReportIntentGenerator.buildForTest(TestContexts.context(model), model.getReports()
                                                                                             .get(0))
                                             .get("query");
    }

    /**
     * The window parameters the generated repository binds, as literals - the report declares them as
     * {@code .report} parameters and this test has no repository to bind them.
     */
    private static String bound(String query) {
        return query.replace(":fromDate", "DATE '2026-01-01'")
                    .replace(":toDate", "DATE '2026-12-31'");
    }

    /** Run the statement and read its lines back, in the order the query returns them. */
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

    /** A private in-memory ledger, created fresh for each connection. */
    private static Connection database() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:h2:mem:statement;DB_CLOSE_DELAY=-1;INIT=SET SCHEMA PUBLIC");
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
            for (String ddl : SCHEMA) {
                statement.execute(ddl);
            }
        }
        return connection;
    }
}
