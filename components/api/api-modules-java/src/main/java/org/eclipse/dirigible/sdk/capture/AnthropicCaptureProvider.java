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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The built-in capture provider: the Anthropic Messages API over plain HTTP, not streamed, with the
 * answer constrained to the capture's JSON schema.
 */
public final class AnthropicCaptureProvider implements CaptureProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnthropicCaptureProvider.class);

    private static final Gson GSON = new Gson();
    private static final String MESSAGES_PATH = "/v1/messages";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(5);
    private static final Set<Integer> RETRYABLE = Set.of(429, 500, 502, 503, 529);
    private static final int ATTEMPTS = 2;

    /** Pause before the single retry; package-visible so tests need not wait. */
    static volatile long retryDelayMillis = 2000L;

    @Override
    public String name() {
        return "anthropic";
    }

    @Override
    public CaptureAnswer extract(CaptureRequest request) {
        String apiKey = DirigibleConfig.CAPTURE_API_KEY.getStringValue();
        if (apiKey == null || apiKey.isBlank()) {
            throw new CaptureException(CaptureException.Kind.NOT_CONFIGURED,
                    "document capture is not configured: set DIRIGIBLE_CAPTURE_API_KEY");
        }
        URI endpoint = messagesEndpoint();
        String body = GSON.toJson(requestBody(request));
        HttpClient client = HttpClient.newBuilder()
                                      .connectTimeout(CONNECT_TIMEOUT)
                                      .build();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                                             .uri(endpoint)
                                             .timeout(REQUEST_TIMEOUT)
                                             .header("content-type", "application/json")
                                             .header("x-api-key", apiKey.trim())
                                             .header("anthropic-version", DirigibleConfig.CAPTURE_VERSION.getStringValue())
                                             .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                                             .build();
        HttpResponse<String> response = null;
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            boolean last = attempt == ATTEMPTS;
            try {
                response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (!last && RETRYABLE.contains(response.statusCode())) {
                    LOGGER.warn("The capture provider answered {}, retrying once", response.statusCode());
                    pause();
                    continue;
                }
                break;
            } catch (IOException e) {
                if (last) {
                    throw new CaptureException(CaptureException.Kind.UPSTREAM, "could not reach the capture provider", e);
                }
                LOGGER.warn("Could not reach the capture provider ({}), retrying once", e.getClass()
                                                                                         .getSimpleName());
                pause();
            } catch (InterruptedException e) {
                Thread.currentThread()
                      .interrupt();
                throw new CaptureException(CaptureException.Kind.UPSTREAM, "the call to the capture provider was interrupted", e);
            }
        }
        return answer(response);
    }

    private static void pause() {
        try {
            Thread.sleep(retryDelayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread()
                  .interrupt();
        }
    }

    private static CaptureAnswer answer(HttpResponse<String> response) {
        int status = response.statusCode();
        String text = response.body();
        if (status < 200 || status >= 300) {
            String detail = errorMessage(text);
            throw new CaptureException(CaptureException.Kind.UPSTREAM,
                    "the capture provider answered " + status + (detail == null ? "" : ": " + detail));
        }
        JsonObject root;
        try {
            root = JsonParser.parseString(text)
                             .getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            throw new CaptureException(CaptureException.Kind.UPSTREAM, "the capture provider's response is not valid JSON", e);
        }
        String stopReason = string(root, "stop_reason");
        if ("refusal".equals(stopReason)) {
            throw new CaptureException(CaptureException.Kind.REFUSED, "the capture provider declined to read the document");
        }
        if ("max_tokens".equals(stopReason)) {
            throw new CaptureException(CaptureException.Kind.UPSTREAM,
                    "the answer was cut off at max_tokens; raise DIRIGIBLE_CAPTURE_MAX_TOKENS or split the document");
        }
        StringBuilder json = new StringBuilder();
        JsonElement content = root.get("content");
        if (content != null && content.isJsonArray()) {
            for (JsonElement block : content.getAsJsonArray()) {
                if (block.isJsonObject() && "text".equals(string(block.getAsJsonObject(), "type"))) {
                    String part = string(block.getAsJsonObject(), "text");
                    if (part != null) {
                        json.append(part);
                    }
                }
            }
        }
        if (json.length() == 0) {
            throw new CaptureException(CaptureException.Kind.UPSTREAM, "the capture provider's response has no text content");
        }
        int in = 0;
        int out = 0;
        JsonElement usage = root.get("usage");
        if (usage != null && usage.isJsonObject()) {
            in = integer(usage.getAsJsonObject(), "input_tokens");
            out = integer(usage.getAsJsonObject(), "output_tokens");
        }
        return new CaptureAnswer(json.toString(), string(root, "model"), in, out);
    }

    private static String errorMessage(String body) {
        try {
            JsonElement error = JsonParser.parseString(body)
                                          .getAsJsonObject()
                                          .get("error");
            return error != null && error.isJsonObject() ? string(error.getAsJsonObject(), "message") : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String string(JsonObject object, String name) {
        JsonElement e = object.get(name);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }

    private static int integer(JsonObject object, String name) {
        JsonElement e = object.get(name);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive()
                                                    .isNumber() ? e.getAsInt() : 0;
    }

    private static JsonObject requestBody(CaptureRequest request) {
        boolean pdf = "application/pdf".equals(request.contentType());
        JsonObject source = new JsonObject();
        source.addProperty("type", "base64");
        source.addProperty("media_type", request.contentType());
        source.addProperty("data", Base64.getEncoder()
                                         .encodeToString(request.content()));
        JsonObject document = new JsonObject();
        document.addProperty("type", pdf ? "document" : "image");
        document.add("source", source);
        JsonObject instruction = new JsonObject();
        instruction.addProperty("type", "text");
        instruction.addProperty("text", "Read this document and fill the requested fields.");
        JsonArray content = new JsonArray();
        content.add(document);
        content.add(instruction);
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.add("content", content);
        JsonArray messages = new JsonArray();
        messages.add(message);

        JsonObject format = new JsonObject();
        format.addProperty("type", "json_schema");
        format.add("schema", JsonParser.parseString(request.jsonSchema()));
        JsonObject outputConfig = new JsonObject();
        outputConfig.add("format", format);

        JsonObject body = new JsonObject();
        body.addProperty("model", DirigibleConfig.CAPTURE_MODEL.getStringValue());
        body.addProperty("max_tokens", maxTokens());
        body.addProperty("system", request.instructions());
        body.add("messages", messages);
        body.add("output_config", outputConfig);
        String geo = DirigibleConfig.CAPTURE_INFERENCE_GEO.getStringValue();
        if (geo != null && !geo.isBlank()) {
            body.addProperty("inference_geo", geo.trim());
        }
        return body;
    }

    private static int maxTokens() {
        try {
            return Integer.parseInt(DirigibleConfig.CAPTURE_MAX_TOKENS.getStringValue()
                                                                      .trim());
        } catch (RuntimeException e) {
            return 16000;
        }
    }

    static URI messagesEndpoint() {
        String configured = DirigibleConfig.CAPTURE_BASE_URL.getStringValue();
        String trimmed = configured == null ? "" : configured.trim();
        String withoutTrailingSlash = trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        URI base;
        try {
            base = new URI(withoutTrailingSlash);
        } catch (URISyntaxException ex) {
            throw new CaptureException(CaptureException.Kind.NOT_CONFIGURED, unusableBaseUrl(withoutTrailingSlash), ex);
        }
        boolean http = "http".equalsIgnoreCase(base.getScheme()) || "https".equalsIgnoreCase(base.getScheme());
        if (!http || base.getHost() == null || base.getHost()
                                                   .isBlank()
                || base.getUserInfo() != null) {
            throw new CaptureException(CaptureException.Kind.NOT_CONFIGURED, unusableBaseUrl(withoutTrailingSlash));
        }
        return URI.create(base.getScheme() + "://" + base.getAuthority() + (base.getPath() == null ? "" : base.getPath()) + MESSAGES_PATH);
    }

    private static String unusableBaseUrl(String configured) {
        return "document capture is not usable: DIRIGIBLE_CAPTURE_BASE_URL must be an absolute http(s) URL without credentials, but is ["
                + configured + "].";
    }
}
