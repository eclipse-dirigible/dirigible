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

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The bounded validate-and-repair loop every proposing assistant runs.
 *
 * <p>
 * A model's first draft is checked server-side by the same machinery that will judge it later - the
 * intent parser for a proposed {@code app.intent}, the Java compiler for a proposed {@code .java} -
 * <em>before</em> it reaches the developer. An invalid proposal is replayed to the model as its own
 * assistant turn together with the issues, and a correction is requested, at most
 * {@link #MAX_REPAIR_ROUNDS} times, so a stubbornly invalid proposal cannot spin the request
 * forever. After the last round the proposal is returned as-is with the outstanding issues, which
 * the caller is expected to surface rather than hide.
 *
 * <p>
 * A proposal does not have to arrive as the finished document. The {@link Extractor} turns a reply
 * into a {@link Proposal}, so a patch-shaped proposal (dirigible #6958) is materialized against the
 * current document there - and a patch that cannot be applied comes back as a refusal carrying its
 * own issues, which enters exactly the same repair round a validation failure does. That is the
 * point: an unusable proposal is corrected by the model, never half-applied and never shown to the
 * developer as if it were finished.
 */
public final class ProposalRepairLoop {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProposalRepairLoop.class);

    /**
     * How many times an invalid proposal is sent back to the model for correction. Bounds the loop at
     * {@code 1 + MAX_REPAIR_ROUNDS} upstream calls per turn.
     *
     * <p>
     * Raised from 2 once two things held (dirigible #6956): the upstream call is streamed with a
     * generous outer bound instead of one 120-second window (#6955), so more rounds no longer multiply
     * exposure to a wall-clock cliff; and the intent validator sees the generation layer as well as the
     * parse, so later rounds have genuinely new defects to fix rather than restating one parse error.
     * The loop still exits on the first clean round - four is a ceiling, not a target.
     */
    public static final int MAX_REPAIR_ROUNDS = 4;

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

    /**
     * A proposal, read off the model's reply.
     *
     * @param content the complete proposed content to validate and return, or {@code null} when the
     *        reply proposed nothing or the proposal could not be read
     * @param replay what to replay to the model as its own turn when a repair round is needed - the
     *        content itself for a whole-document proposal, the edits for a patch (the model corrects
     *        the edits it wrote, and re-anchors them on the unchanged current document)
     * @param replayFence the markdown fence language {@code replay} is replayed in, or {@code null} for
     *        the loop's own
     * @param issues why the proposal could not be read at all; empty when {@code content} stands
     */
    public record Proposal(String content, String replay, String replayFence, List<String> issues) {

        /** Nothing was proposed - a clarifying question or a plain answer. */
        public static Proposal none() {
            return new Proposal(null, null, null, List.of());
        }

        /** A whole-document proposal, replayed as itself. */
        public static Proposal of(String content) {
            return new Proposal(content, content, null, List.of());
        }

        /** A patch that applied, replayed as the edits the model wrote. */
        public static Proposal patched(String content, String edits) {
            return new Proposal(content, edits, "json", List.of());
        }

        /** A proposal that could not be read; the model is asked to correct it. */
        public static Proposal refused(String edits, List<String> issues) {
            return new Proposal(null, edits, "json", List.copyOf(issues));
        }
    }

    /** Reads the proposal out of a model reply. */
    @FunctionalInterface
    public interface Extractor {

        /**
         * Read the proposal.
         *
         * @param reply the model's reply
         * @return the proposal, possibly empty or refused
         */
        Proposal read(ModelClient.ModelReply reply);
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

    private final Extractor extractor;
    private final String fence;
    private final Validator validator;
    private final Function<List<String>, String> repairPrompt;

    /**
     * Instantiates the loop for a proposal that arrives as one whole-document tool argument.
     *
     * @param proposalMember the tool-input member carrying the proposed content, e.g. {@code yaml}
     * @param fence the markdown fence language the failed proposal is replayed in, e.g. {@code java}
     * @param validator judges a proposal server-side
     * @param repairPrompt renders the corrective user turn from the outstanding issues
     */
    public ProposalRepairLoop(String proposalMember, String fence, Validator validator, Function<List<String>, String> repairPrompt) {
        this(reply -> Proposal.of(reply.toolString(proposalMember)), fence, validator, repairPrompt);
    }

    /**
     * Instantiates the loop for one kind of proposal.
     *
     * @param extractor reads the proposal out of a reply, materializing it when it arrives as a patch
     * @param fence the markdown fence language the failed proposal is replayed in, e.g. {@code java}
     * @param validator judges a proposal server-side
     * @param repairPrompt renders the corrective user turn from the outstanding issues
     */
    public ProposalRepairLoop(Extractor extractor, String fence, Validator validator, Function<List<String>, String> repairPrompt) {
        this.extractor = extractor;
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
        Proposal proposal = extractor.read(reply);
        List<String> issues = validate(proposal);
        for (int repair = 1; !issues.isEmpty() && repair <= MAX_REPAIR_ROUNDS; repair++) {
            LOGGER.info("AI proposal failed validation with [{}] issue(s); requesting a correction (round [{}] of [{}])", issues.size(),
                    repair, MAX_REPAIR_ROUNDS);
            messages.add(Map.of("role", "assistant", "content", proposalTurn(reply.text(), proposal)));
            messages.add(Map.of("role", "user", "content", repairPrompt.apply(issues)));
            reply = round.call(messages);
            proposal = extractor.read(reply);
            issues = validate(proposal);
        }
        return new Outcome(reply, proposal.content(), issues);
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
     * A proposal that could not be read carries its own reasons; one that proposed nothing is a
     * clarifying question or an answer - there is nothing to judge.
     */
    private List<String> validate(Proposal proposal) {
        if (!proposal.issues()
                     .isEmpty()) {
            return proposal.issues();
        }
        return proposal.content() == null ? List.of() : validator.issues(proposal.content());
    }

    /** Replay a failed proposal as the assistant's turn, so the model can correct its own output. */
    private String proposalTurn(String text, Proposal proposal) {
        String explanation = StringUtils.defaultIfBlank(text, "(no explanation)");
        if (proposal.replay() == null) {
            return explanation;
        }
        String language = StringUtils.defaultIfBlank(proposal.replayFence(), fence);
        return explanation + "\n\n```" + language + "\n" + proposal.replay() + "\n```";
    }
}
