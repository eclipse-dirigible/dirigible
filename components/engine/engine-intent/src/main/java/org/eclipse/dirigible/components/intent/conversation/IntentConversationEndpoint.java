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

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import jakarta.annotation.security.RolesAllowed;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.intent.LoggedValue;
import org.eclipse.dirigible.components.intent.conversation.IntentConversationService.ConversationKey;
import org.eclipse.dirigible.components.intent.conversation.IntentConversationService.MessageDraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST surface for the AI conversations that produce and evolve an application - the record of
 * <em>why</em> an app looks the way it does.
 * <p>
 * Both operations take the same three request parameters - {@code project}, {@code surface} and
 * {@code path} - which together with the caller's tenant identify one conversation:
 * <ul>
 * <li>{@code GET /services/ide/intent/conversations} - the conversation to restore when the app is
 * opened, messages included. Answers {@code 200} with a {@code null} id and an empty message list
 * when nothing has been said yet, so a client never has to treat "no history" as an error.</li>
 * <li>{@code POST /services/ide/intent/conversations/messages} - append the messages of a completed
 * turn, starting the conversation when this is its first. Append-only: there is no update and no
 * delete here, by design.</li>
 * </ul>
 *
 * <p>
 * Same access rules as the rest of the intent family, and every operation is scoped to the caller's
 * tenant inside {@link IntentConversationService}.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_IDE + "intent/conversations")
@RolesAllowed({"ADMINISTRATOR", "DEVELOPER"})
public class IntentConversationEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntentConversationEndpoint.class);

    /** The project name column's ceiling - a longer value is a client bug, not a storage problem. */
    private static final int MAX_PROJECT_LENGTH = 255;

    /** The intent file path column's ceiling. */
    private static final int MAX_PATH_LENGTH = 512;

    private final IntentConversationService conversationService;

    /**
     * Instantiates a new intent conversation endpoint.
     *
     * @param conversationService the conversation service
     */
    public IntentConversationEndpoint(IntentConversationService conversationService) {
        this.conversationService = conversationService;
    }

    /**
     * The conversation of one application in one surface, for restore after login on any device.
     *
     * @param project the workspace project
     * @param surface the authoring surface, {@code builder} or {@code intent-editor}
     * @param path the intent file path within the project
     * @return the conversation and its messages; an empty conversation when there is no history yet
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ConversationView> get(@RequestParam("project") String project, @RequestParam("surface") String surface,
            @RequestParam("path") String path) {
        ConversationKey key = key(project, surface, path);
        Optional<IntentConversation> conversation = conversationService.find(key);
        if (conversation.isEmpty()) {
            return ResponseEntity.ok(ConversationView.empty(key));
        }
        List<IntentConversationMessage> stored = conversationService.messagesOf(conversation.get());
        List<MessageView> messages = stored.stream()
                                           .map(MessageView::of)
                                           .toList();
        return ResponseEntity.ok(ConversationView.of(conversation.get(), messages, ConversationTranscript.of(stored)));
    }

    /**
     * Appends the messages of a completed turn.
     *
     * @param project the workspace project
     * @param surface the authoring surface, {@code builder} or {@code intent-editor}
     * @param path the intent file path within the project
     * @param request the messages to append, in order
     * @return the conversation id and how many messages it now holds
     */
    @PostMapping(value = "/messages", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AppendResult> append(@RequestParam("project") String project, @RequestParam("surface") String surface,
            @RequestParam("path") String path, @RequestBody AppendRequest request) {
        ConversationKey key = key(project, surface, path);
        List<MessageDraft> drafts = drafts(request);
        try {
            IntentConversation conversation = conversationService.append(key, drafts);
            return ResponseEntity.ok(new AppendResult(conversation.getId(), drafts.size()));
        } catch (DataIntegrityViolationException e) {
            // Two clients extended the same conversation at the same instant and the database arbitrated.
            // Nothing was appended, and the loser's client keeps the turn pending, so its next append
            // carries it - reporting the conflict is therefore enough.
            LOGGER.warn("Concurrent append to the [{}] conversation of project [{}] was rejected", LoggedValue.of(surface),
                    LoggedValue.of(project), e);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The conversation was extended concurrently. Please retry.", e);
        }
    }

    /**
     * Validates the request parameters and builds the conversation key.
     *
     * @param project the workspace project
     * @param surface the authoring surface
     * @param path the intent file path within the project
     * @return the conversation key
     */
    private static ConversationKey key(String project, String surface, String path) {
        require(project, "project", MAX_PROJECT_LENGTH);
        require(path, "path", MAX_PATH_LENGTH);
        try {
            return new ConversationKey(project, ConversationSurface.of(surface), path);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }

    /**
     * Ensures a request parameter carries a value that fits its column.
     *
     * @param value the value
     * @param name the parameter name, for the failure message
     * @param maxLength the column's ceiling
     */
    private static void require(String value, String name, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The [" + name + "] parameter is required");
        }
        if (value.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The [" + name + "] parameter must not exceed " + maxLength + " characters");
        }
    }

    /**
     * Validates and converts the requested messages.
     *
     * @param request the append request
     * @return the message drafts
     */
    private static List<MessageDraft> drafts(AppendRequest request) {
        List<MessageInput> inputs = request == null ? null : request.messages();
        if (inputs == null || inputs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one message is required");
        }
        return inputs.stream()
                     .map(input -> {
                         if (input == null || input.role() == null || input.content() == null) {
                             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Every message needs a role and a content");
                         }
                         return new MessageDraft(input.role(), input.content());
                     })
                     .toList();
    }

    /**
     * ISO-8601 rendering of a stored timestamp. Explicit, so the payload does not depend on how the
     * application's Jackson happens to be configured for temporal types.
     *
     * @param timestamp the timestamp
     * @return the ISO-8601 instant, or null
     */
    private static String iso(Timestamp timestamp) {
        return timestamp == null ? null
                : timestamp.toInstant()
                           .toString();
    }

    /**
     * The messages of one turn.
     *
     * @param messages the messages, in order
     */
    record AppendRequest(List<MessageInput> messages) {
    }

    /**
     * One message a client wants appended. Authorship and timing are stamped server-side.
     *
     * @param role what the message is
     * @param content the message text
     */
    record MessageInput(ConversationRole role, String content) {
    }

    /**
     * The outcome of an append.
     *
     * @param id the conversation id
     * @param appended how many messages were appended
     */
    record AppendResult(Long id, int appended) {
    }

    /**
     * A conversation as the clients restore it: everything that was said, plus the transcript to send
     * upstream on the next turn. The two are one payload on purpose - a client that had to derive the
     * transcript itself would have to know that a failed turn's unanswered message must not be
     * replayed, which is a property of the stored roles and not of any surface (see
     * {@link ConversationTranscript}).
     *
     * @param id the conversation id, null when there is no history yet
     * @param project the workspace project
     * @param surface the authoring surface
     * @param path the intent file path within the project
     * @param createdAt when the conversation started
     * @param updatedAt when a message was last appended
     * @param messages everything that was said, in order - display and record
     * @param turns the alternating user/assistant transcript to send upstream
     */
    record ConversationView(Long id, String project, String surface, String path, String createdAt, String updatedAt,
            List<MessageView> messages, List<ConversationTranscript.Turn> turns) {

        /**
         * The answer for an application nothing has been said about yet.
         *
         * @param key the conversation key
         * @return the empty view
         */
        static ConversationView empty(ConversationKey key) {
            return new ConversationView(null, key.project(), key.surface()
                                                                .wireName(),
                    key.path(), null, null, List.of(), List.of());
        }

        /**
         * The stored conversation.
         *
         * @param conversation the conversation
         * @param messages its messages
         * @param turns its upstream transcript
         * @return the view
         */
        static ConversationView of(IntentConversation conversation, List<MessageView> messages, List<ConversationTranscript.Turn> turns) {
            return new ConversationView(conversation.getId(), conversation.getProject(), conversation.getSurface(), conversation.getPath(),
                    iso(conversation.getCreatedAt()), iso(conversation.getUpdatedAt()), messages, turns);
        }
    }

    /**
     * One stored message.
     *
     * @param sequence the position in the conversation
     * @param role what the message is
     * @param content the message text
     * @param createdBy the authoring user
     * @param createdAt when it was appended
     */
    record MessageView(int sequence, ConversationRole role, String content, String createdBy, String createdAt) {

        /**
         * The stored message.
         *
         * @param message the message
         * @return the view
         */
        static MessageView of(IntentConversationMessage message) {
            return new MessageView(message.getSequence(), message.getRole(), message.getContent(), message.getCreatedBy(),
                    iso(message.getCreatedAt()));
        }
    }
}
