/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * A roll-up's target fields on the parent are derived, so a user update never writes them, whether
 * or not the author marked them {@code readOnly: true} (dirigible #8442).
 *
 * <p>
 * The targets are deliberately declared as plain numeric fields, the way most models declare them.
 * Before the fix only an authored {@code readOnly} put a target in the repository's
 * preserved-on-update set, so an edit form that does not carry {@code Paid} saved the invoice with
 * {@code Paid} null, and a body that carried another value overwrote the computed total - both
 * stayed wrong until the next payment recomputed them. Every generated surface that updates the
 * parent is walked (the power, the personal and the partner controller, which all reach the same
 * repository {@code update()}), each with a body that omits the targets and one that carries bogus
 * values; the count, sum and capacity-balance targets must all keep what the roll-up computed. A
 * payment afterwards must still move them: the roll-up's targeted write is not a user update.
 *
 * <p>
 * The same model carries a balance kept against a capacity that is measured but not enforced
 * ({@code enforce: false}, dirigible #7904): budget against actual hours, where hours booked past
 * the budget are stored and the balance goes negative, an edit of the budget moves the balance, and
 * an unbudgeted project has no balance. A capacity left enforced on the same child still refuses.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentRollupTargetPreservedIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "rolluptarget";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String INVOICES = API + "/invoice/InvoiceController";
    private static final String MY_INVOICES = API + "/invoice/InvoiceMyController";
    private static final String PAYMENTS = API + "/payment/PaymentController";
    private static final String BILLS = API + "/bill/BillController";
    private static final String PARTNER_BILLS = API + "/bill/BillPartnerController";
    private static final String BILL_PAYMENTS = API + "/billpayment/BillPaymentController";
    private static final String PROJECTS = API + "/project/ProjectController";
    private static final String TIMESHEETS = API + "/timesheet/TimesheetController";
    private static final long TIMEOUT_SECONDS = 90;
    /** The roll-up runs off the child's topics, after the commit. */
    private static final long ROLLUP_TIMEOUT_SECONDS = 30;

    private static final String INTENT_YAML = """
            name: rolluptarget
            description: roll-up targets keep their computed values through a user update

            entities:
              - name: Customer
                identity: email
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: name,  type: string, required: true, length: 100 }
                  - { name: email, type: string, required: true, length: 100 }

              - name: Invoice
                fields:
                  - { name: id,           type: integer, primaryKey: true, generated: true }
                  - { name: name,         type: string, length: 40 }
                  - { name: total,        type: decimal, precision: 18, scale: 2 }
                  # Roll-up targets, none of them marked readOnly.
                  - { name: paid,         type: decimal, precision: 18, scale: 2 }
                  - { name: balance,      type: decimal, precision: 18, scale: 2 }
                  - { name: paymentCount, type: integer }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer, required: true, personal: true }

              - name: Payment
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Invoice, kind: manyToOne, to: Invoice, required: true }

              - name: Bill
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 40 }
                  - { name: paid, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer, required: true, partner: true }

              - name: BillPayment
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Bill, kind: manyToOne, to: Bill, required: true }

              - name: Project
                fields:
                  - { name: id,             type: integer, primaryKey: true, generated: true }
                  - { name: name,           type: string, length: 40 }
                  - { name: budgetHours,    type: decimal, precision: 18, scale: 2 }
                  - { name: actualHours,    type: decimal, precision: 18, scale: 2 }
                  - { name: remainingHours, type: decimal, precision: 18, scale: 2 }
                  - { name: costCeiling,    type: decimal, precision: 18, scale: 2 }
                  - { name: actualCost,     type: decimal, precision: 18, scale: 2 }

              - name: Timesheet
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: hours, type: decimal, precision: 18, scale: 2 }
                  - { name: cost,  type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Project, kind: manyToOne, to: Project, required: true }

            rollups:
              - { name: invoicePaid, entity: Payment, via: Invoice, field: paid, op: sum, of: amount,
                  capacity: total, balance: balance }
              - { name: invoicePayments, entity: Payment, via: Invoice, field: paymentCount }
              - { name: billPaid, entity: BillPayment, via: Bill, field: paid, op: sum, of: amount }
              # Budget versus actual (#7904): measured against the budget, never refused...
              - { name: projectActualHours, entity: Timesheet, via: Project, field: actualHours, op: sum, of: hours,
                  capacity: budgetHours, balance: remainingHours, enforce: false }
              # ...while a capacity left enforced on the same child still refuses.
              - { name: projectActualCost, entity: Timesheet, via: Project, field: actualCost, op: sum, of: cost,
                  capacity: costCeiling }

            seeds:
              - name: customers
                entity: Customer
                rows:
                  - { id: 1, name: Admin, email: admin }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_user_update_keeps_the_roll_up_targets_the_platform_computed() {
        writeIntent();
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        // A create that carries a target does not seed it: the roll-up owns it from the start.
        int invoice = create(INVOICES, "{\"Name\":\"A\",\"Total\":100,\"Customer\":1,\"Paid\":999}");
        create(PAYMENTS, "{\"Amount\":30,\"Invoice\":" + invoice + "}");
        create(PAYMENTS, "{\"Amount\":20,\"Invoice\":" + invoice + "}");
        awaitInvoice(invoice, 50.0f, 50.0f, 2);

        // The power surface: an edit form that does not carry the targets, then a body with bogus ones.
        put(INVOICES, invoice, "{\"Name\":\"B\",\"Total\":100,\"Customer\":1}");
        assertInvoice(invoice, "B", 50.0f, 50.0f, 2);
        put(INVOICES, invoice, "{\"Name\":\"C\",\"Total\":100,\"Customer\":1,\"Paid\":999,\"Balance\":-1,\"PaymentCount\":9}");
        assertInvoice(invoice, "C", 50.0f, 50.0f, 2);

        // The personal surface reaches the same repository update().
        put(MY_INVOICES, invoice, "{\"Name\":\"D\",\"Total\":100}");
        assertInvoice(invoice, "D", 50.0f, 50.0f, 2);
        put(MY_INVOICES, invoice, "{\"Name\":\"E\",\"Total\":100,\"Paid\":0,\"Balance\":100,\"PaymentCount\":0}");
        assertInvoice(invoice, "E", 50.0f, 50.0f, 2);

        // The roll-up still writes them: its targeted write is not a user update.
        create(PAYMENTS, "{\"Amount\":10,\"Invoice\":" + invoice + "}");
        awaitInvoice(invoice, 60.0f, 40.0f, 3);

        // The partner surface, on a parent of its own.
        int bill = create(BILLS, "{\"Name\":\"A\",\"Customer\":1}");
        create(BILL_PAYMENTS, "{\"Amount\":40,\"Bill\":" + bill + "}");
        awaitBillPaid(bill, 40.0f);
        put(PARTNER_BILLS, bill, "{\"Name\":\"B\"}");
        assertBill(bill, "B", 40.0f);
        put(PARTNER_BILLS, bill, "{\"Name\":\"C\",\"Paid\":999}");
        assertBill(bill, "C", 40.0f);
    }

    @Test
    void an_unenforced_capacity_keeps_the_balance_and_refuses_nothing() {
        writeIntent();
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        // Hours booked past the budget are all stored, and the balance goes negative.
        int project = create(PROJECTS, "{\"Name\":\"P\",\"BudgetHours\":10,\"CostCeiling\":1000}");
        create(TIMESHEETS, "{\"Hours\":6,\"Cost\":100,\"Project\":" + project + "}");
        create(TIMESHEETS, "{\"Hours\":7,\"Cost\":100,\"Project\":" + project + "}");
        assertProject(project, 13.0f, -3.0f, ROLLUP_TIMEOUT_SECONDS);

        // An edit of the budget moves the balance, with no timesheet event behind it...
        put(PROJECTS, project, "{\"Name\":\"P\",\"BudgetHours\":20,\"CostCeiling\":1000}");
        assertProject(project, 13.0f, 7.0f, TIMEOUT_SECONDS);
        // ...and a project left without a budget has no balance, not minus what was booked.
        put(PROJECTS, project, "{\"Name\":\"P\",\"CostCeiling\":1000}");
        assertProject(project, 13.0f, null, TIMEOUT_SECONDS);

        // The roll-up's own write agrees: an unbudgeted project's hours are summed, its balance stays null.
        int unbudgeted = create(PROJECTS, "{\"Name\":\"Q\",\"CostCeiling\":1000}");
        create(TIMESHEETS, "{\"Hours\":5,\"Cost\":10,\"Project\":" + unbudgeted + "}");
        assertProject(unbudgeted, 5.0f, null, ROLLUP_TIMEOUT_SECONDS);

        // The enforced capacity on the same child still refuses: 200 booked and 900 more past a 1000
        // ceiling.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Hours\":1,\"Cost\":900,\"Project\":" + project + "}")
                                                 .when()
                                                 .post(TIMESHEETS)
                                                 .then()
                                                 .statusCode(400)
                                                 .body(containsString("Project capacity exceeded")),
                TIMEOUT_SECONDS);
    }

    private void assertProject(int project, float actualHours, Float remainingHours, long timeoutSeconds) {
        Matcher<?> remaining = remainingHours == null ? nullValue() : equalTo(remainingHours);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(PROJECTS + "/" + project)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("ActualHours", equalTo(actualHours))
                                                 .body("RemainingHours", remaining),
                timeoutSeconds);
    }

    private void awaitInvoice(int invoice, float paid, float balance, int payments) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(INVOICES + "/" + invoice)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Paid", equalTo(paid))
                                                 .body("Balance", equalTo(balance))
                                                 .body("PaymentCount", equalTo(payments)),
                ROLLUP_TIMEOUT_SECONDS);
    }

    private void assertInvoice(int invoice, String name, float paid, float balance, int payments) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(INVOICES + "/" + invoice)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Name", equalTo(name))
                                                 .body("Paid", equalTo(paid))
                                                 .body("Balance", equalTo(balance))
                                                 .body("PaymentCount", equalTo(payments)),
                TIMEOUT_SECONDS);
    }

    private void awaitBillPaid(int bill, float paid) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(BILLS + "/" + bill)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Paid", equalTo(paid)),
                ROLLUP_TIMEOUT_SECONDS);
    }

    private void assertBill(int bill, String name, float paid) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(BILLS + "/" + bill)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Name", equalTo(name))
                                                 .body("Paid", equalTo(paid)),
                TIMEOUT_SECONDS);
    }

    private void put(String controller, int id, String body) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(body)
                                                 .when()
                                                 .put(controller + "/" + id)
                                                 .then()
                                                 .statusCode(200),
                TIMEOUT_SECONDS);
    }

    private int create(String controller, String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(controller)
                                                        .then()
                                                        .statusCode(200)
                                                        .body("Paid", nullValue())
                                                        .extract()
                                                        .path("Id")),
                TIMEOUT_SECONDS);
        return id.get();
    }

    private void generateProject() {
        AtomicReference<List<Map<String, Object>>> plan = new AtomicReference<>();
        restAssuredExecutor.execute(() -> plan.set(given().when()
                                                          .post("/services/ide/intent/generate?workspace=" + WORKSPACE + "&project="
                                                                  + PROJECT + "&path=app.intent")
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .jsonPath()
                                                          .getList("codeGenerations")));
        for (Map<String, Object> codeGeneration : plan.get()) {
            assertEquals(Boolean.TRUE, codeGeneration.get("generated"),
                    "generating code from " + codeGeneration.get("path") + " failed: " + codeGeneration.get("error"));
        }
    }

    private void publishProject() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT + "/")
                                                 .then()
                                                 .statusCode(200));
    }

    private void writeIntent() {
        String path = PROJECT_PATH + "/app.intent";
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        }
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
