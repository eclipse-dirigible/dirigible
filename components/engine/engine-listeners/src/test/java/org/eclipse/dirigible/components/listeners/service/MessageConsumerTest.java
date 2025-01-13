/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.listeners.service;

import jakarta.jms.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.IllegalStateException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * The Class MessageConsumerTest.
 */
@SuppressWarnings("resource")
@ExtendWith(MockitoExtension.class)
class MessageConsumerTest {

    /** The Constant QUEUE. */
    private static final String QUEUE = "test-queue";

    /** The Constant TENANT_QUEUE. */
    private static final String TENANT_QUEUE = "1e7252b1-3bca-4285-bd4e-60e19886d063###test-queue";

    /** The Constant TIMEOUT. */
    private static final long TIMEOUT = 100L;

    /** The Constant MESSAGE. */
    private static final String MESSAGE = "This is a test message";

    /** The Constant TOPIC. */
    private static final String TOPIC = "test-topic";

    /** The Constant TENANT_TOPIC. */
    private static final String TENANT_TOPIC = "1e7252b1-3bca-4285-bd4e-60e19886d063###test-topic";

    /** The consumer. */
    @InjectMocks
    private MessageConsumer consumer;

    /** The session. */
    @Mock
    private Session session;

    /** The jsm consumer. */
    @Mock
    private jakarta.jms.MessageConsumer jsmConsumer;

    /** The queue. */
    @Mock
    private Queue queue;

    /** The topic. */
    @Mock
    private Topic topic;

    /** The txt message. */
    @Mock
    private TextMessage txtMessage;

    /** The byte message. */
    @Mock
    private BytesMessage byteMessage;

    /** The destination name manager. */
    @Mock
    private DestinationNameManager destinationNameManager;

    /**
     * Test receive message from queue.
     *
     * @throws TimeoutException the timeout exception
     * @throws JMSException the JMS exception
     */
    @Test
    void testReceiveMessageFromQueue() throws TimeoutException, JMSException {
        when(destinationNameManager.toTenantName(QUEUE)).thenReturn(TENANT_QUEUE);
        when(session.createQueue(TENANT_QUEUE)).thenReturn(queue);
        when(session.createConsumer(queue)).thenReturn(jsmConsumer);
        when(jsmConsumer.receive(TIMEOUT)).thenReturn(txtMessage);
        when(txtMessage.getText()).thenReturn(MESSAGE);

        String actualMessage = consumer.receiveMessageFromQueue(QUEUE, TIMEOUT);

        assertThat(actualMessage).isEqualTo(MESSAGE);
    }

    /**
     * Test receive message from queue on timeout.
     *
     * @throws TimeoutException the timeout exception
     * @throws JMSException the JMS exception
     */
    @Test
    void testReceiveMessageFromQueueOnTimeout() throws TimeoutException, JMSException {
        when(destinationNameManager.toTenantName(QUEUE)).thenReturn(TENANT_QUEUE);
        when(session.createQueue(TENANT_QUEUE)).thenReturn(queue);
        when(session.createConsumer(queue)).thenReturn(jsmConsumer);
        when(jsmConsumer.receive(TIMEOUT)).thenReturn(null);

        assertThrows(TimeoutException.class, () -> consumer.receiveMessageFromQueue(QUEUE, TIMEOUT));
    }

    /**
     * Test receive message from queue on unsupported message is received.
     *
     * @throws TimeoutException the timeout exception
     * @throws JMSException the JMS exception
     */
    @Test
    void testReceiveMessageFromQueueOnUnsupportedMessageIsReceived() throws TimeoutException, JMSException {
        when(destinationNameManager.toTenantName(QUEUE)).thenReturn(TENANT_QUEUE);
        when(session.createQueue(TENANT_QUEUE)).thenReturn(queue);
        when(session.createConsumer(queue)).thenReturn(jsmConsumer);
        when(jsmConsumer.receive(TIMEOUT)).thenReturn(byteMessage);

        assertThrows(IllegalStateException.class, () -> consumer.receiveMessageFromQueue(QUEUE, TIMEOUT));
    }

    /**
     * Test receive message from topic.
     *
     * @throws JMSException the JMS exception
     */
    @Test
    void testReceiveMessageFromTopic() throws JMSException {
        when(destinationNameManager.toTenantName(TOPIC)).thenReturn(TENANT_TOPIC);
        when(session.createTopic(TENANT_TOPIC)).thenReturn(topic);
        when(session.createConsumer(topic)).thenReturn(jsmConsumer);
        when(jsmConsumer.receive(TIMEOUT)).thenReturn(txtMessage);
        when(txtMessage.getText()).thenReturn(MESSAGE);

        String actualMessage = consumer.receiveMessageFromTopic(TOPIC, TIMEOUT);

        assertThat(actualMessage).isEqualTo(MESSAGE);
    }

    /**
     * Test receive message from topic on timeout.
     *
     * @throws JMSException the JMS exception
     */
    @Test
    void testReceiveMessageFromTopicOnTimeout() throws JMSException {
        when(destinationNameManager.toTenantName(TOPIC)).thenReturn(TENANT_TOPIC);
        when(session.createTopic(TENANT_TOPIC)).thenReturn(topic);
        when(session.createConsumer(topic)).thenReturn(jsmConsumer);
        when(jsmConsumer.receive(TIMEOUT)).thenReturn(null);

        assertThrows(TimeoutException.class, () -> consumer.receiveMessageFromTopic(TOPIC, TIMEOUT));
    }

}
