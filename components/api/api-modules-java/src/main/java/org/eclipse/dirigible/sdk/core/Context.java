/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.core;

import org.eclipse.dirigible.components.api.core.ContextFacade;

/**
 * Per-request scratch storage bound to the calling thread. Useful for passing values from a filter
 * / interceptor down to a controller method without threading the data through every method
 * signature — for example user-derived language preferences, correlation ids, request- scoped
 * feature flags.
 * <p>
 * Values are <em>not</em> persisted across requests. For longer-lived global state use
 * {@link Globals}; for the actual HTTP session-scoped storage use
 * {@link org.eclipse.dirigible.sdk.http.Session}.
 */
public final class Context {

    private Context() {}

    public static Object get(String name) {
        return ContextFacade.get(name);
    }

    public static void set(String name, Object value) {
        ContextFacade.set(name, value);
    }
}
