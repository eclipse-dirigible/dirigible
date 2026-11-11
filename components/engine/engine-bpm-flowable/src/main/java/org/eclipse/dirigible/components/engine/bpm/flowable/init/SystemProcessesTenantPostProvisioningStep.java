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

import java.util.Set;

import org.eclipse.dirigible.commons.api.helpers.LogSanitizer;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.base.tenant.TenantPostProvisioningStep;
import org.eclipse.dirigible.components.base.tenant.TenantProvisioningException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Gives a tenant provisioned while the instance runs the system processes.
 *
 * <p>
 * {@link FlowableProcessesInitializer} deploys them once, when the instance is ready, for the
 * tenants that exist then. Without this step a tenant provisioned afterwards had none of them until
 * the next restart, and starting one - a schema export, for example - failed for that tenant alone.
 */
@Component
class SystemProcessesTenantPostProvisioningStep implements TenantPostProvisioningStep {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemProcessesTenantPostProvisioningStep.class);

    private final SystemProcessesDeployer deployer;
    private final TenantContext tenantContext;

    SystemProcessesTenantPostProvisioningStep(SystemProcessesDeployer deployer, TenantContext tenantContext) {
        this.deployer = deployer;
        this.tenantContext = tenantContext;
    }

    /**
     * Does nothing: a caller that cannot name the tenants it provisioned has none this step could
     * deploy for, and the tenants that existed at startup already have the processes.
     */
    @Override
    public void execute() throws TenantProvisioningException {
        // nothing to deploy without the tenants
    }

    @Override
    public void execute(Set<String> tenantIds) throws TenantProvisioningException {
        for (String tenantId : tenantIds) {
            LOGGER.info("Deploying the system processes for the new tenant [{}]", LogSanitizer.sanitize(tenantId));
            tenantContext.execute(tenantId, () -> {
                deployer.deployForCurrentTenant();
                return null;
            });
        }
    }

}
