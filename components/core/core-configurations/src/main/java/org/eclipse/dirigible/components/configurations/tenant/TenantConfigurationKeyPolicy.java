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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.commons.config.ConfigDescriptors;
import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Decides which configuration keys a tenant is allowed to override through its per-tenant
 * configuration.
 * <p>
 * This is an explicit white-list of full configuration keys (exact match, no wildcards): the
 * {@link DirigibleConfig} entries marked tenant-overridable, in their tenant order - plus a short
 * list of key prefixes for settings that exist once per platform object rather than once per
 * tenant. A key is injectable only when it is one of {@link #ALLOWED_KEYS} or starts with one of
 * {@link #ALLOWED_PREFIXES} (and names something after it); everything else is stored but inert, so
 * a tenant can never shadow infrastructure keys (database, repository, security, multi-tenancy
 * plumbing, ...). To expose another key, mark its entry with {@code meta().tenant(order)} once it
 * is safe to override per tenant - read per call, not cached at startup; add a prefix only for a
 * family of keys whose members cannot be enumerated in advance.
 */
@Component
class TenantConfigurationKeyPolicy {

    /**
     * The full configuration keys a tenant is allowed to override, in display order: branding, the
     * Region & Language settings, the documents switches and the application base URL.
     */
    private static final List<String> ALLOWED_KEYS = DirigibleConfig.tenantOverridableKeys();

    /**
     * The key prefixes a tenant may override every member of. Each prefix names a family of keys whose
     * members follow the objects the tenant's applications declare, so they cannot be listed here.
     */
    private static final List<String> ALLOWED_PREFIXES = List.of( //
            // The print template a document type prints with, per language:
            // DIRIGIBLE_PRINT_TEMPLATE_<ENTITY>_<LANG> = acme-blue | standard@1.28.0. One key per
            // document type and language the tenant chose a layout for (engine-document).
            "DIRIGIBLE_PRINT_TEMPLATE_");

    /**
     * Tells the configuration descriptors which keys a tenant may override, so the platform view can
     * flag them.
     */
    @PostConstruct
    void registerWithDescriptors() {
        ConfigDescriptors.setTenantOverridablePolicy(this::isInjectable);
    }

    /**
     * The full list of configuration keys a tenant may override, in display order.
     *
     * @return the allowed keys
     */
    List<String> allowedKeys() {
        return ALLOWED_KEYS;
    }

    /**
     * Checks whether a key belongs to one of the allowed key families, as opposed to being one of the
     * exact allowed keys.
     *
     * @param key the configuration key
     * @return true if the key starts with an allowed prefix and names something after it
     */
    boolean isPrefixed(String key) {
        if (key == null) {
            return false;
        }
        for (String prefix : ALLOWED_PREFIXES) {
            if (key.length() > prefix.length() && key.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the subset of the given entries whose keys the tenant is permitted to override.
     *
     * @param entries the raw tenant configuration entries
     * @return an insertion-ordered map with only the injectable entries, never {@code null}
     */
    Map<String, String> filterInjectable(Map<String, String> entries) {
        Map<String, String> injectable = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            if (isInjectable(entry.getKey())) {
                injectable.put(entry.getKey(), entry.getValue());
            }
        }
        return injectable;
    }

    /**
     * Checks whether a single key is one of the allowed keys or a member of an allowed key family.
     *
     * @param key the configuration key
     * @return true if the tenant may override the key
     */
    boolean isInjectable(String key) {
        return key != null && (ALLOWED_KEYS.contains(key) || isPrefixed(key));
    }

}
