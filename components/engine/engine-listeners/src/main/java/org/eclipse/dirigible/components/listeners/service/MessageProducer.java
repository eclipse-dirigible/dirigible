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

/**
 * The Class MessageProducer.
 */
@Component
public class MessageProducer {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageProducer.class);

    /** The session. */
    private final Session session;

    /** The destination name manager. */
    private final DestinationNameManager destinationNameManager;

    /** The tenant property manager. */
    private final TenantPropertyManager tenantPropertyManager;

    /**
     * Instantiates a new message producer.
     *
     * @param session the session
     * @param destinationNameManager the destination name manager
     * @param tenantPropertyManager the tenant property manager
     */
    @Autowired
    MessageProducer(@Qualifier("ActiveMQSession") Session session, DestinationNameManager destinationNameManager,
            TenantPropertyManager tenantPropertyManager) {
        this.session = session;
        this.destinationNameManager = destinationNameManager;
        this.tenantPropertyManager = tenantPropertyManager;
    }

    /**
     * Send message to topic.
     *
     * @param topic the topic
     * @param message the message
     * @throws JMSException the JMS exception
     */
    public void sendMessageToTopic(String topic, String message) throws JMSException {
        String destinationName = destinationNameManager.toTenantName(topic);
        Destination destination = session.createTopic(destinationName);
        sendMessage(message, destination);
    }

    /**
     * Send message.
     *
     * @param message the message
     * @param destination the destination
     * @throws JMSException the JMS exception
     */
    private void sendMessage(String message, Destination destination) throws JMSException {
        try (jakarta.jms.MessageProducer producer = session.createProducer(destination)) {
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            TextMessage textMessage = session.createTextMessage(message);
            tenantPropertyManager.setCurrentTenant(textMessage);

            producer.send(textMessage);
            LOGGER.trace("Message sent in [{}]", destination);
        }
    }

    /**
     * Send message to queue.
     *
     * @param queue the queue
     * @param message the message
     * @throws JMSException the JMS exception
     */
    public void sendMessageToQueue(String queue, String message) throws JMSException {
        String destinationName = destinationNameManager.toTenantName(queue);
        Destination destination = session.createQueue(destinationName);
        sendMessage(message, destination);
    }

}
