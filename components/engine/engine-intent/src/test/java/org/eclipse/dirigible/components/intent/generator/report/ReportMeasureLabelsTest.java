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

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.generator.TestContexts;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.junit.jupiter.api.Test;

/**
 * A report measure can be captioned (dirigible #8671, #8621): {@code { of: "count(*)", label: Lines
 * }} labels the column while its alias - the REST row key - and its translation id stay derived
 * from the expression, so an existing catalog entry keeps translating it.
 */
class ReportMeasureLabelsTest {

    private static final String YAML = """
            name: assets
            entities:
              - name: Category
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Asset
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: residual, type: decimal, label: Carrying amount }
                  - { name: cost, type: decimal }
                relations:
                  - { name: category, kind: manyToOne, to: Category }
            reports:
            #REPORT#
            """;

    private static IntentModel parse(String reportBlock) {
        return IntentParser.parse(YAML.replace("#REPORT#", reportBlock));
    }

    private static Map<String, Object> document(String reportBlock) {
        IntentModel model = parse(reportBlock);
        return ReportIntentGenerator.buildForTest(TestContexts.context(model), model.getReports()
                                                                                    .get(0));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> column(Map<String, Object> document, String alias) {
        return ((List<Map<String, Object>>) document.get("columns")).stream()
                                                                    .filter(column -> alias.equals(column.get("alias")))
                                                                    .findFirst()
                                                                    .orElseThrow(() -> new AssertionError(
                                                                            "no column [" + alias + "] in " + document.get("columns")));
    }

    @Test
    void aCaptionedMeasureKeepsItsAliasAndTranslationIdAndTakesTheLabel() {
        Map<String, Object> document = document("""
                  - name: AssetsByCategory
                    source: Asset
                    dimensions: [category.name]
                    measures: [{ of: "count(*)", label: Lines }, { of: "sum(residual)", label: Carrying amount }, "sum(cost)"]
                """);
        Map<String, Object> count = column(document, "Count");
        assertEquals("Count", count.get("tId"), "the translation id stays derived from the expression");
        assertEquals("Lines", count.get("label"));
        Map<String, Object> residual = column(document, "Sum Residual");
        assertEquals("SumResidual", residual.get("tId"), "an existing catalog entry keeps its key");
        assertEquals("Carrying amount", residual.get("label"));
        Map<String, Object> cost = column(document, "Sum Cost");
        assertEquals("Sum Cost", cost.get("label"), "a bare measure is labelled from its alias, as before");
        String query = String.valueOf(document.get("query"));
        assertTrue(query.contains("COUNT(*) as \"Count\"") && query.contains("as \"Sum Residual\""),
                "the row keys are unchanged, got: " + query);
    }

    @Test
    void theObjectFormIsReducedToItsExpressionForEveryOtherReader() {
        IntentModel model = parse("""
                  - name: AssetsByCategory
                    source: Asset
                    dimensions: [category.name]
                    measures: [{ of: "sum(residual)", label: Carrying amount }]
                    orderBy: [{ field: "sum(residual)", dir: desc }]
                """);
        assertEquals(List.of("sum(residual)"), model.getReports()
                                                    .get(0)
                                                    .getMeasures());
        assertEquals(Map.of("sum(residual)", "Carrying amount"), model.getReports()
                                                                      .get(0)
                                                                      .getMeasureLabels());
    }

    @Test
    void aReportWithoutCaptionsIsUnchanged() {
        Map<String, Object> document = document("""
                  - name: AssetsByCategory
                    source: Asset
                    dimensions: [category.name]
                    measures: ["count(*)", "sum(residual)"]
                """);
        assertEquals("Count", column(document, "Count").get("label"));
        assertEquals("Sum Residual", column(document, "Sum Residual").get("label"));
    }

    @Test
    void anObjectMeasureWithoutOfOrLabelOrWithAnUnknownKeyIsRefused() {
        IntentValidationException noOf = assertThrows(IntentValidationException.class, () -> parse("""
                  - name: R
                    source: Asset
                    dimensions: [category.name]
                    measures: [{ label: Lines }]
                """));
        assertTrue(noOf.getMessage()
                       .contains("needs of:"),
                noOf.getMessage());
        IntentValidationException noLabel = assertThrows(IntentValidationException.class, () -> parse("""
                  - name: R
                    source: Asset
                    dimensions: [category.name]
                    measures: [{ of: "count(*)" }]
                """));
        assertTrue(noLabel.getMessage()
                          .contains("needs a non-blank label:"),
                noLabel.getMessage());
        IntentValidationException unknown = assertThrows(IntentValidationException.class, () -> parse("""
                  - name: R
                    source: Asset
                    dimensions: [category.name]
                    measures: [{ of: "count(*)", label: Lines, alias: N }]
                """));
        assertTrue(unknown.getMessage()
                          .contains("unknown key [alias]"),
                unknown.getMessage());
    }
}
