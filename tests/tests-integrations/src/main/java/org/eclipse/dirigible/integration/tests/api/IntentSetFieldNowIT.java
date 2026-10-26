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

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
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
 * A process step can record when something happened - dirigible #7873.
 *
 * <p>
 * {@code setField} took only literal values on string fields, so a flow that wanted an
 * {@code approvedAt} beside the status it writes had to hand-write a {@code delegate:} whose whole
 * body was one {@code updateProperty(id, "ApprovedAt", Instant.now())}. {@code value: "{now}"} on a
 * {@code timestamp} or {@code date} field is that stamp, and this asserts it end to end - the
 * generated setter delegates really compile against the entity's {@code java.time} properties,
 * really run, and really write the moment the flow reached them: the timestamp lies between the
 * instants taken before the record was created and after both columns were observed, and the date
 * is today.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentSetFieldNowIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "setfieldnow";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String ORDERS = API + "/order/OrderController";
    private static final long TIMEOUT_SECONDS = 90;
    /** The steps run once the create event has started the instance. */
    private static final long PROCESS_TIMEOUT_SECONDS = 60;

    private static final String INTENT_YAML = """
            name: setfieldnow
            description: an approval flow stamps the moment and the day it approved the order

            entities:
              - name: Order
                fields:
                  - { name: id,         type: integer, primaryKey: true, generated: true }
                  - { name: reference,  type: string, required: true, length: 100 }
                  - { name: approvedAt, type: timestamp, readOnly: true }
                  - { name: approvedOn, type: date, readOnly: true }

            processes:
              - name: OrderApproval
                trigger: { onCreate: Order }
                steps:
                  - { name: stampMoment, kind: serviceTask, args: { setField: approvedAt, value: "{now}", next: stampDay } }
                  - { name: stampDay,    kind: serviceTask, args: { setField: approvedOn, value: "{now}", next: done } }
                  - { name: done,        kind: end }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_process_stamps_a_timestamp_and_a_date_field_with_the_moment_it_reached_them() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        Instant before = Instant.now();
        LocalDate dayBefore = LocalDate.now();
        int order = create("{\"Reference\":\"PO-1\"}");
        Map<String, Object> stamped = awaitStamped(order);
        Instant after = Instant.now();
        LocalDate dayAfter = LocalDate.now();

        // The controller answers ISO-8601 (#8083): an Instant as "...Z", a LocalDate as "yyyy-MM-dd".
        Instant approvedAt = Instant.parse((String) stamped.get("ApprovedAt"));
        assertFalse(approvedAt.isBefore(before), "the stamp must be taken after the order was created: " + approvedAt + " < " + before);
        assertFalse(approvedAt.isAfter(after), "the stamp must be taken before the flow was observed done: " + approvedAt + " > " + after);

        LocalDate approvedOn = LocalDate.parse((String) stamped.get("ApprovedOn"));
        assertTrue(approvedOn.equals(dayBefore) || approvedOn.equals(dayAfter), "the date stamp must be today: " + approvedOn);
    }

    /** The order once both stamp steps have run. */
    private Map<String, Object> awaitStamped(int order) {
        AtomicReference<Map<String, Object>> stamped = new AtomicReference<>();
        restAssuredExecutor.execute(() -> stamped.set(given().when()
                                                             .get(ORDERS + "/" + order)
                                                             .then()
                                                             .statusCode(200)
                                                             .body("ApprovedAt", notNullValue())
                                                             .body("ApprovedOn", notNullValue())
                                                             .extract()
                                                             .jsonPath()
                                                             .getMap("")),
                PROCESS_TIMEOUT_SECONDS);
        return stamped.get();
    }

    private int create(String body) {
        AtomicInteger id = new AtomicInteger();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(ORDERS)
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
