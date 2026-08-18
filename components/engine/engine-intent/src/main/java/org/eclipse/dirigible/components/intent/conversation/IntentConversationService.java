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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.dirigible.components.api.security.UserFacade;
import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stores and restores the AI conversations that produce and evolve an application.
 *
 * <p>
 * Every operation is scoped to the current tenant - the tenant id is stamped on write and is part
 * of every lookup, so one tenant can neither read nor extend another's conversation. Messages are
 * only ever appended: this is the audit trail that lets support reconstruct which assistant
 * proposal introduced a change, in response to what request, and when.
 */
@Service
public class IntentConversationService {

    /** The conversation repository. */
    private final IntentConversationRepository conversations;

    /** The message repository. */
    private final IntentConversationMessageRepository messages;

    /** The tenant context. */
    private final TenantContext tenantContext;

    /** The default tenant. */
    private final Tenant defaultTenant;

    /**
     * Instantiates a new intent conversation service.
     *
     * @param conversations the conversation repository
     * @param messages the message repository
     * @param tenantContext the tenant context
     * @param defaultTenant the default tenant
     */
    public IntentConversationService(IntentConversationRepository conversations, IntentConversationMessageRepository messages,
            TenantContext tenantContext, @DefaultTenant Tenant defaultTenant) {
        this.conversations = conversations;
        this.messages = messages;
        this.tenantContext = tenantContext;
        this.defaultTenant = defaultTenant;
    }

    /**
     * The current tenant's conversation for one application in one surface, or empty when nothing has
     * been said yet.
     *
     * @param key the conversation key
     * @return the conversation, when there is one
     */
    @Transactional(readOnly = true)
    public Optional<IntentConversation> find(ConversationKey key) {
        return lookup(key);
    }

    /**
     * The conversation's messages, in the order they were appended.
     *
     * @param conversation the conversation
     * @return the messages
     */
    @Transactional(readOnly = true)
    public List<IntentConversationMessage> messagesOf(IntentConversation conversation) {
        return messages.findByConversationIdOrderBySequenceAsc(conversation.getId());
    }

    /**
     * Appends messages to the current tenant's conversation for one application in one surface,
     * starting the conversation when this is its first message. Each message is attributed to the
     * logged-in user and stamped with the append time.
     *
     * @param key the conversation key
     * @param drafts the messages to append, in order
     * @return the conversation the messages were appended to
     */
    @Transactional
    public IntentConversation append(ConversationKey key, List<MessageDraft> drafts) {
        Timestamp now = Timestamp.from(Instant.now());
        IntentConversation conversation = lookup(key).orElseGet(() -> conversations
                                                                                   .saveAndFlush(new IntentConversation(currentTenantId(),
                                                                                           key.project(), key.surface()
                                                                                                             .wireName(),
                                                                                           key.path(), now)));

        String user = UserFacade.getName();
        int sequence = messages.countByConversationId(conversation.getId());
        List<IntentConversationMessage> appended = new ArrayList<>(drafts.size());
        for (MessageDraft draft : drafts) {
            appended.add(new IntentConversationMessage(conversation.getId(), sequence++, draft.role(), draft.content(), user, now));
        }
        messages.saveAll(appended);

        conversation.setUpdatedAt(now);
        return conversations.saveAndFlush(conversation);
    }

    /**
     * The tenant-scoped lookup both the read and the append path go through.
     *
     * @param key the conversation key
     * @return the conversation, when there is one
     */
    private Optional<IntentConversation> lookup(ConversationKey key) {
        return conversations.findByTenantIdAndProjectAndSurfaceAndPath(currentTenantId(), key.project(), key.surface()
                                                                                                            .wireName(),
                key.path());
    }

    /**
     * Gets the current tenant id, falling back to the default tenant when the tenant context is not yet
     * initialized.
     *
     * @return the current tenant id
     */
    private String currentTenantId() {
        return tenantContext.isNotInitialized() ? defaultTenant.getId()
                : tenantContext.getCurrentTenant()
                               .getId();
    }

    /**
     * Which conversation is being read or extended: one application's intent file, in one authoring
     * surface. The tenant is not part of it - the service always resolves that from the request.
     *
     * @param project the workspace project
     * @param surface the authoring surface
     * @param path the intent file path within the project
     */
    public record ConversationKey(String project, ConversationSurface surface, String path) {
    }

    /**
     * A message about to be appended. Authorship and timing are the service's to stamp, never the
     * caller's to claim.
     *
     * @param role what the message is
     * @param content the message text
     */
    public record MessageDraft(ConversationRole role, String content) {
    }
}
