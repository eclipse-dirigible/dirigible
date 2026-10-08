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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.commons.config.ConfigDescriptors;
import org.eclipse.dirigible.commons.config.ConfigGroup;
import org.eclipse.dirigible.commons.config.ConfigGroups;
import org.eclipse.dirigible.commons.config.SensitiveConfigs;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.configurations.domain.TenantConfiguration;
import org.eclipse.dirigible.components.configurations.domain.TenantConfigurationDescriptor;
import org.eclipse.dirigible.components.configurations.domain.TenantConfigurationGroup;
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

    /** Tenants whose configuration loaded successfully at least once - a later failure is abnormal. */
    private final java.util.Set<String> loadedOnce = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** Tenants already warned during warm-up - keeps the boot log to ONE line per tenant. */
    private final java.util.Set<String> warmupWarned = java.util.concurrent.ConcurrentHashMap.newKeySet();

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
        String tenantId = currentTenantId();
        try {
            Map<String, String> injectable = keyPolicy.filterInjectable(load());
            loadedOnce.add(tenantId);
            return injectable;
        } catch (SQLException | RuntimeException ex) {
            // The store is unreachable on the FIRST requests of a fresh instance: the default
            // datasource artefact is registered by the first synchronization pass, which may still
            // be running when the request arrives. That warm-up gap is expected and fail-soft - one
            // WARN line without a stack trace, not an ERROR per request. A failure AFTER the tenant's
            // configuration has loaded successfully is abnormal and keeps the full ERROR.
            if (loadedOnce.contains(tenantId)) {
                LOGGER.error("Failed to resolve tenant configuration for tenant [{}]. Continuing without tenant overrides.", tenantId, ex);
            } else if (warmupWarned.add(tenantId)) {
                LOGGER.warn(
                        "Tenant configuration for tenant [{}] is not readable yet ({}). Continuing without tenant overrides"
                                + " - expected on a starting instance until the first synchronization pass completes.",
                        tenantId, ex.getMessage());
            } else {
                LOGGER.debug("Tenant configuration for tenant [{}] is still not readable. Continuing without tenant overrides.", tenantId,
                        ex);
            }
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
     * Lists the predefined (allow-listed) configuration keys together with the current tenant's stored
     * value for each - {@code value} is {@code null} when the tenant has not set that key. This is what
     * the settings UI renders: the fixed set of overridable properties, not just the ones already
     * stored.
     *
     * @return one entry per allowed key, in the policy's display order
     * @throws SQLException if the read fails
     */
    public List<TenantConfiguration> listPredefinedForCurrentTenant() throws SQLException {
        Map<String, String> stored = load();
        return keyPolicy.allowedKeys()
                        .stream()
                        .map(key -> new TenantConfiguration(key, stored.get(key)))
                        .toList();
    }

    /**
     * {@link #listPredefinedForCurrentTenant()} limited to the keys of the given groups.
     *
     * @param groups the groups to include; {@code null} or empty means every group
     * @return one entry per allowed key in those groups, in the policy's display order
     * @throws SQLException if the read fails
     */
    public List<TenantConfiguration> listPredefinedForCurrentTenant(Collection<ConfigGroup> groups) throws SQLException {
        return listPredefinedForCurrentTenant().stream()
                                               .filter(entry -> ConfigDescriptors.inGroups(entry.key(), groups))
                                               .toList();
    }

    /**
     * The groups that hold tenant-overridable keys, with the number of those keys and of the ones the
     * current tenant sets.
     *
     * @return the groups, in display order
     * @throws SQLException if the read fails
     */
    public List<ConfigDescriptors.GroupSummary> listGroupsForCurrentTenant() throws SQLException {
        List<ConfigDescriptors.GroupSummary> result = new ArrayList<>();
        for (TenantConfigurationGroup group : describeForCurrentTenant(null)) {
            ConfigGroup configGroup = ConfigGroup.fromId(group.group())
                                                 .orElse(ConfigGroup.OTHER);
            int set = (int) group.entries()
                                 .stream()
                                 .filter(entry -> entry.value() != null)
                                 .count();
            result.add(
                    new ConfigDescriptors.GroupSummary(
                            configGroup.getId(), configGroup.getSection(), configGroup.getLabel(), configGroup.getOrder(), group.entries()
                                                                                                                                .size(),
                            set));
        }
        return result;
    }

    /**
     * The tenant-overridable keys, grouped, each with the current tenant's value and the platform value
     * it overrides. Every value is masked when sensitive.
     *
     * @param groups the groups to include; {@code null} or empty means every group
     * @return the groups that hold overridable keys, in display order
     * @throws SQLException if the read fails
     */
    public List<TenantConfigurationGroup> describeForCurrentTenant(Collection<ConfigGroup> groups) throws SQLException {
        Map<ConfigGroup, List<TenantConfigurationDescriptor>> byGroup = new LinkedHashMap<>();
        for (TenantConfiguration entry : listPredefinedForCurrentTenant(groups)) {
            String key = entry.key();
            byGroup.computeIfAbsent(ConfigGroups.resolve(key), group -> new ArrayList<>())
                   .add(new TenantConfigurationDescriptor(key, ConfigGroups.subgroup(key), null, ConfigDescriptors.platformValue(key),
                           SensitiveConfigs.mask(key, entry.value()), SensitiveConfigs.isSensitive(key)));
        }
        List<TenantConfigurationGroup> result = new ArrayList<>();
        for (ConfigGroup group : ConfigGroup.values()) {
            List<TenantConfigurationDescriptor> entries = byGroup.get(group);
            if (entries != null) {
                result.add(new TenantConfigurationGroup(group.getId(), group.getSection(), group.getLabel(), entries));
            }
        }
        return result;
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
