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
    }

    @Test
    void ignores_what_is_not_a_report_page() {
        create(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/shop/gen/shop/reports/Draft/readme.txt", "not a page");
        create(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/shop/bin/gen/shop/reports/Mirror/index.html", "<html/>");
        create(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/plain/index.html", "<html/>");

        assertEquals(List.of(), aggregate());
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
