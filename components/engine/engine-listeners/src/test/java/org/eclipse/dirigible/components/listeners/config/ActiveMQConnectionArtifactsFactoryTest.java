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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import jakarta.jms.Connection;
import jakarta.jms.ExceptionListener;
import jakarta.jms.JMSException;
import jakarta.jms.Session;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * The Class ActiveMQConnectionArtifactsFactoryTest.
 */
@SuppressWarnings("resource")
@ExtendWith(MockitoExtension.class)
class ActiveMQConnectionArtifactsFactoryTest {

    /** The connection artifacts factory. */
    @InjectMocks
    private ActiveMQConnectionArtifactsFactory connectionArtifactsFactory;

    /** The connection factory. */
    @Mock
    private ActiveMQConnectionFactory connectionFactory;

    /** The exception listener. */
    @Mock
    private ExceptionListener exceptionListener;

    /** The connection. */
    @Mock
    private Connection connection;

    /** The session. */
    @Mock
    private Session session;

    /**
	 * Test create connection.
	 *
	 * @throws JMSException the JMS exception
	 */
	@Test
	void testCreateConnection() throws JMSException {
		when(connectionFactory.createConnection()).thenReturn(connection);

		Connection actualConnection = connectionArtifactsFactory.createConnection(exceptionListener);

		assertThat(actualConnection).isEqualTo(connection);
		verify(connection).setExceptionListener(exceptionListener);
		verify(connection).start();
	}

    /**
     * Test create connection on JMS exception.
     *
     * @throws JMSException the JMS exception
     */
    @Test
    void testCreateConnectionOnJMSException() throws JMSException {
    	when(connectionFactory.createConnection()).thenThrow(JMSException.class);

        assertThrows(IllegalStateException.class, ()->connectionArtifactsFactory.createConnection(exceptionListener));
    }

    /**
	 * Test create session.
	 *
	 * @throws JMSException the JMS exception
	 */
	@Test
	void testCreateSession() throws JMSException {
		when(connection.createSession(false, Session.AUTO_ACKNOWLEDGE)).thenReturn(session);

		Session actualSession = connectionArtifactsFactory.createSession(connection);

		assertThat(actualSession).isEqualTo(session);
	}

}
