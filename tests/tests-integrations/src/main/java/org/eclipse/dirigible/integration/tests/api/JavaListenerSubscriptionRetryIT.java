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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.api.messaging.MessagingFacade;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import ch.qos.logback.classic.Level;

/**
 * A client-Java listener whose subscription the broker refuses must end up subscribed anyway.
 *
 * <p>
 * On a restart the attempt races the embedded broker's {@code vm://localhost} transport, and when
 * it lost that race nothing ever tried again: the generation is rebuilt only on publish, and the
 * post-provisioning top-up runs only when a tenant was actually provisioned. A topic discards
 * whatever it delivers to nobody, so the handler stayed silent for the life of the process while
 * every generated controller answered 200 - which is how an intent application's {@code onCreate}
 * process trigger came to write the row and start nothing (issue #7217).
 *
 * <p>
 * The refusal is injected at the one place it really originates, and in its real shape: the
 * connection factory turns the broker's {@code JMSException} into an {@link IllegalStateException}.
 * It is scoped to this probe listener's own durable subscription id, so the platform's other
 * messaging is untouched. The recovery is then asserted end to end - a message published to the
 * topic has to reach the handler and come back on its queue - with nothing between the deploy and
 * the round trip but the passage of time.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class JavaListenerSubscriptionRetryIT extends IntegrationTest {

    private static final String PROJECT = "java-listener-retry-it";
    private static final String HANDLER_CLASS = "RetryProbeListener";
    private static final String TOPIC = PROJECT + "-in";
    private static final String QUEUE = PROJECT + "-out";

    /**
     * Short enough to keep the test quick, long enough to be a real timer rather than a direct call.
     */
    private static final String RECONCILE_INTERVAL_SECONDS = "2";

    private static final int SUBSCRIPTION_FAILURE_TIMEOUT_SECONDS = 60;
    private static final int ROUND_TRIP_TIMEOUT_SECONDS = 90;

    /** Per attempt - short, because a failed attempt is retried with a fresh publish. */
    private static final long RECEIVE_TIMEOUT_MILLIS = 2000;

    private static String previousReconcileInterval;

    /** While set, this probe's subscription attempts fail the way a starting broker fails them. */
    private volatile boolean refuseProbeSubscription = true;

    @MockitoSpyBean
    private ActiveMQConnectionArtifactsFactory connectionArtifactsFactory;

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

    /**
     * One answer, installed once and steered by a flag. Re-stubbing a spy other threads are calling is
     * how these tests turn flaky.
     */
    @BeforeEach
    void armTheBrokerRefusal() {
        // Attached here rather than in a field: Spring re-initializes logback while it starts the
        // application context, which drops an appender attached any earlier.
        consumerLogs = new LogsAsserter(ListenerClassConsumer.class, Level.WARN);

        doAnswer(invocation -> {
            String clientId = invocation.getArgument(1);
            // A queue subscription passes no client id at all, and so does the platform's own shared
            // connection - the one-arg overload delegates here with null, and a spy intercepts even that
            // self-call. Everything but this probe's durable topic subscription must behave normally.
            if (refuseProbeSubscription && clientId != null && clientId.contains(HANDLER_CLASS)) {
                throw new IllegalStateException("Failed to create connection to ActiveMQ");
            }
            return invocation.callRealMethod();
        }).when(connectionArtifactsFactory)
          .createConnection(any(), any());
    }

    @Test
    void aSubscriptionTheBrokerRefusedIsRetriedUntilItConnects() {
        deployProbeListener();

        // Without this the whole test would pass on a spy that never intercepted anything.
        Awaitility.await()
                  .pollInterval(1, TimeUnit.SECONDS)
                  .atMost(SUBSCRIPTION_FAILURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .until(() -> consumerLogs.containsMessage("Failed to start listener [demo." + HANDLER_CLASS + "]", Level.WARN));

        refuseProbeSubscription = false;

        // Nothing republishes and no tenant is provisioned from here on: only the reconciliation timer
        // can open this subscription now.
        assertRoundTrip();
    }

    /**
     * Publish into the topic and draw the listener's echo back off its queue, retrying the whole
     * exchange. A durable subscription retains only what is published once it exists, and the refused
     * attempt never created it, so every message sent before the retry lands is simply gone.
     */
    private void assertRoundTrip() {
        String message = "ping-" + System.currentTimeMillis();
        String[] received = new String[1];

        Awaitility.await()
                  .pollInterval(1, TimeUnit.SECONDS)
                  .atMost(ROUND_TRIP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .until(() -> {
                      MessagingFacade.sendToTopic(TOPIC, message);
                      try {
                          received[0] = MessagingFacade.receiveFromQueue(QUEUE, RECEIVE_TIMEOUT_MILLIS);
                          return true;
                      } catch (RuntimeException notYet) {
                          return false;
                      }
                  });

        assertEquals("echo:" + message, received[0],
                "the listener must handle what is published once the retry has opened its subscription");
    }

    /** Write a client listener source into the registry and reconcile it. */
    private void deployProbeListener() {
        String source = """
                package demo;

                import org.eclipse.dirigible.sdk.component.Component;
                import org.eclipse.dirigible.sdk.messaging.ListenerKind;
                import org.eclipse.dirigible.sdk.messaging.MessageHandler;
                import org.eclipse.dirigible.sdk.messaging.Producer;

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
                        Producer.sendToQueue("%s", "echo:" + message);
                    }
                }
                """.formatted(HANDLER_CLASS, TOPIC, QUEUE);

        String path = IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + PROJECT + "/demo/" + HANDLER_CLASS + ".java";
        repository.createResource(path, source.getBytes(StandardCharsets.UTF_8), false, "text/x-java", true);
        synchronizationProcessor.forceProcessSynchronizers();
    }
}
