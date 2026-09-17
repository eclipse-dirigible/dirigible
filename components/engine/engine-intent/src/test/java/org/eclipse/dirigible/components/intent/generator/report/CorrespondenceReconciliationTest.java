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
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.dirigible.components.intent.generator.TestContexts;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The property the correspondence axis exists to keep, checked by RUNNING the generated SQL: each
 * account's turnover summed across its correspondence buckets equals what the plain balance report
 * shows for the same window. A general ledger that does not reconcile with the trial balance is
 * worse than no general ledger, and the allocation arithmetic is the one part of this shape a
 * string assertion cannot vouch for.
 *
 * <p>
 * The ledger below is the four cases that matter: a simple entry (one line on each side), a
 * compound one (one debit line against two credit lines - the proportional split), a fully compound
 * one (two debit lines against two credit lines, where both sides are allocated at once), and a
 * one-sided entry with no counter side at all (the empty bucket the LEFT join keeps).
 */
class CorrespondenceReconciliationTest {

    private static final String LEDGER = """
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
              - name: Ledger
                kind: balance
                source: JournalEntryItem
                date: journalEntry.entryDate
                debit: debit
                credit: credit
                dimensions: [account.code]
                #EXTRA#
            """;

    private static final String DDL = """
            CREATE TABLE "LEDGER_ACCOUNT" ("ACCOUNT_ID" INTEGER PRIMARY KEY, "ACCOUNT_CODE" VARCHAR(20));
            CREATE TABLE "LEDGER_JOURNAL_ENTRY" ("JOURNAL_ENTRY_ID" INTEGER PRIMARY KEY, "JOURNAL_ENTRY_ENTRY_DATE" DATE);
            CREATE TABLE "LEDGER_JOURNAL_ENTRY_ITEM" ("JOURNAL_ENTRY_ITEM_ID" INTEGER PRIMARY KEY,
                "JOURNAL_ENTRY_ITEM_DEBIT" DECIMAL(18,2), "JOURNAL_ENTRY_ITEM_CREDIT" DECIMAL(18,2),
                "JOURNAL_ENTRY_ITEM_JOURNAL_ENTRY" INTEGER, "JOURNAL_ENTRY_ITEM_ACCOUNT" INTEGER);
            INSERT INTO "LEDGER_ACCOUNT" VALUES (1, '411'), (2, '702'), (3, '4532'), (4, '412');
            INSERT INTO "LEDGER_JOURNAL_ENTRY" VALUES (1, DATE '2026-03-01'), (2, DATE '2026-03-02'), (3, DATE '2026-03-03'),
                (4, DATE '2026-03-04');
            -- entry 1, simple: 411 debit 100 against 702 credit 100
            INSERT INTO "LEDGER_JOURNAL_ENTRY_ITEM" VALUES (1, 100, NULL, 1, 1), (2, NULL, 100, 1, 2);
            -- entry 2, compound: 411 debit 300 against 702 credit 200 and 4532 credit 100
            INSERT INTO "LEDGER_JOURNAL_ENTRY_ITEM" VALUES (3, 300, NULL, 2, 1), (4, NULL, 200, 2, 2), (5, NULL, 100, 2, 3);
            -- entry 3, one-sided: 411 debit 50 with nothing on the counter side
            INSERT INTO "LEDGER_JOURNAL_ENTRY_ITEM" VALUES (6, 50, NULL, 3, 1);
            -- entry 4, M against N: 411 debit 60 and 412 debit 40 against 702 credit 70 and 4532 credit 30
            INSERT INTO "LEDGER_JOURNAL_ENTRY_ITEM" VALUES (7, 60, NULL, 4, 1), (8, 40, NULL, 4, 4),
                (9, NULL, 70, 4, 2), (10, NULL, 30, 4, 3);
            """;

    @Test
    void everyAccountsBucketsAddUpToItsPlainBalance() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:correspondence;DB_CLOSE_DELAY=-1", "sa", "");
                Statement statement = connection.createStatement()) {
            statement.execute(DDL);
            // The correspondence query reads its generated structural view (dirigible #6938) - the
            // self-join and the allocation live there; install it the way the ViewsSynchronizer would.
            for (Map.Entry<String, String> view : views("correspondence: account.code").entrySet()) {
                statement.execute("CREATE VIEW \"" + view.getKey() + "\" AS " + view.getValue());
            }

            Map<String, BigDecimal> plain = totalsByAccount(statement, query(""), false);
            Map<String, BigDecimal> allocated = totalsByAccount(statement, query("correspondence: account.code"), false);
            assertEquals(plain, allocated, "the correspondence buckets must sum to the plain balance's debit turnover");
            assertEquals(new BigDecimal("510.00"), plain.get("411"), "the debit turnover of 411 over the window");

            Map<String, BigDecimal> credits = totalsByAccount(statement, query(""), true);
            assertEquals(credits, totalsByAccount(statement, query("correspondence: account.code"), true),
                    "and so must the credit turnover");

            // The compound entry splits 411's 300 by the counter-side amounts, the simple one attributes
            // its 100 whole, the one-sided entry's 50 lands in the empty bucket rather than vanishing, and
            // entry 4 splits 411's 60 as 42 / 18 - the counter side's own 70:30 proportion.
            Map<String, BigDecimal> buckets = debitBucketsOf(statement, query("correspondence: account.code"), "411");
            assertEquals(new BigDecimal("342.00"), buckets.get("702"));
            assertEquals(new BigDecimal("118.00"), buckets.get("4532"));
            assertEquals(new BigDecimal("50.00"), buckets.get(null));
        }
    }

    private static String query(String extra) {
        IntentModel model = IntentParser.parse(LEDGER.replace("#EXTRA#", extra));
        String query = (String) ReportIntentGenerator.buildForTest(TestContexts.context(model), model.getReports()
                                                                                                     .get(0))
                                                     .get("query");
        // The window is a pair of named parameters the generated repository binds; bind them here.
        return query.replace(":fromDate", "DATE '2026-01-01'")
                    .replace(":toDate", "DATE '2026-12-31'");
    }

    /** The structural views the report emits next to its query - parameter-free, installed verbatim. */
    private static Map<String, String> views(String extra) {
        IntentModel model = IntentParser.parse(LEDGER.replace("#EXTRA#", extra));
        return ReportIntentGenerator.buildViewsForTest(TestContexts.context(model), model.getReports()
                                                                                         .get(0));
    }

    /**
     * The period debit (or credit) turnover per account code, summed over whatever rows the query
     * emits.
     */
    private static Map<String, BigDecimal> totalsByAccount(Statement statement, String query, boolean credit) throws Exception {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        try (ResultSet rows = statement.executeQuery(query)) {
            while (rows.next()) {
                String account = rows.getString("Account Code");
                BigDecimal amount = rows.getBigDecimal(credit ? "Credit" : "Debit");
                totals.merge(account, amount.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);
            }
        }
        return totals;
    }

    /** One account's period debit per correspondence bucket - null keyed for the empty bucket. */
    private static Map<String, BigDecimal> debitBucketsOf(Statement statement, String query, String account) throws Exception {
        Map<String, BigDecimal> buckets = new LinkedHashMap<>();
        try (ResultSet rows = statement.executeQuery(query)) {
            while (rows.next()) {
                if (account.equals(rows.getString("Account Code"))) {
                    buckets.merge(rows.getString("Correspondent Account Code"), rows.getBigDecimal("Debit")
                                                                                    .setScale(2, RoundingMode.HALF_UP),
                            BigDecimal::add);
                }
            }
        }
        return buckets;
    }
}
