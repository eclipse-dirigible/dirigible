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
 * This is an explicit white-list of full configuration keys (exact match, no wildcards), plus a
 * short list of key prefixes for settings that exist once per platform object rather than once per
 * tenant. A key is injectable only when it is one of {@link #ALLOWED_KEYS} or starts with one of
 * {@link #ALLOWED_PREFIXES} (and names something after it); everything else is stored but inert, so
 * a tenant can never shadow infrastructure keys (database, repository, security, multi-tenancy
 * plumbing, ...). Add concrete keys to {@link #ALLOWED_KEYS} as more become safe to override per
 * tenant; add a prefix only for a family of keys whose members cannot be enumerated in advance.
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
            "DIRIGIBLE_BRANDING_ANALYTICS", //
            // The platform language set the Region & Language picker offers (comma-separated codes,
            // e.g. "en,bg,fr"). Per-tenant: each tenant decides which languages its users see; the
            // modules themselves carry whatever translations they ship, falling back to the default
            // (first) language for anything missing.
            "DIRIGIBLE_APPLICATION_LANGUAGES", //
            // The tenant's country as an ISO 3166-1 alpha-2 code, resolving the country-scoped label
            // variants a generated application declares. Per-tenant by definition: one deployment
            // serves companies in several jurisdictions, and the term a field goes by follows the
            // company's country rather than the reader's language.
            "DIRIGIBLE_APPLICATION_COUNTRY", //
            // Serve and store Office documents with the legacy Microsoft mime types. Per-tenant
            // because it depends on the client software a tenant's users actually open files with;
            // the resolver reads it per call, so a change applies without a restart.
            "DIRIGIBLE_DOCUMENTS_EXT_CONTENT_TYPE_MS_ENABLED", //
            // Whether the per-path CMS access grants are enforced at all. Per-tenant because whether
            // a tenant restricts folders by role is its own decision; the resolver reads it per
            // request, so switching it applies immediately.
            "DIRIGIBLE_CMS_ROLES_ENABLED", //
            // The application's externally-reachable base URL (e.g. the {appUrl} notify token) -
            // per-tenant because a tenant may be served from its own subdomain/host.
            "DIRIGIBLE_APP_BASE_URL");

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
