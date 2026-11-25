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
import static org.hamcrest.Matchers.endsWith;
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
 * A {@code month} field holds a {@code YYYY-MM} month and a {@code week} field an ISO
 * {@code YYYY-Www} week (dirigible #8328) - on the power, the personal and the partner surface, on
 * a create and on an update.
 *
 * <p>
 * Both are string columns, and the generated controllers stored whatever string a REST client sent:
 * {@code 2026-5} beside {@code 2026-05} opened one month twice under a unique key, and
 * {@code 2026-13} failed every reader that parses the value. Asserted on the running, generated
 * code: each value of the issue is refused with a 400 naming the field, and a real month and week
 * are stored as sent - 2026-W53 included, 2026 being a year with a 53rd ISO week.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentMonthWeekShapeIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "monthweek";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String TIMESHEETS = API + "/timesheet/TimesheetController";
    private static final String MY_TIMESHEETS = API + "/timesheet/TimesheetMyController";
    private static final String BOOKINGS = API + "/booking/BookingController";
    private static final String PARTNER_BOOKINGS = API + "/booking/BookingPartnerController";
    private static final long TIMEOUT_SECONDS = 90;

    /** The values of the issue, and the neighbours of a valid month. */
    private static final List<String> NOT_MONTHS = List.of("2026-5", "202605", "abc", "2026-13", "2026-00", "26-05");

    /** The week counterparts - 2025 has no 53rd week. */
    private static final List<String> NOT_WEEKS = List.of("2026-W5", "2026W05", "abc", "2026-W00", "2026-W54", "2025-W53", "2026-05");

    private static final String INTENT_YAML = """
            name: monthweek
            description: a month field holds a month, a week field an ISO week

            entities:
              - name: Employee
                identity: email
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: name,  type: string, required: true, length: 100 }
                  - { name: email, type: string, required: true, length: 100 }

              # The power and the personal surface; a required month and an optional week.
              - name: Timesheet
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: period, type: month, required: true }
                  - { name: sprint, type: week }
                relations:
                  - { name: Employee, kind: manyToOne, to: Employee, required: true, personal: true }

              # The partner surface; an optional month and a required week.
              - name: Booking
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: startMonth, type: month }
                  - { name: slot,    type: week, required: true }
                relations:
                  - { name: Employee, kind: manyToOne, to: Employee, required: true, partner: true }

            seeds:
              - name: employees
                entity: Employee
                rows:
                  - { id: 1, name: Admin, email: admin }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Test
    void a_month_field_holds_a_month_and_a_week_field_an_iso_week_on_every_surface() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        // Power surface: a real month and week - and a blank optional week, which is no value - are stored.
        int timesheet = create(TIMESHEETS, "{\"Period\":\"2026-05\",\"Sprint\":\"2026-W53\",\"Employee\":1}");
        create(TIMESHEETS, "{\"Period\":\"2026-12\",\"Sprint\":\"\",\"Employee\":1}");
        for (String month : NOT_MONTHS) {
            assertRefused("POST", TIMESHEETS, "{\"Period\":\"" + month + "\",\"Employee\":1}", "Period", "fieldMonth");
            assertRefused("PUT", TIMESHEETS + "/" + timesheet, "{\"Id\":" + timesheet + ",\"Period\":\"" + month + "\",\"Employee\":1}",
                    "Period", "fieldMonth");
        }
        for (String week : NOT_WEEKS) {
            assertRefused("POST", TIMESHEETS, "{\"Period\":\"2026-05\",\"Sprint\":\"" + week + "\",\"Employee\":1}", "Sprint", "fieldWeek");
            assertRefused("PUT", TIMESHEETS + "/" + timesheet,
                    "{\"Id\":" + timesheet + ",\"Period\":\"2026-05\",\"Sprint\":\"" + week + "\",\"Employee\":1}", "Sprint", "fieldWeek");
        }
        update(TIMESHEETS + "/" + timesheet, "{\"Id\":" + timesheet + ",\"Period\":\"2026-01\",\"Sprint\":\"2026-W01\",\"Employee\":1}");
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(TIMESHEETS + "/" + timesheet)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Period", equalTo("2026-01"))
                                                 .body("Sprint", equalTo("2026-W01")));

        // Personal surface.
        create(MY_TIMESHEETS, "{\"Period\":\"2026-06\",\"Sprint\":\"2026-W22\"}");
        for (String month : NOT_MONTHS) {
            assertRefused("POST", MY_TIMESHEETS, "{\"Period\":\"" + month + "\"}", "Period", "fieldMonth");
        }
        for (String week : NOT_WEEKS) {
            assertRefused("POST", MY_TIMESHEETS, "{\"Period\":\"2026-06\",\"Sprint\":\"" + week + "\"}", "Sprint", "fieldWeek");
        }

        // Partner surface.
        create(PARTNER_BOOKINGS, "{\"StartMonth\":\"2026-07\",\"Slot\":\"2020-W53\"}");
        for (String month : NOT_MONTHS) {
            assertRefused("POST", PARTNER_BOOKINGS, "{\"StartMonth\":\"" + month + "\",\"Slot\":\"2026-W10\"}", "StartMonth", "fieldMonth");
        }
        for (String week : NOT_WEEKS) {
            assertRefused("POST", PARTNER_BOOKINGS, "{\"Slot\":\"" + week + "\"}", "Slot", "fieldWeek");
        }
        create(BOOKINGS, "{\"Slot\":\"2026-W10\",\"Employee\":1}");
        assertRefused("POST", BOOKINGS, "{\"Slot\":\"2026-W54\",\"Employee\":1}", "Slot", "fieldWeek");
    }

    private void assertRefused(String method, String controller, String body, String field, String key) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(body)
                                                 .when()
                                                 .request(method, controller)
                                                 .then()
                                                 .statusCode(400)
                                                 .body("field", equalTo(field))
                                                 .body("messageKey", endsWith(".checks." + key)),
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

    private void update(String path, String body) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(body)
                                                 .when()
                                                 .put(path)
                                                 .then()
                                                 .statusCode(200));
    }

    private void generateProject() {
        writeResource(PROJECT_PATH + "/app.intent", INTENT_YAML);
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
