/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.provisioning;

import org.eclipse.dirigible.components.base.tenant.TenantPostProvisioningStep;
import org.eclipse.dirigible.components.base.tenant.TenantProvisioningStep;
import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.eclipse.dirigible.components.tenants.tenant.TenantFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * The Class TenantsProvisioner.
 */
@Component
class TenantsProvisioner {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TenantsProvisioner.class);

    /** The tenant service. */
    private final TenantService tenantService;

    /** The provisioning steps. */
    private final Set<TenantProvisioningStep> provisioningSteps;

    /** The post provisioning steps. */
    private final Set<TenantPostProvisioningStep> postProvisioningSteps;

    /** The tenant factory. */
    private final TenantFactory tenantFactory;

    /**
     * Instantiates a new tenants provisioner.
     *
     * @param tenantService the tenant service
     * @param provisioningSteps the provisioning steps
     * @param postProvisioningSteps the post provisioning steps
     * @param tenantFactory the tenant factory
     */
    TenantsProvisioner(TenantService tenantService, Set<TenantProvisioningStep> provisioningSteps,
            Set<TenantPostProvisioningStep> postProvisioningSteps, TenantFactory tenantFactory) {
        this.tenantService = tenantService;
        this.provisioningSteps = provisioningSteps;
        this.postProvisioningSteps = postProvisioningSteps;
        this.tenantFactory = tenantFactory;
    }

    /**
     * Provision.
     */
    synchronized void provision() {
        LOGGER.debug("Starting tenants provisioning...");
        Set<Tenant> tenants = tenantService.findByStatus(TenantStatus.INITIAL);
        if (tenants.size() > 0) {
            LOGGER.info("Tenants applicable for provisioning [{}]", tenants);
        } else {
            LOGGER.debug("No tenants applicable for provisioning");
        }

        tenants.forEach(this::provisionTenant);

        if (!tenants.isEmpty()) {
            LOGGER.info("Starting post provisioning process...");
            postProvisioningSteps.forEach(this::callPostProvisioningStep);
            LOGGER.info("Post provisioning process has completed.");
        }
        if (tenants.size() > 0) {
            LOGGER.info("Tenants [{}] have been provisioned successfully.", tenants);
        } else {
            LOGGER.debug("No tenants needed to be provisioned.");
        }
    }

    /**
     * Provision tenant.
     *
     * @param tenant the tenant
     */
    private void provisionTenant(Tenant tenant) {
        LOGGER.info("Starting provisioning process for tenant [{}]...", tenant);

        try {
            org.eclipse.dirigible.components.base.tenant.Tenant t = tenantFactory.createFromEntity(tenant);
            provisioningSteps.forEach(step -> step.execute(t));

            tenant.setStatus(TenantStatus.PROVISIONED);
            tenantService.save(tenant);

            LOGGER.info("Tenant [{}] has been provisioned successfully.", tenant);
        } catch (RuntimeException ex) {
            LOGGER.error("Failed to provision tenant [{}]. Continue with the next one.", tenant, ex);
        }
    }

    /**
     * Call post provisioning step.
     *
     * @param step the step
     */
    private void callPostProvisioningStep(TenantPostProvisioningStep step) {
        try {
            step.execute();
        } catch (RuntimeException ex) {
            LOGGER.error("PostProvisioning step [{}] has failed.", step, ex);
        }
    }

}
