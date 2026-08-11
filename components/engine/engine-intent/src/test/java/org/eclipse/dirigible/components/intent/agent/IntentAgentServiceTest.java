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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.commons.config.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The validate-and-repair loop: a proposed intent is validated with the real
 * {@link org.eclipse.dirigible.components.intent.parser.IntentParser} before it reaches the editor,
 * an invalid proposal is sent back for correction, and the loop is bounded. The upstream model is
 * scripted - no network.
 */
class IntentAgentServiceTest {

    private static final String API_KEY_ENV = "DIRIGIBLE_INTENT_AI_API_KEY";

    private static final String VALID_YAML = """
            name: lib
            entities:
              - name: Member
                fields:
                  - { name: id,    type: integer, primaryKey: true, generated: true }
                  - { name: name,  type: string }
                  - { name: notes, type: text }
            """;

    /** The exact first-proposal failure shape: {@code editable} lists a field not in {@code fields}. */
    private static final String INVALID_YAML = VALID_YAML + """
            forms:
              - { name: Review, forEntity: Member, fields: [name], editable: [notes], actions: [approve] }
            """;

    @BeforeAll
    static void configureApiKey() {
        Configuration.set(API_KEY_ENV, "test-key");
    }

    @AfterAll
    static void removeApiKey() {
        Configuration.remove(API_KEY_ENV);
    }

    @Test
    void aValidFirstProposalIsReturnedWithoutARepairRound() {
        ScriptedAgentService service = new ScriptedAgentService(new AgentReply("Added notes.", VALID_YAML));

        AgentReply reply = service.chat(request());

        assertEquals(1, service.calls.size());
        assertEquals(VALID_YAML, reply.proposedYaml());
        assertEquals("Added notes.", reply.reply());
    }

    @Test
    void aPlainTextReplyIsReturnedWithoutValidation() {
        ScriptedAgentService service = new ScriptedAgentService(new AgentReply("Which entity do you mean?", null));

        AgentReply reply = service.chat(request());

        assertEquals(1, service.calls.size());
        assertEquals("Which entity do you mean?", reply.reply());
    }

    @Test
    void anInvalidProposalIsSentBackWithTheIssuesForCorrection() {
        ScriptedAgentService service =
                new ScriptedAgentService(new AgentReply("First try.", INVALID_YAML), new AgentReply("Fixed.", VALID_YAML));

        AgentReply reply = service.chat(request());

        assertEquals(2, service.calls.size());
        assertEquals(VALID_YAML, reply.proposedYaml());

        List<Map<String, Object>> secondCall = service.calls.get(1);
        String assistantEcho = (String) secondCall.get(secondCall.size() - 2)
                                                  .get("content");
        String repairTurn = (String) secondCall.get(secondCall.size() - 1)
                                               .get("content");
        assertEquals("assistant", secondCall.get(secondCall.size() - 2)
                                            .get("role"));
        assertEquals("user", secondCall.get(secondCall.size() - 1)
                                       .get("role"));
        assertTrue(assistantEcho.contains(INVALID_YAML), "the failed proposal is replayed as the assistant turn");
        assertTrue(repairTurn.contains("only a displayed field"), "the parser's issue text is sent back verbatim");
        assertTrue(repairTurn.contains("corrected COMPLETE YAML"), "the repair instruction asks for a full re-proposal");
    }

    @Test
    void repairRoundsAreBoundedAndTheOutstandingIssuesAreSurfaced() {
        ScriptedAgentService service = new ScriptedAgentService(new AgentReply("Try 1.", INVALID_YAML),
                new AgentReply("Try 2.", INVALID_YAML), new AgentReply("Try 3.", INVALID_YAML));

        AgentReply reply = service.chat(request());

        assertEquals(3, service.calls.size(), "one initial call plus exactly two repair rounds");
        assertEquals(INVALID_YAML, reply.proposedYaml(), "the last proposal is still returned for the editor to flag");
        assertTrue(reply.reply()
                        .contains("still fails intent validation"),
                "the outstanding issues are appended to the reply text");
    }

    private static AgentRequest request() {
        return new AgentRequest("name: lib\n", "Add a notes field", List.of());
    }

    /** Overrides the upstream call with a scripted sequence of replies, recording each call's turns. */
    private static final class ScriptedAgentService extends IntentAgentService {

        private final Deque<AgentReply> replies;
        private final List<List<Map<String, Object>>> calls = new ArrayList<>();

        private ScriptedAgentService(AgentReply... scripted) {
            this.replies = new ArrayDeque<>(List.of(scripted));
        }

        @Override
        AgentReply callModel(String apiKey, String baseUrl, List<Map<String, Object>> messages) {
            calls.add(List.copyOf(messages));
            return replies.pop();
        }
    }
}
