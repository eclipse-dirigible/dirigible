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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
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
 * #8367: the record page shows each value as stored - the same text the list cell shows.
 *
 * <p>
 * The page used to print its Details rail from the input buffer, whose dates are cut to the shape a
 * date input takes (hours and minutes), so a timestamp stored with seconds read {@code 13:49:00};
 * and it formatted every number with the instance Number pattern, so an integer read {@code 21.00}.
 * Both are rendering in the browser, so this needs one.
 */
class RecordValueDisplayHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final String PROJECT = "recordvalueit";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/recordvalue/index.html#";
    private static final String SHIPMENTS = "/services/java/" + PROJECT + "/gen/recordvalue/api/shipment/ShipmentController";

    private static final String INTENT_YAML = """
            name: recordvalue
            entities:
              - name: Shipment
                fields:
                  - { name: id,         type: integer, primaryKey: true, generated: true }
                  - { name: reference,  type: string, length: 40 }
                  - { name: dispatched, type: timestamp, readOnly: true }
                  - { name: attempts,   type: integer, readOnly: true }
            """;

    /** The text of a Details rail row, by its label - {@code pending} until the record has loaded. */
    private static final String RAIL_VALUE = """
            var root = document.querySelector('[x-data="ShipmentFormPage"]');
            if (!root || !window.Alpine || !Alpine.$data(root).loadedRecord) return 'pending';
            var label = Array.from(root.querySelectorAll('span.text-xs.text-muted-foreground'))
                .find(function (s) { return s.textContent.trim() === '%s'; });
            return label ? label.nextElementSibling.textContent.trim() : 'no-row';
            """;

    /**
     * The text of the first list row's cell under a column header - {@code pending} until it renders.
     */
    private static final String LIST_CELL = """
            var heads = Array.from(document.querySelectorAll('table thead th')).map(function (th) { return th.textContent.trim(); });
            var row = document.querySelector('table tbody tr');
            var index = heads.indexOf('%s');
            if (index < 0 || !row || !row.children[index]) return 'pending';
            return row.children[index].textContent.trim();
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
    void the_record_page_and_the_list_show_a_stored_timestamp_and_integer_alike() {
        generateAndPublish();
        int id = createShipment("SHP-1");
        // readOnly fields are system-owned: a REST create drops them (#7549), so the stored values are
        // written the way a job would write them.
        storeReadOnlyValues(id, Timestamp.valueOf("2026-10-30 13:49:11"), 21);
        ide.openHomePage();

        browser.openPath(APP + "/Shipment/" + id + "/preview");
        String railDispatched = pollUntil(RAIL_VALUE.formatted("Dispatched"), v -> v.endsWith("49:11"));
        assertTrue(railDispatched.endsWith("49:11"), "the rail must show the stored seconds, not :00 - was " + railDispatched);
        assertEquals("21", pollUntil(RAIL_VALUE.formatted("Attempts"), "21"::equals), "an integer must gain no decimals on the rail");

        browser.openPath(APP + "/Shipment");
        assertEquals(railDispatched, pollUntil(LIST_CELL.formatted("Dispatched"), railDispatched::equals),
                "the list cell and the record page must print the same timestamp");
        assertEquals("21", pollUntil(LIST_CELL.formatted("Attempts"), "21"::equals),
                "the list cell and the record page must print the same integer");
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

    private void storeReadOnlyValues(int id, Timestamp dispatched, int attempts) {
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                PreparedStatement update = connection.prepareStatement(
                        "UPDATE \"RECORDVALUE_SHIPMENT\" SET \"SHIPMENT_DISPATCHED\" = ?, \"SHIPMENT_ATTEMPTS\" = ? WHERE \"SHIPMENT_ID\" = ?")) {
            update.setTimestamp(1, dispatched);
            update.setInt(2, attempts);
            update.setInt(3, id);
            assertEquals(1, update.executeUpdate(), "the seeded shipment must exist");
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to store the read-only values", ex);
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
