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

import java.util.List;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.tenants.provisioning.external.TenantProvisioningApiEnabledCondition;
import org.eclipse.dirigible.components.tenants.provisioning.external.TenantProvisioningRoles;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.security.RolesAllowed;

/**
 * The roles a person of a tenant may be given, for the provisioning system (#8728) - the catalog
 * Settings → Users offers, so the system that creates the identity provider's groups offers the
 * same names. The users' counterpart of {@code client-roles}.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_TENANT_PROVISIONING)
@RolesAllowed({TenantProvisioningRoles.TENANT_PROVISIONER, TenantProvisioningRoles.ADMINISTRATOR, TenantProvisioningRoles.OPERATOR})
@Conditional(TenantProvisioningApiEnabledCondition.class)
class TenantUserRolesEndpoint extends BaseEndpoint {

    /** The catalog. */
    private final TenantUserRoles roles;

    /**
     * Instantiates the endpoint.
     *
     * @param roles the catalog
     */
    TenantUserRolesEndpoint(TenantUserRoles roles) {
        this.roles = roles;
    }

    /**
     * The roles a person of a tenant may be given.
     *
     * @return the catalog
     */
    @GetMapping(path = "user-roles", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<AssignableRoles> assignableRoles() {
        return ResponseEntity.ok(new AssignableRoles(roles.catalog()));
    }

    /**
     * The roles a person may be given.
     *
     * @param roles the catalog, in the order a page offers it
     */
    record AssignableRoles(List<TenantUserRoles.Offer> roles) {
    }
}
