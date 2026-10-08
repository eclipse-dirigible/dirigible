/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.ui.tests;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Selenide;

/**
 * The generated slots page ({@code view: slots}) offers its open/close/step schedule on every day
 * no booking occupies, before anything is booked as well as after. The page hands Harmonia's slot
 * picker the booked days as explicit {@code slots}; a day missing from them shows the schedule only
 * with {@code fillEmptyDays}, and since Harmonia 3.6.0 (codbex/harmonia#144) an empty {@code slots}
 * array is an empty schedule rather than no configuration at all. Without the option a booking
 * entity offered no free slot until something was booked, and afterwards only on the booked days.
 *
 * <p>
 * This needs a real browser because the schedule is laid out by Harmonia from the config the page
 * builds at runtime - the generated source is byte-correct either way.
 */
class SlotPickerHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final String PROJECT = "slot-picker-it";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/clinic/index.html#";
    private static final String API = "/services/java/" + PROJECT + "/gen/clinic/api/appointment/AppointmentController";

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    /** Three one-hour slots a day, no closed weekdays - every visible day offers the same three. */
    private static final String INTENT_YAML = """
            name: clinic
            entities:
              - name: Appointment
                view: slots
                slots: { start: startsAt, open: "09:00", close: "12:00", step: 60 }
                fields:
                  - { name: id, type: integer, primaryKey: true, generated: true }
                  - { name: startsAt, type: timestamp, required: true }
                  - { name: title, type: string, length: 60 }
            """;

    private static final String FREE_DAY = "free=09:00,10:00,11:00 taken=";

    private static final String BOOKED_DAY = "free=09:00,11:00 taken=10:00";

    /**
     * The picker's day columns and slots: a first line {@code columns=<n>}, then one line per day that
     * renders any slot, {@code <date> free=<starts> taken=<starts>} in date order. A free slot is a
     * button, a taken one carries {@code aria-disabled}; a day with no slot at all has no line.
     */
    private static final String SLOT_DAYS = """
            var picker = document.querySelector('[data-slot=slot-picker]');
            if (!picker) return '';
            var days = {};
            picker.querySelectorAll('[data-slot=slot-picker-cell]').forEach(function (cell) {
              var date = cell.getAttribute('data-date');
              var day = days[date] = days[date] || { free: [], taken: [] };
              (cell.getAttribute('aria-disabled') === 'true' ? day.taken : day.free).push(cell.getAttribute('data-start'));
            });
            var lines = ['columns=' + picker.querySelectorAll('[data-slot=slot-picker-header]').length];
            Object.keys(days).sort().forEach(function (date) {
              lines.push(date + ' free=' + days[date].free.join(',') + ' taken=' + days[date].taken.join(','));
            });
            return lines.join('\\n');
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Test
    void every_day_without_a_booking_offers_the_schedule() {
        generateAndPublish();

        // Authenticate the browser session first - a bare openPath lands on the sign-in form.
        ide.openHomePage();

        // Nothing booked: the page passes an empty slots array.
        browser.openPath(APP + "/Appointment");
        String summary = poll(current -> showsSchedule(current, null));
        assertTrue(showsSchedule(summary, null),
                "with nothing booked every visible day must offer the whole schedule, the picker shows:\n" + summary);

        // One booking on the first visible day. The instant is UTC, as the page reads it back.
        String bookedDate = slotDays(summary).keySet()
                                             .iterator()
                                             .next();
        book(bookedDate + "T10:00:00Z");
        Selenide.refresh();
        summary = poll(current -> showsSchedule(current, bookedDate));
        assertTrue(showsSchedule(summary, bookedDate),
                "a booking on " + bookedDate + " must take its own slot and leave every other day bookable, the picker shows:\n" + summary);
    }

    /**
     * Every day column renders slots, the {@code bookedDate} day (when given) with its 10:00 slot taken
     * and every other day with the whole schedule free.
     */
    private static boolean showsSchedule(String summary, String bookedDate) {
        Map<String, String> days = slotDays(summary);
        if (days.isEmpty() || days.size() != columns(summary) || (bookedDate != null && !days.containsKey(bookedDate))) {
            return false;
        }
        return days.entrySet()
                   .stream()
                   .allMatch(day -> (day.getKey()
                                        .equals(bookedDate) ? BOOKED_DAY : FREE_DAY).equals(day.getValue()));
    }

    private static int columns(String summary) {
        String first = summary.lines()
                              .findFirst()
                              .orElse("");
        return first.startsWith("columns=") ? Integer.parseInt(first.substring("columns=".length())) : 0;
    }

    private static Map<String, String> slotDays(String summary) {
        Map<String, String> days = new LinkedHashMap<>();
        summary.lines()
               .skip(1)
               .forEach(line -> {
                   int separator = line.indexOf(' ');
                   days.put(line.substring(0, separator), line.substring(separator + 1));
               });
        return days;
    }

    private String poll(Predicate<String> settled) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
        String value = "";
        do {
            value = Selenide.executeJavaScript(SLOT_DAYS);
            if (value != null && settled.test(value)) {
                return value;
            }
            Selenide.sleep(POLL_INTERVAL_MILLIS);
        } while (System.currentTimeMillis() < deadline);
        return value == null ? "" : value;
    }

    private void book(String startsAt) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"StartsAt\":\"" + startsAt + "\",\"Title\":\"Booked\"}")
                                                 .when()
                                                 .post(API)
                                                 .then()
                                                 .statusCode(200));
    }

    private void generateAndPublish() {
        String path = PROJECT_PATH + "/app.intent";
        if (repository.hasResource(path)) {
            repository.getResource(path)
                      .setContent(INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, INTENT_YAML.getBytes(StandardCharsets.UTF_8));
        }
        AtomicReference<List<Map<String, Object>>> plan = new AtomicReference<>();
        restAssuredExecutor.execute(() -> plan.set(given().when()
                                                          .post(GENERATE_URL)
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .jsonPath()
                                                          .getList("codeGenerations")));
        for (Map<String, Object> codeGeneration : plan.get()) {
            assertEquals(Boolean.TRUE, codeGeneration.get("generated"),
                    "generating code from " + codeGeneration.get("path") + " failed: " + codeGeneration.get("error"));
        }
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT + "/")
                                                 .then()
                                                 .statusCode(200));
        synchronizationProcessor.forceProcessSynchronizers();
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
