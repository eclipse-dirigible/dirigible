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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

/**
 * dirigible #7979: an OPTIONAL {@code uses:} entry. One expenses intent, generated, compiled and
 * run twice - first in a workspace WITHOUT the projects model it optionally uses, then again after
 * the projects model is generated next to it.
 *
 * <p>
 * Without the owner the Generate succeeds and says what it left out: the cross-model relation, the
 * two checks naming it and the report grouping by it are not generated, everything else is - a
 * billable line is saved without a project, the later positional check still refuses, and the other
 * report answers. With the owner the same intent behaves as a mandatory {@code uses:} always did:
 * the relation, both checks and the report are back.
 */
class IntentOptionalUsesIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String OWNER = "optuses-projects";
    private static final String CONSUMER = "optuses-expenses";
    private static final String OWNER_MODEL = "projects";
    private static final String CONSUMER_MODEL = "expenses";

    private static final String NEEDS_PROJECT = "A billable line needs the project it is billed to";
    private static final String NOT_BILLABLE = "The project is not billable";
    private static final String CEILING = "A line cannot exceed 1000";

    private static final String OWNER_INTENT = """
            name: projects
            description: optional uses fixture - the owner model, present only in the second half

            entities:
              - name: Project
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: name,     type: string,  required: true, length: 100 }
                  - { name: billable, type: boolean, defaultValue: true }
            """;

    private static final String CONSUMER_INTENT = """
            name: expenses
            description: optional uses fixture - billable lines need projects only where projects exist

            uses:
              - { model: projects, project: optuses-projects, optional: true }

            entities:
              - name: ExpenseLine
                order: [Id, description, Project, billable, amount]
                checks:
                  - { kind: compare, field: amount, op: gt, value: 0, message: "A line must be a positive amount" }
                  - { kind: requiredWhen, field: Project, when: "billable == true",
                      message: "A billable line needs the project it is billed to" }
                  - { kind: forbidWhen, when: ["billable == true", "Project.billable == false"], verbs: [create, update],
                      message: "The project is not billable" }
                  - { kind: compare, field: amount, op: le, value: 1000, message: "A line cannot exceed 1000" }
                fields:
                  - { name: id,          type: integer, primaryKey: true, generated: true }
                  - { name: description, type: string,  length: 200 }
                  - { name: amount,      type: decimal, precision: 18, scale: 2, required: true }
                  - { name: billable,    type: boolean, defaultValue: false }
                relations:
                  - { name: Project, kind: manyToOne, to: Project, model: projects }

            reports:
              - name: LinesByProject
                source: ExpenseLine
                dimensions: [Project, billable]
                measures: ["count(*)", "sum(amount)"]
              - name: LinesByBillable
                source: ExpenseLine
                dimensions: [billable]
                measures: ["count(*)", "sum(amount)"]
            """;

    @Autowired
    private IRepository repository;
    @Autowired
    private RestAssuredExecutor restAssuredExecutor;
    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    private final String lines = "/services/java/" + CONSUMER + "/gen/" + CONSUMER_MODEL + "/api/expenseline/ExpenseLineController";
    private final String byProject = "/services/java/" + CONSUMER + "/gen/linesbyproject/api/reports/LinesByProjectController";
    private final String byBillable = "/services/java/" + CONSUMER + "/gen/linesbybillable/api/reports/LinesByBillableController";

    @Test
    void the_same_intent_runs_without_and_then_with_its_optional_model() {
        // ---- the projects model is absent: generated, compiled and run without it --------------
        writeIntent(CONSUMER, CONSUMER_INTENT);
        List<String> warnings = generateProject(CONSUMER);
        String dropped = String.join("\n", warnings);
        assertTrue(dropped.contains("uses [projects] is optional"), "the Generate must say what it left out: " + warnings);
        assertTrue(dropped.contains("relation [ExpenseLine.Project]"), dropped);
        assertTrue(dropped.contains("report [LinesByProject]"), dropped);
        assertTrue(dropped.contains("check [ExpenseLine_requiredWhen_1]"), dropped);
        assertTrue(dropped.contains("check [ExpenseLine_forbidWhen_2]"), dropped);
        assertFalse(contentOf(CONSUMER, CONSUMER_MODEL + ".model").contains("\"Project\""),
                "no Project property without the projects model");
        assertFalse(exists(CONSUMER, "LinesByProject.report"), "a report grouping by the absent relation is not generated");
        assertFalse(contentOf(CONSUMER, "gen/" + CONSUMER_MODEL + "/api/expenseline/ExpenseLineController.java").contains("gen.projects"),
                "the generated controller must not reference the absent model");

        publishProject(CONSUMER);
        synchronizationProcessor.forceProcessSynchronizers();

        // The requiredWhen and the forbidWhen on the absent relation are not there: a billable line saves.
        int line = create(lines, "{\"Description\":\"Taxi\",\"Amount\":10,\"Billable\":true}");
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(lines + "/" + line)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("$", not(hasKey("Project")))
                                                 .body("Billable", equalTo(true)),
                60);
        // The check AFTER the dropped ones still refuses, under its own message.
        refused(lines, "{\"Description\":\"Flight\",\"Amount\":2000,\"Billable\":false}", CEILING);
        // The report grouping by the absent relation is not deployed; the other one answers.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(byProject)
                                                 .then()
                                                 .statusCode(404));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(byBillable)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("size()", equalTo(1))
                                                 .body("[0].Count", equalTo(1)),
                60);

        // ---- the projects model is generated next to it: the same intent, as today -----------
        writeIntent(OWNER, OWNER_INTENT);
        generateProject(OWNER);
        List<String> withOwner = generateProject(CONSUMER);
        assertFalse(String.join("\n", withOwner)
                          .contains("is optional"),
                "with the owner present nothing is left out: " + withOwner);
        assertTrue(contentOf(CONSUMER, CONSUMER_MODEL + ".model").contains("\"Project\""), "the relation is back with the projects model");
        assertTrue(exists(CONSUMER, "LinesByProject.report"), "the report is back with the projects model");

        publishProject(OWNER);
        publishProject(CONSUMER);
        synchronizationProcessor.forceProcessSynchronizers();

        String projects = "/services/java/" + OWNER + "/gen/" + OWNER_MODEL + "/api/project/ProjectController";
        int internal = create(projects, "{\"Name\":\"Internal\",\"Billable\":false}");
        int client = create(projects, "{\"Name\":\"Client\",\"Billable\":true}");

        refused(lines, "{\"Description\":\"Hotel\",\"Amount\":20,\"Billable\":true}", NEEDS_PROJECT);
        refused(lines, "{\"Description\":\"Hotel\",\"Amount\":20,\"Billable\":true,\"Project\":" + internal + "}", NOT_BILLABLE);
        int billed = create(lines, "{\"Description\":\"Hotel\",\"Amount\":20,\"Billable\":true,\"Project\":" + client + "}");
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(lines + "/" + billed)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Project", equalTo(client)),
                60);
        refused(lines, "{\"Description\":\"Flight\",\"Amount\":2000,\"Billable\":false}", CEILING);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(byProject)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("find { it.Project == 'Client' }.Count", equalTo(1)),
                60);
    }

    private void refused(String controller, String body, String message) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(body)
                                                 .when()
                                                 .post(controller)
                                                 .then()
                                                 .statusCode(400)
                                                 .body(containsString(message)),
                60);
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
     * Generate the model files and the code (the response's own plan), and answer the Generate's
     * warnings.
     */
    private List<String> generateProject(String project) {
        AtomicReference<List<Map<String, Object>>> plan = new AtomicReference<>();
        AtomicReference<List<String>> warnings = new AtomicReference<>();
        restAssuredExecutor.execute(() -> {
            io.restassured.path.json.JsonPath response = given().when()
                                                                .post("/services/ide/intent/generate?workspace=" + WORKSPACE + "&project="
                                                                        + project + "&path=app.intent")
                                                                .then()
                                                                .statusCode(200)
                                                                .extract()
                                                                .jsonPath();
            plan.set(response.getList("codeGenerations"));
            warnings.set(response.getList("warnings", String.class));
        });
        for (Map<String, Object> codeGeneration : plan.get()) {
            assertEquals(Boolean.TRUE, codeGeneration.get("generated"),
                    "generating code from " + codeGeneration.get("path") + " failed: " + codeGeneration.get("error"));
        }
        return warnings.get();
    }

    private void publishProject(String project) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + project + "/")
                                                 .then()
                                                 .statusCode(200));
    }

    private void writeIntent(String project, String yaml) {
        String path = projectPath(project) + "/app.intent";
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(yaml.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, yaml.getBytes(StandardCharsets.UTF_8));
        }
    }

    private boolean exists(String project, String fileName) {
        return repository.getResource(projectPath(project) + "/" + fileName)
                         .exists();
    }

    private String contentOf(String project, String fileName) {
        return new String(repository.getResource(projectPath(project) + "/" + fileName)
                                    .getContent(),
                StandardCharsets.UTF_8);
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
