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

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.UserInterfaceIntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Selenide;

import io.restassured.http.ContentType;

/**
 * #8369: {@code placement:} says where a field sits on the record page. A long read-only note
 * placed {@code main} renders full width in the main column with its line breaks, not in the narrow
 * Details rail; a technical column placed {@code hidden} is on no generated page, while the REST
 * answer still carries it.
 */
class FieldPlacementHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final String PROJECT = "placementit";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/placement/index.html#";
    private static final String SHIPMENTS = "/services/java/" + PROJECT + "/gen/placement/api/shipment/ShipmentController";

    private static final String INTENT_YAML = """
            name: placement
            entities:
              - name: Shipment
                fields:
                  - { name: id,          type: integer, primaryKey: true, generated: true }
                  - { name: reference,   type: string, length: 40 }
                  - { name: dispatched,  type: timestamp, readOnly: true }
                  - { name: carrierNote, type: text, readOnly: true, placement: main }
                  - { name: syncToken,   type: string, length: 64, readOnly: true, placement: hidden }
            """;

    private static final String TOKEN = "sync-8369-c0ffee";

    /**
     * The main-column value as {@code <in rail>;<column span class>;<white-space>;<text>}, or
     * {@code pending} until the record has loaded.
     */
    private static final String MAIN_VALUE = """
            var root = document.querySelector('[x-data="ShipmentFormPage"]');
            if (!root || !window.Alpine || !Alpine.$data(root).loadedRecord) return 'pending';
            var cell = root.querySelector('[data-placement="main"]');
            if (!cell) return 'no-cell';
            var value = cell.querySelector('.whitespace-pre-wrap');
            var inRail = Array.from(root.querySelectorAll('span.text-xs.text-muted-foreground'))
                .some(function (s) { return s.textContent.trim() === 'Carrier Note' && !s.closest('[data-placement="main"]'); });
            var span = Array.from(cell.classList).filter(function (c) { return c.indexOf('col-span') >= 0; }).join(' ');
            return inRail + ';' + span + ';' + getComputedStyle(value).whiteSpace + ';' + value.textContent;
            """;

    /** Whether the page's root shows the hidden field anywhere - its value or its label. */
    private static final String SHOWS_HIDDEN = """
            var root = document.querySelector('#app') || document.body;
            var text = root.innerText;
            return text.indexOf('%s') >= 0 || text.indexOf('Sync Token') >= 0 ? 'shown' : 'absent';
            """.formatted(TOKEN);

    /** {@code pending} until the list has rendered its first row, then {@code ready}. */
    private static final String LIST_READY = """
            var row = document.querySelector('table tbody tr td');
            return row ? 'ready' : 'pending';
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Test
    void a_main_note_sits_in_the_main_column_and_a_hidden_token_on_no_page() {
        generateAndPublish();
        int id = createShipment("SHP-8369");
        String note = noteOf(800);
        storeSystemValues(id, note, TOKEN);
        ide.openHomePage();

        browser.openPath(APP + "/Shipment/" + id + "/preview");
        String main = pollUntil(MAIN_VALUE, v -> !"pending".equals(v));
        assertEquals("false;md:col-span-12;pre-wrap;" + note, main,
                "the note must render full width in the main column with its line breaks, and not in the rail");
        assertEquals("absent", Selenide.executeJavaScript(SHOWS_HIDDEN), "the record page must not show the hidden field");

        browser.openPath(APP + "/Shipment");
        assertEquals("ready", pollUntil(LIST_READY, "ready"::equals), "the list must render the shipment");
        assertEquals("absent", Selenide.executeJavaScript(SHOWS_HIDDEN), "the list must not show the hidden field");

        AtomicReference<String> served = new AtomicReference<>();
        restAssuredExecutor.execute(() -> served.set(given().when()
                                                            .get(SHIPMENTS + "/" + id)
                                                            .then()
                                                            .statusCode(200)
                                                            .extract()
                                                            .path("SyncToken")));
        assertEquals(TOKEN, served.get(), "the REST answer must still carry the hidden field");
    }

    /** An 800-character note in lines - the case that wrapped over dozens of lines in the rail. */
    private static String noteOf(int length) {
        StringBuilder note = new StringBuilder();
        int line = 1;
        while (note.length() < length) {
            note.append("Carrier update ")
                .append(line++)
                .append(": parcel scanned at the regional hub and handed over for the last mile.\n");
        }
        return note.substring(0, length)
                   .trim();
    }

    private int createShipment(String reference) {
        AtomicReference<Integer> id = new AtomicReference<>();
        restAssuredExecutor.execute(() -> id.set(given().contentType(ContentType.JSON)
                                                        .body(Map.of("Reference", reference))
                                                        .when()
                                                        .post(SHIPMENTS)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")));
        return id.get();
    }

    /**
     * The read-only values are system-owned - a REST create drops them - so they are written as a job
     * would.
     */
    private void storeSystemValues(int id, String note, String token) {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                PreparedStatement update = connection.prepareStatement(
                        "UPDATE PLACEMENT_SHIPMENT SET SHIPMENT_CARRIER_NOTE = ?, SHIPMENT_SYNC_TOKEN = ? WHERE SHIPMENT_ID = ?")) {
            update.setString(1, note);
            update.setString(2, token);
            update.setInt(3, id);
            assertEquals(1, update.executeUpdate(), "the seeded shipment must exist");
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to store the system-owned values", ex);
        }
    }

    private String pollUntil(String script, Predicate<String> done) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MILLIS;
        String value = "";
        do {
            value = Selenide.executeJavaScript(script);
            if (value != null && done.test(value)) {
                return value;
            }
            Selenide.sleep(POLL_INTERVAL_MILLIS);
        } while (System.currentTimeMillis() < deadline);
        return value;
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
