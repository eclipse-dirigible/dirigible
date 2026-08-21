/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.messaging;

/**
 * Publishes a message that survives a broker outage: the message is recorded durably before the
 * broker sees it, and whatever the broker refuses is retried until it is taken, so delivery is
 * at-least-once instead of fire-and-forget.
 *
 * <p>
 * This is the contract for an announcement that is deliberately DECOUPLED from the write it is
 * about — deferred past a synchronous chain's commit, or ordered after several transactions — where
 * the transactional outbox's write-attached recording cannot apply. The implementation lives with
 * the outbox (the event-store layer provides it); callers reach it through the SDK
 * {@code Producer.sendToTopicDurable}, never directly.
 */
public interface DurableMessagePublisher {

    /**
     * Records the message durably and hands it to the broker; what the broker refuses is retried until
     * delivered. Never throws for a broker problem — the caller's work has already committed and must
     * not be failed for an announcement the retry machinery owns.
     *
     * @param topic the topic to publish on
     * @param payload the message body
     */
    void publishToTopic(String topic, String payload);
}
