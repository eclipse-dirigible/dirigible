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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;

/**
 * The users callback of the tenant provisioning API: an external provisioning system records here
 * what it did for a user - invited them, assigned them a role, or failed - so a tenant owner sees
 * it in the application without the application ever calling that system.
 *
 * <p>
 * It exists exactly when the tenant provisioning API does, behind the same roles, and its URL is
 * already claimed by that API's security configurator.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_TENANT_PROVISIONING + "tenants/{tenantId}/users")
@RolesAllowed({TenantProvisioningRoles.TENANT_PROVISIONER, TenantProvisioningRoles.ADMINISTRATOR, TenantProvisioningRoles.OPERATOR})
@Conditional(TenantProvisioningApiEnabledCondition.class)
class ApplicationUserProvisioningEndpoint extends BaseEndpoint {

    /** The service. */
    private final ApplicationUserService service;

    /**
     * Instantiates the endpoint.
     *
     * @param service the service
     */
    ApplicationUserProvisioningEndpoint(ApplicationUserService service) {
        this.service = service;
    }

    /**
     * Records the outcome of a request for a user.
     *
     * @param tenantId the tenant id
     * @param upsert the outcome
     * @return 201 when the user was created, 200 when it existed
     */
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ApplicationUserState> record(@PathVariable("tenantId") String tenantId,
            @Valid @RequestBody ApplicationUserUpsert upsert) {
        ApplicationUserService.CallbackResult result = service.applyCallback(tenantId, upsert);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                             .body(result.state());
    }

    /**
     * The users of a tenant.
     *
     * @param tenantId the tenant id
     * @return the users
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<ApplicationUserState>> list(@PathVariable("tenantId") String tenantId) {
        return ResponseEntity.ok(service.listOf(tenantId));
    }
}
