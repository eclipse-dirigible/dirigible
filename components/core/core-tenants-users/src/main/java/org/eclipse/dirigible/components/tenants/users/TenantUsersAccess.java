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

import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Who may do what with the users of the current tenant, decided in one place.
 *
 * <p>
 * The owner role is configuration, so it cannot be a {@code @RolesAllowed} constant: the
 * authorities are read here. It deliberately does not use {@code UserFacade.isInRole}, which
 * answers true for every developer and administrator. Administrators and operators may read, as
 * break-glass, but not invite: an invitation needs a requester who belongs to the tenant.
 */
@Component
class TenantUsersAccess {

    /** The authority prefix. */
    private static final String ROLE_PREFIX = "ROLE_";

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
     * @return true for a holder of the owner role in a tenant other than the default one
     */
    boolean canManage() {
        return currentTenantOrNull() != null && authorities().contains(ROLE_PREFIX + TenantUsersSettings.ownerRole());
    }

    /**
     * Whether the caller may read the users of the current tenant.
     *
     * @return true for a manager, an administrator or an operator
     */
    boolean canRead() {
        Set<String> authorities = authorities();
        return currentTenantOrNull() != null
                && (canManage() || authorities.contains(ROLE_PREFIX + "ADMINISTRATOR") || authorities.contains(ROLE_PREFIX + "OPERATOR"));
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
     * Refuses a caller who may not manage users.
     *
     * @return the tenant id
     */
    String requireManager() {
        String tenantId = requireTenant();
        if (!canManage()) {
            throw new TenantUsersException(HttpStatus.FORBIDDEN, "NOT_A_TENANT_OWNER",
                    "Only a holder of the [" + TenantUsersSettings.ownerRole() + "] role of this tenant may manage its users");
        }
        return tenantId;
    }

    /**
     * Refuses a caller who may not read users.
     *
     * @return the tenant id
     */
    String requireReader() {
        String tenantId = requireTenant();
        if (!canRead()) {
            throw new TenantUsersException(HttpStatus.FORBIDDEN, "NOT_A_TENANT_OWNER",
                    "Only a holder of the [" + TenantUsersSettings.ownerRole() + "] role of this tenant may see its users");
        }
        return tenantId;
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
     * The caller's authorities.
     *
     * @return the authority names
     */
    private static Set<String> authorities() {
        Authentication authentication = SecurityContextHolder.getContext()
                                                             .getAuthentication();
        if (authentication == null) {
            return Set.of();
        }
        return authentication.getAuthorities()
                             .stream()
                             .map(GrantedAuthority::getAuthority)
                             .collect(Collectors.toSet());
    }
}
