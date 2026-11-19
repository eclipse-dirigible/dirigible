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
import static org.hamcrest.Matchers.containsInAnyOrder;
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
 * dirigible #7914: the integrity-check conditions of a report - {@code having:} over the groups and
 * {@code notExists:} across a relation, including one whose target another model owns - end to end:
 * two generated, published projects, rows created through their own controllers, and the generated
 * report controllers answering only the rows that break the invariant.
 */
class IntentReportIntegrityChecksIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    /** Owns the journal entries. */
    private static final String OWNER = "integrity-journal";
    /** Owns the documents and the integrity reports over them. */
    private static final String CONSUMER = "integrity-books";

    private static final String OWNER_MODEL = "journal";
    private static final String CONSUMER_MODEL = "books";

    private static final String OWNER_INTENT = """
            name: journal
            description: integrity-check fixture - the journal the documents are booked into

            entities:
              - name: JournalEntry
                fields:
                  - { name: id,             type: integer, primaryKey: true, generated: true }
                  - { name: documentNumber, type: string,  length: 50 }
                  - { name: reversed,       type: boolean }
            """;

    private static final String CONSUMER_INTENT = """
            name: books
            description: integrity-check fixture - the reports an accountant runs before a period close

            uses:
              - { model: journal, project: integrity-journal }

            entities:
              - name: Supplier
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }
              - name: Company
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }
              - name: PurchaseInvoice
                fields:
                  - { name: id,             type: integer, primaryKey: true, generated: true }
                  - { name: number,         type: string, length: 50 }
                  - { name: date,           type: date }
                  - { name: supplierNumber, type: string, length: 50 }
                relations:
                  - { name: Supplier, kind: manyToOne, to: Supplier, required: true }
              - name: FiscalYear
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: startDate, type: date }
                  - { name: endDate,   type: date }
                relations:
                  - { name: Company, kind: manyToOne, to: Company, required: true }
              - name: Posting
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: number,   type: string, length: 50 }
                  - { name: postedOn, type: date }
                relations:
                  - { name: Company, kind: manyToOne, to: Company }

            reports:
              - name: DuplicateSupplierNumbers
                source: PurchaseInvoice
                dimensions: [Supplier, "year(date)", supplierNumber]
                measures: ["count(*)"]
                having: "count(*) > 1"
              - name: InvoicesWithoutEntry
                source: PurchaseInvoice
                dimensions: [number]
                notExists: { entity: JournalEntry, model: journal, match: { documentNumber: number }, filter: "reversed = false" }
              - name: PostingsOutsideTheirYear
                source: Posting
                dimensions: [number]
                notExists:
                  entity: FiscalYear
                  match: { Company: Company }
                  between: { start: startDate, end: endDate, value: postedOn }
            """;

    @Autowired
    private IRepository repository;
    @Autowired
    private RestAssuredExecutor restAssuredExecutor;
    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void the_integrity_reports_list_only_the_rows_that_break_their_invariant() {
        writeResource(OWNER, "app.intent", OWNER_INTENT);
        generateProject(OWNER);
        writeResource(CONSUMER, "app.intent", CONSUMER_INTENT);
        generateProject(CONSUMER);

        publishProject(OWNER);
        publishProject(CONSUMER);
        synchronizationProcessor.forceProcessSynchronizers();

        String ownerApi = "/services/java/" + OWNER + "/gen/" + OWNER_MODEL + "/api";
        String consumerApi = "/services/java/" + CONSUMER + "/gen/" + CONSUMER_MODEL + "/api";
        String entries = ownerApi + "/journalentry/JournalEntryController";
        String suppliers = consumerApi + "/supplier/SupplierController";
        String companies = consumerApi + "/company/CompanyController";
        String invoices = consumerApi + "/purchaseinvoice/PurchaseInvoiceController";
        String years = consumerApi + "/fiscalyear/FiscalYearController";
        String postings = consumerApi + "/posting/PostingController";

        int acme = create(suppliers, "{\"Name\":\"Acme\"}");
        int beta = create(suppliers, "{\"Name\":\"Beta\"}");
        // Acme's A-1 twice in 2026 is the duplicate; its 2025 A-1 and Beta's A-1 are not.
        create(invoices, invoice("PI-1", "2026-02-01", "A-1", acme));
        create(invoices, invoice("PI-2", "2026-03-01", "A-1", acme));
        create(invoices, invoice("PI-3", "2025-03-01", "A-1", acme));
        create(invoices, invoice("PI-4", "2026-02-01", "A-1", beta));

        // PI-1 is booked; PI-2's only entry is reversed, so it is not.
        create(entries, "{\"DocumentNumber\":\"PI-1\",\"Reversed\":false}");
        create(entries, "{\"DocumentNumber\":\"PI-2\",\"Reversed\":true}");

        int north = create(companies, "{\"Name\":\"North\"}");
        int south = create(companies, "{\"Name\":\"South\"}");
        create(years, "{\"Company\":" + north + ",\"StartDate\":\"2026-01-01\",\"EndDate\":\"2026-12-31\"}");
        // South's current year is still open-ended.
        create(years, "{\"Company\":" + south + ",\"StartDate\":\"2026-01-01\"}");
        create(postings, "{\"Number\":\"E-1\",\"PostedOn\":\"2026-12-31\",\"Company\":" + north + "}");
        create(postings, "{\"Number\":\"E-2\",\"PostedOn\":\"2025-12-31\",\"Company\":" + north + "}");
        create(postings, "{\"Number\":\"E-3\",\"PostedOn\":\"2027-06-01\",\"Company\":" + south + "}");
        create(postings, "{\"Number\":\"E-4\",\"PostedOn\":\"2026-05-01\"}");

        String reports = "/services/java/" + CONSUMER + "/gen/";
        // having: only the group counted twice.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(reports + "duplicatesuppliernumbers/api/reports/DuplicateSupplierNumbersController")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("size()", equalTo(1))
                                                 .body("[0].Supplier", equalTo("Acme"))
                                                 .body("[0].'Supplier Number'", equalTo("A-1"))
                                                 .body("[0].Count", equalTo(2)),
                60);
        // notExists across models, with the owner's filter on its own property.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(reports + "invoiceswithoutentry/api/reports/InvoicesWithoutEntryController")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Number", containsInAnyOrder("PI-2", "PI-3", "PI-4")),
                60);
        // notExists with a validity window: E-2 falls in no year of North, E-4 has no company at all.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(reports + "postingsoutsidetheiryear/api/reports/PostingsOutsideTheirYearController")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Number", containsInAnyOrder("E-2", "E-4")),
                60);
    }

    private static String invoice(String number, String date, String supplierNumber, int supplier) {
        return "{\"Number\":\"" + number + "\",\"Date\":\"" + date + "\",\"SupplierNumber\":\"" + supplierNumber + "\",\"Supplier\":"
                + supplier + "}";
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
