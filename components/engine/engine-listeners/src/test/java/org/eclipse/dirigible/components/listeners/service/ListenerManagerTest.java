/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.listeners.service;

import jakarta.jms.MessageConsumer;
import jakarta.jms.*;
import org.eclipse.dirigible.components.listeners.config.ActiveMQConnectionArtifactsFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.IllegalStateException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * The Class BackgroundListenerManagerTest.
 */
@SuppressWarnings("resource")
@ExtendWith(MockitoExtension.class)
class ListenerManagerTest {

    /** The Constant TOPIC. */
    private static final String TOPIC = "test-topic";

    /** The Constant QUEUE. */
    private static final String QUEUE = "test-queue";

    /** The manager. */
    @InjectMocks
    private ListenerManager manager;

    /** The listener. */
    @Mock
    private ListenerDescriptor listenerDescriptor;

    /** The connection artifacts factory. */
    @Mock
    private ActiveMQConnectionArtifactsFactory connectionArtifactsFactory;

    /** The asynchronous message listener factory. */
    @Mock
    private AsynchronousMessageListenerFactory asynchronousMessageListenerFactory;

    /** The connection. */
    @Mock
    private Connection connection;

    /** The session. */
    @Mock
    private Session session;

    /** The topic. */
    @Mock
    private Topic topic;

    /** The queue. */
    @Mock
    private Queue queue;

    /** The consumer. */
    @Mock
    private MessageConsumer consumer;

    /** The exception listener captor. */
    @Captor
    private ArgumentCaptor<ExceptionListener> exceptionListenerCaptor;

    /** The message listener captor. */
    @Captor
    private ArgumentCaptor<AsynchronousMessageListener> messageListenerCaptor;

    /** The asynchronous message listener. */
    @Mock
    private AsynchronousMessageListener asynchronousMessageListener;

    /**
     * Sets the up.
     */
    @BeforeEach
    void setUp() {
        lenient().when(asynchronousMessageListenerFactory.create(listenerDescriptor))
                 .thenReturn(asynchronousMessageListener);
    }

    /**
     * Test start listener on start error.
     *
     * @throws JMSException the JMS exception
     */
    @Test
    void testStartListenerOnStartError() throws JMSException {
        when(connectionArtifactsFactory.createConnection(any(ExceptionListener.class))).thenReturn(connection);
        when(connectionArtifactsFactory.createSession(connection)).thenThrow(JMSException.class);

        assertThrows(IllegalStateException.class, () -> manager.startListener());
    }

    /**
     * Test start listener for unsupported listener kind.
     */
    @Test
    void testStartListenerForUnsupportedListenerType() {
        when(listenerDescriptor.getType()).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> manager.startListener());
    }

    /**
     * Test start listener for queue.
     *
     * @throws JMSException the JMS exception
     */
    @Test
    void testStartListenerForQueue() throws JMSException {
        mockConnectionAndSession();
        when(listenerDescriptor.getType()).thenReturn(ListenerType.QUEUE);
        when(listenerDescriptor.getDestination()).thenReturn(QUEUE);

        when(session.createQueue(QUEUE)).thenReturn(queue);
        when(session.createConsumer(queue)).thenReturn(consumer);

        manager.startListener();

        manager.startListener();

        // verify second call to start doesn't create new artifacts
        verifyConfiguredExceptionListener();
        verifyConfiguredMessageListener();
        verify(session).createQueue(QUEUE);
        verify(session).createConsumer(queue);
        verify(connectionArtifactsFactory).createConnection(any(ExceptionListener.class));
        verify(connectionArtifactsFactory).createSession(connection);
    }

    /**
     * Mock connection and session.
     *
     * @throws JMSException the JMS exception
     */
    private void mockConnectionAndSession() throws JMSException {
        when(connectionArtifactsFactory.createConnection(any(ExceptionListener.class))).thenReturn(connection);
        when(connectionArtifactsFactory.createSession(connection)).thenReturn(session);
    }

    /**
     * Verify configured exception listener.
     */
    private void verifyConfiguredExceptionListener() {
        verify(connectionArtifactsFactory).createConnection(exceptionListenerCaptor.capture());
        ExceptionListener actualExceptionListener = exceptionListenerCaptor.getValue();
        assertThat(actualExceptionListener).isInstanceOf(ListenerExceptionHandler.class);
    }

    /**
     * Verify configured message listener.
     *
     * @throws JMSException the JMS exception
     */
    private void verifyConfiguredMessageListener() throws JMSException {
        verify(consumer).setMessageListener(messageListenerCaptor.capture());
        assertThat(messageListenerCaptor.getValue()).isInstanceOf(AsynchronousMessageListener.class);
    }

    /**
     * Test stop.
     *
     * @throws JMSException the JMS exception
     */
    @Test
    void testStop() throws JMSException {
        testStartListenerForTopic();

        manager.stopListener();

        manager.stopListener();

        InOrder inOrder = Mockito.inOrder(consumer, session, connection);

        // verify second call to stop doesn't close the artifacts again
        inOrder.verify(consumer)
               .close();

        inOrder.verify(session)
               .close();

        inOrder.verify(connection)
               .close();
    }

    /**
     * Test start listener for topic.
     *
     * @throws JMSException the JMS exception
     */
    @Test
    void testStartListenerForTopic() throws JMSException {
        mockConnectionAndSession();
        when(listenerDescriptor.getType()).thenReturn(ListenerType.TOPIC);
        when(listenerDescriptor.getDestination()).thenReturn(TOPIC);

        when(session.createTopic(TOPIC)).thenReturn(topic);
        when(session.createConsumer(topic)).thenReturn(consumer);

        manager.startListener();

        verifyConfiguredExceptionListener();
        verifyConfiguredMessageListener();
    }

}
