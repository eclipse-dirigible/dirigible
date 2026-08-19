/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.listeners.config;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.broker.region.policy.RedeliveryPolicyMap;
import org.apache.activemq.command.ActiveMQDestination;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import jakarta.jms.Connection;
import jakarta.jms.Destination;
import jakarta.jms.ExceptionListener;
import jakarta.jms.JMSException;
import jakarta.jms.Session;

/**
 * A factory for creating ActiveMQConnectionArtifacts objects.
 */
@Component
public class ActiveMQConnectionArtifactsFactory {

    /** The Constant INITIAL_REDELIVERY_DELAY. */
    private static final int INITIAL_REDELIVERY_DELAY = 1000;

    /** The Constant REDELIVERY_DELAY. */
    private static final int REDELIVERY_DELAY = 5000;

    /** The Constant MAXIMUM_REDELIVERIES. */
    private static final int MAXIMUM_REDELIVERIES = 3;

    /** The connection factory. */
    private final ActiveMQConnectionFactory connectionFactory;

    /**
     * Instantiates a new active MQ connection artifacts factory.
     *
     * @param connectionFactory the connection factory
     */
    @Autowired
    ActiveMQConnectionArtifactsFactory(ActiveMQConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * Creates a new ActiveMQConnectionArtifacts object.
     *
     * @param connection the connection
     * @return the session
     * @throws JMSException the JMS exception
     */
    public Session createSession(Connection connection) throws JMSException {
        return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    }

    /**
     * Creates a new ActiveMQConnectionArtifacts object.
     *
     * @param exceptionListener the exception listener
     * @return the connection
     * @throws IllegalStateException the illegal state exception
     */
    public Connection createConnection(ExceptionListener exceptionListener) throws IllegalStateException {
        try {
            Connection connection = connectionFactory.createConnection();
            connection.setExceptionListener(exceptionListener);

            connection.start();

            return connection;
        } catch (JMSException ex) {
            throw new IllegalStateException("Failed to create connection to ActiveMQ", ex);
        }
    }

    /**
     * Bounds how often a failed delivery is retried before the broker gives up and dead-letters the
     * message. The session is AUTO_ACKNOWLEDGE, which for an asynchronous listener acknowledges only
     * after {@code onMessage} RETURNS - so a listener that throws leaves the message unacknowledged and
     * this policy is what decides its fate. A listener that swallows its exception never reaches here:
     * to the broker, swallowing and succeeding are the same outcome.
     *
     * Every listener path must apply this, so the retry budget is one number rather than one per
     * caller.
     *
     * @param connection the connection
     * @param destination the destination
     */
    public void configureRedeliveryPolicy(Connection connection, Destination destination) {
        if (connection instanceof ActiveMQConnection amqConnection && destination instanceof ActiveMQDestination amqDestination) {
            RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();

            redeliveryPolicy.setInitialRedeliveryDelay(INITIAL_REDELIVERY_DELAY);
            redeliveryPolicy.setRedeliveryDelay(REDELIVERY_DELAY);
            redeliveryPolicy.setUseExponentialBackOff(true);
            redeliveryPolicy.setMaximumRedeliveries(MAXIMUM_REDELIVERIES);

            RedeliveryPolicyMap policyMap = amqConnection.getRedeliveryPolicyMap();
            policyMap.put(amqDestination, redeliveryPolicy);
        }
    }

}
