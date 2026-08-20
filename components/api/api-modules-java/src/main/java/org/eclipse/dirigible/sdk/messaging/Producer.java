/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.messaging;

import org.eclipse.dirigible.components.api.messaging.DurableMessagePublisher;
import org.eclipse.dirigible.components.api.messaging.MessagingFacade;
import org.eclipse.dirigible.components.base.spring.BeanProvider;

/**
 * Sends a message into the embedded ActiveMQ broker. {@link #sendToQueue(String, String)} delivers
 * to a queue (point-to-point — one listener receives the message),
 * {@link #sendToTopic(String, String)} delivers to a topic (pub/sub — all active subscribers
 * receive the message).
 * <p>
 * Both methods are non-blocking past the broker's acknowledgement; the broker handles persistence
 * and at-least-once delivery semantics. Pair with a
 * {@link org.eclipse.dirigible.sdk.messaging.Listener @Listener} annotated class on the receiving
 * side or with {@link Consumer#receiveFromQueue(String, long)} when you want to pull messages
 * synchronously.
 */
public final class Producer {

    private Producer() {}

    public static void sendToQueue(String queue, String message) {
        MessagingFacade.sendToQueue(queue, message);
    }

    public static void sendToTopic(String topic, String message) {
        MessagingFacade.sendToTopic(topic, message);
    }

    /**
     * Publishes to a topic with at-least-once delivery: the message is recorded durably before the
     * broker sees it, and whatever the broker refuses is retried until it is taken. Use this for an
     * announcement whose loss would silently break a downstream reaction — a status transition a
     * posting or a create-from is keyed on — when the announcement is deliberately decoupled from the
     * write it is about (deferred past a workflow chain's commit, or ordered after several writes). An
     * announcement that belongs to ONE write should ride that write instead: hand the topic to the
     * repository's write methods, which record it in the same transaction.
     *
     * @param topic the topic to publish on
     * @param message the message body
     */
    public static void sendToTopicDurable(String topic, String message) {
        BeanProvider.getBean(DurableMessagePublisher.class)
                    .publishToTopic(topic, message);
    }
}
