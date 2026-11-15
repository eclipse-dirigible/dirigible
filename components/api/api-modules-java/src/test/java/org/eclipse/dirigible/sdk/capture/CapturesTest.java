/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.capture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.dirigible.commons.config.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CapturesTest {

    private static final String[] KEYS =
            {"DIRIGIBLE_CAPTURE_API_KEY", "DIRIGIBLE_CAPTURE_BASE_URL", "DIRIGIBLE_CAPTURE_PROVIDER", "DIRIGIBLE_CAPTURE_MAX_FILE_SIZE"};

    private HttpServer server;
    private final List<String> bodies = new ArrayList<>();
    private volatile String answerText;

    private static final CaptureSchema SCHEMA = CaptureSchema.create()
                                                             .field("Number", CaptureType.STRING, null)
                                                             .item("Qty", CaptureType.NUMBER, null);

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                bodies.add(new String(exchange.getRequestBody()
                                              .readAllBytes(),
                        StandardCharsets.UTF_8));
                JsonObject response = new JsonObject();
                response.addProperty("model", "m-1");
                response.addProperty("stop_reason", "end_turn");
                var content = new com.google.gson.JsonArray();
                JsonObject block = new JsonObject();
                block.addProperty("type", "text");
                block.addProperty("text", answerText);
                content.add(block);
                response.add("content", content);
                JsonObject usage = new JsonObject();
                usage.addProperty("input_tokens", 10);
                usage.addProperty("output_tokens", 5);
                response.add("usage", usage);
                byte[] bytes = response.toString()
                                       .getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody()
                        .write(bytes);
            } finally {
                exchange.close();
            }
        });
        server.start();
        Configuration.set("DIRIGIBLE_CAPTURE_BASE_URL", "http://127.0.0.1:" + server.getAddress()
                                                                                    .getPort());
        Configuration.set("DIRIGIBLE_CAPTURE_API_KEY", "k");
    }

    @AfterEach
    void stop() {
        server.stop(0);
        for (String key : KEYS) {
            Configuration.remove(key);
        }
    }

    private CaptureReading read(String file, String type, byte[] content, Map<String, String> hints) {
        return Captures.read(SCHEMA, "Be careful", file, type, content, hints);
    }

    @Test
    void parsesTheReading() {
        answerText =
                "{\"fields\":{\"Number\":{\"value\":\" INV-7 \",\"confidence\":\"HIGH\"},\"Total\":{\"value\":1.0E+3,\"confidence\":\"medium\"},"
                        + "\"Count\":{\"value\":3,\"confidence\":\"low\"},\"Paid\":{\"value\":true,\"confidence\":\"high\"},"
                        + "\"Date\":{\"value\":\"2026-11-15\",\"confidence\":\"high\"},\"Blank\":{\"value\":\"  \",\"confidence\":\"high\"},"
                        + "\"Gone\":{\"value\":null,\"confidence\":\"nonsense\"},\"Dec\":{\"value\":12.50,\"confidence\":\"high\"}},"
                        + "\"items\":[{\"Qty\":2,\"confidence\":\"medium\"},{\"Qty\":null,\"confidence\":\"high\"}]}";
        CaptureReading reading = read("a.png", "image/png", new byte[] {1}, null);
        assertEquals("anthropic", reading.getProvider());
        assertEquals("m-1", reading.getModel());
        assertEquals(10, reading.getInputTokens());
        assertEquals(5, reading.getOutputTokens());
        assertEquals(answerText, reading.getAnswer());
        CaptureValues f = reading.fields();
        assertEquals("INV-7", f.text("Number"));
        assertEquals("1000", f.text("Total"));
        assertEquals("3", f.text("Count"));
        assertEquals("true", f.text("Paid"));
        assertEquals("2026-11-15", f.text("Date"));
        assertEquals("12.50", f.text("Dec"));
        assertNull(f.text("Blank"));
        assertNull(f.text("Gone"));
        assertNull(f.text("Missing"));
        assertEquals(CaptureConfidence.HIGH, f.confidence("Number"));
        assertEquals(CaptureConfidence.MEDIUM, f.confidence("Total"));
        assertEquals(CaptureConfidence.LOW, f.confidence("Count"));
        assertEquals(CaptureConfidence.LOW, f.confidence("Gone"));
        assertEquals(CaptureConfidence.LOW, f.confidence("Missing"));
        assertEquals(2, reading.items()
                               .size());
        assertEquals("2", reading.items()
                                 .get(0)
                                 .text("Qty"));
        assertEquals(CaptureConfidence.MEDIUM, reading.items()
                                                      .get(0)
                                                      .confidence("anything"));
        assertNull(reading.items()
                          .get(1)
                          .text("Qty"));
    }

    @Test
    void noItemsIsEmpty() {
        answerText = "{\"fields\":{}}";
        assertTrue(read("a.png", "image/png", new byte[] {1}, null).items()
                                                                   .isEmpty());
    }

    @Test
    void theInstructionsCarryThePreambleTheCallersTextAndTheHints() {
        answerText = "{\"fields\":{},\"items\":[]}";
        Map<String, String> hints = new LinkedHashMap<>();
        hints.put("Number", "0001");
        hints.put("Total", "12.00");
        read("a.png", "image/png", new byte[] {1}, hints);
        String system = JsonParser.parseString(bodies.get(0))
                                  .getAsJsonObject()
                                  .get("system")
                                  .getAsString();
        assertTrue(system.contains("any language and script"));
        assertTrue(system.contains("Be careful"));
        assertTrue(system.contains("Values decoded from a machine-readable code on the document (exact, prefer them over what you read):"));
        assertTrue(system.contains("Number: 0001\nTotal: 12.00"));
        read("a.png", "image/png", new byte[] {1}, Map.of());
        assertFalse(JsonParser.parseString(bodies.get(1))
                              .getAsJsonObject()
                              .get("system")
                              .getAsString()
                              .contains("Values decoded"));
    }

    @Test
    void theTypeComesFromTheExtensionWhenNotGiven() {
        answerText = "{\"fields\":{}}";
        read("scan.PDF", "application/octet-stream", new byte[] {1}, null);
        read("scan.jpeg", null, new byte[] {1}, null);
        String first = bodies.get(0);
        assertTrue(first.contains("\"type\":\"document\""));
        assertTrue(bodies.get(1)
                         .contains("image/jpeg"));
    }

    @Test
    void refusesEmptyOversizedAndUnsupportedFiles() {
        answerText = "{\"fields\":{}}";
        assertEquals(CaptureException.Kind.UNREADABLE,
                assertThrows(CaptureException.class, () -> read("a.png", "image/png", new byte[0], null)).getKind());
        assertEquals(CaptureException.Kind.UNREADABLE,
                assertThrows(CaptureException.class, () -> read("a.txt", "text/plain", new byte[] {1}, null)).getKind());
        assertEquals(CaptureException.Kind.UNREADABLE,
                assertThrows(CaptureException.class, () -> read("a", null, new byte[] {1}, null)).getKind());
        Configuration.set("DIRIGIBLE_CAPTURE_MAX_FILE_SIZE", "2");
        assertEquals(CaptureException.Kind.UNREADABLE,
                assertThrows(CaptureException.class, () -> read("a.png", "image/png", new byte[] {1, 2, 3}, null)).getKind());
        assertEquals(0, bodies.size());
    }

    @Test
    void malformedOrIncompleteAnswersAreUpstream() {
        answerText = "not json";
        assertEquals(CaptureException.Kind.UPSTREAM,
                assertThrows(CaptureException.class, () -> read("a.png", "image/png", new byte[] {1}, null)).getKind());
        answerText = "{\"items\":[]}";
        assertEquals(CaptureException.Kind.UPSTREAM,
                assertThrows(CaptureException.class, () -> read("a.png", "image/png", new byte[] {1}, null)).getKind());
    }

    @Test
    void anUnknownProviderIsNotConfigured() {
        Configuration.set("DIRIGIBLE_CAPTURE_PROVIDER", "nope");
        CaptureException e = assertThrows(CaptureException.class, () -> read("a.png", "image/png", new byte[] {1}, null));
        assertEquals(CaptureException.Kind.NOT_CONFIGURED, e.getKind());
        assertEquals("no capture provider named [nope] is registered", e.getMessage());
        assertFalse(Captures.isConfigured());
    }

    @Test
    void isConfiguredFollowsTheKey() {
        assertTrue(Captures.isConfigured());
        Configuration.remove("DIRIGIBLE_CAPTURE_API_KEY");
        assertFalse(Captures.isConfigured());
    }

    @Test
    void theResultDoesNotSerialiseTheRawAnswer() {
        answerText = "{\"fields\":{}}";
        CaptureReading reading = read("a.png", "image/png", new byte[] {1}, null);
        CaptureResult result = CaptureResult.captured("c", "a.png", "sum", reading, 42, null);
        String json = new com.google.gson.Gson().toJson(result);
        assertFalse(json.contains("fields"));
        assertEquals("42", result.getRecordId());
        assertEquals("m-1", result.getModel());
        assertEquals("{\"fields\":{}}", result.answer());
        assertTrue(result.getFlags()
                         .isEmpty());
    }
}
