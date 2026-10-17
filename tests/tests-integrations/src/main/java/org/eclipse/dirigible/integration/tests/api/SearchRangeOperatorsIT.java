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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
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

import io.restassured.http.ContentType;

/**
 * The generated controller's {@code /search} takes a half-open range (dirigible #7837): {@code GTE}
 * and {@code LT} on a date and on a timestamp column, which is how the slot picker reads the
 * bookings of the days it shows instead of every booking ever made.
 *
 * <p>
 * The values arrive as the ISO strings a page sends - a {@code YYYY-MM-DD} day for a date column,
 * an instant for a timestamp column - and the controller coerces them to the column's Java type.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SearchRangeOperatorsIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "search-range";
    private static final String BOOKINGS = "/services/java/" + PROJECT + "/gen/range/api/booking/BookingController";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String INTENT = """
            name: range
            description: range-search fixture - bookings on a day and at a moment

            entities:
              - name: Booking
                view: slots
                slots: { start: startsAt, open: "09:00", close: "17:00", step: 30 }
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: day,      type: date }
                  - { name: startsAt, type: timestamp }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void gte_and_lt_select_the_half_open_range_of_a_date_and_a_timestamp_column() {
        writeIntent();
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int before = create("2026-03-09", "2026-03-09T23:30:00Z");
        int first = create("2026-03-10", "2026-03-10T00:00:00Z");
        int last = create("2026-03-11", "2026-03-11T23:59:00Z");
        int after = create("2026-03-12", "2026-03-12T00:00:00Z");

        // The lower bound is inclusive, the upper one exclusive: [10th, 12th).
        assertSearch("Day", "2026-03-10", "2026-03-12", first, last);
        assertSearch("StartsAt", "2026-03-10T00:00:00Z", "2026-03-12T00:00:00Z", first, last);
        // Each bound on its own.
        assertSearchOne("Day", "GTE", "2026-03-12", after);
        assertSearchOne("StartsAt", "LT", "2026-03-10T00:00:00Z", before);
    }

    private void assertSearch(String property, String from, String to, Integer... ids) {
        String body = "{\"conditions\":[{\"propertyName\":\"" + property + "\",\"operator\":\"GTE\",\"value\":\"" + from + "\"},"
                + "{\"propertyName\":\"" + property + "\",\"operator\":\"LT\",\"value\":\"" + to + "\"}]}";
        restAssuredExecutor.execute(() -> given().when()
                                                 .contentType(ContentType.JSON)
                                                 .body(body)
                                                 .post(BOOKINGS + "/search")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Id", containsInAnyOrder(ids)),
                TIMEOUT_SECONDS);
    }

    private void assertSearchOne(String property, String operator, String value, int id) {
        String body =
                "{\"conditions\":[{\"propertyName\":\"" + property + "\",\"operator\":\"" + operator + "\",\"value\":\"" + value + "\"}]}";
        restAssuredExecutor.execute(() -> given().when()
                                                 .contentType(ContentType.JSON)
                                                 .body(body)
                                                 .post(BOOKINGS + "/search")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Id", containsInAnyOrder(id)),
                TIMEOUT_SECONDS);
    }

    private int create(String day, String startsAt) {
        AtomicReference<Integer> id = new AtomicReference<>();
        restAssuredExecutor.execute(() -> id.set(given().when()
                                                        .contentType(ContentType.JSON)
                                                        .body("{\"Day\":\"" + day + "\",\"StartsAt\":\"" + startsAt + "\"}")
                                                        .post(BOOKINGS)
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
        String path = projectPath() + "/app.intent";
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(INTENT.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, INTENT.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String projectPath() {
        return IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    }

    @AfterEach
    void cleanup() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT)
                                                 .then()
                                                 .statusCode(both(greaterThanOrEqualTo(200)).and(lessThan(300))));
        if (repository.hasCollection(projectPath())) {
            repository.removeCollection(projectPath());
        }
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
