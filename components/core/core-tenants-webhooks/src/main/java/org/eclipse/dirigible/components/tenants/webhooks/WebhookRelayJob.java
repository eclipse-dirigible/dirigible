/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.tenants.webhooks;

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.jobs.SystemJob;
import org.eclipse.dirigible.components.tenants.provisioning.external.TenantProvisioningApiEnabledCondition;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

/**
 * Delivers every running tenant's due webhook deliveries, each inside its tenant's scope - the
 * deliveries live in the tenant's schema. {@code executeForEachTenant} reaches {@code PROVISIONED}
 * tenants only, so a suspended tenant's deliveries are held, not dropped.
 *
 * <p>
 * Instantiated by Quartz; collaborators wired by {@code AutoWiringSpringBeanJobFactory} - the
 * documented exception to the constructor-injection convention that applies to every
 * {@link SystemJob} in this repo.
 */
@Component
@Conditional(TenantProvisioningApiEnabledCondition.class)
@DisallowConcurrentExecution
public class WebhookRelayJob extends SystemJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookRelayJob.class);

    @Autowired
    private WebhookRelay relay;

    @Autowired
    private TenantContext tenantContext;

    @Override
    protected String getJobKey() {
        return this.getClass()
                   .getSimpleName();
    }

    @Override
    protected String getTriggerKey() {
        return this.getClass()
                   .getSimpleName()
                + "Trigger";
    }

    @Override
    protected SimpleScheduleBuilder getSchedule() {
        int seconds = Math.max(1, DirigibleConfig.TENANT_WEBHOOKS_RELAY_INTERVAL_SECONDS.getIntValue());
        return simpleSchedule().withIntervalInSeconds(seconds)
                               .repeatForever()
                               .withMisfireHandlingInstructionNextWithExistingCount();
    }

    @Override
    public void execute(JobExecutionContext context) {
        tenantContext.executeForEachTenant(() -> {
            String tenantId = tenantContext.getCurrentTenant()
                                           .getId();
            try {
                relay.relay(tenantId);
            } catch (Exception ex) {
                LOGGER.error("Webhook relay failed for tenant [{}]; other tenants are unaffected.", tenantId, ex);
            }
            return null;
        });
    }
}
