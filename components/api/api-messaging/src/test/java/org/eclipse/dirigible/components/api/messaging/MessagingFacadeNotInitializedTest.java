/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.messaging;

import static org.junit.Assert.assertThrows;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MessagingFacadeNotInitializedTest {

    private static final String QUEUE = "test-queue";
    private static final String TOPIC = "test-topic";
    private static final long TIMEOUT = 100L;
    private static final String MESSAGE = "a test message";

    @BeforeAll
    static void setUp() {
        new MessagingFacade(null, null);
    }

    @Test
    void testReceiveFromQueue() throws TimeoutException {
        assertThrows(IllegalStateException.class, () -> MessagingFacade.receiveFromQueue(QUEUE, TIMEOUT));
    }

    @Test
    void testReceiveFromTopic() throws TimeoutException {
        assertThrows(IllegalStateException.class, () -> MessagingFacade.receiveFromTopic(TOPIC, TIMEOUT));
    }

    @Test
    void testsendToQueue() throws TimeoutException {
        assertThrows(IllegalStateException.class, () -> MessagingFacade.sendToQueue(QUEUE, MESSAGE));
    }

    @Test
    void testsendToTopic() throws TimeoutException {
        assertThrows(IllegalStateException.class, () -> MessagingFacade.sendToTopic(TOPIC, MESSAGE));
    }

}
