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
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * dirigible #8338: {@code checks: kind: exists} - a write is refused unless a row of ANOTHER
 * entity, owned by another model, matches values the record names - end to end on two generated,
 * published projects.
 *
 * <p>
 * A billable expense line names a project, and its claim names an employee; the line may be billed
 * to that project only when the employee is assigned to it - an assignment row, a composition child
 * of the project, owned by the projects model. No check kind could say "a matching row exists", so
 * the line was stored (200). The line's check reads one value of its own and one off its claim (one
 * hop); the claim's own check is gated on SUBMITTED, so it holds at that transition only.
 */
class IntentExistsCheckIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    /**
     * Owns the employees, the projects and the assignments. Its project name differs from the model's.
     */
    private static final String OWNER = "exists-projects";
    /** Owns the claims and their lines. */
    private static final String CONSUMER = "exists-expenses";

    private static final String OWNER_MODEL = "projects";
    private static final String CONSUMER_MODEL = "expenses";

    private static final String LINE_MESSAGE = "Only an employee assigned to the project bills an expense to it";
    private static final String LINE_MESSAGE_BG = "Само служител, назначен към проекта, може да му отчита разход";
    private static final String CLAIM_MESSAGE = "Only an employee assigned to the project submits a claim for it";

    private static final String OWNER_INTENT = """
            name: projects
            description: exists-check fixture - the owner of the assignment rows

            entities:
              - name: Employee
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string,  required: true, length: 100 }
              - name: Project
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string,  required: true, length: 100 }
              - name: EmployeeProjectAssignment
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                relations:
                  - { name: Project,  kind: manyToOne, to: Project, composition: true, required: true }
                  - { name: Employee, kind: manyToOne, to: Employee, required: true }
            """;

    private static final String CONSUMER_INTENT = """
            name: expenses
            description: exists-check fixture - a billable line needs the employee's assignment to the project
            languages: [en, bg]

            uses:
              - { model: projects, project: exists-projects }

            entities:
              - name: ClaimStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 50 }
              - name: ExpenseClaim
                checks:
                  - { kind: exists, entity: EmployeeProjectAssignment, model: projects,
                      match: { Project: Project, Employee: Employee }, status: SUBMITTED,
                      message: "Only an employee assigned to the project submits a claim for it" }
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 100 }
                relations:
                  - { name: Employee, kind: manyToOne, to: Employee, model: projects, required: true }
                  - { name: Project,  kind: manyToOne, to: Project,  model: projects }
                  - { name: Status,   kind: manyToOne, to: ClaimStatus, function: EntityStatus, init: DRAFT }
              - name: ExpenseClaimItem
                checks:
                  - { kind: exists, entity: EmployeeProjectAssignment, model: projects,
                      match: { Project: Project, Employee: ExpenseClaim.Employee },
                      when: "billable == true", verbs: [create, update],
                      message: "Only an employee assigned to the project bills an expense to it" }
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: amount,   type: decimal, precision: 18, scale: 2 }
                  - { name: billable, type: boolean }
                relations:
                  - { name: ExpenseClaim, kind: manyToOne, to: ExpenseClaim, composition: true, required: true }
                  - { name: Project,      kind: manyToOne, to: Project, model: projects }

            seeds:
              - name: claim-statuses
                entity: ClaimStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: SUBMITTED }
            """;

    /**
     * A translator's overlay, under the key the generated en-US catalog writes the line's message at.
     */
    private static final String BG_OVERLAY = """
            {
              "expenses-model": {
                "checks": {
                  "ExpenseClaimItem_exists_0": "Само служител, назначен към проекта, може да му отчита разход"
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
    void a_write_is_refused_unless_a_matching_row_of_another_entity_exists() {
        writeResource(OWNER, "app.intent", OWNER_INTENT);
        generateProject(OWNER);
        writeResource(CONSUMER, "app.intent", CONSUMER_INTENT);
        generateProject(CONSUMER);
        writeResource(CONSUMER, "i18n/bg-BG/" + CONSUMER_MODEL + ".model.json", BG_OVERLAY);

        publishProject(OWNER);
        publishProject(CONSUMER);
        synchronizationProcessor.forceProcessSynchronizers();

        String ownerApi = "/services/java/" + OWNER + "/gen/" + OWNER_MODEL + "/api";
        String consumerApi = "/services/java/" + CONSUMER + "/gen/" + CONSUMER_MODEL + "/api";
        String employees = ownerApi + "/employee/EmployeeController";
        String projects = ownerApi + "/project/ProjectController";
        String assignments = ownerApi + "/project/EmployeeProjectAssignmentController";
        String claims = consumerApi + "/expenseclaim/ExpenseClaimController";
        String lines = consumerApi + "/expenseclaim/ExpenseClaimItemController";

        int ann = create(employees, "{\"Name\":\"Ann\"}");
        int bob = create(employees, "{\"Name\":\"Bob\"}");
        int apollo = create(projects, "{\"Name\":\"Apollo\"}");
        create(assignments, "{\"Project\":" + apollo + ",\"Employee\":" + ann + "}");

        // Both claims name the project and stay drafts: the claim's own check is gated on SUBMITTED.
        int bobsClaim = create(claims, "{\"Employee\":" + bob + ",\"Project\":" + apollo + "}");
        int annsClaim = create(claims, "{\"Employee\":" + ann + ",\"Project\":" + apollo + "}");

        // The issue's reproduction: a billable line by an employee not assigned to the project was a 200.
        String unassigned = "{\"ExpenseClaim\":" + bobsClaim + ",\"Amount\":450,\"Billable\":true,\"Project\":" + apollo + "}";
        assertRefused(lines, unassigned, null, LINE_MESSAGE);
        // ...in the reader's language, through the module's catalog like every other check message.
        assertRefused(lines, unassigned, "bg", LINE_MESSAGE_BG);
        // An assigned employee bills the project.
        create(lines, "{\"ExpenseClaim\":" + annsClaim + ",\"Amount\":450,\"Billable\":true,\"Project\":" + apollo + "}");
        // `when` decides whether the rule applies at all: a non-billable line needs no assignment.
        int internal = create(lines, "{\"ExpenseClaim\":" + bobsClaim + ",\"Amount\":20,\"Billable\":false,\"Project\":" + apollo + "}");
        // A null match value skips the check, as agree's whenNull: skip does.
        create(lines, "{\"ExpenseClaim\":" + bobsClaim + ",\"Amount\":30,\"Billable\":true}");
        // The update is covered too: the internal line turned billable is refused.
        assertRefused(lines + "/" + internal,
                "{\"Id\":" + internal + ",\"ExpenseClaim\":" + bobsClaim + ",\"Amount\":20,\"Billable\":true,\"Project\":" + apollo + "}",
                null, LINE_MESSAGE);

        // The gated check holds at its transition: Bob cannot submit a claim on a project he is not
        // assigned to, Ann can.
        assertRefused(claims + "/" + bobsClaim,
                "{\"Id\":" + bobsClaim + ",\"Employee\":" + bob + ",\"Project\":" + apollo + ",\"Status\":2}", null, CLAIM_MESSAGE);
        assertStatus(claims + "/" + bobsClaim, 1);
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + annsClaim + ",\"Employee\":" + ann + ",\"Project\":" + apollo
                                                         + ",\"Status\":2}")
                                                 .when()
                                                 .put(claims + "/" + annsClaim)
                                                 .then()
                                                 .statusCode(200));
        assertStatus(claims + "/" + annsClaim, 2);
    }

    private void assertRefused(String target, String body, String language, String message) {
        boolean update = body.startsWith("{\"Id\"");
        restAssuredExecutor.execute(() -> {
            var request = given().contentType("application/json");
            if (language != null) {
                request = request.header("Accept-Language", language);
            }
            request = request.body(body)
                             .when();
            (update ? request.put(target) : request.post(target)).then()
                                                                 .statusCode(400)
                                                                 .body("message", equalTo(message));
        }, 60);
    }

    private void assertStatus(String record, int status) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(record)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(status)));
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
                60);
        return id.get();
    }

    /**
     * Generate the model files from the intent and drive model-to-code from the generate response's own
     * plan.
     */
    private void generateProject(String project) {
        AtomicReference<List<Map<String, Object>>> plan = new AtomicReference<>();
        restAssuredExecutor.execute(() -> plan.set(given().when()
                                                          .post("/services/ide/intent/generate?workspace=" + WORKSPACE + "&project="
                                                                  + project + "&path=app.intent")
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

    private void publishProject(String project) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + project + "/")
                                                 .then()
                                                 .statusCode(200));
    }

    private void writeResource(String project, String file, String content) {
        String path = projectPath(project) + "/" + file;
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(content.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String projectPath(String project) {
        return IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + project;
    }

    @AfterEach
    void cleanup() {
        for (String project : List.of(CONSUMER, OWNER)) {
            restAssuredExecutor.execute(() -> given().when()
                                                     .delete("/services/ide/publisher/" + WORKSPACE + "/" + project)
                                                     .then()
                                                     .statusCode(both(greaterThanOrEqualTo(200)).and(lessThan(300))));
            if (repository.hasCollection(projectPath(project))) {
                repository.removeCollection(projectPath(project));
            }
        }
    }
}
