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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.api.messaging.MessagingFacade;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.initializers.synchronizer.SynchronizationProcessor;
import org.eclipse.dirigible.components.listeners.config.ActiveMQConnectionArtifactsFactory;
import org.eclipse.dirigible.components.listeners.domain.Listener;
import org.eclipse.dirigible.components.listeners.service.ListenerService;
import org.eclipse.dirigible.integration.tests.api.java.messaging.MessagesHolder;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.jms.ExceptionListener;

/**
 * A {@code .listener} artefact whose subscription the broker refuses at startup is retried on every
 * synchronization pass until it subscribes (#7248). Before the fix the second pass promoted it to
 * FATAL and the third stripped it from synchronization for good, so every message published to its
 * topic afterwards was silently lost.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ListenerArtefactSubscriptionRetryIT extends IntegrationTest {

    private static final String PROJECT = "listener-artefact-retry-it";
    private static final String LISTENER_LOCATION = "/" + PROJECT + "/probe.listener";
    private static final String HANDLER_PATH = PROJECT + "/handler.js";
    private static final String TOPIC = PROJECT + "-topic";

    /**
     * Keeps a pass with a refused listener short: one in-pass retry a second later, not ten a 10 s
     * apart.
     */
    private static final String CROSS_RETRY_COUNT = "1";
    private static final String CROSS_RETRY_INTERVAL_MILLIS = "1000";

    /**
     * The idle instance's own retry cadence, shortened so the heal arrives within the test's patience.
     */
    private static final String FAILED_RETRY_INTERVAL_SECONDS = "2";

    private static final int HEAL_TIMEOUT_SECONDS = 90;
    private static final int MESSAGE_TIMEOUT_SECONDS = 60;

    private static String previousCrossRetryCount;
    private static String previousCrossRetryInterval;
    private static String previousFailedRetryInterval;

    private volatile boolean refuseProbeSubscription = true;

    @MockitoSpyBean
    private ActiveMQConnectionArtifactsFactory connectionArtifactsFactory;

    @Autowired
    private IRepository repository;

    @Autowired
    private SynchronizationProcessor synchronizationProcessor;

    @Autowired
    private ListenerService listenerService;

    @BeforeAll
    static void shortenTheInPassRetry() {
        previousCrossRetryCount = DirigibleConfig.SYNCHRONIZER_CROSS_RETRY_COUNT.getStringValue();
        previousCrossRetryInterval = DirigibleConfig.SYNCHRONIZER_CROSS_RETRY_INTERVAL_MILLIS.getStringValue();
        previousFailedRetryInterval = DirigibleConfig.SYNCHRONIZER_FAILED_RETRY_INTERVAL_SECONDS.getStringValue();
        DirigibleConfig.SYNCHRONIZER_CROSS_RETRY_COUNT.setStringValue(CROSS_RETRY_COUNT);
        DirigibleConfig.SYNCHRONIZER_CROSS_RETRY_INTERVAL_MILLIS.setStringValue(CROSS_RETRY_INTERVAL_MILLIS);
        DirigibleConfig.SYNCHRONIZER_FAILED_RETRY_INTERVAL_SECONDS.setStringValue(FAILED_RETRY_INTERVAL_SECONDS);
    }

    @AfterAll
    static void restoreTheInPassRetry() {
        DirigibleConfig.SYNCHRONIZER_CROSS_RETRY_COUNT.setStringValue(previousCrossRetryCount);
        DirigibleConfig.SYNCHRONIZER_CROSS_RETRY_INTERVAL_MILLIS.setStringValue(previousCrossRetryInterval);
        DirigibleConfig.SYNCHRONIZER_FAILED_RETRY_INTERVAL_SECONDS.setStringValue(previousFailedRetryInterval);
    }

    @BeforeEach
    void armTheBrokerRefusal() {
        MessagesHolder.clearLatestReceivedMessage();
        doAnswer(invocation -> {
            // The artefact path opens its connection with the listener's own exception handler, which
            // carries the handler path - the one thing that identifies this probe. Everything else,
            // the platform's shared connection included, must behave normally.
            if (refuseProbeSubscription && HANDLER_PATH.equals(handlerPathOf(invocation.getArgument(0)))) {
                throw new IllegalStateException("Failed to create connection to ActiveMQ");
            }
            return invocation.callRealMethod();
        }).when(connectionArtifactsFactory)
          .createConnection(any(), any());
    }

    @Test
    void aSubscriptionTheBrokerRefusedIsRetriedOnEveryPassUntilItConnects() {
        deployProbeListener();

        // Pass 1: the refusal happened (without this the test would pass on a spy that intercepted
        // nothing), and it reads FAILED - not CREATED with nothing subscribed.
        synchronizationProcessor.forceProcessSynchronizers();
        Listener afterFirstPass = probeListener();
        assertEquals(ArtefactLifecycle.FAILED, afterFirstPass.getLifecycle(), "a refused subscription must read FAILED");
        assertNotEquals(Boolean.TRUE, afterFirstPass.getRunning(), "nothing subscribed, so nothing may read as running");

        // Pass 2: still refused. This is the pass that used to register FATAL.
        synchronizationProcessor.forceProcessSynchronizers();
        assertEquals(ArtefactLifecycle.FAILED, probeListener().getLifecycle(), "a transiently refused listener must never go FATAL");

        refuseProbeSubscription = false;

        // The broker accepts. Nothing is published and nothing is forced from here on: only the idle
        // instance's own FAILED retry can subscribe the listener now. Before the fix the artefact was
        // stripped from every later pass, and a pass without a registry change ran no phase at all.
        Awaitility.await()
                  .pollInterval(1, TimeUnit.SECONDS)
                  .atMost(HEAL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .until(() -> ArtefactLifecycle.CREATED.equals(probeListener().getLifecycle()));
        assertEquals(Boolean.TRUE, probeListener().getRunning(), "the retried listener must be running");

        String message = "ping-" + System.currentTimeMillis();
        MessagingFacade.sendToTopic(TOPIC, message);
        Awaitility.await()
                  .atMost(MESSAGE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                  .until(() -> message.equals(MessagesHolder.getLatestReceivedMessage()));
    }

    private Listener probeListener() {
        List<Listener> listeners = listenerService.findByLocation(LISTENER_LOCATION);
        assertEquals(1, listeners.size(), "exactly one artefact must be recorded for " + LISTENER_LOCATION);
        return listeners.get(0);
    }

    private static String handlerPathOf(ExceptionListener exceptionListener) {
        if (exceptionListener == null || !"ListenerExceptionHandler".equals(exceptionListener.getClass()
                                                                                             .getSimpleName())) {
            return null;
        }
        return (String) ReflectionTestUtils.getField(exceptionListener, "handlerPath");
    }

    private void deployProbeListener() {
        String handler = """
                const MessagesHolder = Java.type("org.eclipse.dirigible.integration.tests.api.java.messaging.MessagesHolder");

                export function onMessage(message) {
                    MessagesHolder.setLatestReceivedMessage(message);
                }

                export function onError(error) {
                    MessagesHolder.setLatestReceivedError(error);
                }
                """;
        String listener = """
                {
                    "name": "%s",
                    "kind": "T",
                    "handler": "%s",
                    "description": "retried until the broker accepts"
                }
                """.formatted(TOPIC, HANDLER_PATH);
        repository.createResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + HANDLER_PATH, handler.getBytes(StandardCharsets.UTF_8),
                false, "text/javascript", true);
        repository.createResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + LISTENER_LOCATION, listener.getBytes(StandardCharsets.UTF_8),
                false, "application/json", true);
    }
}
