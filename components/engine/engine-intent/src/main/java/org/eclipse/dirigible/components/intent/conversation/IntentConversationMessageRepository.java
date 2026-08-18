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

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * The Interface IntentConversationMessageRepository.
 */
@Repository("intentConversationMessageRepository")
public interface IntentConversationMessageRepository extends JpaRepository<IntentConversationMessage, Long> {

    /**
     * The conversation's messages, in the order they were appended.
     *
     * @param conversationId the owning conversation
     * @return the messages
     */
    List<IntentConversationMessage> findByConversationIdOrderBySequenceAsc(Long conversationId);

    /**
     * How many messages the conversation already holds - the sequence the next append starts at.
     *
     * @param conversationId the owning conversation
     * @return the message count
     */
    int countByConversationId(Long conversationId);
}
