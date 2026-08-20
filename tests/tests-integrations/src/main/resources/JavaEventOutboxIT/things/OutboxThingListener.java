/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package things;

import org.eclipse.dirigible.sdk.component.Component;
import org.eclipse.dirigible.sdk.messaging.ListenerKind;
import org.eclipse.dirigible.sdk.messaging.MessageHandler;
import org.eclipse.dirigible.sdk.messaging.Producer;

/** Echoes whatever reaches the entity's create topic onto a queue the test can drain. */
@Component
public class OutboxThingListener implements MessageHandler {

    public static final String ECHO_QUEUE = "event-outbox-it-echo";

    @Override
    public String destination() {
        return OutboxThingRepository.CREATED_TOPIC;
    }

    @Override
    public ListenerKind kind() {
        return ListenerKind.TOPIC;
    }

    @Override
    public void onMessage(String message) {
        Producer.sendToQueue(ECHO_QUEUE, message);
    }
}
