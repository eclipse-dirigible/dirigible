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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import jakarta.jms.JMSException;
import org.eclipse.dirigible.components.listeners.service.MessageConsumer;
import org.eclipse.dirigible.components.listeners.service.MessageProducer;
import org.eclipse.dirigible.components.listeners.service.TimeoutException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessagingFacadeTest {

    private static final String QUEUE = "test-queue";
    private static final String TOPIC = "test-topic";
    private static final long TIMEOUT = 100L;
    private static final String MESSAGE = "a test message";

    @InjectMocks
    private MessagingFacade messagingFacade;

    @Mock
    private MessageConsumer messageConsumer;

    @Mock
    private MessageProducer messageProducer;

    @Nested
    class ReceiveFromQueueTest {

        @Test
        void happyPath() throws TimeoutException, JMSException {
            when(messageConsumer.receiveMessageFromQueue(QUEUE, TIMEOUT)).thenReturn(MESSAGE);

            String message = MessagingFacade.receiveFromQueue(QUEUE, TIMEOUT);

            assertThat(message).isEqualTo(MESSAGE ).withFailMessage("Unexpected message");
        }

        @Test
        void onJMSException() throws TimeoutException, JMSException {
            when(messageConsumer.receiveMessageFromQueue(QUEUE, TIMEOUT)).thenThrow(JMSException.class);

            assertThrows(MessagingAPIException.class, ()->MessagingFacade.receiveFromQueue(QUEUE, TIMEOUT));
        }

        @Test
        void onRuntimeException() throws TimeoutException, JMSException {
            when(messageConsumer.receiveMessageFromQueue(QUEUE, TIMEOUT)).thenThrow(RuntimeException.class);

            assertThrows( MessagingAPIException.class, ()->MessagingFacade.receiveFromQueue(QUEUE, TIMEOUT));
        }

        @Test
        void onTimeoutException() throws TimeoutException, JMSException {
            when(messageConsumer.receiveMessageFromQueue(QUEUE, TIMEOUT)).thenThrow(TimeoutException.class);

            assertThrows(org.eclipse.dirigible.components.api.messaging.TimeoutException.class, ()->MessagingFacade.receiveFromQueue(QUEUE, TIMEOUT));
        }
    }

    @Nested
    class ReceiveFromTopicTest {

        @Test
        void happyPath() throws TimeoutException, JMSException {
            when(messageConsumer.receiveMessageFromTopic(TOPIC, TIMEOUT)).thenReturn(MESSAGE);

            String message = MessagingFacade.receiveFromTopic(TOPIC, TIMEOUT);

            assertThat(message).isEqualTo(MESSAGE ).withFailMessage("Unexpected message");
        }

        @Test
        void onJMSException() throws TimeoutException, JMSException {
            when(messageConsumer.receiveMessageFromTopic(TOPIC, TIMEOUT)).thenThrow(JMSException.class);

            assertThrows( MessagingAPIException.class, ()->MessagingFacade.receiveFromTopic(TOPIC, TIMEOUT));
        }

        @Test
        void onRuntimeException() throws TimeoutException, JMSException {
            when(messageConsumer.receiveMessageFromTopic(TOPIC, TIMEOUT)).thenThrow(RuntimeException.class);

            assertThrows( MessagingAPIException.class, ()->   MessagingFacade.receiveFromTopic(TOPIC, TIMEOUT));
        }

        @Test
        void onTimeoutException() throws TimeoutException, JMSException {
            when(messageConsumer.receiveMessageFromTopic(TOPIC, TIMEOUT)).thenThrow(TimeoutException.class);

            assertThrows(org.eclipse.dirigible.components.api.messaging.TimeoutException.class, ()->MessagingFacade.receiveFromTopic(TOPIC, TIMEOUT));
        }
    }

    @Nested
    class SendMessageToQueueTest {

        @Test
        void happyPath() throws TimeoutException, JMSException {
            MessagingFacade.sendToQueue(QUEUE, MESSAGE);

            verify(messageProducer).sendMessageToQueue(QUEUE, MESSAGE);
        }

        @Test
        void onJMSException() throws TimeoutException, JMSException {
            doThrow(JMSException.class).when(messageProducer)
                                       .sendMessageToQueue(QUEUE, MESSAGE);

            assertThrows(MessagingAPIException.class, () -> MessagingFacade.sendToQueue(QUEUE, MESSAGE));
        }

        @Test
        void onRuntimeException() throws TimeoutException, JMSException {
            doThrow(RuntimeException.class).when(messageProducer)
                                           .sendMessageToQueue(QUEUE, MESSAGE);

            assertThrows(MessagingAPIException.class, () -> MessagingFacade.sendToQueue(QUEUE, MESSAGE));
        }
    }

    @Nested
    class SendMessageToTopicTest {

        @Test
        void happyPath() throws TimeoutException, JMSException {
            MessagingFacade.sendToTopic(TOPIC, MESSAGE);

            verify(messageProducer).sendMessageToTopic(TOPIC, MESSAGE);
        }

        @Test
        void onJMSException() throws TimeoutException, JMSException {
            doThrow(JMSException.class).when(messageProducer)
                                       .sendMessageToTopic(TOPIC, MESSAGE);

            assertThrows(MessagingAPIException.class, () -> MessagingFacade.sendToTopic(TOPIC, MESSAGE));
        }

        @Test
        void onRuntimeException() throws TimeoutException, JMSException {
            doThrow(RuntimeException.class).when(messageProducer)
                                           .sendMessageToTopic(TOPIC, MESSAGE);

            assertThrows(MessagingAPIException.class, () -> MessagingFacade.sendToTopic(TOPIC, MESSAGE));
        }
    }

}
