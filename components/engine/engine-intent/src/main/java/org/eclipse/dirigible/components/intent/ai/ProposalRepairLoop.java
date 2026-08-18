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

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The bounded validate-and-repair loop every proposing assistant runs.
 *
 * <p>
 * A model's first draft is checked server-side by the same machinery that will judge it later - the
 * intent parser for a proposed {@code app.intent}, the Java compiler for a proposed {@code .java} -
 * <em>before</em> it reaches the developer. An invalid proposal is replayed to the model as its own
 * assistant turn together with the issues, and a corrected complete proposal is requested, at most
 * {@link #MAX_REPAIR_ROUNDS} times, so a stubbornly invalid proposal cannot spin the request
 * forever. After the last round the proposal is returned as-is with the outstanding issues, which
 * the caller is expected to surface rather than hide.
 */
public final class ProposalRepairLoop {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProposalRepairLoop.class);

    /**
     * How many times an invalid proposal is sent back to the model for correction. Bounds the loop at
     * {@code 1 + MAX_REPAIR_ROUNDS} upstream calls per turn.
     */
    public static final int MAX_REPAIR_ROUNDS = 2;

    /** One upstream round-trip over the accumulated turns. */
    @FunctionalInterface
    public interface Round {

        /**
         * Send the turns upstream.
         *
         * @param messages the conversation turns
         * @return the model's reply
         */
        ModelClient.ModelReply call(List<Map<String, Object>> messages);
    }

    /** Judges a proposal; an empty list accepts it. */
    @FunctionalInterface
    public interface Validator {

        /**
         * Validate a proposal.
         *
         * @param proposal the proposed content
         * @return the issues found, empty when the proposal is acceptable
         */
        List<String> issues(String proposal);
    }

    /**
     * The result of a turn.
     *
     * @param reply the last reply the model produced
     * @param proposal the proposed content, or {@code null} when the model proposed nothing
     * @param issues the issues still outstanding on {@code proposal} after the last round
     */
    public record Outcome(ModelClient.ModelReply reply, String proposal, List<String> issues) {
    }

    private final String proposalMember;
    private final String fence;
    private final Validator validator;
    private final Function<List<String>, String> repairPrompt;

    /**
     * Instantiates the loop for one kind of proposal.
     *
     * @param proposalMember the tool-input member carrying the proposed content, e.g. {@code yaml}
     * @param fence the markdown fence language the failed proposal is replayed in, e.g. {@code java}
     * @param validator judges a proposal server-side
     * @param repairPrompt renders the corrective user turn from the outstanding issues
     */
    public ProposalRepairLoop(String proposalMember, String fence, Validator validator, Function<List<String>, String> repairPrompt) {
        this.proposalMember = proposalMember;
        this.fence = fence;
        this.validator = validator;
        this.repairPrompt = repairPrompt;
    }

    /**
     * Run one assistant turn, repairing an invalid proposal up to {@link #MAX_REPAIR_ROUNDS} times. The
     * message list is extended in place with the replayed proposals and repair turns.
     *
     * @param messages the conversation turns, ending with the current user request
     * @param round the upstream call
     * @return the last reply, its proposal and any outstanding issues
     */
    public Outcome run(List<Map<String, Object>> messages, Round round) {
        ModelClient.ModelReply reply = round.call(messages);
        String proposal = reply.toolString(proposalMember);
        List<String> issues = validate(proposal);
        for (int repair = 1; !issues.isEmpty() && repair <= MAX_REPAIR_ROUNDS; repair++) {
            LOGGER.info("AI proposal failed validation with [{}] issue(s); requesting a correction (round [{}] of [{}])", issues.size(),
                    repair, MAX_REPAIR_ROUNDS);
            messages.add(Map.of("role", "assistant", "content", proposalTurn(reply.text(), proposal)));
            messages.add(Map.of("role", "user", "content", repairPrompt.apply(issues)));
            reply = round.call(messages);
            proposal = reply.toolString(proposalMember);
            issues = validate(proposal);
        }
        return new Outcome(reply, proposal, issues);
    }

    /**
     * Render outstanding issues as a bullet list, the shape both the repair turn and the
     * developer-facing note use.
     *
     * @param issues the issues
     * @return one {@code - issue} line each
     */
    public static String bulleted(List<String> issues) {
        StringBuilder sb = new StringBuilder();
        for (String issue : issues) {
            sb.append("- ")
              .append(issue)
              .append('\n');
        }
        return sb.toString();
    }

    /**
     * A reply that proposed nothing is a clarifying question or an answer - there is nothing to judge.
     */
    private List<String> validate(String proposal) {
        return proposal == null ? List.of() : validator.issues(proposal);
    }

    /** Replay a failed proposal as the assistant's turn, so the model can correct its own output. */
    private String proposalTurn(String text, String proposal) {
        return text + "\n\n```" + fence + "\n" + proposal + "\n```";
    }
}
