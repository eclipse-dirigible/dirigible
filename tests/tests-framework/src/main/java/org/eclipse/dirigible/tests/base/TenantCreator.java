/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.tests.base;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.components.tenants.domain.Tenant;
import org.eclipse.dirigible.components.tenants.domain.TenantStatus;
import org.eclipse.dirigible.components.tenants.service.TenantService;
import org.eclipse.dirigible.components.tenants.service.UserService;
import org.eclipse.dirigible.tests.framework.tenant.DirigibleTestTenant;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class TenantCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantCreator.class);

    /** The Quartz key the platform registers its tenant provisioning system job under. */
    private static final JobKey PROVISIONING_JOB = JobKey.jobKey("TenantsProvisioningJob", "system");

    private final TenantService tenantService;
    private final UserService userService;
    private final Scheduler scheduler;

    TenantCreator(TenantService tenantService, UserService userService, Scheduler scheduler) {
        this.tenantService = tenantService;
        this.userService = userService;
        this.scheduler = scheduler;
    }

    public void createTenant(DirigibleTestTenant tenant) {
        if (tenant.isDefaultTenant()) {
            LOGGER.info("Tenant [{}] is the default and creation is not needed.", tenant);
            return;
        }
        createTenantEntity(tenant);
        userService.createNewUser(tenant.getUsername(), tenant.getPassword(), tenant.getId());

        LOGGER.info("Created tenant [{}]", tenant);
    }

    private Tenant createTenantEntity(DirigibleTestTenant tenant) {
        Tenant tenantEntity = new Tenant();
        tenantEntity.setId(tenant.getId());
        tenantEntity.setName(tenant.getName());
        tenantEntity.setSubdomain(tenant.getSubdomain());
        tenantEntity.setStatus(TenantStatus.INITIAL);
        tenantEntity.setLocation("-");
        tenantEntity.setType(org.eclipse.dirigible.components.tenants.domain.Tenant.ARTEFACT_TYPE);
        tenantEntity.updateKey();

        return tenantService.save(tenantEntity);
    }

    /**
     * Fires the platform's tenant provisioning job now, instead of waiting for its schedule.
     *
     * <p>
     * {@link #createTenant} only stores the tenant in status {@code INITIAL}; the transition to
     * {@code PROVISIONED} happens exclusively when the Quartz {@code TenantsProvisioningJob} runs, and
     * its schedule is {@code DIRIGIBLE_TENANTS_PROVISIONING_FREQUENCY_SECONDS} wide (15 minutes by
     * default). The only prompt firing is the one at scheduler startup, so a test that merely waited
     * would be racing that boot firing: a tenant created after it could not be provisioned within any
     * sane test timeout.
     *
     * <p>
     * The job is registered on {@code ApplicationReadyEvent}; the short wait for its key covers a call
     * made before that listener has run. The provisioner picks up every {@code INITIAL} tenant in one
     * pass and is synchronized, so a trigger that lands while the boot firing is still running simply
     * queues behind it and provisions whatever that run did not see.
     */
    public void triggerTenantsProvisioning() {
        try {
            Awaitility.await()
                      .pollInterval(500, TimeUnit.MILLISECONDS)
                      .atMost(30, TimeUnit.SECONDS)
                      .until(() -> scheduler.checkExists(PROVISIONING_JOB));
            scheduler.triggerJob(PROVISIONING_JOB);
            LOGGER.info("Triggered tenants provisioning job [{}]", PROVISIONING_JOB);
        } catch (SchedulerException ex) {
            throw new IllegalStateException("Failed to trigger tenants provisioning job [" + PROVISIONING_JOB + "]", ex);
        }
    }

    public boolean isTenantProvisioned(DirigibleTestTenant tenant) {
        Tenant foundTenant = tenantService.findById(tenant.getId())
                                          .orElseThrow(() -> new IllegalStateException("Tenant [" + tenant + "] doesn't exist"));

        boolean tenantProvisioned = foundTenant.getStatus() == TenantStatus.PROVISIONED;
        LOGGER.info("Tenant [{}] provisioned [{}]", foundTenant, tenantProvisioned);
        return tenantProvisioned;
    }
}
