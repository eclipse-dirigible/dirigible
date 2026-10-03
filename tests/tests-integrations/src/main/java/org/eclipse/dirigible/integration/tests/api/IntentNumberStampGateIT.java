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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.matchesPattern;
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
import org.springframework.test.annotation.DirtiesContext;

/**
 * A {@code stampOn: issue} number is not spent on an Issue that is then refused (#7577).
 *
 * <p>
 * The process the issue reproduced on: the Issue task, the number stamp, then the status set to
 * ISSUED - in that order on purpose, so the posting that consumes {@code -transitioned} sees the
 * real number. A check gated on ISSUED (the issuing company's registration number) refused the
 * status set AFTER the stamp had allocated and written the number, so the document stayed APPROVED
 * holding {@code ...0001}, the next document issued took {@code ...0002}, and once fixed the
 * refused one issued out of order - a gap and a reordering in a series the law requires to be
 * chronological and gap-free. The stamp now asks the repository whether the move will be accepted
 * before it allocates.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentNumberStampGateIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "stampgate";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String INVOICES = API + "/invoice/InvoiceController";
    private static final String COMPANIES = API + "/company/CompanyController";
    private static final String TASKS = "/services/inbox/tasks";
    private static final String REFUSAL = "The issuing company's registration number is required before this invoice can be issued";
    private static final String UUID = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    private static final long TIMEOUT_SECONDS = 90;
    /** The task appears once the create event has started the instance. */
    private static final long PROCESS_TIMEOUT_SECONDS = 60;

    private static final String NUMBERS_JSON = """
            {"series": [
              {"name": "Gate Invoice", "prefix": "GI", "size": 8}
            ]}
            """;

    private static final String INTENT_YAML = """
            name: stampgate
            description: number-stamp fixture - a refused Issue keeps its placeholder, not a number

            entities:
              - name: InvoiceStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: Company
                fields:
                  - { name: id,                 type: integer, primaryKey: true, generated: true }
                  - { name: name,               type: string, length: 100 }
                  - { name: registrationNumber, type: string, length: 50 }

              - name: Invoice
                checks:
                  - { kind: requiredWhen, field: Company.registrationNumber, when: "Status == 3", status: 3,
                      message: "The issuing company's registration number is required before this invoice can be issued" }
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: note,   type: string, length: 200 }
                  - { name: number, type: string, length: 100, number: { series: Gate Invoice, stampOn: issue } }
                relations:
                  - { name: Status,  kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 1 }
                  - { name: Company, kind: manyToOne, to: Company }

            processes:
              # The stamp BEFORE the status write, as in the field: the posting must see the real number.
              - name: InvoiceIssue
                trigger: { onCreate: Invoice }
                steps:
                  - { name: issue,          kind: userTask,    args: { assignee: issuer, form: IssueInvoice, next: generateNumber } }
                  - { name: generateNumber, kind: serviceTask, args: { delegate: gen.events.InvoiceNumberStamp, next: markIssued } }
                  - { name: markIssued,     kind: serviceTask, args: { setRelationField: Status, value: 3, next: done } }
                  - { name: done,           kind: end }

            forms:
              - { name: IssueInvoice, forEntity: Invoice, fields: [note], editable: [note], actions: [issue] }

            permissions:
              - { role: Issuer, description: Issuer, can: [Invoice:read] }

            seeds:
              - name: invoice-statuses
                entity: InvoiceStatus
                rows:
                  - { id: 1, name: Draft }
                  - { id: 3, name: Issued }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_refused_issue_keeps_its_placeholder_and_the_series_stays_gap_free() {
        writeProjectFile(PROJECT + ".numbers", NUMBERS_JSON);
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int company = create(COMPANIES, "{\"Name\":\"Acme\"}");
        int invoice = create(INVOICES, "{\"Note\":\"first\",\"Company\":" + company + "}");
        String task = taskFor(invoice);

        // The refusal reaches the person who pressed Issue - and the number is NOT spent: the stamp asked
        // before it allocated, so the document still carries its create-time placeholder.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"action\":\"COMPLETE\",\"data\":{\"action\":\"issue\"}}")
                                                 .when()
                                                 .post(TASKS + "/" + task)
                                                 .then()
                                                 .statusCode(400)
                                                 .body(containsString(REFUSAL)));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(INVOICES + "/" + invoice)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(1))
                                                 .body("Number", matchesPattern(UUID)));
        assertEquals(task, taskFor(invoice), "the refused completion rolled back, so the task is still the issuer's");

        // Once the company carries what the check asks, the same Issue takes the FIRST value of the
        // series - nothing was spent by the refused attempt.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + company + ",\"Name\":\"Acme\",\"RegistrationNumber\":\"BG123456789\"}")
                                                 .when()
                                                 .put(COMPANIES + "/" + company)
                                                 .then()
                                                 .statusCode(200));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"action\":\"COMPLETE\",\"data\":{\"action\":\"issue\"}}")
                                                 .when()
                                                 .post(TASKS + "/" + task)
                                                 .then()
                                                 .statusCode(200));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(INVOICES + "/" + invoice)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(3))
                                                 .body("Number", matchesPattern("GI0*1")),
                PROCESS_TIMEOUT_SECONDS);
    }

    /** The issue task of one invoice's instance, found by the business key the trigger stamped. */
    private String taskFor(int invoice) {
        AtomicReference<String> task = new AtomicReference<>();
        restAssuredExecutor.execute(() -> task.set(given().when()
                                                          .get(TASKS + "?type=groups")
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .path("find { it.processInstanceBusinessKey == '" + invoice + "' }.id")),
                PROCESS_TIMEOUT_SECONDS);
        return task.get();
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

    private void generateProject() {
        writeProjectFile("app.intent", INTENT_YAML);
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

    private void writeProjectFile(String relativePath, String content) {
        String path = PROJECT_PATH + "/" + relativePath;
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
