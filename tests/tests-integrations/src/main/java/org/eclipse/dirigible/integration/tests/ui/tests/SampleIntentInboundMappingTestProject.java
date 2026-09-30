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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import java.util.Collections;

import org.eclipse.dirigible.components.api.messaging.MessagingFacade;
import org.eclipse.dirigible.tests.base.BaseIntentTestProject;
import org.eclipse.dirigible.tests.base.ProjectUtil;
import org.eclipse.dirigible.tests.framework.ide.EdmView;
import org.eclipse.dirigible.tests.framework.ide.IDE;
import org.eclipse.dirigible.tests.framework.ide.IntentEditorView;
import org.eclipse.dirigible.tests.framework.logging.LogsAsserter;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import ch.qos.logback.classic.Level;

/**
 * The {@code sample-intent-inbound-mapping} fixture project (dirigible #6769), configured exactly
 * as a developer would in the browser IDE and verified over REST and over the broker. The fixture
 * doubles as the manual-testing sample (see its README), so this project is what keeps the sample
 * from silently rotting - it asserts the three outcomes mapping on arrival was built to produce, on
 * both a webhook and a queue:
 *
 * <ul>
 * <li>a matching envelope is INGESTED with its business keys resolved to the seeded foreign keys -
 * {@code tenantId: "acme"} becomes the Tenant id and {@code role: "User"} the AssignmentRole id,
 * which is the requirement that forced a hand-written consumer before this construct;
 * <li>an envelope this application does not understand ({@code version: 2}) is ACKNOWLEDGED AND
 * IGNORED with a warning - 202 on the webhook, a warning and nothing stored on the queue - so a
 * sender rolling out a new version cannot fill the receiver's error path;
 * <li>an envelope naming a register row that does not exist is REJECTED - 400 on the webhook, an
 * error log on the queue - rather than stored with a null relation.
 * </ul>
 *
 * <p>
 * It also asserts arrival collections (dirigible #7593): an order arriving with an array of lines
 * and an array of tags becomes one PurchaseOrder plus one child row per element, written in ONE
 * transaction - so the CountLines process its create event starts counts every line - and a single
 * bad element rejects the whole order, leaving no row at all.
 */
@Lazy
@Component
class SampleIntentInboundMappingTestProject extends BaseIntentTestProject {

    private static final String PROJECT = "sample-intent-inbound-mapping";
    private static final String ASSIGNMENT_API =
            "/services/java/" + PROJECT + "/gen/assignments/api/tenantuserassignment/TenantUserAssignmentController";
    private static final String WEBHOOK = "/services/java/" + PROJECT + "/gen/events/assignments/AssignmentHookWebhook/assignments";
    private static final String QUEUE = "codbex.user-assignment-requests";

    /** The generated queue consumer's own logger - where an ignored or rejected arrival is reported. */
    private static final String CONSUMER_LOGGER = "app.gen.events.assignments.AssignmentRequestsConsumer";

    private static final String ORDER_API = "/services/java/" + PROJECT + "/gen/assignments/api/purchaseorder/PurchaseOrderController";
    private static final String LINE_API = "/services/java/" + PROJECT + "/gen/assignments/api/purchaseorder/PurchaseOrderLineController";
    private static final String TAG_API = "/services/java/" + PROJECT + "/gen/assignments/api/purchaseorder/PurchaseOrderTagController";
    private static final String ORDER_WEBHOOK = "/services/java/" + PROJECT + "/gen/events/assignments/OrderHookWebhook/orders";
    private static final String ORDER_QUEUE = "sample.orders";
    private static final String ORDER_CONSUMER_LOGGER = "app.gen.events.assignments.OrdersConsumer";

    /** Two lines naming both seeded products by sku: Product 1 is sku A, Product 2 is sku B. */
    private static final String TWO_LINES = "[{\"sku\":\"A\",\"qty\":2},{\"sku\":\"B\",\"qty\":1}]";
    private static final String TWO_TAGS = "[\"rush\",\"gift\"]";
    /** A good first line and a second one naming a product that does not exist. */
    private static final String UNKNOWN_SKU_LINES = "[{\"sku\":\"A\",\"qty\":1},{\"sku\":\"NOPE\",\"qty\":1}]";

    /** Seed ids of the two registers the arriving envelope references by name. */
    private static final int TENANT_ACME = 1;
    private static final int ROLE_USER = 1;

    private final RestAssuredExecutor restAssuredExecutor;

    SampleIntentInboundMappingTestProject(IDE ide, ProjectUtil projectUtil, EdmView edmView, IntentEditorView intentEditorView,
            RestAssuredExecutor restAssuredExecutor) {
        super(PROJECT, ide, projectUtil, edmView, intentEditorView);
        this.restAssuredExecutor = restAssuredExecutor;
    }

    @Override
    public void verify() {
        // Attached here, inside the test method: Spring re-initializes logback while it starts the
        // application context, which drops an appender attached any earlier.
        LogsAsserter consumerLogs = new LogsAsserter(CONSUMER_LOGGER, Level.WARN);
        LogsAsserter orderLogs = new LogsAsserter(ORDER_CONSUMER_LOGGER, Level.WARN);

        verifyWebhook();
        verifyQueue(consumerLogs);
        verifyCollections(orderLogs);
    }

    /**
     * The webhook half. It is synchronous, so each outcome is asserted from its own response - and the
     * absence of a record needs no waiting.
     */
    private void verifyWebhook() {
        // Ingested: the envelope's names come back as the seeded ids, and seatCount as Seats.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(envelope("hook-ok", 1, "acme", "User", 3))
                                                 .when()
                                                 .post(WEBHOOK)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("MessageId", equalTo("hook-ok"))
                                                 .body("Tenant", equalTo(TENANT_ACME))
                                                 .body("Role", equalTo(ROLE_USER))
                                                 .body("Seats", equalTo(3)));

        // Not understood: acknowledged and ignored. 202 says "your message was fine, this receiver
        // does not handle it" - there is nothing for the sender to retry.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(envelope("hook-v2", 2, "acme", "User", 1))
                                                 .when()
                                                 .post(WEBHOOK)
                                                 .then()
                                                 .statusCode(202));

        // Unresolvable business key: rejected. Storing the record with a null Tenant would be a row
        // nobody can trace back to the tenant that asked for it.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(envelope("hook-nope", 1, "nope", "User", 1))
                                                 .when()
                                                 .post(WEBHOOK)
                                                 .then()
                                                 .statusCode(400));

        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ASSIGNMENT_API)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("MessageId", hasItem("hook-ok"))
                                                 .body("MessageId", not(hasItem("hook-v2")))
                                                 .body("MessageId", not(hasItem("hook-nope"))));
    }

    /**
     * The queue half. The two refused messages are published FIRST and the accepted one last, so the
     * arrival of the accepted record proves the refused ones were already consumed - one consumer, one
     * queue, in order - and their absence is a fact rather than a race.
     */
    private void verifyQueue(LogsAsserter consumerLogs) {
        MessagingFacade.sendToQueue(QUEUE, envelope("queue-v2", 2, "acme", "User", 1));
        MessagingFacade.sendToQueue(QUEUE, envelope("queue-nope", 1, "acme", "Nonexistent", 1));
        MessagingFacade.sendToQueue(QUEUE, envelope("queue-ok", 1, "globex", "Administrator", 7));

        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ASSIGNMENT_API)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("MessageId", hasItem("queue-ok"))
                                                 .body("MessageId", not(hasItem("queue-v2")))
                                                 .body("MessageId", not(hasItem("queue-nope"))),
                120);

        // Both refusals are observable, which is the whole point of ignoring rather than failing: a
        // silent drop and a working receiver look identical from the outside.
        consumerLogs.assertLoggedMessage("does not match accept", Level.WARN);
        consumerLogs.assertLoggedMessage("no unique AssignmentRole matches", Level.ERROR);
    }

    private static String envelope(String messageId, int version, String tenantId, String role, int seats) {
        return "{\"messageId\":\"" + messageId + "\",\"type\":\"user.assignment.requested\",\"version\":" + version + ",\"tenantId\":\""
                + tenantId + "\",\"email\":\"" + messageId + "@example.com\",\"role\":\"" + role + "\",\"seatCount\":" + seats + "}";
    }

    /**
     * Arrival collections, over the broker and the webhook. The refused orders are published first and
     * the accepted ones last, as in {@link #verifyQueue(LogsAsserter)}, so their absence is a fact
     * rather than a race.
     */
    private void verifyCollections(LogsAsserter orderLogs) {
        MessagingFacade.sendToQueue(ORDER_QUEUE, order("PO-BAD", UNKNOWN_SKU_LINES, TWO_TAGS));
        MessagingFacade.sendToQueue(ORDER_QUEUE, order("PO-MAX", sixLines(), null));
        MessagingFacade.sendToQueue(ORDER_QUEUE, order("PO-NULLTAG", "[{\"sku\":\"A\",\"qty\":1}]", "[\"x\",null]"));
        MessagingFacade.sendToQueue(ORDER_QUEUE, order("PO-EMPTY", "[]", null));
        MessagingFacade.sendToQueue(ORDER_QUEUE, order("PO-1", TWO_LINES, TWO_TAGS));

        // LinesSeen is written by the process the order's create event started. 2 proves the lines
        // were committed before that event was dispatched; a process started inside the transaction,
        // or by the order's save alone, would have counted 0.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ORDER_API)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("find { it.OrderNumber == 'PO-1' }.LinesSeen", equalTo(2))
                                                 .body("find { it.OrderNumber == 'PO-EMPTY' }.LinesSeen", equalTo(0))
                                                 .body("OrderNumber", not(hasItem("PO-BAD")))
                                                 .body("OrderNumber", not(hasItem("PO-MAX")))
                                                 .body("OrderNumber", not(hasItem("PO-NULLTAG"))),
                120);

        // One arrival, one parent, one process: the stamp column carries exactly one CountLines entry.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ORDER_API)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("find { it.OrderNumber == 'PO-1' }.ProcessId", notNullValue())
                                                 .body("find { it.OrderNumber == 'PO-1' }.ProcessIds", matchesPattern("CountLines=[^,]+")));
        int queued = orderId("PO-1");
        assertLines(queued);
        assertTags(queued);

        // Each refusal names what was wrong with which element - the log is the queue's only answer.
        orderLogs.assertLoggedMessage("element [1] of [lines]: no unique Product matches Sku [NOPE]", Level.ERROR);
        orderLogs.assertLoggedMessage("[lines] holds [6] elements, more than the declared max 5", Level.ERROR);
        orderLogs.assertLoggedMessage("element [1] of [tags] is not a value", Level.ERROR);

        // The webhook: the same order is answered with the saved record, a bad element with 400.
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(order("PO-HOOK-BAD", UNKNOWN_SKU_LINES, TWO_TAGS))
                                                 .when()
                                                 .post(ORDER_WEBHOOK)
                                                 .then()
                                                 .statusCode(400)
                                                 .body("error", containsString("element 1 of [lines]")));
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body(order("PO-HOOK", TWO_LINES, TWO_TAGS))
                                                 .when()
                                                 .post(ORDER_WEBHOOK)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("OrderNumber", equalTo("PO-HOOK")));
        int posted = orderId("PO-HOOK");
        assertLines(posted);
        assertTags(posted);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(ORDER_API)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("find { it.OrderNumber == 'PO-HOOK' }.LinesSeen", equalTo(2))
                                                 .body("OrderNumber", not(hasItem("PO-HOOK-BAD"))),
                120);

        // Nothing of a refused order was written anywhere: every child row belongs to the two accepted
        // orders that carried children, and the empty order has none.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(LINE_API)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("$", hasSize(4)));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(TAG_API)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("$", hasSize(4)));
    }

    private int orderId(String orderNumber) {
        return restAssuredExecutor.executeWithResult(() -> given().when()
                                                                  .get(ORDER_API)
                                                                  .then()
                                                                  .statusCode(200)
                                                                  .extract()
                                                                  .path("find { it.OrderNumber == '" + orderNumber + "' }.Id"));
    }

    /** The two lines of {@link #TWO_LINES}, each resolved from its sku to the seeded Product id. */
    private void assertLines(int order) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(LINE_API + "?PurchaseOrder=" + order)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("$", hasSize(2))
                                                 .body("Quantity", containsInAnyOrder(2, 1))
                                                 .body("Product", containsInAnyOrder(1, 2)));
    }

    /** The two tags of {@link #TWO_TAGS}: an array of bare strings, one row per element. */
    private void assertTags(int order) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(TAG_API + "?PurchaseOrder=" + order)
                                                 .then()
                                                 .statusCode(200)
                                                 .body("$", hasSize(2))
                                                 .body("Label", containsInAnyOrder("rush", "gift")));
    }

    private static String sixLines() {
        return "[" + String.join(",", Collections.nCopies(6, "{\"sku\":\"A\",\"qty\":1}")) + "]";
    }

    private static String order(String orderNumber, String lines, String tags) {
        return "{\"orderNumber\":\"" + orderNumber + "\",\"lines\":" + lines + (tags == null ? "" : ",\"tags\":" + tags) + "}";
    }
}
