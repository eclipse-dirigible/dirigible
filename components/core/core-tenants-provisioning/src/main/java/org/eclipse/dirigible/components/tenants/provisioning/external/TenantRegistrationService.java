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

import java.util.Optional;

import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Registers tenants on behalf of an external provisioner, and reads them back.
 *
 * <p>
 * Registration is idempotent because the provisioner that drives it is a retrying process: a step
 * that timed out may have registered the tenant already, and re-running the whole sequence has to
 * converge rather than collide. So a registration of a tenant that exists is an update of what the
 * caller can own - its name and, when supplied, its subdomain - and never a change of its status,
 * which belongs to the activation.
 */
@Service
@Conditional(TenantProvisioningApiEnabledCondition.class)
class TenantRegistrationService {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantRegistrationService.class);

    /** Marks the tenants this API owns, so an operator can tell them from the ones created by hand. */
    static final String LOCATION = "TENANT_PROVISIONING_API";

    /**
     * The artefact location of a tenant: the marker above, made unique by the tenant's own id.
     *
     * <p>
     * An artefact's unique key is {@code type:location:name}, so a constant location would make the
     * <em>display name</em> the unique part - and display names are not unique. Two customers may both
     * be called "Acme Ltd", and the second registration would fail on the key index. The tenant id is
     * the identity here (ADR-010, ids are shared across applications), so it belongs in the key.
     *
     * @param tenantId the tenant id
     * @return the location
     */
    private static String locationOf(String tenantId) {
        return LOCATION + "/" + tenantId;
    }

    /** The tenant service. */
    private final TenantService tenantService;

    /** The initialization status calculator. */
    private final TenantInitializationStatusCalculator statusCalculator;

    /**
     * Instantiates a new tenant registration service.
     *
     * @param tenantService the tenant service
     * @param statusCalculator the initialization status calculator
     */
    TenantRegistrationService(TenantService tenantService, TenantInitializationStatusCalculator statusCalculator) {
        this.tenantService = tenantService;
        this.statusCalculator = statusCalculator;
    }

    /**
     * Registers a tenant, or updates the one already registered under that id.
     *
     * @param tenantId the caller-supplied tenant id
     * @param parameter the registration body
     * @return the resulting state, and whether the tenant was created
     */
    RegistrationResult register(String tenantId, RegisterTenantParameter parameter) {
        if (!TenantIds.isValid(tenantId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, TenantIds.invalidMessage(tenantId));
        }
        String subdomain = parameter.getSubdomain() == null || parameter.getSubdomain()
                                                                        .isBlank() ? tenantId : parameter.getSubdomain();
        if (!TenantIds.isValid(subdomain)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, TenantIds.invalidSubdomainMessage(subdomain));
        }
        rejectSubdomainOfAnotherTenant(tenantId, subdomain);

        Optional<Tenant> existing = tenantService.findById(tenantId);
        if (existing.isPresent()) {
            Tenant tenant = existing.get();
            LOGGER.info("Tenant [{}] is already registered in status [{}]. Updating its registration.", tenantId, tenant.getStatus());
            tenant.setName(parameter.getName());
            if (parameter.getSubdomain() != null && !parameter.getSubdomain()
                                                              .isBlank()) {
                tenant.setSubdomain(parameter.getSubdomain());
            }
            // also converges a tenant registered before the location carried the id
            tenant.setLocation(locationOf(tenantId));
            tenant.updateKey();
            return new RegistrationResult(toState(tenantService.save(tenant)), false);
        }

        Tenant tenant = new Tenant(locationOf(tenantId), parameter.getName(), "Registered by the tenant provisioning API", subdomain,
                TenantStatus.PENDING_ACTIVATION);
        tenant.setId(tenantId);
        tenant.updateKey();
        LOGGER.info("Registering tenant [{}] with subdomain [{}] in status [{}].", tenantId, subdomain, TenantStatus.PENDING_ACTIVATION);
        return new RegistrationResult(toState(tenantService.save(tenant)), true);
    }

    /**
     * Reads a tenant.
     *
     * @param tenantId the tenant id
     * @return the state
     */
    TenantProvisioningState read(String tenantId) {
        return toState(requireTenant(tenantId));
    }

    /**
     * Finds a tenant or answers 404 - the shared precondition of every operation of this API.
     *
     * @param tenantId the tenant id
     * @return the tenant
     */
    Tenant requireTenant(String tenantId) {
        return tenantService.findById(tenantId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                    "There is no tenant with id [" + tenantId + "]"));
    }

    /**
     * Projects a tenant onto what the API exposes.
     *
     * @param tenant the tenant
     * @return the state
     */
    TenantProvisioningState toState(Tenant tenant) {
        return new TenantProvisioningState(tenant.getId(), tenant.getName(), tenant.getSubdomain(), tenant.getStatus(),
                statusCalculator.calculate(tenant));
    }

    /**
     * The subdomain column is unique platform-wide, so a collision would surface as a constraint
     * violation deep in the persistence layer. Answering 409 here says which tenant owns it instead.
     *
     * @param tenantId the tenant being registered
     * @param subdomain the subdomain it asks for
     */
    private void rejectSubdomainOfAnotherTenant(String tenantId, String subdomain) {
        tenantService.findBySubdomain(subdomain)
                     .filter(owner -> !owner.getId()
                                            .equals(tenantId))
                     .ifPresent(owner -> {
                         throw new ResponseStatusException(HttpStatus.CONFLICT,
                                 "Subdomain [" + subdomain + "] is already used by tenant [" + owner.getId() + "]");
                     });
    }

    /**
     * The outcome of a registration.
     *
     * @param state the resulting tenant state
     * @param created whether the tenant was created rather than updated
     */
    record RegistrationResult(TenantProvisioningState state, boolean created) {
    }
}
