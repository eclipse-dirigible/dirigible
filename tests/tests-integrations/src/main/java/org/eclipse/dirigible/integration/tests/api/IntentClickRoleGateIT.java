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
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.restassured.path.json.JsonPath;

import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.security.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * The generated click endpoints are gated like the controllers beside them (#7913), and so is the
 * print feeder (#7921).
 *
 * <p>
 * A create-from ({@code <Name>Generate/run}) changes its source through {@code sourceStatus:} and
 * creates its target, and a transition ({@code <Name>Transition/run}) flips its record's status -
 * yet both were served to any signed-in user, a user holding no role at all included, while the
 * same user was refused by the two entities' own REST controllers. A create-from now demands the
 * write gate of its source AND of its target, a transition the write gate of its record, and a
 * refused caller is answered 403 before any record is read - an id that does not exist is refused
 * the same way, so the endpoint cannot be used to probe which ids exist.
 *
 * <p>
 * The print feeder ({@code <Name>PrintFeeder/{id}}) reads a whole document, and was likewise served
 * to any signed-in user. It now demands the read gate of the document - whoever its controller lets
 * read it - refused with 403 before the record is loaded, and answers 404 for an id that does not
 * exist rather than 200 {@code {}}.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentClickRoleGateIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "rolegate";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String QUOTES = API + "/quote/QuoteController";
    private static final String ORDERS = API + "/salesorder/SalesOrderController";
    private static final String EVENTS = "/services/java/" + PROJECT + "/gen/events/" + PROJECT;
    private static final String GENERATE = EVENTS + "/OrderFromQuoteGenerate/run";
    private static final String TRANSITION = EVENTS + "/AcceptQuoteTransition/run";
    private static final String PRINT = EVENTS + "/QuotePrintFeeder/";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String PASSWORD = "role-gate-it-password";
    /** Signed in, holding no role at all. */
    private static final String NOBODY = "role-gate-it-nobody";
    /** May write quotes, not orders. */
    private static final String CLERK = "role-gate-it-clerk";
    /** May write both. */
    private static final String SALES = "role-gate-it-sales";
    /** May read quotes only. */
    private static final String VIEWER = "role-gate-it-viewer";
    /** May read orders only. */
    private static final String SHIPPER = "role-gate-it-shipper";
    /** May write quotes, as {@link #CLERK} - its own user, see {@link #deploy()}. */
    private static final String WRITER = "role-gate-it-print-writer";

    private static final int OPEN = 1;
    private static final int ORDERED = 2;
    private static final int ACCEPTED = 3;

    private static final String INTENT_YAML = """
            name: rolegate
            description: the click endpoints demand the write gates of what they write

            entities:
              - name: QuoteStatus
                function: Setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string }

              - name: Quote
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 40 }
                relations:
                  - { name: Status, kind: manyToOne, to: QuoteStatus, function: EntityStatus, init: 1 }

              - name: QuoteItem
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 40 }
                relations:
                  - { name: Quote, kind: manyToOne, to: Quote, composition: true, required: true }

              - name: SalesOrder
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, length: 40 }

            generates:
              - name: order-from-quote
                from: Quote
                to: SalesOrder
                label: Order
                map: { name: name }
                fromStatus: [1]
                sourceStatus: 2

            transitions:
              - { name: AcceptQuote, forEntity: Quote, from: [1], setStatus: 3, label: Accept }

            permissions:
              - { role: GateClerk, can: [Quote:write] }
              - { role: GateSales, can: [Quote:write, SalesOrder:write] }
              - { role: GateViewer, can: [Quote:read] }
              - { role: GateShipper, can: [SalesOrder:read] }

            seeds:
              - name: quote-statuses
                entity: QuoteStatus
                rows:
                  - { id: 1, name: OPEN }
                  - { id: 2, name: ORDERED }
                  - { id: 3, name: ACCEPTED }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private SecurityUtil securityUtil;

    @Test
    void a_click_demands_the_write_gates_of_what_it_writes() {
        deploy();
        securityUtil.ensureUserInDefaultTenant(NOBODY, PASSWORD);
        securityUtil.ensureUserInDefaultTenant(CLERK, PASSWORD, "GateClerk");
        securityUtil.ensureUserInDefaultTenant(SALES, PASSWORD, "GateSales");

        int quote = create(QUOTES, "{\"Name\":\"q-1\"}");

        // No role at all: refused by both clicks - and for an id that does not exist alike, so the
        // refusal says nothing about which records exist.
        post(NOBODY, GENERATE, quote, 403);
        post(NOBODY, GENERATE, 987654, 403);
        post(NOBODY, TRANSITION, quote, 403);
        post(NOBODY, TRANSITION, 987654, 403);

        // The quote's writer may not create the order the click would create.
        post(CLERK, GENERATE, quote, 403);
        assertEquals(0, count(ORDERS), "a refused create-from must create nothing");
        assertEquals(OPEN, status(quote), "a refused create-from must not move its source");

        // Both write gates: the order is created and the quote moves on.
        post(SALES, GENERATE, quote, 200);
        assertEquals(1, count(ORDERS));
        assertEquals(ORDERED, status(quote));

        // The quote's write gate is all a transition of a quote demands.
        int other = create(QUOTES, "{\"Name\":\"q-2\"}");
        post(CLERK, TRANSITION, other, 200);
        assertEquals(ACCEPTED, status(other));
    }

    @Test
    void a_print_demands_the_read_gate_of_its_document() {
        deploy();
        securityUtil.ensureUserInDefaultTenant(NOBODY, PASSWORD);
        securityUtil.ensureUserInDefaultTenant(VIEWER, PASSWORD, "GateViewer");
        securityUtil.ensureUserInDefaultTenant(SHIPPER, PASSWORD, "GateShipper");
        securityUtil.ensureUserInDefaultTenant(WRITER, PASSWORD, "GateClerk");

        int quote = create(QUOTES, "{\"Name\":\"q-print\"}");

        // No role at all, or a role that reads only another entity: refused before the quote is loaded
        // - and for an id that does not exist alike.
        print(NOBODY, quote, 403);
        print(NOBODY, 987654, 403);
        print(SHIPPER, quote, 403);

        // The quote's read gate prints it. Its status is printed too, though the viewer holds no gate of
        // the status entity: reading the document reads what it prints.
        restAssuredExecutor.execute(() -> {
            JsonPath payload = JsonPath.from(given().when()
                                                    .get(PRINT + quote)
                                                    .then()
                                                    .statusCode(200)
                                                    .extract()
                                                    .asString());
            assertEquals("q-print", payload.getString("document.Name"));
            assertEquals("OPEN", payload.getString("document.Status.Name"));
        }, VIEWER, PASSWORD, TIMEOUT_SECONDS);
        // The controller admits a writer on a read, and so does the print.
        print(WRITER, quote, 200);

        // A caller who may read is told that an id does not exist.
        print(VIEWER, 987654, 404);
    }

    /**
     * Generate and publish the intent. The authored roles exist once the published {@code .roles} is
     * synchronized, so each test creates its users after this - and only users no other test created:
     * the cleanup's unpublish deletes the roles and their assignments with them, so a user created by
     * an earlier test would sign in holding none.
     */
    private void deploy() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();
    }

    private void print(String user, int id, int expectedStatus) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(PRINT + id)
                                                 .then()
                                                 .statusCode(expectedStatus),
                user, PASSWORD, TIMEOUT_SECONDS);
    }

    private void post(String user, String endpoint, int id, int expectedStatus) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + id + "}")
                                                 .when()
                                                 .post(endpoint)
                                                 .then()
                                                 .statusCode(expectedStatus),
                user, PASSWORD, TIMEOUT_SECONDS);
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

    private int status(int quote) {
        AtomicInteger status = new AtomicInteger();
        restAssuredExecutor.execute(() -> status.set(given().when()
                                                            .get(QUOTES + "/" + quote)
                                                            .then()
                                                            .statusCode(200)
                                                            .body("Id", equalTo(quote))
                                                            .extract()
                                                            .path("Status")));
        return status.get();
    }

    private int count(String controller) {
        AtomicInteger count = new AtomicInteger();
        restAssuredExecutor.execute(() -> count.set(given().when()
                                                           .get(controller + "/count")
                                                           .then()
                                                           .statusCode(200)
                                                           .extract()
                                                           .jsonPath()
                                                           .getInt("count")));
        return count.get();
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
