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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 * A client-Java listener must subscribe to the SAME physical destination the producer publishes to.
 * The producer prefixes a non-default tenant's destination name; this consumer used to subscribe to
 * the raw name once, JVM-wide, so a generated application's whole glue layer — triggers,
 * notifications, roll-ups, transitions — was silently dead in every non-default tenant while
 * looking perfectly healthy on the default one.
 */
class ListenerClassConsumerTenantSubscriptionTest {

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

    private static final String DEFAULT_TENANT_ID = "default-tenant";

    /** The tenants {@code executeForEachTenant} fans out over; mutable, so a test can add one. */
    private final Map<String, Tenant> provisionedTenants = new LinkedHashMap<>();

    /** The tenant whose context the fan-out is currently simulating. */
    private final AtomicReference<String> currentTenantId = new AtomicReference<>();

    private Session session;
    private ListenerClassConsumer consumer;

    @BeforeEach
    @SuppressWarnings("rawtypes")
    void setUp() throws Exception {
        addTenant(DEFAULT_TENANT_ID);
        addTenant("acme");

        ComponentContainer componentContainer = mock(ComponentContainer.class);
        when(componentContainer.instanceOf(OrdersHandler.class)).thenReturn(Optional.of(new OrdersHandler()));

        ActiveMQConnectionArtifactsFactory connectionFactory = mock(ActiveMQConnectionArtifactsFactory.class);
        Connection connection = mock(Connection.class);
        session = mock(Session.class);
        when(connectionFactory.createConnection(any())).thenReturn(connection);
        when(connectionFactory.createSession(connection)).thenReturn(session);
        when(session.createQueue(anyString())).thenReturn(mock(Queue.class));
        when(session.createConsumer(any())).thenReturn(mock(MessageConsumer.class));

        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getCurrentTenant()).thenAnswer(invocation -> provisionedTenants.get(currentTenantId.get()));
        // Run the callable once per provisioned tenant, inline, with that tenant current — the contract
        // the real TenantContext offers and the sibling ScheduledClassConsumerTest relies on too.
        when(tenantContext.executeForEachTenant(any())).thenAnswer(invocation -> {
            for (String tenantId : new ArrayList<>(provisionedTenants.keySet())) {
                currentTenantId.set(tenantId);
                ((CallableResultAndException) invocation.getArgument(0)).call();
            }
            currentTenantId.set(null);
            return List.of();
        });

        // Stands in for the real rule: the default tenant keeps the logical name, everyone else is
        // prefixed. DestinationNameManagerTest owns that rule; here it only has to be applied.
        DestinationNameManager destinationNameManager = mock(DestinationNameManager.class);
        when(destinationNameManager.toTenantName(anyString())).thenAnswer(invocation -> {
            String logicalName = invocation.getArgument(0);
            String tenantId = currentTenantId.get();
            return DEFAULT_TENANT_ID.equals(tenantId) ? logicalName : tenantId + "###" + logicalName;
        });

        TenantConfigurationService tenantConfigurationService = mock(TenantConfigurationService.class);
        when(tenantConfigurationService.resolveInjectableForCurrentTenant()).thenReturn(new HashMap<>());

        consumer = new ListenerClassConsumer(componentContainer, connectionFactory, tenantContext, mock(TenantPropertyManager.class),
                mock(Tenant.class), tenantConfigurationService, destinationNameManager);
    }

    @Test
    void aLoadedListenerSubscribesInEveryProvisionedTenantUnderThatTenantsDestinationName() throws Exception {
        loadListener();

        verify(session).createQueue("orders");
        verify(session).createQueue("acme###orders");
    }

    @Test
    void aTenantProvisionedAfterTheClassWasLoadedIsSubscribedByThePostProvisioningStep() throws Exception {
        loadListener();

        addTenant("beta");
        consumer.execute();

        verify(session).createQueue("beta###orders");
    }

    @Test
    void toppingUpDoesNotReSubscribeATenantThatIsAlreadyConnected() throws Exception {
        loadListener();

        consumer.execute();

        // Re-subscribing would close nothing and duplicate everything: two consumers on one queue in
        // the same tenant means a message reaches the handler once at random and the other consumer
        // competes for the next one.
        verify(session, times(1)).createQueue("orders");
        verify(session, times(1)).createQueue("acme###orders");
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
}
