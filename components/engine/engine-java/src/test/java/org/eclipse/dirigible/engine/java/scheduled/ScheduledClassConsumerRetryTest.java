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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.components.base.callable.CallableResultAndException;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.jobs.domain.Job;
import org.eclipse.dirigible.components.jobs.manager.JobsManager;
import org.eclipse.dirigible.components.jobs.service.JobService;
import org.eclipse.dirigible.engine.java.component.ComponentContainer;
import org.eclipse.dirigible.engine.java.spi.LoadedClass;
import org.eclipse.dirigible.sdk.job.Scheduled;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A registration that threw has to stay retryable. The consumer used to record only the
 * declarations that registered successfully, which cost twice: the failed job was dropped from its
 * tracking, so even the post-provisioning top-up could not bring it back, and the stale-name sweep
 * then compared the PREVIOUS names against the successful ones - so a transient failure on reload
 * deleted the {@code Job} row an earlier run had created, taking the operator's enabled flag with
 * it.
 *
 * <p>
 * The sibling of the listener defect in issue #7217, and repaired by the same reconciliation timer.
 */
class ScheduledClassConsumerRetryTest {

    /** A method-level job, so the assertions also cover the {@code <fqn>#<method>} handler shape. */
    static class ReportsJob {

        @Scheduled(expression = "0 0 6 * * ?")
        public void sendDaily() {
            // The registration, not the run, is what this test is about.
        }
    }

    private static final String DEFAULT_TENANT_ID = "default-tenant";
    private static final String JOB_NAME = ReportsJob.class.getName() + ".sendDaily";

    private final Map<String, Tenant> provisionedTenants = new LinkedHashMap<>();

    private final AtomicReference<String> currentTenantId = new AtomicReference<>();

    private final List<String> scheduledForTenants = new ArrayList<>();

    private final Map<String, Map<String, Job>> rowsByTenant = new LinkedHashMap<>();

    /** While set, scheduling fails the way an unavailable scheduler or tenant database fails. */
    private final AtomicBoolean schedulerRefusing = new AtomicBoolean();

    private JobsManager jobsManager;
    private JobService jobService;
    private ScheduledClassConsumer consumer;

    @BeforeEach
    @SuppressWarnings("rawtypes")
    void setUp() throws Exception {
        addTenant(DEFAULT_TENANT_ID);
        addTenant("acme");

        ComponentContainer componentContainer = mock(ComponentContainer.class);
        when(componentContainer.instanceOf(ReportsJob.class)).thenReturn(Optional.of(new ReportsJob()));

        jobsManager = mock(JobsManager.class);
        doAnswer(invocation -> {
            if (schedulerRefusing.get()) {
                throw new IllegalStateException("The scheduler is not available");
            }
            scheduledForTenants.add(currentTenantId.get());
            return null;
        }).when(jobsManager)
          .scheduleJob(any());

        jobService = mock(JobService.class);
        when(jobService.findByName(anyString())).thenAnswer(invocation -> {
            String name = invocation.getArgument(0);
            Job row = rows().get(name);
            if (row == null) {
                throw new IllegalArgumentException("Job with name does not exist: " + name);
            }
            return row;
        });
        when(jobService.save(any())).thenAnswer(invocation -> {
            Job job = invocation.getArgument(0);
            rows().put(job.getName(), job);
            return job;
        });

        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.executeForEachTenant(any())).thenAnswer(invocation -> {
            for (String tenantId : new ArrayList<>(provisionedTenants.keySet())) {
                currentTenantId.set(tenantId);
                ((CallableResultAndException) invocation.getArgument(0)).call();
            }
            currentTenantId.set(null);
            return List.of();
        });

        consumer = new ScheduledClassConsumer(componentContainer, jobsManager, jobService, tenantContext);
    }

    @Test
    void aRegistrationThatFailedAtLoadIsRetriedByTheNextReconciliationPass() {
        schedulerRefusing.set(true);
        loadJob();
        assertTrue(scheduledForTenants.isEmpty(), "nothing can be scheduled while the scheduler refuses");

        schedulerRefusing.set(false);
        consumer.reconcile();

        assertEquals(List.of(DEFAULT_TENANT_ID, "acme"), scheduledForTenants);
    }

    @Test
    void aPassThatSucceededIsNotRepeatedByTheNextTick() {
        loadJob();
        scheduledForTenants.clear();

        consumer.reconcile();

        assertTrue(scheduledForTenants.isEmpty(), "nothing was outstanding, so the tick must do no work at all");
    }

    /**
     * The expensive half of the old behaviour: the row survives the failure. Deleting it would strand
     * the operator's enable/disable choice and, on the next successful pass, silently switch the job
     * back on.
     */
    @Test
    void aTransientFailureOnReloadDoesNotDeleteTheRowAnEarlierRunRegistered() throws Exception {
        loadJob();
        row("acme").setEnabled(false);

        schedulerRefusing.set(true);
        loadJob();

        verify(jobService, never()).delete(any());
        verify(jobsManager, never()).unscheduleJob(anyString(), anyString());
        assertFalse(row("acme").isEnabled(), "the operator's disable must survive a failed re-registration");
    }

    private Job row(String tenantId) {
        Job job = rowsByTenant.getOrDefault(tenantId, Map.of())
                              .get(JOB_NAME);
        assertNotNull(job, "tenant [" + tenantId + "] has no row for the client-Java job [" + JOB_NAME + "]");
        return job;
    }

    private Map<String, Job> rows() {
        return rowsByTenant.computeIfAbsent(currentTenantId.get(), tenantId -> new LinkedHashMap<>());
    }

    private void addTenant(String tenantId) {
        Tenant tenant = mock(Tenant.class);
        when(tenant.getId()).thenReturn(tenantId);
        when(tenant.isDefault()).thenReturn(DEFAULT_TENANT_ID.equals(tenantId));
        provisionedTenants.put(tenantId, tenant);
    }

    private void loadJob() {
        consumer.onClassLoaded(new LoadedClass("sample", ReportsJob.class.getName(), ReportsJob.class, ReportsJob.class.getClassLoader()));
    }
}
