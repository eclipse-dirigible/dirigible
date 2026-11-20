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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
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
 * A roll-up's write-back is not refused by the parent's {@code lifecycle:} either (dirigible
 * #8750).
 *
 * <p>
 * An invoice's paid total is summed from its allocations, and the roll-up owns the invoice's status
 * while that sum is above zero: ISSUED moves to PARTIAL with the first allocation. The invoice's
 * lifecycle models only the forward moves, so when the only allocation is deleted the recomputed
 * row - Paid 0, the displaced ISSUED restored - is a move PARTIAL -> ISSUED the lifecycle does not
 * declare. Before the fix the handler's write was refused, logged and redelivered into the same
 * refusal: the delete had committed, so the invoice kept Paid 40 against no allocation at all. The
 * refusal is permanent, so the await below never succeeds on a build without the fix.
 *
 * <p>
 * Authored writes stay gated: {@code IntentOneHopSetterIT} keeps a workflow setter's unmodeled move
 * refused through the same targeted write path.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentRollupWriteBackLifecycleIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "rolluplifecycle";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String INVOICES = API + "/invoice/InvoiceController";
    private static final String ALLOCATIONS = API + "/allocation/AllocationController";
    private static final long TIMEOUT_SECONDS = 90;
    /** The roll-up runs off the child's topics, after the commit. */
    private static final long ROLLUP_TIMEOUT_SECONDS = 30;
    private static final int ISSUED = 2;
    private static final int PARTIAL = 3;

    private static final String INTENT_YAML = """
            name: rolluplifecycle
            description: a roll-up write-back is not refused by the parent's lifecycle

            entities:
              - name: InvoiceStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: Invoice
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: name,  type: string, length: 40 }
                  - { name: total, type: decimal, precision: 18, scale: 2 }
                  - { name: paid,  type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 2 }
                lifecycle:
                  edges:
                    - { from: 1, to: [2] }
                    - { from: 2, to: [3, 4] }
                    - { from: 3, to: [4] }

              - name: Allocation
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Invoice, kind: manyToOne, to: Invoice, required: true }

            rollups:
              - { name: invoicePaid, entity: Allocation, via: Invoice, field: paid, op: sum, of: amount,
                  capacity: total, status: Status, statusWhenFull: 4, statusWhenPartial: 3 }

            seeds:
              - name: invoice-statuses
                entity: InvoiceStatus
                rows:
                  - { id: 1, name: Draft }
                  - { id: 2, name: Issued }
                  - { id: 3, name: Partial }
                  - { id: 4, name: Paid }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_roll_up_write_back_the_parent_lifecycle_would_refuse_still_lands() {
        writeIntent();
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        // Issued at create: the lifecycle's start. The status is the roll-up's column, so no plain edit
        // moves it - the roll-up and the lifecycle are the only two parties to this test.
        int invoice = create(INVOICES, "{\"Name\":\"INV-1\",\"Total\":100}");
        awaitInvoice(invoice, null, ISSUED);
        int allocation = create(ALLOCATIONS, "{\"Amount\":40,\"Invoice\":" + invoice + "}");
        awaitInvoice(invoice, 40.0f, PARTIAL);

        // The only allocation goes: the recomputed row is ISSUED again with Paid 0 - a move the
        // lifecycle does not declare. The delete has committed, so the total must follow it.
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete(ALLOCATIONS + "/" + allocation)
                                                 .then()
                                                 .statusCode(greaterThanOrEqualTo(200)),
                TIMEOUT_SECONDS);
        awaitInvoice(invoice, 0.0f, ISSUED);
    }

    private void awaitInvoice(int invoice, Float paid, int status) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(INVOICES + "/" + invoice)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Paid", paid == null ? nullValue() : equalTo(paid))
                                                 .body("Status", equalTo(status)),
                ROLLUP_TIMEOUT_SECONDS);
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
