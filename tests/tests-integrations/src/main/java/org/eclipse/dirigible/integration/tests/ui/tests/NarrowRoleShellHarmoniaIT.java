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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.ide.IDEFactory;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.security.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Selenide;

/**
 * The generated shell for a user who holds ONE narrow role (#8728): here {@code Accountant}, who
 * writes invoices and may not read the customers they point at. Such a user must get usable pages,
 * not a row of refusals:
 * <ul>
 * <li>the invoice form shows the stored customer read-only, says why, and offers no New - where it
 * showed an empty picker over a value the record holds, and a New the controller refuses;</li>
 * <li>the customer controller is asked once per page: its first 403 answers every later read (the
 * stored row by id, an Inbox subject's label) without a request;</li>
 * <li>the dashboard leaves out the report the role may not read, and the Inbox lists the role's
 * task with the subject it can read.</li>
 * </ul>
 * A caller who reads both entities keeps the picker as before.
 */
class NarrowRoleShellHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final String PROJECT = "narrow-role-it";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/narrowrole/index.html#";
    private static final String INVOICES = "/services/java/" + PROJECT + "/gen/narrowrole/api/invoice/InvoiceController";

    private static final String PASSWORD = "narrow-role-it-password";
    private static final String ACCOUNTANT = "narrow-role-it-accountant";
    private static final String CLERK = "narrow-role-it-clerk";

    private static final String INTENT_YAML = """
            name: narrowrole
            entities:
              - name: Customer
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 100 }
              - name: Invoice
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, length: 40 }
                  - { name: amount, type: integer }
                relations:
                  - { name: customer, kind: manyToOne, to: Customer }
            reports:
              - name: CustomersByName
                source: Customer
                dimensions: [name]
                measures: ["count(*)"]
                widget: { kind: list, limit: 5 }
              - name: InvoiceTotals
                source: Invoice
                dimensions: [number]
                measures: ["sum(amount)"]
            processes:
              - name: InvoiceApproval
                trigger: { onCreate: Invoice }
                steps:
                  - { name: approve, kind: userTask, args: { assignee: NarrowAccountant, form: ApproveInvoice } }
            forms:
              - { name: ApproveInvoice, forEntity: Invoice, fields: [amount], actions: [approve] }
            permissions:
              - { role: NarrowAccountant, can: [Invoice:write, InvoiceTotals:read] }
              - { role: NarrowClerk,      can: [Invoice:write, Customer:write, InvoiceTotals:read, CustomersByName:read] }
            seeds:
              - name: customers
                entity: Customer
                rows:
                  - { id: 1, name: Acme }
              - name: invoices
                entity: Invoice
                rows:
                  - { id: 1, number: INV-1, amount: 10, customer: 1 }
            """;

    /**
     * The invoice form's Customer field once the page has settled:
     * {@code picker=<a select is rendered>;value=<read-only value>;hint=<the why shown>;new=<New shown>;
     * options=<option labels>;refusals=<customer requests refused>}.
     */
    private static final String FORM_STATE = """
            var root = document.querySelector('[x-data="InvoiceFormPage"]');
            if (!root || !window.Alpine) return 'not-rendered';
            var page = Alpine.$data(root);
            if (page.state === 'loading' || !page.form || page.form.Customer == null) return 'pending';
            var refused = page.readRefused('%1$s');
            if (!refused && !(page.optionsCustomer || []).length) return 'pending';
            var field = root.querySelector('#f_Customer');
            var hint = Array.from(root.querySelectorAll('[x-h-field-description]'))
                            .some(d => d.textContent.includes('cannot list these records') && d.checkVisibility());
            var newButton = Array.from(root.querySelectorAll('button'))
                                 .some(b => b.textContent.trim() === 'New' && b.checkVisibility());
            var refusals = App.services.recentFailures.items.filter(f => f.path.includes('CustomerController')).length;
            return 'picker=' + (!!field && field.hasAttribute('x-h-select-input'))
                 + ';value=' + (field && field.readOnly ? field.value : '')
                 + ';hint=' + hint + ';new=' + newButton
                 + ';options=' + (page.optionsCustomer || []).map(o => o.text).join(',')
                 + ';refusals=' + refusals;
            """;

    /** The dashboard's reports once the reports store has settled: {@code reports=<names>}. */
    private static final String DASHBOARD_STATE = """
            if (!window.Alpine || !Alpine.store('reports') || !Alpine.store('reports').items.length) return 'pending';
            return 'reports=' + Alpine.store('reports').items.map(r => r.name).sort().join(',');
            """;

    /**
     * The Inbox once the task's subject is resolved:
     * {@code tasks=<task names>;subject=<subject>;refusals=<customer requests refused>}.
     */
    private static final String INBOX_STATE = """
            if (!window.Alpine || !Alpine.store('processTasks')) return 'not-rendered';
            var store = Alpine.store('processTasks');
            var task = store.tasks[0];
            if (!task || !store.subject(task)) return 'pending';
            var refusals = App.services.recentFailures.items.filter(f => f.path.includes('CustomerController')).length;
            return 'tasks=' + store.tasks.map(t => t.name).join(',') + ';subject=' + store.subject(task) + ';refusals=' + refusals;
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private IDEFactory ideFactory;

    @Test
    void a_narrow_role_gets_usable_pages_and_one_refusal_per_unreadable_entity() {
        generateAndPublish();
        securityUtil.ensureUserInDefaultTenant(ACCOUNTANT, PASSWORD, "NarrowAccountant");
        securityUtil.ensureUserInDefaultTenant(CLERK, PASSWORD, "NarrowClerk");
        // An invoice the accountant creates starts the approval, whose task is theirs.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Number\":\"INV-2\",\"Amount\":5,\"Customer\":1}")
                                                 .post(INVOICES)
                                                 .then()
                                                 .statusCode(200),
                ACCOUNTANT, PASSWORD);

        ideFactory.create(browser, ACCOUNTANT, PASSWORD)
                  .openHomePage();
        browser.openPath(APP + "/Invoice/1/edit");
        String customers = "/services/java/" + PROJECT + "/gen/narrowrole/api/customer/CustomerController";
        String expectedForm = "picker=false;value=1;hint=true;new=false;options=;refusals=1";
        assertEquals(expectedForm, poll(FORM_STATE.formatted(customers), expectedForm),
                "an invoice's unreadable customer must read as its stored value, say why, offer no New, and be asked once");

        browser.openPath(APP + "/dashboard");
        assertEquals("reports=InvoiceTotals", poll(DASHBOARD_STATE, "reports=InvoiceTotals"),
                "the dashboard must leave out the report the role may not read");

        browser.openPath(APP + "/inbox");
        String expectedInbox = "tasks=Approve;subject=INV-2 · 5;refusals=1";
        assertEquals(expectedInbox, poll(INBOX_STATE, expectedInbox),
                "the Inbox must list the role's task with the subject it can read, asking the customers once");

        browser.clearCookies();
        ideFactory.create(browser, CLERK, PASSWORD)
                  .openHomePage();
        browser.openPath(APP + "/Invoice/1/edit");
        String expectedClerk = "picker=true;value=;hint=false;new=true;options=Acme;refusals=0";
        assertEquals(expectedClerk, poll(FORM_STATE.formatted(customers), expectedClerk),
                "a caller who reads the customers must keep the picker and its New");
    }

    private String poll(String script, String expected) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
        String value = "";
        do {
            value = Selenide.executeJavaScript(script);
            if (expected.equals(value)) {
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
