/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.agent;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Bridges the Intent Editor's AI assistant to the Anthropic Messages API.
 *
 * <p>
 * The assistant edits the intent at the same altitude the developer does - it never re-emits a
 * model file. Per the intent layer's "edit shape, not file shape" contract, Claude is given the
 * current {@code app.intent} and the user's request and may call a single {@code propose_intent}
 * tool that returns the <em>complete</em> updated YAML. The editor diffs that against the current
 * buffer and lets the developer accept or reject - nothing is written to disk here. When the model
 * only needs to answer or ask a clarifying question it replies in plain text and proposes nothing.
 *
 * <p>
 * The API key lives server-side ({@link DirigibleConfig#INTENT_AI_API_KEY}); it is never sent to
 * the browser. A blank key disables the assistant ({@link IntentAgentNotConfiguredException}).
 */
@Service
class IntentAgentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntentAgentService.class);

    /**
     * Plain Gson - the platform's JsonHelper excludes un-{@code @Expose}d fields, which we do not use
     * here.
     */
    private static final Gson GSON = new Gson();

    private static final String TOOL_NAME = "propose_intent";

    /**
     * The system prompt - the full capability contract (schema, rules, the propose-the-whole-file
     * diff-stability discipline) lives in the {@code intent-assistant-guide.md} classpath resource so
     * it can be reviewed and edited as documentation and stays in lockstep with the schema the
     * {@link org.eclipse.dirigible.components.intent.parser.IntentParser} enforces.
     */
    private static final String SYSTEM_PROMPT = loadGuide();

    /**
     * Load the assistant guide packaged in this module's jar. A missing or unreadable resource is a
     * build/packaging defect, so it fails fast at class initialization rather than degrading the
     * assistant to a promptless state at request time.
     *
     * @return the guide contents as the system prompt
     */
    private static String loadGuide() {
        try (InputStream in = IntentAgentService.class.getResourceAsStream("/intent-assistant-guide.md")) {
            if (in == null) {
                throw new IllegalStateException("Required classpath resource /intent-assistant-guide.md is missing");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load the intent assistant guide", ex);
        }
    }

    private final HttpClient httpClient = HttpClient.newBuilder()
                                                    .connectTimeout(Duration.ofSeconds(15))
                                                    .build();

    /**
     * Run one assistant turn against the configured Claude model.
     *
     * @param request the current intent YAML, the user's message and the prior plain-text transcript
     * @return the assistant's reply text and, when it proposed an edit, the complete proposed YAML
     * @throws IntentAgentNotConfiguredException when no API key is configured
     * @throws IntentAgentException when the upstream call fails or returns an error
     */
    AgentReply chat(AgentRequest request) {
        String apiKey = DirigibleConfig.INTENT_AI_API_KEY.getStringValue();
        if (StringUtils.isBlank(apiKey)) {
            throw new IntentAgentNotConfiguredException(
                    "The Intent AI assistant is not configured. Set the DIRIGIBLE_INTENT_AI_API_KEY environment variable.");
        }
        String configuredBaseUrl = DirigibleConfig.INTENT_AI_BASE_URL.getStringValue();
        String baseUrl = configuredBaseUrl != null && configuredBaseUrl.endsWith("/")
                ? configuredBaseUrl.substring(0, configuredBaseUrl.length() - 1)
                : configuredBaseUrl;
        String body = GSON.toJson(buildRequestBody(request));

        HttpRequest httpRequest = HttpRequest.newBuilder()
                                             .uri(URI.create(baseUrl + "/v1/messages"))
                                             .timeout(Duration.ofSeconds(120))
                                             .header("content-type", "application/json")
                                             .header("x-api-key", apiKey)
                                             .header("anthropic-version", DirigibleConfig.INTENT_AI_VERSION.getStringValue())
                                             .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                                             .build();
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOGGER.error("Intent AI assistant upstream call failed with status [{}]: {}", response.statusCode(), response.body());
                throw new IntentAgentException("The AI assistant request failed (HTTP " + response.statusCode() + ").");
            }
            return parseReply(response.body());
        } catch (IOException ex) {
            throw new IntentAgentException("Could not reach the AI assistant.", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread()
                  .interrupt();
            throw new IntentAgentException("The AI assistant request was interrupted.", ex);
        }
    }

    /** Shape the Anthropic Messages request: system prompt, the single proposal tool, and the turns. */
    private Map<String, Object> buildRequestBody(AgentRequest request) {
        Map<String, Object> yamlProp = Map.of("type", "string", "description", "The COMPLETE updated app.intent YAML document.");
        Map<String, Object> explanationProp =
                Map.of("type", "string", "description", "A short, plain explanation of what changed and why.");
        Map<String, Object> inputSchema = Map.of("type", "object", "properties", Map.of("explanation", explanationProp, "yaml", yamlProp),
                "required", List.of("explanation", "yaml"));
        Map<String, Object> tool = Map.of("name", TOOL_NAME, "description",
                "Propose a complete, updated app.intent YAML for the developer to review as a diff.", "input_schema", inputSchema);

        List<Map<String, Object>> messages = new ArrayList<>();
        if (request.history() != null) {
            for (AgentTurn turn : request.history()) {
                // Only replay genuine conversational turns; the model API requires user/assistant roles.
                if (turn != null && ("user".equals(turn.role()) || "assistant".equals(turn.role())) && turn.content() != null) {
                    messages.add(Map.of("role", turn.role(), "content", turn.content()));
                }
            }
        }
        messages.add(Map.of("role", "user", "content", buildUserTurn(request)));

        return Map.of("model", DirigibleConfig.INTENT_AI_MODEL.getStringValue(), "max_tokens",
                DirigibleConfig.INTENT_AI_MAX_TOKENS.getIntValue(), "system", SYSTEM_PROMPT, "tools", List.of(tool), "messages", messages);
    }

    /**
     * Embed the ground-truth current YAML alongside the request so the model always diffs against
     * reality.
     */
    private String buildUserTurn(AgentRequest request) {
        String yaml = request.yaml() == null ? "" : request.yaml();
        return "Current app.intent:\n```yaml\n" + yaml + "\n```\n\nRequest: " + StringUtils.defaultString(request.message());
    }

    /**
     * Collect text blocks as the reply; a {@code propose_intent} tool call carries the proposed YAML.
     */
    private AgentReply parseReply(String responseBody) {
        JsonObject root = GSON.fromJson(responseBody, JsonObject.class);
        JsonArray content = root.getAsJsonArray("content");
        StringBuilder text = new StringBuilder();
        String proposedYaml = null;
        String explanation = null;
        if (content != null) {
            for (JsonElement element : content) {
                JsonObject block = element.getAsJsonObject();
                String type = block.has("type") ? block.get("type")
                                                       .getAsString()
                        : "";
                if ("text".equals(type) && block.has("text")) {
                    text.append(block.get("text")
                                     .getAsString());
                } else if ("tool_use".equals(type) && TOOL_NAME.equals(asString(block, "name"))) {
                    JsonObject input = block.getAsJsonObject("input");
                    if (input != null) {
                        proposedYaml = asString(input, "yaml");
                        explanation = asString(input, "explanation");
                    }
                }
            }
        }
        String reply = text.length() > 0 ? text.toString() : StringUtils.defaultString(explanation);
        if (StringUtils.isBlank(reply)) {
            reply = proposedYaml != null ? "I've proposed an update to the intent." : "(no response)";
        }
        return new AgentReply(reply, proposedYaml);
    }

    private static String asString(JsonObject object, String member) {
        return object.has(member) && !object.get(member)
                                            .isJsonNull() ? object.get(member)
                                                                  .getAsString()
                                                    : null;
    }
}
