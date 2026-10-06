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
import org.springframework.test.annotation.DirtiesContext;

/**
 * A {@code forbidWhen} scoped by {@code verbs:} refuses only the writes it names (dirigible #7710).
 *
 * <p>
 * Since #7372 every forbidWhen also refused the DELETE of the row it guards. That is right for "no
 * line may change on a sent quotation" and wrong for a rule about ADDING only: "no further
 * allocation onto a PAID invoice" also made a wrong allocation impossible to remove once the
 * invoice was PAID. {@code verbs: [create, update]} keeps the refusal of the allocation and lets it
 * be deleted; {@code verbs: [delete]} is the other half - a row anyone may write and nobody may
 * remove while the condition holds.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentForbidWhenVerbsIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "verbguard";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String INVOICES = API + "/invoice/InvoiceController";
    private static final String ALLOCATIONS = API + "/allocation/AllocationController";
    private static final String NOTES = API + "/note/NoteController";
    private static final String NO_ALLOCATION = "Cannot add a payment to a fully paid invoice";
    private static final String NO_REMOVAL = "A locked note cannot be removed";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String INTENT_YAML = """
            name: verbguard
            description: a forbidWhen scoped to the verbs it is about

            entities:
              - name: Invoice
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: label, type: string, length: 100 }
                  - { name: paid,  type: boolean }

              - name: Allocation
                checks:
                  - { kind: forbidWhen, when: "Invoice.paid == true", verbs: [create, update], message: "%s" }
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal }
                relations:
                  - { name: Invoice, kind: manyToOne, to: Invoice }

              - name: Note
                checks:
                  - { kind: forbidWhen, when: "locked == true", verbs: [delete], message: "%s" }
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: text,   type: string, length: 100 }
                  - { name: locked, type: boolean }
            """.formatted(NO_ALLOCATION, NO_REMOVAL);

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void each_scope_refuses_only_the_verbs_it_names() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int invoice = create(INVOICES, "{\"Label\":\"INV-1\",\"Paid\":false}");
        int wrong = create(ALLOCATIONS, "{\"Amount\":100,\"Invoice\":" + invoice + "}");
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + invoice + ",\"Label\":\"INV-1\",\"Paid\":true}")
                                                 .when()
                                                 .put(INVOICES + "/" + invoice)
                                                 .then()
                                                 .statusCode(200));

        // verbs: [create, update] - no further allocation onto the PAID invoice, nor an edit of one...
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Amount\":5,\"Invoice\":" + invoice + "}")
                                                 .when()
                                                 .post(ALLOCATIONS)
                                                 .then()
                                                 .statusCode(400)
                                                 .body(containsString(NO_ALLOCATION)));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + wrong + ",\"Amount\":7,\"Invoice\":" + invoice + "}")
                                                 .when()
                                                 .put(ALLOCATIONS + "/" + wrong)
                                                 .then()
                                                 .statusCode(400)
                                                 .body(containsString(NO_ALLOCATION)));
        // ...but the wrong allocation can still be REMOVED - before #7710 this was the 400 with no way out.
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete(ALLOCATIONS + "/" + wrong)
                                                 .then()
                                                 .statusCode(greaterThanOrEqualTo(200))
                                                 .statusCode(lessThan(300)));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ALLOCATIONS + "/" + wrong)
                                                 .then()
                                                 .statusCode(404));

        // verbs: [delete] - a locked note is written freely and refused only its removal.
        int note = create(NOTES, "{\"Text\":\"keep\",\"Locked\":true}");
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"Id\":" + note + ",\"Text\":\"kept\",\"Locked\":true}")
                                                 .when()
                                                 .put(NOTES + "/" + note)
                                                 .then()
                                                 .statusCode(200));
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete(NOTES + "/" + note)
                                                 .then()
                                                 .statusCode(400)
                                                 .body(containsString(NO_REMOVAL)));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(NOTES + "/" + note)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Text", equalTo("kept")));
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
