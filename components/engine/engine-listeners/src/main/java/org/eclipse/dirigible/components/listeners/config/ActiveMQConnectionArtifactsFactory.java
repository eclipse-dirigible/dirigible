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
import org.apache.activemq.transport.failover.FailoverTransport;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveMQConnectionArtifactsFactory.class);

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
     * <p>
     * <b>Such a connection is never reconnected by the transport</b> (#7851). Over a {@code failover:}
     * URL, ActiveMQ's {@code FailoverTransport} would reconnect it by itself after an interruption,
     * within milliseconds, replaying the same client id while the broker still holds the previous,
     * half-open socket under that id - so the broker refuses it ("already connected"), the refusal is
     * another failure, and the transport reconnects again, every subscription at once, until the extra
     * load on the broker keeps the loop going for good. So once it is connected, a client-id connection
     * gives up on the first interruption instead: the failure reaches the {@code exceptionListener},
     * and the caller re-establishes it at its own pace, by which time the broker has reaped the stale
     * socket. The first connect keeps the transport's own retries.
     *
     * @param exceptionListener the exception listener - for a client-id connection, also what learns
     *        that it is gone for good
     * @param clientId the client id to identify the connection by, or {@code null} for an anonymous one
     * @return the connection
     * @throws IllegalStateException the illegal state exception
     */
    public Connection createConnection(ExceptionListener exceptionListener, String clientId) throws IllegalStateException {
        // Held outside the try: once created, the connection owns a started transport, and a failure
        // after that - the broker refusing the client id at start() above all - must not leave it behind.
        // The caller never receives it, so nobody else could close it, and over a failover: URL its
        // transport would keep reconnecting and replaying the client id for the life of the process,
        // refused against the instance's own later subscription (#7853).
        Connection connection = null;
        try {
            connection = connectionFactory.createConnection();
            connection.setExceptionListener(exceptionListener);
            if (clientId != null) {
                // Must precede start() - JMS forbids setting the client id on a started connection.
                connection.setClientID(clientId);
            }

            connection.start();

            if (clientId != null) {
                disableTransportReconnect(connection);
            }

            return connection;
        } catch (JMSException ex) {
            closeQuietly(connection);
            throw new IllegalStateException("Failed to create connection to ActiveMQ", ex);
        } catch (RuntimeException ex) {
            closeQuietly(connection);
            throw ex;
        }
    }

    /**
     * Close a connection that failed to start. Quietly: the broker that just refused it may refuse the
     * close too, and the failure that brought us here is the one the caller has to see.
     *
     * @param connection the connection, or {@code null} if it was never created
     */
    private static void closeQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (JMSException | RuntimeException e) {
            LOGGER.debug("Failed to close a JMS connection that did not start: {}", e.getMessage(), e);
        }
    }

    /**
     * Stop a started connection's {@code FailoverTransport} from reconnecting it. Only after
     * {@code start()}: the transport applies {@code maxReconnectAttempts} to the first connect as well
     * (unless {@code startupMaxReconnectAttempts} is set), and that one must keep retrying a broker
     * that is still coming up. A connection without a failover transport ({@code vm://}, a plain
     * {@code tcp://} or {@code ssl://} URL) has no such reconnect to stop.
     *
     * @param connection the started connection
     */
    static void disableTransportReconnect(Connection connection) {
        if (connection instanceof ActiveMQConnection activeMQConnection) {
            FailoverTransport failover = activeMQConnection.getTransport()
                                                           .narrow(FailoverTransport.class);
            if (failover != null) {
                failover.setMaxReconnectAttempts(0);
            }
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
     * <p>
     * The budget is the deployment's to choose (#8745):
     * {@link DirigibleConfig#MESSAGING_REDELIVERY_INITIAL_DELAY_MILLIS},
     * {@link DirigibleConfig#MESSAGING_REDELIVERY_BACK_OFF_MULTIPLIER},
     * {@link DirigibleConfig#MESSAGING_REDELIVERY_MAXIMUM_DELAY_MILLIS} and
     * {@link DirigibleConfig#MESSAGING_REDELIVERY_MAXIMUM_REDELIVERIES}, read each time a subscription
     * is opened. Their defaults are the budget this method always applied: about 1, 6 and 31 seconds
     * after the first delivery, then the dead-letter queue.
     *
     * @param connection the connection
     * @param destination the destination
     */
    public void configureRedeliveryPolicy(Connection connection, Destination destination) {
        if (connection instanceof ActiveMQConnection amqConnection && destination instanceof ActiveMQDestination amqDestination) {
            RedeliveryPolicyMap policyMap = amqConnection.getRedeliveryPolicyMap();
            policyMap.put(amqDestination, redeliveryPolicy());
        }
    }

    /**
     * The redelivery policy the configuration asks for. A value that is not a number, or is out of
     * range, is reported and replaced by its default - a typo must not leave a deployment without a
     * retry budget, nor fail every subscription.
     *
     * @return a new policy
     */
    static RedeliveryPolicy redeliveryPolicy() {
        long initialDelay = readLong(DirigibleConfig.MESSAGING_REDELIVERY_INITIAL_DELAY_MILLIS, 0);
        double multiplier = readMultiplier();
        long maximumDelay = readLong(DirigibleConfig.MESSAGING_REDELIVERY_MAXIMUM_DELAY_MILLIS, -1);
        int maximumRedeliveries = (int) readLong(DirigibleConfig.MESSAGING_REDELIVERY_MAXIMUM_REDELIVERIES, -1);

        RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
        // ActiveMQ waits the initial delay before the first redelivery and multiplies the previous
        // delay for each one after; the plain redelivery delay is used only when there is no previous
        // one to multiply, so it is kept equal to the initial delay.
        redeliveryPolicy.setInitialRedeliveryDelay(initialDelay);
        redeliveryPolicy.setRedeliveryDelay(initialDelay);
        redeliveryPolicy.setUseExponentialBackOff(multiplier > 1);
        redeliveryPolicy.setBackOffMultiplier(multiplier);
        redeliveryPolicy.setMaximumRedeliveryDelay(maximumDelay);
        redeliveryPolicy.setMaximumRedeliveries(maximumRedeliveries);
        return redeliveryPolicy;
    }

    private static long readLong(DirigibleConfig key, long minimum) {
        String value = key.getStringValue();
        try {
            long parsed = Long.parseLong(String.valueOf(value)
                                               .trim());
            if (parsed >= minimum) {
                return parsed;
            }
        } catch (NumberFormatException ex) {
            LOGGER.debug("[{}] is not a whole number: [{}]", key.getKey(), value, ex);
        }
        return invalid(key, value, "a whole number of at least " + minimum);
    }

    private static double readMultiplier() {
        DirigibleConfig key = DirigibleConfig.MESSAGING_REDELIVERY_BACK_OFF_MULTIPLIER;
        String value = key.getStringValue();
        try {
            double parsed = Double.parseDouble(String.valueOf(value)
                                                     .trim());
            if (parsed >= 1 && Double.isFinite(parsed)) {
                return parsed;
            }
        } catch (NumberFormatException ex) {
            LOGGER.debug("[{}] is not a number: [{}]", key.getKey(), value, ex);
        }
        return invalid(key, value, "a number of at least 1");
    }

    private static long invalid(DirigibleConfig key, String value, String expected) {
        LOGGER.warn("Ignoring [{}={}]: expected {}. Using the default [{}].", key.getKey(), value, expected, key.getDefaultValue());
        return Long.parseLong(key.getDefaultValue());
    }

}
