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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.model.ReportIntent;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
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
}
