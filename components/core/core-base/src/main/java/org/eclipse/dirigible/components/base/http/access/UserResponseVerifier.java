/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.http.access;

import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.dirigible.components.base.context.ContextException;
import org.eclipse.dirigible.components.base.context.ThreadContextFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class UserResponseVerifier.
 */
public class UserResponseVerifier {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(UserResponseVerifier.class);

    /**
     * Returns the HttpServletResponse associated with the current thread context.
     *
     * @return the response
     */
    public static final HttpServletResponse getResponse() {
        if (!ThreadContextFacade.isValid()) {
            return null;
        }
        try {
            return (HttpServletResponse) ThreadContextFacade.get(HttpServletResponse.class.getCanonicalName());
        } catch (ContextException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * Checks if there is a HttpServletResponse associated with the current thread context.
     *
     * @return true, if there is a HttpServletResponse associated with the current thread context
     */
    public static final boolean isValid() {
        HttpServletResponse response = getResponse();
        return response != null;
    }

}
