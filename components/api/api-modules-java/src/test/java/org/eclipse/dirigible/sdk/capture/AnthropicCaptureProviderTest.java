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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.dirigible.commons.config.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnthropicCaptureProviderTest {

    private static final String[] KEYS =
            {"DIRIGIBLE_CAPTURE_API_KEY", "DIRIGIBLE_CAPTURE_BASE_URL", "DIRIGIBLE_CAPTURE_INFERENCE_GEO", "DIRIGIBLE_CAPTURE_MODEL"};

    private HttpServer server;
    private final List<String> bodies = new ArrayList<>();
    private final List<HttpExchange> exchanges = new ArrayList<>();
    private final List<String> paths = new ArrayList<>();
    private final List<String> apiKeys = new ArrayList<>();
    private final List<String> versions = new ArrayList<>();
    private final List<Consumer<HttpExchange>> script = new ArrayList<>();

    @BeforeEach
    void start() throws IOException {
        AnthropicCaptureProvider.retryDelayMillis = 0;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                bodies.add(new String(exchange.getRequestBody()
                                              .readAllBytes(),
                        StandardCharsets.UTF_8));
                paths.add(exchange.getRequestURI()
                                  .getPath());
                apiKeys.add(exchange.getRequestHeaders()
                                    .getFirst("x-api-key"));
                versions.add(exchange.getRequestHeaders()
                                     .getFirst("anthropic-version"));
                int index = Math.min(bodies.size(), script.size()) - 1;
                script.get(index)
                      .accept(exchange);
            } finally {
                exchange.close();
            }
        });
        server.start();
        Configuration.set("DIRIGIBLE_CAPTURE_BASE_URL", "http://127.0.0.1:" + server.getAddress()
                                                                                    .getPort());
        Configuration.set("DIRIGIBLE_CAPTURE_API_KEY", "sk-test-key");
    }

    @AfterEach
    void stop() {
        server.stop(0);
        for (String key : KEYS) {
            Configuration.remove(key);
        }
    }

    private static Consumer<HttpExchange> reply(int status, String body) {
        return exchange -> {
            try {
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders()
                        .add("content-type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody()
                        .write(bytes);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        };
    }

    private static final String OK = "{\"model\":\"claude-opus-5-5\",\"stop_reason\":\"end_turn\",\"content\":["
            + "{\"type\":\"thinking\",\"thinking\":\"hmm\"},{\"type\":\"text\",\"text\":\"{\\\"fields\\\":\"},{\"type\":\"text\",\"text\":\"{}}\"}],"
            + "\"usage\":{\"input_tokens\":1200,\"output_tokens\":85}}";

    private static CaptureRequest request(String contentType) {
        return new CaptureRequest("Read it", CaptureSchema.create()
                                                          .field("Number", CaptureType.STRING, "n")
                                                          .toJsonSchema(),
                "r.bin", contentType, new byte[] {1, 2, 3});
    }

    @Test
    void sendsTheDocumentedRequestForAnImage() {
        script.add(reply(200, OK));
        CaptureAnswer answer = new AnthropicCaptureProvider().extract(request("image/jpeg"));
        assertEquals("{\"fields\":{}}", answer.json());
        assertEquals("claude-opus-5-5", answer.model());
        assertEquals(1200, answer.inputTokens());
        assertEquals(85, answer.outputTokens());
        assertEquals("/v1/messages", paths.get(0));
        assertEquals("sk-test-key", apiKeys.get(0));
        assertEquals("2026-10-21", versions.get(0));
        JsonObject body = JsonParser.parseString(bodies.get(0))
                                    .getAsJsonObject();
        assertEquals("claude-opus-5-5", body.get("model")
                                            .getAsString());
        assertEquals(16000, body.get("max_tokens")
                                .getAsInt());
        assertEquals("Read it", body.get("system")
                                    .getAsString());
        JsonObject first = body.getAsJsonArray("messages")
                               .get(0)
                               .getAsJsonObject()
                               .getAsJsonArray("content")
                               .get(0)
                               .getAsJsonObject();
        assertEquals("image", first.get("type")
                                   .getAsString());
        assertEquals("image/jpeg", first.getAsJsonObject("source")
                                        .get("media_type")
                                        .getAsString());
        assertEquals("AQID", first.getAsJsonObject("source")
                                  .get("data")
                                  .getAsString());
        JsonObject format = body.getAsJsonObject("output_config")
                                .getAsJsonObject("format");
        assertEquals("json_schema", format.get("type")
                                          .getAsString());
        assertTrue(format.get("schema")
                         .isJsonObject());
        assertFalse(body.has("inference_geo"));
        for (String forbidden : new String[] {"temperature", "top_p", "top_k", "thinking", "tool_choice", "tools"}) {
            assertFalse(body.has(forbidden), forbidden);
        }
    }

    @Test
    void sendsADocumentBlockForAPdfAndInferenceGeoWhenConfigured() {
        Configuration.set("DIRIGIBLE_CAPTURE_INFERENCE_GEO", "us");
        script.add(reply(200, OK));
        new AnthropicCaptureProvider().extract(request("application/pdf"));
        JsonObject body = JsonParser.parseString(bodies.get(0))
                                    .getAsJsonObject();
        JsonObject first = body.getAsJsonArray("messages")
                               .get(0)
                               .getAsJsonObject()
                               .getAsJsonArray("content")
                               .get(0)
                               .getAsJsonObject();
        assertEquals("document", first.get("type")
                                      .getAsString());
        assertEquals("us", body.get("inference_geo")
                               .getAsString());
    }

    @Test
    void refusalIsRefused() {
        script.add(reply(200, "{\"stop_reason\":\"refusal\",\"content\":[]}"));
        CaptureException e = assertThrows(CaptureException.class, () -> new AnthropicCaptureProvider().extract(request("image/png")));
        assertEquals(CaptureException.Kind.REFUSED, e.getKind());
    }

    @Test
    void maxTokensIsUpstream() {
        script.add(reply(200, "{\"stop_reason\":\"max_tokens\",\"content\":[{\"type\":\"text\",\"text\":\"{\"}]}"));
        CaptureException e = assertThrows(CaptureException.class, () -> new AnthropicCaptureProvider().extract(request("image/png")));
        assertEquals(CaptureException.Kind.UPSTREAM, e.getKind());
        assertTrue(e.getMessage()
                    .contains("max_tokens"));
    }

    @Test
    void a401IsUpstreamWithoutRetryAndDoesNotLeakTheKey() {
        script.add(reply(401, "{\"error\":{\"type\":\"authentication_error\",\"message\":\"invalid x-api-key\"}}"));
        CaptureException e = assertThrows(CaptureException.class, () -> new AnthropicCaptureProvider().extract(request("image/png")));
        assertEquals(CaptureException.Kind.UPSTREAM, e.getKind());
        assertEquals("the capture provider answered 401: invalid x-api-key", e.getMessage());
        assertFalse(e.getMessage()
                     .contains("sk-test-key"));
        assertEquals(1, bodies.size());
    }

    @Test
    void a529IsRetriedOnce() {
        script.add(reply(529, "{\"error\":{\"message\":\"overloaded\"}}"));
        script.add(reply(200, OK));
        CaptureAnswer answer = new AnthropicCaptureProvider().extract(request("image/png"));
        assertEquals(2, bodies.size());
        assertEquals("{\"fields\":{}}", answer.json());
    }

    @Test
    void aSecond529IsGivenUpOn() {
        script.add(reply(529, "{}"));
        script.add(reply(529, "{}"));
        CaptureException e = assertThrows(CaptureException.class, () -> new AnthropicCaptureProvider().extract(request("image/png")));
        assertEquals(CaptureException.Kind.UPSTREAM, e.getKind());
        assertEquals(2, bodies.size());
    }

    @Test
    void noKeyIsNotConfiguredAndMakesNoRequest() {
        Configuration.remove("DIRIGIBLE_CAPTURE_API_KEY");
        CaptureException e = assertThrows(CaptureException.class, () -> new AnthropicCaptureProvider().extract(request("image/png")));
        assertEquals(CaptureException.Kind.NOT_CONFIGURED, e.getKind());
        assertEquals(0, bodies.size());
    }

    @Test
    void anUnusableBaseUrlIsNotConfigured() {
        Configuration.set("DIRIGIBLE_CAPTURE_BASE_URL", "ftp://example.com");
        CaptureException e = assertThrows(CaptureException.class, () -> new AnthropicCaptureProvider().extract(request("image/png")));
        assertEquals(CaptureException.Kind.NOT_CONFIGURED, e.getKind());
    }
}
