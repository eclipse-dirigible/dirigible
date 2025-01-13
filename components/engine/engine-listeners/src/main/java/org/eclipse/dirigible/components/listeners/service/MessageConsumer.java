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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.lang.IllegalStateException;

/**
 * The Class MessageConsumer.
 */
@Component
public class MessageConsumer {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumer.class);

    /** The session. */
    private final Session session;

    /** The destination name manager. */
    private final DestinationNameManager destinationNameManager;

    /**
     * Instantiates a new message consumer.
     *
     * @param session the session
     * @param destinationNameManager the destination name manager
     */
    @Autowired
    MessageConsumer(@Qualifier("ActiveMQSession") Session session, DestinationNameManager destinationNameManager) {
        this.session = session;
        this.destinationNameManager = destinationNameManager;
    }

    /**
     * Receive message from queue.
     *
     * @param queue the queue
     * @param timeout the timeout
     * @return the string
     * @throws JMSException the JMS exception
     * @throws TimeoutException the timeout exception
     */
    public String receiveMessageFromQueue(String queue, long timeout) throws JMSException, TimeoutException {
        String destination = destinationNameManager.toTenantName(queue);
        return receiveMessage(timeout, session.createQueue(destination));
    }

    /**
     * Receive message.
     *
     * @param timeout the timeout
     * @param destination the destination
     * @return the string
     * @throws JMSException the JMS exception
     * @throws TimeoutException the timeout exception
     */
    private String receiveMessage(long timeout, Destination destination) throws JMSException, TimeoutException {
        try (jakarta.jms.MessageConsumer consumer = session.createConsumer(destination)) {

            Message message = consumer.receive(timeout);
            LOGGER.debug("Received message [{}] by synchronous consumer.", message);
            if (null == message) {
                throw new TimeoutException("Timeout to get a message");
            }
            if (message instanceof TextMessage textMessage) {
                return textMessage.getText();
            }
            throw new IllegalStateException("Received an unsupported message " + message);
        }
    }

    /**
     * Receive message from topic.
     *
     * @param topic the topic
     * @param timeout the timeout
     * @return the string
     * @throws JMSException the JMS exception
     * @throws TimeoutException the timeout exception
     */
    public String receiveMessageFromTopic(String topic, long timeout) throws JMSException, TimeoutException {
        String destination = destinationNameManager.toTenantName(topic);
        return receiveMessage(timeout, session.createTopic(destination));
    }

}
