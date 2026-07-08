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

import org.springframework.stereotype.Component;

/**
 * Decides which configuration keys a tenant is allowed to override through its per-tenant
 * configuration.
 * <p>
 * This is an explicit white-list of full configuration keys (exact match, no wildcards). A key is
 * injectable only when it is one of {@link #ALLOWED_KEYS}; everything else is stored but inert, so
 * a tenant can never shadow infrastructure keys (database, repository, security, multi-tenancy
 * plumbing, ...). For now only the branding properties are exposed; add concrete keys to
 * {@link #ALLOWED_KEYS} as more become safe to override per tenant.
 */
@Component
class TenantConfigurationKeyPolicy {

    /**
     * The full configuration keys a tenant is allowed to override - the branding properties for now.
     */
    private static final List<String> ALLOWED_KEYS = List.of( //
            "DIRIGIBLE_BRANDING_NAME", //
            "DIRIGIBLE_BRANDING_SUBTITLE", //
            "DIRIGIBLE_BRANDING_BRAND", //
            "DIRIGIBLE_BRANDING_BRAND_URL", //
            "DIRIGIBLE_BRANDING_FAVICON", //
            "DIRIGIBLE_BRANDING_THEME", //
            "DIRIGIBLE_BRANDING_PREFIX", //
            "DIRIGIBLE_BRANDING_ANALYTICS");

    /**
     * The full list of configuration keys a tenant may override, in display order.
     *
     * @return the allowed keys
     */
    List<String> allowedKeys() {
        return ALLOWED_KEYS;
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
     * Checks whether a single key is one of the allowed keys.
     *
     * @param key the configuration key
     * @return true if the tenant may override the key
     */
    boolean isInjectable(String key) {
        return key != null && ALLOWED_KEYS.contains(key);
    }

}
