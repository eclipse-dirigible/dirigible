/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.extensions.aggregation;

import static org.eclipse.dirigible.components.extensions.aggregation.Modules.values;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.base.context.ThreadContextFacade;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.local.LocalRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The application shell's report manifest: every published report page, its display metadata from
 * the {@code .report} beside it, and only the reports the caller may read (#8345).
 */
class ReportsAggregatorTest {

    @TempDir
    Path root;

    private IRepository repository;

    @BeforeEach
    void signInAsViewer() throws Exception {
        repository = new LocalRepository(root.toString(), true);
        HttpServletRequest request = mock(HttpServletRequest.class);
        lenient().when(request.isUserInRole("Viewer"))
                 .thenReturn(true);
        ThreadContextFacade.setUp();
        ThreadContextFacade.set(HttpServletRequest.class.getCanonicalName(), request);
    }

    @AfterEach
    void signOut() {
        ThreadContextFacade.tearDown();
    }

    @Test
    @SuppressWarnings("unchecked")
    void lists_a_report_page_with_its_metadata_and_the_urls_of_its_page_and_controller() {
        page("shop", "Invoice-Tally", "InvoiceTally");
        report("shop", "InvoiceTally", """
                {"name": "InvoiceTally", "label": "Invoice tally", "description": "All invoices", "dashboard": false,
                 "tId": "InvoiceTally", "query": "SELECT 1",
                 "widget": {"kind": "count", "label": "Invoices on file"},
                 "columns": [{"alias": "Number", "select": true}]}""");

        List<Map<String, Object>> reports = aggregate();

        assertEquals(1, reports.size());
        Map<String, Object> entry = reports.get(0);
        assertEquals("InvoiceTally", entry.get("name"));
        assertEquals("shop", entry.get("project"));
        assertEquals("Invoice tally", entry.get("label"));
        assertEquals("All invoices", entry.get("description"));
        assertEquals(Boolean.FALSE, entry.get("dashboard"));
        assertEquals("InvoiceTally", entry.get("tId"));
        assertEquals("Invoices on file", ((Map<String, Object>) entry.get("widget")).get("label"));
        assertEquals(List.of(Map.of("alias", "Number", "select", true)), entry.get("columns"));
        assertEquals(1, entry.get("columnCount"));
        assertEquals("/services/web/shop/gen/Invoice-Tally/reports/InvoiceTally/index.html", entry.get("url"));
        assertEquals("/services/java/shop/gen/invoice_tally/api/reports/InvoiceTallyController", entry.get("apiBase"),
                "the controller lives under the Java package the gen folder's name sanitizes to");
        assertFalse(entry.containsKey("query"), "only the display metadata reaches the browser");
    }

    @Test
    void leaves_out_a_report_whose_read_gate_the_caller_does_not_pass() {
        page("shop", "granted", "Granted");
        report("shop", "Granted", "{\"security\": {\"roleRead\": \"Clerk, Viewer\"}}");
        page("shop", "refused", "Refused");
        report("shop", "Refused", "{\"security\": {\"roleRead\": \"Clerk\"}}");
        page("shop", "open", "Open");
        report("shop", "Open", "{\"security\": {\"generateDefaultRoles\": \"true\"}}");

        assertEquals(List.of("Granted", "Open"), values(aggregate(), "name").stream()
                                                                            .sorted()
                                                                            .toList());
    }

    /**
     * A KPI tile runs its report with no parameter of its own, so the entry names the parameters that
     * declare no initial and the dashboard skips the tile rather than call the report without them
     * (#8749).
     */
    @Test
    void names_the_parameters_a_widget_report_cannot_run_without() {
        page("shop", "ledger", "Ledger");
        report("shop", "Ledger", "{\"widget\": {\"kind\": \"count\"}, \"parameters\": [{\"name\": \"account\", \"initial\": \"\"},"
                + " {\"name\": \"fromDate\", \"initial\": \"1900-01-01\"}, {\"name\": \"toDate\"}]}");
        page("shop", "defaulted", "Defaulted");
        report("shop", "Defaulted",
                "{\"widget\": {\"kind\": \"count\"}, \"parameters\": [{\"name\": \"fromDate\", \"initial\": \"1900-01-01\"}]}");
        page("shop", "tileless", "Tileless");
        report("shop", "Tileless", "{\"parameters\": [{\"name\": \"account\"}]}");

        Map<String, Map<String, Object>> byName = new java.util.HashMap<>();
        aggregate().forEach(entry -> byName.put(String.valueOf(entry.get("name")), entry));
        assertEquals(List.of("account", "toDate"), byName.get("Ledger")
                                                         .get("requiredParameters"));
        assertFalse(byName.get("Defaulted")
                          .containsKey("requiredParameters"),
                "every parameter declares an initial: the tile runs with them");
        assertFalse(byName.get("Tileless")
                          .containsKey("requiredParameters"),
                "a report without a tile is not run by the dashboard");
    }

    /**
     * The generated controller also refuses a caller outside the report's top-level
     * {@code perspectiveRole}, so the shell must not list it for them (#8753).
     */
    @Test
    void leaves_out_a_report_whose_perspective_role_the_caller_does_not_hold() {
        page("shop", "granted", "Granted");
        report("shop", "Granted", "{\"perspectiveRole\": \"Clerk, Viewer\", \"security\": {\"roleRead\": \"Viewer\"}}");
        page("shop", "refused", "Refused");
        report("shop", "Refused", "{\"perspectiveRole\": \"Clerk\"}");
        page("shop", "both", "Both");
        report("shop", "Both", "{\"perspectiveRole\": \"Clerk\", \"security\": {\"roleRead\": \"Viewer\"}}");
        page("shop", "blank", "Blank");
        report("shop", "Blank", "{\"perspectiveRole\": \"\"}");

        assertEquals(List.of("Blank", "Granted"), values(aggregate(), "name").stream()
                                                                             .sorted()
                                                                             .toList());
    }

    @Test
    void lists_a_page_without_its_report_file_under_its_name_alone() {
        page("legacy", "legacy", "Totals");

        List<Map<String, Object>> reports = aggregate();

        assertEquals(List.of("Totals"), values(reports, "name"));
        assertFalse(reports.get(0)
                           .containsKey("label"),
                "the shell humanizes the name");
        assertFalse(reports.get(0)
                           .containsKey("widget"));
        assertFalse(reports.get(0)
                           .containsKey("columnCount"),
                "without a report file the column count is unknown");
    }

    @Test
    void counts_the_columns_a_report_without_a_widget_shows() {
        page("shop", "by-status", "ByStatus");
        report("shop", "ByStatus", """
                {"name": "ByStatus", "query": "SELECT 1",
                 "columns": [{"alias": "Status", "select": true}, {"alias": "Count"}, {"alias": "StatusId", "select": false}]}""");

        Map<String, Object> entry = aggregate().get(0);

        assertEquals(2, entry.get("columnCount"), "a column marked select false is grouped by, not shown");
        assertFalse(entry.containsKey("columns"), "the columns themselves travel only with a widget");
    }

    @Test
    void ignores_what_is_not_a_report_page() {
        create(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/shop/gen/shop/reports/Draft/readme.txt", "not a page");
        create(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/shop/bin/gen/shop/reports/Mirror/index.html", "<html/>");
        create(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/plain/index.html", "<html/>");

        assertEquals(List.of(), aggregate());
    }

    /**
     * A generated application asks for its own project's readable reports, so it never runs a report to
     * learn whether the reader may read it (#8699).
     */
    @Test
    void lists_only_the_readable_reports_of_the_project_asked_for() {
        page("shop", "granted", "Granted");
        report("shop", "Granted", "{\"security\": {\"roleRead\": \"Viewer\"}}");
        page("shop", "refused", "Refused");
        report("shop", "Refused", "{\"security\": {\"roleRead\": \"Clerk\"}}");
        page("other", "elsewhere", "Elsewhere");

        ReportsAggregator aggregator = new ReportsAggregator(repository);

        assertEquals(List.of("Granted"), values(aggregator.aggregate("shop"), "name"));
        assertEquals(List.of("Elsewhere"), values(aggregator.aggregate("other"), "name"));
        assertEquals(List.of(), aggregator.aggregate("missing"), "an unpublished project has no reports");
        assertEquals(List.of(), aggregator.aggregate(".."), "a project name never leaves the registry");
        assertEquals(List.of(), aggregator.aggregate("shop/gen"), "a project name is one path segment");
    }

    private List<Map<String, Object>> aggregate() {
        return new ReportsAggregator(repository).aggregate();
    }

    private void page(String project, String folder, String name) {
        create(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + project + "/gen/" + folder + "/reports/" + name + "/index.html",
                "<html/>");
    }

    private void report(String project, String name, String json) {
        create(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + project + "/" + name + ".report", json);
    }

    private void create(String path, String content) {
        repository.createResource(path, content.getBytes(StandardCharsets.UTF_8));
    }
}
