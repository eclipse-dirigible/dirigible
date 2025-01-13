/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.http;

import java.util.Collections;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.base.context.ThreadContextFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Java facade for working with HttpSession.
 */
@Component
public class HttpSessionFacade {

    /** The Constant NO_VALID_REQUEST. */
    private static final String NO_VALID_REQUEST = "Trying to use HTTP Session Facade without a valid Session (HTTP Request/Response)";

    /** The Constant INVOCATION_COUNT. */
    private static final String INVOCATION_COUNT = "invocation.count";

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(HttpSessionFacade.class);

    /**
     * Returns the session associated with the current thread context.
     *
     * @return the session
     */
    static final HttpSession getSession() {
        if (!ThreadContextFacade.isValid()) {
            return null;
        }
        try {
            HttpServletRequest request = (HttpServletRequest) ThreadContextFacade.get(HttpServletRequest.class.getCanonicalName());
            if (request != null) {
                HttpSession httpSession = request.getSession(true);
                Integer count = (Integer) httpSession.getAttribute(INVOCATION_COUNT);
                count = count == null ? 1 : ++count;
                httpSession.setAttribute(INVOCATION_COUNT, count);
                return httpSession;
            }
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * Checks if there is a session associated with the current thread context.
     *
     * @return true, if is valid
     */
    public static final boolean isValid() {
        HttpSession session = getSession();
        return session != null;
    }

    /**
     * Returns the object bound to the specified name.
     *
     * @param arg0 the name
     * @return the attribute
     */
    public static final String getAttribute(String arg0) {
        HttpSession session = getSession();
        if (session != null) {
            return session.getAttribute(arg0) != null ? session.getAttribute(arg0)
                                                               .toString()
                    : null;
        }
        return null;
    }

    /**
     * Returns the attribute names.
     *
     * @return the attribute names
     */
    public static final String[] getAttributeNames() {
        HttpSession session = getSession();
        if (session != null) {
            return Collections.list(session.getAttributeNames())
                              .toArray(new String[] {});
        }
        return null;
    }

    /**
     * Returns the attribute names.
     *
     * @return the attribute names
     */
    public static final String getAttributeNamesJson() {
        HttpSession session = getSession();
        if (session != null) {
            String[] array = Collections.list(session.getAttributeNames())
                                        .toArray(new String[] {});
            return GsonHelper.toJson(array);
        }
        return null;
    }

    /**
     * Returns the time the session was created.
     *
     * @return the creation time
     */
    public static final long getCreationTime() {
        HttpSession session = getSession();
        if (session != null) {
            return session.getCreationTime();
        }
        return 0;
    }

    /**
     * Returns the id of the session.
     *
     * @return the id
     */
    public static final String getId() {
        HttpSession session = getSession();
        if (session != null) {
            return session.getId();
        }
        return null;
    }

    /**
     * Returns the last accessed time.
     *
     * @return the last accessed time
     */
    public static final long getLastAccessedTime() {
        HttpSession session = getSession();
        if (session != null) {
            return session.getLastAccessedTime();
        }
        return 0;
    }

    /**
     * Returns the max inactive interval.
     *
     * @return the max inactive interval
     */
    public static final int getMaxInactiveInterval() {
        HttpSession session = getSession();
        if (session != null) {
            return session.getMaxInactiveInterval();
        }
        return 0;
    }

    /**
     * Invalidates the session.
     */
    public static final void invalidate() {
        HttpSession session = getSession();
        if (session != null) {
            session.invalidate();
        }
    }

    /**
     * Checks if the session is new.
     *
     * @return true, if is new
     */
    public static final boolean isNew() {
        HttpSession session = getSession();
        if (session != null) {
            return session.isNew();
        }
        return false;
    }

    /**
     * Sets the attribute.
     *
     * @param arg0 the name
     * @param arg1 the value
     */
    public static final void setAttribute(String arg0, String arg1) {
        HttpSession session = getSession();
        if (session != null) {
            session.setAttribute(arg0, arg1);
        }
    }

    /**
     * Removes the attribute.
     *
     * @param arg0 the name of the attribute
     */
    public static final void removeAttribute(String arg0) {
        HttpSession session = getSession();
        if (session != null) {
            session.removeAttribute(arg0);
        }
    }

    /**
     * Sets the max inactive interval.
     *
     * @param arg0 the new max inactive interval
     */
    public static final void setMaxInactiveInterval(int arg0) {
        HttpSession session = getSession();
        if (session != null) {
            session.setMaxInactiveInterval(arg0);
        }
    }

    /**
     * Sets the max inactive interval.
     *
     * @param arg0 the new max inactive interval
     */
    public static final void setMaxInactiveInterval(Double arg0) {
        setMaxInactiveInterval(arg0.intValue());
    }

}
