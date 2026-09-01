/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.security;

import java.util.Collection;
import org.eclipse.dirigible.components.api.security.ActAsFacade;
import org.eclipse.dirigible.components.api.security.UserFacade;

/**
 * Identity and role lookup for the user behind the current request. The methods return whatever the
 * platform's authentication chain populated — Spring Security principal, OIDC subject, basic-auth
 * username, etc.
 * <p>
 * {@link #isInRole(String)} is the authoritative check used by {@code @Roles} dispatch in
 * controllers; {@link #getRoles()} returns the full set if you need to make a finer-grained
 * decision inside a method body.
 * <p>
 * In an unauthenticated context (anonymous request, scheduled job) the user name resolves to the
 * platform's anonymous user; role checks then defer to the platform's anonymous-mode configuration.
 */
public final class User {

    private User() {}

    public static String getName() {
        return UserFacade.getName();
    }

    /**
     * The identity PERSONAL surfaces resolve against: the acting identity when an entitled user armed
     * "act as" (delegated entry - a manager filling a timesheet in a worker's name), else the real
     * login. Use it ONLY where the question is "whose records/whose tasks" - roles, security checks and
     * audit stamping stay on {@link #getName()}, so a record entered on behalf of someone always shows
     * whose it is AND who really entered it.
     *
     * @return the effective username for personal-identity resolution
     */
    public static String getEffectiveName() {
        return ActAsFacade.effectiveUser();
    }

    public static boolean isInRole(String role) {
        return UserFacade.isInRole(role);
    }

    public static Collection<String> getRoles() {
        return UserFacade.getUserRoles();
    }

    public static String getAuthType() {
        return UserFacade.getAuthType();
    }

    public static String getSecurityToken() {
        return UserFacade.getSecurityToken();
    }

    public static String getInvocationCount() {
        return UserFacade.getInvocationCount();
    }

    public static String getLanguage() {
        return UserFacade.getLanguage();
    }

    /**
     * Binds a language to the current thread, preferred over the request's Accept-Language by every
     * read that resolves the language through {@link #getLanguage()} - the multilingual translation
     * overlay in particular. Intended for in-process renders that run outside an HTTP request (a
     * document snapshot, a mailed PDF attachment): the caller MUST clear it in a finally via
     * {@link #clearLanguage()}.
     *
     * @param language the language to bind (a null or blank value leaves the override unset)
     */
    public static void setLanguage(String language) {
        UserFacade.setLanguage(language);
    }

    /**
     * Clears the thread-bound language override set by {@link #setLanguage(String)}.
     */
    public static void clearLanguage() {
        UserFacade.clearLanguage();
    }

    public static Integer getTimeout() {
        return UserFacade.getTimeout();
    }
}
