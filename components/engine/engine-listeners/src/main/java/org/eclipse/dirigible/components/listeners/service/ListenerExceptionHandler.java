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

import jakarta.jms.ExceptionListener;
import jakarta.jms.JMSException;
import org.eclipse.dirigible.graalium.core.DirigibleJavascriptCodeRunner;
import org.eclipse.dirigible.graalium.core.javascript.modules.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class ListenerExceptionHandler.
 */
class ListenerExceptionHandler implements ExceptionListener {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ListenerExceptionHandler.class);

    /** The handler path. */
    private final String handlerPath;

    /**
     * Instantiates a new listener exception handler.
     *
     * @param handlerPath the handler path
     */
    ListenerExceptionHandler(String handlerPath) {
        this.handlerPath = handlerPath;
    }

    /**
     * On exception.
     *
     * @param jmsException the jms exception
     */
    @Override
    public void onException(JMSException jmsException) {
        try {
            String errorMessage = jmsException.getMessage();
            executeOnErrorHandler(errorMessage);
        } catch (RuntimeException ex) {
            ex.addSuppressed(jmsException);
            LOGGER.error("Failed to handle exception properly", ex);
        }
    }

    /**
     * Execute on error handler.
     *
     * @param errorMessage the error message
     */
    private void executeOnErrorHandler(String errorMessage) {
        try (DirigibleJavascriptCodeRunner runner = createJSCodeRunner()) {
            Module module = runner.run(handlerPath);
            runner.runMethod(module, "onError", errorMessage);
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
