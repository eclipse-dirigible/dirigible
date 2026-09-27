/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.users;

import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.dirigible.components.base.http.roles.ApplicationRoles;
import org.eclipse.dirigible.components.base.http.roles.Roles;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * What the users endpoints need beyond their {@code @RolesAllowed}: the tenant they act on - never
 * the default one, which has no users of its own - and, for the page, whether the caller would pass
 * those role checks.
 *
 * <p>
 * The role sets here are the ones {@link TenantUsersEndpoint} declares: managing is for the
 * {@link ApplicationRoles#OWNER} of the selected tenant, a developer or an administrator; reading
 * also for an operator.
 */
@Component
class TenantUsersAccess {

    /** The authority prefix of a role. */
    private static final String ROLE_PREFIX = "ROLE_";

    /** The roles that manage users - as {@code TenantUsersEndpoint} declares them. */
    static final Set<String> MANAGERS = Set.of(ApplicationRoles.OWNER, Roles.RoleNames.DEVELOPER, Roles.RoleNames.ADMINISTRATOR);

    /** The roles that read users - as {@code TenantUsersEndpoint} declares them. */
    static final Set<String> READERS =
            Set.of(ApplicationRoles.OWNER, Roles.RoleNames.DEVELOPER, Roles.RoleNames.ADMINISTRATOR, Roles.RoleNames.OPERATOR);

    /** The tenant context. */
    private final TenantContext tenantContext;

    /**
     * Instantiates the access rules.
     *
     * @param tenantContext the tenant context
     */
    TenantUsersAccess(TenantContext tenantContext) {
        this.tenantContext = tenantContext;
    }

    /**
     * Whether the caller may manage the users of the current tenant.
     *
     * @return true for an owner, a developer or an administrator in a tenant other than the default one
     */
    boolean canManage() {
        return currentTenantOrNull() != null && holdsAny(MANAGERS);
    }

    /**
     * Whether the caller may read the users of the current tenant.
     *
     * @return true for a manager or an operator in a tenant other than the default one
     */
    boolean canRead() {
        return currentTenantOrNull() != null && holdsAny(READERS);
    }

    /**
     * The current tenant, refusing the default one.
     *
     * @return the tenant id
     */
    String requireTenant() {
        Tenant tenant = currentTenantOrNull();
        if (tenant == null) {
            throw new TenantUsersException(HttpStatus.CONFLICT, "DEFAULT_TENANT",
                    "Users are managed per tenant - select a tenant first; the default tenant has none to manage");
        }
        return tenant.getId();
    }

    /**
     * The caller's name.
     *
     * @return the principal name
     */
    String callerName() {
        Authentication authentication = SecurityContextHolder.getContext()
                                                             .getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    /**
     * The current tenant unless it is the default one.
     *
     * @return the tenant, or null
     */
    private Tenant currentTenantOrNull() {
        if (tenantContext.isNotInitialized()) {
            return null;
        }
        Tenant tenant = tenantContext.getCurrentTenant();
        return tenant == null || tenant.isDefault() ? null : tenant;
    }

    /**
     * Whether the caller holds any of the roles.
     *
     * @param roles the roles
     * @return whether they do
     */
    private static boolean holdsAny(Set<String> roles) {
        Authentication authentication = SecurityContextHolder.getContext()
                                                             .getAuthentication();
        if (authentication == null) {
            return false;
        }
        Set<String> held = authentication.getAuthorities()
                                         .stream()
                                         .map(GrantedAuthority::getAuthority)
                                         .collect(Collectors.toSet());
        return roles.stream()
                    .anyMatch(role -> held.contains(ROLE_PREFIX + role));
    }
}
