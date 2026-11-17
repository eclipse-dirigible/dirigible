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
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.security.SecurityUtil;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;

/**
 * Tenant webhook subscriptions end to end (#8689): an intent application exposing two of an
 * entity's events is generated, compiled and published; the test plays the provisioning system (the
 * registry) and the tenant's external system (an in-process HTTP receiver on a loopback address,
 * reachable only through the test switch
 * {@code DIRIGIBLE_TENANT_WEBHOOKS_ALLOW_LOOPBACK_FOR_TESTS}).
 *
 * <p>
 * Walked in one scenario, because each step builds on what the previous one registered: a create
 * and a transition each deliver one signed, thin event; a receiver answering 500 gets the same
 * event id again until it answers 2xx, and the log shows every attempt; a resend delivers the same
 * id once more; a receiver that never answers 2xx turns its subscription {@code FAILING} after the
 * retry window; a rotation signs with both secrets; a second tenant's events reach only its own
 * subscription; a removed subscription is gone; and an internal address or an undeclared event type
 * is refused at registration. The retry schedule and window are shrunk to seconds for the run.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TenantWebhooksIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "webhookit";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String LEAVES = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api/leave/LeaveController";
    private static final String APPROVE = "/services/java/" + PROJECT + "/gen/events/" + PROJECT + "/ApproveLeaveTransition/run";
    private static final String PROVISIONING = "/services/tenant-provisioning/";

    private static final String CREATED = PROJECT + "-Leave-Leave";
    private static final String TRANSITIONED = PROJECT + "-Leave-Leave-transitioned";

    private static final String SECRET = "it-webhook-secret-number-one-0123456789";
    private static final String ROTATED_SECRET = "it-webhook-secret-number-two-0123456789";
    private static final String DEAD_SECRET = "it-webhook-secret-of-the-dead-receiver";
    private static final String OTHER_SECRET = "it-webhook-secret-of-the-other-tenant";

    private static final long TIMEOUT_SECONDS = 90;
    private static final long DELIVERY_TIMEOUT_SECONDS = 30;

    private static final int DRAFT = 1;
    private static final int APPROVED = 2;

    private static final String INTENT = """
            name: webhookit
            description: a leave request whose creation and transitions a tenant's external system subscribes to

            entities:
              - name: LeaveStatus
                kind: setting
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string, required: true, length: 40 }

              - name: Leave
                webhooks: [created, transitioned]
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: note, type: string, length: 100 }
                relations:
                  - { name: Status, kind: manyToOne, to: LeaveStatus, function: EntityStatus, init: 1 }

            transitions:
              - { name: ApproveLeave, forEntity: Leave, from: [1], setStatus: 2, label: Approve }

            seeds:
              - name: leave-statuses
                entity: LeaveStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: APPROVED }
            """;

    /** What the receiver got, in order. */
    private static final List<Received> RECEIVED = new CopyOnWriteArrayList<>();

    /** The statuses a path answers next; 200 once a path's queue is empty. */
    private static final Map<String, Deque<Integer>> SCRIPTED = new ConcurrentHashMap<>();

    private static HttpServer receiver;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    @DefaultTenant
    private Tenant defaultTenant;

    /** One received delivery. */
    private record Received(String path, String id, String timestamp, String signature, String body) {

        JsonPath json() {
            return JsonPath.from(body);
        }
    }

    @BeforeAll
    static void configure() throws IOException {
        DirigibleConfig.TENANT_PROVISIONING_API_ENABLED.setBooleanValue(true);
        DirigibleConfig.TENANT_WEBHOOKS_ALLOW_LOOPBACK_FOR_TESTS.setBooleanValue(true);
        DirigibleConfig.TENANT_WEBHOOKS_ENCRYPTION_KEY.setStringValue(Base64.getEncoder()
                                                                            .encodeToString("0123456789abcdef0123456789abcdef".getBytes(
                                                                                    StandardCharsets.UTF_8)));
        DirigibleConfig.TENANT_WEBHOOKS_RETRY_SCHEDULE_SECONDS.setStringValue("1");
        DirigibleConfig.TENANT_WEBHOOKS_RETRY_WINDOW_SECONDS.setStringValue("8");
        DirigibleConfig.TENANT_WEBHOOKS_RELAY_INTERVAL_SECONDS.setStringValue("1");
        DirigibleConfig.TENANT_WEBHOOKS_TIMEOUT_SECONDS.setStringValue("3");

        receiver = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        receiver.createContext("/", TenantWebhooksIT::receive);
        receiver.start();
    }

    @AfterAll
    static void restore() {
        receiver.stop(0);
        for (DirigibleConfig config : List.of(DirigibleConfig.TENANT_PROVISIONING_API_ENABLED,
                DirigibleConfig.TENANT_WEBHOOKS_ALLOW_LOOPBACK_FOR_TESTS, DirigibleConfig.TENANT_WEBHOOKS_ENCRYPTION_KEY,
                DirigibleConfig.TENANT_WEBHOOKS_RETRY_SCHEDULE_SECONDS, DirigibleConfig.TENANT_WEBHOOKS_RETRY_WINDOW_SECONDS,
                DirigibleConfig.TENANT_WEBHOOKS_RELAY_INTERVAL_SECONDS, DirigibleConfig.TENANT_WEBHOOKS_TIMEOUT_SECONDS)) {
            Configuration.remove(config.getKey());
        }
    }

    private static void receive(HttpExchange exchange) throws IOException {
        String body;
        try (InputStream in = exchange.getRequestBody()) {
            body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String path = exchange.getRequestURI()
                              .getPath();
        RECEIVED.add(new Received(path, exchange.getRequestHeaders()
                                                .getFirst("Webhook-Id"),
                exchange.getRequestHeaders()
                        .getFirst("Webhook-Timestamp"),
                exchange.getRequestHeaders()
                        .getFirst("Webhook-Signature"),
                body));
        int status;
        if ("/dead".equals(path)) {
            status = 503;
        } else {
            Integer next = SCRIPTED.getOrDefault(path, new ArrayDeque<>())
                                   .pollFirst();
            status = next == null ? 200 : next;
        }
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }

    @BeforeEach
    void reset() {
        RECEIVED.clear();
        SCRIPTED.clear();
    }

    @Test
    void aTenantsExternalSystemReceivesSignedThinEventsReliably() {
        writeIntent();
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();
        String tenant = defaultTenant.getId();

        // The application declares exactly the two exposed events - none of the internal channels.
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(PROVISIONING + "webhook-events")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("events.type", hasItems(CREATED, TRANSITIONED))
                                                 .body("events.type", not(hasItems(PROJECT + "-Leave-Leave-updated")))
                                                 .body("events.find { it.type == '" + TRANSITIONED + "' }.action", equalTo("transitioned")),
                TIMEOUT_SECONDS);

        // Refused at registration: an internal address, an undeclared event type, a missing secret.
        putRegistry(tenant, 1, List.of(subscription("s-meta", "https://169.254.169.254/latest", List.of(CREATED), SECRET, null)), 400,
                "resolves to an internal address");
        putRegistry(tenant, 1, List.of(subscription("s-private", "https://10.0.0.1/hook", List.of(CREATED), SECRET, null)), 400,
                "resolves to an internal address");
        putRegistry(tenant, 1, List.of(subscription("s-internal", url("/main"), List.of(PROJECT + "-Leave-Leave-rekeyed"), SECRET, null)),
                400, "which no published application declares");
        putRegistry(tenant, 1, List.of(subscription("s-nosecret", url("/main"), List.of(CREATED), null, null)), 400,
                "secret is required for a new subscription");

        putRegistry(tenant, 1, List.of(subscription("s-main", url("/main"), List.of(CREATED, TRANSITIONED), SECRET, null),
                subscription("s-dead", url("/dead"), List.of(CREATED), DEAD_SECRET, null)), 200, null);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(PROVISIONING + "tenants/" + tenant + "/webhooks")
                                                 .then()
                                                 .statusCode(200)
                                                 .body("revision", equalTo(1))
                                                 .body("subscriptions.find { it.subscriptionId == 's-main' }.status", equalTo("ACTIVE"))
                                                 .body("subscriptions.find { it.subscriptionId == 's-main' }.secret", nullValue())
                                                 .body(not(containsString(SECRET))));

        // 1. A create and a transition each deliver one signed event carrying the id and the status only.
        int first = createLeave("first", null);
        Received created = awaitOne("/main", CREATED, first);
        assertSignedWith(created, SECRET);
        JsonPath body = created.json();
        assertEquals(tenant, body.getString("tenant"));
        assertEquals(DRAFT, body.getInt("data.status"));
        assertEquals(created.id(), body.getString("id"));
        assertFalse(created.body()
                           .contains("first"),
                "the record's content never travels: " + created.body());
        assertEquals(List.of("id", "status"), new ArrayList<>(body.getMap("data")
                                                                  .keySet()));

        restAssuredExecutor.execute(() -> given().contentType(ContentType.JSON)
                                                 .body(Map.of("id", first))
                                                 .when()
                                                 .post(APPROVE)
                                                 .then()
                                                 .statusCode(200),
                TIMEOUT_SECONDS);
        Received transitioned = awaitOne("/main", TRANSITIONED, first);
        assertSignedWith(transitioned, SECRET);
        assertEquals(APPROVED, transitioned.json()
                                           .getInt("data.status"));

        // 2. A receiver answering 500 gets the same event id again until it answers 2xx.
        SCRIPTED.put("/main", new ArrayDeque<>(List.of(500, 500)));
        int second = createLeave("second", null);
        List<Received> attempts = awaitAttempts("/main", CREATED, second, 3);
        assertEquals(1, attempts.stream()
                                .map(Received::id)
                                .distinct()
                                .count(),
                "every attempt carries one event id");
        String secondEvent = attempts.get(0)
                                     .id();
        Awaitility.await()
                  .atMost(DELIVERY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .untilAsserted(() -> restAssuredExecutor.execute(() -> given().when()
                                                                                .get(PROVISIONING + "tenants/" + tenant
                                                                                        + "/webhooks/s-main/deliveries")
                                                                                .then()
                                                                                .statusCode(200)
                                                                                .body("find { it.eventId == '" + secondEvent + "' }.state",
                                                                                        equalTo("DELIVERED"))
                                                                                .body("find { it.eventId == '" + secondEvent
                                                                                        + "' }.attempts.statusCode",
                                                                                        equalTo(List.of(500, 500, 200)))));

        // 3. A resend delivers the same event once more, with the same id.
        int before = count("/main");
        restAssuredExecutor.execute(() -> given().when()
                                                 .post(PROVISIONING + "tenants/" + tenant + "/webhooks/s-main/deliveries/" + created.id()
                                                         + "/resend")
                                                 .then()
                                                 .statusCode(202));
        Awaitility.await()
                  .atMost(DELIVERY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .until(() -> count("/main") > before);
        assertTrue(RECEIVED.stream()
                           .filter(received -> "/main".equals(received.path()))
                           .filter(received -> created.id()
                                                      .equals(received.id()))
                           .count() == 2,
                "the resend carries the original event id");

        // 4. A receiver that never answers 2xx turns its subscription FAILING after the retry window.
        Awaitility.await()
                  .atMost(DELIVERY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .untilAsserted(() -> restAssuredExecutor.execute(() -> given().when()
                                                                                .get(PROVISIONING + "tenants/" + tenant + "/webhooks")
                                                                                .then()
                                                                                .body("subscriptions.find { it.subscriptionId == 's-dead' }.status",
                                                                                        equalTo("FAILING"))));

        // 5. During a rotation both secrets sign; a receiver holding either verifies the delivery. The
        // dead subscription is left out, which removes it.
        putRegistry(tenant, 2, List.of(subscription("s-main", url("/main"), List.of(CREATED, TRANSITIONED), ROTATED_SECRET, SECRET)), 200,
                null);
        int third = createLeave("third", null);
        Received rotated = awaitOne("/main", CREATED, third);
        assertSignedWith(rotated, ROTATED_SECRET);
        assertSignedWith(rotated, SECRET);
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(PROVISIONING + "tenants/" + tenant + "/webhooks")
                                                 .then()
                                                 .body("subscriptions.subscriptionId", equalTo(List.of("s-main"))));

        // 6. A second tenant's events reach its own subscription only, and never the first tenant's.
        DirigibleTestTenant other = new DirigibleTestTenant("webhook-other");
        createTenants(other);
        waitForTenantProvisioning(other);
        securityUtil.assignAllSystemRolesToUser(other.getUsername(), other.getId());
        putRegistry(other.getId(), 1, List.of(subscription("s-other", url("/other"), List.of(CREATED), OTHER_SECRET, null)), 200, null);
        int mainBefore = count("/main");
        int theirs = createLeave("theirs", other);
        Received foreign = awaitOne("/other", CREATED, theirs);
        assertEquals(other.getId(), foreign.json()
                                           .getString("tenant"));
        assertSignedWith(foreign, OTHER_SECRET);
        int ours = createLeave("ours", null);
        awaitOne("/main", CREATED, ours);
        assertEquals(mainBefore + 1, count("/main"), "the other tenant's write reached the first tenant's subscription");
        assertEquals(1, count("/other"), "the first tenant's write reached the other tenant's subscription");
    }

    private int createLeave(String note, DirigibleTestTenant tenant) {
        AtomicInteger id = new AtomicInteger();
        Runnable create = () -> id.set(given().contentType(ContentType.JSON)
                                              .body(Map.of("Note", note))
                                              .when()
                                              .post(LEAVES)
                                              .then()
                                              .statusCode(200)
                                              .extract()
                                              .path("Id"));
        if (tenant == null) {
            restAssuredExecutor.execute(create::run, TIMEOUT_SECONDS);
        } else {
            restAssuredExecutor.execute(tenant, create::run, TIMEOUT_SECONDS);
        }
        return id.get();
    }

    private Received awaitOne(String path, String type, int recordId) {
        return awaitAttempts(path, type, recordId, 1).get(0);
    }

    private List<Received> awaitAttempts(String path, String type, int recordId, int count) {
        AtomicReference<List<Received>> found = new AtomicReference<>();
        Awaitility.await()
                  .atMost(DELIVERY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .until(() -> {
                      List<Received> matching = RECEIVED.stream()
                                                        .filter(received -> path.equals(received.path()))
                                                        .filter(received -> type.equals(received.json()
                                                                                                .getString("type")))
                                                        .filter(received -> recordId == received.json()
                                                                                                .getInt("data.id"))
                                                        .toList();
                      found.set(matching);
                      return matching.size() >= count;
                  });
        return found.get();
    }

    private static int count(String path) {
        return (int) RECEIVED.stream()
                             .filter(received -> path.equals(received.path()))
                             .count();
    }

    private static void assertSignedWith(Received received, String secret) {
        String expected = "v1=" + hmac(secret, received.timestamp() + "." + received.body());
        assertTrue(List.of(received.signature()
                                   .split(","))
                       .contains(expected),
                "signature " + received.signature() + " does not verify with the secret");
    }

    private static String hmac(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of()
                            .formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String url(String path) {
        return "http://127.0.0.1:" + receiver.getAddress()
                                             .getPort()
                + path;
    }

    private static Map<String, Object> subscription(String id, String url, List<String> events, String secret, String previousSecret) {
        Map<String, Object> subscription = new LinkedHashMap<>();
        subscription.put("subscriptionId", id);
        subscription.put("name", "Scheduling " + id);
        subscription.put("url", url);
        subscription.put("events", events);
        if (secret != null) {
            subscription.put("secret", secret);
        }
        if (previousSecret != null) {
            subscription.put("previousSecret", previousSecret);
        }
        subscription.put("createdBy", "owner@acme.test");
        subscription.put("createdAt", "2026-11-17T12:00:00Z");
        return subscription;
    }

    private void putRegistry(String tenantId, long revision, List<Map<String, Object>> subscriptions, int status, String message) {
        restAssuredExecutor.execute(() -> {
            var response = given().contentType(ContentType.JSON)
                                  .body(Map.of("revision", revision, "subscriptions", subscriptions))
                                  .when()
                                  .put(PROVISIONING + "tenants/" + tenantId + "/webhooks")
                                  .then()
                                  .statusCode(status);
            if (message != null) {
                response.body("message", containsString(message));
            }
        });
    }

    private void generateProject() {
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
        assertTrue(repository.getResource(PROJECT_PATH + "/gen/" + PROJECT + "/data/leave/Leave.webhooks.json")
                             .exists(),
                "the descriptor of the exposed entity is generated");
        assertFalse(repository.getResource(PROJECT_PATH + "/gen/" + PROJECT + "/data/leavestatus/LeaveStatus.webhooks.json")
                              .exists(),
                "an entity that exposes nothing gets no descriptor");
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
            existing.setContent(INTENT.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, INTENT.getBytes(StandardCharsets.UTF_8));
        }
    }

    @AfterEach
    void cleanup() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT)
                                                 .then()
                                                 .statusCode(both(greaterThanOrEqualTo(200)).and(lessThan(300))));
        if (repository.hasCollection(PROJECT_PATH)) {
            repository.removeCollection(PROJECT_PATH);
        }
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
