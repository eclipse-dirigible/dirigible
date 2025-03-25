/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api.java.messaging;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.components.api.messaging.MessagingFacade;
import org.eclipse.dirigible.components.api.messaging.TimeoutException;
import org.eclipse.dirigible.tests.IntegrationTest;
import org.eclipse.dirigible.tests.util.SleepUtil;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class MessagingFacadeIT extends IntegrationTest {

    private static final String TEST_MESSAGE = "Test message";
    private static final String TEST_MESSAGE_2 = "Test message 2";


    @Nested
    class QueueTest {
        private static final String QUEUE = "my-test-queue";
        private static final long TIMEOUT_MILLIS = 500L;

        @Test
        void testSendReceiveOneMessage() {
            MessagingFacade.sendToQueue(QUEUE, TEST_MESSAGE);

            String actualMessage = MessagingFacade.receiveFromQueue(QUEUE, TIMEOUT_MILLIS);

            assertEquals("Unexpected message", TEST_MESSAGE, actualMessage);
        }

        @Test
        void testSendReceiveTwoMessages() {
            MessagingFacade.sendToQueue(QUEUE, TEST_MESSAGE);
            MessagingFacade.sendToQueue(QUEUE, TEST_MESSAGE_2);

            String actualMessage = MessagingFacade.receiveFromQueue(QUEUE, TIMEOUT_MILLIS);
            String actualMessage2 = MessagingFacade.receiveFromQueue(QUEUE, TIMEOUT_MILLIS);

            assertEquals("Unexpected message", TEST_MESSAGE, actualMessage);
            assertEquals("Unexpected message", TEST_MESSAGE_2, actualMessage2);
        }

        @Test
        void testReceiveOnTimeout() {
            assertThrows(TimeoutException.class, () -> MessagingFacade.receiveFromQueue(QUEUE, 50));
        }
    }


    @Nested
    class TopicTest {
        private static final String TOPIC = "my-test-topic";
        private static final int TIMEOUT_SECONDS = 5;


        private class TopicMessageReciver extends Thread {

            private final String topic;
            private final long timeout;
            private String message;

            private TopicMessageReciver(String topic, long timeout) {
                this.topic = topic;
                this.timeout = timeout;
            }

            @Override
            public void run() {
                message = MessagingFacade.receiveFromTopic(topic, timeout);
            }

            private String getMessage() {
                return message;
            }

        }

        @Test
        void testSendReceiveTwoMessages() throws InterruptedException {
            TopicMessageReciver msgReceiver = new TopicMessageReciver(TOPIC, TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
            TopicMessageReciver msgReceiver2 = new TopicMessageReciver(TOPIC, TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));

            msgReceiver.start();
            msgReceiver2.start();

            SleepUtil.sleepMillis(200);

            MessagingFacade.sendToTopic(TOPIC, TEST_MESSAGE);

            Awaitility.await()
                      .atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                      .until(() -> areMessagesReceived(msgReceiver, msgReceiver2));

            assertEquals("Unexpected message", TEST_MESSAGE, msgReceiver.getMessage());
            assertEquals("Unexpected message", TEST_MESSAGE, msgReceiver2.getMessage());
        }

        private boolean areMessagesReceived(TopicMessageReciver msgReceiver, TopicMessageReciver msgReceiver2) {
            return null != msgReceiver.getMessage() && null != msgReceiver2.getMessage();
        }

        @Test
        void testReceiveOnTimeout() {
            assertThrows(TimeoutException.class, () -> MessagingFacade.receiveFromTopic(TOPIC, 50));
        }
    }

}
