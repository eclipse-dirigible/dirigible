/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api.java.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.concurrent.TimeUnit;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.awaitility.Awaitility;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.api.messaging.MessagingFacade;
import org.eclipse.dirigible.components.ide.messaging.monitoring.endpoint.MessagingMonitoringEndpoint;
import org.eclipse.dirigible.components.ide.messaging.monitoring.service.MessagingMonitoringService;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.util.PortUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Boots the platform against a broker it does not own - a real ActiveMQ started by this test on a
 * TCP port - and proves that the messaging works over it while nothing embedded is left running.
 * <p>
 * The broker URL is set in a static {@code @BeforeAll}, which is what makes the mode selection
 * work: the conditions choosing between the two messaging configurations are evaluated once, when
 * the Spring context is refreshed, and that happens after every {@code @BeforeAll} has run. The
 * context is dirtied after the class and {@code IntegrationTest} reloads the configuration, so
 * neither the mode nor the URL leaks into the next test class.
 */
// One Dirigible boot for the whole class - the external broker outlives the individual methods.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Tag("slow")
class ExternalMessagingBrokerIT extends IntegrationTest {

    /** Declared by integration-tests-project/test-queue-listener.listener, handled in JS. */
    private static final String LISTENED_QUEUE = "integration-tests-queue";

    /** The Constant TEST_MESSAGE. */
    private static final String TEST_MESSAGE = "A message over the external broker";

    /** The Constant MESSAGE_TIMEOUT_SECONDS. */
    private static final int MESSAGE_TIMEOUT_SECONDS = 30;

    /** The broker this test owns, standing in for an Amazon MQ or a dockerized ActiveMQ. */
    private static BrokerService externalBroker;

    /** The URL the platform is pointed at. */
    private static String externalBrokerUrl;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Start the external broker and point the platform at it - before the context exists.
     *
     * @throws Exception if the broker fails to start
     */
    @BeforeAll
    static void startExternalBroker() throws Exception {
        externalBrokerUrl = "tcp://localhost:" + PortUtil.getFreeRandomPort();

        externalBroker = new BrokerService();
        externalBroker.setBrokerName("external-test-broker");
        externalBroker.setPersistent(false);
        externalBroker.setUseJmx(false);
        externalBroker.addConnector(externalBrokerUrl);
        externalBroker.start();
        externalBroker.waitUntilStarted();

        DirigibleConfig.MESSAGING_BROKER_URL.setStringValue(externalBrokerUrl);
    }

    /**
     * Stop external broker.
     *
     * @throws Exception if the broker fails to stop
     */
    @AfterAll
    static void stopExternalBroker() throws Exception {
        if (null != externalBroker) {
            externalBroker.stop();
        }
    }

    /**
     * Clear the received message.
     */
    @BeforeEach
    void clearReceivedMessage() {
        MessagesHolder.clearLatestReceivedMessage();
    }

    /**
     * A published message reaches the registered listener - over the external broker, since that is the
     * only broker running.
     */
    @Test
    void testListenerConsumesFromTheExternalBroker() {
        MessagingFacade.sendToQueue(LISTENED_QUEUE, TEST_MESSAGE);

        Awaitility.await()
                  .atMost(MESSAGE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .until(() -> TEST_MESSAGE.equals(MessagesHolder.getLatestReceivedMessage()));
    }

    /**
     * The platform started no broker of its own, and its connection factory points at the external one.
     */
    @Test
    void testNoEmbeddedBrokerIsStarted() {
        assertThat(applicationContext.getBeanNamesForType(BrokerService.class)).isEmpty();
        assertThat(applicationContext.containsBean("ActiveMQBroker")).isFalse();

        ActiveMQConnectionFactory connectionFactory = applicationContext.getBean(ActiveMQConnectionFactory.class);
        assertThat(connectionFactory.getBrokerURL()).isEqualTo(externalBrokerUrl);
    }

    /**
     * The connection and session beans the messaging engine consumes exist in this mode too - their
     * absence would break every producer and consumer.
     */
    @Test
    void testTheEngineStillHasItsConnectionAndSession() {
        assertThat(applicationContext.containsBean("ActiveMQConnection")).isTrue();
        assertThat(applicationContext.containsBean("ActiveMQSession")).isTrue();
    }

    /**
     * The monitoring surface reads the in-process broker object, so it is not registered here at all -
     * its endpoints answer 404 rather than failing the startup.
     */
    @Test
    void testMonitoringIsNotRegistered() {
        assertThat(applicationContext.getBeanNamesForType(MessagingMonitoringService.class)).isEmpty();
        assertThat(applicationContext.getBeanNamesForType(MessagingMonitoringEndpoint.class)).isEmpty();
    }

    /**
     * A queue round trip through the facade rides the external broker as well.
     */
    @Test
    void testFacadeRoundTripOverTheExternalBroker() {
        String queue = "external-broker-facade-queue";

        MessagingFacade.sendToQueue(queue, TEST_MESSAGE);

        assertThat(MessagingFacade.receiveFromQueue(queue, TimeUnit.SECONDS.toMillis(MESSAGE_TIMEOUT_SECONDS))).isEqualTo(TEST_MESSAGE);
    }
}
