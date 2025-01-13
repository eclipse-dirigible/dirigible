/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api.java.messaging;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.eclipse.dirigible.components.api.messaging.MessagingFacade;

/**
 * used by <b>messaging-test.ts</b>
 */
public class MessageProducer {

    public static void asyncSendMessageToTopic(String topic, String message) {
        ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1);

        scheduledThreadPool.schedule(() -> MessagingFacade.sendToTopic(topic, message), 1, TimeUnit.SECONDS);
    }

}
