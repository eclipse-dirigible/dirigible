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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
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
 * The browser half of the subset relation (issue #6878): a {@code kind: subset} renders as a
 * Harmonia multi-select whose SELECTIONS round-trip the normative csv - select, DESELECT, save,
 * reopen, clear - resolved to labels everywhere a person reads them.
 *
 * <p>
 * This needs a real browser because the widget's multiple mode is a client-side contract: Harmonia
 * switches modes from the bound model's TYPE, and the deselect path is exactly where Harmonia 2.9.0
 * corrupted the bound array (each deselect pushed a nested one-element array into the model, so the
 * deselected key still serialized - the defect that forced the 2.13/2.14 migration). No HTTP test
 * can see any of that: the generated page tokens are asserted by {@code IntentEmissionCoverageIT},
 * and they were byte-correct throughout that defect.
 */
class MultiselectHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final String PROJECT = "multiselect-it";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/schedules/index.html#";
    private static final String API = "/services/java/" + PROJECT + "/gen/schedules/api/schedule/ScheduleController";

    /**
     * The production shape (a schedule's accepted payment methods): a small seeded nomenclature and a
     * subset relation over it. The relation is deliberately OPTIONAL so the clear-to-null path is
     * drivable end-to-end; "required means at least one" is pinned by the emission unit tests.
     */
    private static final String INTENT_YAML = """
            name: schedules
            entities:
              - name: PayerType
                kind: setting
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 60 }
              - name: Schedule
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: title, type: string, length: 100 }
                relations:
                  - { name: payerTypes, kind: subset, to: PayerType }
            seeds:
              - name: payer-types
                entity: PayerType
                rows:
                  - { id: 1, name: Health fund }
                  - { id: 2, name: Paid visit }
                  - { id: 3, name: Corporate client }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void selections_round_trip_the_normative_csv_and_a_deselect_does_not_corrupt_the_model() {
        generateAndPublish();

        // Authenticate the browser session first - a bare openPath lands on the sign-in form.
        ide.openHomePage();

        // CREATE: pick two options. The popup stays open in multiple mode (a click is stopPropagation'd),
        // so both picks go through one open trigger.
        browser.openPath(APP + "/Schedule/create");
        browser.clickOnElementByAttributePatternAndText(HtmlElementType.BUTTON, HtmlAttribute.ROLE, "combobox", "Select Payer Types...");
        browser.clickOnElementByAttributePatternAndText(HtmlElementType.DIV, HtmlAttribute.ROLE, "option", "Health fund");
        browser.clickOnElementByAttributePatternAndText(HtmlElementType.DIV, HtmlAttribute.ROLE, "option", "Corporate client");
        // The trigger joins the selected labels - also the synchronization point for the two picks.
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.BUTTON, HtmlAttribute.ROLE, "combobox",
                "Health fund, Corporate client");

        // DESELECT one - the exact spot Harmonia 2.9.0 corrupted the bound array (the deselected key
        // kept serializing). The trigger dropping to the one remaining label proves the model shrank.
        browser.clickOnElementByAttributePatternAndText(HtmlElementType.DIV, HtmlAttribute.ROLE, "option", "Corporate client");
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.BUTTON, HtmlAttribute.ROLE, "combobox", "Health fund");

        // Close the popup (the trigger toggles it) so it cannot swallow the footer click, then create.
        browser.clickOnElementByAttributePatternAndText(HtmlElementType.BUTTON, HtmlAttribute.ROLE, "combobox", "Health fund");
        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Create");

        // The stored value is the normative csv of what SURVIVED the deselect - "1", never "1,3".
        AtomicReference<Integer> id = new AtomicReference<>();
        restAssuredExecutor.execute(() -> id.set(given().when()
                                                        .get(API)
                                                        .then()
                                                        .statusCode(200)
                                                        .body("$", hasSize(1))
                                                        .body("[0].PayerTypes", equalTo("1"))
                                                        .extract()
                                                        .path("[0].Id")),
                30);

        // EDIT: the stored csv preselects (the trigger resolves the label), a further pick joins back
        // ASCENDING regardless of click order - "1,2", not "2,1".
        browser.openPath(APP + "/Schedule/" + id.get() + "/edit");
        browser.clickOnElementByAttributePatternAndText(HtmlElementType.BUTTON, HtmlAttribute.ROLE, "combobox", "Health fund");
        browser.clickOnElementByAttributePatternAndText(HtmlElementType.DIV, HtmlAttribute.ROLE, "option", "Paid visit");
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.BUTTON, HtmlAttribute.ROLE, "combobox",
                "Health fund, Paid visit");
        browser.clickOnElementByAttributePatternAndText(HtmlElementType.BUTTON, HtmlAttribute.ROLE, "combobox", "Health fund, Paid visit");
        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Save");
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/" + id.get())
                                                 .then()
                                                 .statusCode(200)
                                                 .body("PayerTypes", equalTo("1,2")),
                30);

        // CLEAR: deselecting everything stores null (the ABSENT value), never "" or a lone delimiter.
        browser.openPath(APP + "/Schedule/" + id.get() + "/edit");
        browser.clickOnElementByAttributePatternAndText(HtmlElementType.BUTTON, HtmlAttribute.ROLE, "combobox", "Health fund, Paid visit");
        browser.clickOnElementByAttributePatternAndText(HtmlElementType.DIV, HtmlAttribute.ROLE, "option", "Health fund");
        browser.clickOnElementByAttributePatternAndText(HtmlElementType.DIV, HtmlAttribute.ROLE, "option", "Paid visit");
        browser.assertElementExistByAttributePatternAndText(HtmlElementType.BUTTON, HtmlAttribute.ROLE, "combobox",
                "Select Payer Types...");
        browser.clickOnElementByAttributePatternAndText(HtmlElementType.BUTTON, HtmlAttribute.ROLE, "combobox", "Select Payer Types...");
        browser.clickOnElementContainingText(HtmlElementType.BUTTON, "Save");
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/" + id.get())
                                                 .then()
                                                 .statusCode(200)
                                                 .body("PayerTypes", nullValue()),
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
