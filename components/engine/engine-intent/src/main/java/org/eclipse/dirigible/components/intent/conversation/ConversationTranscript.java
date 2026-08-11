/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.intent.conversation;

import java.util.ArrayList;
import java.util.List;

/**
 * Rebuilds the upstream transcript from a stored conversation - the strictly alternating
 * user/assistant dialogue the model API accepts as {@code history} on the next turn.
 *
 * <p>
 * It is not a plain filter on the role, and that distinction is the whole reason this class exists.
 * A <em>failed</em> turn stores the message the developer sent plus the error the client showed,
 * because "what was asked when it broke" is precisely what support needs - but that user message
 * was never answered, so replaying it would put two consecutive user turns in the next request and
 * the model API rejects that. So a user message counts only when an assistant answer follows it,
 * and an assistant message only when it has a kept user turn to answer.
 *
 * <p>
 * Deriving this once, next to the {@link ConversationRole} vocabulary it interprets, is deliberate:
 * the rule is a property of the stored roles rather than of any one surface, and both clients would
 * otherwise have to implement it identically in two unrelated UI stacks.
 */
final class ConversationTranscript {

    private ConversationTranscript() {}

    /**
     * The alternating user/assistant transcript of a conversation, in order.
     *
     * @param messages the stored messages, in order
     * @return the transcript
     */
    static List<Turn> of(List<IntentConversationMessage> messages) {
        List<IntentConversationMessage> dialogue = messages.stream()
                                                           .filter(message -> message.getRole() == ConversationRole.USER
                                                                   || message.getRole() == ConversationRole.ASSISTANT)
                                                           .toList();
        List<Turn> transcript = new ArrayList<>(dialogue.size());
        for (int i = 0; i < dialogue.size(); i++) {
            ConversationRole role = dialogue.get(i)
                                            .getRole();
            ConversationRole previous = transcript.isEmpty() ? null
                    : transcript.get(transcript.size() - 1)
                                .role();
            if (role == previous) {
                continue; // two of the same role in a row can never be sent
            }
            if (role == ConversationRole.USER && !isAnsweredAt(dialogue, i)) {
                continue; // a turn that failed - kept in the history, never replayed
            }
            if (role == ConversationRole.ASSISTANT && previous == null) {
                continue; // the dialogue has to open with the developer
            }
            transcript.add(new Turn(role, dialogue.get(i)
                                                  .getContent()));
        }
        return transcript;
    }

    /**
     * Whether the user message at the given position was answered by the assistant.
     *
     * @param dialogue the user/assistant messages, in order
     * @param index the position of the user message
     * @return true when an assistant answer follows
     */
    private static boolean isAnsweredAt(List<IntentConversationMessage> dialogue, int index) {
        return index + 1 < dialogue.size() && dialogue.get(index + 1)
                                                      .getRole() == ConversationRole.ASSISTANT;
    }

    /**
     * One turn of the upstream transcript.
     *
     * @param role who spoke
     * @param content what was said
     */
    record Turn(ConversationRole role, String content) {
    }
}
