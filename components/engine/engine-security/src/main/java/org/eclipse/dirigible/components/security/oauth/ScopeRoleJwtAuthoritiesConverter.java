/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.http.roles.Roles;
import org.eclipse.dirigible.components.base.util.AuthoritiesUtil;
import org.eclipse.dirigible.components.base.util.ScopeAuthoritiesUtil;
import org.eclipse.dirigible.components.security.service.ScopeService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Converts the {@code scope} claim of a resource-server (Bearer) JWT into Dirigible role
 * authorities.
 *
 * <p>
 * OAuth2 Client Credentials (machine-to-machine) tokens carry no user, no groups and no id token,
 * so the only access signal is the {@code scope} claim. Each scope's bare name (the part after the
 * last {@code /}) is mapped to Dirigible roles - 1:1 by default, or expanded through a
 * {@code *.scopes} artefact when one scope must grant several roles. The resulting authorities use
 * the same {@code ROLE_} convention as {@link AuthoritiesUtil}, so role-protected endpoints
 * authorize M2M requests exactly as they do interactive ones.
 *
 * <p>
 * Token validation (signature, issuer, jwk-set) is unaffected - this converter only derives
 * authorities from the already validated claims. It is shared by every resource-server security
 * configuration (Cognito, Keycloak, ...).
 */
@Component
public class ScopeRoleJwtAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    /** The standard OAuth2 scope claim. */
    private static final String SCOPE_CLAIM = "scope";

    /** The alternative scope claim used by some authorization servers. */
    private static final String SCP_CLAIM = "scp";

    /** The scope service. */
    private final ScopeService scopeService;

    /** Whether trial mode is enabled. */
    private final boolean trialModeEnabled;

    /**
     * Instantiates a new scope-role JWT authorities converter.
     *
     * @param scopeService the scope service
     */
    public ScopeRoleJwtAuthoritiesConverter(ScopeService scopeService) {
        this.scopeService = scopeService;
        this.trialModeEnabled = DirigibleConfig.TRIAL_ENABLED.getBooleanValue();
    }

    /**
     * Convert.
     *
     * @param jwt the validated JWT
     * @return the derived role authorities
     */
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        if (trialModeEnabled) {
            return new HashSet<>(AuthoritiesUtil.toAuthorities(Arrays.stream(Roles.values())
                                                                     .map(Roles::getRoleName)
                                                                     .collect(Collectors.toSet())));
        }
        Collection<String> rawScopes = extractScopes(jwt);
        Set<String> roleNames = ScopeAuthoritiesUtil.resolveRoleNames(rawScopes, scopeService.getScopeRolesMappings());
        return new HashSet<>(AuthoritiesUtil.toAuthorities(roleNames));
    }

    /**
     * Extracts the raw scope values from the {@code scope} (or {@code scp}) claim, which may be either
     * a space-delimited string or a list.
     *
     * @param jwt the JWT
     * @return the raw scope values
     */
    private Collection<String> extractScopes(Jwt jwt) {
        Object scopeClaim = jwt.getClaim(SCOPE_CLAIM);
        if (scopeClaim == null) {
            scopeClaim = jwt.getClaim(SCP_CLAIM);
        }
        if (scopeClaim instanceof String scopeString) {
            return Arrays.stream(scopeString.trim()
                                            .split("\\s+"))
                         .filter(token -> !token.isEmpty())
                         .collect(Collectors.toList());
        }
        if (scopeClaim instanceof Collection<?> scopeCollection) {
            return scopeCollection.stream()
                                  .filter(Objects::nonNull)
                                  .map(Object::toString)
                                  .collect(Collectors.toList());
        }
        return Collections.<String>emptyList();
    }
}
