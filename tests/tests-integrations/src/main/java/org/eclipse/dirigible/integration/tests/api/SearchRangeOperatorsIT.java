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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
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
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;

/**
 * The slot picker's server side.
 *
 * <p>
 * What it reads: the generated controller's {@code /search} takes a half-open range (dirigible
 * #7837): {@code GTE} and {@code LT} on a date and on a timestamp column, which is how the slot
 * picker reads the bookings of the days it shows instead of every booking ever made. The values
 * arrive as the ISO strings a page sends - a {@code YYYY-MM-DD} day for a date column, an instant
 * for a timestamp column - and the controller coerces them to the column's Java type.
 *
 * <p>
 * What it promises (dirigible #8097): a {@code slots:} window and a booked slot hold for every user
 * write, not only for the picker.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SearchRangeOperatorsIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "search-range";
    private static final String API = "/services/java/" + PROJECT + "/gen/range/api";
    private static final String BOOKINGS = API + "/booking/BookingController";
    private static final String APPOINTMENTS = API + "/appointment/AppointmentController";
    private static final String MY_APPOINTMENTS = API + "/appointment/AppointmentMyController";
    private static final String PARTNER_APPOINTMENTS = API + "/appointment/AppointmentPartnerController";
    private static final long TIMEOUT_SECONDS = 90;

    /** One week of 2030: the days the window is read on, as wall-clock dates in the server's zone. */
    private static final String SATURDAY = "2030-01-05";
    private static final String SUNDAY = "2030-01-06";
    private static final String MONDAY = "2030-01-07";

    private static final String CLOSED = "Appointment slots are closed on this day";
    private static final String HOURS = "Appointment slots run from 09:00 to 18:00";
    private static final String GRID = "Appointment slots start every 30 minutes from 09:00";
    private static final String BOOKED = "This Appointment slot is already booked for this Employee";
    private static final String BOOKED_BG = "Служителят вече има запазен час в този слот";

    /**
     * Booking carries the day-edge rows the range search reads, so it is no slots entity: its rows lie
     * outside any window. Appointment is the issue's shape, booked per Employee (the personal owner),
     * for a Customer (the partner owner) - so all three surfaces write it.
     */
    private static final String INTENT = """
            name: range
            description: the slot picker's server side - bookings read by range, and the window they are held to

            entities:
              - name: Booking
                fields:
                  - { name: id,       type: integer, primaryKey: true, generated: true }
                  - { name: day,      type: date }
                  - { name: startsAt, type: timestamp }

              - name: Person
                identity: email
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: name,  type: string, required: true, length: 100 }
                  - { name: email, type: string, required: true, length: 100 }

              - name: Appointment
                view: slots
                slots: { start: at, open: "09:00", close: "18:00", step: 30, disabledDays: [0, 6], resource: Employee }
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: at,   type: timestamp, required: true }
                  - { name: note, type: string, length: 100 }
                relations:
                  - { name: Employee, kind: manyToOne, to: Person, personal: true }
                  - { name: Customer, kind: manyToOne, to: Person, partner: true }

            seeds:
              - name: people
                entity: Person
                rows:
                  - { id: 1, name: Admin, email: admin }
                  - { id: 2, name: Other, email: other }
            """;

    /**
     * A translator's overlay of the booked refusal, under the key the generated en-US catalog writes.
     */
    private static final String BG_OVERLAY = """
            {
              "range-model": {
                "checks": {
                  "Appointment_slots_booked": "Служителят вече има запазен час в този слот"
                }
              }
            }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void gte_and_lt_select_the_half_open_range_of_a_date_and_a_timestamp_column() {
        writeResource(projectPath() + "/app.intent", INTENT);
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

    /**
     * The picker offers weekdays, 09:00 to 18:00, every 30 minutes, and marks a slot an Employee holds
     * taken. A REST write gets the same answers, read in the server's zone as the picker reads its own:
     * a closed day, an hour outside the window and a minute off the grid are a 400, and a second
     * booking of one Employee's slot a 409 - on the power, the personal and the partner surface. The
     * same slot stays free for another Employee, and a booking edited where it is does not collide with
     * itself.
     */
    @Test
    void a_write_outside_the_slot_window_or_into_a_booked_slot_is_refused() {
        writeResource(projectPath() + "/app.intent", INTENT);
        generateProject();
        String catalog = contentOf(projectPath() + "/i18n/en-US/range.model.json");
        assertTrue(catalog.contains("\"Appointment_slots_booked\": \"" + BOOKED + "\""), catalog);
        writeResource(projectPath() + "/i18n/bg-BG/range.model.json", BG_OVERLAY);
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        // The window: a closed weekday, before opening, at closing (the end is exclusive), off the grid.
        post(APPOINTMENTS, appointment(SUNDAY, 10, 0, 2), null, 400).body("message", equalTo(CLOSED));
        post(APPOINTMENTS, appointment(MONDAY, 8, 30, 2), null, 400).body("message", equalTo(HOURS));
        post(APPOINTMENTS, appointment(MONDAY, 18, 0, 2), null, 400).body("message", equalTo(HOURS));
        post(APPOINTMENTS, appointment(MONDAY, 10, 7, 2), null, 400).body("message", equalTo(GRID));

        // A free slot is booked; the same slot for the same Employee is taken, in the reader's language.
        int booked = id(post(APPOINTMENTS, appointment(MONDAY, 10, 0, 2), null, 200));
        post(APPOINTMENTS, appointment(MONDAY, 10, 0, 2), null, 409).body("message", equalTo(BOOKED));
        post(APPOINTMENTS, appointment(MONDAY, 10, 0, 2), "bg", 409).body("message", equalTo(BOOKED_BG));
        // ...and free for another Employee.
        int other = id(post(APPOINTMENTS, appointment(MONDAY, 10, 0, 1), null, 200));

        // An edit that keeps the booking where it is does not collide with itself; moving the other
        // Employee's booking onto the taken slot does.
        put(APPOINTMENTS, booked, "{\"At\":\"" + at(MONDAY, 10, 0) + "\",\"Employee\":2,\"Note\":\"kept\"}", 200).body("Note",
                equalTo("kept"));
        put(APPOINTMENTS, other, "{\"At\":\"" + at(MONDAY, 10, 0) + "\",\"Employee\":2}", 409).body("message", equalTo(BOOKED));

        // The personal surface books for the caller (Employee 1, who holds 10:00), the partner one for
        // its customer - both held to the same window and the same slot.
        post(MY_APPOINTMENTS, "{\"At\":\"" + at(SATURDAY, 10, 0) + "\"}", null, 400).body("message", equalTo(CLOSED));
        post(MY_APPOINTMENTS, "{\"At\":\"" + at(MONDAY, 10, 0) + "\"}", null, 409).body("message", equalTo(BOOKED));
        post(PARTNER_APPOINTMENTS, appointment(MONDAY, 10, 15, 2), null, 400).body("message", equalTo(GRID));
        post(PARTNER_APPOINTMENTS, appointment(MONDAY, 10, 0, 2), null, 409).body("message", equalTo(BOOKED));
        post(PARTNER_APPOINTMENTS, appointment(MONDAY, 10, 30, 2), null, 200);
    }

    /**
     * {@code date} at {@code hour:minute} on the server's wall clock - the zone the window is read in.
     */
    private static String at(String date, int hour, int minute) {
        return LocalDate.parse(date)
                        .atTime(hour, minute)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toString();
    }

    private static String appointment(String date, int hour, int minute, int employee) {
        return "{\"At\":\"" + at(date, hour, minute) + "\",\"Employee\":" + employee + ",\"Customer\":1}";
    }

    /** The answer once the controller is up - a write sent before it is deployed is retried. */
    private ValidatableResponse post(String controller, String body, String language, int status) {
        AtomicReference<ValidatableResponse> response = new AtomicReference<>();
        restAssuredExecutor.execute(() -> response.set(request(language).body(body)
                                                                        .when()
                                                                        .post(controller)
                                                                        .then()
                                                                        .statusCode(status)),
                TIMEOUT_SECONDS);
        return response.get();
    }

    private ValidatableResponse put(String controller, int id, String body, int status) {
        AtomicReference<ValidatableResponse> response = new AtomicReference<>();
        restAssuredExecutor.execute(() -> response.set(request(null).body(body)
                                                                    .when()
                                                                    .put(controller + "/" + id)
                                                                    .then()
                                                                    .statusCode(status)),
                TIMEOUT_SECONDS);
        return response.get();
    }

    private static RequestSpecification request(String language) {
        RequestSpecification request = given().contentType(ContentType.JSON);
        return language == null ? request : request.header("Accept-Language", language);
    }

    private static int id(ValidatableResponse created) {
        return created.extract()
                      .path("Id");
    }

    private String contentOf(String path) {
        IResource resource = repository.getResource(path);
        assertTrue(resource.exists(), path + " is missing");
        return new String(resource.getContent(), StandardCharsets.UTF_8);
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

    private void writeResource(String path, String content) {
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(content.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, content.getBytes(StandardCharsets.UTF_8));
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
