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
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.configurations.domain.TenantConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Facade over the per-tenant configuration store, cache and key policy. All operations act on the
 * tenant of the current execution scope; the caller must run inside a tenant scope (as every
 * request does once the tenant context filter has run).
 */
@Service
public class TenantConfigurationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantConfigurationService.class);

    private final TenantConfigurationStore store;

    private final TenantConfigurationCache cache;

    private final TenantConfigurationKeyPolicy keyPolicy;

    private final TenantContext tenantContext;

    TenantConfigurationService(TenantConfigurationStore store, TenantConfigurationCache cache, TenantConfigurationKeyPolicy keyPolicy,
            TenantContext tenantContext) {
        this.store = store;
        this.cache = cache;
        this.keyPolicy = keyPolicy;
        this.tenantContext = tenantContext;
    }

    /**
     * Resolves the configuration entries the current tenant is permitted to override, ready to be
     * injected into the thread-scoped configuration. Never throws: on any failure it logs and returns
     * an empty map, so a configuration problem can never break request handling.
     *
     * @return the injectable entries, never {@code null}
     */
    public Map<String, String> resolveInjectableForCurrentTenant() {
        if (tenantContext.isNotInitialized()) {
            return Map.of();
        }
        try {
            return keyPolicy.filterInjectable(load());
        } catch (SQLException | RuntimeException ex) {
            LOGGER.error("Failed to resolve tenant configuration for tenant [{}]. Continuing without tenant overrides.", currentTenantId(),
                    ex);
            return Map.of();
        }
    }

    /**
     * Lists all raw configuration entries of the current tenant (unfiltered by the key policy).
     *
     * @return the entries
     * @throws SQLException if the read fails
     */
    public List<TenantConfiguration> listForCurrentTenant() throws SQLException {
        return load().entrySet()
                     .stream()
                     .map(entry -> new TenantConfiguration(entry.getKey(), entry.getValue()))
                     .toList();
    }

    /**
     * Inserts or updates a configuration entry of the current tenant.
     *
     * @param key the configuration key
     * @param value the configuration value
     * @throws SQLException if the write fails
     */
    public void set(String key, String value) throws SQLException {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Configuration key must not be blank");
        }
        store.set(key, value);
        cache.invalidate(currentTenantId());
    }

    /**
     * Deletes a configuration entry of the current tenant.
     *
     * @param key the configuration key
     * @throws SQLException if the delete fails
     */
    public void delete(String key) throws SQLException {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Configuration key must not be blank");
        }
        store.delete(key);
        cache.invalidate(currentTenantId());
    }

    private Map<String, String> load() throws SQLException {
        return cache.get(currentTenantId(), store::readAll);
    }

    private String currentTenantId() {
        return tenantContext.getCurrentTenant()
                            .getId();
    }

}
