/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.engine.java.scheduled;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.eclipse.dirigible.components.base.callable.CallableResultAndException;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.jobs.domain.Job;
import org.eclipse.dirigible.components.jobs.manager.JobsManager;
import org.eclipse.dirigible.components.jobs.service.JobService;
import org.eclipse.dirigible.engine.java.component.ComponentContainer;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.eclipse.dirigible.sdk.job.JobHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Registering a client-Java job keeps the operator's enable/disable choice (#6626): the flag lives
 * on the {@code Job} row, and the row is re-registered on every class load - at every server start,
 * and on every client-Java rebuild.
 */
class ScheduledClassConsumerTest {

    /** A client job of the self-describing-interface style. */
    static class CleanupJob implements JobHandler {

        @Override
        public String cron() {
            return "0 0 3 * * ?";
        }

        @Override
        public void run() {
            // nothing - the schedule registration is what is under test
        }
    }

    private static final String JOB_NAME = CleanupJob.class.getName();

    private ComponentContainer componentContainer;
    private JobsManager jobsManager;
    private JobService jobService;
    private ScheduledClassConsumer consumer;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() throws Exception {
        componentContainer = mock(ComponentContainer.class);
        jobsManager = mock(JobsManager.class);
        jobService = mock(JobService.class);
        TenantContext tenantContext = mock(TenantContext.class);
        // Registration runs per tenant; run the callable once so the test sees what it writes.
        when(tenantContext.executeForEachTenant(any())).thenAnswer(invocation -> {
            ((CallableResultAndException) invocation.getArgument(0)).call();
            return List.of();
        });
        when(componentContainer.instanceOf(CleanupJob.class)).thenReturn(Optional.of(new CleanupJob()));

        consumer = new ScheduledClassConsumer(componentContainer, jobsManager, jobService, tenantContext);
    }

    private static LoadedClass loadedClass() {
        return new LoadedClass("sample", JOB_NAME, CleanupJob.class, CleanupJob.class.getClassLoader());
    }

    private Job savedJob() {
        ArgumentCaptor<Job> saved = ArgumentCaptor.forClass(Job.class);
        verify(jobService, times(1)).save(saved.capture());
        return saved.getValue();
    }

    @Test
    void aNewJobStartsEnabled() {
        // findByName THROWS when the row is absent - that is what "brand new" looks like here.
        when(jobService.findByName(JOB_NAME)).thenThrow(new IllegalArgumentException("Job with name does not exist: " + JOB_NAME));

        consumer.onClassLoaded(loadedClass());

        assertTrue(savedJob().isEnabled(), "a job registered for the first time should be scheduled");
    }

    @Test
    void aDisabledJobStaysDisabledWhenItIsRegisteredAgain() {
        Job existing = new Job();
        existing.setName(JOB_NAME);
        existing.setEnabled(false);
        when(jobService.findByName(JOB_NAME)).thenReturn(existing);

        consumer.onClassLoaded(loadedClass());

        // The flag is the operator's, not the code's: a restart or an unrelated client-Java rebuild
        // must not turn a job the operator switched off back on.
        assertFalse(savedJob().isEnabled(), "the operator's disable must survive re-registration");
    }

    @Test
    void aReloadUpdatesTheExistingRowInsteadOfDeletingIt() throws Exception {
        Job existing = new Job();
        existing.setName(JOB_NAME);
        existing.setEnabled(false);
        when(jobService.findByName(JOB_NAME)).thenReturn(existing);

        consumer.onClassLoaded(loadedClass());
        consumer.onClassLoaded(loadedClass()); // the hot reload

        // Deleting and recreating the row would drop the flag with it, so the row must survive.
        verify(jobService, never()).delete(any());
        verify(jobsManager, never()).unscheduleJob(anyString(), anyString());
    }

    @Test
    void anUnloadedClassLosesItsJob() throws Exception {
        Job existing = new Job();
        existing.setName(JOB_NAME);
        when(jobService.findByName(JOB_NAME)).thenReturn(existing);

        consumer.onClassLoaded(loadedClass());
        consumer.onClassUnloaded(loadedClass());

        verify(jobsManager).unscheduleJob(JOB_NAME, "defined");
        verify(jobService).delete(existing);
    }
}
