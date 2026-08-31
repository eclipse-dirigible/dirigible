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
import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;

/**
 * Registers the data source of an externally provisioned tenant from credentials the provisioner
 * created itself.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_TENANT_PROVISIONING + "tenants/{tenantId}/datasources")
@RolesAllowed({TenantProvisioningRoles.TENANT_PROVISIONER, TenantProvisioningRoles.ADMINISTRATOR, TenantProvisioningRoles.OPERATOR})
@Conditional(TenantProvisioningApiEnabledCondition.class)
class TenantDataSourceProvisioningEndpoint extends BaseEndpoint {

    /** The registration service. */
    private final TenantRegistrationService registrationService;

    /** The data source registration service. */
    private final TenantDataSourceRegistrationService dataSourceRegistrationService;

    /**
     * Instantiates a new tenant data source provisioning endpoint.
     *
     * @param registrationService the tenant registration service
     * @param dataSourceRegistrationService the data source registration service
     */
    TenantDataSourceProvisioningEndpoint(TenantRegistrationService registrationService,
            TenantDataSourceRegistrationService dataSourceRegistrationService) {
        this.registrationService = registrationService;
        this.dataSourceRegistrationService = dataSourceRegistrationService;
    }

    /**
     * Registers or updates the tenant's default data source.
     *
     * @param tenantId the tenant id
     * @param parameter the credentials
     * @return 201 when the data source was created, 200 when it was updated
     */
    @PutMapping("/default")
    ResponseEntity<Void> registerDefaultDataSource(@PathVariable("tenantId") String tenantId,
            @Valid @RequestBody TenantDataSourceParameter parameter) {
        Tenant tenant = registrationService.requireTenant(tenantId);
        boolean created = dataSourceRegistrationService.register(tenant, parameter);
        return ResponseEntity.status(created ? HttpStatus.CREATED : HttpStatus.OK)
                             .build();
    }
}
