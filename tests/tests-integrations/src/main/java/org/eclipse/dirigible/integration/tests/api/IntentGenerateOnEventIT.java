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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.components.api.messaging.MessagingFacade;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.logging.LogsAsserter;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import ch.qos.logback.classic.Level;
import io.restassured.path.json.JsonPath;

/**
 * The generated create-from ({@code generates:}), run as its two callers run it: the click on
 * {@code <Name>Generate/run}, and the listener of an event-driven rule.
 *
 * <p>
 * The click names its source by id, and a source that is not there is the caller's to hear about
 * (#8488): no id is a 400, an id with no record behind it a 404 naming it - on every create-from,
 * with or without a from-status guard.
 *
 * <p>
 * The listener runs after the transition that fired it committed, so the source may have moved on
 * by the time it runs (#8510). The transition decides whether the follow-up document is generated -
 * read off the event's payload - not the status the source stands in by then; a source withdrawn
 * since is not generated from, the completion hook does not overrule the later move, and a
 * redelivery still generates nothing a second time.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IntentGenerateOnEventIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "genevent";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String SALES = API + "/sale/SaleController";
    private static final String DELIVERIES = API + "/delivery/DeliveryController";
    private static final String MEMOS = API + "/memo/MemoController";
    private static final String EVENTS = "/services/java/" + PROJECT + "/gen/events/" + PROJECT + "/";
    private static final long TIMEOUT_SECONDS = 90;
    /**
     * The event-driven create-from runs off the source's {@code -transitioned} topic, after the commit.
     */
    private static final long LISTENER_TIMEOUT_SECONDS = 30;
    private static final int NO_SUCH_SALE = 987654;

    private static final String INTENT_YAML = """
            name: genevent
            description: create-from fixture - a click naming no source, and a listener that runs late

            entities:
              - name: SaleStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 100 }

              # The source. Issuing it is the moment its delivery is generated on; the completion hook
              # then moves it to Delivering. Sent is where something else may take it before the listener
              # runs - and Sent has no edge to Delivering, so re-flipping a sent sale is refused.
              - name: Sale
                lifecycle:
                  edges:
                    - { from: 1, to: [2, 3, 4] }
                    - { from: 2, to: [3, 4, 5] }
                    - { from: 3, to: [4] }
                    - { from: 5, to: [3] }
                fields:
                  - { name: id,  type: integer, primaryKey: true, generated: true }
                  - { name: ref, type: string, length: 50 }
                relations:
                  - { name: Status, kind: manyToOne, to: SaleStatus, function: EntityStatus, init: 1 }

              # Generated by the event, at most once per sale.
              - name: Delivery
                fields:
                  - { name: id,  type: integer, primaryKey: true, generated: true }
                  - { name: ref, type: string, length: 50 }
                relations:
                  - { name: Sale, kind: manyToOne, to: Sale }

              # Generated by a click - with no from-status guard, and with one.
              - name: Memo
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: text, type: string, length: 50 }
                relations:
                  - { name: Sale, kind: manyToOne, to: Sale }

            transitions:
              - { name: IssueSale, forEntity: Sale, from: [1],       setStatus: 2, label: Issue }
              - { name: SendSale,  forEntity: Sale, from: [1, 2, 5], setStatus: 3, label: Send }
              - { name: VoidSale,  forEntity: Sale, from: [1, 2, 3], setStatus: 4, label: Void }

            generates:
              - name: delivery-from-sale
                from: Sale
                to: Delivery
                event: { onTransition: Sale, when: "Status == 2" }
                sourceStatus: 5
                map:
                  Sale: id
                  ref: ref
              - name: memo-from-sale
                from: Sale
                to: Memo
                map:
                  Sale: id
                  text: ref
              - name: draft-memo-from-sale
                from: Sale
                to: Memo
                fromStatus: [1]
                map:
                  Sale: id
                  text: ref

            seeds:
              # The stages tell the listener that a Voided sale was withdrawn (#8510).
              - name: sale-statuses
                entity: SaleStatus
                rows:
                  - { id: 1, name: Draft,      stage: draft }
                  - { id: 2, name: Issued,     stage: live }
                  - { id: 3, name: Sent,       stage: live }
                  - { id: 4, name: Voided,     stage: void }
                  - { id: 5, name: Delivering, stage: live }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    /**
     * The generated handlers log through the client SDK, which prefixes their names with {@code app.};
     * attached in {@code @BeforeEach} because Spring re-initializes logback while it starts the
     * context.
     */
    private LogsAsserter generateLogs;

    @BeforeEach
    void attachLogsAsserter() {
        generateLogs = new LogsAsserter("app.gen.events", Level.INFO);
    }

    /**
     * #8488: a create-from with no from-status guard answered 200 to a run naming no record, so every
     * client branching on the status - the action button, an integration - read it as created. The
     * missing source is now asked once, up front, on every create-from: both shapes answer the same 400
     * and the same 404, and a valid run still creates.
     */
    @Test
    void every_create_from_refuses_a_run_that_names_no_existing_source() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int memosBefore = count(MEMOS);
        for (String createFrom : List.of("MemoFromSale", "DraftMemoFromSale")) {
            assertRunRefused(createFrom, "{}", 400, "missing source id");
            assertRunRefused(createFrom, "{\"id\":" + NO_SUCH_SALE + "}", 404, "Sale " + NO_SUCH_SALE + " does not exist");
        }
        assertEquals(memosBefore, count(MEMOS), "a refused run must create no memo");

        int sale = create(SALES, "{\"Ref\":\"S-memo\"}");
        int memo = run("MemoFromSale", sale);
        int draftMemo = run("DraftMemoFromSale", sale);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(MEMOS)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("findAll { it.Sale == " + sale + " }.Id.sort()", equalTo(List.of(memo, draftMemo)))
                                                 .body("findAll { it.Sale == " + sale + " }.Text.unique()", equalTo(List.of("S-memo"))));
        // The guarded one still refuses a sale out of its from-status, now that the sale exists.
        transition("SendSale", sale);
        assertRunRefused("DraftMemoFromSale", "{\"id\":" + sale + "}", 409, "current status is [3]");
    }

    /** The event path as it normally runs: issuing the sale generates its delivery and flips it on. */
    @Test
    void an_issued_sale_generates_its_delivery_and_moves_to_delivering() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int sale = create(SALES, "{\"Ref\":\"S-1\"}");
        transition("IssueSale", sale);
        onlyDeliveryOf(sale);
        awaitStatus(sale, 5);
    }

    /**
     * #8510: the listener judged its {@code when:} against the RE-LOADED sale, so a sale something had
     * already moved on before the listener ran was never generated from, and nothing offered it again.
     * The transition decides now, read off the event.
     *
     * <p>
     * The race is staged deterministically: the sale is moved on first, with no Issue event of its own,
     * and the Issue's {@code -transitioned} event - the row as that transition leaves it, in status 2 -
     * is then delivered: exactly what a listener that fell behind receives. The sent sale gets its
     * delivery and stays Sent - the completion hook does not overrule the later move, which its
     * lifecycle would refuse anyway (Sent has no edge to Delivering) and which, made unconditionally,
     * took the delivery back with it. A second delivery of the same event generates nothing more; a
     * voided sale is not generated from; a sale that does not exist is logged.
     */
    @Test
    void the_transition_decides_the_delivery_not_the_status_the_sale_moved_on_to() {
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int sent = create(SALES, "{\"Ref\":\"S-sent\"}");
        transition("SendSale", sent);
        deliverIssued(sent, "S-sent");
        int delivery = onlyDeliveryOf(sent);
        Awaitility.await()
                  .pollInterval(1, TimeUnit.SECONDS)
                  .atMost(LISTENER_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .until(() -> generateLogs.containsMessage("Sale [" + sent + "] moved on to status [3] before the create-from ran",
                          Level.INFO));
        awaitStatus(sent, 3);

        // A redelivery is a no-op. Read once the listener has demonstrably got PAST it: a sale issued
        // afterwards travels the same topic to the same subscriber, so its delivery existing means the
        // redelivery was consumed before it.
        deliverIssued(sent, "S-sent");
        int fence = create(SALES, "{\"Ref\":\"S-fence\"}");
        transition("IssueSale", fence);
        onlyDeliveryOf(fence);
        assertEquals(delivery, onlyDeliveryOf(sent), "a redelivered event must not generate a second delivery");

        int voided = create(SALES, "{\"Ref\":\"S-void\"}");
        transition("VoidSale", voided);
        deliverIssued(voided, "S-void");
        Awaitility.await()
                  .pollInterval(1, TimeUnit.SECONDS)
                  .atMost(LISTENER_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .until(() -> generateLogs.containsMessage(
                          "Sale [" + voided + "] was withdrawn (status [4]) before its create-from ran - not generated", Level.INFO));
        assertNoDelivery(voided);

        // No caller to answer on this path (#8488): a source that is gone is said in the log.
        deliverIssued(NO_SUCH_SALE, "S-gone");
        Awaitility.await()
                  .pollInterval(1, TimeUnit.SECONDS)
                  .atMost(LISTENER_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .until(() -> generateLogs.containsMessage("Sale " + NO_SUCH_SALE + " does not exist - nothing generated", Level.WARN));
    }

    /** Delivers the {@code -transitioned} event of the sale's Issue - the row in status 2. */
    private void deliverIssued(int sale, String ref) {
        MessagingFacade.sendToTopic(PROJECT + "-Sale-Sale-transitioned", "{\"Id\":" + sale + ",\"Ref\":\"" + ref + "\",\"Status\":2}");
    }

    /**
     * The single delivery generated from the sale, asserting there is exactly one. Filtered here: the
     * back-reference is a plain association, which the generated list endpoint does not filter by.
     */
    private int onlyDeliveryOf(int sale) {
        String ofSale = "findAll { it.Sale == " + sale + " }";
        AtomicInteger delivery = new AtomicInteger();
        restAssuredExecutor.execute(() -> delivery.set(given().when()
                                                              .get(DELIVERIES)
                                                              .then()
                                                              .statusCode(200)
                                                              .body(ofSale, hasSize(1))
                                                              .extract()
                                                              .jsonPath()
                                                              .getInt(ofSale + "[0].Id")),
                LISTENER_TIMEOUT_SECONDS);
        return delivery.get();
    }

    private void assertNoDelivery(int sale) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(DELIVERIES)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("findAll { it.Sale == " + sale + " }", hasSize(0)));
    }

    private void awaitStatus(int sale, int status) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(SALES + "/" + sale)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("Status", equalTo(status)),
                LISTENER_TIMEOUT_SECONDS);
    }

    private void assertRunRefused(String createFrom, String body, int status, String message) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(body)
                                                 .when()
                                                 .post(EVENTS + createFrom + "Generate/run")
                                                 .then()
                                                 .statusCode(status)
                                                 .body(containsString(message)),
                TIMEOUT_SECONDS);
    }

    private int run(String createFrom, int sale) {
        AtomicInteger id = new AtomicInteger();
        // The run answers its JSON as text/plain, so the body is parsed explicitly.
        restAssuredExecutor.execute(() -> id.set(JsonPath.from(given().contentType("application/json")
                                                                      .body("{\"id\":" + sale + "}")
                                                                      .when()
                                                                      .post(EVENTS + createFrom + "Generate/run")
                                                                      .then()
                                                                      .statusCode(200)
                                                                      .extract()
                                                                      .asString())
                                                         .getInt("Id")),
                TIMEOUT_SECONDS);
        return id.get();
    }

    private int count(String controller) {
        AtomicInteger count = new AtomicInteger();
        restAssuredExecutor.execute(() -> count.set(given().when()
                                                           .get(controller)
                                                           .then()
                                                           .statusCode(200)
                                                           .extract()
                                                           .jsonPath()
                                                           .getList("")
                                                           .size()),
                TIMEOUT_SECONDS);
        return count.get();
    }

    private void transition(String name, int id) {
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"id\":" + id + "}")
                                                 .when()
                                                 .post(EVENTS + name + "Transition/run")
                                                 .then()
                                                 .statusCode(200),
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
