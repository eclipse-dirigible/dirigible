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

import static org.assertj.core.api.Assertions.assertThat;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.listeners.util.LogsAsserter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ch.qos.logback.classic.Level;

/**
 * The Class ExternalBrokerMessagingConfigTest.
 */
class ExternalBrokerMessagingConfigTest {

    /** The Constant BROKER_URL. */
    private static final String BROKER_URL = "tcp://broker.example.com:61616";

    /** The Constant IGNORED_CONFIG_MESSAGE. */
    private static final String IGNORED_CONFIG_MESSAGE = "Ignoring [DIRIGIBLE_MESSAGING_USE_DEFAULT_DATABASE]";

    /** The config. */
    private final ExternalBrokerMessagingConfig config = new ExternalBrokerMessagingConfig();

    /** The logs asserter. */
    private LogsAsserter logsAsserter;

    /**
     * Sets up.
     */
    @BeforeEach
    void setUp() {
        logsAsserter = new LogsAsserter(ExternalBrokerMessagingConfig.class, Level.INFO);
        Configuration.set(DirigibleConfig.MESSAGING_BROKER_URL.getKey(), BROKER_URL);
    }

    /**
     * Tear down.
     */
    @AfterEach
    void tearDown() {
        Configuration.remove(DirigibleConfig.MESSAGING_BROKER_URL.getKey());
        Configuration.remove(DirigibleConfig.MESSAGING_BROKER_USERNAME.getKey());
        Configuration.remove(DirigibleConfig.MESSAGING_BROKER_PASSWORD.getKey());
        Configuration.remove(DirigibleConfig.MESSAGING_USE_DEFAULT_DATABASE.getKey());
    }

    /**
     * Test the factory points at the configured broker with its credentials.
     */
    @Test
    void testFactoryUsesConfiguredBrokerAndCredentials() {
        Configuration.set(DirigibleConfig.MESSAGING_BROKER_USERNAME.getKey(), "broker-user");
        Configuration.set(DirigibleConfig.MESSAGING_BROKER_PASSWORD.getKey(), "broker-pass");

        ActiveMQConnectionFactory factory = config.createActiveMQConnectionFactory();

        assertThat(factory.getBrokerURL()).isEqualTo(BROKER_URL);
        assertThat(factory.getUserName()).isEqualTo("broker-user");
        assertThat(factory.getPassword()).isEqualTo("broker-pass");
    }

    /**
     * A broker that needs no credentials is connected to anonymously.
     */
    @Test
    void testFactoryWithoutCredentialsConnectsAnonymously() {
        ActiveMQConnectionFactory factory = config.createActiveMQConnectionFactory();

        assertThat(factory.getBrokerURL()).isEqualTo(BROKER_URL);
        assertThat(factory.getUserName()).isNull();
        assertThat(factory.getPassword()).isNull();
    }

    /**
     * Test the embedded-only persistence setting is reported as ignored when it is set.
     */
    @Test
    void testEmbeddedOnlyConfigIsReportedAsIgnored() {
        Configuration.set(DirigibleConfig.MESSAGING_USE_DEFAULT_DATABASE.getKey(), "true");

        config.createActiveMQConnectionFactory();

        logsAsserter.assertLoggedMessage(IGNORED_CONFIG_MESSAGE, Level.INFO);
    }

    /**
     * Nothing is reported when the deployment never set it - the message must not read as a complaint
     * about a default.
     */
    @Test
    void testNothingIsReportedWhenEmbeddedOnlyConfigIsUnset() {
        config.createActiveMQConnectionFactory();

        assertThat(logsAsserter.containsMessage(IGNORED_CONFIG_MESSAGE, Level.INFO)).isFalse();
    }

    /**
     * Test the broker password never reaches the log.
     */
    @Test
    void testPasswordIsNotLogged() {
        Configuration.set(DirigibleConfig.MESSAGING_BROKER_PASSWORD.getKey(), "s3cret");

        config.createActiveMQConnectionFactory();

        assertThat(logsAsserter.containsMessage("s3cret", Level.INFO)).isFalse();
    }
}
