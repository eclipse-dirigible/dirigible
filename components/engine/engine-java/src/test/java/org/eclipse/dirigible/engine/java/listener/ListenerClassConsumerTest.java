/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.callable.CallableResultAndException;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.configurations.tenant.TenantConfigurationService;
import org.eclipse.dirigible.components.listeners.config.ActiveMQConnectionArtifactsFactory;
import org.eclipse.dirigible.components.listeners.service.DestinationNameManager;
import org.eclipse.dirigible.components.listeners.service.TenantPropertyManager;
import org.eclipse.dirigible.engine.java.component.ComponentContainer;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.eclipse.dirigible.sdk.messaging.MessageHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import jakarta.jms.Connection;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageListener;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;

/**
 * {@link ListenerClassConsumer}'s dispatch must re-establish the sending tenant's configuration
 * overrides - not just its identity - before invoking the handler, mirroring what
 * {@code TenantConfigurationInitFilter} does for HTTP requests. Otherwise a generated
 * notification's {@code {appUrl}} (or any other tenant-overridable config value) would silently
 * read the global default for every tenant instead of its own.
 */
class ListenerClassConsumerTest {

    /** A listener that records the thread-scoped configuration visible while it runs. */
    static class RecordingHandler implements MessageHandler {

        Map<String, String> observedDuringDispatch;

        @Override
        public String destination() {
            return "notifications";
        }

        @Override
        public void onMessage(String message) {
            observedDuringDispatch = Configuration.getThreadConfiguration();
        }
    }

    private TenantConfigurationService tenantConfigurationService;
    private RecordingHandler handler;
    private MessageListener capturedListener;

    @BeforeEach
    @SuppressWarnings("rawtypes")
    void setUp() throws Exception {
        ComponentContainer componentContainer = mock(ComponentContainer.class);
        ActiveMQConnectionArtifactsFactory connectionFactory = mock(ActiveMQConnectionArtifactsFactory.class);
        TenantContext tenantContext = mock(TenantContext.class);
        TenantPropertyManager tenantPropertyManager = mock(TenantPropertyManager.class);
        tenantConfigurationService = mock(TenantConfigurationService.class);
        Tenant defaultTenant = mock(Tenant.class);
        handler = new RecordingHandler();

        when(componentContainer.instanceOf(RecordingHandler.class)).thenReturn(Optional.of(handler));

        Connection connection = mock(Connection.class);
        Session session = mock(Session.class);
        Queue queue = mock(Queue.class);
        MessageConsumer messageConsumer = mock(MessageConsumer.class);
        when(connectionFactory.createConnection(any())).thenReturn(connection);
        when(connectionFactory.createSession(connection)).thenReturn(session);
        when(session.createQueue("notifications")).thenReturn(queue);
        when(session.createConsumer(queue)).thenReturn(messageConsumer);

        when(tenantPropertyManager.getCurrentTenantId(any())).thenReturn("acme");
        // Runs the dispatch inline on this test thread, the same contract the sibling
        // ScheduledClassConsumerTest relies on for TenantContext.executeForEachTenant.
        when(tenantContext.execute(any(String.class), any())).thenAnswer(
                invocation -> ((CallableResultAndException) invocation.getArgument(1)).call());
        // The subscription fan-out: one (default) tenant is all this test needs, so the logical name
        // is also the physical one. ListenerClassConsumerTenantSubscriptionTest covers the fan-out.
        Tenant currentTenant = mock(Tenant.class);
        when(currentTenant.getId()).thenReturn("default-tenant");
        when(tenantContext.getCurrentTenant()).thenReturn(currentTenant);
        when(tenantContext.executeForEachTenant(any())).thenAnswer(invocation -> {
            ((CallableResultAndException) invocation.getArgument(0)).call();
            return List.of();
        });
        DestinationNameManager destinationNameManager = mock(DestinationNameManager.class);
        when(destinationNameManager.toTenantName("notifications")).thenReturn("notifications");

        ListenerClassConsumer listenerConsumer = new ListenerClassConsumer(componentContainer, connectionFactory, tenantContext,
                tenantPropertyManager, defaultTenant, tenantConfigurationService, destinationNameManager);
        listenerConsumer.onClassLoaded(new LoadedClass("sample", RecordingHandler.class.getName(), RecordingHandler.class,
                RecordingHandler.class.getClassLoader()));

        ArgumentCaptor<MessageListener> captor = ArgumentCaptor.forClass(MessageListener.class);
        verify(messageConsumer).setMessageListener(captor.capture());
        capturedListener = captor.getValue();
    }

    @Test
    void dispatchLoadsTheTenantsConfigOverridesBeforeInvokingTheHandler() throws Exception {
        Map<String, String> tenantOverrides = new HashMap<>();
        tenantOverrides.put("DIRIGIBLE_APP_BASE_URL", "https://acme.example.com");
        when(tenantConfigurationService.resolveInjectableForCurrentTenant()).thenReturn(tenantOverrides);

        TextMessage message = mock(TextMessage.class);
        when(message.getText()).thenReturn("{}");

        capturedListener.onMessage(message);

        assertEquals(tenantOverrides, handler.observedDuringDispatch,
                "the handler must see the sending tenant's own config overrides, not just its identity");
    }

    @Test
    void theInjectedConfigIsClearedAfterDispatchSoItNeverLeaksOntoThePooledThread() throws Exception {
        when(tenantConfigurationService.resolveInjectableForCurrentTenant()).thenReturn(
                Map.of("DIRIGIBLE_APP_BASE_URL", "https://acme.example.com"));

        TextMessage message = mock(TextMessage.class);
        when(message.getText()).thenReturn("{}");

        capturedListener.onMessage(message);

        assertTrue(Configuration.getThreadConfiguration()
                                .isEmpty(),
                "the broker thread must not keep the previous message's tenant configuration");
    }
}
