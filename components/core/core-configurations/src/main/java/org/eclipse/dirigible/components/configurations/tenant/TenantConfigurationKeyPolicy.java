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

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.springframework.stereotype.Component;

/**
 * Decides which configuration keys a tenant is allowed to override through its per-tenant
 * configuration.
 * <p>
 * A key is injectable only when it matches the configurable allow-list
 * ({@code DIRIGIBLE_TENANT_CONFIGURATION_ALLOWED_KEYS}) and is not one of the protected
 * infrastructure keys. The protected list always wins, so even a wildcard allow-list can never let
 * a tenant shadow the database, repository, security or multi-tenancy plumbing.
 */
@Component
class TenantConfigurationKeyPolicy {

    /**
     * Prefixes of keys that must never be overridable per tenant, regardless of the allow-list. These
     * drive the runtime infrastructure and a tenant-level override would compromise isolation or
     * stability.
     */
    private static final List<String> PROTECTED_PREFIXES = List.of( //
            "DIRIGIBLE_DATABASE_", //
            "DIRIGIBLE_DATASOURCE_", //
            "DIRIGIBLE_MASTER_REPOSITORY_", //
            "DIRIGIBLE_REPOSITORY_", //
            "DIRIGIBLE_SCHEDULER_DATASOURCE", //
            "DIRIGIBLE_FLOWABLE_DATABASE_", //
            "DIRIGIBLE_MULTI_TENANT", //
            "DIRIGIBLE_TENANT_SUBDOMAIN", //
            "DIRIGIBLE_TENANT_CONFIGURATION_", //
            "DIRIGIBLE_BASIC_", //
            "DIRIGIBLE_OAUTH_", //
            "DIRIGIBLE_KEYCLOAK_", //
            "DIRIGIBLE_S3_", //
            "AWS_", //
            "spring.", //
            "server.", //
            "management.");

    /**
     * Returns the subset of the given entries whose keys the tenant is permitted to override.
     *
     * @param entries the raw tenant configuration entries
     * @return an insertion-ordered map with only the injectable entries, never {@code null}
     */
    Map<String, String> filterInjectable(Map<String, String> entries) {
        List<String> allowPatterns = parseAllowPatterns();
        Map<String, String> injectable = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            if (isInjectable(entry.getKey(), allowPatterns)) {
                injectable.put(entry.getKey(), entry.getValue());
            }
        }
        return injectable;
    }

    /**
     * Checks whether a single key is injectable.
     *
     * @param key the configuration key
     * @return true if the tenant may override the key
     */
    boolean isInjectable(String key) {
        return isInjectable(key, parseAllowPatterns());
    }

    private boolean isInjectable(String key, List<String> allowPatterns) {
        if (key == null || key.isBlank()) {
            return false;
        }
        if (isProtected(key)) {
            return false;
        }
        return matchesAny(key, allowPatterns);
    }

    private boolean isProtected(String key) {
        for (String prefix : PROTECTED_PREFIXES) {
            if (key.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesAny(String key, List<String> allowPatterns) {
        for (String pattern : allowPatterns) {
            if (matches(key, pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean matches(String key, String pattern) {
        if (pattern.endsWith("*")) {
            return key.startsWith(pattern.substring(0, pattern.length() - 1));
        }
        return key.equals(pattern);
    }

    private List<String> parseAllowPatterns() {
        String raw = DirigibleConfig.TENANT_CONFIGURATION_ALLOWED_KEYS.getStringValue();
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(raw.split(","))
                               .map(String::trim)
                               .filter(s -> !s.isEmpty())
                               .toList();
    }

}
