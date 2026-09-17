/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * The streamed upstream call: the request the client sends, and the reply it assembles out of the
 * server-sent events.
 *
 * <p>
 * Both halves are the point of the change. The call is streamed because the tool contract re-emits
 * the COMPLETE {@code app.intent} on every turn, which one blocking response with a single deadline
 * could not hold; and it asks for adaptive thinking, which on the configured default model has to
 * be sent explicitly or the document is emitted with no reasoning at all. Neither is visible from a
 * reply that merely arrives, so the request body is asserted alongside the assembly.
 *
 * <p>
 * The upstream is a local HTTP server scripted per test - no network, no key.
 */
class ModelClientStreamTest {

    private static final String API_KEY_ENV = "DIRIGIBLE_INTENT_AI_API_KEY";
    private static final String BASE_URL_ENV = "DIRIGIBLE_INTENT_AI_BASE_URL";

    private static final String TOOL_NAME = "propose_intent";
    private static final Gson GSON = new Gson();

    /** Pause between flushed events when the upstream trickles its answer. */
    private static final long TRICKLE_PAUSE_MILLIS = 40;

    /** Long enough that its serialized form has to be split across many fragments. */
    private static final String PROPOSED_YAML = """
            name: library
            entities:
              - name: Member
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: name,  type: string, length: 120, required: true }
                  - { name: notes, type: text }
            """;

    private HttpServer upstream;
    private volatile String scriptedBody = "";
    private volatile int scriptedStatus = 200;
    private volatile boolean scriptedTrickle;
    private volatile String sentRequestBody;

    @BeforeEach
    void startScriptedUpstream() throws IOException {
        upstream = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        upstream.createContext("/v1/messages", this::respond);
        upstream.start();
        Configuration.set(API_KEY_ENV, "test-key");
        Configuration.set(BASE_URL_ENV, "http://localhost:" + upstream.getAddress()
                                                                      .getPort());
    }

    @AfterEach
    void stopScriptedUpstream() {
        Configuration.remove(API_KEY_ENV);
        Configuration.remove(BASE_URL_ENV);
        if (upstream != null) {
            upstream.stop(0);
        }
    }

    @Test
    void theRequestIsStreamedAndAsksForAdaptiveThinking() {
        scriptedBody = event("message_stop", "{\"type\":\"message_stop\"}");

        call();

        JsonObject body = GSON.fromJson(sentRequestBody, JsonObject.class);
        assertTrue(body.get("stream")
                       .getAsBoolean(),
                "the call must be streamed - a non-streamed one has to deliver the whole document inside one deadline");
        assertEquals("adaptive", body.getAsJsonObject("thinking")
                                     .get("type")
                                     .getAsString(),
                "adaptive thinking is sent explicitly; omitting it means no thinking at all on the default model");
        assertEquals(DirigibleConfig.INTENT_AI_MAX_TOKENS.getIntValue(), body.get("max_tokens")
                                                                             .getAsInt());
        assertEquals("32768", DirigibleConfig.INTENT_AI_MAX_TOKENS.getDefaultValue(),
                "the default ceiling has to hold a whole application plus its explanation, not one edit");
    }

    @Test
    void theTextAndTheProposalAreAssembledFromTheEventStream() {
        String toolInput = GSON.toJson(proposal());
        StringBuilder script = new StringBuilder();
        script.append(event("message_start", "{\"type\":\"message_start\",\"message\":{\"id\":\"msg_1\",\"content\":[]}}"));
        script.append(": a comment line no client reads\n\n");
        script.append(event("ping", "{\"type\":\"ping\"}"));
        script.append(blockStart(0, "{\"type\":\"text\",\"text\":\"\"}"));
        script.append(textDelta(0, "Added a Member "));
        script.append(textDelta(0, "entity."));
        script.append(blockStop(0));
        // A thinking block is streamed on the same connection and must not reach either output.
        script.append(blockStart(1, "{\"type\":\"thinking\",\"thinking\":\"\"}"));
        script.append(thinkingDelta(1, "The model reasons here."));
        script.append(blockStop(1));
        script.append(blockStart(2, toolUseBlock(TOOL_NAME)));
        // Split at an arbitrary width, so fragments land inside strings and inside escape sequences -
        // which is what "partial JSON strings" means: the pieces are concatenated verbatim.
        for (String fragment : fragments(toolInput, 17)) {
            script.append(jsonDelta(2, fragment));
        }
        script.append(blockStop(2));
        script.append(event("message_delta", "{\"type\":\"message_delta\",\"delta\":{\"stop_reason\":\"tool_use\"}}"));
        script.append(event("message_stop", "{\"type\":\"message_stop\"}"));
        // An event type this client has never seen: the API's versioning policy says new ones may appear.
        script.append(event("something_new", "{\"type\":\"something_new\"}"));
        scriptedBody = script.toString();

        ModelClient.ModelReply reply = call();

        assertEquals("Added a Member entity.", reply.text());
        assertEquals(PROPOSED_YAML, reply.toolString("yaml"), "the streamed fragments re-join into the proposal byte for byte");
        assertEquals("Added a Member entity.", reply.toolString("explanation"));
    }

    @Test
    void aTextOnlyAnswerCarriesNoToolInput() {
        scriptedBody = blockStart(0, "{\"type\":\"text\",\"text\":\"\"}") + textDelta(0, "Which entity do you mean?") + blockStop(0)
                + event("message_stop", "{\"type\":\"message_stop\"}");

        ModelClient.ModelReply reply = call();

        assertEquals("Which entity do you mean?", reply.text());
        assertNull(reply.toolInput());
    }

    @Test
    void theAnswerIsAssembledAcrossRealChunkBoundaries() {
        // The previous client read one buffered response, so a whole document had to be generated inside
        // a single 120-second window. This is the shape that replaces it: a chunked response of unknown
        // length, flushed event by event with the connection held open between them. It cannot assert the
        // old wall is gone in wall-clock terms - it asserts the client no longer needs the answer in one
        // piece, which is what the wall was made of.
        String toolInput = GSON.toJson(proposal());
        StringBuilder script = new StringBuilder();
        script.append(blockStart(0, "{\"type\":\"text\",\"text\":\"\"}"));
        script.append(textDelta(0, "Streaming."));
        script.append(blockStop(0));
        script.append(blockStart(1, toolUseBlock(TOOL_NAME)));
        for (String fragment : fragments(toolInput, 29)) {
            script.append(jsonDelta(1, fragment));
        }
        script.append(blockStop(1));
        script.append(event("message_stop", "{\"type\":\"message_stop\"}"));
        scriptedBody = script.toString();
        scriptedTrickle = true;

        ModelClient.ModelReply reply = call();

        assertEquals("Streaming.", reply.text());
        assertEquals(PROPOSED_YAML, reply.toolString("yaml"));
    }

    @Test
    void aFragmentOfAnotherBlockNeverLandsInTheProposal() {
        // Matching by block index rather than by "the last tool_use block seen": interleaved blocks are
        // legal, and a fragment of the wrong one would corrupt the proposal into unparseable JSON.
        String toolInput = GSON.toJson(proposal());
        scriptedBody = blockStart(0, toolUseBlock("some_other_tool")) + jsonDelta(0, "{\"garbage\":")
                + blockStart(1, toolUseBlock(TOOL_NAME)) + jsonDelta(1, toolInput) + jsonDelta(0, "\"more garbage\"}") + blockStop(1)
                + event("message_stop", "{\"type\":\"message_stop\"}");

        assertEquals(PROPOSED_YAML, call().toolString("yaml"));
    }

    @Test
    void anErrorEventMidStreamIsAnUpstreamFailure() {
        // A 529 the API reports inside a 200 response - the endpoints must answer 502 for it exactly as
        // they do for a non-2xx status.
        scriptedBody = blockStart(0, "{\"type\":\"text\",\"text\":\"\"}") + textDelta(0, "Working on it")
                + event("error", "{\"type\":\"error\",\"error\":{\"type\":\"overloaded_error\",\"message\":\"Overloaded\"}}");

        assertThrows(AssistantUpstreamException.class, this::call);
    }

    @Test
    void aProposalCutOffMidJsonIsAnUpstreamFailure() {
        scriptedBody = blockStart(0, toolUseBlock(TOOL_NAME)) + jsonDelta(0, "{\"explanation\": \"Added a Member ent");

        AssistantUpstreamException failure = assertThrows(AssistantUpstreamException.class, this::call);
        assertTrue(failure.getMessage()
                          .contains("cut off"),
                "the message has to name the truncation and the ceiling behind it - the JSON parse failure alone says nothing");
    }

    @Test
    void aNonSuccessStatusIsAnUpstreamFailure() {
        scriptedStatus = 400;
        scriptedBody = "{\"type\":\"error\",\"error\":{\"type\":\"invalid_request_error\",\"message\":\"bad request\"}}";

        assertThrows(AssistantUpstreamException.class, this::call);
    }

    @Test
    void withoutAnApiKeyNothingIsSent() {
        // Blanked rather than removed: a developer machine may well have the real key in its environment,
        // and a runtime value is what outranks it.
        Configuration.set(API_KEY_ENV, "");

        assertThrows(AssistantNotConfiguredException.class, this::call);
        assertNull(sentRequestBody);
    }

    private ModelClient.ModelReply call() {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("explanation", "What changed, in prose.");
        properties.put("yaml", "The complete app.intent.");
        ModelClient.ToolSpec tool =
                new ModelClient.ToolSpec(TOOL_NAME, "Propose the complete intent.", ModelClient.stringSchema(properties));
        return new ModelClient().call("You are an assistant.", List.of(Map.of("role", "user", "content", "Add a notes field")), tool);
    }

    private void respond(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            sentRequestBody = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        exchange.getResponseHeaders()
                .set("Content-Type", scriptedStatus == 200 ? "text/event-stream" : "application/json");
        if (scriptedTrickle) {
            trickle(exchange);
            return;
        }
        byte[] payload = scriptedBody.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(scriptedStatus, payload.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(payload);
        }
    }

    /**
     * Answer the way the real upstream does: a chunked response of unknown length, one event flushed at
     * a time with the connection held open in between.
     */
    private void trickle(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(200, 0);
        try (OutputStream out = exchange.getResponseBody()) {
            for (String event : scriptedBody.split("(?<=\\n\\n)")) {
                out.write(event.getBytes(StandardCharsets.UTF_8));
                out.flush();
                try {
                    Thread.sleep(TRICKLE_PAUSE_MILLIS);
                } catch (InterruptedException ex) {
                    Thread.currentThread()
                          .interrupt();
                    return;
                }
            }
        }
    }

    private static Map<String, String> proposal() {
        Map<String, String> input = new LinkedHashMap<>();
        input.put("explanation", "Added a Member entity.");
        input.put("yaml", PROPOSED_YAML);
        return input;
    }

    /** Split a serialized tool input the way the API does - at arbitrary character boundaries. */
    private static List<String> fragments(String json, int width) {
        List<String> fragments = new ArrayList<>();
        for (int start = 0; start < json.length(); start += width) {
            fragments.add(json.substring(start, Math.min(start + width, json.length())));
        }
        return fragments;
    }

    /** One server-sent event: the {@code event:} name line this client ignores, plus its data line. */
    private static String event(String type, String data) {
        return "event: " + type + "\ndata: " + data + "\n\n";
    }

    private static String blockStart(int index, String contentBlock) {
        return event("content_block_start",
                "{\"type\":\"content_block_start\",\"index\":" + index + ",\"content_block\":" + contentBlock + "}");
    }

    private static String blockStop(int index) {
        return event("content_block_stop", "{\"type\":\"content_block_stop\",\"index\":" + index + "}");
    }

    private static String toolUseBlock(String name) {
        return "{\"type\":\"tool_use\",\"id\":\"toolu_1\",\"name\":\"" + name + "\",\"input\":{}}";
    }

    private static String textDelta(int index, String text) {
        return delta(index, "text_delta", "text", text);
    }

    private static String jsonDelta(int index, String fragment) {
        return delta(index, "input_json_delta", "partial_json", fragment);
    }

    private static String thinkingDelta(int index, String thinking) {
        return delta(index, "thinking_delta", "thinking", thinking);
    }

    /** Built through Gson so the fragment's own quotes and escapes are encoded, not hand-escaped. */
    private static String delta(int index, String deltaType, String member, String value) {
        JsonObject delta = new JsonObject();
        delta.addProperty("type", deltaType);
        delta.addProperty(member, value);
        JsonObject payload = new JsonObject();
        payload.addProperty("type", "content_block_delta");
        payload.addProperty("index", index);
        payload.add("delta", delta);
        return event("content_block_delta", payload.toString());
    }
}
