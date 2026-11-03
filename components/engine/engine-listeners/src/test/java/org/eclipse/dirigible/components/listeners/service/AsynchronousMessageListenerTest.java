/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.listeners.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.components.base.context.OriginatingUser;
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

    /** The executor of the listener's script handler. */
    @Mock
    private ListenerScriptExecutor scriptExecutor;

    /** The text message. */
    @Mock
    private TextMessage textMessage;

    /** The listener. */
    @Mock
    private ListenerDescriptor listenerDescriptor;

    /** The bytes message. */
    @Mock
    private BytesMessage bytesMessage;

    /** The tenant property manager. */
    @Mock
    private TenantPropertyManager tenantPropertyManager;

    /**
     * Sets the up.
     */
    @BeforeEach
    void setUp() {
        asyncMessageListener =
                new AsynchronousMessageListener(listenerDescriptor, tenantPropertyManager, new TestTenantContext(), scriptExecutor);
    }

    /**
     * Test on message.
     *
     * @throws JMSException the JMS exception
     */
    @Test
    void testOnMessage() throws JMSException {
        when(tenantPropertyManager.getCurrentTenantId(textMessage)).thenReturn(TENANT_ID);
        when(listenerDescriptor.getHandlerPath()).thenReturn(HANDLER);
        when(textMessage.getText()).thenReturn(MESSAGE);

        asyncMessageListener.onMessage(textMessage);

        verify(scriptExecutor).onMessage(HANDLER, MESSAGE);
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


    /**
     * The handler runs with the user the producer stamped bound, and the pooled broker thread does not
     * keep it afterwards (#8228).
     *
     * @throws JMSException the JMS exception
     */
    @Test
    void theHandlerRunsWithTheOriginatingUser() throws JMSException {
        when(tenantPropertyManager.getCurrentTenantId(textMessage)).thenReturn(TENANT_ID);
        when(textMessage.getObjectProperty(OriginatingUserProperty.PROPERTY_NAME)).thenReturn("alice");
        when(listenerDescriptor.getHandlerPath()).thenReturn(HANDLER);
        when(textMessage.getText()).thenReturn(MESSAGE);
        AtomicReference<String> seen = new AtomicReference<>();
        doAnswer(invocation -> {
            seen.set(OriginatingUser.get()
                                    .orElse(null));
            return null;
        }).when(scriptExecutor)
          .onMessage(any(), any());

        asyncMessageListener.onMessage(textMessage);

        assertEquals("alice", seen.get());
        assertTrue(OriginatingUser.get()
                                  .isEmpty());
    }

}
