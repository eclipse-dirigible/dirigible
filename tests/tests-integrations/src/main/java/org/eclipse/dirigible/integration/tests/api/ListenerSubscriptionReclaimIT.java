/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.region.DurableTopicSubscription;
import org.apache.activemq.broker.region.RegionBroker;
import org.apache.activemq.broker.region.TopicRegion;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.components.listeners.config.ActiveMQConnectionArtifactsFactory;
import org.eclipse.dirigible.engine.java.listener.ListenerClassConsumer;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.eclipse.dirigible.tests.framework.logging.LogsAsserter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import ch.qos.logback.classic.Level;
import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.jms.Session;

/**
 * A switch of {@code DIRIGIBLE_MESSAGING_LISTENER_CONNECTIONS} leaves no durable subscription
 * behind (#7890).
 *
 * <p>
 * The broker remembers a durable subscription by client id and name, and the per-listener mode used
 * the subscription's name as its client id, where the shared mode (the default) uses the scope's.
 * So an instance that switched to the shared mode subscribed afresh and left the per-listener
 * subscriptions offline, retaining every message published to their topics - on a broker without an
 * offline timeout, for good. This test registers those subscriptions the way the per-listener mode
 * did, against the embedded broker, before the handlers are deployed: one already offline, one
 * still connected as the previous instance of a rolling deploy holds it. The first must be
 * reclaimed by the reconciliation pass; the second must survive while it is held, and be reclaimed
 * once it is released.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ListenerSubscriptionReclaimIT extends IntegrationTest {

    private static final String PROJECT = "listener-subscription-reclaim-it";

    /**
     * Short enough to keep the test quick, long enough to be a real timer rather than a direct call.
     */
    private static final String RECONCILE_INTERVAL_SECONDS = "2";

    private static final int RECLAIM_TIMEOUT_SECONDS = 90;

    /** The prefix of a tenant's shared client id, the identity the handlers subscribe under now. */
    private static final String TENANT_CLIENT_ID_PREFIX = "dirigible-java-tenant-";

    private static String previousReconcileInterval;

    @Autowired
    private BrokerService broker;

    @Autowired
    private ActiveMQConnectionArtifactsFactory connectionFactory;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    private LogsAsserter consumerLogs;

    /** Before the context exists, so the reconciler reads the shortened interval when it starts. */
    @BeforeAll
    static void shortenTheReconcileInterval() {
        previousReconcileInterval = DirigibleConfig.JAVA_RECONCILE_INTERVAL_SECONDS.getStringValue();
        DirigibleConfig.JAVA_RECONCILE_INTERVAL_SECONDS.setStringValue(RECONCILE_INTERVAL_SECONDS);
    }

    @AfterAll
    static void restoreTheReconcileInterval() {
        DirigibleConfig.JAVA_RECONCILE_INTERVAL_SECONDS.setStringValue(previousReconcileInterval);
    }

    @BeforeEach
    void attachTheLogs() {
        // Here rather than in a field: Spring re-initializes logback while it starts the application
        // context, which drops an appender attached any earlier.
        consumerLogs = new LogsAsserter(ListenerClassConsumer.class, Level.WARN);
    }

    @Test
    void theSubscriptionsThePerListenerModeRegisteredAreReclaimedOnceTheirClientIdsAreFree() throws Exception {
        String orphan = perListenerSubscriptionId(0);
        String held = perListenerSubscriptionId(1);
        registerPerListenerSubscription(0).close();
        Connection previousInstance = registerPerListenerSubscription(1);
        try {
            assertThat(durable(orphan, orphan)).as("the offline per-listener subscription before the switch")
                                               .isNotNull();

            deployHandlers();
            synchronizationProcessor.forceProcessSynchronizers();

            await().until(() -> durable(orphan, orphan) == null);
            await().until(() -> subscribedInTheSharedMode(orphan) && subscribedInTheSharedMode(held));
            await().until(() -> consumerLogs.containsMessage("[" + held + "] a previous listener connection mode or client id registered",
                    Level.WARN));
            assertThat(durable(held, held)).as("a subscription whose client id is still held is left alone")
                                           .isNotNull();
        } finally {
            previousInstance.close();
        }

        await().until(() -> durable(held, held) == null);
        assertThat(subscribedInTheSharedMode(orphan)).as("the reclaim never touches a subscription in use")
                                                     .isTrue();
        assertThat(subscribedInTheSharedMode(held)).as("the reclaim never touches a subscription in use")
                                                   .isTrue();
    }

    private static ConditionFactory await() {
        return Awaitility.await()
                         .pollInterval(1, TimeUnit.SECONDS)
                         .atMost(RECLAIM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Register a handler's durable subscription exactly as the per-listener mode did: its own name as
     * the connection's client id and as the subscription's name.
     *
     * @return the connection, still holding the client id
     */
    private Connection registerPerListenerSubscription(int handler) throws JMSException {
        String id = perListenerSubscriptionId(handler);
        Connection connection = connectionFactory.createConnection(ex -> {
        }, id);
        Session session = connectionFactory.createSession(connection);
        session.createDurableSubscriber(session.createTopic(topic(handler)), id);
        return connection;
    }

    /** The per-listener id of a handler on its topic in the default tenant (whose names are bare). */
    private static String perListenerSubscriptionId(int handler) {
        return ("dirigible-java-demo." + className(handler) + "-" + topic(handler)).replaceAll("[^A-Za-z0-9._-]", "_");
    }

    /** The handler's subscription in the shared mode: the same name, under the tenant's client id. */
    private boolean subscribedInTheSharedMode(String subscriptionName) throws Exception {
        return topicRegion().getDurableSubscriptions()
                            .values()
                            .stream()
                            .anyMatch(subscription -> subscription.getSubscriptionKey()
                                                                  .getClientId()
                                                                  .startsWith(TENANT_CLIENT_ID_PREFIX)
                                    && subscriptionName.equals(subscription.getSubscriptionKey()
                                                                           .getSubscriptionName())
                                    && subscription.isActive());
    }

    private DurableTopicSubscription durable(String clientId, String subscriptionName) throws Exception {
        return topicRegion().lookupSubscription(clientId, subscriptionName);
    }

    private TopicRegion topicRegion() throws Exception {
        return (TopicRegion) ((RegionBroker) broker.getRegionBroker()).getTopicRegion();
    }

    private static String className(int handler) {
        return "ReclaimProbe" + handler;
    }

    private static String topic(int handler) {
        return PROJECT + "-" + handler;
    }

    private void deployHandlers() {
        for (int handler = 0; handler < 2; handler++) {
            String source = """
                    package demo;
                    import org.eclipse.dirigible.sdk.component.Component;
                    import org.eclipse.dirigible.sdk.messaging.ListenerKind;
                    import org.eclipse.dirigible.sdk.messaging.MessageHandler;
                    @Component
                    public class %s implements MessageHandler {
                        @Override
                        public String destination() {
                            return "%s";
                        }
                        @Override
                        public ListenerKind kind() {
                            return ListenerKind.TOPIC;
                        }
                        @Override
                        public void onMessage(String message) {
                        }
                    }
                    """.formatted(className(handler), topic(handler));
            repository.createResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/demo/" + className(handler) + ".java",
                    source.getBytes(StandardCharsets.UTF_8), false, "text/x-java", true);
        }
    }
}
