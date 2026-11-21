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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
 * A duplicate refused at a status gate - dirigible #7750.
 *
 * <p>
 * Two DRAFT bills from one supplier with the same supplier invoice number may coexist (a clerk
 * re-keys a bill; the soft {@code duplicate} warning asks first), but the second one may not be
 * APPROVED while the first is APPROVED or PAID - that is the same paper booked twice. A
 * {@code duplicate} check with a {@code status} gate and {@code among} declares it. This asserts
 * the generated repository end to end: the transition into the gate is refused with the authored
 * message naming the record it collides with, a cancelled bill does not count, and two approvals of
 * twins fired at once let exactly one through.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentGatedDuplicateIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "gateddup";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String SUPPLIERS = API + "/supplier/SupplierController";
    private static final String BILLS = API + "/bill/BillController";
    private static final String TRANSITIONS = "/services/java/" + PROJECT + "/gen/events/" + PROJECT + "/";
    private static final String APPROVE = TRANSITIONS + "ApproveBillTransition/run";
    private static final String CANCEL = TRANSITIONS + "CancelBillTransition/run";
    private static final String CONFIRM = "X-Confirm-Warnings";
    private static final String SOFT_DUPLICATE = "Bill.duplicate.0";
    private static final String REFUSAL = "This supplier invoice number is already approved for this supplier: ";
    private static final int DRAFT = 1;
    private static final int APPROVED = 2;
    private static final int CANCELLED = 4;
    private static final int RACE_ATTEMPTS = 8;
    private static final long RACE_TIMEOUT_SECONDS = 60;
    private static final long TIMEOUT_SECONDS = 90;

    private static final String INTENT_YAML = """
            name: gateddup
            description: gated duplicate fixture - a supplier invoice may be keyed twice, never approved twice

            entities:
              - name: BillStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: Supplier
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 100 }

              - name: Bill
                checks:
                  - { kind: duplicate, fields: [Supplier, supplierNumber], message: "This supplier invoice is already entered: {match}" }
                  - { kind: duplicate, fields: [Supplier, supplierNumber], status: APPROVED, among: [APPROVED, PAID], severity: error,
                      message: "This supplier invoice number is already approved for this supplier: {match}" }
                fields:
                  - { name: id,             type: integer, primaryKey: true, generated: true }
                  - { name: name,           type: string, length: 100 }
                  - { name: supplierNumber, type: string, length: 50 }
                relations:
                  - { name: Status,   kind: manyToOne, to: BillStatus, function: EntityStatus, init: 1 }
                  - { name: Supplier, kind: manyToOne, to: Supplier }

            transitions:
              - { name: ApproveBill, forEntity: Bill, from: [DRAFT], setStatus: APPROVED }
              - { name: CancelBill,  forEntity: Bill, from: [DRAFT, APPROVED], setStatus: CANCELLED }

            seeds:
              - name: bill-statuses
                entity: BillStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: APPROVED }
                  - { id: 3, name: PAID }
                  - { id: 4, name: CANCELLED }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void the_same_supplier_invoice_is_keyed_twice_but_approved_once() {
        writeIntent();
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int supplier = create(SUPPLIERS, "{\"Name\":\"Acme\"}", null);
        int first = create(BILLS, bill("first", supplier, "INV-1"), null);
        // A draft twin is the soft warning, as before: asked, then written once confirmed.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(bill("second", supplier, "INV-1"))
                                                 .when()
                                                 .post(BILLS)
                                                 .then()
                                                 .statusCode(428)
                                                 .body("warnings[0].code", equalTo(SOFT_DUPLICATE)),
                TIMEOUT_SECONDS);
        int second = create(BILLS, bill("second", supplier, "INV-1"), SOFT_DUPLICATE);

        // The first approval is nobody's duplicate...
        assertEquals(200, postOnce(APPROVE, first));
        // ...the second is refused with the authored message naming the approved bill, and stays a draft.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + second + "}")
                                                 .when()
                                                 .post(APPROVE)
                                                 .then()
                                                 .statusCode(400)
                                                 .body(containsString(REFUSAL + "first")),
                TIMEOUT_SECONDS);
        assertStatus(second, DRAFT);
        // The values compare as the warning compares them (#7524): another spelling is the same number.
        int respelled = create(BILLS, bill("respelled", supplier, " inv-1 "), SOFT_DUPLICATE);
        assertEquals(400, postOnce(APPROVE, respelled));

        // A cancelled bill is not among the statuses that count: once the first is cancelled, the twin
        // is approved.
        assertEquals(200, postOnce(CANCEL, first));
        assertStatus(first, CANCELLED);
        assertEquals(200, postOnce(APPROVE, second));
        assertStatus(second, APPROVED);

        // Another supplier's bill with the same number is no duplicate.
        int other = create(SUPPLIERS, "{\"Name\":\"Globex\"}", null);
        assertEquals(200, postOnce(APPROVE, create(BILLS, bill("other", other, "INV-1"), null)));

        // Two approvals of twins fired at once: exactly one passes, the other is refused.
        ExecutorService racers = Executors.newFixedThreadPool(2);
        try {
            for (int attempt = 0; attempt < RACE_ATTEMPTS; attempt++) {
                String number = "RACE-" + attempt;
                int left = create(BILLS, bill("left-" + attempt, supplier, number), null);
                int right = create(BILLS, bill("right-" + attempt, supplier, number), SOFT_DUPLICATE);
                List<Integer> statuses = race(racers, List.of(() -> post(APPROVE, left), () -> post(APPROVE, right)));
                assertEquals(1, statuses.stream()
                                        .filter(status -> status == 200)
                                        .count(),
                        "attempt " + attempt + ": exactly one approval may pass, answered " + statuses);
                assertEquals(1, statuses.stream()
                                        .filter(status -> status == 400)
                                        .count(),
                        "attempt " + attempt + ": the other approval must be refused 400, answered " + statuses);
            }
        } finally {
            racers.shutdownNow();
        }
    }

    private static String bill(String name, int supplier, String number) {
        return "{\"Name\":\"" + name + "\",\"Supplier\":" + supplier + ",\"SupplierNumber\":\"" + number + "\"}";
    }

    /** Releases the requests through one barrier; answers their status codes in order. */
    private List<Integer> race(ExecutorService racers, List<Callable<Integer>> requests) {
        return restAssuredExecutor.executeWithResult(() -> {
            CyclicBarrier start = new CyclicBarrier(requests.size());
            List<Future<Integer>> answers = new ArrayList<>();
            for (Callable<Integer> request : requests) {
                answers.add(racers.submit(() -> {
                    start.await();
                    return request.call();
                }));
            }
            List<Integer> statuses = new ArrayList<>();
            try {
                for (Future<Integer> answer : answers) {
                    statuses.add(answer.get(RACE_TIMEOUT_SECONDS, TimeUnit.SECONDS));
                }
            } catch (InterruptedException e) {
                Thread.currentThread()
                      .interrupt();
                throw new IllegalStateException("interrupted while racing concurrent approvals", e);
            } catch (Exception e) {
                throw new IllegalStateException("a racing approval did not answer", e);
            }
            return statuses;
        });
    }

    /** One transition, sent once: a retry would turn a refusal into a pass. */
    private int postOnce(String transition, int bill) {
        return restAssuredExecutor.executeWithResult(() -> post(transition, bill));
    }

    /** The bare request, for a caller already inside the executor (the race). */
    private static int post(String transition, int bill) {
        return given().contentType("application/json")
                      .body("{\"id\":" + bill + "}")
                      .when()
                      .post(transition)
                      .statusCode();
    }

    private void assertStatus(int bill, int status) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(BILLS + "/" + bill)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(status)),
                TIMEOUT_SECONDS);
    }

    private int create(String controller, String body, String confirm) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> {
            io.restassured.specification.RequestSpecification request = given().contentType("application/json");
            if (confirm != null) {
                request = request.header(CONFIRM, confirm);
            }
            id.set(request.body(body)
                          .when()
                          .post(controller)
                          .then()
                          .statusCode(200)
                          .extract()
                          .path("Id"));
        }, TIMEOUT_SECONDS);
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
