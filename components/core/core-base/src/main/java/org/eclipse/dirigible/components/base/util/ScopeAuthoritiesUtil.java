/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.util;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Translates OAuth2 {@code scope} claim values into Dirigible role names.
 *
 * <p>
 * OAuth2 Client Credentials (machine-to-machine) tokens carry no user, no groups and no id token -
 * the only access signal available is the {@code scope} claim. This utility derives Dirigible role
 * names from those scopes so that the standard {@link AuthoritiesUtil} role-authority machinery can
 * authorize M2M requests against role-protected endpoints.
 *
 * <p>
 * The logic is intentionally free of any Spring Security / OAuth2 types so it can be reused by
 * every resource-server security configuration (Cognito, Keycloak, ...) and unit tested in
 * isolation.
 */
public final class ScopeAuthoritiesUtil {

    /**
     * The separator between a resource-server identifier and the bare scope name in a Cognito scope
     * value.
     */
    private static final char SCOPE_SEPARATOR = '/';

    private ScopeAuthoritiesUtil() {}

    /**
     * Extracts the bare scope name from a raw scope value.
     *
     * <p>
     * AWS Cognito qualifies custom scopes as {@code <resource-server-identifier>/<scope-name>}, where
     * the resource-server identifier carries a generated random suffix and therefore must not be
     * assumed. The bare scope name is the substring after the <em>last</em> {@code /}. Standard OIDC
     * scopes ({@code openid}, {@code email}, {@code profile}, {@code aws.cognito.signin.user.admin},
     * ...) contain no {@code /} and are not roles.
     *
     * @param rawScope a single raw scope value
     * @return the bare scope name, or {@code null} if the value carries no {@code /} (i.e. is not a
     *         role-bearing scope)
     */
    public static String extractScopeName(String rawScope) {
        if (rawScope == null) {
            return null;
        }
        String trimmed = rawScope.trim();
        int separatorIndex = trimmed.lastIndexOf(SCOPE_SEPARATOR);
        if (separatorIndex < 0) {
            return null;
        }
        String scopeName = trimmed.substring(separatorIndex + 1);
        return scopeName.isEmpty() ? null : scopeName;
    }

    /**
     * Resolves the Dirigible role names granted by a collection of raw scope values.
     *
     * <p>
     * For each raw scope the bare scope name is derived (see {@link #extractScopeName(String)}); scopes
     * that are not role-bearing are ignored. Each bare scope name is then mapped to role names:
     * <ul>
     * <li>if the scope name has an entry in {@code scopeToRoles}, it expands to the role names declared
     * there;</li>
     * <li>otherwise it falls back to a 1:1 mapping - the scope name is the role name.</li>
     * </ul>
     *
     * @param rawScopes the raw scope values (e.g. from the space-delimited {@code scope} claim); may be
     *        {@code null}
     * @param scopeToRoles the configured scope-name to role-names mappings; may be {@code null} or
     *        empty
     * @return the set of resolved Dirigible role names (never {@code null})
     */
    public static Set<String> resolveRoleNames(Collection<String> rawScopes, Map<String, ? extends Collection<String>> scopeToRoles) {
        if (rawScopes == null || rawScopes.isEmpty()) {
            return Collections.emptySet();
        }
        Map<String, ? extends Collection<String>> mappings = scopeToRoles == null ? Collections.emptyMap() : scopeToRoles;
        Set<String> roleNames = new LinkedHashSet<>();
        for (String rawScope : rawScopes) {
            String scopeName = extractScopeName(rawScope);
            if (scopeName == null) {
                continue;
            }
            Collection<String> mappedRoles = mappings.get(scopeName);
            if (mappedRoles != null && !mappedRoles.isEmpty()) {
                roleNames.addAll(mappedRoles);
            } else {
                roleNames.add(scopeName);
            }
        }
        return roleNames;
    }
}
