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

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import jakarta.jms.Connection;
import jakarta.jms.ExceptionListener;
import jakarta.jms.JMSException;
import jakarta.jms.Session;

/**
 * A factory for creating ActiveMQConnectionArtifacts objects.
 */
@Component
public class ActiveMQConnectionArtifactsFactory {

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
        return createConnection(exceptionListener, null);
    }

    /**
     * Creates a connection, optionally identified by a client id. A client id is what lets a
     * subscription on it be DURABLE: the broker remembers what a {@code clientId + subscription name}
     * pair has already consumed, so a topic message published while that subscriber is disconnected is
     * kept for it instead of dropped. It must therefore be stable across reconnects - a fresh id each
     * time is a fresh subscription, which retains nothing and leaves the old one orphaned - and unique
     * among live connections, since the broker refuses a second connection claiming an id already in
     * use.
     *
     * @param exceptionListener the exception listener
     * @param clientId the client id to identify the connection by, or {@code null} for an anonymous one
     * @return the connection
     * @throws IllegalStateException the illegal state exception
     */
    public Connection createConnection(ExceptionListener exceptionListener, String clientId) throws IllegalStateException {
        try {
            Connection connection = connectionFactory.createConnection();
            connection.setExceptionListener(exceptionListener);
            if (clientId != null) {
                // Must precede start() - JMS forbids setting the client id on a started connection.
                connection.setClientID(clientId);
            }

            connection.start();

            return connection;
        } catch (JMSException ex) {
            throw new IllegalStateException("Failed to create connection to ActiveMQ", ex);
        }
    }

}
