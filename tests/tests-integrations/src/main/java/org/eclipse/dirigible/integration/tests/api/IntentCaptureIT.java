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
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
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
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.sun.net.httpserver.HttpServer;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.path.json.config.JsonPathConfig.NumberReturnType;
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
 * The {@code captures:} construct end to end (#8544): an intent declaring a capture of supplier
 * bills is generated, compiled and published, and a photographed bill is uploaded to the generated
 * endpoint. The vision-model provider is a local server speaking the Messages API, so the test
 * asserts both what the platform sends - the image, the JSON schema derived from the declared
 * fields, the QR code's parts as known values - and what it makes of the answer.
 *
 * <p>
 * The answer is deliberately imperfect, the way a reading of print is: a date that disagrees with
 * the document's QR code, a printed VAT that differs from the calculated one, a registration number
 * nothing matches next to a VAT number that does, and a second line read with low confidence and no
 * price. The bill must still be created - with the code's date, the calculated VAT, the vendor the
 * VAT number identifies, both lines and the uploaded file - and each of those is a flag in the
 * capture ledger, served by the review endpoint. A provider that declines creates nothing and is
 * recorded as a failed attempt.
 */
class IntentCaptureIT extends IntegrationTest {

    private static final String WORKSPACE = "workspace";
    private static final String PROJECT = "billcapture";
    private static final String PROJECT_PATH = IRepositoryStructure.PATH_USERS + "/admin/" + WORKSPACE + "/" + PROJECT;
    private static final String API = "/services/java/" + PROJECT + "/gen/" + PROJECT + "/api";
    private static final String VENDORS = API + "/vendor/VendorController";
    private static final String BILLS = API + "/bill/BillController";
    private static final String LINES = API + "/bill/BillLineController";
    private static final String SCANS = API + "/bill/BillScanController";
    private static final String CAPTURE = "/services/java/" + PROJECT + "/gen/events/" + PROJECT + "/BillScanCapture";
    private static final String LEDGER = "BILLCAPTURE_BILL_CAPTURES";
    private static final String QR_TEXT = "DT123456*0000042*2026-03-15*10:20:30*120.00";
    private static final long TIMEOUT_SECONDS = 90;

    private static final String INTENT_YAML = """
            name: billcapture
            description: supplier bills captured from a scan

            entities:
              - name: Vendor
                fields:
                  - { name: id,                 type: integer, primaryKey: true, generated: true }
                  - { name: name,               type: string,  length: 100 }
                  - { name: registrationNumber, type: string,  length: 20 }
                  - { name: vatNumber,          type: string,  length: 20 }
              - name: Bill
                fields:
                  - { name: id,           type: integer, primaryKey: true, generated: true }
                  - { name: vendorNumber, type: string,  length: 50 }
                  - { name: billedOn,     type: date }
                  - { name: gross,        type: decimal, precision: 18, scale: 2 }
                  - { name: net,          type: decimal, precision: 18, scale: 2 }
                  - { name: vat,          type: decimal, precision: 18, scale: 2,
                      calculatedOnCreate: "round(Net * 0.2, 2)", calculatedOnUpdate: "round(Net * 0.2, 2)" }
                relations:
                  - { name: Vendor, kind: manyToOne, to: Vendor }
              - name: BillLine
                fields:
                  - { name: id,          type: integer, primaryKey: true, generated: true }
                  - { name: description, type: string,  length: 200 }
                  - { name: quantity,    type: decimal, precision: 18, scale: 3 }
                  - { name: price,       type: decimal, precision: 18, scale: 2 }
                relations:
                  - { name: Bill, kind: manyToOne, to: Bill, composition: true, required: true }
              - name: BillScan
                function: Attachment
                relations:
                  - { name: Bill, kind: manyToOne, to: Bill, composition: true, required: true }

            captures:
              - name: billScan
                into: Bill
                attachment: BillScan
                instructions: "The vendor is the party that issued the document."
                qr: { separator: "*", parts: [device, receipt, date, time, total] }
                fields:
                  - { field: vendorNumber, describe: "the vendor's own number of the document" }
                  - { field: billedOn, qr: date }
                  - { field: gross, qr: total }
                  - { field: net }
                  - { field: vat, compare: true }
                  - { field: Vendor, match: [registrationNumber, vatNumber], describe: "the vendor" }
                items:
                  into: BillLine
                  fields:
                    - { field: description }
                    - { field: quantity }
                    - { field: price }
            """;

    /** What the model "read": imperfect on purpose - see the class comment. */
    private static final String ANSWER = """
            {"fields":{
              "vendorNumber":{"value":"INV-77","confidence":"high"},
              "billedOn":{"value":"2026-03-14","confidence":"high"},
              "gross":{"value":120.0,"confidence":"medium"},
              "net":{"value":100,"confidence":"high"},
              "vat":{"value":21.00,"confidence":"high"},
              "Vendor_registrationNumber":{"value":"999999999","confidence":"high"},
              "Vendor_vatNumber":{"value":"BG123456789","confidence":"high"}},
             "items":[
              {"description":"Хартия А4","quantity":2,"price":30.00,"confidence":"high"},
              {"description":"Тонер","quantity":1,"price":null,"confidence":"low"}]}
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
    private final List<JsonObject> requests = new CopyOnWriteArrayList<>();
    private final AtomicBoolean decline = new AtomicBoolean();

    @BeforeEach
    void startProvider() throws IOException {
        provider = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        provider.createContext("/v1/messages", exchange -> {
            String body = new String(exchange.getRequestBody()
                                             .readAllBytes(),
                    StandardCharsets.UTF_8);
            JsonObject request = JsonParser.parseString(body)
                                           .getAsJsonObject();
            request.addProperty("_apiKey", exchange.getRequestHeaders()
                                                   .getFirst("x-api-key"));
            requests.add(request);
            JsonObject reply = new JsonObject();
            reply.addProperty("model", "claude-test-model");
            JsonObject usage = new JsonObject();
            usage.addProperty("input_tokens", 1800);
            usage.addProperty("output_tokens", 240);
            reply.add("usage", usage);
            com.google.gson.JsonArray content = new com.google.gson.JsonArray();
            if (decline.get()) {
                reply.addProperty("stop_reason", "refusal");
            } else {
                reply.addProperty("stop_reason", "end_turn");
                JsonObject thinking = new JsonObject();
                thinking.addProperty("type", "thinking");
                thinking.addProperty("thinking", "");
                content.add(thinking);
                JsonObject text = new JsonObject();
                text.addProperty("type", "text");
                text.addProperty("text", ANSWER);
                content.add(text);
            }
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
    void an_uploaded_scan_becomes_a_draft_with_its_lines_its_file_and_a_flag_for_everything_to_review() throws SQLException {
        writeProjectFile("app.intent", INTENT_YAML);
        generateProject();
        publishProject();
        synchronizationProcessor.forceProcessSynchronizers();

        int vendor = create(VENDORS, "{\"Name\":\"ACME OOD\",\"RegistrationNumber\":\"111111111\",\"VatNumber\":\"BG123456789\"}");
        byte[] scan = qrImage(QR_TEXT);

        AtomicReference<String> recordId = new AtomicReference<>();
        restAssuredExecutor.execute(() -> recordId.set(given().multiPart("file", "bill.png", scan, "image/png")
                                                              .when()
                                                              .post(CAPTURE + "/upload")
                                                              .then()
                                                              .statusCode(200)
                                                              .body("status", equalTo("CAPTURED"))
                                                              .body("recordId", notNullValue())
                                                              .body("model", equalTo("claude-test-model"))
                                                              .extract()
                                                              .path("recordId")),
                TIMEOUT_SECONDS);

        // What the platform asked the provider.
        assertEquals(1, requests.size(), "one document is one provider call");
        JsonObject request = requests.get(0);
        assertEquals("test-key", request.get("_apiKey")
                                        .getAsString());
        JsonObject image = request.getAsJsonArray("messages")
                                  .get(0)
                                  .getAsJsonObject()
                                  .getAsJsonArray("content")
                                  .get(0)
                                  .getAsJsonObject();
        assertEquals("image", image.get("type")
                                   .getAsString());
        assertEquals("image/png", image.getAsJsonObject("source")
                                       .get("media_type")
                                       .getAsString());
        JsonObject format = request.getAsJsonObject("output_config")
                                   .getAsJsonObject("format");
        assertEquals("json_schema", format.get("type")
                                          .getAsString());
        JsonObject header = format.getAsJsonObject("schema")
                                  .getAsJsonObject("properties")
                                  .getAsJsonObject("fields")
                                  .getAsJsonObject("properties");
        assertEquals(
                new TreeSet<>(List.of("vendorNumber", "billedOn", "gross", "net", "vat", "Vendor_registrationNumber", "Vendor_vatNumber")),
                new TreeSet<>(header.keySet()), "the schema asks for every declared field and one key per relation match");
        String system = request.get("system")
                               .getAsString();
        assertTrue(system.contains("The vendor is the party that issued the document."), "the capture's instructions reach the model");
        assertTrue(system.contains("total: 120.00") && system.contains("date: 2026-03-15"),
                "the QR code's parts reach the model as known values: " + system);
        assertFalse(request.has("temperature") || request.has("tool_choice"), "no parameter the current models reject");

        // The record the answer became.
        Map<String, Object> bill = get(BILLS + "/" + recordId.get());
        assertEquals("INV-77", bill.get("VendorNumber"));
        assertEquals("2026-03-15", bill.get("BilledOn"), "the QR code's date wins over the one read: " + bill);
        assertDecimal("120.00", bill.get("Gross"));
        assertDecimal("100.00", bill.get("Net"));
        assertDecimal("20.00", bill.get("Vat"), "the entity's calculation stands, whatever the document prints");
        assertEquals(vendor, ((Number) bill.get("Vendor")).intValue(),
                "the VAT number identified the vendor the registration number did not");

        List<Map<String, Object>> lines = list(LINES + "?Bill=" + recordId.get());
        assertEquals(2, lines.size(), "every line read is a row of the bill: " + lines);
        List<Map<String, Object>> scans = list(SCANS + "?Bill=" + recordId.get());
        assertEquals(1, scans.size(), "the uploaded file is kept on the bill: " + scans);
        assertEquals("bill.png", scans.get(0)
                                      .get("FileName"));

        // What a person must look at.
        List<String> expected = List.of("billedOn QR_MISMATCH", "items[2] LINE", "items[2].price MISSING", "vat MISMATCH");
        AtomicReference<List<Map<String, Object>>> flags = new AtomicReference<>();
        restAssuredExecutor.execute(() -> flags.set(given().when()
                                                           .get(CAPTURE + "/review/" + recordId.get())
                                                           .then()
                                                           .statusCode(200)
                                                           .body("captureName", equalTo("billScan"))
                                                           .extract()
                                                           .jsonPath()
                                                           .getList("flags")));
        assertEquals(expected, flagNames(flags.get()), "the review endpoint answers the capture's flags: " + flags.get());

        // A provider that declines creates nothing.
        decline.set(true);
        restAssuredExecutor.execute(() -> given().multiPart("file", "refused.png", scan, "image/png")
                                                 .when()
                                                 .post(CAPTURE + "/upload")
                                                 .then()
                                                 .statusCode(422)
                                                 .body("status", equalTo("FAILED")));
        assertEquals(1, list(BILLS).size(), "a failed capture creates no record");

        assertEquals(List.of("billScan CAPTURED bill.png anthropic claude-test-model 1800 240 record",
                "billScan FAILED refused.png null null 0 0 -"), ledger());
    }

    private static List<String> flagNames(List<Map<String, Object>> flags) {
        List<String> names = new ArrayList<>();
        for (Map<String, Object> flag : flags) {
            names.add(flag.get("field") + " " + flag.get("reason"));
        }
        names.sort(null);
        return names;
    }

    private List<String> ledger() throws SQLException {
        List<String> entries = new ArrayList<>();
        try (Connection connection = dataSourcesManager.getDefaultDataSource()
                                                       .getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT * FROM \"" + LEDGER + "\" ORDER BY \"Id\"")) {
            while (result.next()) {
                assertEquals(64, result.getString("Checksum")
                                       .length(),
                        "every attempt records the file's checksum");
                assertNotNull(result.getTimestamp("CapturedAt"));
                boolean captured = "CAPTURED".equals(result.getString("Status"));
                assertEquals(captured, result.getString("Answer") != null, "the provider's raw answer is kept with a captured record");
                entries.add(result.getString("CaptureName") + " " + result.getString("Status") + " " + result.getString("FileName") + " "
                        + result.getString("Provider") + " " + result.getString("Model") + " " + result.getInt("InputTokens") + " "
                        + result.getInt("OutputTokens") + " " + (result.getString("RecordId") == null ? "-" : "record"));
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

    /** A PNG carrying one QR code - the part of a fiscal receipt the platform reads without a model. */
    private static byte[] qrImage(String text) {
        try {
            BufferedImage image = MatrixToImageWriter.toBufferedImage(new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 400, 400));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (WriterException e) {
            throw new IllegalStateException("cannot render the QR fixture", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Map<String, Object> get(String url) {
        AtomicReference<Map<String, Object>> row = new AtomicReference<>();
        restAssuredExecutor.execute(() -> row.set(given().when()
                                                         .get(url)
                                                         .then()
                                                         .statusCode(200)
                                                         .extract()
                                                         .jsonPath()
                                                         .using(new JsonPathConfig(NumberReturnType.BIG_DECIMAL))
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
                                                          .using(new JsonPathConfig(NumberReturnType.BIG_DECIMAL))
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
        assertTrue(repository.hasResource(PROJECT_PATH + "/gen/events/" + PROJECT + "/BillScanCapture.java"),
                "the capture class is generated");
        assertTrue(repository.hasResource(PROJECT_PATH + "/billScan-capture-action.json"), "the capture button is contributed");
        assertTrue(repository.hasResource(PROJECT_PATH + "/billScan-capture-review-action.json"), "the review button is contributed");
    }

    private void publishProject() {
        restAssuredExecutor.execute(() -> given().when()
                                                 .post("/services/ide/publisher/" + WORKSPACE + "/" + PROJECT + "/")
                                                 .then()
                                                 .statusCode(200));
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
