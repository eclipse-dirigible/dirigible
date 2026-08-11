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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.dirigible.components.intent.conversation.ConversationTranscript.Turn;
import org.junit.jupiter.api.Test;

/**
 * The stored conversation is the whole record of what happened; the transcript is only the part
 * that may be replayed to the model. These tests pin that difference - a restored conversation
 * whose transcript is not strictly alternating makes the very next request fail upstream.
 */
class ConversationTranscriptTest {

    @Test
    void a_clean_dialogue_is_the_transcript() {
        assertEquals(List.of(new Turn(ConversationRole.USER, "build an order app"), new Turn(ConversationRole.ASSISTANT, "here it is")),
                ConversationTranscript.of(
                        conversation(ConversationRole.USER, "build an order app", ConversationRole.ASSISTANT, "here it is")));
    }

    @Test
    void display_only_notes_and_errors_are_never_replayed() {
        // The note and the error stay in the history - they are what the developer saw - but the model
        // never said them, so replaying them would put words in its mouth.
        assertEquals(List.of(new Turn(ConversationRole.USER, "ask"), new Turn(ConversationRole.ASSISTANT, "answer")),
                ConversationTranscript.of(conversation(ConversationRole.USER, "ask", ConversationRole.ASSISTANT, "answer",
                        ConversationRole.NOTE, "Applied to the editor", ConversationRole.ERROR, "That change does not validate")));
    }

    @Test
    void a_failed_turn_is_kept_in_the_history_but_dropped_from_the_transcript() {
        // The whole reason this class is not a role filter: the assistant never answered "ask again", so
        // replaying it would send two consecutive user turns and the model API rejects that.
        assertEquals(List.of(new Turn(ConversationRole.USER, "ask"), new Turn(ConversationRole.ASSISTANT, "answer")),
                ConversationTranscript.of(conversation(ConversationRole.USER, "ask", ConversationRole.ASSISTANT, "answer",
                        ConversationRole.USER, "ask again", ConversationRole.ERROR, "The assistant could not be reached")));
    }

    @Test
    void a_trailing_unanswered_message_is_dropped_even_with_no_error_beside_it() {
        assertEquals(List.of(), ConversationTranscript.of(conversation(ConversationRole.USER, "ask")));
    }

    @Test
    void the_transcript_always_opens_with_the_developer_and_never_repeats_a_role() {
        // Defensive: neither shape can be produced by the two surfaces today, but the transcript's
        // contract is the model API's, not the surfaces'.
        assertEquals(List.of(new Turn(ConversationRole.USER, "ask"), new Turn(ConversationRole.ASSISTANT, "first answer")),
                ConversationTranscript.of(conversation(ConversationRole.ASSISTANT, "unprompted", ConversationRole.USER, "ask",
                        ConversationRole.ASSISTANT, "first answer", ConversationRole.ASSISTANT, "second answer")));
    }

    @Test
    void an_empty_conversation_has_an_empty_transcript() {
        assertEquals(List.of(), ConversationTranscript.of(List.of()));
    }

    /** Builds stored messages from alternating role/content arguments. */
    private static List<IntentConversationMessage> conversation(Object... roleThenContent) {
        Timestamp now = Timestamp.from(Instant.EPOCH);
        List<IntentConversationMessage> messages = new ArrayList<>(roleThenContent.length / 2);
        for (int i = 0; i < roleThenContent.length; i += 2) {
            messages.add(new IntentConversationMessage(1L, i / 2, (ConversationRole) roleThenContent[i], (String) roleThenContent[i + 1],
                    "admin", now));
        }
        return messages;
    }
}
