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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
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
 * A task form shows the record's lines (issue #7907): the approver of an invoice approved a total
 * without seeing what is billed, because a task form showed header fields only.
 * <p>
 * {@code items:} lists the lines under the fields. They are read the way the header is: the trigger
 * seeds the lines' controller as a process variable, and the form lists the record's lines through
 * it with the approver's own access - so the lines added after the process started show, and a
 * caller who may not read the invoice is refused them.
 */
class TaskFormItemsHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;
    private static final long POLL_INTERVAL_MILLIS = 250;
    private static final long PROCESS_TIMEOUT_SECONDS = 60;

    private static final String PROJECT = "task-items-it";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String API = "/services/java/" + PROJECT + "/gen/taskitems/api/invoice/";
    private static final String FORM = "/services/web/" + PROJECT + "/gen/ApproveInvoice/forms/ApproveInvoice/index.html";
    private static final String TASKS = "/services/inbox/tasks";

    private static final String PASSWORD = "task-items-it-password";
    private static final String APPROVER = "task-items-it-approver";
    private static final String OUTSIDER = "task-items-it-outsider";

    private static final String INTENT_YAML = """
            name: taskitems
            entities:
              - name: Invoice
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, length: 40 }
              - name: InvoiceLine
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: name,     type: string, length: 100 }
                  - { name: quantity, type: decimal, precision: 18, scale: 3 }
                relations:
                  - { name: Invoice, kind: manyToOne, to: Invoice, composition: true }
            processes:
              - name: InvoiceApproval
                trigger: { onCreate: Invoice }
                steps:
                  - { name: review, kind: userTask, args: { assignee: TaskItemsApprover, form: ApproveInvoice } }
                  - { name: done,   kind: end }
            forms:
              - { name: ApproveInvoice, forEntity: Invoice, fields: [number],
                  items: { entity: InvoiceLine, fields: [name, quantity] }, actions: [confirm] }
            permissions:
              - { role: TaskItemsApprover, can: [Invoice:write] }
              - { role: TaskItemsOutsider, description: May read nothing here }
            """;

    /**
     * The lines table once it has rows: {@code <header>;<row>;<row>} with the cells of a row joined by
     * |.
     */
    private static final String TABLE_STATE = """
            var table = document.querySelector('table[x-h-table]');
            if (!table || !table.checkVisibility()) return 'no-table';
            var text = (row) => Array.from(row.children).map(c => c.textContent.trim()).join('|');
            var rows = Array.from(table.querySelectorAll('tbody tr')).filter(r => r.checkVisibility());
            return [text(table.querySelector('thead tr'))].concat(rows.map(text)).join(';');
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
    void the_task_form_lists_the_records_lines_through_the_approvers_own_access() {
        generateAndPublish();
        securityUtil.ensureUserInDefaultTenant(APPROVER, PASSWORD, "TaskItemsApprover");
        securityUtil.ensureUserInDefaultTenant(OUTSIDER, PASSWORD, "TaskItemsOutsider");

        // The invoice starts the approval; its lines are added AFTER the process started.
        int invoice = post("InvoiceController", "{\"Number\":\"INV-7907\"}");
        post("InvoiceLineController", "{\"Invoice\":" + invoice + ",\"Name\":\"Bolt\",\"Quantity\":2}");
        post("InvoiceLineController", "{\"Invoice\":" + invoice + ",\"Name\":\"Nut\",\"Quantity\":10.5}");

        String task = taskFor(invoice);
        AtomicReference<String> rowsUrl = new AtomicReference<>();
        restAssuredExecutor.execute(() -> rowsUrl.set(given().when()
                                                             .get(TASKS + "/" + task + "/variables")
                                                             .then()
                                                             .statusCode(200)
                                                             .extract()
                                                             .path("variables.__InvoiceLineRowsUrl")),
                APPROVER, PASSWORD);
        assertEquals(API + "InvoiceLineController", rowsUrl.get(), "the trigger must hand the form the lines' controller");

        // The list the form calls is the lines' own read-gated one: nothing is opened to a caller who
        // may not read the invoice.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(rowsUrl.get() + "?Invoice=" + invoice)
                                                 .then()
                                                 .statusCode(403),
                OUTSIDER, PASSWORD);

        ideFactory.create(browser, APPROVER, PASSWORD)
                  .openHomePage();
        browser.openPath(FORM + "?taskId=" + task);
        String expected = "Name|Quantity;Bolt|2.000;Nut|10.500";
        assertEquals(expected, poll(TABLE_STATE, expected), "the approver must see the invoice's lines under its fields");
    }

    private int post(String controller, String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(API + controller)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                APPROVER, PASSWORD);
        return id.get();
    }

    /** The approval task of this invoice's instance, found by the business key the trigger stamped. */
    private String taskFor(int invoice) {
        AtomicReference<String> task = new AtomicReference<>();
        restAssuredExecutor.execute(() -> task.set(given().when()
                                                          .get(TASKS + "?type=groups")
                                                          .then()
                                                          .statusCode(200)
                                                          .body("find { it.processInstanceBusinessKey == '" + invoice + "' }.name",
                                                                  equalTo("Review"))
                                                          .extract()
                                                          .path("find { it.processInstanceBusinessKey == '" + invoice + "' }.id")),
                APPROVER, PASSWORD, PROCESS_TIMEOUT_SECONDS);
        assertNotNull(task.get(), "the approval task must be the approver's");
        return task.get();
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
