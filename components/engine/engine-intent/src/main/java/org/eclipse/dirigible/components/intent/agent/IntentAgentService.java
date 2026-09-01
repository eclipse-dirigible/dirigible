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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.intent.ai.AssistantGuide;
import org.eclipse.dirigible.components.intent.ai.ModelClient;
import org.eclipse.dirigible.components.intent.ai.ProposalRepairLoop;
import org.eclipse.dirigible.components.intent.parser.IntentParser;
import org.eclipse.dirigible.components.intent.parser.IntentValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Bridges the Intent Editor's AI assistant to the model.
 *
 * <p>
 * The assistant edits the intent at the same altitude the developer does - it never re-emits a
 * model file, and it never writes Java. Per the intent layer's "edit shape, not file shape"
 * contract, Claude is given the current {@code app.intent} and the user's request and may call a
 * single {@code propose_intent} tool that returns the <em>complete</em> updated YAML. The editor
 * diffs that against the current buffer and lets the developer accept or reject - nothing is
 * written to disk here. When the model only needs to answer or ask a clarifying question it replies
 * in plain text and proposes nothing.
 *
 * <p>
 * Every proposal is validated server-side with the same {@link IntentParser} that backs the
 * editor's parse endpoint, <em>before</em> it reaches the developer, through the shared
 * {@link ProposalRepairLoop} - after its last round the proposal is returned as-is with the
 * outstanding issues appended to the reply text, and the editor's own inline validation still
 * applies on Accept.
 *
 * <p>
 * The API key lives server-side ({@link DirigibleConfig#INTENT_AI_API_KEY}) and is never sent to
 * the browser; a blank key disables the assistant.
 */
@Service
class IntentAgentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntentAgentService.class);

    private static final String TOOL_NAME = "propose_intent";

    /**
     * The system prompt - the full capability contract (schema, rules, the propose-the-whole-file
     * diff-stability discipline) lives in the {@code intent-assistant-guide.md} classpath resource so
     * it can be reviewed and edited as documentation and stays in lockstep with the schema the
     * {@link IntentParser} enforces.
     */
    private static final String SYSTEM_PROMPT = AssistantGuide.load("/intent-assistant-guide.md");

    private static final ModelClient.ToolSpec TOOL = new ModelClient.ToolSpec(TOOL_NAME,
            "Propose a complete, updated app.intent YAML for the developer to review as a diff.", inputSchema());

    private final ModelClient modelClient;

    IntentAgentService(ModelClient modelClient) {
        this.modelClient = modelClient;
    }

    /**
     * The proposal's shape. {@code boundaries} is the structured half of the honesty contract: a
     * requirement the DSL cannot express must arrive as data the editor can render distinctly and the
     * developer can forward verbatim, not buried in prose that reads like the rest of the answer.
     */
    private static Map<String, Object> inputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("explanation", Map.of("type", "string", "description", "A short, plain explanation of what changed and why."));
        properties.put("yaml", Map.of("type", "string", "description", "The COMPLETE updated app.intent YAML document."));
        properties.put("boundaries",
                Map.of("type", "array", "description",
                        "Every requirement this proposal could NOT express in the intent - one entry each, "
                                + "including ones the proposal omits entirely. Empty when the request fit inside the DSL.",
                        "items", boundaryItemSchema()));
        return Map.of("type", "object", "properties", properties, "required", List.of("explanation", "yaml"));
    }

    private static Map<String, Object> boundaryItemSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("requirement", Map.of("type", "string", "description", "The developer's requirement, in their own words."));
        properties.put("explanation",
                Map.of("type", "string", "description", "Why the intent layer does not express it, and what this proposal does instead."));
        properties.put("extensionKind",
                Map.of("type", "string", "description",
                        "The extension point that carries it: calculatedAction, delegate, camelRoute, printTemplate, "
                                + "customPage, widget - or none when the proposal simply omits it."));
        properties.put("suggestedClass", Map.of("type", "string", "description",
                "The class the developer will hand-write, when the extension point is a Java one."));
        return Map.of("type", "object", "properties", properties, "required", List.of("requirement", "explanation", "extensionKind"));
    }

    /**
     * Whether the assistant is usable at all. Deliberately free of any upstream call: a client asking
     * "can I use the assistant?" must not cost a model round-trip.
     *
     * @return {@code true} when an API key is configured
     */
    boolean isConfigured() {
        return modelClient.isConfigured();
    }

    /**
     * Run one assistant turn against the configured Claude model, validating any proposed YAML with
     * {@link IntentParser} and sending the issues back for correction.
     *
     * @param request the current intent YAML, the user's message and the prior plain-text transcript
     * @return the assistant's reply text and, when it proposed an edit, the complete proposed YAML
     */
    AgentReply chat(AgentRequest request) {
        List<Map<String, Object>> messages = ModelClient.messages(request.history(), buildUserTurn(request));
        ProposalRepairLoop loop =
                new ProposalRepairLoop("yaml", "yaml", IntentAgentService::validationIssues, IntentAgentService::repairTurn);
        ProposalRepairLoop.Outcome outcome = loop.run(messages, this::callModel);

        String reply = replyText(outcome);
        if (!outcome.issues()
                    .isEmpty()) {
            reply += "\n\nNote: this proposal still fails intent validation:\n" + ProposalRepairLoop.bulleted(outcome.issues());
        }
        return new AgentReply(reply, outcome.proposal(), boundaries(outcome.reply()));
    }

    /**
     * The reported boundaries, in the order the model listed them. A malformed entry is dropped rather
     * than failing the turn - a proposal the developer can still read beats a 500 over a reporting
     * field.
     */
    private static List<AgentBoundary> boundaries(ModelClient.ModelReply reply) {
        JsonObject input = reply.toolInput();
        if (input == null || !input.has("boundaries") || !input.get("boundaries")
                                                               .isJsonArray()) {
            return List.of();
        }
        List<AgentBoundary> boundaries = new ArrayList<>();
        for (JsonElement element : input.getAsJsonArray("boundaries")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject entry = element.getAsJsonObject();
            String requirement = member(entry, "requirement");
            if (StringUtils.isBlank(requirement)) {
                continue;
            }
            boundaries.add(new AgentBoundary(requirement, member(entry, "explanation"), member(entry, "extensionKind"),
                    member(entry, "suggestedClass")));
        }
        return boundaries;
    }

    private static String member(JsonObject entry, String name) {
        return entry.has(name) && !entry.get(name)
                                        .isJsonNull() ? entry.get(name)
                                                             .getAsString()
                                                : null;
    }

    /**
     * One upstream round-trip. Package-visible so tests can substitute a scripted upstream.
     *
     * @param messages the conversation turns to send
     * @return the model's reply
     */
    ModelClient.ModelReply callModel(List<Map<String, Object>> messages) {
        return modelClient.call(SYSTEM_PROMPT, messages, TOOL);
    }

    /** The text blocks, else the tool's own explanation, else a neutral stand-in. */
    private static String replyText(ProposalRepairLoop.Outcome outcome) {
        String text = outcome.reply()
                             .text();
        if (StringUtils.isNotBlank(text)) {
            return text;
        }
        String explanation = outcome.reply()
                                    .toolString("explanation");
        if (StringUtils.isNotBlank(explanation)) {
            return explanation;
        }
        return outcome.proposal() != null ? "I've proposed an update to the intent." : "(no response)";
    }

    /**
     * Validate a proposed intent with the same parser that backs the editor's parse endpoint.
     *
     * @param proposedYaml the proposal
     * @return the validation issues; empty for a valid proposal
     */
    private static List<String> validationIssues(String proposedYaml) {
        try {
            IntentParser.parse(proposedYaml);
            return List.of();
        } catch (IntentValidationException ex) {
            LOGGER.debug("Intent AI proposal failed structural validation", ex);
            return ex.getIssues();
        } catch (RuntimeException ex) {
            LOGGER.debug("Intent AI proposal is not parseable", ex);
            return List.of("the proposed YAML could not be parsed: " + ex.getMessage());
        }
    }

    /** The corrective user turn: the validation issues plus the repair instruction. */
    private static String repairTurn(List<String> issues) {
        return "The proposed app.intent fails intent validation with the following issue(s):\n" + ProposalRepairLoop.bulleted(issues)
                + "\nCall propose_intent again with the corrected COMPLETE YAML. Fix only these issues and keep everything else"
                + " exactly as proposed.\n\nHow to fix them: a key rejected at one level is often valid at another - relocate it"
                + " before removing it, and re-read the guide section for the feature rather than inferring the platform's limits"
                + " from the message. Do NOT satisfy an issue by deleting a requirement the user asked for, by weakening a"
                + " cardinality, or by replacing a declarative construct with a hand-written delegate or service task. If after"
                + " that you still believe something cannot be expressed, keep the construct closest to the requirement and say"
                + " what is missing in the explanation - do not silently drop it.";
    }

    /**
     * Embed the ground-truth current YAML alongside the request so the model always diffs against
     * reality.
     */
    private static String buildUserTurn(AgentRequest request) {
        String yaml = request.yaml() == null ? "" : request.yaml();
        return "Current app.intent:\n```yaml\n" + yaml + "\n```\n\nRequest: " + StringUtils.defaultString(request.message());
    }
}
