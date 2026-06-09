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

import org.eclipse.dirigible.components.api.core.GlobalsFacade;

/**
 * Application-wide global variables — values survive across requests and across all tenants in the
 * JVM (so be careful in multi-tenant deployments). Strictly typed to {@link String} (use JSON
 * encoding for richer payloads); for richer types or per-tenant scoping prefer the
 * {@link org.eclipse.dirigible.sdk.cache.Cache Cache} or a database table.
 * <p>
 * Globals are commonly set once at startup (a {@code @Scheduled} job, a controller endpoint
 * triggered on deploy) and read frequently from the rest of the application.
 */
public final class Globals {

    private Globals() {}

    public static String get(String name) {
        return GlobalsFacade.get(name);
    }

    public static void set(String name, String value) {
        GlobalsFacade.set(name, value);
    }

    public static String list() {
        return GlobalsFacade.list();
    }
}
