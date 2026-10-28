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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;

/**
 * A to-one relation picked on create and fixed afterwards (intent {@code fixedAfterCreate: true},
 * dirigible #7903).
 *
 * <p>
 * The case: an employee moved their own timesheet to another project-month, so its hours landed on
 * another customer's invoice. No construct could say "the user picks it when creating the record
 * and it never moves afterwards" - an update recompute disables the picker on the New form too, a
 * relation refuses {@code readOnly:}, and {@code immutableWhen} locks the whole record.
 *
 * <p>
 * Each answer the guard gives is asserted on the running, generated code, because each is a way it
 * could be wrong: a changed value is refused with a 409 on the power, the personal and the partner
 * surface, in the request's language; the same value and an omitted one still save, the omitted one
 * keeping the stored relation; and a system write - here a workflow step moving the very relation -
 * still lands, since it never meets a controller.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentFixedAfterCreateIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "fixedrel";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String TIMESHEETS = API + "/timesheet/TimesheetController";
    private static final String MY_TIMESHEETS = API + "/timesheet/TimesheetMyController";
    private static final String EXPENSES = API + "/expense/ExpenseController";
    private static final String PARTNER_EXPENSES = API + "/expense/ExpensePartnerController";
    private static final String CATALOG_KEY = "Timesheet_fixed_Project";
    private static final String REFUSAL = "'Project' cannot change once the Timesheet is created";
    private static final String REFUSAL_BG = "Проектът не може да се смени след създаването на отчета";
    private static final long TIMEOUT_SECONDS = 90;
    /** The workflow step runs once the create event has started its instance. */
    private static final long PROCESS_TIMEOUT_SECONDS = 60;

    private static final String INTENT_YAML = """
            name: fixedrel
            description: a timesheet's project and owner are picked on create and fixed afterwards
            languages: [en, bg]

            entities:
              - name: Employee
                identity: email
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: name,  type: string, required: true, length: 100 }
                  - { name: email, type: string, required: true, length: 100 }

              - name: Project
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: Timesheet
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 200 }
                relations:
                  - { name: Project,  kind: manyToOne, to: Project, required: true, fixedAfterCreate: true }
                  - { name: Employee, kind: manyToOne, to: Employee, required: true, personal: true, fixedAfterCreate: true }

              # The partner surface guards it too.
              - name: Expense
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 200 }
                relations:
                  - { name: Project,  kind: manyToOne, to: Project, required: true, fixedAfterCreate: true }
                  - { name: Employee, kind: manyToOne, to: Employee, required: true, partner: true }

            processes:
              # A system writer of the fixed relation: the guard is the controllers', so this still lands.
              - name: Reassign
                trigger: { onCreate: Timesheet }
                steps:
                  - { name: move, kind: serviceTask, args: { setRelationField: Project, value: 2 } }
                  - { name: end,  kind: end }

            seeds:
              - name: employees
                entity: Employee
                rows:
                  - { id: 1, name: Admin, email: admin }
              - name: projects
                entity: Project
                rows:
                  - { id: 1, name: Alpha }
                  - { id: 2, name: Beta }
                  - { id: 3, name: Gamma }
            """;

    /** A translator's overlay of the refusal, under the key the generated en-US catalog writes. */
    private static final String BG_OVERLAY = """
            {
              "fixedrel-model": {
                "checks": {
                  "Timesheet_fixed_Project": "Проектът не може да се смени след създаването на отчета"
                }
              }
            }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_relation_fixed_after_create_is_refused_to_a_user_update_and_still_written_by_the_system() {
        generateProject();
        assertGeneratedCatalog();
        assertGeneratedForms();
        writeResource(PROJECT_PATH + "/i18n/bg-BG/fixedrel.model.json", BG_OVERLAY);
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        // Picked on create, as on the New form.
        int timesheet = create(TIMESHEETS, "{\"Note\":\"week 1\",\"Project\":1,\"Employee\":1}");
        // The system write lands: the workflow step moves the fixed relation through the repository.
        awaitProject(timesheet, 2);

        // A user update that moves it is refused, in the reader's language.
        put(TIMESHEETS, timesheet, "{\"Note\":\"week 1\",\"Project\":3,\"Employee\":1}", null).statusCode(409)
                                                                                              .body("message", equalTo(REFUSAL));
        put(TIMESHEETS, timesheet, "{\"Note\":\"week 1\",\"Project\":3,\"Employee\":1}", "bg").statusCode(409)
                                                                                              .body("message", equalTo(REFUSAL_BG));
        // ...and so is one through the personal surface, and through the partner one.
        put(MY_TIMESHEETS, timesheet, "{\"Note\":\"week 1\",\"Project\":3}", null).statusCode(409)
                                                                                  .body("message", equalTo(REFUSAL));
        int expense = create(EXPENSES, "{\"Note\":\"taxi\",\"Project\":1,\"Employee\":1}");
        put(PARTNER_EXPENSES, expense, "{\"Note\":\"taxi\",\"Project\":3}", null).statusCode(409)
                                                                                 .body("message", equalTo(
                                                                                         "'Project' cannot change once the Expense is created"));
        put(PARTNER_EXPENSES, expense, "{\"Note\":\"taxi home\"}", null).statusCode(200)
                                                                        .body("Project", equalTo(1));

        // An ordinary edit still saves - it carries the relation back unchanged...
        put(TIMESHEETS, timesheet, "{\"Note\":\"edited\",\"Project\":2,\"Employee\":1}", null).statusCode(200);
        // ...and one that omits it keeps the stored value instead of erasing it.
        put(TIMESHEETS, timesheet, "{\"Note\":\"edited again\",\"Employee\":1}", null).statusCode(200);
        put(MY_TIMESHEETS, timesheet, "{\"Note\":\"mine\"}", null).statusCode(200);
        read(timesheet).body("Project", equalTo(2))
                       .body("Note", equalTo("mine"));
    }

    /**
     * The New form offers the picker and the Edit form renders it read-only, on every surface; the
     * relation the record does not fix stays editable.
     */
    private void assertGeneratedForms() {
        String expenseForm = contentOf("gen/" + PROJECT + "/views/Expense/Expense-form.html");
        assertTrue(elementAt(expenseForm, "x-h-select-input id=\"f_Project\"").contains(":disabled=\"isPreview || isEdit\""), expenseForm);
        assertTrue(elementAt(expenseForm, "x-h-select-input id=\"f_Employee\"").contains(":disabled=\"isPreview\""), expenseForm);
        String myForm = contentOf("gen/" + PROJECT + "/views/my/Timesheet-form.html");
        assertTrue(elementAt(myForm, "x-model=\"form.Project\"").contains(":disabled=\"mode === 'edit'\""), myForm);
        String partnerForm = contentOf("gen/" + PROJECT + "/views/partner/Expense-form.html");
        assertTrue(elementAt(partnerForm, "x-model=\"form.Project\"").contains(":disabled=\"mode === 'edit'\""), partnerForm);
    }

    /** The element a marker opens, up to its self-closing end - the picker's attributes may wrap. */
    private static String elementAt(String content, String marker) {
        int start = content.indexOf(marker);
        if (start < 0) {
            throw new AssertionError("no element with " + marker + " in: " + content);
        }
        return content.substring(start, content.indexOf("/>", start));
    }

    private String contentOf(String fileName) {
        return new String(repository.getResource(PROJECT_PATH + "/" + fileName)
                                    .getContent(),
                StandardCharsets.UTF_8);
    }

    /** The refusal's default text is in the en-US catalog under its stable key, for a translator. */
    private void assertGeneratedCatalog() {
        IResource catalog = repository.getResource(PROJECT_PATH + "/i18n/en-US/fixedrel.model.json");
        assertTrue(catalog.exists(), "the generated en-US catalog is missing");
        String content = new String(catalog.getContent(), StandardCharsets.UTF_8);
        assertTrue(content.contains("\"" + CATALOG_KEY + "\": \"" + REFUSAL + "\""), content);
    }

    private ValidatableResponse put(String controller, int id, String body, String language) {
        AtomicReference<ValidatableResponse> response = new AtomicReference<>();
        restAssuredExecutor.execute(() -> response.set(request(language).body(body)
                                                                        .when()
                                                                        .put(controller + "/" + id)
                                                                        .then()),
                TIMEOUT_SECONDS);
        return response.get();
    }

    private ValidatableResponse read(int timesheet) {
        AtomicReference<ValidatableResponse> response = new AtomicReference<>();
        restAssuredExecutor.execute(() -> response.set(given().when()
                                                              .get(TIMESHEETS + "/" + timesheet)
                                                              .then()
                                                              .statusCode(200)));
        return response.get();
    }

    private void awaitProject(int timesheet, int project) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(TIMESHEETS + "/" + timesheet)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Project", equalTo(project)),
                PROCESS_TIMEOUT_SECONDS);
    }

    private int create(String controller, String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(controller)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                TIMEOUT_SECONDS);
        return id.get();
    }

    private static RequestSpecification request(String language) {
        RequestSpecification request = given().contentType("application/json");
        return language == null ? request : request.header("Accept-Language", language);
    }

    private void generateProject() {
        writeResource(PROJECT_PATH + "/app.intent", INTENT_YAML);
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

    private void writeResource(String path, String content) {
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(content.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, content.getBytes(StandardCharsets.UTF_8));
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
