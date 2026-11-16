/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests;

import static com.codeborne.selenide.Condition.text;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Selenide;

import io.restassured.http.ContentType;

/**
 * #8651: a record opened from its list shows its {@code related:} registers in the details sheet,
 * below its properties, as its form page does. Clicking a customer's row shows its invoices;
 * clicking another customer shows that one's, never the previous one's - also when the selection
 * changes while the sheet stays open; and a register row's Open opens the invoice's own record
 * page.
 */
class RelatedRegisterSheetHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final String PROJECT = "relatedsheetit";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/relatedsheet/index.html#";
    private static final String API = "/services/java/" + PROJECT + "/gen/relatedsheet/api";

    private static final String INTENT_YAML = """
            name: relatedsheet
            entities:
              - name: Customer
                related:
                  - { entity: Invoice, label: Invoices, show: [number] }
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 50 }
              - name: Invoice
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, length: 20 }
                relations:
                  - { name: customer, kind: manyToOne, to: Customer }
            """;

    private static final String PAGE = "Alpine.$data(document.querySelector('[x-data=\"CustomerManageListPage\"]'))";

    /** The list once loaded: {@code <state>;rows=<n>;sheet=<open>}. */
    private static final String LIST_STATE = """
            var root = document.querySelector('[x-data="CustomerManageListPage"]');
            if (!root || !window.Alpine) return 'not-rendered';
            var page = Alpine.$data(root);
            if (page.state === 'loading') return 'pending';
            return page.state + ';rows=' + page.items.length + ';sheet=' + page.detailsOpen;
            """;

    /**
     * The sheet's register as {@code <heading>|<count>|<row texts>}, {@code no-panel} while the sheet
     * renders none, {@code pending} while it loads.
     */
    private static final String SHEET_REGISTER = """
            var panel = document.querySelector('[x-h-sheet] [x-data^="relatedPanel"]');
            if (!panel || !window.Alpine) return 'no-panel';
            if (Alpine.$data(panel).state === 'loading') return 'pending';
            var rows = Array.from(panel.querySelectorAll('tbody tr')).map(function (tr) { return tr.textContent.trim(); });
            return panel.querySelector('h3').textContent.trim() + '|' + panel.querySelector('[x-h-badge]').textContent.trim()
                + '|' + rows.sort().join(',');
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void the_list_sheet_shows_the_selected_records_registers() {
        generateAndPublish();
        int acme = create("/customer/CustomerController", Map.of("Name", "Acme"));
        int beta = create("/customer/CustomerController", Map.of("Name", "Beta"));
        int acmeFirst = create("/invoice/InvoiceController", Map.of("Number", "INV-A1", "Customer", acme));
        create("/invoice/InvoiceController", Map.of("Number", "INV-A2", "Customer", acme));
        create("/invoice/InvoiceController", Map.of("Number", "INV-B1", "Customer", beta));
        ide.openHomePage();

        browser.openPath(APP + "/Customer");
        assertEquals("default;rows=2;sheet=false", poll(LIST_STATE, "default;rows=2;sheet=false"::equals),
                "the list must render both customers");

        clickListRow("Acme");
        assertEquals("Invoices|2|INV-A1,INV-A2", poll(SHEET_REGISTER, "Invoices|2|INV-A1,INV-A2"::equals),
                "the sheet must show the clicked customer's invoices, with their heading and count");

        Selenide.$$("[x-h-sheet] button")
                .findBy(text("Back to list"))
                .click();
        assertEquals("no-panel", poll(SHEET_REGISTER, "no-panel"::equals), "a closed sheet keeps no register");
        clickListRow("Beta");
        assertEquals("Invoices|1|INV-B1", poll(SHEET_REGISTER, "Invoices|1|INV-B1"::equals),
                "another customer's sheet must show that customer's invoices, never the previous one's");

        // A selection that changes while the sheet stays open re-reads the register too.
        Selenide.executeJavaScript("var page = " + PAGE + "; page.selectRow(page.items.find(function (r) { return r.Name === 'Acme'; }));");
        assertEquals("Invoices|2|INV-A1,INV-A2", poll(SHEET_REGISTER, "Invoices|2|INV-A1,INV-A2"::equals),
                "the register must follow the selection while the sheet is open");

        Selenide.$$("[x-h-sheet] [x-data^='relatedPanel'] tbody tr")
                .findBy(text("INV-A1"))
                .$("button[aria-label='Open']")
                .click();
        String dialog =
                poll("var s = Alpine.store('related'); return s.open ? String(s.src) : 'closed';", value -> !"closed".equals(value));
        assertTrue(dialog.contains("#/Invoice/" + acmeFirst + "/preview"),
                "a register row's Open must open the invoice's own page: " + dialog);
    }

    /** Clicks the list's row of a customer, as a user does, once the sheet is closed. */
    private void clickListRow(String name) {
        Selenide.$$("[x-data='CustomerManageListPage'] table tbody tr")
                .findBy(text(name))
                .click();
    }

    private int create(String controller, Map<String, Object> body) {
        AtomicReference<Integer> id = new AtomicReference<>();
        restAssuredExecutor.execute(() -> id.set(given().contentType(ContentType.JSON)
                                                        .body(body)
                                                        .when()
                                                        .post(API + controller)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")));
        return id.get();
    }

    private String poll(String script, Predicate<String> done) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
        String value;
        do {
            value = Selenide.executeJavaScript(script);
            if (value != null && done.test(value)) {
                return value;
            }
            Selenide.sleep(POLL_INTERVAL_MILLIS);
        } while (System.currentTimeMillis() < deadline);
        return value;
    }

    private void generateAndPublish() {
        String path = PROJECT_PATH + "/app.intent";
        if (repository.hasResource(path)) {
            repository.getResource(path)
                      .setContent(INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        }
        AtomicReference<List<Map<String, Object>>> plan = new AtomicReference<>();
        restAssuredExecutor.execute(() -> plan.set(given().when()
                                                          .post(GENERATE_URL)
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .jsonPath()
                                                          .getList("codeGenerations")));
        for (Map<String, Object> codeGeneration : plan.get()) {
            assertEquals(Boolean.TRUE, codeGeneration.get("generated"),
                    "generating code from " + codeGeneration.get("path") + " failed: " + codeGeneration.get("error"));
        }
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT + "/")
                                                 .then()
                                                 .statusCode(200));
        synchronizationProcessor.forceProcessSynchronizers();
    }

    @AfterEach
    void cleanup() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT)
                                                 .then()
                                                 .statusCode(greaterThanOrEqualTo(200)));
        if (repository.hasCollection(PROJECT_PATH)) {
            repository.removeCollection(PROJECT_PATH);
        }
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
