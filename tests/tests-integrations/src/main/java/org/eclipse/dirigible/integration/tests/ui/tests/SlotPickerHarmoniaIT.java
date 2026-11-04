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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WrapsDriver;
import org.openqa.selenium.chromium.HasCdp;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverConditions;
import com.codeborne.selenide.WebDriverRunner;

import io.restassured.specification.RequestSpecification;

/**
 * The generated slots page ({@code view: slots}) offers its open/close/step schedule on every day
 * no booking occupies, before anything is booked as well as after. The page hands Harmonia's slot
 * picker the booked days as explicit {@code slots}; a day missing from them shows the schedule only
 * with {@code fillEmptyDays}, and since Harmonia 3.6.0 (codbex/harmonia#144) an empty {@code slots}
 * array is an empty schedule rather than no configuration at all. Without the option a booking
 * entity offered no free slot until something was booked, and afterwards only on the booked days.
 *
 * <p>
 * A booked slot stays clickable and opens its booking (#7837). Harmonia renders such a slot as a
 * plain button rather than an {@code aria-disabled} one, so the page marks it {@code data-booking}
 * (#7919).
 *
 * <p>
 * The window is read in the browser's zone on both sides (#8097): the picker draws it there, and
 * the page names that zone on every write, so a browser far from the server's zone still books the
 * slots it is offered.
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
     * renders any slot, {@code <date> free=<starts> taken=<starts>} in date order. A taken slot carries
     * {@code aria-disabled} or, when it is a booking that opens on click, {@code data-booking}; every
     * other slot is free. A day with no slot at all has no line.
     */
    private static final String SLOT_DAYS = """
            var picker = document.querySelector('[data-slot=slot-picker]');
            if (!picker) return '';
            var days = {};
            picker.querySelectorAll('[data-slot=slot-picker-cell]').forEach(function (cell) {
              var date = cell.getAttribute('data-date');
              var day = days[date] = days[date] || { free: [], taken: [] };
              var taken = cell.getAttribute('aria-disabled') === 'true' || cell.hasAttribute('data-booking');
              (taken ? day.taken : day.free).push(cell.getAttribute('data-start'));
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

        // One booking at 10:00 on the first visible day. The page places a booking in the browser's
        // local time, and the browser runs on this machine, so the instant is 10:00 in its zone.
        String bookedDate = slotDays(summary).keySet()
                                             .iterator()
                                             .next();
        int bookingId = book(LocalDate.parse(bookedDate)
                                      .atTime(10, 0)
                                      .atZone(ZoneId.systemDefault())
                                      .toInstant()
                                      .toString());
        Selenide.refresh();
        summary = poll(current -> showsSchedule(current, bookedDate));
        assertTrue(showsSchedule(summary, bookedDate),
                "a booking on " + bookedDate + " must take its own slot and leave every other day bookable, the picker shows:\n" + summary);

        // The taken slot is the booking's way in: a click opens it.
        Selenide.$("[data-slot=slot-picker-cell][data-booking='" + bookingId + "']")
                .click();
        Selenide.webdriver()
                .shouldHave(WebDriverConditions.urlContaining("/Appointment/" + bookingId + "/edit"), Duration.ofSeconds(30));
    }

    /**
     * A deployment's server runs on UTC while its users sit in Europe/Sofia: the picker offers 09:00 in
     * the browser's zone, and the server must read the write in that zone too, or the window's first
     * slot is refused as before opening (#8097). The browser here is moved fourteen hours east of the
     * server, picks the first slot of a day and creates the booking on the form the slot opens. A REST
     * write before opening is still refused - in the server's zone, and in the zone it names.
     */
    @Test
    void a_slot_picked_in_another_zone_than_the_servers_is_booked() {
        generateAndPublish();
        ide.openHomePage();

        ZoneId server = ZoneId.systemDefault();
        String zone = server.getRules()
                            .getOffset(Instant.now())
                            .getTotalSeconds() == 14 * 3600 ? "America/Los_Angeles" : "Pacific/Kiritimati";
        overrideBrowserZone(zone);
        try {
            browser.openPath(APP + "/Appointment");
            SelenideElement firstSlot = Selenide.$("[data-slot=slot-picker-cell][data-start='09:00']:not([data-booking])")
                                                .shouldBe(Condition.visible, Duration.ofSeconds(60));
            String date = firstSlot.getAttribute("data-date");
            firstSlot.click();
            Selenide.webdriver()
                    .shouldHave(WebDriverConditions.urlContaining("/Appointment/create"), Duration.ofSeconds(30));
            Selenide.$$("button[data-variant=primary]")
                    .findBy(Condition.text("Create"))
                    .click();

            Instant nine = LocalDate.parse(date)
                                    .atTime(9, 0)
                                    .atZone(ZoneId.of(zone))
                                    .toInstant();
            int booking = awaitBookingAt(nine);
            restAssuredExecutor.execute(() -> given().when()
                                                     .delete(API + "/" + booking)
                                                     .then()
                                                     .statusCode(lessThan(300)));
        } finally {
            overrideBrowserZone("");
        }

        LocalDate day = LocalDate.now(server)
                                 .plusDays(7);
        refusedBeforeOpening(day.atTime(8, 0)
                                .atZone(server)
                                .toInstant(),
                null);
        refusedBeforeOpening(day.atTime(8, 0)
                                .atZone(ZoneId.of(zone))
                                .toInstant(),
                zone);
    }

    /** CDP's zone override for the browser's tab; an empty zone restores the machine's. */
    private static void overrideBrowserZone(String zone) {
        WebDriver driver = WebDriverRunner.getWebDriver();
        while (driver instanceof WrapsDriver wrapper) {
            driver = wrapper.getWrappedDriver();
        }
        ((HasCdp) driver).executeCdpCommand("Emulation.setTimezoneOverride", Map.of("timezoneId", zone));
    }

    /** The id of the one booking that starts at {@code at}, once the form's create has stored it. */
    private int awaitBookingAt(Instant at) {
        String body = "{\"conditions\":[{\"propertyName\":\"StartsAt\",\"operator\":\"GTE\",\"value\":\"" + at
                + "\"},{\"propertyName\":\"StartsAt\",\"operator\":\"LT\",\"value\":\"" + at.plusSeconds(60) + "\"}]}";
        AtomicReference<Integer> id = new AtomicReference<>();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body(body)
                                                        .when()
                                                        .post(API + "/search")
                                                        .then()
                                                        .statusCode(200)
                                                        .body("Id", hasSize(1))
                                                        .extract()
                                                        .path("Id[0]")),
                60);
        return id.get();
    }

    private void refusedBeforeOpening(Instant at, String zone) {
        restAssuredExecutor.execute(() -> {
            RequestSpecification request = given().contentType("application/json");
            if (zone != null) {
                request.header("X-Time-Zone", zone);
            }
            request.body("{\"StartsAt\":\"" + at + "\"}")
                   .when()
                   .post(API)
                   .then()
                   .statusCode(400)
                   .body("message", equalTo("Appointment slots run from 09:00 to 12:00"));
        });
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

    private int book(String startsAt) {
        AtomicReference<Integer> id = new AtomicReference<>();
        restAssuredExecutor.execute(() -> id.set(given().contentType("application/json")
                                                        .body("{\"StartsAt\":\"" + startsAt + "\",\"Title\":\"Booked\"}")
                                                        .when()
                                                        .post(API)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")));
        return id.get();
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
