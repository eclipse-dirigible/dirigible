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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * The single bridge from this platform to the Anthropic Messages API.
 *
 * <p>
 * Every assistant surface (the intent editor's YAML assistant, the Workbench's Java assistant)
 * calls the model through this one client, so the configuration keys, the timeouts, the error
 * contract and the tool-call parsing exist once. The API key lives server-side
 * ({@link DirigibleConfig#INTENT_AI_API_KEY}) and is never sent to a browser; a blank key disables
 * every assistant ({@link AssistantNotConfiguredException}).
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
     * One upstream round-trip.
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
                                             .timeout(Duration.ofSeconds(120))
                                             .header("content-type", "application/json")
                                             .header("x-api-key", apiKey)
                                             .header("anthropic-version", DirigibleConfig.INTENT_AI_VERSION.getStringValue())
                                             .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                                             .build();
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOGGER.error("AI assistant upstream call failed with status [{}]: {}", response.statusCode(), response.body());
                throw new AssistantUpstreamException("The AI assistant request failed (HTTP " + response.statusCode() + ").");
            }
            return parseReply(response.body(), tool.name());
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

    /** Shape the Anthropic Messages request: system prompt, the single proposal tool, and the turns. */
    private static Map<String, Object> requestBody(String systemPrompt, List<Map<String, Object>> messages, ToolSpec tool) {
        Map<String, Object> toolBody = Map.of("name", tool.name(), "description", tool.description(), "input_schema", tool.inputSchema());
        return Map.of("model", DirigibleConfig.INTENT_AI_MODEL.getStringValue(), "max_tokens",
                DirigibleConfig.INTENT_AI_MAX_TOKENS.getIntValue(), "system", systemPrompt, "tools", List.of(toolBody), "messages",
                messages);
    }

    /** Collect the text blocks as the reply; a matching {@code tool_use} block carries the proposal. */
    private static ModelReply parseReply(String responseBody, String toolName) {
        JsonObject root = GSON.fromJson(responseBody, JsonObject.class);
        JsonArray content = root.getAsJsonArray("content");
        StringBuilder text = new StringBuilder();
        JsonObject toolInput = null;
        if (content != null) {
            for (JsonElement element : content) {
                JsonObject block = element.getAsJsonObject();
                String type = block.has("type") ? block.get("type")
                                                       .getAsString()
                        : "";
                if ("text".equals(type) && block.has("text")) {
                    text.append(block.get("text")
                                     .getAsString());
                } else if ("tool_use".equals(type) && block.has("name") && toolName.equals(block.get("name")
                                                                                                .getAsString())) {
                    toolInput = block.getAsJsonObject("input");
                }
            }
        }
        return new ModelReply(text.toString(), toolInput);
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
