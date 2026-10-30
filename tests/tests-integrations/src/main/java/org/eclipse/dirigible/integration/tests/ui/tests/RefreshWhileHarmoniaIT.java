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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;

import io.restassured.http.ContentType;

/**
 * A page re-reads itself while a record is in a moving status (#8375): with {@code refresh: {
 * while: [BOOKED, IN_TRANSIT], every: PT5S }}, a list showing an IN_TRANSIT shipment picks up the
 * DELIVERED status the server set, with no Refresh press; a list whose rows are all settled sends
 * nothing; and a record being edited is never re-read under the user.
 */
class RefreshWhileHarmoniaIT extends UserInterfaceIntegrationTest {

    private static final long POLL_INTERVAL_MILLIS = 250;

    /** The declared interval plus the 2 s the issue allows for the request and the render. */
    private static final long WITHIN_MILLIS = 5_000 + 2_000;

    private static final String PROJECT = "refreshwhileit";
    private static final String WORKSPACE = "workspace";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String GENERATE_URL =
            "/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT + "&path=app.intent";
    private static final String APP = "/services/web/" + PROJECT + "/gen/shipping/index.html#";
    private static final String SHIPMENTS = "/services/java/" + PROJECT + "/gen/shipping/api/shipment/ShipmentController";

    private static final String INTENT_YAML = """
            name: shipping
            entities:
              - name: ShipmentStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }
              - name: Shipment
                refresh:
                  while: [BOOKED, IN_TRANSIT]
                  every: PT5S
                fields:
                  - { name: id,        type: integer, primaryKey: true, generated: true }
                  - { name: reference, type: string }
                relations:
                  - { name: status, kind: manyToOne, to: ShipmentStatus, function: EntityStatus }
            seeds:
              - name: shipment-statuses
                entity: ShipmentStatus
                rows:
                  - { id: 1, name: BOOKED }
                  - { id: 2, name: IN_TRANSIT }
                  - { id: 3, name: DELIVERED, stage: live }
                  - { id: 4, name: RETURNED,  stage: cancelled }
            """;

    /** The Shipments list once a read has settled: each row's reference and status label, sorted. */
    private static final String LIST_STATE = """
            var root = document.querySelector('[x-data="ShipmentManageListPage"]');
            if (!root || !window.Alpine) return 'not-rendered';
            var page = Alpine.$data(root);
            if (page.state === 'loading' || page.filtering || !page.lookups.Status) return 'pending';
            return page.items.map(i => i.Reference + ':' + page.lookupText('Status', i.Status)).sort().join(',');
            """;

    /** How many times the page read the shipments - the list, or one record - since resetReads(). */
    private static final String SHIPMENT_READS = "return performance.getEntriesByType('resource').map(e => e.name)"
            + ".filter(n => /\\/ShipmentController(\\/\\d+)?(\\?|$)/.test(n)).length;";

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @BeforeEach
    void generateAndPublish() {
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
        deleteAllShipments();
    }

    @Test
    void a_moving_row_updates_without_refresh() {
        int id = createShipment("S-1", 2);
        ide.openHomePage();
        browser.openPath(APP + "/Shipment");
        assertEquals("S-1:IN_TRANSIT", poll(state -> state.startsWith("S-"), 60_000), "the list shows the shipment in transit");

        setStatus(id, "S-1", 3);
        long changed = System.currentTimeMillis();
        String state = poll(s -> s.equals("S-1:DELIVERED"), WITHIN_MILLIS);
        assertEquals("S-1:DELIVERED", state, "the list must pick up the server's change without a Refresh press");
        assertTrue(System.currentTimeMillis() - changed <= WITHIN_MILLIS + 1_000, "within the declared interval");
    }

    @Test
    void a_settled_list_does_not_poll() {
        createShipment("S-2", 3);
        ide.openHomePage();
        browser.openPath(APP + "/Shipment");
        assertEquals("S-2:DELIVERED", poll(state -> state.startsWith("S-"), 60_000));
        Selenide.sleep(1_000);
        resetReads();
        Selenide.sleep(2 * 5_000 + 2_000);
        assertEquals(0, reads(), "a list whose rows are all settled sends no request");
    }

    @Test
    void an_edit_in_progress_is_not_overwritten() {
        int id = createShipment("S-3", 2);
        ide.openHomePage();
        browser.openPath(APP + "/Shipment/" + id + "/edit");
        Selenide.$("#f_Reference")
                .shouldBe(Condition.visible)
                .shouldHave(Condition.value("S-3"))
                .setValue("typed by the user");
        resetReads();
        // The server moves the record meanwhile - it stays in a moving status, so a preview would poll.
        setStatus(id, "S-3", 1);
        Selenide.sleep(2 * 5_000 + 2_000);
        assertEquals("typed by the user", Selenide.$("#f_Reference")
                                                  .getValue(),
                "the edit in progress must keep what the user typed");
        assertEquals(0, reads(), "a record being edited is never re-read");
    }

    /** Forgets the requests so far - the browser's buffer of them is bounded, and the SPA fills it. */
    private static void resetReads() {
        Selenide.executeJavaScript("performance.clearResourceTimings();");
    }

    private static long reads() {
        return ((Number) Selenide.executeJavaScript(SHIPMENT_READS)).longValue();
    }

    private String poll(Predicate<String> settled, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        String value = "";
        do {
            value = Selenide.executeJavaScript(LIST_STATE);
            if (value != null && !"pending".equals(value) && !"not-rendered".equals(value) && settled.test(value)) {
                return value;
            }
            Selenide.sleep(POLL_INTERVAL_MILLIS);
        } while (System.currentTimeMillis() < deadline);
        return value;
    }

    private int createShipment(String reference, int status) {
        AtomicReference<Integer> id = new AtomicReference<>();
        restAssuredExecutor.execute(() -> id.set(given().contentType(ContentType.JSON)
                                                        .body(Map.of("Reference", reference, "Status", status))
                                                        .when()
                                                        .post(SHIPMENTS)
                                                        .then()
                                                        .statusCode(200)
                                                        .extract()
                                                        .jsonPath()
                                                        .getInt("Id")));
        return id.get();
    }

    private void setStatus(int id, String reference, int status) {
        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body(Map.of("Id", id, "Reference", reference, "Status", status))
                                                 .when()
                                                 .put(SHIPMENTS + "/" + id)
                                                 .then()
                                                 .statusCode(200));
    }

    private void deleteAllShipments() {
        AtomicReference<List<Integer>> ids = new AtomicReference<>();
        restAssuredExecutor.execute(() -> ids.set(given().when()
                                                         .get(SHIPMENTS)
                                                         .then()
                                                         .statusCode(200)
                                                         .extract()
                                                         .jsonPath()
                                                         .getList("Id", Integer.class)));
        for (Integer id : ids.get()) {
            restAssuredExecutor.execute(() -> given().when()
                                                     .delete(SHIPMENTS + "/" + id)
                                                     .then()
                                                     .statusCode(greaterThanOrEqualTo(200)));
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
