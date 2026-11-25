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
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.restassured.path.json.JsonPath;

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
 * dirigible #7954: {@code sourceStatusOnRetire} for a create-from whose target ANOTHER model owns.
 *
 * <p>
 * A milestone of the projects module is billed into the billing module's invoice; the billing
 * module cannot know about milestones (it would need {@code uses:} back - the cycle), so the reopen
 * is generated in the projects module, bound to the invoice's {@code -transitioned} topic, and
 * decides "retired" by the {@code stage:} classification the billing module seeds and publishes on
 * its {@code .model}. Each module is generated and published as its own project, under a project
 * name that differs from its model alias, so the listener's topic must name the OWNER project.
 *
 * <p>
 * The flip rides the retirement's committed {@code -transitioned} notice: a void the invoice
 * refuses rolls back and publishes nothing, so the milestone stays invoiced; a void that commits
 * returns it to REACHED, and a second Generate bills it again.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentCrossModelReopenIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String OWNER = "reopen-billing";
    private static final String CONSUMER = "reopen-projects";
    private static final String OWNER_MODEL = "billing";
    private static final String CONSUMER_MODEL = "projects";
    private static final String OWNER_API = "/services/java/" + OWNER + "/gen/" + OWNER_MODEL + "/api";
    private static final String CONSUMER_API = "/services/java/" + CONSUMER + "/gen/" + CONSUMER_MODEL + "/api";
    private static final String GENERATE_RUN =
            "/services/java/" + CONSUMER + "/gen/events/" + CONSUMER_MODEL + "/InvoiceFromMilestoneGenerate/run";
    private static final String VOID_INVOICE = "/services/java/" + OWNER + "/gen/events/" + OWNER_MODEL + "/VoidInvoiceTransition/run";
    private static final String PAID_REFUSAL = "A paid invoice cannot be voided";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String OWNER_INTENT = """
            name: billing
            description: cross-model reopen fixture - the invoice another module bills into

            entities:
              - name: InvoiceStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: Invoice
                checks:
                  - { kind: compare, field: paid, op: le, value: 0, status: 3, message: "A paid invoice cannot be voided" }
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: reference, type: string, length: 100 }
                  # The back-reference the reopen reads: the milestone's id, a plain integer - this
                  # module never names the projects module.
                  - { name: milestone, type: integer }
                  - { name: paid,      type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }

            seeds:
              - name: invoice-statuses
                entity: InvoiceStatus
                rows:
                  - { id: 1, name: Issued,    stage: live }
                  - { id: 2, name: Cancelled, stage: cancelled }
                  - { id: 3, name: Voided,    stage: void }

            transitions:
              - { name: VoidInvoice, forEntity: Invoice, from: [1], setStatus: 3, label: Void }
            """;

    private static final String CONSUMER_INTENT = """
            name: projects
            description: cross-model reopen fixture - a milestone billed into the billing module's invoice

            uses:
              - { model: billing, project: reopen-billing }

            entities:
              - name: MilestoneStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: Milestone
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 100 }
                relations:
                  - { name: Status, kind: manyToOne, to: MilestoneStatus, function: EntityStatus, init: 1 }

            seeds:
              - name: milestone-statuses
                entity: MilestoneStatus
                rows:
                  - { id: 1, name: PLANNED }
                  - { id: 2, name: REACHED }
                  - { id: 3, name: INVOICED }

            generates:
              - name: invoice-from-milestone
                from: Milestone
                to: Invoice
                uses: billing
                forEntity: Milestone
                map:
                  reference: name
                  milestone: id
                fromStatus: [REACHED]
                sourceStatus: INVOICED
                sourceStatusOnRetire: REACHED

            transitions:
              - { name: MarkReached, forEntity: Milestone, from: [PLANNED], setStatus: REACHED, label: Mark Reached }
            """;

    @Autowired
    private IRepository repository;
    @Autowired
    private RestAssuredExecutor restAssuredExecutor;
    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void voiding_the_other_modules_invoice_returns_the_milestone_and_a_second_generate_bills_it_again() {
        writeIntent(OWNER, OWNER_INTENT);
        generateProject(OWNER);
        writeIntent(CONSUMER, CONSUMER_INTENT);
        generateProject(CONSUMER);

        String reopen = contentOf(CONSUMER, "gen/events/" + CONSUMER_MODEL + "/InvoiceFromMilestoneGenerateReopen.java");
        assertTrue(reopen.contains("\"" + OWNER + "-Invoice-Invoice-transitioned\""),
                "the reopen must bind the invoice's topic in the OWNER project: " + reopen);

        publishProject(OWNER);
        publishProject(CONSUMER);
        synchronizationProcessor.forceProcessSynchronizers();

        int milestone = create(CONSUMER_API + "/milestone/MilestoneController", "{\"Name\":\"Design signed off\"}");
        run("/services/java/" + CONSUMER + "/gen/events/" + CONSUMER_MODEL + "/MarkReachedTransition/run", milestone, 200);
        int first = generate(milestone);
        assertMilestone(milestone, 3);

        // A void the invoice refuses rolls back - it publishes no -transitioned, so the milestone stays
        // invoiced and the Generate stays refused.
        update(first, milestone, "10");
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + first + "}")
                                                 .when()
                                                 .post(VOID_INVOICE)
                                                 .then()
                                                 .statusCode(400)
                                                 .body(containsString(PAID_REFUSAL)));
        assertInvoice(first, 1);
        assertMilestone(milestone, 3);
        run(GENERATE_RUN, milestone, 409);

        // The void that commits returns the milestone to REACHED in the OTHER module...
        update(first, milestone, "0");
        run(VOID_INVOICE, first, 200);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(CONSUMER_API + "/milestone/MilestoneController/" + milestone)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(2)),
                TIMEOUT_SECONDS);

        // ...so it is billed again, into a NEW invoice; the voided one is kept.
        int second = generate(milestone);
        assertNotEquals(first, second);
        assertMilestone(milestone, 3);
        assertInvoice(first, 3);
    }

    private int generate(int milestone) {
        AtomicInteger invoice = new AtomicInteger();
        restAssuredExecutor.execute(() -> invoice.set(JsonPath.from(given().contentType("application/json")
                                                                           .body("{\"id\":" + milestone + "}")
                                                                           .when()
                                                                           .post(GENERATE_RUN)
                                                                           .then()
                                                                           .statusCode(200)
                                                                           .extract()
                                                                           .asString())
                                                              .getInt("Id")),
                TIMEOUT_SECONDS);
        return invoice.get();
    }

    /** A full update, so it carries the back-reference the reopen reads the milestone through. */
    private void update(int invoice, int milestone, String paid) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + invoice + ",\"Reference\":\"Design signed off\",\"Milestone\":"
                                                         + milestone + ",\"Paid\":" + paid + ",\"Status\":1}")
                                                 .when()
                                                 .put(OWNER_API + "/invoice/InvoiceController/" + invoice)
                                                 .then()
                                                 .statusCode(200));
    }

    private void run(String endpoint, int id, int status) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + id + "}")
                                                 .when()
                                                 .post(endpoint)
                                                 .then()
                                                 .statusCode(status));
    }

    private void assertMilestone(int milestone, int status) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(CONSUMER_API + "/milestone/MilestoneController/" + milestone)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(status)));
    }

    private void assertInvoice(int invoice, int status) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(OWNER_API + "/invoice/InvoiceController/" + invoice)
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
                TIMEOUT_SECONDS);
        return id.get();
    }

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
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
