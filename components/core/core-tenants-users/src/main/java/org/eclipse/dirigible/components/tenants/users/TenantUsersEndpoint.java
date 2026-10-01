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

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.base.http.roles.ApplicationRoles;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The users of the current tenant, for its owners.
 *
 * <p>
 * Protected like every other service, with {@code @RolesAllowed} on each route; the context answers
 * every authenticated user so a page can decide whether to offer the section. The tenant is always
 * the caller's selected tenant - never a request field - and never the default one.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_SECURITY + "tenant-users")
@Conditional(TenantUsersEnabledCondition.class)
class TenantUsersEndpoint extends BaseEndpoint {

    /** The access rules. */
    private final TenantUsersAccess access;

    /**
     * Instantiates the endpoint.
     *
     * @param access the access rules
     */
    TenantUsersEndpoint(TenantUsersAccess access) {
        this.access = access;
    }

    /**
     * Whether the caller may see and manage users here - answers every authenticated user.
     *
     * @return the context
     */
    @GetMapping(path = "/context", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TenantUsersContext> context() {
        boolean canManage = access.canManage();
        boolean canRead = access.canRead();
        String tenantId = canRead ? access.requireTenant() : null;
        return ResponseEntity.ok(new TenantUsersContext(true, tenantId, canManage, canRead, ApplicationRoles.OWNER, ApplicationRoles.ALL));
    }
}
