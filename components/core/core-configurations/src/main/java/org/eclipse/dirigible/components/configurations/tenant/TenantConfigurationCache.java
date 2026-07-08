/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.configurations.tenant;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * In-memory cache of the raw per-tenant configuration entries, keyed by tenant id. Entries are
 * loaded on first access and kept until explicitly invalidated on a write. This avoids a database
 * round-trip on every request while still reflecting configuration changes made through this
 * instance immediately.
 * <p>
 * Note: in a multi-node deployment a write on one node does not invalidate the caches on the
 * others; such setups need an external invalidation signal, which is out of scope here.
 */
@Component
class TenantConfigurationCache {

    /** Loads the raw configuration entries for the current tenant. */
    @FunctionalInterface
    interface Loader {

        /**
         * Loads the entries.
         *
         * @return the entries
         * @throws SQLException if loading fails
         */
        Map<String, String> load() throws SQLException;
    }

    private final Map<String, Map<String, String>> cache = new ConcurrentHashMap<>();

    /**
     * Returns the cached entries for the tenant, loading and caching them on a miss.
     *
     * @param tenantId the tenant id
     * @param loader the loader invoked on a cache miss
     * @return the entries, never {@code null}
     * @throws SQLException if loading fails
     */
    Map<String, String> get(String tenantId, Loader loader) throws SQLException {
        Map<String, String> cached = cache.get(tenantId);
        if (cached != null) {
            return cached;
        }
        Map<String, String> loaded = Map.copyOf(loader.load());
        cache.put(tenantId, loaded);
        return loaded;
    }

    /**
     * Invalidates the cached entries of a single tenant.
     *
     * @param tenantId the tenant id
     */
    void invalidate(String tenantId) {
        cache.remove(tenantId);
    }

    /**
     * Invalidates the cached entries of all tenants.
     */
    void invalidateAll() {
        cache.clear();
    }

}
