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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

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
 * A status roll-up whose sum drops back to zero gives up its FULL status even when it displaced
 * nothing on the way in (dirigible #8312).
 *
 * <p>
 * The #7016 fix restores the status a roll-up displaced, but a parent that already sat in
 * {@code statusWhenPartial} (here the asset's default ACTIVE) displaces nothing when the sum first
 * moves, so at a zero sum there was nothing to restore and the asset stayed DEPRECIATED with its
 * whole capacity left. It now falls back to the partial status. The sum is taken back to zero both
 * ways the issue names - every document dropping out of the roll-up's {@code where}, and every
 * document deleted - and a parent that DID displace a status (an invoice entering from CONFIRMED)
 * still gets that one back.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentRollupZeroSumStatusIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "rollupzerosum";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String ASSETS = API + "/asset/AssetController";
    private static final String DEPRECIATIONS = API + "/depreciation/DepreciationController";
    private static final String INVOICES = API + "/invoice/InvoiceController";
    private static final String PAYMENTS = API + "/payment/PaymentController";
    private static final long TIMEOUT_SECONDS = 90;
    /** The roll-up runs off the child's topics, after the commit. */
    private static final long ROLLUP_TIMEOUT_SECONDS = 30;
    private static final int ACTIVE = 1;
    private static final int DEPRECIATED = 2;
    private static final int PAID = 2;
    private static final int CONFIRMED = 3;

    private static final String INTENT_YAML = """
            name: rollupzerosum
            description: a status roll-up whose sum drops back to zero

            entities:
              - name: AssetStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: Asset
                fields:
                  - { name: id,               type: integer, primaryKey: true, generated: true }
                  - { name: name,             type: string, length: 40 }
                  - { name: depreciableValue, type: decimal, precision: 18, scale: 2 }
                  - { name: depreciated,      type: decimal, precision: 18, scale: 2 }
                  - { name: residual,         type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Status, kind: manyToOne, to: AssetStatus, function: EntityStatus, init: 1 }

              - name: Depreciation
                fields:
                  - { name: id,              type: integer, primaryKey: true, generated: true }
                  - { name: amount,          type: decimal, precision: 18, scale: 2 }
                  - { name: reversedEntries, type: integer, defaultValue: 0 }
                relations:
                  - { name: Asset, kind: manyToOne, to: Asset, required: true }

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
                  - { name: Status, kind: manyToOne, to: InvoiceStatus, function: EntityStatus, init: 3 }

              - name: Payment
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Invoice, kind: manyToOne, to: Invoice, required: true }

            rollups:
              - { name: assetDepreciated, entity: Depreciation, via: Asset, field: depreciated, op: sum, of: amount,
                  capacity: depreciableValue, balance: residual, status: Status, statusWhenFull: 2, statusWhenPartial: 1,
                  where: [{ field: reversedEntries, op: eq, value: 0 }] }
              - { name: invoicePaid, entity: Payment, via: Invoice, field: paid, op: sum, of: amount,
                  capacity: total, status: Status, statusWhenFull: 2, statusWhenPartial: 1 }

            seeds:
              - name: asset-statuses
                entity: AssetStatus
                rows:
                  - { id: 1, name: Active }
                  - { id: 2, name: Depreciated }
                  - { id: 3, name: Disposed }
              - name: invoice-statuses
                entity: InvoiceStatus
                rows:
                  - { id: 1, name: Partially paid }
                  - { id: 2, name: Paid }
                  - { id: 3, name: Confirmed }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_full_status_never_outlives_the_sum_that_produced_it() {
        writeIntent();
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        // The asset starts in the partial status, so reaching the full band displaces nothing; every
        // depreciation reversed out of the where takes the sum to zero and the asset back to ACTIVE.
        int reversedAsset = create(ASSETS, "{\"Name\":\"Truck\",\"DepreciableValue\":100}");
        int first = create(DEPRECIATIONS, "{\"Amount\":40,\"ReversedEntries\":0,\"Asset\":" + reversedAsset + "}");
        awaitRow(ASSETS, reversedAsset, "Depreciated", 40.0, ACTIVE);
        int second = create(DEPRECIATIONS, "{\"Amount\":60,\"ReversedEntries\":0,\"Asset\":" + reversedAsset + "}");
        awaitRow(ASSETS, reversedAsset, "Depreciated", 100.0, DEPRECIATED);
        update(DEPRECIATIONS, first, "{\"Id\":" + first + ",\"Amount\":40,\"ReversedEntries\":1,\"Asset\":" + reversedAsset + "}");
        awaitRow(ASSETS, reversedAsset, "Depreciated", 60.0, ACTIVE);
        update(DEPRECIATIONS, second, "{\"Id\":" + second + ",\"Amount\":60,\"ReversedEntries\":1,\"Asset\":" + reversedAsset + "}");
        awaitRow(ASSETS, reversedAsset, "Depreciated", 0.0, ACTIVE);

        // The same zero sum reached by deleting the only depreciation.
        int deletedAsset = create(ASSETS, "{\"Name\":\"Crane\",\"DepreciableValue\":50}");
        int only = create(DEPRECIATIONS, "{\"Amount\":50,\"ReversedEntries\":0,\"Asset\":" + deletedAsset + "}");
        awaitRow(ASSETS, deletedAsset, "Depreciated", 50.0, DEPRECIATED);
        delete(DEPRECIATIONS, only);
        awaitRow(ASSETS, deletedAsset, "Depreciated", 0.0, ACTIVE);

        // A parent that displaced a status on the way in still gets that one back (#7016).
        int invoice = create(INVOICES, "{\"Name\":\"INV-1\",\"Total\":100}");
        awaitRow(INVOICES, invoice, "Total", 100.0, CONFIRMED);
        int payment = create(PAYMENTS, "{\"Amount\":100,\"Invoice\":" + invoice + "}");
        awaitRow(INVOICES, invoice, "Paid", 100.0, PAID);
        delete(PAYMENTS, payment);
        awaitRow(INVOICES, invoice, "Paid", 0.0, CONFIRMED);
    }

    /**
     * A summed decimal reads back as {@code 100.00} or as a bare {@code 0}, so it is compared by value.
     */
    private void awaitRow(String controller, int id, String total, double value, int status) {
        restAssuredExecutor.execute(() -> {
            Map<String, Object> row = given().when()
                                             .get(controller + "/" + id)
                                             .then()
                                             .statusCode(200)
                                             .body("Status", equalTo(status))
                                             .extract()
                                             .jsonPath()
                                             .getMap("$");
            Object actual = row.get(total);
            assertInstanceOf(Number.class, actual, total + " of " + controller + "/" + id);
            assertEquals(value, ((Number) actual).doubleValue(), 0.001, total + " of " + controller + "/" + id);
        }, ROLLUP_TIMEOUT_SECONDS);
    }

    private void update(String controller, int id, String body) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(body)
                                                 .when()
                                                 .put(controller + "/" + id)
                                                 .then()
                                                 .statusCode(200),
                TIMEOUT_SECONDS);
    }

    private void delete(String controller, int id) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete(controller + "/" + id)
                                                 .then()
                                                 .statusCode(greaterThanOrEqualTo(200)),
                TIMEOUT_SECONDS);
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
