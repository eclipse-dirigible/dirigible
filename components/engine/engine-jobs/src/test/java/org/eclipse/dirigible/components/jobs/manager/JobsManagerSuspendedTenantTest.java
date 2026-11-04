/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.manager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.jobs.domain.Job;
import org.eclipse.dirigible.components.jobs.handler.JavaJobExecutor;
import org.eclipse.dirigible.components.jobs.handler.JobHandlerRunner;
import org.eclipse.dirigible.components.jobs.tenant.JobNameCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.Trigger;

/**
 * A suspended tenant is maintained by the synchronizers, so its jobs are scheduled - and paused at
 * once, so nothing of it runs until it is resumed (#8477). A running tenant's job is never paused.
 */
class JobsManagerSuspendedTenantTest {

    private final Scheduler scheduler = mock(Scheduler.class);
    private final TenantContext tenantContext = mock(TenantContext.class);
    private final Tenant acme = mock(Tenant.class);
    private JobsManager jobsManager;

    @BeforeEach
    void setUp() {
        when(acme.getId()).thenReturn("acme");
        when(tenantContext.getCurrentTenant()).thenReturn(acme);
        JobNameCreator jobNameCreator = mock(JobNameCreator.class);
        when(jobNameCreator.toTenantName(anyString())).thenAnswer(invocation -> "acme###" + invocation.getArgument(0));
        jobsManager = new JobsManager(scheduler, tenantContext, jobNameCreator, mock(JobHandlerRunner.class));
    }

    @Test
    void aSuspendedTenantsNewJobIsPausedOnceScheduled() throws Exception {
        when(tenantContext.isSuspended("acme")).thenReturn(true);

        jobsManager.scheduleJob(job());

        InOrder order = inOrder(scheduler);
        order.verify(scheduler)
             .scheduleJob(any(JobDetail.class), any(Trigger.class));
        order.verify(scheduler)
             .pauseJob(new JobKey("acme###probe", "defined"));
    }

    @Test
    void aSuspendedTenantsExistingJobIsPausedToo() throws Exception {
        when(tenantContext.isSuspended("acme")).thenReturn(true);
        when(scheduler.checkExists(any(JobKey.class))).thenReturn(true);
        when(scheduler.checkExists(any(org.quartz.TriggerKey.class))).thenReturn(true);

        jobsManager.scheduleJob(job());

        verify(scheduler, never()).scheduleJob(any(JobDetail.class), any(Trigger.class));
        verify(scheduler).pauseJob(new JobKey("acme###probe", "defined"));
    }

    @Test
    void aRunningTenantsJobIsNotPaused() throws Exception {
        jobsManager.scheduleJob(job());

        verify(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
        verify(scheduler, never()).pauseJob(any());
    }

    private static Job job() {
        return new Job("/project/probe.job", "probe", "", Set.of(), "defined", "", "0 0 * * * ?", "app.jobs.CleanupJob",
                JavaJobExecutor.ENGINE_JAVA, false, true, null, null, null);
    }
}
