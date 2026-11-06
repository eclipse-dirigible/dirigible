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

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.generator.TestContexts;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.junit.jupiter.api.Test;

/**
 * A report's row order and its relative-period filter (dirigible #8435): {@code orderBy:} becomes
 * the query's ORDER BY and the editor's {@code orderings}, a {@code month(x)}/{@code year(x)}
 * report is chronological by default, and a filter may compare with {@code START_OF_YEAR} or
 * {@code CURRENT_DATE-P30D}.
 */
class ReportOrderAndMomentsTest {

    private static final String YAML = """
            name: sales
            entities:
              - name: Product
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: OrderLine
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: orderedOn, type: date }
                  - { name: quantity, type: decimal }
                  - { name: amount, type: decimal }
                relations:
                  - { name: product, kind: manyToOne, to: Product }
            reports:
            #REPORT#
            """;

    private static Map<String, Object> document(String reportBlock) {
        IntentModel model = IntentParser.parse(YAML.replace("#REPORT#", reportBlock));
        return ReportIntentGenerator.buildForTest(TestContexts.context(model), model.getReports()
                                                                                    .get(0));
    }

    private static String query(Map<String, Object> document) {
        return String.valueOf(document.get("query"));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> orderings(Map<String, Object> document) {
        return (List<Map<String, Object>>) document.get("orderings");
    }

    @Test
    void orderByAMeasureDescendingMakesTheTileATopN() {
        Map<String, Object> document = document("""
                  - name: MostOrderedProducts
                    source: OrderLine
                    dimensions: [product.name]
                    measures: ["sum(quantity)"]
                    orderBy: [{ field: "sum(quantity)", dir: desc }]
                """);
        String query = query(document);
        assertTrue(query.endsWith("\nORDER BY \"Sum Quantity\" DESC"), "the measure orders the rows, got: " + query);
        assertEquals(List.of(Map.of("column", "\"Sum Quantity\"", "direction", "DESC")), orderings(document));
    }

    @Test
    void orderByADimensionDefaultsToAscendingAndAcceptsTheBareShorthand() {
        String query = query(document("""
                  - name: SalesByProduct
                    source: OrderLine
                    dimensions: [product.name]
                    measures: ["sum(amount)"]
                    orderBy: product.name
                """));
        assertTrue(query.endsWith("\nORDER BY \"Product Name\" ASC"), "alphabetical by the product, got: " + query);
    }

    @Test
    void aMonthBucketIsChronologicalByDefault() {
        Map<String, Object> document = document("""
                  - name: RevenueByMonth
                    source: OrderLine
                    dimensions: ["month(orderedOn)", product.name]
                    measures: ["sum(amount)"]
                """);
        String query = query(document);
        assertTrue(query.endsWith("\nORDER BY \"Month Ordered On\" ASC"), "months ascending, got: " + query);
        assertEquals(1, orderings(document).size());
    }

    @Test
    void anExplicitOrderByReplacesTheChronologicalDefault() {
        String query = query(document("""
                  - name: RevenueByMonth
                    source: OrderLine
                    dimensions: ["year(orderedOn)"]
                    measures: ["sum(amount)"]
                    orderBy: [{ field: "year(orderedOn)", dir: desc }]
                """));
        assertTrue(query.endsWith("\nORDER BY \"Year Ordered On\" DESC"), "got: " + query);
    }

    @Test
    void aReportWithoutOrderByOrADateBucketStaysUnordered() {
        Map<String, Object> document = document("""
                  - name: SalesByProduct
                    source: OrderLine
                    dimensions: [product.name]
                    measures: ["sum(amount)"]
                """);
        assertFalse(query(document).contains("ORDER BY"), "got: " + query(document));
        assertNull(document.get("orderings"));
    }

    @Test
    void anUnknownOrderByTermIsRejected() {
        IntentValidationException failure = assertThrows(IntentValidationException.class, () -> document("""
                  - name: SalesByProduct
                    source: OrderLine
                    dimensions: [product.name]
                    measures: ["sum(amount)"]
                    orderBy: [{ field: "sum(quantity)" }]
                """));
        assertTrue(failure.getMessage()
                          .contains("orderBy references [sum(quantity)] which is not one of the report's dimensions or measures"),
                failure.getMessage());
    }

    @Test
    void aBadDirectionIsRejected() {
        IntentValidationException failure = assertThrows(IntentValidationException.class, () -> document("""
                  - name: SalesByProduct
                    source: OrderLine
                    dimensions: [product.name]
                    measures: ["sum(amount)"]
                    orderBy: [{ field: product.name, dir: down }]
                """));
        assertTrue(failure.getMessage()
                          .contains("has dir [down] - it is asc or desc"),
                failure.getMessage());
    }

    @Test
    void aYearToDateFilterBecomesPortableSql() {
        String query = query(document("""
                  - name: SalesByProduct
                    source: OrderLine
                    dimensions: [product.name]
                    measures: ["sum(amount)"]
                    filter: "orderedOn >= START_OF_YEAR"
                """));
        assertTrue(query.contains("OrderLine.\"ORDER_LINE_ORDERED_ON\" >= CAST(DATE_TRUNC('YEAR', CURRENT_DATE) AS DATE)"),
                "the year start, got: " + query);
    }

    @Test
    void aRelativeOffsetFilterBecomesIntervalArithmetic() {
        String query = query(document("""
                  - name: RecentSales
                    source: OrderLine
                    dimensions: [product.name]
                    measures: ["sum(amount)"]
                    filter: "orderedOn >= CURRENT_DATE-P30D and orderedOn < CURRENT_DATE"
                """));
        assertTrue(query.contains(">= CAST(CURRENT_DATE - INTERVAL '30' DAY AS DATE)"), "got: " + query);
        assertTrue(query.contains("< CURRENT_DATE"), "a bare CURRENT_DATE stays as written, got: " + query);
    }

    @Test
    void anUnknownPeriodStartIsRejected() {
        IntentValidationException failure = assertThrows(IntentValidationException.class, () -> document("""
                  - name: SalesByProduct
                    source: OrderLine
                    dimensions: [product.name]
                    measures: ["sum(amount)"]
                    filter: "orderedOn >= START_OF_DECADE"
                """));
        assertTrue(failure.getMessage()
                          .contains("unknown moment [START_OF_DECADE]"),
                failure.getMessage());
    }

    @Test
    void aTimeOffsetIsRejected() {
        IntentValidationException failure = assertThrows(IntentValidationException.class, () -> document("""
                  - name: SalesByProduct
                    source: OrderLine
                    dimensions: [product.name]
                    measures: ["sum(amount)"]
                    filter: "orderedOn >= CURRENT_DATE-PT1H"
                """));
        assertTrue(failure.getMessage()
                          .contains("is not a date period"),
                failure.getMessage());
    }

    @Test
    void momentsRewriteWithOffsetsAndLeaveLiteralsAlone() {
        assertEquals(
                "d >= CAST(DATE_TRUNC('YEAR', CURRENT_DATE) - INTERVAL '1' YEAR AS DATE) AND d < CAST(DATE_TRUNC('YEAR', CURRENT_DATE) AS DATE)",
                ReportMoments.rewrite("d >= START_OF_YEAR-P1Y AND d < START_OF_YEAR"));
        assertEquals("d < CAST(CURRENT_DATE + INTERVAL '1' MONTH + INTERVAL '15' DAY AS DATE)",
                ReportMoments.rewrite("d < CURRENT_DATE + P1M2W1D"));
        assertEquals("d >= CAST(DATE_TRUNC('QUARTER', CURRENT_DATE) AS DATE) AND note = 'START_OF_MONTH it''s CURRENT_DATE-P1D'",
                ReportMoments.rewrite("d >= START_OF_QUARTER AND note = 'START_OF_MONTH it''s CURRENT_DATE-P1D'"));
        assertEquals(List.of(), ReportMoments.problems("note = 'START_OF_NOTHING'"));
    }

    /**
     * CodeQL java/uncaught-number-format-exception: an overflowing period is an authoring issue, not a
     * crash.
     */
    @Test
    void anOverflowingPeriodIsAnIssueNotACrash() {
        assertEquals(1, ReportMoments.problems("d >= CURRENT_DATE-P99999999999999999999999D")
                                     .size());
        assertEquals("d >= CURRENT_DATE-P99999999999999999999999D", ReportMoments.rewrite("d >= CURRENT_DATE-P99999999999999999999999D"));
        IntentValidationException failure = assertThrows(IntentValidationException.class, () -> document("""
                  - name: SalesByProduct
                    source: OrderLine
                    dimensions: [product.name]
                    measures: ["sum(amount)"]
                    filter: "orderedOn >= CURRENT_DATE-P99999999999999999999999D"
                """));
        assertTrue(failure.getMessage()
                          .contains("is not a date period"),
                failure.getMessage());
    }

    /**
     * CodeQL java/polynomial-redos: a filter of repeated START_OF_ prefixes is scanned in linear time,
     * and a multi-segment misspelling is still reported whole.
     */
    @Test
    void repeatedPrefixesScanQuicklyAndMultiSegmentNamesAreReported() {
        String hostile = "START_OF_".repeat(200_000) + ".";
        long started = System.nanoTime();
        ReportMoments.problems(hostile);
        ReportMoments.rewrite(hostile);
        assertTrue(System.nanoTime() - started < 2_000_000_000L, "the moment scan must stay linear");
        assertEquals(List.of("references unknown moment [START_OF_FISCAL_YEAR] - expected CURRENT_DATE or one of "
                + "[START_OF_YEAR, START_OF_QUARTER, START_OF_MONTH]"), ReportMoments.problems("d >= START_OF_FISCAL_YEAR"));
    }
    // --- #8506: a relative moment as a date parameter's initial ---

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parameter(Map<String, Object> document, String name) {
        return ((List<Map<String, Object>>) document.get("parameters")).stream()
                                                                       .filter(p -> name.equals(p.get("name")))
                                                                       .findFirst()
                                                                       .orElseThrow();
    }

    private static Map<String, Object> windowReport(String fromInitial, String toInitial) {
        return document("""
                  - name: SalesInWindow
                    source: OrderLine
                    dimensions: [product.name]
                    measures: ["sum(amount)"]
                    parameters:
                      - { name: fromDay, target: orderedOn, op: ge, initial: "%s" }
                      - { name: toDay, target: orderedOn, op: le, initial: "%s" }
                """.formatted(fromInitial, toInitial));
    }

    @Test
    void everyMomentFormIsAParameterInitialCarriedForTheRepositoryToResolve() {
        for (String moment : List.of("CURRENT_DATE", "CURRENT_DATE-P30D", "START_OF_YEAR", "START_OF_QUARTER", "START_OF_MONTH",
                "START_OF_YEAR-P1Y", "CURRENT_DATE+P1M2W", "START_OF_MONTH+P1M-P1D", "START_OF_QUARTER-P1D")) {
            Map<String, Object> document = windowReport(moment, "CURRENT_DATE");
            assertEquals(moment, parameter(document, "fromDay").get("initial"), "the moment stays a moment, never a frozen date");
            assertEquals("DATE", parameter(document, "fromDay").get("type"));
            // The query binds the marker - the moment is resolved per request, not spelt into the SQL.
            assertTrue(query(document).contains(">= :fromDay"), query(document));
            assertFalse(query(document).contains("INTERVAL"), "the moment is not rewritten into the query: " + query(document));
        }
    }

    @Test
    void aMomentInitialIsCarriedInItsCanonicalSpelling() {
        Map<String, Object> document = windowReport("start_of_month - p1m", "start_of_month - P1D");
        assertEquals("START_OF_MONTH-P1M", parameter(document, "fromDay").get("initial"));
        assertEquals("START_OF_MONTH-P1D", parameter(document, "toDay").get("initial"));
    }

    @Test
    void aFixedDateInitialStaysAsWritten() {
        assertEquals("2026-01-01", parameter(windowReport("2026-01-01", "2026-12-31"), "fromDay").get("initial"));
    }

    @Test
    void anUnknownMomentAsAnInitialIsRejected() {
        IntentValidationException failure =
                assertThrows(IntentValidationException.class, () -> windowReport("START_OF_WEEK", "CURRENT_DATE"));
        assertTrue(failure.getMessage()
                          .contains("parameter [fromDay] initial references unknown moment [START_OF_WEEK]"),
                failure.getMessage());
    }

    @Test
    void aTimeOffsetAsAnInitialIsRejected() {
        IntentValidationException failure =
                assertThrows(IntentValidationException.class, () -> windowReport("CURRENT_DATE-PT1H", "CURRENT_DATE"));
        assertTrue(failure.getMessage()
                          .contains("parameter [fromDay] initial moment [CURRENT_DATE-PT1H] has an offset that is not a date period"),
                failure.getMessage());
    }

    @Test
    void anInitialThatIsMoreThanOneMomentIsRejected() {
        IntentValidationException failure =
                assertThrows(IntentValidationException.class, () -> windowReport("CURRENT_DATE-P30D or START_OF_YEAR", "CURRENT_DATE"));
        assertTrue(failure.getMessage()
                          .contains("parameter [fromDay] initial [CURRENT_DATE-P30D or START_OF_YEAR] must be one moment"),
                failure.getMessage());
    }

    @Test
    void aDateInitialThatIsNeitherADateNorAMomentIsRejected() {
        IntentValidationException failure = assertThrows(IntentValidationException.class, () -> windowReport("yesterday", "CURRENT_DATE"));
        assertTrue(failure.getMessage()
                          .contains(
                                  "parameter [fromDay] has initial [yesterday], which is neither a date (yyyy-MM-dd) nor a relative moment"),
                failure.getMessage());
    }

    @Test
    void aMomentOnANumericParameterIsRejected() {
        IntentValidationException failure = assertThrows(IntentValidationException.class, () -> document("""
                  - name: BigSales
                    source: OrderLine
                    dimensions: [product.name]
                    measures: ["sum(amount)"]
                    parameters:
                      - { name: minAmount, target: amount, op: ge, initial: CURRENT_DATE-P1D }
                """));
        assertTrue(failure.getMessage()
                          .contains("parameter [minAmount] has the moment initial [CURRENT_DATE-P1D] but filters a [number] field"),
                failure.getMessage());
    }

    @Test
    void chainedOffsetsRewriteLeftToRight() {
        assertEquals("d <= CAST(DATE_TRUNC('MONTH', CURRENT_DATE) + INTERVAL '1' MONTH - INTERVAL '1' DAY AS DATE)",
                ReportMoments.rewrite("d <= START_OF_MONTH+P1M-P1D"));
        assertEquals("d < CAST(DATE_TRUNC('QUARTER', CURRENT_DATE) - INTERVAL '3' MONTH AS DATE) AND e = 1",
                ReportMoments.rewrite("d < START_OF_QUARTER - P3M AND e = 1"));
        assertTrue(ReportMoments.isMoment(" START_OF_MONTH + P1M - P1D "));
        assertFalse(ReportMoments.isMoment("CURRENT_DATE-1"));
        assertFalse(ReportMoments.isMoment("2026-01-01"));
        assertEquals(1, ReportMoments.problems("d <= START_OF_MONTH+P1M-PT1H")
                                     .size());
        assertEquals("START_OF_MONTH+P1M-P1D", ReportMoments.canonical("start_of_month + p1m - p1d"));
    }
}
