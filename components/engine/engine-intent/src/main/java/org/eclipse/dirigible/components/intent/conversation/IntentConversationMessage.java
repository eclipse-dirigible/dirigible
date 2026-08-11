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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * One message of an {@link IntentConversation}.
 *
 * <p>
 * <b>Append-only.</b> The authoring flow never updates or deletes a message, which is what makes
 * the history usable as an audit trail: a support engineer can reconstruct exactly which request
 * produced which assistant proposal, who made it and when. There is intentionally no setter for
 * anything but the generated id.
 */
@Entity
@Table(name = "DIRIGIBLE_INTENT_CONVERSATION_MESSAGES")
public class IntentConversationMessage {

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "INTENTMESSAGE_ID", columnDefinition = "BIGINT", nullable = false)
    private Long id;

    /** The conversation this message belongs to. */
    @Column(name = "INTENTMESSAGE_CONVERSATION_ID", columnDefinition = "BIGINT", nullable = false)
    private Long conversationId;

    /** The message's position in the conversation, starting at 0. Unique within the conversation. */
    @Column(name = "INTENTMESSAGE_SEQUENCE", columnDefinition = "INT", nullable = false)
    private int sequence;

    /** What this message is. */
    @Column(name = "INTENTMESSAGE_ROLE", columnDefinition = "VARCHAR", nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private ConversationRole role;

    /** The message text. Unbounded - an assistant explanation can be long. */
    @Column(name = "INTENTMESSAGE_CONTENT", columnDefinition = "CLOB")
    private String content;

    /** The logged-in user this message is attributed to. */
    @Column(name = "INTENTMESSAGE_CREATED_BY", columnDefinition = "VARCHAR", nullable = false, length = 255)
    private String createdBy;

    /** When the message was appended. */
    @Column(name = "INTENTMESSAGE_CREATED_AT", columnDefinition = "TIMESTAMP", nullable = false)
    private Timestamp createdAt;

    /**
     * Instantiates a new intent conversation message. Required by JPA.
     */
    IntentConversationMessage() {}

    /**
     * Instantiates a new intent conversation message.
     *
     * @param conversationId the owning conversation
     * @param sequence the position in the conversation
     * @param role what the message is
     * @param content the message text
     * @param createdBy the authoring user
     * @param createdAt the append timestamp
     */
    IntentConversationMessage(Long conversationId, int sequence, ConversationRole role, String content, String createdBy,
            Timestamp createdAt) {
        this.conversationId = conversationId;
        this.sequence = sequence;
        this.role = role;
        this.content = content;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Gets the owning conversation.
     *
     * @return the conversation id
     */
    public Long getConversationId() {
        return conversationId;
    }

    /**
     * Gets the position in the conversation.
     *
     * @return the sequence
     */
    public int getSequence() {
        return sequence;
    }

    /**
     * Gets what the message is.
     *
     * @return the role
     */
    public ConversationRole getRole() {
        return role;
    }

    /**
     * Gets the message text.
     *
     * @return the content
     */
    public String getContent() {
        return content;
    }

    /**
     * Gets the authoring user.
     *
     * @return the user name
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     * Gets the append timestamp.
     *
     * @return the append timestamp
     */
    public Timestamp getCreatedAt() {
        return createdAt;
    }
}
