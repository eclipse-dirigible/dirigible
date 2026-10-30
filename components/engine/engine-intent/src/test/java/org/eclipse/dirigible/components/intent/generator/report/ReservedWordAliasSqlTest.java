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
import org.junit.jupiter.api.Test;

/**
 * Reports over an entity whose name is an SQL reserved word, RUN on a real database (dirigible
 * #8234).
 *
 * <p>
 * The generator aliases every table by its entity name, unquoted. {@code Order}, {@code User},
 * {@code Group}, {@code Value}, {@code Year} and {@code Key} are keywords, so a report over such an
 * entity parsed, generated and published clean and failed at query time with a syntax error - and a
 * correspondence view over it stayed FAILED in every synchronization pass. The string-asserting
 * tests never saw it, so this one builds the tables the generator names, installs its views and
 * executes every query shape the issue reports: a sum by dimension, a balance, a join over a
 * reserved-word parent (with a filter crossing the same hop), a correspondence balance, and a sum
 * by label over each of the other five names.
 */
class ReservedWordAliasSqlTest {

    private static final String INTENT = """
            name: shop
            entities:
              - name: Order
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: docDate, type: date }
                  - { name: total, type: decimal }
                  - { name: credit, type: decimal }
                  - { name: channel, type: string }
                relations:
                  - { name: items, kind: oneToMany, to: OrderItem }
              - name: OrderItem
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                  - { name: credit, type: decimal }
                relations:
                  - { name: order, kind: manyToOne, to: Order, composition: true }
              - name: User
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: label, type: string }
                  - { name: amount, type: decimal }
              - name: Group
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: label, type: string }
                  - { name: amount, type: decimal }
              - name: Value
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: label, type: string }
                  - { name: amount, type: decimal }
              - name: Year
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: label, type: string }
                  - { name: amount, type: decimal }
              - name: Key
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: label, type: string }
                  - { name: amount, type: decimal }
            reports:
              - name: OrderTotals
                source: Order
                dimensions: [docDate]
                measures: [sum(total)]
                filter: "total > 0"
              - name: OrderBal
                kind: balance
                source: Order
                date: docDate
                debit: total
                credit: credit
                dimensions: [channel]
              - name: ItemsByOrderDate
                source: OrderItem
                dimensions: [order.docDate]
                measures: [sum(amount)]
                filter: "order.total > 0"
              - name: ItemCorrespondence
                kind: balance
                source: OrderItem
                date: order.docDate
                debit: amount
                credit: credit
                dimensions: [order.docDate]
                correspondence: order.docDate
              - { name: UserTotals,  source: User,  dimensions: [label], measures: [sum(amount)] }
              - { name: GroupTotals, source: Group, dimensions: [label], measures: [sum(amount)] }
              - { name: ValueTotals, source: Value, dimensions: [label], measures: [sum(amount)] }
              - { name: YearTotals,  source: Year,  dimensions: [label], measures: [sum(amount)] }
              - { name: KeyTotals,   source: Key,   dimensions: [label], measures: [sum(amount)] }
            """;

    /** The tables the generator names, in the shape the schema generator creates them. */
    private static final String[] SCHEMA = {
            "CREATE TABLE \"SHOP_ORDER\" (\"ORDER_ID\" INT PRIMARY KEY, \"ORDER_DOC_DATE\" DATE, \"ORDER_TOTAL\" DECIMAL(19,2),"
                    + " \"ORDER_CREDIT\" DECIMAL(19,2), \"ORDER_CHANNEL\" VARCHAR(10))",
            "CREATE TABLE \"SHOP_ORDER_ITEM\" (\"ORDER_ITEM_ID\" INT PRIMARY KEY, \"ORDER_ITEM_AMOUNT\" DECIMAL(19,2),"
                    + " \"ORDER_ITEM_CREDIT\" DECIMAL(19,2), \"ORDER_ITEM_ORDER\" INT)",
            "CREATE TABLE \"SHOP_USER\" (\"USER_ID\" INT PRIMARY KEY, \"USER_LABEL\" VARCHAR(20), \"USER_AMOUNT\" DECIMAL(19,2))",
            "CREATE TABLE \"SHOP_GROUP\" (\"GROUP_ID\" INT PRIMARY KEY, \"GROUP_LABEL\" VARCHAR(20), \"GROUP_AMOUNT\" DECIMAL(19,2))",
            "CREATE TABLE \"SHOP_VALUE\" (\"VALUE_ID\" INT PRIMARY KEY, \"VALUE_LABEL\" VARCHAR(20), \"VALUE_AMOUNT\" DECIMAL(19,2))",
            "CREATE TABLE \"SHOP_YEAR\" (\"YEAR_ID\" INT PRIMARY KEY, \"YEAR_LABEL\" VARCHAR(20), \"YEAR_AMOUNT\" DECIMAL(19,2))",
            "CREATE TABLE \"SHOP_KEY\" (\"KEY_ID\" INT PRIMARY KEY, \"KEY_LABEL\" VARCHAR(20), \"KEY_AMOUNT\" DECIMAL(19,2))",
            // One order before the window, two inside it.
            "INSERT INTO \"SHOP_ORDER\" VALUES (1, DATE '2026-01-15', 10, 0, 'web'), (2, DATE '2026-02-10', 20, 5, 'web'), (3, DATE '2025-12-01', 7, 0, 'web')",
            // Each order's lines: a debit line against a credit line, so the correspondence axis has a counter
            // side.
            "INSERT INTO \"SHOP_ORDER_ITEM\" VALUES (1, 4, NULL, 1), (2, NULL, 4, 1), (3, 20, NULL, 2), (4, NULL, 20, 2), (5, 7, NULL, 3)",
            "INSERT INTO \"SHOP_USER\" VALUES (1, 'a', 1), (2, 'a', 2), (3, 'b', 5)",
            "INSERT INTO \"SHOP_GROUP\" VALUES (1, 'a', 1), (2, 'a', 2), (3, 'b', 5)",
            "INSERT INTO \"SHOP_VALUE\" VALUES (1, 'a', 1), (2, 'a', 2), (3, 'b', 5)",
            "INSERT INTO \"SHOP_YEAR\" VALUES (1, 'a', 1), (2, 'a', 2), (3, 'b', 5)",
            "INSERT INTO \"SHOP_KEY\" VALUES (1, 'a', 1), (2, 'a', 2), (3, 'b', 5)"};

    @Test
    void aReservedWordEntityNameIsAliasedOffTheKeywordList() {
        String query = query("OrderTotals");
        assertTrue(query.contains("FROM \"SHOP_ORDER\" as Order_"), query);
        assertTrue(query.contains("SUM(Order_.\"ORDER_TOTAL\") as \"Sum Total\""), query);
        assertTrue(query.contains("WHERE Order_.\"ORDER_TOTAL\" > 0"), query);
        assertFalse(query.contains(" as Order\n") || query.contains("Order.\""), "the bare keyword must not be emitted: " + query);

        String joined = query("ItemsByOrderDate");
        assertTrue(joined.contains("INNER JOIN \"SHOP_ORDER\" as Order_ ON OrderItem.\"ORDER_ITEM_ORDER\" = Order_.\"ORDER_ID\""), joined);
        assertTrue(joined.contains("WHERE Order_.\"ORDER_TOTAL\" > 0"), "the filter must use the same alias the join declares: " + joined);

        Map<String, Object> document = document("OrderTotals");
        assertEquals("Order_", document.get("alias"), "the .report's builder model carries the alias the query uses");
    }

    /** A derived alias is never itself touched: {@code OrderCorrespondent} is not a keyword. */
    @Test
    void theCorrespondenceViewKeepsItsDerivedAliasesAndOnlyRenamesTheBareOne() {
        String view = views("ItemCorrespondence").get("SHOP_ITEM_CORRESPONDENCE_CORRESPONDENCE");
        assertTrue(view.contains("FROM \"SHOP_ORDER_ITEM\" as OrderItem"), view);
        assertTrue(view.contains("INNER JOIN \"SHOP_ORDER\" as Order_ ON OrderItem.\"ORDER_ITEM_ORDER\" = Order_.\"ORDER_ID\""), view);
        assertTrue(view.contains("LEFT JOIN \"SHOP_ORDER_ITEM\" as OrderItemCorrespondent ON"), view);
        assertTrue(view.contains("as OrderCorrespondent ON OrderItemCorrespondent.\"ORDER_ITEM_ORDER\" = OrderCorrespondent.\"ORDER_ID\""),
                view);
    }

    @Test
    void everyReportOverAReservedWordEntityRuns() throws SQLException {
        try (Connection connection = database(); Statement statement = connection.createStatement()) {
            Map<String, BigDecimal> totals = read(statement, query("OrderTotals"), "Doc Date", "Sum Total");
            assertEquals(new BigDecimal("7.00"), totals.get("2025-12-01"));
            assertEquals(new BigDecimal("10.00"), totals.get("2026-01-15"));
            assertEquals(new BigDecimal("20.00"), totals.get("2026-02-10"));

            try (ResultSet rows = statement.executeQuery(bound(query("OrderBal")))) {
                assertTrue(rows.next(), "the balance should have its one row");
                assertEquals(new BigDecimal("7.00"), rows.getBigDecimal("Opening Debit"), "the order before the window opens the balance");
                assertEquals(new BigDecimal("30.00"), rows.getBigDecimal("Debit"), "the two orders inside the window are the period debit");
                assertEquals(new BigDecimal("5.00"), rows.getBigDecimal("Credit"));
                assertEquals(new BigDecimal("37.00"), rows.getBigDecimal("Closing Debit"));
            }

            Map<String, BigDecimal> items = read(statement, query("ItemsByOrderDate"), "Order Doc Date", "Sum Amount");
            assertEquals(new BigDecimal("7.00"), items.get("2025-12-01"));
            assertEquals(new BigDecimal("4.00"), items.get("2026-01-15"));
            assertEquals(new BigDecimal("20.00"), items.get("2026-02-10"));

            // The correspondence buckets allocate each line over its document's counter side; the
            // window's debit (4 + 20) and credit (4 + 20) must come back whole.
            BigDecimal debit = BigDecimal.ZERO;
            BigDecimal credit = BigDecimal.ZERO;
            try (ResultSet rows = statement.executeQuery(bound(query("ItemCorrespondence")))) {
                while (rows.next()) {
                    debit = debit.add(rows.getBigDecimal("Debit"));
                    credit = credit.add(rows.getBigDecimal("Credit"));
                }
            }
            assertEquals(0, new BigDecimal("24").compareTo(debit), "the allocated debit should add up to the window's debit: " + debit);
            assertEquals(0, new BigDecimal("24").compareTo(credit), "the allocated credit should add up to the window's credit: " + credit);

            for (String report : List.of("UserTotals", "GroupTotals", "ValueTotals", "YearTotals", "KeyTotals")) {
                Map<String, BigDecimal> byLabel = read(statement, query(report), "Label", "Sum Amount");
                assertEquals(new BigDecimal("3.00"), byLabel.get("a"), report);
                assertEquals(new BigDecimal("5.00"), byLabel.get("b"), report);
            }
        }
    }

    /** The words the issue reports, and the words that must stay plain aliases. */
    @Test
    void theReservedWordsAreTheOnesTheSupportedDatabasesRefuse() {
        for (String word : List.of("Order", "User", "Group", "Value", "Year", "Key", "ORDER", "order", "Table", "Select", "Account")) {
            assertTrue(SqlReservedWords.isReserved(word), word);
        }
        for (String word : List.of("Customer", "OrderItem", "Date", "Position", "Member", "Status", "Period", "Type", "Role", "Name", "")) {
            assertFalse(SqlReservedWords.isReserved(word), word);
        }
        assertFalse(SqlReservedWords.isReserved(null), "a missing name is not a keyword");
    }

    private static ReportIntent report(IntentModel model, String name) {
        return model.getReports()
                    .stream()
                    .filter(report -> name.equals(report.getName()))
                    .findFirst()
                    .orElseThrow();
    }

    private static Map<String, Object> document(String name) {
        IntentModel model = IntentParser.parse(INTENT);
        return ReportIntentGenerator.buildForTest(TestContexts.context(model), report(model, name));
    }

    private static String query(String name) {
        return (String) document(name).get("query");
    }

    private static Map<String, String> views(String name) {
        IntentModel model = IntentParser.parse(INTENT);
        return ReportIntentGenerator.buildViewsForTest(TestContexts.context(model), report(model, name));
    }

    /** The window parameters the generated repository binds, as literals. */
    private static String bound(String query) {
        return query.replace(":fromDate", "DATE '2026-01-01'")
                    .replace(":toDate", "DATE '2026-12-31'");
    }

    private static Map<String, BigDecimal> read(Statement statement, String query, String keyColumn, String valueColumn)
            throws SQLException {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        try (ResultSet rows = statement.executeQuery(bound(query))) {
            while (rows.next()) {
                values.put(rows.getString(keyColumn), rows.getBigDecimal(valueColumn));
            }
        }
        return values;
    }

    /**
     * A private in-memory database holding the tables and the generated views, as the synchronizers
     * provision them.
     */
    private static Connection database() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:h2:mem:reserved;DB_CLOSE_DELAY=-1;INIT=SET SCHEMA PUBLIC");
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
            for (String ddl : SCHEMA) {
                statement.execute(ddl);
            }
            for (String report : List.of("OrderBal", "ItemCorrespondence")) {
                for (Map.Entry<String, String> view : views(report).entrySet()) {
                    statement.execute("CREATE VIEW \"" + view.getKey() + "\" AS " + view.getValue());
                }
            }
        }
        return connection;
    }
}
