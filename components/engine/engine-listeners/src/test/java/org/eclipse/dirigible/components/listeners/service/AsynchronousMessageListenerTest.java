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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.dirigible.graalium.core.DirigibleJavascriptCodeRunner;
import org.eclipse.dirigible.graalium.core.javascript.modules.Module;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.jms.BytesMessage;
import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;

/**
 * The Class BackgroundMessageListenerTest.
 */
@SuppressWarnings("resource")
@ExtendWith(MockitoExtension.class)
class AsynchronousMessageListenerTest {

    /** The Constant MESSAGE. */
    private static final String MESSAGE = "This is a test message";

    /** The Constant HANDLER. */
    private static final String HANDLER = "test-handler";

    /** The Constant TENANT_ID. */
    private static final String TENANT_ID = "1e7252b1-3bca-4285-bd4e-60e19886d063";

    /** The background message listener. */
    private AsynchronousMessageListener asyncMessageListener;

    /** The js code runner. */
    @Mock
    private DirigibleJavascriptCodeRunner jsCodeRunner;

    /** The text message. */
    @Mock
    private TextMessage textMessage;

    /** The listener. */
    @Mock
    private ListenerDescriptor listenerDescriptor;

    /** The bytes message. */
    @Mock
    private BytesMessage bytesMessage;

    /** The module. */
    @Mock
    private Module module;

    /** The tenant property manager. */
    @Mock
    private TenantPropertyManager tenantPropertyManager;

    /**
     * Sets the up.
     */
    @BeforeEach
    void setUp() {
        asyncMessageListener = spy(new AsynchronousMessageListener(listenerDescriptor, tenantPropertyManager, new TestTenantContext()));
    }

    /**
     * Test on message.
     *
     * @throws JMSException the JMS exception
     */
    @Test
    void testOnMessage() throws JMSException {
        when(tenantPropertyManager.getCurrentTenantId(textMessage)).thenReturn(TENANT_ID);
        doReturn(jsCodeRunner).when(asyncMessageListener)
                              .createJSCodeRunner();
        when(listenerDescriptor.getHandlerPath()).thenReturn(HANDLER);
        when(textMessage.getText()).thenReturn(MESSAGE);
        when(jsCodeRunner.run(HANDLER)).thenReturn(module);

        asyncMessageListener.onMessage(textMessage);

        verify(jsCodeRunner).runMethod(module, "onMessage", MESSAGE);
    }

    /**
     * Test on message failed to extract message.
     *
     * @throws JMSException the JMS exception
     */
    @Test
    void testOnMessageFailedToExtractMessage() throws JMSException {
        when(textMessage.getText()).thenThrow(JMSException.class);

        assertThrows(IllegalStateException.class, () -> asyncMessageListener.onMessage(textMessage));
    }

    /**
     * Test on message with unsupported message.
     */
    @Test
    void testOnMessageWithUnsupportedMessage() {
        assertThrows(IllegalStateException.class, () -> asyncMessageListener.onMessage(bytesMessage));
    }

}
