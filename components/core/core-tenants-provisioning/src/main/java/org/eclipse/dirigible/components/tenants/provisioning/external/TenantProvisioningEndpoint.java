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

import java.net.URI;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.provisioning.external.TenantRegistrationService.RegistrationResult;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    /** The activation service. */
    private final TenantActivationService activationService;

    /** The deprovisioning service. */
    private final TenantDeprovisioningService deprovisioningService;

    /** The suspension service. */
    private final TenantSuspensionService suspensionService;

    /**
     * Instantiates a new tenant provisioning endpoint.
     *
     * @param registrationService the registration service
     * @param activationService the activation service
     * @param deprovisioningService the deprovisioning service
     * @param suspensionService the suspension service
     */
    TenantProvisioningEndpoint(TenantRegistrationService registrationService, TenantActivationService activationService,
            TenantDeprovisioningService deprovisioningService, TenantSuspensionService suspensionService) {
        this.registrationService = registrationService;
        this.activationService = activationService;
        this.deprovisioningService = deprovisioningService;
        this.suspensionService = suspensionService;
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
     * Removes the tenant and everything this application holds of it - its deprovisioning.
     *
     * <p>
     * Synchronous: when it answers, nothing runs for the tenant any more and its data source,
     * connection pool and system database rows are gone. A tenant that does not exist answers 204 as
     * well, so a caller retrying after a timeout converges; a 404 from this route can therefore only
     * mean that the route does not exist. The tenant's data schema is the caller's: drop it after this
     * call.
     *
     * @param tenantId the tenant id
     * @return 204; 409 while the tenant is being activated, 400 for an invalid id or the default tenant
     */
    @DeleteMapping("/{tenantId}")
    ResponseEntity<Void> deprovisionTenant(@PathVariable("tenantId") String tenantId) {
        deprovisioningService.deprovision(tenantId);
        return ResponseEntity.noContent()
                             .build();
    }

    /**
     * Suspends the tenant ({@code {"status":"SUSPENDED"}}) or resumes it
     * ({@code {"status":"PROVISIONED"}}), without touching its data.
     *
     * <p>
     * Synchronous: when it answers, a suspended tenant's people and integration clients are refused and
     * nothing runs for it, and a resumed one runs exactly as before. Suspending a suspended tenant and
     * resuming a provisioned one run that half again, which is how a run that failed half way is
     * finished.
     *
     * @param tenantId the tenant id
     * @param parameter the requested status
     * @return 200 with the tenant; 409 for a tenant that is neither provisioned nor suspended, 404 for
     *         an unknown one, 400 for an invalid id, the default tenant or another status
     */
    @PutMapping("/{tenantId}/status")
    ResponseEntity<TenantProvisioningState> changeTenantStatus(@PathVariable("tenantId") String tenantId,
            @RequestBody TenantStatusParameter parameter) {
        return ResponseEntity.ok(suspensionService.changeStatus(tenantId, parameter.status()));
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

    /**
     * Activates the tenant and starts materializing its artefacts.
     *
     * <p>
     * Answers before the materialization is done: it is a full synchronization pass, tens of seconds to
     * minutes, far beyond any sane HTTP timeout. The caller polls the location in the response.
     * Repeating the call is safe and is how a failed initialization is retried.
     *
     * @param tenantId the tenant id
     * @return 202, pointing at the status to poll
     */
    @PostMapping("/{tenantId}/activation")
    ResponseEntity<TenantInitializationState> activateTenant(@PathVariable("tenantId") String tenantId) {
        Tenant tenant = registrationService.requireTenant(tenantId);
        activationService.activate(tenant);
        return ResponseEntity.accepted()
                             .location(URI.create(
                                     "/" + BaseEndpoint.PREFIX_ENDPOINT_TENANT_PROVISIONING + "tenants/" + tenantId + "/activation"))
                             .body(registrationService.read(tenantId)
                                                      .initialization());
    }

    /**
     * How far the tenant's initialization has got.
     *
     * @param tenantId the tenant id
     * @return the initialization state
     */
    @GetMapping("/{tenantId}/activation")
    ResponseEntity<TenantInitializationState> getActivation(@PathVariable("tenantId") String tenantId) {
        return ResponseEntity.ok(registrationService.read(tenantId)
                                                    .initialization());
    }
}
