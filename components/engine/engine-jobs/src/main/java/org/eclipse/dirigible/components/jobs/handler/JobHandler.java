/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.handler;

import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The built-in scripting service job handler.
 */
public class JobHandler implements Job {

    public static final String TENANT_PARAMETER = "tenant-id";

    @Autowired
    private JobExecutionService jobExecutionService;

    @Autowired
    private TenantContext tenantContext;

    /**
     * Execute.
     *
     * @param context the context
     * @throws JobExecutionException the job execution exception
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap params = context.getJobDetail()
                                   .getJobDataMap();
        Tenant tenant = (Tenant) params.get(TENANT_PARAMETER);
        if (tenant == null) {
            throw new MissingTenantException(
                    "Missing tenant parameter with key [" + TENANT_PARAMETER + "] for job with details: " + context.getJobDetail());
        }

        try {
            tenantContext.execute(tenant, () -> {
                jobExecutionService.executeJob(context);
                return null;
            });
        } catch (RuntimeException ex) {
            throw new JobExecutionException("Failed to execute job with details " + context.getJobDetail(), ex);
        }
    }

}
