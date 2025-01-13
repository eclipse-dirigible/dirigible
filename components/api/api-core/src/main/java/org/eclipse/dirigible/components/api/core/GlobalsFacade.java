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

import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The GlobalsFacade is used to store and retrieve values per name.
 */
@Component
public class GlobalsFacade {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(GlobalsFacade.class);

    /**
     * Gets the value per name.
     *
     * @param name the name
     * @return the string value
     */
    public static final String get(String name) {
        if (logger.isTraceEnabled()) {
            logger.trace("API - GlobalsFacade.get() -> begin");
        }
        String value = System.getProperty(name);
        if (logger.isTraceEnabled()) {
            logger.trace("API - GlobalsFacade.get() -> end");
        }
        return value;
    }

    /**
     * Sets the value per name.
     *
     * @param name the name
     * @param value the value
     */
    public static final void set(String name, String value) {
        if (logger.isTraceEnabled()) {
            logger.trace("API - GlobalsFacade.set() -> begin");
        }
        System.setProperty(name, value);
        if (logger.isTraceEnabled()) {
            logger.trace("API - GlobalsFacade.set() -> end");
        }
    }

    /**
     * List all the name value pairs.
     *
     * @return the string
     */
    public static final String list() {
        if (logger.isTraceEnabled()) {
            logger.trace("API - GlobalsFacade.get() -> begin");
        }
        String value = GsonHelper.toJson(System.getProperties());
        if (logger.isTraceEnabled()) {
            logger.trace("API - GlobalsFacade.get() -> end");
        }
        return value;
    }

}
