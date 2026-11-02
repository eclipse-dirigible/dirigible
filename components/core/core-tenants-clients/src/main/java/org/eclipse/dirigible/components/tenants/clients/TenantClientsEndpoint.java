/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.clients;

import java.util.List;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.tenants.provisioning.external.TenantProvisioningApiEnabledCondition;
import org.eclipse.dirigible.components.tenants.provisioning.external.TenantProvisioningRoles;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.security.RolesAllowed;

/**
 * The integration clients of the tenant provisioning API: the external provisioning system - which
 * creates the clients at the identity provider - writes a tenant's complete client registry here,
 * and asks which roles a client may be given.
 *
 * <p>
 * It exists exactly when the tenant provisioning API does, behind the same roles, and its URLs are
 * already claimed by that API's security configurator.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_TENANT_PROVISIONING)
@RolesAllowed({TenantProvisioningRoles.TENANT_PROVISIONER, TenantProvisioningRoles.ADMINISTRATOR, TenantProvisioningRoles.OPERATOR})
@Conditional(TenantProvisioningApiEnabledCondition.class)
class TenantClientsEndpoint extends BaseEndpoint {

    /** The registry. */
    private final TenantClientRegistryService registry;

    /** The roles a client may hold. */
    private final TenantClientRoles roles;

    /**
     * Instantiates the endpoint.
     *
     * @param registry the registry
     * @param roles the roles a client may hold
     */
    TenantClientsEndpoint(TenantClientRegistryService registry, TenantClientRoles roles) {
        this.registry = registry;
        this.roles = roles;
    }

    /**
     * Applies a tenant's complete client registry. Answers 200 whether or not it was newer than the
     * stored one: a stale registry changes nothing, which is normal, never an error.
     *
     * @param tenantId the tenant id
     * @param body the registry
     * @return what the apply did
     */
    @PutMapping(path = "tenants/{tenantId}/clients", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TenantClientSync.Result> apply(@PathVariable("tenantId") String tenantId, @RequestBody TenantClientSync body) {
        return ResponseEntity.ok(registry.apply(tenantId, body));
    }

    /**
     * The client registry of a tenant, revoked clients included, for diagnostics.
     *
     * @param tenantId the tenant id
     * @return the registry
     */
    @GetMapping(path = "tenants/{tenantId}/clients", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<TenantClientSync> list(@PathVariable("tenantId") String tenantId) {
        return ResponseEntity.ok(registry.list(tenantId));
    }

    /**
     * The roles an integration client may be given: the roles this application declares, without the
     * tenant's {@code Owner} and the roles of the instance.
     *
     * @return the roles
     */
    @GetMapping(path = "client-roles", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<GrantableRoles> grantableRoles() {
        return ResponseEntity.ok(new GrantableRoles(roles.grantable()));
    }

    /**
     * The roles a client may be given.
     *
     * @param roles the role names, sorted
     */
    record GrantableRoles(List<String> roles) {
    }
}
