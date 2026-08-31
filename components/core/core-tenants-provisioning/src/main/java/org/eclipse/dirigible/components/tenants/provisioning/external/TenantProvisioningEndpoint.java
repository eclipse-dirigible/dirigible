/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.provisioning.external;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.tenants.provisioning.external.TenantRegistrationService.RegistrationResult;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
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
 * Registers a tenant on behalf of an external provisioner and reads it back.
 *
 * <p>
 * The tenant id is supplied by the caller, not generated: one tenant is the same customer across a
 * landscape of applications, so the provisioner that owns the landscape owns the id as well.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_TENANT_PROVISIONING + "tenants")
@RolesAllowed({TenantProvisioningRoles.TENANT_PROVISIONER, TenantProvisioningRoles.ADMINISTRATOR, TenantProvisioningRoles.OPERATOR})
@Conditional(TenantProvisioningApiEnabledCondition.class)
class TenantProvisioningEndpoint extends BaseEndpoint {

    /** The registration service. */
    private final TenantRegistrationService registrationService;

    /**
     * Instantiates a new tenant provisioning endpoint.
     *
     * @param registrationService the registration service
     */
    TenantProvisioningEndpoint(TenantRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    /**
     * Registers the tenant, or updates the registration of the one already under that id.
     *
     * @param tenantId the tenant id
     * @param parameter the registration body
     * @return 201 when the tenant was created, 200 when it already existed
     */
    @PutMapping("/{tenantId}")
    ResponseEntity<TenantProvisioningState> registerTenant(@PathVariable("tenantId") String tenantId,
            @Valid @RequestBody RegisterTenantParameter parameter) {
        RegistrationResult result = registrationService.register(tenantId, parameter);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                             .body(result.state());
    }

    /**
     * Reads the tenant and how far its initialization has got.
     *
     * @param tenantId the tenant id
     * @return the tenant state
     */
    @GetMapping("/{tenantId}")
    ResponseEntity<TenantProvisioningState> getTenant(@PathVariable("tenantId") String tenantId) {
        return ResponseEntity.ok(registrationService.read(tenantId));
    }
}
