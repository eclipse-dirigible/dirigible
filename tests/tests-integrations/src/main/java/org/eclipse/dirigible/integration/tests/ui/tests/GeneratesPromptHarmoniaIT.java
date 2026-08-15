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
import org.eclipse.dirigible.tests.framework.browser.HtmlAttribute;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The browser half of the prompted create-from (issue #6685): a per-record generate action carrying
 * a {@code prompt:} must open the input dialog instead of the plain confirm, render its controls
 * from the target's detail registration, run the target's own {@code dependsOn:} cascade (the
 * payment list narrows to the invoice's customer, the amount defaults to the picked payment), and
 * create the record with the collected values.
 *
 * <p>
 * The HTTP layers (parser, glue, generated controller, 400 on a missing required input) are covered
 * by {@code IntentEmissionCoverageIT}; this test exists because the dialog itself has no DOM or
 * server-side symptom when it breaks - an Alpine failure in the shared shells is visible only in a
 * real browser.
 */
class GeneratesPromptHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final String PROJECT = "generates-prompt-it";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String API = "/services/java/" + PROJECT + "/gen/allocations/api";

    /**
     * The canonical prompted create-from: manual payment allocation. The allocation child authors the
     * whole cascade itself - Customer defaults from the invoice, the payment list filters to that
     * customer, the amount defaults from the picked payment - and the prompt only names the two answers
     * the source cannot derive.
     */
    private static final String INTENT_YAML = """
            name: allocations
            entities:
              - name: Customer
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true }
              - name: CustomerPayment
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, required: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer, required: true }
              - name: SalesInvoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, required: true }
                  - { name: total, type: decimal }
                relations:
                  - { name: Customer, kind: manyToOne, to: Customer, required: true }
              - name: SalesInvoiceCustomerPayment
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, required: true,
                      dependsOn: { relation: CustomerPayment, valueFrom: amount } }
                relations:
                  - { name: SalesInvoice, kind: manyToOne, to: SalesInvoice, composition: true, required: true }
                  - { name: Customer, kind: manyToOne, to: Customer,
                      dependsOn: { relation: SalesInvoice, valueFrom: Customer } }
                  - { name: CustomerPayment, kind: manyToOne, to: CustomerPayment, required: true,
                      dependsOn: { relation: Customer, filterBy: Customer } }
            generates:
              - name: allocate-payment
                from: SalesInvoice
                to: SalesInvoiceCustomerPayment
                label: Allocate Payment
                icon: link
                map:
                  SalesInvoice: id
                  Customer: Customer
                prompt:
                  - { field: CustomerPayment, required: true }
                  - { field: amount, required: true }
            seeds:
              - name: customers
                entity: Customer
                rows:
                  - { id: 1, name: Acme }
                  - { id: 2, name: Globex }
              - name: payments
                entity: CustomerPayment
                rows:
                  - { id: 1, number: PAY-001, amount: 100.00, Customer: 1 }
                  - { id: 2, number: PAY-002, amount: 250.00, Customer: 1 }
                  - { id: 3, number: PAY-003, amount: 999.00, Customer: 2 }
              - name: invoices
                entity: SalesInvoice
                rows:
                  - { id: 1, number: INV-001, total: 300.00, Customer: 1 }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void prompted_generate_collects_input_runs_the_cascade_and_creates_the_allocation() {
        generateAndPublish();

        // Authenticate the browser session first - a bare openPath lands on the sign-in form.
        ide.openHomePage();

        // The SalesInvoice master page; selecting the row is what arms the per-record actions.
        browser.openPath("/services/web/" + PROJECT + "/gen/allocations/index.html#/SalesInvoice");
        // The cell, not the row: the framework's text condition is a FULL match of the element's
        // text, and a <tr>'s text is every cell concatenated.
        browser.clickOnElementContainingText("td", "INV-001");

        // A prompted action must open the INPUT dialog (not the plain confirm): the two declared
        // controls, typed from the target's detail registration. The title is the action's label.
        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Allocate Payment");
        browser.assertElementExistsByTypeAndText(HtmlElementType.HEADER2, "Allocate Payment");

        // The payment combobox rendered (its trigger shows the placeholder) - the one visible
        // dropdown control the prompt declared.
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.SPAN, HtmlAttribute.ROLE, "combobox", "Select...");

        // From here the flow is asserted and completed through the customActions STORE - the same
        // state and methods every dialog control binds. WebDriver's synthesized clicks on the
        // Harmonia popup dismiss the overlay stack (a real pointer does not - the full pointer
        // flow is verified interactively on #6685), so the pointer-level pick is not assertable
        // here. What still runs IN THE PAGE: the option loading + dependsOn narrowing, the
        // valueFrom amount default, and promptRun's required-check + POST. The frame-iterating
        // finders above may leave the driver inside an iframe - return to the top document first.
        com.codeborne.selenide.Selenide.switchTo()
                                       .defaultContent();

        // The offered set must be the dependsOn-narrowed one: ONLY the invoice's customer's
        // payments (the invoice -> Customer -> filterBy chain, seeded from the clicked record
        // before the user touches anything). The seeding is async, so poll.
        String offered = null;
        for (int i = 0; i < 40; i++) {
            offered = com.codeborne.selenide.Selenide.executeJavaScript("var s = window.Alpine && Alpine.store('customActions');"
                    + "return s && s.promptOptions && s.promptOptions.CustomerPayment"
                    + " ? s.promptOptions.CustomerPayment.map(o => o.text).join(',') : 'pending';");
            if ("PAY-001,PAY-002".equals(offered)) {
                break;
            }
            com.codeborne.selenide.Selenide.sleep(250);
        }
        assertEquals("PAY-001,PAY-002", offered,
                "the payment options must be narrowed to the invoice's customer (PAY-003 belongs to another customer)");

        // Pick PAY-002 - the store write every option click performs.
        com.codeborne.selenide.Selenide.executeJavaScript(
                "var s = Alpine.store('customActions'); s.promptValues.CustomerPayment = '2'; s.promptChanged('CustomerPayment');");
        String promptState = null;
        for (int i = 0; i < 40; i++) {
            promptState = com.codeborne.selenide.Selenide.executeJavaScript("var s = window.Alpine && Alpine.store('customActions');"
                    + "return s && s.promptValues ? String(s.promptValues.CustomerPayment) + '/' + String(s.promptValues.Amount)"
                    + " : 'no-store';");
            if ("2/250".equals(promptState)) {
                break;
            }
            com.codeborne.selenide.Selenide.sleep(250);
        }
        assertEquals("2/250", promptState,
                "picking PAY-002 must set the prompted FK and default the amount from the picked payment (valueFrom)");

        // A partial allocation: lower the amount, then run - promptRun() is the exact method the
        // dialog's Run button invokes (required-check + POST {id, values}).
        com.codeborne.selenide.Selenide.executeJavaScript(
                "var s = Alpine.store('customActions'); s.promptValues.Amount = 50; s.promptRun();");

        // The outermost assertion: the allocation row exists with the prompted values, created
        // through the generated controller (the POST is async from the click - retry-wrapped).
        restAssuredExecutor.execute(() -> {
            io.restassured.path.json.JsonPath rows = given().when()
                                                            .get(API + "/salesinvoice/SalesInvoiceCustomerPaymentController")
                                                            .then()
                                                            .statusCode(200)
                                                            .extract()
                                                            .jsonPath();
            String matching = "findAll { it.SalesInvoice == 1 && it.CustomerPayment == 2 }";
            assertEquals(1, rows.getList(matching)
                                .size(),
                    "the prompted allocation must exist exactly once");
            assertEquals(50.0f, rows.getFloat(matching + ".Amount[0]"), 0.001f,
                    "the edited (partial) amount must win over the cascade default");
        }, 30);
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
        // Generate runs the model-to-code recipes itself, in the same call - assert each one succeeded.
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
