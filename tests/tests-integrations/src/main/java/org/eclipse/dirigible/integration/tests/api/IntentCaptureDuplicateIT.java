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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.restassured.RestAssuredExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A capture meets the soft checks of its target (#8631): the same supplier bill scanned twice is
 * the duplicate a capture produces most, and the {@code duplicate} check that asks a person saving
 * the form must ask the person uploading the scan as well. The generated capture is compiled and
 * run: the second upload of the same document is held - 428 listing the warning with the existing
 * record, no record created, the attempt in the ledger - until the request confirms the warning's
 * code, and the confirmed record carries the warning as a flag for its review. A different document
 * is not asked.
 */
class IntentCaptureDuplicateIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "billrescan";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String BILLS = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api/bill/BillController";
    private static final String CAPTURE = "/services/java/" + PROJECT + "/gen/events/" + PROJECT + "/BillScanCapture";
    private static final String VENDORS = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api/vendor/VendorController";
    private static final String LEDGER = "BILLRESCAN_BILL_CAPTURES";
    private static final String CODE = "Bill.duplicate.0";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String INTENT_YAML = """
            name: billrescan
            description: the same supplier bill scanned twice

            entities:
              - name: Vendor
                fields:
                  - { name: id,                 type: integer, primaryKey: true, generated: true }
                  - { name: registrationNumber, type: string,  length: 20 }
              - name: Bill
                checks:
                  - { kind: duplicate, fields: [Vendor, vendorNumber],
                      message: "A bill with this vendor number already exists: {match} - continue?" }
                fields:
                  - { name: id,           type: integer, primaryKey: true, generated: true }
                  - { name: vendorNumber, type: string,  length: 50, required: true }
                relations:
                  - { name: Vendor, kind: manyToOne, to: Vendor }

            captures:
              - name: billScan
                into: Bill
                fields:
                  - { field: vendorNumber }
                  - { field: Vendor, match: [registrationNumber] }
            """;

    @Autowired
    private IRepository repository;

    @Autowired
    private RestAssuredExecutor restAssuredExecutor;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private DataSourcesManager dataSourcesManager;

    private HttpServer provider;
    private final AtomicReference<String> vendorNumber = new AtomicReference<>("INV-77");

    @BeforeEach
    void startProvider() throws IOException {
        provider = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        provider.createContext("/v1/messages", exchange -> {
            exchange.getRequestBody()
                    .readAllBytes();
            JsonObject reply = new JsonObject();
            reply.addProperty("model", "claude-test-model");
            JsonObject usage = new JsonObject();
            usage.addProperty("input_tokens", 900);
            usage.addProperty("output_tokens", 60);
            reply.add("usage", usage);
            reply.addProperty("stop_reason", "end_turn");
            JsonObject text = new JsonObject();
            text.addProperty("type", "text");
            text.addProperty("text", "{\"fields\":{\"vendorNumber\":{\"value\":\"" + vendorNumber.get() + "\",\"confidence\":\"high\"},"
                    + "\"Vendor_registrationNumber\":{\"value\":\"111111111\",\"confidence\":\"high\"}}}");
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
    void the_same_bill_scanned_twice_is_held_until_the_duplicate_is_confirmed() throws SQLException {
        writeProjectFile("app.intent", INTENT_YAML);
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/intent/generate?workspace=" + WORKSPACE + "&project=" + PROJECT
                                                         + "&path=app.intent")
                                                 .then()
                                                 .statusCode(200));
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT + "/")
                                                 .then()
                                                 .statusCode(200));
        synchronizationProcessor.forceProcessSynchronizers();
        restAssuredExecutor.execute(() -> given().contentType("application/json")
                                                 .body("{\"RegistrationNumber\":\"111111111\"}")
                                                 .when()
                                                 .post(VENDORS)
                                                 .then()
                                                 .statusCode(200),
                TIMEOUT_SECONDS);
        byte[] scan = "%PDF-1.4 the scanned bill".getBytes(StandardCharsets.UTF_8);

        // The first scan has nothing to repeat. Awaited, as the generated capture may still be loading;
        // every later upload is sent once - a retry would be another capture.
        AtomicReference<Map<String, Object>> firstResult = new AtomicReference<>();
        restAssuredExecutor.execute(() -> firstResult.set(upload(scan, null, 200)), TIMEOUT_SECONDS);
        Map<String, Object> first = firstResult.get();
        assertEquals("CAPTURED", first.get("status"));
        assertEquals(List.of(), first.get("flags"), "nothing to review on the first scan: " + first);
        Object firstId = first.get("recordId");

        // The same paper again: asked, and nothing is created until it is confirmed.
        AtomicReference<Map<String, Object>> held = new AtomicReference<>();
        restAssuredExecutor.execute(() -> held.set(given().multiPart("file", "bill.pdf", scan, "application/pdf")
                                                          .when()
                                                          .post(CAPTURE + "/upload")
                                                          .then()
                                                          .statusCode(428)
                                                          .body("warnings[0].code", equalTo(CODE))
                                                          .body("warnings[0].message",
                                                                  containsString(
                                                                          "A bill with this vendor number already exists: " + firstId))
                                                          .extract()
                                                          .jsonPath()
                                                          .getMap("$")));
        assertEquals(1, bills(), "an unconfirmed duplicate creates no record: " + held.get());

        // Confirmed by its code, the capture goes through - flagged for the review.
        Map<String, Object> confirmed = upload(scan, CODE, 200);
        assertEquals("CAPTURED", confirmed.get("status"));
        assertEquals(2, bills());
        List<String> flags = flags(confirmed);
        assertEquals(List.of("Bill WARNING A bill with this vendor number already exists: " + firstId + " - continue?"), flags,
                "the confirmed duplicate is a flag naming the existing record");
        AtomicReference<Map<String, Object>> review = new AtomicReference<>();
        restAssuredExecutor.execute(() -> review.set(given().when()
                                                            .get(CAPTURE + "/review/" + confirmed.get("recordId"))
                                                            .then()
                                                            .statusCode(200)
                                                            .extract()
                                                            .jsonPath()
                                                            .getMap("$")));
        assertEquals(flags, flags(review.get()), "the review lists it beside the file");

        // A different bill is not asked.
        vendorNumber.set("INV-78");
        Map<String, Object> other = upload(scan, null, 200);
        assertEquals(List.of(), other.get("flags"), "a different document is no duplicate: " + other);
        assertEquals(3, bills());

        assertEquals(List.of("CAPTURED -", "REJECTED " + held.get()
                                                             .get("message"),
                "CAPTURED -", "CAPTURED -"), ledger(), "every attempt is in the ledger, the held one with the warning it waits on");
    }

    private Map<String, Object> upload(byte[] scan, String confirm, int status) {
        AtomicReference<Map<String, Object>> result = new AtomicReference<>();
        restAssuredExecutor.execute(() -> {
            var request = given().multiPart("file", "bill.pdf", scan, "application/pdf");
            if (confirm != null) {
                request = request.header("X-Confirm-Warnings", confirm);
            }
            result.set(request.when()
                              .post(CAPTURE + "/upload")
                              .then()
                              .statusCode(status)
                              .extract()
                              .jsonPath()
                              .getMap("$"));
        });
        return result.get();
    }

    @SuppressWarnings("unchecked")
    private static List<String> flags(Map<String, Object> result) {
        List<String> names = new ArrayList<>();
        for (Map<String, Object> flag : (List<Map<String, Object>>) result.get("flags")) {
            names.add(flag.get("field") + " " + flag.get("reason") + " " + flag.get("message"));
        }
        return names;
    }

    private int bills() {
        AtomicReference<List<Object>> rows = new AtomicReference<>();
        restAssuredExecutor.execute(() -> rows.set(given().when()
                                                          .get(BILLS)
                                                          .then()
                                                          .statusCode(200)
                                                          .extract()
                                                          .jsonPath()
                                                          .getList("$")));
        return rows.get()
                   .size();
    }

    private List<String> ledger() throws SQLException {
        List<String> entries = new ArrayList<>();
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT * FROM \"" + LEDGER + "\" ORDER BY \"Id\"")) {
            while (result.next()) {
                String message = result.getString("Message");
                entries.add(result.getString("Status") + " " + (message == null ? "-" : message));
            }
        }
        return entries;
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
