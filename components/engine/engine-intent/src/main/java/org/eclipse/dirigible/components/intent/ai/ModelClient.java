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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * The single bridge from this platform to the Anthropic Messages API.
 *
 * <p>
 * Every assistant surface (the intent editor's YAML assistant, the Workbench's Java assistant)
 * calls the model through this one client, so the configuration keys, the timeouts, the error
 * contract and the tool-call parsing exist once. The API key lives server-side
 * ({@link DirigibleConfig#INTENT_AI_API_KEY}) and is never sent to a browser; a blank key disables
 * every assistant ({@link AssistantNotConfiguredException}).
 *
 * <p>
 * The call is <b>streamed</b> ({@code "stream": true}, consumed as server-sent events) and asks for
 * <b>adaptive thinking</b>. Both are capacity decisions, not cosmetics: the tool contract re-emits
 * the COMPLETE {@code app.intent} on every turn and every repair round, so a real application is
 * thousands of output tokens - which a single blocking response with one fixed deadline could not
 * hold, and which the model should reason about rather than emit cold. On the configured default
 * model omitting {@code thinking} means running with no thinking at all, so it is sent explicitly.
 */
@Component
public class ModelClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModelClient.class);

    /**
     * Plain Gson - the platform's JsonHelper excludes un-{@code @Expose}d fields, which we do not use
     * here.
     */
    private static final Gson GSON = new Gson();

    /** The Anthropic Messages path, appended to the configured base URL. */
    private static final String MESSAGES_PATH = "/v1/messages";

    /** The only line of a server-sent event this client reads; the payload carries its own type. */
    private static final String DATA_PREFIX = "data:";

    /**
     * An outer bound on the whole exchange - deliberately generous, and deliberately not a window the
     * answer has to fit into. The previous non-streamed call had to deliver the entire document within
     * 120 seconds, which is the wall this bound replaces: a reasoning pass over a few hundred lines of
     * structured YAML routinely outlives it, and a truncated-or-timed-out answer is the same failure to
     * the user.
     */
    private static final Duration RESPONSE_TIMEOUT = Duration.ofMinutes(10);

    private final HttpClient httpClient = HttpClient.newBuilder()
                                                    .connectTimeout(Duration.ofSeconds(15))
                                                    .build();

    /**
     * A tool the model may call to return a structured proposal.
     *
     * @param name the tool name, matched against the model's {@code tool_use} blocks
     * @param description what calling the tool means, in the model's terms
     * @param inputSchema the tool's JSON Schema, as nested maps/lists
     */
    public record ToolSpec(String name, String description, Map<String, Object> inputSchema) {
    }

    /**
     * One assistant response.
     *
     * @param text the concatenated text blocks - an explanation, an answer or a clarifying question
     * @param toolInput the tool call's input object, or {@code null} when the model called no tool
     */
    public record ModelReply(String text, JsonObject toolInput) {

        /**
         * Read a string member off the tool input.
         *
         * @param member the member name
         * @return the value, or {@code null} when there was no tool call or the member is absent/null
         */
        public String toolString(String member) {
            if (toolInput == null || !toolInput.has(member) || toolInput.get(member)
                                                                        .isJsonNull()) {
                return null;
            }
            return toolInput.get(member)
                            .getAsString();
        }
    }

    /**
     * A JSON Schema for an object of required string properties - the shape every proposal tool uses
     * today.
     *
     * @param properties property name to description, in the order the model should think about them
     * @return the schema as nested maps
     */
    public static Map<String, Object> stringSchema(Map<String, String> properties) {
        Map<String, Object> typed = new LinkedHashMap<>();
        properties.forEach((name, description) -> typed.put(name, Map.of("type", "string", "description", description)));
        return Map.of("type", "object", "properties", typed, "required", List.copyOf(properties.keySet()));
    }

    /**
     * Whether an API key is configured, i.e. whether {@link #call} can do anything at all. Read live
     * (not cached) so a key set after start-up is picked up, and deliberately free of any upstream
     * call: a client asking "can I use the assistant?" must not cost a model round-trip.
     *
     * @return {@code true} when the assistants are usable
     */
    public boolean isConfigured() {
        return StringUtils.isNotBlank(DirigibleConfig.INTENT_AI_API_KEY.getStringValue());
    }

    /**
     * One upstream round-trip, streamed.
     *
     * @param systemPrompt the assistant's guide
     * @param messages the conversation turns to send, oldest first
     * @param tool the single tool the model may call to propose a change
     * @return the parsed reply
     * @throws AssistantNotConfiguredException when no API key is configured
     * @throws AssistantUpstreamException when the call fails or returns an error status
     */
    public ModelReply call(String systemPrompt, List<Map<String, Object>> messages, ToolSpec tool) {
        String apiKey = DirigibleConfig.INTENT_AI_API_KEY.getStringValue();
        if (StringUtils.isBlank(apiKey)) {
            throw new AssistantNotConfiguredException(
                    "The AI assistant is not configured. Set the DIRIGIBLE_INTENT_AI_API_KEY environment variable.");
        }
        String body = GSON.toJson(requestBody(systemPrompt, messages, tool));

        HttpRequest httpRequest = HttpRequest.newBuilder()
                                             .uri(messagesEndpoint())
                                             .timeout(RESPONSE_TIMEOUT)
                                             .header("content-type", "application/json")
                                             .header("accept", "text/event-stream")
                                             .header("x-api-key", apiKey)
                                             .header("anthropic-version", DirigibleConfig.INTENT_AI_VERSION.getStringValue())
                                             .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                                             .build();
        try {
            HttpResponse<Stream<String>> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofLines());
            try (Stream<String> lines = response.body()) {
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    LOGGER.error("AI assistant upstream call failed with status [{}]: {}", response.statusCode(),
                            lines.collect(Collectors.joining("\n")));
                    throw new AssistantUpstreamException("The AI assistant request failed (HTTP " + response.statusCode() + ").");
                }
                return assembleReply(lines, tool.name());
            }
        } catch (JsonParseException ex) {
            throw new AssistantUpstreamException("The AI assistant returned an unreadable event stream.", ex);
        } catch (UncheckedIOException ex) {
            throw new AssistantUpstreamException("The connection to the AI assistant was lost before the answer was complete.", ex);
        } catch (IOException ex) {
            throw new AssistantUpstreamException("Could not reach the AI assistant.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread()
                  .interrupt();
            throw new AssistantUpstreamException("The AI assistant request was interrupted.", ex);
        }
    }

    /**
     * The Messages endpoint, built from the configured base URL after checking it is one we are willing
     * to send an API key to.
     *
     * <p>
     * The check is not ceremony. This request carries {@link DirigibleConfig#INTENT_AI_API_KEY} in a
     * header, so wherever this URL points is where the key goes - and the base URL is a
     * {@code Configuration} value, which on this platform is not as fixed as "deployment configuration"
     * suggests: {@code JobService.trigger} writes every parameter of a manually triggered job straight
     * into the global runtime configuration, so a caller privileged enough to trigger a job can
     * transiently redefine any key. Refusing anything that is not an absolute
     * {@code http}/{@code https} URL with a host and no embedded credentials keeps a redefinition from
     * turning into a credential hand-off, and turns a typo'd base URL into a clear message instead of
     * an obscure failure inside the HTTP client.
     *
     * @return the endpoint to POST to
     * @throws AssistantNotConfiguredException when the configured base URL is not usable
     */
    static URI messagesEndpoint() {
        String configured = DirigibleConfig.INTENT_AI_BASE_URL.getStringValue();
        String trimmed = StringUtils.trimToEmpty(configured);
        String withoutTrailingSlash = trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        URI base;
        try {
            base = new URI(withoutTrailingSlash);
        } catch (URISyntaxException ex) {
            throw new AssistantNotConfiguredException(unusableBaseUrl(withoutTrailingSlash), ex);
        }
        boolean http = "http".equalsIgnoreCase(base.getScheme()) || "https".equalsIgnoreCase(base.getScheme());
        if (!http || StringUtils.isBlank(base.getHost()) || base.getUserInfo() != null) {
            throw new AssistantNotConfiguredException(unusableBaseUrl(withoutTrailingSlash));
        }
        return URI.create(base.getScheme() + "://" + base.getAuthority() + StringUtils.defaultString(base.getPath()) + MESSAGES_PATH);
    }

    private static String unusableBaseUrl(String configured) {
        return "The AI assistant is not usable: DIRIGIBLE_INTENT_AI_BASE_URL must be an absolute http(s) URL without credentials,"
                + " but is [" + configured + "].";
    }

    /**
     * Shape the Anthropic Messages request: system prompt, the single proposal tool, and the turns -
     * streamed, and with adaptive thinking on.
     *
     * <p>
     * {@code thinking} is sent explicitly rather than left to the model's default because on the
     * configured default model omitting it means running with no thinking at all. The reply is not
     * rendered anywhere, so the display default ({@code omitted}) is left alone - the reasoning is
     * wanted for the answer's sake, not for the user to read.
     */
    private static Map<String, Object> requestBody(String systemPrompt, List<Map<String, Object>> messages, ToolSpec tool) {
        Map<String, Object> toolBody = Map.of("name", tool.name(), "description", tool.description(), "input_schema", tool.inputSchema());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", DirigibleConfig.INTENT_AI_MODEL.getStringValue());
        body.put("max_tokens", DirigibleConfig.INTENT_AI_MAX_TOKENS.getIntValue());
        body.put("stream", Boolean.TRUE);
        body.put("thinking", Map.of("type", "adaptive"));
        body.put("system", systemPrompt);
        body.put("tools", List.of(toolBody));
        body.put("messages", messages);
        return body;
    }

    /**
     * Assemble one reply out of the event stream: the text blocks concatenated as the answer, and the
     * matching {@code tool_use} block's streamed input-JSON fragments re-joined into the proposal.
     *
     * <p>
     * A {@code tool_use} block's {@code input} arrives as {@code input_json_delta} fragments of a
     * partial JSON string - the granularity is per fragment, not per member - so the pieces are
     * concatenated verbatim and parsed once the stream ends. They are matched by the block's
     * {@code index}, so a fragment of some other block can never land in the proposal. Thinking deltas
     * and the events that carry no content ({@code message_start}, {@code content_block_stop},
     * {@code ping}, and whatever the API adds next) are skipped: the versioning policy is explicit that
     * new event types may appear, so an unknown one is not an error.
     */
    private static ModelReply assembleReply(Stream<String> lines, String toolName) {
        StringBuilder text = new StringBuilder();
        StringBuilder toolJson = new StringBuilder();
        int toolBlockIndex = -1;
        Iterator<String> events = lines.iterator();
        while (events.hasNext()) {
            JsonObject event = eventData(events.next());
            if (event == null) {
                continue;
            }
            switch (StringUtils.defaultString(member(event, "type"))) {
                case "error" -> throw streamError(event);
                case "content_block_start" -> {
                    JsonObject block = event.getAsJsonObject("content_block");
                    if (block != null && "tool_use".equals(member(block, "type")) && toolName.equals(member(block, "name"))) {
                        toolBlockIndex = index(event);
                    }
                }
                case "content_block_delta" -> {
                    JsonObject delta = event.getAsJsonObject("delta");
                    String deltaType = delta == null ? "" : StringUtils.defaultString(member(delta, "type"));
                    if ("text_delta".equals(deltaType)) {
                        text.append(StringUtils.defaultString(member(delta, "text")));
                    } else if ("input_json_delta".equals(deltaType) && toolBlockIndex >= 0 && index(event) == toolBlockIndex) {
                        toolJson.append(StringUtils.defaultString(member(delta, "partial_json")));
                    }
                }
                case "message_delta" -> warnIfTruncated(event);
                default -> LOGGER.trace("Skipping AI assistant stream event [{}].", member(event, "type"));
            }
        }
        return new ModelReply(text.toString(), toolBlockIndex >= 0 ? toolInput(toolJson.toString()) : null);
    }

    /**
     * The JSON payload of one server-sent event line, or {@code null} for every line that carries none
     * (the {@code event:} name line, comments, and the blank line between events).
     *
     * <p>
     * Server-sent events are line-framed and each Messages API event's payload is one JSON object on a
     * single line, so a data line is parsed as it stands rather than re-assembled across lines.
     */
    private static JsonObject eventData(String line) {
        if (line == null || !line.startsWith(DATA_PREFIX)) {
            return null;
        }
        String payload = line.substring(DATA_PREFIX.length())
                             .trim();
        return payload.isEmpty() ? null : GSON.fromJson(payload, JsonObject.class);
    }

    /**
     * The proposal, re-joined. An empty buffer is a tool called with no arguments - not a failure - but
     * an unparseable one is the answer having been cut off mid-JSON, which is worth saying plainly:
     * that is what the output ceiling looks like from here.
     */
    private static JsonObject toolInput(String accumulated) {
        String json = StringUtils.trimToEmpty(accumulated);
        if (json.isEmpty()) {
            return new JsonObject();
        }
        try {
            return GSON.fromJson(json, JsonObject.class);
        } catch (JsonParseException ex) {
            throw new AssistantUpstreamException("The AI assistant's proposal was cut off before it was complete."
                    + " Raise DIRIGIBLE_INTENT_AI_MAX_TOKENS if this repeats.", ex);
        }
    }

    /**
     * An answer that stopped because it ran out of output tokens. Not fatal by itself - a text-only
     * reply is still readable, and a truncated proposal fails validation in the repair loop - but the
     * cause is invisible from either symptom, so it is named here.
     */
    private static void warnIfTruncated(JsonObject event) {
        JsonObject delta = event.getAsJsonObject("delta");
        if (delta != null && "max_tokens".equals(member(delta, "stop_reason"))) {
            LOGGER.warn("The AI assistant's answer hit the output ceiling of [{}] tokens and was truncated;"
                    + " raise DIRIGIBLE_INTENT_AI_MAX_TOKENS.", DirigibleConfig.INTENT_AI_MAX_TOKENS.getIntValue());
        }
    }

    /**
     * An {@code error} event mid-stream - an overload or a rate limit the API reports inside a 200
     * response. Mapped to the same upstream failure a non-2xx status is, so the endpoints answer 502
     * either way.
     */
    private static AssistantUpstreamException streamError(JsonObject event) {
        JsonObject error = event.getAsJsonObject("error");
        String type = error == null ? null : member(error, "type");
        LOGGER.error("The AI assistant's event stream carried an error [{}]: {}", type, error == null ? null : member(error, "message"));
        return new AssistantUpstreamException(
                "The AI assistant request failed (" + StringUtils.defaultIfBlank(type, "stream error") + ").");
    }

    /** A string member, or {@code null} when it is absent or JSON null. */
    private static String member(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    /** A content block's index, or {@code -1} when the event carries none. */
    private static int index(JsonObject event) {
        JsonElement value = event.get("index");
        return value == null || value.isJsonNull() ? -1 : value.getAsInt();
    }

    /**
     * The conversation turns: the replayed transcript plus the current user request. Only genuine
     * {@code user}/{@code assistant} turns are replayed - the model API accepts no other role.
     *
     * @param history the prior transcript, oldest first; may be {@code null}
     * @param userTurn the current user turn
     * @return the message list to send
     */
    public static List<Map<String, Object>> messages(List<ChatTurn> history, String userTurn) {
        List<Map<String, Object>> messages = new ArrayList<>();
        if (history != null) {
            for (ChatTurn turn : history) {
                if (turn != null && ("user".equals(turn.role()) || "assistant".equals(turn.role())) && turn.content() != null) {
                    messages.add(Map.of("role", turn.role(), "content", turn.content()));
                }
            }
        }
        messages.add(Map.of("role", "user", "content", userTurn));
        return messages;
    }
}
