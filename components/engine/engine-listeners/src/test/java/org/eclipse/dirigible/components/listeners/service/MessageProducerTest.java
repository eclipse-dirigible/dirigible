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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The Class MessageProducerTest.
 */
@SuppressWarnings("resource")
@ExtendWith(MockitoExtension.class)
class MessageProducerTest {

    /** The Constant QUEUE. */
    private static final String QUEUE = "test-queue";

    /** The Constant TENANT_QUEUE. */
    private static final String TENANT_QUEUE = "1e7252b1-3bca-4285-bd4e-60e19886d063###test-queue";

    /** The Constant MESSAGE. */
    private static final String MESSAGE = "This is a test message";

    /** The Constant TOPIC. */
    private static final String TOPIC = "test-topic";

    /** The Constant TENANT_TOPIC. */
    private static final String TENANT_TOPIC = "1e7252b1-3bca-4285-bd4e-60e19886d063###test-topic";

    /** The producer. */
    @InjectMocks
    private MessageProducer producer;
    /** The session. */
    @Mock
    private Session session;
    /** The jsm producer. */
    @Mock
    private jakarta.jms.MessageProducer jsmProducer;
    /** The queue. */
    @Mock
    private Queue queue;
    /** The topic. */
    @Mock
    private Topic topic;
    /** The txt message. */
    @Mock
    private TextMessage txtMessage;

    /** The destination name manager. */
    @Mock
    private DestinationNameManager destinationNameManager;

    /** The tenant property manager. */
    @Mock
    private TenantPropertyManager tenantPropertyManager;

    /**
     * Test send message to topic.
     *
     * @throws JMSException the JMS exception
     */
    @Test
    void testSendMessageToTopic() throws JMSException {
        when(destinationNameManager.toTenantName(TOPIC)).thenReturn(TENANT_TOPIC);
        when(session.createTopic(TENANT_TOPIC)).thenReturn(topic);
        when(session.createProducer(topic)).thenReturn(jsmProducer);
        when(session.createTextMessage(MESSAGE)).thenReturn(txtMessage);

        producer.sendMessageToTopic(TOPIC, MESSAGE);

        verify(jsmProducer).send(txtMessage);
        verify(tenantPropertyManager).setCurrentTenant(txtMessage);
    }

    /**
     * Test send message to queue.
     *
     * @throws JMSException the JMS exception
     */
    @Test
    void testSendMessageToQueue() throws JMSException {
        when(destinationNameManager.toTenantName(QUEUE)).thenReturn(TENANT_QUEUE);
        when(session.createQueue(TENANT_QUEUE)).thenReturn(queue);
        when(session.createProducer(queue)).thenReturn(jsmProducer);
        when(session.createTextMessage(MESSAGE)).thenReturn(txtMessage);

        producer.sendMessageToQueue(QUEUE, MESSAGE);

        verify(jsmProducer).send(txtMessage);
        verify(tenantPropertyManager).setCurrentTenant(txtMessage);
    }

}
