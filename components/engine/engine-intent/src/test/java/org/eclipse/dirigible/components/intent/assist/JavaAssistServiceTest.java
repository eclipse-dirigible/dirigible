/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.assist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.intent.ai.ModelClient;
import org.eclipse.dirigible.engine.java.runtime.JavaSourceCompiler;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

/**
 * The Java assistant's contract: a proposal is really compiled - together with the project's other
 * sources - before the developer sees it, a proposal that does not compile is sent back with
 * javac's own errors, and the loop is bounded. The upstream model is scripted, so no key and no
 * network are involved.
 */
class JavaAssistServiceTest {

    /** A sibling the proposals below reference; only in the batch does that reference resolve. */
    private static final ProjectSource RATES = new ProjectSource("custom/Rates.java", "custom.Rates", """
            package custom;

            public class Rates {
                public static int base() {
                    return 7;
                }
            }
            """);

    private static final String COMPILES = """
            package custom;

            public class Fees {
                public int fee() {
                    return Rates.base() * 2;
                }
            }
            """;

    private static final String DOES_NOT_COMPILE = """
            package custom;

            public class Fees {
                public int fee() {
                    return Rates.missing() * 2;
                }
            }
            """;

    @Test
    void aCompilingFirstProposalIsReturnedWithoutARepairRound() {
        ScriptedAssistService service = new ScriptedAssistService(proposal("Doubles the base rate.", COMPILES));

        AssistReply reply = service.chat(context(), "Charge twice the base rate", List.of());

        assertEquals(1, service.calls.size());
        assertEquals(COMPILES, reply.proposedSource());
        assertEquals("Doubles the base rate.", reply.reply());
        assertTrue(reply.diagnostics()
                        .isEmpty(),
                "a compiling proposal carries no diagnostics");
    }

    @Test
    void theProposalIsCompiledTogetherWithTheProjectsOtherSources() {
        ScriptedAssistService withSibling = new ScriptedAssistService(proposal("Uses the rate.", COMPILES));
        withSibling.chat(context(), "Use the base rate", List.of());
        assertEquals(1, withSibling.calls.size(), "the reference resolves against the sibling source");

        // The very same proposal, with the sibling withheld: the reference no longer resolves, so the
        // assistant sees a compile error and asks for a correction. This is what makes batching
        // load-bearing rather than incidental - a custom class exists to use the generated ones.
        ScriptedAssistService alone = new ScriptedAssistService(proposal("Uses the rate.", COMPILES), proposal("Still uses it.", COMPILES),
                proposal("And again.", COMPILES), proposal("Once more.", COMPILES), proposal("Final try.", COMPILES));
        AssistContext lonely = new AssistContext("orders", "custom/Fees.java", "", null, List.of());

        AssistReply reply = alone.chat(lonely, "Use the base rate", List.of());

        assertTrue(alone.calls.size() > 1, "an unresolvable reference is sent back for repair");
        assertFalse(reply.diagnostics()
                         .isEmpty(),
                "and the outstanding compiler errors reach the developer");
    }

    @Test
    void aProposalThatDoesNotCompileIsSentBackWithJavacsOwnErrors() {
        ScriptedAssistService service = new ScriptedAssistService(proposal("First try.", DOES_NOT_COMPILE), proposal("Fixed.", COMPILES));

        AssistReply reply = service.chat(context(), "Charge twice the base rate", List.of());

        assertEquals(2, service.calls.size());
        assertEquals(COMPILES, reply.proposedSource());
        assertTrue(reply.diagnostics()
                        .isEmpty());

        List<Map<String, Object>> secondCall = service.calls.get(1);
        Map<String, Object> echo = secondCall.get(secondCall.size() - 2);
        Map<String, Object> repair = secondCall.get(secondCall.size() - 1);
        assertEquals("assistant", echo.get("role"));
        assertEquals("user", repair.get("role"));
        assertTrue(((String) echo.get("content")).contains(DOES_NOT_COMPILE), "the failed proposal is replayed as the assistant turn");
        assertTrue(((String) repair.get("content")).contains("cannot find symbol"), "javac's message is sent back verbatim");
        assertTrue(((String) repair.get("content")).contains("corrected COMPLETE source"),
                "the repair instruction asks for a full re-proposal");
    }

    @Test
    void repairRoundsAreBoundedAndTheOutstandingErrorsAreSurfaced() {
        ScriptedAssistService service =
                new ScriptedAssistService(proposal("Try 1.", DOES_NOT_COMPILE), proposal("Try 2.", DOES_NOT_COMPILE),
                        proposal("Try 3.", DOES_NOT_COMPILE), proposal("Try 4.", DOES_NOT_COMPILE), proposal("Try 5.", DOES_NOT_COMPILE));

        AssistReply reply = service.chat(context(), "Charge twice the base rate", List.of());

        assertEquals(1 + org.eclipse.dirigible.components.intent.ai.ProposalRepairLoop.MAX_REPAIR_ROUNDS, service.calls.size(),
                "one initial call plus exactly the bounded repair rounds");
        assertEquals(DOES_NOT_COMPILE, reply.proposedSource(), "the last proposal is still returned for the developer to judge");
        assertFalse(reply.diagnostics()
                         .isEmpty());
        assertTrue(reply.diagnostics()
                        .get(0)
                        .line() > 0,
                "a compiler error is positioned so the view can render it at its line");
        assertTrue(reply.reply()
                        .contains("does not compile yet"),
                "the outstanding errors are named in the reply text");
    }

    @Test
    void aPlainTextReplyProposesNothingAndIsNotCompiled() {
        ScriptedAssistService service = new ScriptedAssistService(new ModelClient.ModelReply("Which rate do you mean?", null));

        AssistReply reply = service.chat(context(), "Change the rate", List.of());

        assertEquals(1, service.calls.size());
        assertNull(reply.proposedSource());
        assertEquals("Which rate do you mean?", reply.reply());
        assertTrue(reply.diagnostics()
                        .isEmpty());
    }

    @Test
    void theUserTurnCarriesTheFileTheModelAndTheProjectsTypes() {
        ScriptedAssistService service = new ScriptedAssistService(proposal("Doubles the base rate.", COMPILES));

        service.chat(context(), "Charge twice the base rate", List.of());

        String turn = (String) service.calls.get(0)
                                            .get(0)
                                            .get("content");
        assertTrue(turn.contains("custom/Fees.java"), "the file being worked on is named");
        assertTrue(turn.contains("name: orders"), "the application model reaches the assistant");
        assertTrue(turn.contains("custom.Rates"), "and so does what the project offers to import");
        assertTrue(turn.contains("Charge twice the base rate"), "the developer's request is the last thing said");
    }

    private static AssistContext context() {
        return new AssistContext("orders", "custom/Fees.java", "package custom;\n\npublic class Fees {\n}\n", "name: orders\n",
                List.of(RATES));
    }

    private static ModelClient.ModelReply proposal(String text, String source) {
        JsonObject input = new JsonObject();
        input.addProperty("explanation", text);
        input.addProperty("source", source);
        return new ModelClient.ModelReply(text, input);
    }

    /** Overrides the upstream call with a scripted sequence of replies, recording each call's turns. */
    private static final class ScriptedAssistService extends JavaAssistService {

        private final Deque<ModelClient.ModelReply> replies;
        private final List<List<Map<String, Object>>> calls = new ArrayList<>();

        private ScriptedAssistService(ModelClient.ModelReply... scripted) {
            super(new ModelClient(), new JavaSourceCompiler());
            this.replies = new ArrayDeque<>(List.of(scripted));
        }

        @Override
        ModelClient.ModelReply callModel(List<Map<String, Object>> messages) {
            calls.add(List.copyOf(messages));
            return replies.pop();
        }
    }
}
