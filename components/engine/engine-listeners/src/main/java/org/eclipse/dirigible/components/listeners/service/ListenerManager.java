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

import jakarta.jms.MessageConsumer;
import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.broker.region.policy.RedeliveryPolicyMap;
import org.apache.activemq.command.ActiveMQDestination;
import org.eclipse.dirigible.components.listeners.config.ActiveMQConnectionArtifactsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.IllegalStateException;

/**
 * The Class BackgroundListenerManager.
 */
public class ListenerManager {

    /** The Constant INITIAL_REDELIVERY_DELAY. */
    private static final int INITIAL_REDELIVERY_DELAY = 1000;

    /** The Constant REDELIVERY_DELAY. */
    private static final int REDELIVERY_DELAY = 5000;

    /** The Constant MAXIMUM_REDELIVERIES. */
    private static final int MAXIMUM_REDELIVERIES = 3;

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ListenerManager.class);

    /** The listener. */
    private final ListenerDescriptor listenerDescriptor;

    /** The connection artifacts factory. */
    private final ActiveMQConnectionArtifactsFactory connectionArtifactsFactory;

    /** The asynchronous message listener factory. */
    private final AsynchronousMessageListenerFactory asynchronousMessageListenerFactory;

    /** The connection artifacts. */
    private ConnectionArtifacts connectionArtifacts;

    /**
     * Instantiates a new background listener manager.
     *
     * @param listenerDescriptor the listener
     * @param connectionArtifactsFactory the connection artifacts factory
     * @param asynchronousMessageListenerFactory the asynchronous message listener factory
     */
    public ListenerManager(ListenerDescriptor listenerDescriptor, ActiveMQConnectionArtifactsFactory connectionArtifactsFactory,
            AsynchronousMessageListenerFactory asynchronousMessageListenerFactory) {
        this.listenerDescriptor = listenerDescriptor;
        this.connectionArtifactsFactory = connectionArtifactsFactory;
        this.asynchronousMessageListenerFactory = asynchronousMessageListenerFactory;
    }

    /**
     * Start listener.
     */
    @SuppressWarnings("resource")
    public synchronized void startListener() {
        if (null != connectionArtifacts) {
            LOGGER.debug("Listener [{}] IS already configured", listenerDescriptor);
            return;
        }

        LOGGER.info("Starting a message listener for {} ...", listenerDescriptor);
        try {
            String handlerPath = listenerDescriptor.getHandlerPath();
            ListenerExceptionHandler exceptionListener = new ListenerExceptionHandler(handlerPath);

            Connection connection = connectionArtifactsFactory.createConnection(exceptionListener);
            Session session = connectionArtifactsFactory.createSession(connection);

            Destination destination = createDestination(session);
            configureRedeliveryPolicy(connection, destination);

            MessageConsumer consumer = session.createConsumer(destination);

            AsynchronousMessageListener messageListener = asynchronousMessageListenerFactory.create(listenerDescriptor);
            consumer.setMessageListener(messageListener);

            connectionArtifacts = new ConnectionArtifacts(connection, session, consumer);
        } catch (JMSException ex) {
            throw new IllegalStateException("Failed to start listener for " + listenerDescriptor, ex);
        }
    }

    /**
     * Create destination.
     *
     * @param session the session
     * @return the destination
     * @throws JMSException the JMS exception
     */
    private Destination createDestination(Session session) throws JMSException {
        String destination = listenerDescriptor.getDestination();
        ListenerType type = listenerDescriptor.getType();
        if (null == type) {
            throw new IllegalArgumentException("Type cannot be null for: " + listenerDescriptor);
        }
        return switch (type) {
            case QUEUE -> session.createQueue(destination);
            case TOPIC -> session.createTopic(destination);
            default -> throw new IllegalArgumentException("Invalid type: " + type);
        };
    }

    /**
     * Configure redelivery policy.
     *
     * @param connection the connection
     * @param destination the destination
     */
    private void configureRedeliveryPolicy(Connection connection, Destination destination) {
        if (connection instanceof ActiveMQConnection amqConnection && destination instanceof ActiveMQDestination amqDestination) {
            RedeliveryPolicy redeliveryPolicy = createRedeliveryPolicy();
            RedeliveryPolicyMap policyMap = amqConnection.getRedeliveryPolicyMap();
            policyMap.put(amqDestination, redeliveryPolicy);
        }
    }

    /**
     * Creates the redelivery policy.
     *
     * @return the redelivery policy
     */
    private RedeliveryPolicy createRedeliveryPolicy() {
        RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();

        redeliveryPolicy.setInitialRedeliveryDelay(INITIAL_REDELIVERY_DELAY);
        redeliveryPolicy.setRedeliveryDelay(REDELIVERY_DELAY);
        redeliveryPolicy.setUseExponentialBackOff(true);
        redeliveryPolicy.setMaximumRedeliveries(MAXIMUM_REDELIVERIES);

        return redeliveryPolicy;
    }

    /**
     * Stop listener.
     */
    public synchronized void stopListener() {
        if (null == connectionArtifacts) {
            LOGGER.debug("Listener [{}] is NOT started", listenerDescriptor);
            return;
        }
        LOGGER.info("Stopping message listener for {} ...", listenerDescriptor);
        connectionArtifacts.closeAll();
        connectionArtifacts = null;
        LOGGER.info("Stopped message listener for {}", listenerDescriptor);
    }
}
