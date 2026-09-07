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

/**
 * dirigible #7092: an entity-level {@code unique:} key may span a <b>cross-model</b> to-one
 * relation.
 *
 * <p>
 * The natural key of most transactional rows in a modular fleet spans a master-data relation the
 * module does not own - one timesheet per (project-month, employee), one slip per (payroll run,
 * employee), one statement per (customer, period) - and {@code Employee} / {@code Customer} are
 * cross-model by design there. The parser used to refuse such a key on the premise that a
 * cross-model target "is stored as a projection, so this entity has no column for it". The premise
 * was false: the consumer stores the target's id in its own {@code <ENTITY>_<RELATION>} FK column,
 * exactly like a same-model to-one; the projection is only the read-side copy behind the dropdown.
 *
 * <p>
 * The assertions walk the layers the key has to reach, with the owner model generated and published
 * as its own project:
 * <ol>
 * <li><b>Generation</b> - the consumer's intent with a key over (ProjectTimesheet, Employee) is
 * accepted, and the key lands in the consumer's schema over the two local FK columns, with the
 * authored message in the generated controller.</li>
 * <li><b>Runtime</b> - the second timesheet for the same person and project-month is answered 409
 * (the database holds the constraint AND the controller recognises which key was hit), while the
 * same person on another project-month is accepted - so the key is composite, not a single-column
 * unique wearing a composite name.</li>
 * </ol>
 */
class IntentCrossModelUniqueIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    /** Owns the master data the key spans. Its project name differs from the model alias on purpose. */
    private static final String OWNER = "unique-owner";
    /** Declares the key over its own to-one AND the cross-model to-one. */
    private static final String CONSUMER = "unique-consumer";

    private static final String OWNER_MODEL = "employees";
    private static final String CONSUMER_MODEL = "timesheets";

    private static final String OWNER_INTENT = """
            name: employees
            description: cross-model unique fixture - the owner of the referenced master data

            entities:
              - name: Employee
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string,  required: true, length: 100 }
            """;

    private static final String CONSUMER_INTENT = """
            name: timesheets
            description: cross-model unique fixture - one timesheet per person per project-month

            uses:
              - { model: employees, project: unique-owner }

            entities:
              - name: ProjectTimesheet
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: month, type: string,  required: true, length: 7 }
              - name: EmployeeTimesheet
                unique:
                  - { fields: [ProjectTimesheet, Employee], message: "This employee already has a timesheet for this project-month" }
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: hours, type: decimal }
                relations:
                  - { name: ProjectTimesheet, kind: manyToOne, to: ProjectTimesheet, required: true }
                  - { name: Employee, kind: manyToOne, to: Employee, model: employees, required: true }
            """;

    @Autowired
    private IRepository repository;
    @Autowired
    private RestAssuredExecutor restAssuredExecutor;
    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_business_key_may_span_a_cross_model_to_one_and_is_enforced_at_runtime() {
        writeIntent(OWNER, OWNER_INTENT);
        generateProject(OWNER);
        writeIntent(CONSUMER, CONSUMER_INTENT);
        generateProject(CONSUMER);

        assertKeyReachesTheSchemaAndTheController();

        publishProject(OWNER);
        publishProject(CONSUMER);
        synchronizationProcessor.forceProcessSynchronizers();

        assertSecondTimesheetForTheSamePersonAndMonthIsRefused();
    }

    /**
     * Layer 1: the key is emitted over the two LOCAL FK columns - the cross-model one has the same
     * {@code <ENTITY>_<RELATION>} form as the same-model one - and the controller carries the authored
     * message it answers the 409 with.
     */
    private void assertKeyReachesTheSchemaAndTheController() {
        String schema = contentOf(CONSUMER, "gen/" + CONSUMER_MODEL + "/schema/" + CONSUMER + ".schema");
        assertTrue(schema.contains("\"EmployeeTimesheet_ProjectTimesheet_Employee\""),
                "the composite key over a cross-model to-one must reach the schema: " + schema);
        assertTrue(schema.contains("EMPLOYEE_TIMESHEET_EMPLOYEE"),
                "the cross-model FK column is a column of THIS entity, so the key can constrain it: " + schema);

        String controller = contentOf(CONSUMER, "gen/" + CONSUMER_MODEL + "/api/employeetimesheet/EmployeeTimesheetController.java");
        assertTrue(controller.contains("This employee already has a timesheet for this project-month"),
                "the generated controller must carry the authored conflict message");
    }

    /**
     * Layer 2 - the promise: the duplicate that was found as an end user (a second timesheet for the
     * same person, billed twice) cannot land, and the caller is told why. The third write keeps the
     * same employee on another project-month and must be accepted.
     */
    private void assertSecondTimesheetForTheSamePersonAndMonthIsRefused() {
        String ownerApi = "/services/java/" + OWNER + "/gen/" + OWNER_MODEL + "/api";
        String consumerApi = "/services/java/" + CONSUMER + "/gen/" + CONSUMER_MODEL + "/api";

        AtomicInteger employeeId = new AtomicInteger();
        restAssuredExecutor.execute(() -> employeeId.set(given().contentType("application/json")
                                                                .body("{\"Name\":\"Ada\"}")
                                                                .when()
                                                                .post(ownerApi + "/employee/EmployeeController")
                                                                .then()
                                                                .statusCode(200)
                                                                .extract()
                                                                .path("Id")),
                60);

        AtomicInteger januaryId = new AtomicInteger();
        AtomicInteger februaryId = new AtomicInteger();
        restAssuredExecutor.execute(() -> januaryId.set(given().contentType("application/json")
                                                               .body("{\"Month\":\"2026-01\"}")
                                                               .when()
                                                               .post(consumerApi + "/projecttimesheet/ProjectTimesheetController")
                                                               .then()
                                                               .statusCode(200)
                                                               .extract()
                                                               .path("Id")));
        restAssuredExecutor.execute(() -> februaryId.set(given().contentType("application/json")
                                                                .body("{\"Month\":\"2026-02\"}")
                                                                .when()
                                                                .post(consumerApi + "/projecttimesheet/ProjectTimesheetController")
                                                                .then()
                                                                .statusCode(200)
                                                                .extract()
                                                                .path("Id")));

        String timesheets = consumerApi + "/employeetimesheet/EmployeeTimesheetController";
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(timesheet(januaryId.get(), employeeId.get()))
                                                 .when()
                                                 .post(timesheets)
                                                 .then()
                                                 .statusCode(200));
        restAssuredExecutor.execute(() -> {
            String body = given().contentType("application/json")
                                 .body(timesheet(januaryId.get(), employeeId.get()))
                                 .when()
                                 .post(timesheets)
                                 .then()
                                 .statusCode(409)
                                 .extract()
                                 .asString();
            assertTrue(body.contains("This employee already has a timesheet for this project-month"),
                    "the 409 must carry the authored message, not a bare constraint name; got: " + body);
        });
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(timesheet(februaryId.get(), employeeId.get()))
                                                 .when()
                                                 .post(timesheets)
                                                 .then()
                                                 .statusCode(200));
    }

    private static String timesheet(int projectTimesheetId, int employeeId) {
        return "{\"ProjectTimesheet\":" + projectTimesheetId + ",\"Employee\":" + employeeId + ",\"Hours\":8}";
    }

    /**
     * Write the intent, generate the model files and drive model-to-code from the generate response's
     * own plan.
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
        for (String project : List.of(CONSUMER, OWNER)) {
            restAssuredExecutor.execute(() -> given().when()
                                                     .delete("/services/ide/publisher/" + WORKSPACE + "/" + project)
                                                     .then()
                                                     .statusCode(greaterThanOrEqualTo(200)));
            if (repository.hasCollection(projectPath(project))) {
                repository.removeCollection(projectPath(project));
            }
        }
    }
}
