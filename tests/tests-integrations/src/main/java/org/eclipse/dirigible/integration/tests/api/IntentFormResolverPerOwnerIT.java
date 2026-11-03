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
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
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
 * Two task forms of two processes, on two different entities, showing the same one-hop field
 * ({@code Customer.creditLimit}) each read their OWN record - dirigible #8331.
 *
 * <p>
 * The resolver behind such a field loads the trigger entity's record by the process's id variable
 * and reads the field off its related row. It was named after the path alone, so both forms'
 * resolvers were one {@code ResolveCustomerCreditLimit.java}: the one generated last won, and the
 * other process looked its invoice id up as a debit note - its form showed another customer's
 * limit, or nothing. Nothing failed at generation or at compile.
 *
 * <p>
 * The fixture makes the wrong lookup visible: the invoice and the debit note both get id 1, and
 * each belongs to a different customer with a different limit, so a resolver loading the wrong
 * entity publishes the other customer's limit. The assertion is on the task's process variables -
 * what the form actually binds - after the generated code compiled and ran.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentFormResolverPerOwnerIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "resolverowner";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String EVENTS = "gen/events/" + PROJECT + "/";
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String TASKS = "/services/inbox/tasks";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String INTENT_YAML =
            """
                    name: resolverowner
                    description: two forms on two entities show the same one-hop field

                    entities:
                      - name: Customer
                        fields:
                          - { name: id,          type: integer, primaryKey: true, generated: true }
                          - { name: name,        type: string, required: true, length: 100 }
                          - { name: creditLimit, type: decimal }
                      - name: SalesInvoice
                        fields:
                          - { name: id,     type: integer, primaryKey: true, generated: true }
                          - { name: number, type: string, length: 32 }
                        relations:
                          - { name: Customer, kind: manyToOne, to: Customer, required: true }
                      - name: DebitNote
                        fields:
                          - { name: id,     type: integer, primaryKey: true, generated: true }
                          - { name: number, type: string, length: 32 }
                        relations:
                          - { name: Customer, kind: manyToOne, to: Customer, required: true }

                    processes:
                      - name: SalesInvoiceApproval
                        trigger: { onCreate: SalesInvoice }
                        steps:
                          - { name: creditHold, kind: userTask, args: { assignee: clerk, form: CreditHoldSalesInvoice } }
                          - { name: done,       kind: end }
                      - name: DebitNoteApproval
                        trigger: { onCreate: DebitNote }
                        steps:
                          - { name: creditHold, kind: userTask, args: { assignee: clerk, form: CreditHoldDebitNote } }
                          - { name: done,       kind: end }

                    forms:
                      - { name: CreditHoldSalesInvoice, forEntity: SalesInvoice, fields: [number, Customer, Customer.creditLimit], actions: [proceed] }
                      - { name: CreditHoldDebitNote,    forEntity: DebitNote,    fields: [number, Customer, Customer.creditLimit], actions: [proceed] }
                    """;

    /**
     * The class the path-only naming wrote for both forms; a project generated before the fix has it.
     */
    private static final String PATH_ONLY_RESOLVER = EVENTS + "ResolveCustomerCreditLimit.java";

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void each_form_reads_the_customer_of_its_own_record() {
        // A project generated before the fix carries the path-only class; regenerating must not leave
        // it behind, or it would still be compiled beside the two that replace it.
        repository.createResource(PROJECT_PATH + "/" + PATH_ONLY_RESOLVER,
                ("package gen.events." + PROJECT + ";\n\npublic class ResolveCustomerCreditLimit {\n}\n").getBytes(StandardCharsets.UTF_8));
        generateProject();

        String invoiceResolver = contentOf(EVENTS + "ResolveSalesInvoiceCustomerCreditLimit.java");
        assertTrue(invoiceResolver.contains("SalesInvoiceRepository"), "the invoice form's resolver loads the invoice: " + invoiceResolver);
        String debitNoteResolver = contentOf(EVENTS + "ResolveDebitNoteCustomerCreditLimit.java");
        assertTrue(debitNoteResolver.contains("DebitNoteRepository"),
                "the debit note form's resolver loads the debit note: " + debitNoteResolver);
        assertFalse(repository.getResource(PROJECT_PATH + "/" + PATH_ONLY_RESOLVER)
                              .exists(),
                "the path-only resolver of an earlier generation must be gone");

        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int lowLimit = create("customer/CustomerController", "{\"Name\":\"Low\",\"CreditLimit\":1000}");
        int highLimit = create("customer/CustomerController", "{\"Name\":\"High\",\"CreditLimit\":2000}");
        int invoice = create("salesinvoice/SalesInvoiceController", "{\"Number\":\"SI-1\",\"Customer\":" + lowLimit + "}");
        int debitNote = create("debitnote/DebitNoteController", "{\"Number\":\"DN-1\",\"Customer\":" + highLimit + "}");
        // The point of the fixture: the same id in both tables, owned by different customers.
        assertEquals(invoice, debitNote, "the fixture relies on the invoice and the debit note sharing an id");

        assertEquals(0, new BigDecimal("1000").compareTo(creditLimitOnTask("SalesInvoiceApproval", invoice)),
                "the invoice's credit hold must show its own customer's limit");
        assertEquals(0, new BigDecimal("2000").compareTo(creditLimitOnTask("DebitNoteApproval", debitNote)),
                "the debit note's credit hold must show its own customer's limit");
    }

    /** The {@code Customer_creditLimit} variable the form binds, on the task of the given record. */
    private BigDecimal creditLimitOnTask(String process, int record) {
        String task = taskFor(process, record);
        AtomicReference<Object> value = new AtomicReference<>();
        restAssuredExecutor.execute(() -> value.set(given().when()
                                                           .get(TASKS + "/" + task + "/variables")
                                                           .then()
                                                           .statusCode(200)
                                                           .body("variables.Customer_creditLimit", notNullValue())
                                                           .extract()
                                                           .path("variables.Customer_creditLimit")),
                TIMEOUT_SECONDS);
        return new BigDecimal(value.get()
                                   .toString());
    }

    /**
     * The user task of the given process for the given record, found by the business key the trigger
     * stamped.
     */
    private String taskFor(String process, int record) {
        AtomicReference<String> task = new AtomicReference<>();
        String query =
                "find { it.processDefinitionId.startsWith('" + process + ":') && it.processInstanceBusinessKey == '" + record + "' }.id";
        restAssuredExecutor.execute(() -> task.set(given().when()
                                                          .get(TASKS + "?type=groups")
                                                          .then()
                                                          .statusCode(200)
                                                          .body(query, notNullValue())
                                                          .extract()
                                                          .path(query)),
                TIMEOUT_SECONDS);
        return task.get();
    }

    private int create(String controller, String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(API + "/" + controller)
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

    private String contentOf(String fileName) {
        return new String(repository.getResource(PROJECT_PATH + "/" + fileName)
                                    .getContent(),
                StandardCharsets.UTF_8);
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
