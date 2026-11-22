/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.ide.template.service.model.JavaExpressions;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The {@code exports} glue collection (#7855): the facts and readings a declared export is
 * generated from, asserted as the Java the template layer renders them into.
 */
class GlueExportsTest {

    private static final String INTENT = """
            name: vat
            entities:
              - name: Supplier
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: vatNumber, type: string }
              - name: PurchaseInvoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: documentDate, type: date }
                  - { name: taxBase, type: decimal }
                relations:
                  - { name: supplier, kind: manyToOne, to: Supplier }
              - name: ExportRun
            reports:
              - name: PurchasesBySupplier
                source: PurchaseInvoice
                dimensions: [supplier.vatNumber]
                measures: [sum(taxBase)]
                parameters:
                  - { name: fromDate, target: documentDate, op: ge }
            permissions:
              - { role: Accountant, can: ["VatPurchaseLedger:read"] }
            exports:
              - name: VatPurchaseLedger
                source: PurchaseInvoice
                format: fixedWidth
                lineEnding: lf
                ledger: ExportRun
                fileName: "VAT-{fromDate}.txt"
                parameters:
                  - { name: fromDate, field: documentDate, op: ge, required: true }
                records:
                  - type: header
                    fields: [{ param: fromDate, width: 8, pattern: ddMMyyyy }]
                  - type: line
                    fields:
                      - { from: supplier.vatNumber, width: 15, truncate: true }
                      - { from: taxBase, width: 12, scale: 2, align: right, pad: '0' }
                  - type: trailer
                    fields: [{ from: count(), width: 4 }, { from: sum(taxBase), width: 12, scale: 2 }]
              - name: SupplierTotals
                source: PurchasesBySupplier
                format: xml
                ledger: ExportRun
                parameters: [{ name: fromDate }]
                records:
                  - type: line
                    element: Supplier
                    fields: [{ from: sum(taxBase), name: TaxBase, scale: 2 }]
            """;

    @Test
    @SuppressWarnings("unchecked")
    void anEntityExportCarriesItsLayoutLoadsTotalsAndFilters() {
        IntentModel model = IntentParser.parse(INTENT);
        Map<String, Object> export = GlueIntentGenerator.buildExportsForTest(model)
                                                        .get(0);

        assertEquals("VatPurchaseLedger", export.get("className"));
        assertEquals("entity", export.get("sourceKind"));
        assertEquals("\n", export.get("lineEnding"));
        assertEquals("ExportRun", export.get("ledger"));
        assertEquals(List.of("Id"), export.get("orderBy"), "the primary key orders the rows when nothing else does");
        assertEquals("Accountant", export.get("roles"));

        Map<String, Object> line = ((List<Map<String, Object>>) export.get("lineRecords")).get(0);
        List<Map<String, Object>> fields = (List<Map<String, Object>>) line.get("fields");
        assertEquals("(hopSupplier == null ? null : hopSupplier.VatNumber)", JavaExpressions.expression(fields.get(0)
                                                                                                              .get("reading")));
        assertEquals("15", fields.get(0)
                                 .get("width"));
        assertEquals(Boolean.TRUE, fields.get(0)
                                         .get("truncate"));
        assertEquals("row.TaxBase", JavaExpressions.expression(fields.get(1)
                                                                     .get("reading")));
        assertEquals("0", fields.get(1)
                                .get("pad"));

        Map<String, Object> load = ((List<Map<String, Object>>) export.get("loads")).get(0);
        assertEquals("hopSupplier", load.get("local"));
        assertEquals("Supplier", load.get("fkProperty"));

        List<Map<String, Object>> totals = (List<Map<String, Object>>) export.get("aggregates");
        assertEquals("count", totals.get(0)
                                    .get("kind"));
        assertEquals("row.TaxBase", JavaExpressions.expression(totals.get(1)
                                                                     .get("reading")));

        Map<String, Object> parameter = ((List<Map<String, Object>>) export.get("parameters")).get(0);
        assertEquals("paramFromDate", parameter.get("local"));
        assertEquals("ge", parameter.get("op"));
        assertEquals("DocumentDate", parameter.get("property"));
        assertEquals("java.time.LocalDate.parse(String.valueOf(raw))", JavaExpressions.expression(parameter.get("convert")));

        assertEquals("\"VAT-\" + org.eclipse.dirigible.sdk.print.FileNames.part(paramFromDate) + \".txt\"",
                JavaExpressions.expression(export.get("fileNameReading")));
    }

    /**
     * An export has no convention role to keep, so an {@code additive: true} token (issue #8852) gates
     * it exactly as a plain one does - beside the roles of the other entries naming it.
     */
    @Test
    void anAdditiveGrantOnAnExportIsItsGateLikeAnyOther() {
        String additive = INTENT.replace("- { role: Accountant, can: [\"VatPurchaseLedger:read\"] }", """
                - { role: Accountant, can: ["VatPurchaseLedger:read"] }
                  - { role: Auditor, can: ["VatPurchaseLedger:read", "SupplierTotals:read"], additive: true }""");
        List<Map<String, Object>> exports = GlueIntentGenerator.buildExportsForTest(IntentParser.parse(additive));

        assertEquals("Accountant,Auditor", exports.get(0)
                                                  .get("roles"));
        assertEquals("Auditor", exports.get(1)
                                       .get("roles"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void aReportExportReadsItsRowsByColumnAlias() {
        IntentModel model = IntentParser.parse(INTENT);
        Map<String, Object> export = GlueIntentGenerator.buildExportsForTest(model)
                                                        .get(1);

        assertEquals("report", export.get("sourceKind"));
        assertEquals("PurchasesBySupplier", export.get("report"));
        assertEquals("SupplierTotals.xml", JavaExpressions.expression(export.get("fileNameReading"))
                                                          .replace("\"", ""));
        assertFalse(export.containsKey("roles"), "no can: token names it, so no role gates it");
        Map<String, Object> line = ((List<Map<String, Object>>) export.get("lineRecords")).get(0);
        assertEquals("Supplier", line.get("element"));
        Map<String, Object> field = ((List<Map<String, Object>>) line.get("fields")).get(0);
        assertEquals("row.get(\"Sum Tax Base\")", JavaExpressions.expression(field.get("reading")));
        Map<String, Object> parameter = ((List<Map<String, Object>>) export.get("parameters")).get(0);
        assertEquals("report", parameter.get("kind"));
    }

    /** A normalised field carries its normalisation in the order it applies (#7925). */
    @Test
    @SuppressWarnings("unchecked")
    void aNormalisedFieldCarriesItsNormalisationAndItsExportRecordsIt() {
        IntentModel model = IntentParser.parse(INTENT.replace("{ from: supplier.vatNumber, width: 15, truncate: true }",
                "{ from: supplier.vatNumber, width: 15, truncate: true, replace: { \"-\": \"\", \" \": \"\" }, case: upper,"
                        + " allowed: \"A-Z0-9\", onDisallowed: space, collapseSpaces: true, onLoss: fail }"));
        Map<String, Object> export = GlueIntentGenerator.buildExportsForTest(model)
                                                        .get(0);

        assertEquals(Boolean.TRUE, export.get("normalises"));
        Map<String, Object> field = ((List<Map<String, Object>>) ((List<Map<String, Object>>) export.get("lineRecords")).get(0)
                                                                                                                        .get("fields")).get(
                                                                                                                                0);
        assertEquals(List.of(Map.of("target", "-", "replacement", ""), Map.of("target", " ", "replacement", "")), field.get("replace"));
        assertEquals("upper", field.get("case"));
        assertEquals("A-Z0-9", field.get("allowed"));
        assertEquals("space", field.get("onDisallowed"));
        assertEquals(Boolean.TRUE, field.get("collapseSpaces"));
        assertEquals(Boolean.TRUE, field.get("failOnLoss"));
    }

    /** An export that normalises nothing has the glue it always had. */
    @Test
    @SuppressWarnings("unchecked")
    void anExportWithoutNormalisationCarriesNoneOfItsKeys() {
        Map<String, Object> export = GlueIntentGenerator.buildExportsForTest(IntentParser.parse(INTENT))
                                                        .get(0);

        assertFalse(export.containsKey("normalises"));
        Map<String, Object> field = ((List<Map<String, Object>>) ((List<Map<String, Object>>) export.get("lineRecords")).get(0)
                                                                                                                        .get("fields")).get(
                                                                                                                                0);
        for (String key : List.of("replace", "case", "allowed", "onDisallowed", "collapseSpaces", "failOnLoss")) {
            assertFalse(field.containsKey(key), key);
        }
    }

    @Test
    void anOptedOutExportIsNotGenerated() {
        IntentModel model = IntentParser.parse(INTENT);

        List<Map<String, Object>> exports =
                ExportSupport.build(model, IntentEntities.byName(model), IntentEntities.compositionParents(model),
                        IntentSettings.parse("{\"overrides\": {\"exports\": {\"VatPurchaseLedger\": {\"generate\": false}}}}"), null);

        assertEquals(List.of("SupplierTotals"), exports.stream()
                                                       .map(export -> export.get("name"))
                                                       .toList());
    }
}
