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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

/**
 * Suspension pauses the tenant's jobs and resumes them - only the tenant's: not another tenant's
 * (one whose id the suspended one is a prefix of included), and never the default tenant's. Nothing
 * is deleted.
 */
class JobsTenantSuspensionTest {

    private final Scheduler scheduler = mock(Scheduler.class);
    private final JobsTenantSuspension suspension = new JobsTenantSuspension(scheduler);

    private final JobKey acme = new JobKey("acme###nightly", "defined");
    private final JobKey acmeTwin = new JobKey("acme-twin###nightly", "defined");
    private final JobKey defaultTenant = new JobKey("nightly", "defined");

    @BeforeEach
    void setUp() throws SchedulerException {
        when(scheduler.getJobKeys(any())).thenReturn(Set.of(acme, acmeTwin, defaultTenant));
    }

    @Test
    void suspendPausesOnlyTheTenantsJobs() throws SchedulerException {
        suspension.suspend("acme");

        verify(scheduler).pauseJob(acme);
        verify(scheduler, never()).pauseJob(acmeTwin);
        verify(scheduler, never()).pauseJob(defaultTenant);
        verify(scheduler, never()).deleteJob(any());
    }

    @Test
    void resumeResumesOnlyTheTenantsJobs() throws SchedulerException {
        suspension.resume("acme");

        verify(scheduler).resumeJob(acme);
        verify(scheduler, never()).resumeJob(acmeTwin);
        verify(scheduler, never()).resumeJob(defaultTenant);
    }
}
