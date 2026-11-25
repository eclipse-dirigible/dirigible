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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.path.json.config.JsonPathConfig.NumberReturnType;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.callable.CallableNoResultAndNoException;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.eclipse.dirigible.tests.framework.security.SecurityUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A capture whose record is a line of an existing document (#8626): a photographed receipt becomes
 * one line of an expense claim that is already open. The capture's {@code into:} is a composition
 * child, the upload names the claim ({@code ?Claim=<id>}, as the generated attachment upload does),
 * and the generated capture is compiled and run against a local provider. The claim is checked on
 * the server before the document is read: a line is created under the claim named and nowhere else;
 * a claim that does not exist is a 404, and a claim its {@code immutableWhen} locks is refused with
 * the 409 the line's own controller answers - in both cases the provider is not called and nothing
 * is written, no line, no file and no ledger row.
 *
 * <p>
 * The claim has a {@code personal:} owner, so its owner captures from the personal surface too: a
 * real user holding no role at all uploads to the owner-scoped {@code /my/upload} under their own
 * claim, is refused the role-gated upload, gets the 404 of a write to someone else's claim and the
 * lock's 409 on their own submitted one, and reviews only their own lines.
 */
class IntentCaptureUnderMasterIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "claimcapture";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api/claim";
    private static final String CLAIMS = API + "/ClaimController";
    private static final String LINES = API + "/ClaimItemController";
    private static final String RECEIPTS = API + "/ClaimReceiptController";
    private static final String EMPLOYEES = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api/employee/EmployeeController";
    private static final String CAPTURE = "/services/java/" + PROJECT + "/gen/events/" + PROJECT + "/ReceiptPhotoCapture";
    private static final String LEDGER = "CLAIMCAPTURE_CLAIM_ITEM_CAPTURES";
    private static final String LOCKED =
            "The Claim of this ClaimItem is immutable in its current status - corrections go through the workflow";
    private static final long TIMEOUT_SECONDS = 90;
    private static final String PASSWORD = "claim-capture-it-password";
    private static final String OWNER = "claim-owner";
    private static final String COLLEAGUE = "claim-colleague";

    private static final String INTENT_YAML = """
            name: claimcapture
            description: a receipt photographed into a line of an open expense claim

            entities:
              - name: Employee
                identity: email
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: email, type: string,  length: 100 }
              - name: ClaimStatus
                fields:
                  - { name: id,   type: integer, primaryKey: true, generated: true }
                  - { name: name, type: string,  length: 20 }
              - name: Claim
                immutableWhen: "Status == SUBMITTED"
                fields:
                  - { name: id,          type: integer, primaryKey: true, generated: true }
                  - { name: description, type: string,  length: 100 }
                  - { name: amount,      type: decimal, precision: 18, scale: 2, aggregate: true }
                relations:
                  - { name: Status, kind: manyToOne, to: ClaimStatus, function: EntityStatus, init: 1 }
                  - { name: Employee, kind: manyToOne, to: Employee, personal: true }
              - name: ClaimItem
                fields:
                  - { name: id,      type: integer, primaryKey: true, generated: true }
                  - { name: spentOn, type: date }
                  - { name: amount,  type: decimal, precision: 18, scale: 2, required: true }
                relations:
                  - { name: Claim, kind: manyToOne, to: Claim, composition: true, required: true }
              - name: ClaimReceipt
                function: Attachment
                relations:
                  - { name: ClaimItem, kind: manyToOne, to: ClaimItem, composition: true, required: true }

            seeds:
              - name: claim-statuses
                entity: ClaimStatus
                rows:
                  - { id: 1, name: DRAFT }
                  - { id: 2, name: SUBMITTED }

            captures:
              - name: receiptPhoto
                into: ClaimItem
                attachment: ClaimReceipt
                fields:
                  - { field: spentOn }
                  - { field: amount }
            """;

    private static final String ANSWER = """
            {"fields":{
              "spentOn":{"value":"2026-11-20","confidence":"high"},
              "amount":{"value":42.50,"confidence":"high"}}}
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    @Autowired
    private SecurityUtil securityUtil;

    private HttpServer provider;
    private final AtomicInteger calls = new AtomicInteger();

    @BeforeEach
    void startProvider() throws IOException {
        provider = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        provider.createContext("/v1/messages", exchange -> {
            exchange.getRequestBody()
                    .readAllBytes();
            calls.incrementAndGet();
            JsonObject reply = new JsonObject();
            reply.addProperty("model", "claude-test-model");
            JsonObject usage = new JsonObject();
            usage.addProperty("input_tokens", 700);
            usage.addProperty("output_tokens", 40);
            reply.add("usage", usage);
            reply.addProperty("stop_reason", "end_turn");
            JsonObject text = new JsonObject();
            text.addProperty("type", "text");
            text.addProperty("text", ANSWER);
            JsonArray content = new JsonArray();
            content.add(text);
            reply.add("content", content);
            byte[] bytes = reply.toString()
                                .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders()
                    .add("content-type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        provider.start();
        Configuration.set("DIRIGIBLE_CAPTURE_API_KEY", "test-key");
        Configuration.set("DIRIGIBLE_CAPTURE_BASE_URL", "http://localhost:" + provider.getAddress()
                                                                                      .getPort());
    }

    @Test
    void a_receipt_becomes_a_line_of_the_claim_the_upload_names_and_only_of_one_that_exists_is_open_and_the_caller_may_write()
            throws SQLException {
        writeProjectFile("app.intent", INTENT_YAML);
        generateProject();
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT + "/")
                                                 .then()
                                                 .statusCode(200));
        synchronizationProcessor.forceProcessSynchronizers();

        int open = create(CLAIMS, "{\"Description\":\"trip to Plovdiv\"}");
        int other = create(CLAIMS, "{\"Description\":\"trip to Varna\"}");
        int submitted = create(CLAIMS, "{\"Description\":\"trip to Sofia\",\"Status\":2}");
        byte[] receipt = "%PDF-1.4 a fiscal receipt".getBytes(StandardCharsets.UTF_8);

        // Under the open claim: one line, filed under that claim, with the file kept on the line.
        // Awaited, as the generated capture may still be loading; every later upload is sent once.
        AtomicReference<Object> lineId = new AtomicReference<>();
        restAssuredExecutor.execute(() -> lineId.set(given().multiPart("file", "receipt.pdf", receipt, "application/pdf")
                                                            .when()
                                                            .post(CAPTURE + "/upload?Claim=" + open)
                                                            .then()
                                                            .statusCode(200)
                                                            .body("status", equalTo("CAPTURED"))
                                                            .extract()
                                                            .path("recordId")),
                TIMEOUT_SECONDS);
        assertEquals(1, calls.get());
        Map<String, Object> line = get(LINES + "/" + lineId.get());
        assertEquals(open, ((Number) line.get("Claim")).intValue(), "the line is filed under the claim the upload named: " + line);
        assertEquals("2026-11-20", line.get("SpentOn"));
        assertDecimal("42.50", line.get("Amount"));
        assertDecimal("42.50", get(CLAIMS + "/" + open).get("Amount"), "the claim's total sums its new line");
        assertEquals(1, list(LINES + "?Claim=" + open).size());
        assertEquals(0, list(LINES + "?Claim=" + other).size(), "no other claim gets the line");
        List<Map<String, Object>> files = list(RECEIPTS + "?ClaimItem=" + lineId.get());
        assertEquals(1, files.size(), "the receipt is kept on its line: " + files);
        assertEquals("receipt.pdf", files.get(0)
                                         .get("FileName"));
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(CAPTURE + "/review/" + lineId.get())
                                                 .then()
                                                 .statusCode(200)
                                                 .body("captureName", equalTo("receiptPhoto"))
                                                 .body("flags.size()", equalTo(0)));

        // A claim that does not exist, a locked claim and no claim at all are refused before the
        // document is read: nothing is sent to the provider and nothing is written.
        upload(null, "/upload?Claim=" + (submitted + 1000), receipt, 404, "Claim not found");
        upload(null, "/upload?Claim=" + submitted, receipt, 409, LOCKED);
        upload(null, "/upload", receipt, 400, null);
        assertEquals(1, calls.get(), "a refused master costs no reading");
        assertEquals(0, list(LINES + "?Claim=" + submitted).size(), "a locked claim gets no line");
        assertEquals(1, list(LINES).size());
        assertEquals(1, list(RECEIPTS).size(), "no file is kept for a refused capture");
        assertEquals(List.of("receiptPhoto CAPTURED receipt.pdf"), ledger(), "a refused master is no attempt at reading");

        // The capture button sits on the claim's record, and the page is told which key names it.
        JsonObject action = JsonParser.parseString(new String(repository.getResource(PROJECT_PATH + "/receiptPhoto-capture-action.json")
                                                                        .getContent(),
                StandardCharsets.UTF_8))
                                      .getAsJsonObject();
        assertEquals("Claim", action.get("view")
                                    .getAsString());
        assertEquals("entity", action.get("type")
                                     .getAsString());
        assertTrue(action.get("path")
                         .getAsString()
                         .endsWith("&under=Claim"),
                action.toString());

        assertTheOwnerCapturesOntoTheirOwnClaimFromThePersonalSurface(receipt, lineId.get());
    }

    /**
     * The personal surface: users holding no role, each an employee owning claims. The owner's upload
     * is proven theirs by the line's personal controller; the role-gated upload stays closed to them.
     */
    private void assertTheOwnerCapturesOntoTheirOwnClaimFromThePersonalSurface(byte[] receipt, Object adminsLine) throws SQLException {
        securityUtil.ensureUserInDefaultTenant(OWNER, PASSWORD);
        securityUtil.ensureUserInDefaultTenant(COLLEAGUE, PASSWORD);
        int owner = create(EMPLOYEES, "{\"Email\":\"" + OWNER + "\"}");
        int colleague = create(EMPLOYEES, "{\"Email\":\"" + COLLEAGUE + "\"}");
        int mine = create(CLAIMS, "{\"Description\":\"my trip\",\"Employee\":" + owner + "}");
        int theirs = create(CLAIMS, "{\"Description\":\"their trip\",\"Employee\":" + colleague + "}");
        int mineSubmitted = create(CLAIMS, "{\"Description\":\"my last trip\",\"Employee\":" + owner + ",\"Status\":2}");

        AtomicReference<Object> myLine = new AtomicReference<>();
        restAssuredExecutor.execute(() -> myLine.set(given().multiPart("file", "mine.pdf", receipt, "application/pdf")
                                                            .when()
                                                            .post(CAPTURE + "/my/upload?Claim=" + mine)
                                                            .then()
                                                            .statusCode(200)
                                                            .body("status", equalTo("CAPTURED"))
                                                            .extract()
                                                            .path("recordId")),
                OWNER, PASSWORD);
        assertEquals(mine, ((Number) get(LINES + "/" + myLine.get()).get("Claim")).intValue(),
                "the owner's line is filed under their claim");
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(CAPTURE + "/my/review/" + myLine.get())
                                                 .then()
                                                 .statusCode(200)
                                                 .body("captureName", equalTo("receiptPhoto")),
                OWNER, PASSWORD);

        upload(OWNER, "/my/upload?Claim=" + theirs, receipt, 404, "Claim not found");
        upload(OWNER, "/my/upload?Claim=" + mineSubmitted, receipt, 409, LOCKED);
        upload(OWNER, "/upload?Claim=" + mine, receipt, 403, null);
        review(OWNER, adminsLine, 404);
        review(COLLEAGUE, myLine.get(), 404);

        assertEquals(2, calls.get(), "only the owner's own open claim was read for");
        assertEquals(0, list(LINES + "?Claim=" + theirs).size(), "another user's claim gets no line");
        assertEquals(0, list(LINES + "?Claim=" + mineSubmitted).size(), "a locked claim gets no line");
        assertEquals(2, list(LINES).size());
        assertEquals(2, list(RECEIPTS).size());
        assertEquals(List.of("receiptPhoto CAPTURED receipt.pdf", "receiptPhoto CAPTURED mine.pdf"), ledger());

        // The owner's button sits on the personal pages of the claim and talks to the /my endpoints.
        JsonObject action = JsonParser.parseString(new String(repository.getResource(PROJECT_PATH + "/receiptPhoto-capture-my-action.json")
                                                                        .getContent(),
                StandardCharsets.UTF_8))
                                      .getAsJsonObject();
        assertEquals("my", action.get("surface")
                                 .getAsString());
        assertEquals("Claim", action.get("view")
                                    .getAsString());
        assertEquals("entity", action.get("type")
                                     .getAsString());
        String path = action.get("path")
                            .getAsString();
        assertTrue(path.contains("ReceiptPhotoCapture%2Fmy&") && path.endsWith("&under=Claim"), path);
    }

    private void review(String user, Object line, int status) {
        restAssuredExecutor.execute(() -> given().when()
                                                 .get(CAPTURE + "/my/review/" + line)
                                                 .then()
                                                 .statusCode(status),
                user, PASSWORD);
    }

    /** One refused upload, sent once - as admin when {@code user} is null. */
    private void upload(String user, String endpoint, byte[] receipt, int status, String message) {
        CallableNoResultAndNoException call = () -> {
            var response = given().multiPart("file", "refused.pdf", receipt, "application/pdf")
                                  .when()
                                  .post(CAPTURE + endpoint)
                                  .then()
                                  .statusCode(status);
            if (message != null) {
                response.body("message", equalTo(message));
            }
        };
        if (user == null) {
            restAssuredExecutor.execute(call);
        } else {
            restAssuredExecutor.execute(call, user, PASSWORD);
        }
    }

    private List<String> ledger() throws SQLException {
        List<String> entries = new ArrayList<>();
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT * FROM \"" + LEDGER + "\" ORDER BY \"Id\"")) {
            while (result.next()) {
                entries.add(result.getString("CaptureName") + " " + result.getString("Status") + " " + result.getString("FileName"));
            }
        }
        return entries;
    }

    private static void assertDecimal(String expected, Object actual) {
        assertDecimal(expected, actual, "");
    }

    private static void assertDecimal(String expected, Object actual, String message) {
        assertEquals(0, new BigDecimal(expected).compareTo(new BigDecimal(String.valueOf(actual))), message + " - got " + actual);
    }

    private Map<String, Object> get(String url) {
        AtomicReference<Map<String, Object>> row = new AtomicReference<>();
        restAssuredExecutor.execute(() -> row.set(given().when()
                                                         .get(url)
                                                         .then()
                                                         .statusCode(200)
                                                         .extract()
                                                         .jsonPath()
                                                         .using(JsonPathConfig.jsonPathConfig()
                                                                              .numberReturnType(NumberReturnType.BIG_DECIMAL))
                                                         .getMap("$")));
        return row.get();
    }

    private List<Map<String, Object>> list(String url) {
        AtomicReference<List<Map<String, Object>>> rows = new AtomicReference<>();
        restAssuredExecutor.execute(() -> rows.set(given().when()
                                                          .get(url)
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .jsonPath()
                                                          .using(JsonPathConfig.jsonPathConfig()
                                                                               .numberReturnType(NumberReturnType.BIG_DECIMAL))
                                                          .getList("$")));
        return rows.get();
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
        assertTrue(repository.hasResource(PROJECT_PATH + "/gen/events/" + PROJECT + "/ReceiptPhotoCapture.java"),
                "the capture class is generated");
    }

    private void writeProjectFile(String fileName, String content) {
        String path = PROJECT_PATH + "/" + fileName;
        IResource existing = repository.getResource(path);
        if (existing.exists()) {
            existing.setContent(content.getBytes(StandardCharsets.UTF_8));
        } else {
            repository.createResource(path, content.getBytes(StandardCharsets.UTF_8));
        }
    }

    @AfterEach
    void cleanup() {
        if (provider != null) {
            provider.stop(0);
        }
        Configuration.remove("DIRIGIBLE_CAPTURE_API_KEY");
        Configuration.remove("DIRIGIBLE_CAPTURE_BASE_URL");
        restAssuredExecutor.execute(() -> given().when()
                                                 .delete("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT)
                                                 .then()
                                                 .statusCode(greaterThanOrEqualTo(200)));
        if (repository.hasCollection(PROJECT_PATH)) {
            repository.removeCollection(PROJECT_PATH);
        }
    }
}
