/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.security.oauth2.tenant;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.stereotype.Component;

/**
 * The token claim the user groups are read from, and the reading itself.
 *
 * <p>
 * Identity providers disagree on the claim - AWS Cognito uses {@code cognito:groups}, a Keycloak
 * realm typically {@code groups} - so it is configured, through
 * {@link DirigibleConfig#TENANT_GROUPS_CLAIM}. This one bean is what everything granting tenant
 * roles reads, so the login mapper and the tenant selection can never disagree on where to look.
 */
@Component
public class TenantGroupsClaim {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantGroupsClaim.class);

    private final String name;

    /**
     * Instantiates the configured claim.
     */
    public TenantGroupsClaim() {
        this(DirigibleConfig.TENANT_GROUPS_CLAIM.getStringValue());
    }

    /**
     * Instantiates a claim by name.
     *
     * @param name the claim name
     */
    public TenantGroupsClaim(String name) {
        this.name = name;
    }

    /**
     * Gets the name of the claim.
     *
     * @return the claim name
     */
    public String getName() {
        return name;
    }

    /**
     * Reads the groups of an authenticated user.
     *
     * @param authentication the authentication; may be {@code null}
     * @return the group names, never {@code null}
     */
    public Set<String> groupsOf(Authentication authentication) {
        if (null == authentication) {
            return Set.of();
        }
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            return toGroups(oidcUser.getClaims()
                                    .get(name),
                    name, authentication.getName());
        }
        return groupsOf(authentication.getAuthorities());
    }

    /**
     * Reads the groups out of the authorities of a login.
     *
     * @param authorities the authorities; may be {@code null}
     * @return the group names, never {@code null}
     */
    public Set<String> groupsOf(Collection<? extends GrantedAuthority> authorities) {
        return readGroups(authorities, name);
    }

    /**
     * Reads the groups out of the authorities of a login, from a claim named explicitly. Anything that
     * is not an OIDC user authority - a bearer token authority, for instance - carries no groups claim
     * and contributes nothing.
     *
     * @param authorities the authorities; may be {@code null}
     * @param claimName the claim to read
     * @return the group names, never {@code null}
     */
    static Set<String> readGroups(Collection<? extends GrantedAuthority> authorities, String claimName) {
        Set<String> groups = new LinkedHashSet<>();
        if (null == authorities) {
            return groups;
        }
        for (GrantedAuthority authority : authorities) {
            if (authority instanceof OidcUserAuthority oidcUserAuthority) {
                groups.addAll(toGroups(oidcUserAuthority.getAttributes()
                                                        .get(claimName),
                        claimName, oidcUserAuthority.getIdToken()
                                                    .getSubject()));
            }
        }
        return groups;
    }

    private static Set<String> toGroups(Object claimValue, String claimName, String userName) {
        if (null == claimValue) {
            return Set.of();
        }
        if (claimValue instanceof Collection<?> claimValues) {
            Set<String> groups = new LinkedHashSet<>();
            claimValues.stream()
                       .filter(value -> null != value)
                       .map(Object::toString)
                       .forEach(groups::add);
            return groups;
        }
        LOGGER.warn("Claim [{}] of user [{}] is not a collection but [{}] and cannot be read as groups.", claimName, userName,
                claimValue.getClass()
                          .getName());
        return Set.of();
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "TenantGroupsClaim [name=" + name + "]";
    }
}
