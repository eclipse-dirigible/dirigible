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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import jakarta.jms.JMSException;
import org.eclipse.dirigible.components.listeners.util.LogsAsserter;
import org.eclipse.dirigible.graalium.core.DirigibleJavascriptCodeRunner;
import org.eclipse.dirigible.graalium.core.javascript.modules.Module;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import ch.qos.logback.classic.Level;

/**
 * The Class ListenerExceptionHandlerTest.
 */
@SuppressWarnings("resource")
@ExtendWith(MockitoExtension.class)
class ListenerExceptionHandlerTest {

    /** The Constant ERROR_MESSAGE. */
    private static final String ERROR_MESSAGE = "Opsss";

    /** The Constant HANDLER_PATH. */
    private static final String HANDLER_PATH = "handler/path.js";

    /** The handler. */
    private ListenerExceptionHandler handler;

    /** The jms exception. */
    private JMSException jmsException;

    /** The logs asserter. */
    private LogsAsserter logsAsserter;

    /** The js code runner. */
    @Mock
    private DirigibleJavascriptCodeRunner jsCodeRunner;

    /** The module. */
    @Mock
    private Module module;

    /**
     * Sets the up.
     */
    @BeforeEach
    void setUp() {
        jmsException = new JMSException(ERROR_MESSAGE);
        handler = Mockito.spy(new ListenerExceptionHandler(HANDLER_PATH));
        doReturn(jsCodeRunner).when(handler)
                              .createJSCodeRunner();
        logsAsserter = new LogsAsserter(ListenerExceptionHandler.class, Level.ERROR);
    }

    /**
     * Test on exception.
     */
    @Test
    void testOnException() {
        when(jsCodeRunner.run(HANDLER_PATH)).thenReturn(module);


        handler.onException(jmsException);

        verify(jsCodeRunner).runMethod(module, "onError", ERROR_MESSAGE);
    }

    /**
     * Test on exception on fail to run method.
     */
    @Test
    void testOnExceptionOnFailToRunMethod() {
        when(jsCodeRunner.run(HANDLER_PATH)).thenThrow(IllegalStateException.class);

        handler.onException(jmsException);

        logsAsserter.assertLoggedMessage("Failed to handle exception properly", Level.ERROR);
    }

}
