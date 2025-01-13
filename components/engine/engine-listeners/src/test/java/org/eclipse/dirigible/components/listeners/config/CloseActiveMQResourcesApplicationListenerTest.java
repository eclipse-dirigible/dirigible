/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.listeners.config;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.jms.Session;
import org.apache.activemq.broker.BrokerService;
import org.eclipse.dirigible.components.listeners.service.ListenersManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;

/**
 * The Class CloseActiveMQResourcesApplicationListenerTest.
 */
@ExtendWith(MockitoExtension.class)
class CloseActiveMQResourcesApplicationListenerTest {

    /** The listener. */
    @InjectMocks
    private CloseActiveMQResourcesApplicationListener listener;

    /** The broker. */
    @Mock
    private BrokerService broker;

    /** The connection. */
    @Mock
    private Connection connection;

    /** The session. */
    @Mock
    private Session session;

    /** The listeners manager. */
    @Mock
    private ListenersManager listenersManager;

    /** The closed event. */
    @Mock
    private ContextClosedEvent closedEvent;

    /** The stopped event. */
    @Mock
    private ContextStoppedEvent stoppedEvent;

    /** The started event. */
    @Mock
    private ContextStartedEvent startedEvent;

    /**
     * Test on context closed event.
     *
     * @throws Exception the exception
     */
    @Test
    void testOnContextClosedEvent() throws Exception {
        listener.onApplicationEvent(closedEvent);

        verifyClosedResources();
    }

    /**
     * Test on context stopped event.
     *
     * @throws Exception the exception
     */
    @Test
    void testOnContextStoppedEvent() throws Exception {
        listener.onApplicationEvent(stoppedEvent);

        verifyClosedResources();
    }

    /**
     * Verify closed resources.
     *
     * @throws JMSException the JMS exception
     * @throws Exception the exception
     */
    private void verifyClosedResources() throws JMSException, Exception {
        InOrder inOrder = Mockito.inOrder(listenersManager, session, connection, broker);

        inOrder.verify(listenersManager)
               .stopListeners();

        inOrder.verify(session)
               .close();

        inOrder.verify(connection)
               .close();

        inOrder.verify(broker)
               .stop();
    }

    /**
     * Test on not applicable event.
     */
    @Test
    void testOnNotApplicableEvent() {
        listener.onApplicationEvent(startedEvent);

        verifyNoInteractions(listenersManager, session, connection, broker);
    }

    /**
     * Test stop listeners doesnt terminate the close.
     *
     * @throws Exception the exception
     */
    @Test
    void testStopListenersDoesntTerminateTheClose() throws Exception {
        doThrow(RuntimeException.class).when(listenersManager)
                                       .stopListeners();

        listener.onApplicationEvent(closedEvent);

        verifyClosedResources();
    }

    /**
     * Test close session doesnt terminate the close.
     *
     * @throws Exception the exception
     */
    @Test
    void testCloseSessionDoesntTerminateTheClose() throws Exception {
        doThrow(JMSException.class).when(session)
                                   .close();

        listener.onApplicationEvent(closedEvent);

        verifyClosedResources();
    }

    /**
     * Test close connection doesnt terminate the close.
     *
     * @throws Exception the exception
     */
    @Test
    void testCloseConnectionDoesntTerminateTheClose() throws Exception {
        doThrow(JMSException.class).when(connection)
                                   .close();

        listener.onApplicationEvent(closedEvent);

        verifyClosedResources();
    }

    /**
     * Test stop broker doesnt terminate the close.
     *
     * @throws Exception the exception
     */
    @Test
    void testStopBrokerDoesntTerminateTheClose() throws Exception {
        doThrow(Exception.class).when(broker)
                                .stop();

        listener.onApplicationEvent(closedEvent);

        verifyClosedResources();
    }

}
