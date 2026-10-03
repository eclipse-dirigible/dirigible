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
import static org.hamcrest.Matchers.nullValue;
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
 * dirigible #7547: what a DELETE of a referenced record does to the records still pointing at it -
 * {@code whenTargetDeleted: restrict | nullify | cascade} on a to-one, {@code restrict} when
 * nothing is declared - same-model and cross-model, end to end on two generated, published
 * projects.
 *
 * <p>
 * The issue's reproductions were both a 200 that left a dangling reference: an expense category
 * used by a claim, and an employee referenced by another module's expense claims. The target's
 * repository cannot know who references it, so the REFERENCING repository contributes an
 * {@code org.eclipse.dirigible.sdk.db.TargetDeleteRule} and every delete applies all of them: first
 * every restriction, before anything is written, then - in one transaction with the delete - every
 * nullify and cascade.
 */
class IntentTargetDeleteRulesIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    /** Owns the referenced master data. Its project name differs from the model alias on purpose. */
    private static final String OWNER = "rules-owner";
    /** References the owner's Employee under each rule, and a category of its own. */
    private static final String CONSUMER = "rules-consumer";

    private static final String OWNER_MODEL = "staff";
    private static final String CONSUMER_MODEL = "claims";

    private static final String OWNER_INTENT = """
            name: staff
            description: target-delete fixture - the owner of the referenced master data

            entities:
              - name: Employee
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string,  required: true, length: 100 }
            """;

    private static final String CONSUMER_INTENT = """
            name: claims
            description: target-delete fixture - one relation per rule, the default among them

            uses:
              - { model: staff, project: rules-owner }

            entities:
              - name: ExpenseCategory
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string,  required: true, length: 100 }
              - name: ExpenseClaim
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: Employee, kind: manyToOne, to: Employee, model: staff, required: true }
                  - { name: Reviewer, kind: manyToOne, to: Employee, model: staff, whenTargetDeleted: nullify }
                  - { name: Category, kind: manyToOne, to: ExpenseCategory }
              - name: Timesheet
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: hours, type: decimal }
                relations:
                  - { name: Employee, kind: manyToOne, to: Employee, model: staff, required: true, whenTargetDeleted: cascade }
            """;

    @Autowired
    private IRepository repository;
    @Autowired
    private RestAssuredExecutor restAssuredExecutor;
    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    private String employees;
    private String categories;
    private String claims;
    private String timesheets;

    @Test
    void a_targets_delete_applies_the_rules_of_the_records_referencing_it() {
        writeIntent(OWNER, OWNER_INTENT);
        generateProject(OWNER);
        writeIntent(CONSUMER, CONSUMER_INTENT);
        generateProject(CONSUMER);

        String claimRepository = contentOf(CONSUMER, "gen/" + CONSUMER_MODEL + "/data/expenseclaim/ExpenseClaimRepository.java");
        assertTrue(claimRepository.contains("implements org.eclipse.dirigible.sdk.db.TargetDeleteRule"),
                "the referencing repository must contribute its rules: " + claimRepository);

        publishProject(OWNER);
        publishProject(CONSUMER);
        synchronizationProcessor.forceProcessSynchronizers();

        String ownerApi = "/services/java/" + OWNER + "/gen/" + OWNER_MODEL + "/api";
        String consumerApi = "/services/java/" + CONSUMER + "/gen/" + CONSUMER_MODEL + "/api";
        employees = ownerApi + "/employee/EmployeeController";
        categories = consumerApi + "/expensecategory/ExpenseCategoryController";
        claims = consumerApi + "/expenseclaim/ExpenseClaimController";
        timesheets = consumerApi + "/timesheet/TimesheetController";

        assertRestrictIsTheDefaultSameModelAndCrossModel();
        assertNullifyClearsTheReference();
        assertCascadeDeletesTheReferencingRecord();
        assertARestrictionIsCheckedBeforeAnythingIsReleased();
    }

    /** The issue's two reproductions, with nothing declared: both deletes are now refused. */
    private void assertRestrictIsTheDefaultSameModelAndCrossModel() {
        int ada = create(employees, "{\"Name\":\"Ada\"}");
        int travel = create(categories, "{\"Name\":\"Travel\"}");
        int claim = create(claims, "{\"Employee\":" + ada + ",\"Category\":" + travel + ",\"Amount\":12.5}");

        assertRefused(employees + "/" + ada, "This Employee is referenced by 1 Expense Claim record(s) and cannot be deleted");
        assertRefused(categories + "/" + travel, "This Expense Category is referenced by 1 Expense Claim record(s) and cannot be deleted");

        // With the claim gone, nothing restricts either delete any more.
        assertDeleted(claims + "/" + claim);
        assertDeleted(employees + "/" + ada);
        assertDeleted(categories + "/" + travel);
    }

    /** nullify: the employee goes, the claim stays and no longer names a reviewer. */
    private void assertNullifyClearsTheReference() {
        int owner = create(employees, "{\"Name\":\"Owen\"}");
        int bob = create(employees, "{\"Name\":\"Bob\"}");
        int claim = create(claims, "{\"Employee\":" + owner + ",\"Reviewer\":" + bob + ",\"Amount\":7}");

        assertDeleted(employees + "/" + bob);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(claims + "/" + claim)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Reviewer", nullValue())
                                                 .body("Employee", equalTo(owner)));
    }

    /** cascade: the employee and the timesheet referencing it go together. */
    private void assertCascadeDeletesTheReferencingRecord() {
        int cy = create(employees, "{\"Name\":\"Cy\"}");
        int timesheet = create(timesheets, "{\"Employee\":" + cy + ",\"Hours\":8}");

        assertDeleted(employees + "/" + cy);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(timesheets + "/" + timesheet)
                                                 .then()
                                                 .statusCode(404));
    }

    /**
     * One employee both raising a claim (restrict) and reviewing it (nullify): the refusal comes first,
     * so the reviewer is NOT cleared by a delete that is then refused.
     */
    private void assertARestrictionIsCheckedBeforeAnythingIsReleased() {
        int dee = create(employees, "{\"Name\":\"Dee\"}");
        int claim = create(claims, "{\"Employee\":" + dee + ",\"Reviewer\":" + dee + ",\"Amount\":3}");

        assertRefused(employees + "/" + dee, "This Employee is referenced by 1 Expense Claim record(s) and cannot be deleted");
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(claims + "/" + claim)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Reviewer", equalTo(dee)));
    }

    private void assertRefused(String record, String message) {
        restAssuredExecutor.execute(() -> {
            String body = given().when()
                                 .delete(record)
                                 .then()
                                 .statusCode(409)
                                 .extract()
                                 .asString();
            assertTrue(body.contains(message), "the 409 must name the referencing records and their count; got: " + body);
        });
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(record)
                                                 .then()
                                                 .statusCode(200));
    }

    private void assertDeleted(String record) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete(record)
                                                 .then()
                                                 .statusCode(both(greaterThanOrEqualTo(200)).and(lessThan(300))));
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
                                                     .statusCode(both(greaterThanOrEqualTo(200)).and(lessThan(300))));
            if (repository.hasCollection(projectPath(project))) {
                repository.removeCollection(projectPath(project));
            }
        }
    }
}
