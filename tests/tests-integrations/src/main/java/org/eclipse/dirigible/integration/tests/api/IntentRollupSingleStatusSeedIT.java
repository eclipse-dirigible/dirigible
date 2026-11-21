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
 * A roll-up {@code status:} may name only one of its two seeds (dirigible #7941).
 *
 * <p>
 * The missing seed used to reach the emitter as an empty literal -
 * {@code Objects.equals(parent.Status, )} and {@code ? 3 : ;} - which does not compile, and since
 * one javac task compiles the whole client-Java batch, every controller of the application answered
 * 404. Both one-seed shapes are declared here side by side, so the first create already proves the
 * batch compiled; the rest pins what they mean: the roll-up owns only the band it names, and a sum
 * outside it gives back the status the roll-up displaced, as a zero sum does.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentRollupSingleStatusSeedIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "rollupsingleseed";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String ASSETS = API + "/asset/AssetController";
    private static final String DEPRECIATIONS = API + "/depreciation/DepreciationController";
    private static final String BOOKINGS = API + "/booking/BookingController";
    private static final String DEPOSITS = API + "/deposit/DepositController";
    private static final long TIMEOUT_SECONDS = 90;
    /** The roll-up runs off the child's topics, after the commit. */
    private static final long ROLLUP_TIMEOUT_SECONDS = 30;
    private static final int ACTIVE = 1;
    private static final int FULLY_DEPRECIATED = 2;
    private static final int OPEN = 1;
    private static final int PARTIALLY_DEPOSITED = 2;

    private static final String INTENT_YAML = """
            name: rollupsingleseed
            description: a roll-up status naming only one of its two seeds

            entities:
              - name: AssetStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: Asset
                fields:
                  - { name: id,          type: integer, primaryKey: true, generated: true }
                  - { name: name,        type: string, length: 40 }
                  - { name: cost,        type: decimal, precision: 18, scale: 2 }
                  - { name: depreciated, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Status, kind: manyToOne, to: AssetStatus, function: EntityStatus, init: 1 }

              - name: Depreciation
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Asset, kind: manyToOne, to: Asset, required: true }

              - name: BookingStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              - name: Booking
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: name,      type: string, length: 40 }
                  - { name: total,     type: decimal, precision: 18, scale: 2 }
                  - { name: deposited, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Status, kind: manyToOne, to: BookingStatus, function: EntityStatus, init: 1 }

              - name: Deposit
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: amount, type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Booking, kind: manyToOne, to: Booking, required: true }

            rollups:
              - { name: assetDepreciated, entity: Depreciation, via: Asset, field: depreciated, op: sum, of: amount,
                  capacity: cost, status: Status, statusWhenFull: 2 }
              - { name: bookingDeposited, entity: Deposit, via: Booking, field: deposited, op: sum, of: amount,
                  capacity: total, status: Status, statusWhenPartial: 2 }

            seeds:
              - name: asset-statuses
                entity: AssetStatus
                rows:
                  - { id: 1, name: Active }
                  - { id: 2, name: Fully depreciated }
              - name: booking-statuses
                entity: BookingStatus
                rows:
                  - { id: 1, name: Open }
                  - { id: 2, name: Partially deposited }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_status_roll_up_naming_one_seed_compiles_and_owns_only_that_band() {
        writeIntent();
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        // Full only: below the cost the asset stays Active; at the cost it is fully depreciated; a
        // reversed depreciation takes it out of the full band, back to the status it displaced.
        int asset = create(ASSETS, "{\"Name\":\"Truck\",\"Cost\":100}");
        create(DEPRECIATIONS, "{\"Amount\":40,\"Asset\":" + asset + "}");
        awaitRow(ASSETS, asset, "Depreciated", 40.0f, ACTIVE);
        int last = create(DEPRECIATIONS, "{\"Amount\":60,\"Asset\":" + asset + "}");
        awaitRow(ASSETS, asset, "Depreciated", 100.0f, FULLY_DEPRECIATED);
        delete(DEPRECIATIONS, last);
        awaitRow(ASSETS, asset, "Depreciated", 40.0f, ACTIVE);

        // Partial only: a first deposit marks the booking partially deposited; reaching the total leaves
        // the band the roll-up owns, so the booking gets back the status it displaced.
        int booking = create(BOOKINGS, "{\"Name\":\"SO-1\",\"Total\":100}");
        create(DEPOSITS, "{\"Amount\":30,\"Booking\":" + booking + "}");
        awaitRow(BOOKINGS, booking, "Deposited", 30.0f, PARTIALLY_DEPOSITED);
        create(DEPOSITS, "{\"Amount\":70,\"Booking\":" + booking + "}");
        awaitRow(BOOKINGS, booking, "Deposited", 100.0f, OPEN);
    }

    private void awaitRow(String controller, int id, String total, float value, int status) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(controller + "/" + id)
                                                 .then()
                                                 .statusCode(200)
                                                 .body(total, equalTo(value))
                                                 .body("Status", equalTo(status)),
                ROLLUP_TIMEOUT_SECONDS);
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
