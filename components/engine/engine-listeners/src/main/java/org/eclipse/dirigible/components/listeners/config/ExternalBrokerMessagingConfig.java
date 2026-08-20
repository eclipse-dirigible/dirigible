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
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.jms.Session;

/**
 * The messaging beans backed by an external ActiveMQ broker, active when
 * {@link DirigibleConfig#MESSAGING_BROKER_URL} is configured. Its counterpart is
 * {@link EmbeddedBrokerMessagingConfig}; the two are mutually exclusive by construction.
 * <p>
 * No {@code BrokerService} bean exists in this mode - the broker is somebody else's process - so
 * nothing may depend on one. That absence is the whole difference in the bean graph: the connection
 * and session beans keep their names, so every consumer of the messaging engine (the {@code
 * .listener} artifacts, the client-Java listeners, the messaging facade) is unaffected and reaches
 * the external broker through the same {@link ActiveMQConnectionArtifactsFactory}.
 * <p>
 * An unreachable broker fails the startup, deliberately: messaging that silently does not work is
 * worse than a deployment that refuses to start.
 */
@Configuration
@Conditional(ExternalMessagingBrokerCondition.class)
class ExternalBrokerMessagingConfig {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalBrokerMessagingConfig.class);

    /**
     * Creates the active MQ connection factory pointed at the external broker. Credentials left unset
     * mean an anonymous connection.
     *
     * @return the active MQ connection factory
     */
    @Bean
    ActiveMQConnectionFactory createActiveMQConnectionFactory() {
        String brokerUrl = DirigibleConfig.MESSAGING_BROKER_URL.getStringValue();
        String username = DirigibleConfig.MESSAGING_BROKER_USERNAME.getStringValue();

        warnIfEmbeddedOnlyConfigIsSet();
        LOGGER.info("Messaging will use the external ActiveMQ broker at [{}] with user [{}]", brokerUrl, username);

        return new ActiveMQConnectionFactory(username, DirigibleConfig.MESSAGING_BROKER_PASSWORD.getStringValue(), brokerUrl);
    }

    /**
     * Logs the embedded-broker settings that carry no meaning against an external broker, so that a
     * deployment which sets them is not left believing they apply.
     */
    private void warnIfEmbeddedOnlyConfigIsSet() {
        String key = DirigibleConfig.MESSAGING_USE_DEFAULT_DATABASE.getKey();
        if (null != org.eclipse.dirigible.commons.config.Configuration.get(key)) {
            LOGGER.info("Ignoring [{}]: it configures the embedded broker only - an external broker owns its persistence", key);
        }
    }

    /**
     * Creates the session.
     *
     * @param connection the connection
     * @return the session
     */
    @Bean("ActiveMQSession")
    Session createSession(@Qualifier("ActiveMQConnection") Connection connection) {
        try {
            return connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        } catch (JMSException ex) {
            throw new IllegalStateException("Failed to create session to ActiveMQ", ex);
        }
    }

    /**
     * Creates the connection. Unlike the embedded mode this does not depend on a broker bean - there is
     * none to start first.
     *
     * @param connectionArtifactsFactory the connection artifacts factory
     * @param loggingExceptionListener the logging exception listener
     * @return the connection
     */
    @Bean("ActiveMQConnection")
    Connection createConnection(ActiveMQConnectionArtifactsFactory connectionArtifactsFactory,
            LoggingExceptionListener loggingExceptionListener) {
        return connectionArtifactsFactory.createConnection(loggingExceptionListener);
    }
}
