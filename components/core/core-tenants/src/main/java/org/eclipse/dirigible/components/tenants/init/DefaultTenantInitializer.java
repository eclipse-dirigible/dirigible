/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.init;

import java.util.Optional;
import org.eclipse.dirigible.components.base.ApplicationListenersOrder.ApplicationReadyEventListeners;
import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * The Class DefaultTenantInitializer.
 */
@Order(ApplicationReadyEventListeners.DEFAULT_TENANT_INITIALIZER)
@Component
class DefaultTenantInitializer implements ApplicationListener<ApplicationReadyEvent> {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTenantInitializer.class);

    /** The default tenant. */
    private final Tenant defaultTenant;

    /** The tenant service. */
    private final TenantService tenantService;

    /**
     * Instantiates a new default tenant initializer.
     *
     * @param defaultTenant the default tenant
     * @param tenantService the tenant service
     */
    DefaultTenantInitializer(@DefaultTenant Tenant defaultTenant, TenantService tenantService) {
        this.defaultTenant = defaultTenant;
        this.tenantService = tenantService;
    }

    /**
     * On application event.
     *
     * @param event the event
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        LOGGER.info("Executing...");
        createDefaultTenant();
        LOGGER.info("Completed...");

    }

    /**
     * Creates the default tenant.
     */
    private void createDefaultTenant() {
        Optional<org.eclipse.dirigible.components.tenants.domain.Tenant> existingTenant = tenantService.findById(defaultTenant.getId());
        if (existingTenant.isPresent()) {
            LOGGER.info("Default tenant is already registed [{}]. Skipping its creation.", existingTenant.get());
            return;
        }
        org.eclipse.dirigible.components.tenants.domain.Tenant tenantEntity = new org.eclipse.dirigible.components.tenants.domain.Tenant();
        tenantEntity.setId(defaultTenant.getId());
        tenantEntity.setName(defaultTenant.getName());
        tenantEntity.setStatus(TenantStatus.PROVISIONED);
        tenantEntity.setSubdomain(defaultTenant.getSubdomain());
        tenantEntity.setLocation("-");
        tenantEntity.setType(org.eclipse.dirigible.components.tenants.domain.Tenant.ARTEFACT_TYPE);
        tenantEntity.updateKey();

        org.eclipse.dirigible.components.tenants.domain.Tenant savedTenant = tenantService.save(tenantEntity);
        LOGGER.info("Created default tenant [{}]", savedTenant);
    }

}
