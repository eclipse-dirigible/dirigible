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

import jakarta.jms.JMSException;
import org.eclipse.dirigible.components.listeners.util.LogsAsserter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import ch.qos.logback.classic.Level;

/**
 * The Class LoggingExceptionListenerTest.
 */
@ExtendWith(MockitoExtension.class)
class LoggingExceptionListenerTest {

    /** The listener. */
    @InjectMocks
    private LoggingExceptionListener listener;

    /** The logs asserter. */
    private LogsAsserter logsAsserter;

    /**
     * Sets the up.
     */
    @BeforeEach
    void setUp() {
        this.logsAsserter = new LogsAsserter(LoggingExceptionListener.class, Level.ERROR);
    }

    /**
     * Test on exception.
     */
    @Test
    void testOnException() {
        JMSException jmsException = new JMSException("Opsss");

        listener.onException(jmsException);

        logsAsserter.assertLoggedMessage("Exception occur", Level.ERROR);
    }

}
