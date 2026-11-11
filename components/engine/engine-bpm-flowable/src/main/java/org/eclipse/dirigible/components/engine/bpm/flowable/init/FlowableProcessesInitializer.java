/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.init;

import org.eclipse.dirigible.components.base.ApplicationListenersOrder.ApplicationReadyEventListeners;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Deploys the system processes for every tenant this instance maintains, once it is ready. A tenant
 * provisioned later gets them from {@link SystemProcessesTenantPostProvisioningStep}.
 */
@Order(ApplicationReadyEventListeners.PROCESSES_INITIALIZER)
@Component
class FlowableProcessesInitializer implements ApplicationListener<ApplicationReadyEvent> {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableProcessesInitializer.class);

    private final SystemProcessesDeployer deployer;
    private final TenantContext tenantContext;

    FlowableProcessesInitializer(SystemProcessesDeployer deployer, TenantContext tenantContext) {
        this.deployer = deployer;
        this.tenantContext = tenantContext;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        LOGGER.info("Deploying the system processes for every tenant...");

        tenantContext.executeForEachMaintainedTenant(() -> {
            deployer.deployForCurrentTenant();
            return null;
        });

        LOGGER.info("Completed.");
    }

}
