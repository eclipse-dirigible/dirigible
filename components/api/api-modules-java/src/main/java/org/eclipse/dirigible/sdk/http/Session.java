/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.http;

import org.eclipse.dirigible.components.api.http.HttpSessionFacade;

/**
 * Reads and writes the HTTP session attached to the current request — attributes, lifetime,
 * invalidation. Sessions persist across requests for the lifetime configured by the platform
 * (typically 30 minutes idle, configurable via {@link #setMaxInactiveInterval(int)}).
 * <p>
 * Use sessions for short-lived per-user state that does not need to survive a server restart (form
 * wizards, in-progress UI selections). For longer-lived state prefer a database table; for
 * cross-user state prefer {@link org.eclipse.dirigible.sdk.cache.Cache Cache} or
 * {@link org.eclipse.dirigible.sdk.core.Globals Globals}.
 */
public final class Session {

    private Session() {}

    public static boolean isValid() {
        return HttpSessionFacade.isValid();
    }

    public static String getId() {
        return HttpSessionFacade.getId();
    }

    public static String getAttribute(String name) {
        return HttpSessionFacade.getAttribute(name);
    }

    public static String[] getAttributeNames() {
        return HttpSessionFacade.getAttributeNames();
    }

    public static String getAttributeNamesJson() {
        return HttpSessionFacade.getAttributeNamesJson();
    }

    public static void setAttribute(String name, String value) {
        HttpSessionFacade.setAttribute(name, value);
    }

    public static void removeAttribute(String name) {
        HttpSessionFacade.removeAttribute(name);
    }

    public static long getCreationTime() {
        return HttpSessionFacade.getCreationTime();
    }

    public static long getLastAccessedTime() {
        return HttpSessionFacade.getLastAccessedTime();
    }

    public static int getMaxInactiveInterval() {
        return HttpSessionFacade.getMaxInactiveInterval();
    }

    public static void setMaxInactiveInterval(int seconds) {
        HttpSessionFacade.setMaxInactiveInterval(seconds);
    }

    public static void invalidate() {
        HttpSessionFacade.invalidate();
    }

    public static boolean isNew() {
        return HttpSessionFacade.isNew();
    }
}
