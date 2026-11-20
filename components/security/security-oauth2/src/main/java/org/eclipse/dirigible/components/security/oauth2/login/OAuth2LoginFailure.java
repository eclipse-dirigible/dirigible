/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth2.login;

import java.io.Serializable;
import java.util.Optional;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * A sign-in the identity provider refused, as kept in the session of the person who attempted it
 * until the login page reads it once.
 *
 * <p>
 * The session, not the redirect's query, carries it: the text is the provider's, and a link that
 * carried it could be made by anyone. Only {@link OAuth2LoginFailureHandler} stores one, and it
 * runs only for a callback that matched an authorization request of this very session.
 *
 * @param error the provider's error code
 * @param description the sentence to show the person, {@code null} when there is none
 */
record OAuth2LoginFailure(String error, String description) implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The session attribute the failure is kept under. */
    static final String SESSION_ATTRIBUTE = OAuth2LoginFailure.class.getName();

    /** The longest error or description kept - provider text has no length bound of its own. */
    static final int MAX_LENGTH = 300;

    OAuth2LoginFailure {
        error = cut(error);
        description = cut(description);
    }

    /**
     * Keeps the failure for the next read of the login page.
     *
     * @param request the callback request
     */
    void store(HttpServletRequest request) {
        request.getSession()
               .setAttribute(SESSION_ATTRIBUTE, this);
    }

    /**
     * Takes the failure kept in the session, if any - a second call answers empty.
     *
     * @param request the request of the login page
     * @return the failure
     */
    static Optional<OAuth2LoginFailure> take(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }
        Object failure = session.getAttribute(SESSION_ATTRIBUTE);
        session.removeAttribute(SESSION_ATTRIBUTE);
        return failure instanceof OAuth2LoginFailure loginFailure ? Optional.of(loginFailure) : Optional.empty();
    }

    private static String cut(String text) {
        if (text == null || text.length() <= MAX_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_LENGTH - 1)
                   .stripTrailing()
                + "…";
    }
}
