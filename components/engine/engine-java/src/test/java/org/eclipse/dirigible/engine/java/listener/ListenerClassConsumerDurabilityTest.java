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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.eclipse.dirigible.components.base.callable.CallableResultAndException;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.configurations.tenant.TenantConfigurationService;
import org.eclipse.dirigible.components.listeners.config.ActiveMQConnectionArtifactsFactory;
import org.eclipse.dirigible.components.listeners.service.DestinationNameManager;
import org.eclipse.dirigible.components.listeners.service.TenantPropertyManager;
import org.eclipse.dirigible.engine.java.component.ComponentContainer;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.eclipse.dirigible.sdk.messaging.ListenerKind;
import org.eclipse.dirigible.sdk.messaging.MessageHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import jakarta.jms.Connection;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.Topic;
import jakarta.jms.TopicSubscriber;

/**
 * A topic subscription must survive its subscriber going away. The handler set is torn down and
 * re-registered on every republish, and a plain topic subscriber receives only what is published
 * while it is connected - so an event raised inside that window would be dropped with nothing to
 * show it ever existed. A durable subscription makes the broker hold those messages until the
 * handler reconnects, which only works if the id it reconnects under is the same one every time.
 *
 * <p>
 * Queues are deliberately left alone: a queue already retains its messages for a consumer that is
 * not there.
 */
class ListenerClassConsumerDurabilityTest {

    /** Subscribes to a topic - the kind that loses messages without a durable subscription. */
    static class TopicHandler implements MessageHandler {

        @Override
        public String destination() {
            return "fines-Fine-transitioned";
        }

        @Override
        public ListenerKind kind() {
            return ListenerKind.TOPIC;
        }

        @Override
        public void onMessage(String message) {
            // the dispatch path is covered by ListenerClassConsumerTest
        }
    }

    /** Subscribes to a queue, which retains messages on its own. */
    static class QueueHandler implements MessageHandler {

        @Override
        public String destination() {
            return "fines-inbox";
        }

        @Override
        public void onMessage(String message) {
            // the dispatch path is covered by ListenerClassConsumerTest
        }
    }

    private ActiveMQConnectionArtifactsFactory connectionFactory;
    private ComponentContainer componentContainer;
    private Session session;
    private Topic topic;
    private Queue queue;
    private ListenerClassConsumer listenerConsumer;

    @BeforeEach
    @SuppressWarnings("rawtypes")
    void setUp() throws Exception {
        componentContainer = mock(ComponentContainer.class);
        connectionFactory = mock(ActiveMQConnectionArtifactsFactory.class);
        TenantContext tenantContext = mock(TenantContext.class);
        TenantPropertyManager tenantPropertyManager = mock(TenantPropertyManager.class);
        TenantConfigurationService tenantConfigurationService = mock(TenantConfigurationService.class);
        Tenant defaultTenant = mock(Tenant.class);

        Connection connection = mock(Connection.class);
        session = mock(Session.class);
        topic = mock(Topic.class);
        queue = mock(Queue.class);
        when(connectionFactory.createConnection(any(), any())).thenReturn(connection);
        when(connectionFactory.createSession(connection)).thenReturn(session);
        when(session.createTopic(anyString())).thenReturn(topic);
        when(session.createQueue(anyString())).thenReturn(queue);
        when(session.createDurableSubscriber(any(Topic.class), anyString())).thenReturn(mock(TopicSubscriber.class));
        when(session.createConsumer(any())).thenReturn(mock(MessageConsumer.class));

        Tenant currentTenant = mock(Tenant.class);
        when(currentTenant.getId()).thenReturn("default-tenant");
        when(tenantContext.getCurrentTenant()).thenReturn(currentTenant);
        when(tenantContext.executeForEachTenant(any())).thenAnswer(invocation -> {
            ((CallableResultAndException) invocation.getArgument(0)).call();
            return List.of();
        });
        // The physical name is tenant-resolved; a fixed mapping is all this test needs, and it is what
        // the durable id is derived from alongside the handler's own name.
        DestinationNameManager destinationNameManager = mock(DestinationNameManager.class);
        when(destinationNameManager.toTenantName(anyString())).thenAnswer(invocation -> "acme_" + invocation.getArgument(0));

        listenerConsumer = new ListenerClassConsumer(componentContainer, connectionFactory, tenantContext, tenantPropertyManager,
                defaultTenant, tenantConfigurationService, destinationNameManager);
    }

    private void load(Class<?> handlerType, Object instance) {
        when(componentContainer.instanceOf(handlerType)).thenReturn(Optional.of(instance));
        listenerConsumer.onClassLoaded(new LoadedClass("sample", handlerType.getName(), handlerType, handlerType.getClassLoader()));
    }

    @Test
    void aTopicSubscriptionIsDurableSoAnEventPublishedWhileItIsDownIsNotLost() throws Exception {
        load(TopicHandler.class, new TopicHandler());

        ArgumentCaptor<String> clientId = ArgumentCaptor.forClass(String.class);
        verify(connectionFactory).createConnection(any(), clientId.capture());
        assertNotNull(clientId.getValue(), "a durable subscription needs the connection to carry a client id");

        // The subscription must be opened as durable under that same id - an id on the connection
        // alone changes nothing about whether messages are retained.
        verify(session).createDurableSubscriber(eq(topic), eq(clientId.getValue()));
        verify(session, never()).createConsumer(any());
    }

    @Test
    void theDurableIdIsUnchangedByARepublishSoTheSubscriptionIsResumedRatherThanReplaced() throws Exception {
        load(TopicHandler.class, new TopicHandler());
        // Re-registering is exactly what a republish does: tear the handler down, register it again.
        load(TopicHandler.class, new TopicHandler());

        ArgumentCaptor<String> clientId = ArgumentCaptor.forClass(String.class);
        verify(connectionFactory, times(2)).createConnection(any(), clientId.capture());

        List<String> ids = clientId.getAllValues();
        assertNotNull(ids.get(0), "a durable subscription needs the connection to carry a client id");
        assertEquals(ids.get(0), ids.get(1), "a republish must reconnect under the same id - a fresh one each time would start an empty "
                + "subscription and strand the messages held for the previous one");
    }

    @Test
    void theDurableIdDistinguishesTheHandlerAndItsPhysicalDestination() throws Exception {
        load(TopicHandler.class, new TopicHandler());

        ArgumentCaptor<String> clientId = ArgumentCaptor.forClass(String.class);
        verify(connectionFactory).createConnection(any(), clientId.capture());

        String id = clientId.getValue();
        assertTrue(id.contains(TopicHandler.class.getName()
                                                 .replace('$', '_')),
                "the id must name the handler so two handlers on one topic get their own subscription, got: " + id);
        assertTrue(id.contains("acme_fines-Fine-transitioned"),
                "the id must name the tenant-resolved destination so one handler's tenants do not share a subscription, got: " + id);
    }

    @Test
    void aQueueSubscriptionIsLeftAloneBecauseAQueueAlreadyRetainsItsMessages() throws Exception {
        load(QueueHandler.class, new QueueHandler());

        verify(session).createConsumer(queue);
        verify(session, never()).createDurableSubscriber(any(), anyString());

        ArgumentCaptor<String> clientId = ArgumentCaptor.forClass(String.class);
        verify(connectionFactory).createConnection(any(), clientId.capture());
        assertNull(clientId.getValue(), "a queue consumer needs no client id - claiming one only risks a collision");
    }
}
