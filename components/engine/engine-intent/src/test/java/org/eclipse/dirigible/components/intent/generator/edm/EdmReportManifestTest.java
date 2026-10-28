/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.generator.edm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.commons.api.helpers.NamingHelper;
import org.eclipse.dirigible.components.intent.generator.TestContexts;
import org.eclipse.dirigible.components.intent.model.IntentModel;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.junit.jupiter.api.Test;

/**
 * The report manifest the {@code .model} root carries for the Harmonia shell (#8277): one entry per
 * report, in the intent's order, with the display metadata the {@code .report} carries and the URLs
 * of the report's page and controller - the very paths the report-file template generates them to,
 * so the shell needs no runtime discovery through the core repository API.
 */
class EdmReportManifestTest {

    private static final String PROJECT = "sales-app";

    private static final String INTENT = """
            name: sales
            entities:
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string }
                  - { name: issuedOn, type: date }
                  - { name: total, type: decimal }
            reports:
              - name: OpenInvoices
                source: Invoice
                description: Invoices not yet paid
                dimensions: [number, issuedOn]
                widget: { kind: list, label: Latest, limit: 3 }
              - name: MonthlyRevenue
                source: Invoice
                dimensions: ["month(issuedOn)"]
                measures: ["sum(total)"]
                chart: bar
                dashboard: false
            """;

    @Test
    void everyReportIsListedInTheIntentsOrderWithTheUrlsItsPageAndControllerAreGeneratedTo() {
        List<Map<String, Object>> reports = reports(INTENT);

        assertEquals(List.of("OpenInvoices", "MonthlyRevenue"), reports.stream()
                                                                       .map(r -> r.get("name"))
                                                                       .toList());
        Map<String, Object> revenue = reports.get(1);
        // The report-file template renders the page to gen/{{javaGenFolderName}}/reports/{{name}}/ and
        // the controller under gen/{{javaGenFolderName}}/api/{{javaPerspectiveName}}/, where the gen
        // folder is the .report's base name and the perspective "Reports", both sanitized.
        String folder = NamingHelper.sanitizeJavaIdentifier("MonthlyRevenue");
        assertEquals("monthlyrevenue", folder, "precondition: the name is one the sanitizer changes");
        assertEquals("/services/web/" + PROJECT + "/gen/" + folder + "/reports/MonthlyRevenue/index.html", revenue.get("url"));
        assertEquals("/services/java/" + PROJECT + "/gen/" + folder + "/api/" + NamingHelper.sanitizeJavaIdentifier("Reports")
                + "/MonthlyRevenueController", revenue.get("apiBase"));
    }

    @Test
    void anEntryCarriesTheReportsDisplayMetadataAndItsWidgetOnlyWhenItDeclaresOne() {
        List<Map<String, Object>> reports = reports(INTENT);

        Map<String, Object> open = reports.get(0);
        assertEquals("Open Invoices", open.get("label"));
        assertEquals("Invoices not yet paid", open.get("description"));
        assertEquals(Boolean.TRUE, open.get("dashboard"));
        assertEquals("OpenInvoices", open.get("tId"));
        assertEquals("OpenInvoicesDescription", open.get("descriptionTId"));
        Map<?, ?> widget = (Map<?, ?>) open.get("widget");
        assertEquals("list", widget.get("kind"));
        List<?> columns = (List<?>) open.get("columns");
        assertFalse(columns.isEmpty(), "a list tile shows the report's columns, so they travel with the widget");

        Map<String, Object> revenue = reports.get(1);
        assertEquals(Boolean.FALSE, revenue.get("dashboard"));
        assertEquals("bar", revenue.get("chart"));
        assertFalse(revenue.containsKey("widget"), "a report without a widget carries none: " + revenue);
        assertFalse(revenue.containsKey("columns"), "nor the columns only a widget tile reads: " + revenue);
        assertFalse(revenue.containsKey("description"), "an absent description is not written as an empty one");
    }

    @Test
    void anIntentWithoutReportsLeavesTheKeyOffTheModel() {
        Map<String, Object> root = modelRoot("""
                name: sales
                entities:
                  - name: Invoice
                    fields:
                      - { name: id, type: integer, primaryKey: true, generated: true }
                """);

        assertFalse(root.containsKey("reports"), "no manifest, so the shell keeps discovering: " + root.keySet());
        assertTrue(root.containsKey("entities"));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> reports(String intent) {
        return (List<Map<String, Object>>) modelRoot(intent).get("reports");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> modelRoot(String intent) {
        IntentModel model = IntentParser.parse(intent);
        return (Map<String, Object>) EdmIntentGenerator.buildModelJsonForTest(TestContexts.context(model, PROJECT))
                                                       .get("model");
    }
}
