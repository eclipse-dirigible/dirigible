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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.browser.HtmlElementType;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The browser half of a transition that asks for input (#7909): a {@code transitions:} button
 * carrying a {@code prompt:} opens the shell's input dialog instead of the plain confirm, with the
 * record's own field as its control, refuses a Run without the required answer, and posts the
 * answer with the move. The HTTP layers are {@code IntentTransitionPromptIT}'s; this one exists
 * because the dialog has no server-side symptom when it fails to open.
 */
class TransitionPromptHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final String PROJECT = "transition-prompt-it";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String INVOICES = "/services/java/" + PROJECT + "/gen/voids/api/invoice/InvoiceController";

    private static final String INTENT_YAML = """
            name: voids
            entities:
              - name: InvoiceStatus
                function: Setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true }
              - name: Invoice
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, required: true }
                  - { name: voidReason, type: string, length: 200, label: Reason for voiding, readOnly: true }
                relations:
                  - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
            transitions:
              - name: VoidInvoice
                forEntity: Invoice
                from: [2]
                setStatus: 3
                label: Void
                prompt:
                  - { field: voidReason, required: true }
            seeds:
              - name: statuses
                entity: InvoiceStatus
                rows:
                  - { id: 1, name: Draft }
                  - { id: 2, name: Issued }
                  - { id: 3, name: Voided }
              - name: invoices
                entity: Invoice
                rows:
                  - { id: 1, number: INV-001, Status: 2 }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void a_prompted_transition_opens_the_input_dialog_and_moves_with_the_answer() {
        generateAndPublish();
        ide.openHomePage();

        browser.openPath("/services/web/" + PROJECT + "/gen/voids/index.html#/Invoice");
        browser.clickOnElementContainingText("td", "INV-001");
        com.codeborne.selenide.Selenide.$("[x-h-sheet] button[aria-label='More actions']")
                                       .shouldBe(com.codeborne.selenide.Condition.visible)
                                       .click();

        // The INPUT dialog, not the plain confirm: its title is the action's label and its control the
        // record's own field.
        browser.clickOnElementContainingText(HtmlElementType.LI, "Void");
        browser.assertElementExistsByTypeAndText(HtmlElementType.HEADER2, "Void");
        com.codeborne.selenide.Selenide.switchTo()
                                       .defaultContent();
        com.codeborne.selenide.Selenide.$("[data-prompt-dialog] input[data-prompt-field='VoidReason']")
                                       .shouldBe(com.codeborne.selenide.Condition.visible);

        // Run without the answer: the dialog's required check names it and nothing is posted. Driven
        // through the store, the same method the Run button invokes (see GeneratesPromptHarmoniaIT for
        // why the pointer flow is not).
        String missing = com.codeborne.selenide.Selenide.executeJavaScript(
                "var s = Alpine.store('customActions'); s.promptRun(); return s.promptError;");
        assertEquals("Reason for voiding", missing, "a Run without the required reason must be refused in the dialog");

        com.codeborne.selenide.Selenide.executeJavaScript(
                "var s = Alpine.store('customActions'); s.promptValues.VoidReason = 'Wrong customer'; s.promptRun();");
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(INVOICES + "/1")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(3))
                                                 .body("VoidReason", equalTo("Wrong customer")),
                30);
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
