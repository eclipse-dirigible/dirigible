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
 * A report's {@code period:} (dirigible #8506): the shorthand expands to the from/to parameter pair
 * whose initials are the default preset's relative moments, and the {@code .report} carries the
 * offered presets for the report page.
 */
class ReportPeriodTest {

    private static final String YAML = """
            name: sales
            entities:
              - name: Product
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: SalesOrder
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: orderedOn, type: date }
                  - { name: bookedAt, type: timestamp }
              - name: OrderLine
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: orderedOn, type: date }
                  - { name: quantity, type: decimal }
                relations:
                  - { name: product, kind: manyToOne, to: Product }
                  - { name: salesOrder, kind: manyToOne, to: SalesOrder }
            reports:
            #REPORT#
            """;

    private static IntentModel model(String reportBlock) {
        return IntentParser.parse(YAML.replace("#REPORT#", reportBlock));
    }

    private static Map<String, Object> document(String reportBlock) {
        IntentModel model = model(reportBlock);
        return ReportIntentGenerator.buildForTest(TestContexts.context(model), model.getReports()
                                                                                    .get(0));
    }

    private static String failure(String reportBlock) {
        return assertThrows(IntentValidationException.class, () -> document(reportBlock)).getMessage();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> parameters(Map<String, Object> document) {
        return (List<Map<String, Object>>) document.get("parameters");
    }

    @Test
    void aBarePeriodIsTheLastThirtyDaysWithTheAccountingPresets() {
        Map<String, Object> document = document("""
                  - name: OrderedProducts
                    source: OrderLine
                    dimensions: [product.name]
                    measures: ["sum(quantity)"]
                    period: { target: orderedOn }
                """);
        assertEquals(List.of(Map.of("name", "fromDate", "type", "DATE", "initial", "CURRENT_DATE-P29D"),
                Map.of("name", "toDate", "type", "DATE", "initial", "CURRENT_DATE")), parameters(document));
        String query = String.valueOf(document.get("query"));
        assertTrue(query.contains("OrderLine.\"ORDER_LINE_ORDERED_ON\", DATE '1900-01-01') >= :fromDate"), query);
        assertTrue(query.contains("OrderLine.\"ORDER_LINE_ORDERED_ON\", DATE '9999-12-31') <= :toDate"), query);

        @SuppressWarnings("unchecked")
        Map<String, Object> period = (Map<String, Object>) document.get("period");
        assertEquals("fromDate", period.get("from"));
        assertEquals("toDate", period.get("to"));
        assertEquals("last30Days", period.get("default"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> presets = (List<Map<String, Object>>) period.get("presets");
        assertEquals(List.of("last30Days", "currentMonth", "previousMonth", "lastQuarter", "yearToDate"), presets.stream()
                                                                                                                 .map(p -> p.get("name"))
                                                                                                                 .toList());
        assertEquals(Map.of("name", "previousMonth", "from", "START_OF_MONTH-P1M", "to", "START_OF_MONTH-P1D"), presets.get(2));
        assertEquals(Map.of("name", "lastQuarter", "from", "START_OF_QUARTER-P3M", "to", "START_OF_QUARTER-P1D"), presets.get(3));
    }

    @Test
    void theDefaultPresetSetsTheInitialsAndTheNamesAreTheAuthors() {
        Map<String, Object> document = document(
                """
                          - name: OrderedProducts
                            source: OrderLine
                            dimensions: [product.name]
                            measures: ["sum(quantity)"]
                            period: { target: salesOrder.bookedAt, default: currentMonth, presets: [currentMonth, previousYear], from: since, to: until }
                        """);
        // A timestamp target is compared as a date - the parameters are DATE pickers.
        assertEquals(List.of(Map.of("name", "since", "type", "DATE", "initial", "START_OF_MONTH"),
                Map.of("name", "until", "type", "DATE", "initial", "START_OF_MONTH+P1M-P1D")), parameters(document));
        assertTrue(String.valueOf(document.get("query"))
                         .matches("(?s).*\"SALES_ORDER_BOOKED_AT\" AS DATE\\), DATE '1900-01-01'\\) >= :since.*"),
                String.valueOf(document.get("query")));
    }

    @Test
    void aCustomDefaultOpensOnAllTime() {
        Map<String, Object> document = document("""
                  - name: OrderedProducts
                    source: OrderLine
                    dimensions: [product.name]
                    measures: ["sum(quantity)"]
                    period: { target: orderedOn, default: custom }
                """);
        assertEquals("1900-01-01", parameters(document).get(0)
                                                       .get("initial"));
        assertEquals("9999-12-31", parameters(document).get(1)
                                                       .get("initial"));
    }

    @Test
    void aReportWithoutAPeriodCarriesNone() {
        assertNull(document("""
                  - name: OrderedProducts
                    source: OrderLine
                    dimensions: [product.name]
                    measures: ["sum(quantity)"]
                """).get("period"));
    }

    @Test
    void theShellManifestCarriesThePeriod() {
        IntentModel model = model("""
                  - name: OrderedProducts
                    source: OrderLine
                    dimensions: [product.name]
                    measures: ["sum(quantity)"]
                    period: { target: orderedOn, default: yearToDate, presets: [yearToDate] }
                """);
        @SuppressWarnings("unchecked")
        Map<String, Object> period = (Map<String, Object>) ReportIntentGenerator.shellManifest(TestContexts.context(model))
                                                                                .get(0)
                                                                                .get("period");
        assertEquals("yearToDate", period.get("default"));
    }

    @Test
    void anUnknownPresetIsRejected() {
        String message = failure("""
                  - name: OrderedProducts
                    source: OrderLine
                    dimensions: [product.name]
                    measures: ["sum(quantity)"]
                    period: { target: orderedOn, presets: [last30Days, lastWeek] }
                """);
        assertTrue(message.contains("report [OrderedProducts] period has unknown preset [lastWeek]"), message);
    }

    @Test
    void aDefaultOutsideThePresetsIsRejected() {
        String message = failure("""
                  - name: OrderedProducts
                    source: OrderLine
                    dimensions: [product.name]
                    measures: ["sum(quantity)"]
                    period: { target: orderedOn, default: yearToDate, presets: [currentMonth] }
                """);
        assertTrue(message.contains("period default [yearToDate] is not one of its presets [currentMonth]"), message);
    }

    @Test
    void customAndRepeatedPresetsAreRejected() {
        String message = failure("""
                  - name: OrderedProducts
                    source: OrderLine
                    dimensions: [product.name]
                    measures: ["sum(quantity)"]
                    period: { target: orderedOn, presets: [last30Days, last30Days, custom] }
                """);
        assertTrue(message.contains("lists preset [last30Days] twice"), message);
        assertTrue(message.contains("lists [custom] among its presets"), message);
    }

    @Test
    void aPeriodNeedsADateTarget() {
        assertTrue(failure("""
                  - name: OrderedProducts
                    source: OrderLine
                    dimensions: [product.name]
                    measures: ["sum(quantity)"]
                    period: { default: last30Days }
                """).contains("report [OrderedProducts] period has no target"));
        String message = failure("""
                  - name: OrderedProducts
                    source: OrderLine
                    dimensions: [product.name]
                    measures: ["sum(quantity)"]
                    period: { target: quantity }
                """);
        assertTrue(message.contains("period target [quantity] is a [decimal] field - a period filters a date or timestamp"), message);
    }

    @Test
    void aPeriodBoundClashingWithAnAuthoredParameterIsRejected() {
        String message = failure("""
                  - name: OrderedProducts
                    source: OrderLine
                    dimensions: [product.name]
                    measures: ["sum(quantity)"]
                    parameters:
                      - { name: fromDate, target: orderedOn, op: ge }
                    period: { target: orderedOn }
                """);
        assertTrue(message.contains("parameter [fromDate] is declared twice"), message);
    }

    @Test
    void aLedgerKindTakesNoPeriod() {
        String message = failure("""
                  - name: Balances
                    kind: balance
                    source: OrderLine
                    date: orderedOn
                    debit: quantity
                    credit: quantity
                    dimensions: [product.name]
                    period: { target: orderedOn }
                """);
        assertTrue(message.contains("period cannot be combined with kind [balance]"), message);
    }

    @Test
    void anUnknownPeriodKeyIsRejected() {
        String message = failure("""
                  - name: OrderedProducts
                    source: OrderLine
                    dimensions: [product.name]
                    measures: ["sum(quantity)"]
                    period: { target: orderedOn, defualt: currentMonth }
                """);
        assertTrue(message.contains("defualt"), message);
    }
}
