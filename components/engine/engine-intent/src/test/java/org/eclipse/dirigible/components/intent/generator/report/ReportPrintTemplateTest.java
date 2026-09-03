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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.generator.TestContexts;
import org.eclipse.dirigible.components.intent.generator.print.ReportPrintTemplate;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The {@code .print} scaffold a mailed report renders through ({@code attach: { report, bind }},
 * dirigible #6931). The contract worth pinning is that it binds the report's OWN resolved column
 * aliases: the aliases are what the generated query SELECTs, so a placeholder derived any other way
 * renders an empty cell that a template author then hunts through the whole pipeline.
 */
class ReportPrintTemplateTest {

    private static final String INTENT = """
            name: ar
            entities:
              - name: Customer
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: SalesInvoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: issuedOn, type: date }
                  - { name: total, type: decimal }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer }
            reports:
              - name: CustomerStatement
                source: SalesInvoice
                dimensions: [issuedOn]
                measures: ["sum(total)"]
                parameters:
                  - { name: fromDate, target: issuedOn, op: ge }
                  - { name: toDate, target: issuedOn, op: le }
                  - { name: customer, target: Customer.name, op: eq, initial: "-" }
            """;

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> columns(Map<String, Object> document) {
        return (List<Map<String, Object>>) document.get("columns");
    }

    @Test
    void everyColumnOfTheReportIsABoundPlaceholder() {
        IntentModel model = IntentParser.parse(INTENT);
        Map<String, Object> document = ReportIntentGenerator.buildForTest(TestContexts.context(model), model.getReports()
                                                                                                            .get(0));
        List<Map<String, Object>> columns = columns(document);
        assertTrue(columns.size() >= 2, "columns: " + columns);
        String template = ReportPrintTemplate.build(model.getReports()
                                                         .get(0),
                columns);
        for (Map<String, Object> column : columns) {
            String alias = String.valueOf(column.get("alias"));
            assertTrue(template.contains("{{" + alias + "}}"), "missing column [" + alias + "] in:\n" + template);
        }
        assertTrue(template.contains("<table source=\"items\">"), template);
        // The bound parameters are the header - a table of rows never states which slice it is.
        assertTrue(template.contains("{{document.customer}}"), template);
        assertTrue(template.contains("{{document.fromDate}}"), template);
        assertTrue(template.contains("{{document.toDate}}"), template);
        // An aggregated measure is a figure, so it is right-aligned; a dimension is not.
        assertTrue(template.contains("align=\"right\" label=\"Sum Total\""), template);
        assertTrue(template.contains("label=\"Issued On\">{{Issued On}}"), template);
    }

    @Test
    void aBalanceReportsOwnWindowIsPartOfTheHeader() {
        String yaml = """
                name: gl
                entities:
                  - name: Account
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: name, type: string }
                  - name: JournalLine
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                      - { name: postedOn, type: date }
                      - { name: debit, type: decimal }
                      - { name: credit, type: decimal }
                    relations:
                      - { name: Account, kind: manyToOne, to: Account }
                reports:
                  - name: TrialBalance
                    source: JournalLine
                    kind: balance
                    date: postedOn
                    debit: debit
                    credit: credit
                    dimensions: [Account.name]
                """;
        IntentModel model = IntentParser.parse(yaml);
        Map<String, Object> document = ReportIntentGenerator.buildForTest(TestContexts.context(model), model.getReports()
                                                                                                            .get(0));
        String template = ReportPrintTemplate.build(model.getReports()
                                                         .get(0),
                columns(document));
        // A balance report declares its window on its own behalf, so the header states it even though
        // `parameters:` is empty.
        assertTrue(template.contains("{{document.fromDate}}"), template);
        assertTrue(template.contains("{{document.toDate}}"), template);
    }

    /** A statement mailed to a customer is as much the issuer's paper as an invoice is. */
    @Test
    void aMailedReportCarriesTheSameLogoSlotAsADocument() {
        IntentModel model = IntentParser.parse(INTENT);
        Map<String, Object> document = ReportIntentGenerator.buildForTest(TestContexts.context(model), model.getReports()
                                                                                                            .get(0));
        String template = ReportPrintTemplate.build(model.getReports()
                                                         .get(0),
                columns(document));

        assertTrue(template.contains("<image src=\"Templates/Print/logo.png\" width=\"120\"/>"), template);
    }

    @Test
    void theTemplatePathIsTheOneThePrintRenderResolvesByName() {
        // sdk.print.Print.render("<report>", ...) looks the template up by that name, which is why the
        // seeded CMS path carries the report's name where a document carries the entity's.
        assertEquals("doc/Templates/CustomerStatement/Print/en/standard.print", ReportPrintTemplate.fileName("CustomerStatement"));
    }
}
