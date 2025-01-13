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

import jakarta.jms.ExceptionListener;
import jakarta.jms.JMSException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The listener interface for receiving loggingException events. The class that is interested in
 * processing a loggingException event implements this interface, and the object created with that
 * class is registered with a component using the component's object's appropriate method is
 * invoked.
 *
 */
@Component
class LoggingExceptionListener implements ExceptionListener {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingExceptionListener.class);

    /**
     * On exception.
     *
     * @param ex the ex
     */
    @Override
    public synchronized void onException(JMSException ex) {
        LOGGER.error("Exception occur", ex);
    }

}
