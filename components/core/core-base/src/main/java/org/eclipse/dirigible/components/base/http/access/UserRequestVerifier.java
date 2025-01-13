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

import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.context.ContextException;
import org.eclipse.dirigible.components.base.context.InvalidStateException;
import org.eclipse.dirigible.components.base.context.ThreadContextFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

/**
 * The Class UserRequestVerifier.
 */
public class UserRequestVerifier {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(UserRequestVerifier.class);

    /** The Constant ACCESS_MANAGERS. */
    private static final ServiceLoader<UserAccessVerifier> ACCESS_VERIFIERS = ServiceLoader.load(UserAccessVerifier.class);

    /** The Constant NO_VALID_REQUEST. */
    private static final String NO_VALID_REQUEST = "Trying to use HTTP Request Facade without a valid Request";

    /**
     * Checks if there is a request in the current thread context.
     *
     * @return true, if there is a request in the current thread context
     */
    public static final boolean isValid() {
        HttpServletRequest request = getRequest();
        return request != null;
    }

    /**
     * Returns the request in the current thread context.
     *
     * @return the request
     */
    public static final HttpServletRequest getRequest() {
        if (!ThreadContextFacade.isValid()) {
            return null;
        }
        try {
            return (HttpServletRequest) ThreadContextFacade.get(HttpServletRequest.class.getCanonicalName());
        } catch (ContextException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * Checks if is user in role.
     *
     * @param role the role
     * @return true, if is user in role
     */
    public static final boolean isUserInRole(String role) {
        HttpServletRequest request = getRequest();
        return isUserInRole(request, role);
    }

    public static boolean isUserInRole(HttpServletRequest request, String role) {
        if (request == null) {
            throw new InvalidStateException(NO_VALID_REQUEST);
        }
        if (Configuration.isJwtModeEnabled()) {
            for (UserAccessVerifier next : ACCESS_VERIFIERS) {
                if (next.isInRole(request, role)) {
                    return true;
                }
            }
            return false;
        }
        return request.isUserInRole(role);
    }
}
