/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package ledger;

import org.eclipse.dirigible.sdk.component.Component;
import org.eclipse.dirigible.sdk.messaging.ListenerKind;
import org.eclipse.dirigible.sdk.messaging.MessageHandler;
import org.eclipse.dirigible.sdk.messaging.Producer;

/**
 * Echoes the payload a targeted header write publishes onto a queue the test can drain - a roll-up or
 * a notify {@code {placeholder}} downstream is exactly such a subscriber, and it computes from what
 * this payload says the row is (issue #7135).
 */
@Component
public class DocumentUpdatedListener implements MessageHandler {

    public static final String UPDATED_TOPIC = "uow-document-updated";

    public static final String ECHO_QUEUE = "uow-it-updated-echo";

    @Override
    public String destination() {
        return UPDATED_TOPIC;
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
