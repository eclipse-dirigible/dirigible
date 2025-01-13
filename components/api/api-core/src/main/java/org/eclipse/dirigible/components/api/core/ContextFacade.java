/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.core;

import org.eclipse.dirigible.components.base.context.ContextException;
import org.eclipse.dirigible.components.base.context.ThreadContextFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The ContextFacade is used to store name value pairs to the execution (Thread) context.
 */
@Component
public class ContextFacade {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(ContextFacade.class);

    /**
     * Gets the value per name.
     *
     * @param name the name
     * @return the string
     */
    public static final Object get(String name) {
        if (logger.isTraceEnabled()) {
            logger.trace("API - ContextFacade.get() -> begin");
        }
        Object contextValue;
        try {
            contextValue = ThreadContextFacade.get(name);
        } catch (ContextException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
            throw new IllegalStateException(e);
        }
        // String value = contextValue != null ? contextValue.toString() : null;
        if (logger.isTraceEnabled()) {
            logger.trace("API - ContextFacade.get() -> end");
        }
        return contextValue;
    }

    /**
     * Sets the value for name.
     *
     * @param name the name
     * @param value the value
     */
    public static final void set(String name, Object value) {
        if (logger.isTraceEnabled()) {
            logger.trace("API - ContextFacade.set() -> begin");
        }
        try {
            ThreadContextFacade.set(name, value);
        } catch (ContextException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
            throw new IllegalStateException(e);
        }
        if (logger.isTraceEnabled()) {
            logger.trace("API - ContextFacade.set() -> end");
        }
    }

}
