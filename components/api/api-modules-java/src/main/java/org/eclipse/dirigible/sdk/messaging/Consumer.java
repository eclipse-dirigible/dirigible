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

import org.eclipse.dirigible.components.api.messaging.MessagingFacade;

/**
 * Synchronously pulls a single message from a queue or topic, blocking up to {@code timeoutMillis}.
 * Returns {@code null} when the timeout elapses without a delivery.
 * <p>
 * Use this in script-style code that processes a single message and exits, or when you want tight
 * backpressure control. For long-running, always-on consumption use a
 * {@link org.eclipse.dirigible.sdk.messaging.Listener @Listener} annotated class — the platform
 * manages the consumer thread, reconnects, and ordering for you.
 */
public final class Consumer {

    private Consumer() {}

    public static String receiveFromQueue(String queue, long timeoutMillis) {
        return MessagingFacade.receiveFromQueue(queue, timeoutMillis);
    }

    public static String receiveFromTopic(String topic, long timeoutMillis) {
        return MessagingFacade.receiveFromTopic(topic, timeoutMillis);
    }
}
