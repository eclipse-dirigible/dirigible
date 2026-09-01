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

import org.eclipse.dirigible.components.intent.ai.ModelClient;
import org.eclipse.dirigible.components.intent.generator.IntentGenerationService;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * The validate-and-repair loop: a proposed intent is validated with the real
 * {@link org.eclipse.dirigible.components.intent.parser.IntentParser} before it reaches the editor,
 * an invalid proposal is sent back for correction, and the loop is bounded. The upstream model is
 * scripted - no network.
 */
class IntentAgentServiceTest {

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

    @Test
    void aValidFirstProposalIsReturnedWithoutARepairRound() {
        ScriptedAgentService service = new ScriptedAgentService(proposal("Added notes.", VALID_YAML));

        AgentReply reply = service.chat(request());

        assertEquals(1, service.calls.size());
        assertEquals(VALID_YAML, reply.proposedYaml());
        assertEquals("Added notes.", reply.reply());
    }

    @Test
    void aPlainTextReplyIsReturnedWithoutValidation() {
        ScriptedAgentService service = new ScriptedAgentService(new ModelClient.ModelReply("Which entity do you mean?", null));

        AgentReply reply = service.chat(request());

        assertEquals(1, service.calls.size());
        assertEquals("Which entity do you mean?", reply.reply());
    }

    @Test
    void anInvalidProposalIsSentBackWithTheIssuesForCorrection() {
        ScriptedAgentService service = new ScriptedAgentService(proposal("First try.", INVALID_YAML), proposal("Fixed.", VALID_YAML));

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
        ScriptedAgentService service = new ScriptedAgentService(proposal("Try 1.", INVALID_YAML), proposal("Try 2.", INVALID_YAML),
                proposal("Try 3.", INVALID_YAML), proposal("Try 4.", INVALID_YAML), proposal("Try 5.", INVALID_YAML));

        AgentReply reply = service.chat(request());

        assertEquals(1 + org.eclipse.dirigible.components.intent.ai.ProposalRepairLoop.MAX_REPAIR_ROUNDS, service.calls.size(),
                "one initial call plus exactly the bounded repair rounds");
        assertEquals(INVALID_YAML, reply.proposedYaml(), "the last proposal is still returned for the editor to flag");
        assertTrue(reply.reply()
                        .contains("still fails intent validation"),
                "the outstanding issues are appended to the reply text");
    }

    @Test
    void reportedBoundariesReachTheEditorAsStructure() {
        // The honesty contract: a requirement the DSL cannot express arrives as data the editor can
        // render distinctly and the developer can forward - never buried in prose that reads like the
        // rest of the answer.
        JsonObject input = new JsonObject();
        input.addProperty("explanation", "Modelled the fine; the driver lookup is not expressible.");
        input.addProperty("yaml", VALID_YAML);
        JsonArray boundaries = new JsonArray();
        JsonObject boundary = new JsonObject();
        boundary.addProperty("requirement", "automatically identify the driver from the validity register");
        boundary.addProperty("explanation", "a period lookup with a matching rule is an algorithm, not a model");
        boundary.addProperty("extensionKind", "delegate");
        boundary.addProperty("suggestedClass", "IdentifyDriver");
        boundaries.add(boundary);
        JsonObject malformed = new JsonObject();
        malformed.addProperty("explanation", "no requirement named");
        boundaries.add(malformed);
        input.add("boundaries", boundaries);

        ScriptedAgentService service = new ScriptedAgentService(new ModelClient.ModelReply("Here you go.", input));

        AgentReply reply = service.chat(request());

        assertEquals(1, reply.boundaries()
                             .size(),
                "a malformed entry is dropped rather than failing the turn");
        AgentBoundary reported = reply.boundaries()
                                      .get(0);
        assertEquals("automatically identify the driver from the validity register", reported.requirement());
        assertEquals("delegate", reported.extensionKind());
        assertEquals("IdentifyDriver", reported.suggestedClass());
    }

    @Test
    void aProposalWithoutBoundariesReportsNone() {
        ScriptedAgentService service = new ScriptedAgentService(proposal("Added notes.", VALID_YAML));

        assertTrue(service.chat(request())
                          .boundaries()
                          .isEmpty());
    }

    @Test
    void theCoverageAuditReachesTheEditorAsStructure() {
        // The completeness contract (dirigible #6997): the requirement-by-requirement mapping arrives
        // as data, a malformed entry is dropped, and an uncovered requirement is impossible to miss -
        // it is appended to the reply the way outstanding validation issues are.
        JsonObject input = new JsonObject();
        input.addProperty("explanation", "Added the notes field.");
        input.addProperty("yaml", VALID_YAML);
        JsonArray coverage = new JsonArray();
        JsonObject covered = new JsonObject();
        covered.addProperty("requirement", "members carry free-text notes");
        covered.addProperty("construct", "entities: Member (fields: notes)");
        coverage.add(covered);
        JsonObject uncovered = new JsonObject();
        uncovered.addProperty("requirement", "every note edit is recorded in an audit log");
        uncovered.addProperty("construct", "none");
        coverage.add(uncovered);
        JsonObject malformed = new JsonObject();
        malformed.addProperty("construct", "no requirement named");
        coverage.add(malformed);
        input.add("coverage", coverage);

        ScriptedAgentService service = new ScriptedAgentService(new ModelClient.ModelReply("Here you go.", input));

        AgentReply reply = service.chat(request());

        assertEquals(2, reply.coverage()
                             .size(),
                "a malformed entry is dropped rather than failing the turn");
        assertEquals("entities: Member (fields: notes)", reply.coverage()
                                                              .get(0)
                                                              .construct());
        assertTrue(reply.reply()
                        .contains("NOT carried by it"),
                "the uncovered requirement is announced in the reply");
        assertTrue(reply.reply()
                        .contains("every note edit is recorded in an audit log"));
    }

    @Test
    void aFullyCoveredProposalAppendsNoWarning() {
        JsonObject input = new JsonObject();
        input.addProperty("explanation", "Added notes.");
        input.addProperty("yaml", VALID_YAML);
        JsonArray coverage = new JsonArray();
        JsonObject covered = new JsonObject();
        covered.addProperty("requirement", "members carry free-text notes");
        covered.addProperty("construct", "entities: Member (fields: notes)");
        coverage.add(covered);
        input.add("coverage", coverage);

        ScriptedAgentService service = new ScriptedAgentService(new ModelClient.ModelReply("Added notes.", input));

        AgentReply reply = service.chat(request());

        assertEquals(1, reply.coverage()
                             .size());
        assertEquals("Added notes.", reply.reply(), "a clean audit adds nothing to the reply");
    }

    @Test
    void aReplyWithoutACoverageAuditStillAnswers() {
        // An older transcript replayed against the new contract, or a plain-text answer: absence is
        // an empty list, never a failure.
        ScriptedAgentService service = new ScriptedAgentService(proposal("Added notes.", VALID_YAML));

        AgentReply reply = service.chat(request());

        assertTrue(reply.coverage()
                        .isEmpty());
        assertEquals("Added notes.", reply.reply());
    }

    @Test
    void aProposalThatParsesButFailsGenerationIsSentBackForCorrection() {
        // The band the parser cannot see (dirigible #6956): the proposal is structurally valid, but the
        // generation pass would refuse or drop a piece of it. The dry run's issues must reach the
        // repair loop exactly as a parse error does.
        String generationIssue = "generates [audit-log] map [Vehicle] does not resolve against the source - not generated";
        ScriptedGenerationService generation = new ScriptedGenerationService(List.of(List.of(generationIssue)));
        ScriptedAgentService service =
                new ScriptedAgentService(generation, proposal("First try.", VALID_YAML), proposal("Fixed.", VALID_YAML));

        AgentReply reply = service.chat(request());

        assertEquals(2, service.calls.size(), "the generation-layer issue triggers a repair round");
        assertEquals(VALID_YAML, reply.proposedYaml());
        List<Map<String, Object>> secondCall = service.calls.get(1);
        String repairTurn = (String) secondCall.get(secondCall.size() - 1)
                                               .get("content");
        assertTrue(repairTurn.contains(generationIssue), "the dry run's issue text is sent back verbatim");
    }

    @Test
    void aDryRunInfrastructureFailureDoesNotFailTheTurn() {
        // The dry run judging the proposal is a bonus check on top of the parse - its own machinery
        // breaking must degrade to the parse verdict, never to a failed turn.
        ScriptedGenerationService generation = ScriptedGenerationService.failing(new IllegalStateException("no repository"));
        ScriptedAgentService service = new ScriptedAgentService(generation, proposal("Added notes.", VALID_YAML));

        AgentReply reply = service.chat(request());

        assertEquals(1, service.calls.size());
        assertEquals(VALID_YAML, reply.proposedYaml());
        assertEquals("Added notes.", reply.reply(), "no validation note is appended");
    }

    private static ModelClient.ModelReply proposal(String text, String yaml) {
        JsonObject input = new JsonObject();
        input.addProperty("explanation", text);
        input.addProperty("yaml", yaml);
        return new ModelClient.ModelReply(text, input);
    }

    private static AgentRequest request() {
        return new AgentRequest("name: lib\n", "Add a notes field", List.of());
    }

    /** Overrides the upstream call with a scripted sequence of replies, recording each call's turns. */
    private static final class ScriptedAgentService extends IntentAgentService {

        private final Deque<ModelClient.ModelReply> replies;
        private final List<List<Map<String, Object>>> calls = new ArrayList<>();

        private ScriptedAgentService(ModelClient.ModelReply... scripted) {
            this(new ScriptedGenerationService(), scripted);
        }

        private ScriptedAgentService(IntentGenerationService generationService, ModelClient.ModelReply... scripted) {
            super(new ModelClient(), generationService);
            this.replies = new ArrayDeque<>(List.of(scripted));
        }

        @Override
        ModelClient.ModelReply callModel(List<Map<String, Object>> messages) {
            calls.add(List.copyOf(messages));
            return replies.pop();
        }
    }

    /**
     * Scripts the dry generation pass the same way the upstream model is scripted: each parseable
     * proposal consumes the next issue list (empty once the script runs out). The real dry run is
     * exercised by {@code IntentGenerationServiceDryRunTest}; here only the loop's wiring is under
     * test.
     */
    private static final class ScriptedGenerationService extends IntentGenerationService {

        private final Deque<List<String>> scriptedIssues;
        private RuntimeException failure;

        private ScriptedGenerationService(List<List<String>> scripted) {
            super(List.of(), null, null);
            this.scriptedIssues = new ArrayDeque<>(scripted);
        }

        private ScriptedGenerationService() {
            this(List.of());
        }

        private static ScriptedGenerationService failing(RuntimeException failure) {
            ScriptedGenerationService service = new ScriptedGenerationService();
            service.failure = failure;
            return service;
        }

        @Override
        public List<String> dryRun(String yaml) {
            if (failure != null) {
                throw failure;
            }
            return scriptedIssues.isEmpty() ? List.of() : scriptedIssues.pop();
        }
    }
}
