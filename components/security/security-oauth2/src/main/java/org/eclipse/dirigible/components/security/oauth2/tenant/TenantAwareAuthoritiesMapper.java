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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.http.roles.Roles;
import org.eclipse.dirigible.components.base.tenant.TenantResolutionStrategy;
import org.eclipse.dirigible.components.base.tenant.groups.TenantGroupsParser;
import org.eclipse.dirigible.components.base.util.AuthoritiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;

/**
 * Maps the groups of a logged in user to authorities, the way the configured tenant resolution
 * strategy requires.
 *
 * <p>
 * With {@link TenantResolutionStrategy#SUBDOMAIN} every group becomes an authority, exactly as
 * before this class existed - the tenant comes from the host, so the groups carry no tenant.
 *
 * <p>
 * With {@link TenantResolutionStrategy#TOKEN_GROUPS} only the <em>global</em> roles are mapped. The
 * tenant-bearing groups name a tenant each and which of them applies is not known at login: the
 * user picks a tenant afterwards and {@link TenantSelectionManager} grants that tenant's roles
 * then. Granting them all here would give a user every tenant's roles at once.
 *
 * <p>
 * Shared by the OIDC login profiles. They pass the claim their identity provider uses, which is
 * what the subdomain strategy keeps reading; the token groups strategy reads the configured
 * {@link TenantGroupsClaim} instead, since with two supported identity providers the claim is a
 * deployment decision rather than something to guess.
 */
public class TenantAwareAuthoritiesMapper implements GrantedAuthoritiesMapper {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantAwareAuthoritiesMapper.class);

    /** The configured claim, read in the token groups strategy. */
    private final TenantGroupsClaim groupsClaim;

    /** The claim of the identity provider, read in the subdomain strategy. */
    private final String providerGroupsClaim;

    private final boolean trialModeEnabled;

    /**
     * Instantiates a new tenant aware authorities mapper.
     *
     * @param groupsClaim the configured claim the user groups are read from
     * @param providerGroupsClaim the claim the identity provider of the active profile uses, e.g.
     *        {@code cognito:groups} or {@code groups}
     */
    public TenantAwareAuthoritiesMapper(TenantGroupsClaim groupsClaim, String providerGroupsClaim) {
        this(groupsClaim, providerGroupsClaim, DirigibleConfig.TRIAL_ENABLED.getBooleanValue());
    }

    /**
     * Instantiates a new tenant aware authorities mapper.
     *
     * @param groupsClaim the configured claim the user groups are read from
     * @param providerGroupsClaim the claim the identity provider of the active profile uses
     * @param trialModeEnabled whether trial mode grants every system role
     */
    TenantAwareAuthoritiesMapper(TenantGroupsClaim groupsClaim, String providerGroupsClaim, boolean trialModeEnabled) {
        this.groupsClaim = groupsClaim;
        this.providerGroupsClaim = providerGroupsClaim;
        this.trialModeEnabled = trialModeEnabled;
    }

    /**
     * Maps the authorities of the login to the authorities of the session.
     *
     * @param authorities the authorities the OIDC login produced
     * @return the granted authorities
     */
    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
        if (trialModeEnabled) {
            LOGGER.debug("Trial enabled - returning all available system roles for the current user.");
            return AuthoritiesUtil.toAuthorities(Arrays.stream(Roles.values())
                                                       .map(Roles::getRoleName)
                                                       .collect(Collectors.toSet()));
        }
        if (TenantResolutionStrategy.fromConfiguration() != TenantResolutionStrategy.TOKEN_GROUPS) {
            Set<String> providerGroups = TenantGroupsClaim.readGroups(authorities, providerGroupsClaim);
            return providerGroups.isEmpty() ? Collections.emptySet() : AuthoritiesUtil.toAuthorities(providerGroups);
        }
        Set<String> groups = groupsClaim.groupsOf(authorities);
        if (groups.isEmpty()) {
            LOGGER.debug("No groups found in claim [{}] of the current user.", groupsClaim.getName());
            return Collections.emptySet();
        }
        Set<String> globalRoles = TenantGroupsParser.parse(groups, DirigibleConfig.APP_ID.getStringValue())
                                                    .globalRoles();
        LOGGER.debug("Mapped [{}] global roles out of [{}] groups. The roles of a tenant are granted when it is selected.",
                globalRoles.size(), groups.size());
        return AuthoritiesUtil.toAuthorities(globalRoles);
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "TenantAwareAuthoritiesMapper [groupsClaim=" + groupsClaim + ", providerGroupsClaim=" + providerGroupsClaim
                + ", trialModeEnabled=" + trialModeEnabled + "]";
    }
}
