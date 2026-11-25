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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
 * A create-from's {@code items: backLink:} marks every source row it consumed with the document it
 * became a line of, in the run's own unit of work, and a marked row is never consumed again (issue
 * #7956) - pass-through billing, each billable expense line billed exactly once.
 *
 * <p>
 * The source rows are not the source's own composition: the expense lines belong to their claims
 * and point at the project they are billed to, and the run invoices one project. Deleting the
 * invoice frees its lines through the relation's own {@code whenTargetDeleted: nullify}.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentGeneratesItemsConsumedIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "genconsumed";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String GENERATE_RUN =
            "/services/java/" + PROJECT + "/gen/events/" + PROJECT + "/InvoiceBillableExpensesGenerate/run";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String INTENT_YAML = """
            name: genconsumed
            description: create-from whose items mark the source rows they consumed

            entities:
              - name: Project
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 100 }

              - name: ExpenseClaim
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, length: 100 }

              - name: ExpenseClaimItem
                fields:
                  - { name: id,          type: integer, primaryKey: true, generated: true }
                  - { name: description, type: string, length: 200 }
                  - { name: amount,      type: decimal, precision: 18, scale: 2, required: true }
                  - { name: billable,    type: boolean, defaultValue: false }
                relations:
                  - { name: ExpenseClaim,  kind: manyToOne, to: ExpenseClaim, composition: true, required: true }
                  - { name: Project,       kind: manyToOne, to: Project }
                  - { name: BilledInvoice, kind: manyToOne, to: Invoice, whenTargetDeleted: nullify }

              - name: Invoice
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, length: 100 }
                relations:
                  - { name: Project, kind: manyToOne, to: Project }

              - name: InvoiceItem
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: name,   type: string, length: 200 }
                  - { name: amount, type: decimal, precision: 18, scale: 2, required: true }
                relations:
                  - { name: Invoice, kind: manyToOne, to: Invoice, composition: true, required: true }

            generates:
              - name: invoice-billable-expenses
                from: Project
                to: Invoice
                forEntity: Project
                map:
                  Project: id
                items:
                  from: ExpenseClaimItem
                  to: InvoiceItem
                  where:
                    - { field: billable, op: eq, value: true }
                  backLink: BilledInvoice
                  map:
                    name: description
                    amount: amount
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void each_billable_line_is_invoiced_exactly_once_and_a_deleted_invoice_frees_its_lines() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int project = create("/project/ProjectController", "{\"Name\":\"Alpha\"}");
        int other = create("/project/ProjectController", "{\"Name\":\"Beta\"}");
        int claim = create("/expenseclaim/ExpenseClaimController", "{\"Number\":\"EC-1\"}");
        int taxi = line(claim, project, "taxi", 10, true);
        int hotel = line(claim, project, "hotel", 20, true);
        int lunch = line(claim, project, "lunch", 5, false);
        int foreign = line(claim, other, "train", 7, true);

        // The first run bills the project's two billable lines and marks exactly those two.
        int first = run(project);
        assertLines(first, List.of("taxi", "hotel"));
        assertBilled(taxi, first);
        assertBilled(hotel, first);
        assertBilled(lunch, null);
        assertBilled(foreign, null);

        // A second run finds nothing left to bill: refused, and no second invoice.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + project + "}")
                                                 .when()
                                                 .post(GENERATE_RUN)
                                                 .then()
                                                 .statusCode(400)
                                                 .body("messageKey",
                                                         equalTo(PROJECT + ":" + PROJECT + "-model.checks.generateNoItemQualifies")),
                TIMEOUT_SECONDS);
        assertInvoices(project, 1);

        // A line added later is billed on its own; the marked ones stay where they were billed.
        int parking = line(claim, project, "parking", 4, true);
        int second = run(project);
        assertLines(second, List.of("parking"));
        assertBilled(parking, second);
        assertBilled(taxi, first);

        // Deleting the first invoice clears its marks (whenTargetDeleted: nullify), so its lines are
        // billable again - and only they.
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete(API + "/invoice/InvoiceController/" + first)
                                                 .then()
                                                 .statusCode(both(greaterThanOrEqualTo(200)).and(lessThan(300))));
        assertBilled(taxi, null);
        assertBilled(hotel, null);
        int third = run(project);
        assertLines(third, List.of("taxi", "hotel"));
        assertBilled(taxi, third);
        assertBilled(parking, second);
    }

    private int line(int claim, int project, String description, int amount, boolean billable) {
        return create("/expenseclaim/ExpenseClaimItemController", "{\"ExpenseClaim\":" + claim + ",\"Project\":" + project
                + ",\"Description\":\"" + description + "\",\"Amount\":" + amount + ",\"Billable\":" + billable + "}");
    }

    private int run(int project) {
        AtomicInteger invoice = new AtomicInteger();
        restAssuredExecutor.execute(() -> invoice.set(JsonPath.from(given().contentType("application/json")
                                                                           .body("{\"id\":" + project + "}")
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

    private void assertLines(int invoice, List<String> names) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/invoice/InvoiceItemController?Invoice=" + invoice)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("", hasSize(names.size()))
                                                 .body("Name", equalTo(names)));
    }

    private void assertBilled(int line, Integer invoice) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/expenseclaim/ExpenseClaimItemController/" + line)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("BilledInvoice", invoice == null ? nullValue() : equalTo(invoice)));
    }

    private void assertInvoices(int project, int count) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(API + "/invoice/InvoiceController?Project=" + project)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("", hasSize(count)));
    }

    private int create(String controller, String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(API + controller)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")),
                TIMEOUT_SECONDS);
        return id.get();
    }

    private void generateProject() {
        writeIntent();
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

    private void writeIntent() {
        String path = PROJECT_PATH + "/app.intent";
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, INTENT_YAML.getBytes(StandardCharsets.UTF_8));
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
