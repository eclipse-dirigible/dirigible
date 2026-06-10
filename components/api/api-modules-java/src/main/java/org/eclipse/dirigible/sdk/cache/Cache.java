/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.cache;

import org.eclipse.dirigible.components.api.cache.CacheFacade;

/**
 * Process-wide key/value cache shared across all client code in the tenant. Backed by the
 * platform's in-memory cache manager — values survive the request that wrote them but not a JVM
 * restart, so treat the cache as derived state, not a system of record.
 * <p>
 * Useful for memoising expensive lookups, holding short-lived per-user state across requests, and
 * coordinating between a Java controller and a TS / JS handler that shares the same key. Values are
 * stored as plain {@link Object}s; the caller is responsible for cast safety.
 *
 * <pre>
 * String key = "user:" + userId + ":permissions";
 * if (!Cache.contains(key)) {
 *     Cache.set(key, loadPermissions(userId));
 * }
 * Set&lt;String&gt; perms = (Set&lt;String&gt;) Cache.get(key);
 * </pre>
 */
public final class Cache {

    private Cache() {}

    public static boolean contains(String key) {
        return CacheFacade.contains(key);
    }

    public static Object get(String key) {
        return CacheFacade.get(key);
    }

    public static void set(String key, Object content) {
        CacheFacade.set(key, content);
    }

    public static void delete(String key) {
        CacheFacade.delete(key);
    }

    public static void clear() {
        CacheFacade.clear();
    }
}
