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

import org.eclipse.dirigible.components.listeners.service.MessageConsumer;
import org.eclipse.dirigible.components.listeners.service.MessageProducer;

/**
 * Points the static {@link MessagingFacade} at the in-process test slice's broker stand-in
 * (dirigible #7643); the facade's constructor is package-private because the platform builds it as
 * a Spring bean.
 */
public final class IntentSliceMessaging {

    private IntentSliceMessaging() {}

    /**
     * Routes every later {@link MessagingFacade} call to the given consumer and producer.
     *
     * @param consumer what receives from queues
     * @param producer what sends to queues and topics
     */
    public static void install(MessageConsumer consumer, MessageProducer producer) {
        new MessagingFacade(consumer, producer);
    }
}
