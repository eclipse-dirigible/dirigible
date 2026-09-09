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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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

import jakarta.jms.Connection;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Queue;
import jakarta.jms.Session;

/**
 * A subscription the broker refuses must be retried, and the retry must not depend on anything a
 * steady-state instance never does. The client-Java generation is rebuilt only on publish and the
 * post-provisioning step runs only when a tenant was actually provisioned, so a listener that lost
 * the race against the broker's transport at boot stayed unsubscribed for the life of the process,
 * and a topic discarded every message published to it without a word (issue #7217).
 *
 * <p>
 * The refusal is reproduced the way it really arrives: {@code ActiveMQConnectionArtifactsFactory}
 * wraps the broker's {@code JMSException} in an {@link IllegalStateException}, which the consumer
 * used not to catch at all.
 */
class ListenerClassConsumerRetryTest {

    /** A typed listener on a queue, so the assertions can read {@code createQueue} arguments. */
    static class OrdersHandler implements MessageHandler {

        @Override
        public String destination() {
            return "orders";
        }

        @Override
        public void onMessage(String message) {
            // The subscription, not the dispatch, is what this test is about.
        }
    }

    /** A typed listener on a destination shared with another deployment. */
    static class GlobalOrdersHandler implements MessageHandler {

        @Override
        public String destination() {
            return DestinationNameManager.GLOBAL_MARKER + "codbex.orders";
        }

        @Override
        public void onMessage(String message) {
            // The subscription, not the dispatch, is what this test is about.
        }
    }

    private static final String DEFAULT_TENANT_ID = "default-tenant";

    private final Map<String, Tenant> provisionedTenants = new LinkedHashMap<>();

    private final AtomicReference<String> currentTenantId = new AtomicReference<>();

    /** While set, every attempt to attach to the broker fails the way a starting broker fails. */
    private final AtomicBoolean brokerRefusing = new AtomicBoolean();

    /** When set, only attempts made in that tenant's context fail. */
    private final AtomicReference<String> brokerRefusingForTenant = new AtomicReference<>();

    private Session session;
    private TenantContext tenantContext;
    private ListenerClassConsumer consumer;

    @BeforeEach
    @SuppressWarnings("rawtypes")
    void setUp() throws Exception {
        addTenant(DEFAULT_TENANT_ID);
        addTenant("acme");

        ComponentContainer componentContainer = mock(ComponentContainer.class);
        when(componentContainer.instanceOf(OrdersHandler.class)).thenReturn(Optional.of(new OrdersHandler()));
        when(componentContainer.instanceOf(GlobalOrdersHandler.class)).thenReturn(Optional.of(new GlobalOrdersHandler()));

        ActiveMQConnectionArtifactsFactory connectionFactory = mock(ActiveMQConnectionArtifactsFactory.class);
        Connection connection = mock(Connection.class);
        session = mock(Session.class);
        when(connectionFactory.createConnection(any(), any())).thenAnswer(invocation -> {
            String refusingFor = brokerRefusingForTenant.get();
            if (brokerRefusing.get() || (refusingFor != null && refusingFor.equals(currentTenantId.get()))) {
                throw new IllegalStateException("Failed to create connection to ActiveMQ");
            }
            return connection;
        });
        when(connectionFactory.createSession(connection)).thenReturn(session);
        when(session.createQueue(anyString())).thenReturn(mock(Queue.class));
        when(session.createConsumer(any())).thenReturn(mock(MessageConsumer.class));

        tenantContext = mock(TenantContext.class);
        when(tenantContext.getCurrentTenant()).thenAnswer(invocation -> provisionedTenants.get(currentTenantId.get()));
        when(tenantContext.executeForEachTenant(any())).thenAnswer(invocation -> {
            for (String tenantId : new ArrayList<>(provisionedTenants.keySet())) {
                currentTenantId.set(tenantId);
                ((CallableResultAndException) invocation.getArgument(0)).call();
            }
            currentTenantId.set(null);
            return List.of();
        });

        DestinationNameManager destinationNameManager = mock(DestinationNameManager.class);
        when(destinationNameManager.toTenantName(anyString())).thenAnswer(invocation -> {
            String logicalName = invocation.getArgument(0);
            if (DestinationNameManager.isGlobal(logicalName)) {
                return logicalName.substring(DestinationNameManager.GLOBAL_MARKER.length());
            }
            String tenantId = currentTenantId.get();
            return DEFAULT_TENANT_ID.equals(tenantId) ? logicalName : tenantId + "###" + logicalName;
        });

        TenantConfigurationService tenantConfigurationService = mock(TenantConfigurationService.class);
        when(tenantConfigurationService.resolveInjectableForCurrentTenant()).thenReturn(new HashMap<>());

        consumer = new ListenerClassConsumer(componentContainer, connectionFactory, tenantContext, mock(TenantPropertyManager.class),
                mock(Tenant.class), tenantConfigurationService, destinationNameManager);
    }

    @Test
    void aSubscriptionRefusedAtLoadIsOpenedByTheNextReconciliationPass() throws Exception {
        brokerRefusing.set(true);
        loadListener();
        verify(session, never()).createQueue(anyString());

        brokerRefusing.set(false);
        consumer.reconcile();

        verify(session).createQueue("orders");
        verify(session).createQueue("acme###orders");
    }

    /**
     * The refusal arrives as an unchecked exception, and the consumer caught only {@code JMSException}.
     * So the first tenant's failure escaped the fan-out and every tenant behind it was skipped without
     * even an attempt - the failure was not merely un-retried, it was contagious.
     */
    @Test
    void aRefusalInOneTenantDoesNotAbortTheFanOutOverTheOthers() throws Exception {
        brokerRefusingForTenant.set(DEFAULT_TENANT_ID);

        loadListener();

        verify(session, never()).createQueue("orders");
        verify(session).createQueue("acme###orders");
    }

    @Test
    void theTenantARefusalSkippedIsSubscribedByTheNextReconciliationPass() throws Exception {
        brokerRefusingForTenant.set(DEFAULT_TENANT_ID);
        loadListener();

        brokerRefusingForTenant.set(null);
        consumer.reconcile();

        verify(session).createQueue("orders");
        verify(session, times(1)).createQueue("acme###orders");
    }

    @Test
    void aRefusedSubscriptionIsNotRecordedSoTheRetryOpensItExactlyOnce() throws Exception {
        brokerRefusing.set(true);
        loadListener();

        brokerRefusing.set(false);
        consumer.reconcile();
        consumer.reconcile();

        // A second consumer on the same queue in the same tenant competes for its messages.
        verify(session, times(1)).createQueue("orders");
        verify(session, times(1)).createQueue("acme###orders");
    }

    /**
     * A global destination is subscribed once for the deployment, outside any tenant, and that half had
     * no retry at all: {@code subscribeGlobal} was reachable only from a class load.
     */
    @Test
    void aRefusedGlobalSubscriptionIsOpenedByTheNextReconciliationPass() throws Exception {
        brokerRefusing.set(true);
        loadGlobalListener();
        verify(session, never()).createQueue(anyString());

        brokerRefusing.set(false);
        consumer.reconcile();

        verify(session, times(1)).createQueue("codbex.orders");
    }

    @Test
    void aPassThatSucceededCostsTheNextTickNothing() throws Exception {
        loadListener();
        verify(tenantContext, times(1)).executeForEachTenant(any());

        consumer.reconcile();

        // Not even the tenant lookup: executeForEachTenant reads the provisioned tenants from the
        // database, and this tick fires every 30 seconds for the life of the process.
        verify(tenantContext, times(1)).executeForEachTenant(any());
        verify(session, times(1)).createQueue("orders");
    }

    private void addTenant(String tenantId) {
        Tenant tenant = mock(Tenant.class);
        when(tenant.getId()).thenReturn(tenantId);
        when(tenant.isDefault()).thenReturn(DEFAULT_TENANT_ID.equals(tenantId));
        provisionedTenants.put(tenantId, tenant);
    }

    private void loadListener() {
        consumer.onClassLoaded(
                new LoadedClass("sample", OrdersHandler.class.getName(), OrdersHandler.class, OrdersHandler.class.getClassLoader()));
    }

    private void loadGlobalListener() {
        consumer.onClassLoaded(new LoadedClass("sample", GlobalOrdersHandler.class.getName(), GlobalOrdersHandler.class,
                GlobalOrdersHandler.class.getClassLoader()));
    }
}
