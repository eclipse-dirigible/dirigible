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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.google.gson.Gson;

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

/**
 * Cross-model schedule SOURCE coverage: a schedule owned by the CONSUMER model iterates a source
 * entity that lives in another (owner) model via {@code model: <uses alias>}, generating a local
 * target record - and a cross-model forEach child - per matching source row.
 *
 * <p>
 * Two mutually-referencing intent modules: {@code xmsource} owns the source entities
 * ({@code Project} + its {@code ProjectAssignment} collection), {@code xmconsumer}
 * {@code uses: xmsource} and owns the generated {@code ProjectTimesheet} (+
 * {@code EmployeeTimesheet} lines). The schedule's source is cross-model; so is the forEach
 * collection.
 *
 * <p>
 * Per the emission + runtime testing contract the assertions target the OUTERMOST observable layer:
 * first the generated {@code Job} TOKENS (the owner-package source import, the Criteria expression,
 * the owner-package forEach class FQNs), then the RUNNING instance - the cron-fired job creates the
 * target + child rows, asserted over REST. Two loud-failure paths are covered too: the owner
 * {@code .model} absent, and a mistyped {@code where} field - both drop the schedule with a warning
 * in the generate response (never a job that cannot compile).
 */
// One Dirigible boot for the whole class: each method cleans up after itself (or is read-only), so
// the per-method context reset inherited from IntegrationTest would only add boot time per test.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentCrossModelScheduleSourceIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String OWNER = "xmsource";
    private static final String CONSUMER = "xmconsumer";

    /** The owner module: the schedule's cross-model source entities. */
    private static final String OWNER_INTENT = """
            name: xmsource
            description: cross-model schedule source fixture - owns the source entities

            entities:
              - name: Project
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: name,   type: string,  required: true, length: 100 }
                  - { name: status, type: integer, required: true }

              - name: ProjectAssignment
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: project,  type: integer, required: true }
                  - { name: employee, type: string,  required: true, length: 100 }
            """;

    /**
     * The consumer module: owns the created rows and schedules off the owner's Project (+ its
     * ProjectAssignment collection), both cross-model. A fast cron so the job fires within the test.
     */
    private static String consumerIntent(String whereField) {
        return """
                name: xmconsumer
                description: cross-model schedule consumer fixture - owns created rows, schedules off xmsource

                uses:
                  - { model: xmsource }

                entities:
                  - name: ProjectTimesheet
                    fields:
                      - { name: id,      type: integer, primaryKey: true, generated: true }
                      - { name: project, type: integer }
                      - { name: period,  type: date }

                  - name: EmployeeTimesheet
                    fields:
                      - { name: id,       type: integer, primaryKey: true, generated: true }
                      - { name: employee, type: string, length: 100 }
                    relations:
                      - { name: ProjectTimesheet, kind: manyToOne, to: ProjectTimesheet }

                schedules:
                  - name: monthlyProjectTimesheets
                    cron: "0/5 * * * * ?"
                    entity: Project
                    model: xmsource
                    where:
                      - { field: %s, op: eq, value: 1 }
                    generate:
                      to: ProjectTimesheet
                      map:
                        Project: id
                      defaults:
                        Period: now
                      children:
                        - to: EmployeeTimesheet
                          parent: ProjectTimesheet
                          forEach:
                            entity: ProjectAssignment
                            model: xmsource
                            match: { Project: id }
                          map: { Employee: Employee }
                """.formatted(whereField);
    }

    @Autowired
    private IRepository repository;
    @Autowired
    private RestAssuredExecutor restAssuredExecutor;
    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void cross_model_source_schedule_generates_target_and_child_rows() {
        generateProject(OWNER, OWNER_INTENT);
        generateProject(CONSUMER, consumerIntent("status"));

        assertJobTokens();

        publishProject(OWNER);
        publishProject(CONSUMER);
        synchronizationProcessor.forceProcessSynchronizers();

        // Seed the source rows over the OWNER's REST controllers: one active Project + two assignments.
        int projectId = createProject();
        createAssignment(projectId, "emp-1");
        createAssignment(projectId, "emp-2");

        assertGeneratedRowsAppear();
    }

    /**
     * Layer 1: the generated Job imports the OWNER's gen classes and queries the cross-model source.
     */
    private void assertJobTokens() {
        String job = contentOf(CONSUMER, "gen/events/xmconsumer/MonthlyProjectTimesheetsJob.java");
        assertTrue(job.contains("package gen.events.xmconsumer;"), "the job lives in the consumer's events package");
        assertTrue(job.contains("import gen.xmsource.data.project.ProjectEntity;"),
                "the source entity import must resolve against the OWNER's gen package");
        assertTrue(job.contains("import gen.xmsource.data.project.ProjectRepository;"),
                "the source repository import must resolve against the OWNER's gen package");
        assertTrue(job.contains(".eq(\"Status\", 1)"), "the Criteria must filter the source rows: " + job);
        assertTrue(job.contains("gen.xmsource.data.projectassignment.ProjectAssignmentRepository"),
                "the cross-model forEach collection class must resolve against the OWNER's gen package");
        assertTrue(job.contains("gen.xmconsumer.data.projecttimesheet.ProjectTimesheetEntity"),
                "the generation target stays local to the consumer");
        assertTrue(job.contains("gen.xmconsumer.data.employeetimesheet.EmployeeTimesheetEntity"),
                "the child target stays local to the consumer");
    }

    /**
     * Layer 2: the running instance - the cron-fired job reads the cross-model source and creates the
     * local target + child rows, so both consumer controllers report a non-empty count.
     */
    private void assertGeneratedRowsAppear() {
        String api = "/services/java/" + CONSUMER + "/gen/xmconsumer/api";
        // The job fires every five seconds; allow a generous window for sync + the first tick.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(api + "/projecttimesheet/ProjectTimesheetController/count")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("count", greaterThanOrEqualTo(1)),
                90);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(api + "/employeetimesheet/EmployeeTimesheetController/count")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("count", greaterThanOrEqualTo(1)),
                90);
    }

    @Test
    void missing_owner_model_drops_the_schedule_with_a_warning() {
        // The consumer is generated WITHOUT the owner model present in the workspace or registry, so the
        // cross-model source cannot be resolved: the schedule is dropped with a warning, not a 422.
        writeIntent(CONSUMER, consumerIntent("status"));
        List<String> warnings = generateWarnings(CONSUMER);
        assertTrue(warnings.stream()
                           .anyMatch(w -> w.contains("monthlyProjectTimesheets") && w.contains("cannot be resolved")
                                   && w.contains("xmsource")),
                "expected an unresolvable-owner warning naming the schedule and owner model, got: " + warnings);
    }

    @Test
    void mistyped_where_field_drops_the_schedule_naming_the_owner_model() {
        // With the owner model present, a where field that does not exist on the owner's Project is
        // caught at generation and the schedule is dropped with a warning naming the owner model.
        generateProject(OWNER, OWNER_INTENT);
        writeIntent(CONSUMER, consumerIntent("nonexistentField"));
        List<String> warnings = generateWarnings(CONSUMER);
        assertTrue(warnings.stream()
                           .anyMatch(w -> w.contains("monthlyProjectTimesheets") && w.contains("where field [nonexistentField]")
                                   && w.contains("xmsource")),
                "expected a where-field warning naming the schedule, field and owner model, got: " + warnings);
    }

    private int createProject() {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body("{\"Name\":\"Alpha\",\"Status\":1}")
                                                        .when()
                                                        .post("/services/java/" + OWNER + "/gen/xmsource/api/project/ProjectController")
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                60);
        return id.get();
    }

    private void createAssignment(int projectId, String employee) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Project\":" + projectId + ",\"Employee\":\"" + employee + "\"}")
                                                 .when()
                                                 .post("/services/java/" + OWNER
                                                         + "/gen/xmsource/api/projectassignment/ProjectAssignmentController")
                                                 .then()
                                                 .statusCode(200));
    }

    /** Write the intent and drive model-to-code from the generate response's own plan. */
    private void generateProject(String project, String yaml) {
        writeIntent(project, yaml);
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
            String template = String.valueOf(codeGeneration.get("templateId"));
            String modelPath = String.valueOf(codeGeneration.get("path"));
            String parameters = new Gson().toJson(codeGeneration.get("parameters"));
            String payload = "{\"template\":\"" + template + "\",\"parameters\":" + parameters + "}";
            restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                     .body(payload)
                                                     .when()
                                                     .post("/services/js/service-generate/generate.mjs/model/" + WORKSPACE + "/" + project
                                                             + "?path=" + modelPath)
                                                     .then()
                                                     .statusCode(201));
        }
    }

    /** Generate the intent (models only) and return the non-fatal warnings from the response. */
    private List<String> generateWarnings(String project) {
        AtomicReference<List<String>> warnings = new AtomicReference<>();
        restAssuredExecutor.execute(() -> warnings.set(given().when()
                                                              .post("/services/ide/intent/generate?workspace=" + WORKSPACE + "&project="
                                                                      + project + "&path=app.intent")
                                                              .then()
                                                              .statusCode(200)
                                                              .extract()
                                                              .jsonPath()
                                                              .getList("warnings")));
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
        for (String project : List.of(OWNER, CONSUMER)) {
            restAssuredExecutor.execute(() -> given().when()
                                                     .delete("/services/ide/publisher/" + WORKSPACE + "/" + project)
                                                     .then()
                                                     .statusCode(both(greaterThanOrEqualTo(200)).and(lessThan(300))));
            if (repository.hasCollection(projectPath(project))) {
                repository.removeCollection(projectPath(project));
            }
        }
        // The context (and its Quartz scheduler) now outlives the method: run the synchronizers so the
        // JobSynchronizer's DELETE branch unschedules the published 5-second cron job - otherwise it
        // keeps firing against unloaded gen classes for the rest of the class.
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
