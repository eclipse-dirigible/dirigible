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

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TextMessage;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.graalium.core.DirigibleJavascriptCodeRunner;
import org.eclipse.dirigible.graalium.core.javascript.modules.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The listener interface for receiving asynchronousMessage events. The class that is interested in
 * processing a asynchronousMessage event implements this interface, and the object created with
 * that class is registered with a component using the component's addAsynchronousMessageListener
 * method. When the asynchronousMessage event occurs, that object's appropriate method is invoked.
 *
 */
class AsynchronousMessageListener implements MessageListener {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AsynchronousMessageListener.class);

    /** The listener. */
    private final ListenerDescriptor listenerDescriptor;

    /** The tenant property manager. */
    private final TenantPropertyManager tenantPropertyManager;

    /** The tenant context. */
    private final TenantContext tenantContext;

    /**
     * Instantiates a new asynchronous message listener.
     *
     * @param listenerDescriptor the listener
     * @param tenantPropertyManager the tenant property manager
     * @param tenantContext the tenant context
     */
    AsynchronousMessageListener(ListenerDescriptor listenerDescriptor, TenantPropertyManager tenantPropertyManager,
            TenantContext tenantContext) {
        this.listenerDescriptor = listenerDescriptor;
        this.tenantPropertyManager = tenantPropertyManager;
        this.tenantContext = tenantContext;
    }

    /**
     * On message.
     *
     * @param message the message
     */
    @Override
    public void onMessage(Message message) {
        LOGGER.trace("Start processing a received message in [{}] by [{}] ...", listenerDescriptor.getDestination(),
                listenerDescriptor.getHandlerPath());
        if (!(message instanceof TextMessage textMsg)) {
            String msg = String.format("Invalid message [%s] has been received in destination [%s]", message,
                    listenerDescriptor.getDestination());
            throw new IllegalStateException(msg);
        }
        try {
            String tenantId = tenantPropertyManager.getCurrentTenantId(message);
            LOGGER.debug("Processing message WITH context for tenant [{}].", tenantId);

            tenantContext.execute(tenantId, () -> {
                executeOnMessageHandler(textMsg);
                return null;
            });
            LOGGER.trace("Done processing the received message in [{}] by [{}]", listenerDescriptor.getDestination(),
                    listenerDescriptor.getHandlerPath());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to handle message: " + message, e);
        }
    }

    /**
     * Execute on message handler.
     *
     * @param textMsg the text msg
     */
    private void executeOnMessageHandler(TextMessage textMsg) {
        String extractedMsg = extractMessage(textMsg);
        try (DirigibleJavascriptCodeRunner runner = createJSCodeRunner()) {
            String handlerPath = listenerDescriptor.getHandlerPath();
            Module module = runner.run(handlerPath);
            runner.runMethod(module, "onMessage", extractedMsg);
        }
    }

    /**
     * Extract message.
     *
     * @param textMsg the text msg
     * @return the string
     */
    private String extractMessage(TextMessage textMsg) {
        try {
            return textMsg.getText();
        } catch (JMSException ex) {
            throw new IllegalStateException("Failed to extract test message from " + textMsg, ex);
        }
    }

    /**
     * Creates the JS code runner.
     *
     * @return the dirigible javascript code runner
     */
    DirigibleJavascriptCodeRunner createJSCodeRunner() {
        return new DirigibleJavascriptCodeRunner();
    }

}
