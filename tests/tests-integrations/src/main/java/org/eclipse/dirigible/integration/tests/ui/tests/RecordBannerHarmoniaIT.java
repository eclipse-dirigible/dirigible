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
 * #8371: a record page says when the record or its latest related row needs attention, with the
 * action that fixes it. An order whose latest shipment failed shows the negative banner with the
 * failure and Retry; Retry runs, the banner leaves without a reload and the register shows the
 * shipment PENDING; an order whose latest shipment was delivered shows none.
 */
class RecordBannerHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_TIMEOUT_MILLIS = 60_000;

    private static final long POLL_INTERVAL_MILLIS = 250;

    private static final String PROJECT = "recordbannerit";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/recordbanner/index.html#";
    private static final String API = "/services/java/" + PROJECT + "/gen/recordbanner/api";

    private static final String INTENT_YAML = """
            name: recordbanner
            seeds:
              - name: order-statuses
                entity: OrderStatus
                rows:
                  - { id: 1, name: IN_PROGRESS }
                  - { id: 2, name: ON_HOLD }
              - name: shipment-statuses
                entity: ShipmentStatus
                rows:
                  - { id: 1, name: PENDING }
                  - { id: 2, name: DELIVERED }
                  - { id: 3, name: FAILED }
            entities:
              - name: OrderStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 50 }
              - name: ShipmentStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 50 }
              - name: Order
                label: "{number}"
                related:
                  - { entity: Shipment }
                banners:
                  - text: "The last shipment failed: {failure}"
                    variant: negative
                    related: { entity: Shipment, row: latest, when: "status == FAILED" }
                    action: RetryShipment
                  - text: "This order is on hold."
                    variant: warning
                    when: "status == ON_HOLD"
                fields:
                  - { name: id,     type: integer, primaryKey: true, generated: true }
                  - { name: number, type: string, length: 20 }
                relations:
                  - { name: status, kind: manyToOne, to: OrderStatus, function: EntityStatus }
              - name: Shipment
                fields:
                  - { name: id,         type: integer, primaryKey: true, generated: true }
                  - { name: failure,    type: string, length: 500, readOnly: true }
                  - { name: finishedAt, type: timestamp, readOnly: true }
                relations:
                  - { name: order,  kind: manyToOne, to: Order }
                  - { name: status, kind: manyToOne, to: ShipmentStatus, function: EntityStatus }
            transitions:
              - { name: RetryShipment, forEntity: Shipment, from: [3], setStatus: 1, label: Retry }
            """;

    /**
     * The banners on screen as {@code <variant>:<text>:<button>} joined by {@code |}, {@code none}, or
     * {@code pending} until the record and its register have loaded.
     */
    private static final String BANNERS = """
            var root = document.querySelector('[x-data="OrderFormPage"]');
            if (!root || !window.Alpine || !Alpine.$data(root).loadedRecord) return 'pending';
            var panel = root.querySelector('[x-data^="relatedPanel"]');
            if (!panel || Alpine.$data(panel).state === 'loading') return 'pending';
            var shown = function (e) { return e && e.offsetParent !== null; };
            var alerts = Array.from(root.querySelectorAll('[data-record-banners] [x-h-alert]')).filter(shown);
            return alerts.map(function (a) {
                var button = a.querySelector('[x-h-alert-actions] button');
                return a.getAttribute('data-variant') + ':' + a.querySelector('[x-h-alert-description]').textContent.trim() + ':'
                    + (shown(button) ? button.textContent.trim() : '');
            }).join('|') || 'none';
            """;

    /** The register's rows as their texts, for the status a retried shipment shows. */
    private static final String REGISTER = """
            var panel = document.querySelector('[x-data^="relatedPanel"]');
            return panel ? panel.querySelector('tbody').textContent.replace(/\\s+/g, ' ').trim() : 'no-panel';
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
    void the_latest_failed_shipment_shows_a_banner_with_retry_until_it_is_retried() {
        generateAndPublish();
        int failing = create("/order/OrderController", Map.of("Number", "2026-001"));
        shipment(failing, 2, null);
        shipment(failing, 3, "Carrier lost the parcel");
        int delivered = create("/order/OrderController", Map.of("Number", "2026-002"));
        shipment(delivered, 3, "Address not found");
        shipment(delivered, 2, null);
        ide.openHomePage();

        browser.openPath(APP + "/Order/" + failing + "/preview");
        assertEquals("negative:The last shipment failed: Carrier lost the parcel:Retry",
                pollUntil(BANNERS, "negative:The last shipment failed: Carrier lost the parcel:Retry"::equals),
                "an order whose latest shipment failed shows the failure and the action that fixes it");

        Selenide.$("[data-record-banners] [x-h-alert-actions] button")
                .click();
        Selenide.executeJavaScript("return Alpine.store('customActions').confirmRun();");
        assertEquals("none", pollUntil(BANNERS, "none"::equals), "once retried, the banner leaves without a reload");
        String register = pollUntil(REGISTER, v -> v.contains("PENDING"));
        assertTrue(register.contains("PENDING"), "the register shows the retried shipment PENDING: " + register);

        browser.openPath(APP + "/Order/" + delivered + "/preview");
        assertEquals("none", pollUntil(BANNERS, v -> !"pending".equals(v)),
                "an order whose latest shipment was delivered shows no banner - an older failure is not the news");
    }

    private int create(String controller, Map<String, Object> body) {
        AtomicReference<Integer> id = new AtomicReference<>();
        restAssuredExecutor.execute(() -> id.set(given().contentType(ContentType.JSON)
                                                        .body(body)
                                                        .when()
                                                        .post(API + controller)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .path("Id")));
        return id.get();
    }

    /** A shipment of the order, its status and failure written as the carrier integration would. */
    private void shipment(int order, int status, String failure) {
        int id = create("/shipment/ShipmentController", Map.of("Order", order));
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                PreparedStatement update = connection.prepareStatement(
                        "UPDATE \"RECORDBANNER_SHIPMENT\" SET \"SHIPMENT_STATUS\" = ?, \"SHIPMENT_FAILURE\" = ? WHERE \"SHIPMENT_ID\" = ?")) {
            update.setInt(1, status);
            update.setString(2, failure);
            update.setInt(3, id);
            assertEquals(1, update.executeUpdate(), "the shipment must exist");
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to store the shipment's status", ex);
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
