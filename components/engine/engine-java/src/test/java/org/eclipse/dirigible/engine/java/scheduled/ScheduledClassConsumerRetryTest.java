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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
 * A registration that threw has to stay retryable, and a retry has to touch <b>only</b> what is
 * still outstanding.
 *
 * <p>
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

    /** A second, independent job - the healthy one whose registration a retry must leave alone. */
    static class CleanupJob {

        @Scheduled(expression = "0 0 4 * * ?")
        public void purge() {
            // The registration, not the run, is what this test is about.
        }
    }

    private static final String DEFAULT_TENANT_ID = "default-tenant";
    private static final String JOB_NAME = ReportsJob.class.getName() + ".sendDaily";
    private static final String CLEANUP_JOB_NAME = CleanupJob.class.getName() + ".purge";

    private final Map<String, Tenant> provisionedTenants = new LinkedHashMap<>();

    private final AtomicReference<String> currentTenantId = new AtomicReference<>();

    private final List<String> scheduledForTenants = new ArrayList<>();

    private final Map<String, Map<String, Job>> rowsByTenant = new LinkedHashMap<>();

    /** While set, scheduling fails the way an unavailable scheduler or tenant database fails. */
    private final AtomicBoolean schedulerRefusing = new AtomicBoolean();

    /** The jobs the scheduler refuses by name - one broken job beside the healthy ones. */
    private final Set<String> refusedJobs = new LinkedHashSet<>();

    /** The job names actually written, in order - the observable for "what did this pass rewrite". */
    private final List<String> savedNames = new ArrayList<>();

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
        when(componentContainer.instanceOf(CleanupJob.class)).thenReturn(Optional.of(new CleanupJob()));

        jobsManager = mock(JobsManager.class);
        doAnswer(invocation -> {
            Job job = invocation.getArgument(0);
            if (schedulerRefusing.get() || refusedJobs.contains(job.getName())) {
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
            savedNames.add(job.getName());
            rows().put(job.getName(), job);
            return job;
        });

        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getCurrentTenant()).thenAnswer(invocation -> provisionedTenants.get(currentTenantId.get()));
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
     * The cost the retry timer must not multiply: while ONE job is refused, a pass has to re-save and
     * reschedule that job alone. Covering the healthy ones too would mean a {@code saveAndFlush} + a
     * Quartz reschedule + an INFO line per job per tenant every 30 s for the life of the failure - from
     * every node of the cluster, onto the same shared rows (#7265). The INFO line comes with the
     * successful registration, so a job that is not re-saved is not re-logged either.
     */
    @Test
    void aRetryPassRewritesOnlyTheJobThatIsStillFailing() {
        refusedJobs.add(JOB_NAME);
        loadJob();
        loadCleanupJob();
        assertEquals(List.of(DEFAULT_TENANT_ID, "acme"), scheduledForTenants, "the healthy job registers in every tenant at load");
        savedNames.clear();
        scheduledForTenants.clear();

        consumer.reconcile();

        assertEquals(List.of(JOB_NAME, JOB_NAME), savedNames, "only the refused job may be rewritten, once per tenant");
        assertTrue(scheduledForTenants.isEmpty(), "the refused job still fails, and the healthy one is not touched at all");
    }

    /** And once the refusal clears, the outstanding job - and only it - lands. */
    @Test
    void theJobThatWasFailingLandsOnTheFirstPassAfterTheRefusalClears() {
        refusedJobs.add(JOB_NAME);
        loadJob();
        loadCleanupJob();
        savedNames.clear();
        scheduledForTenants.clear();

        refusedJobs.clear();
        consumer.reconcile();

        assertEquals(List.of(JOB_NAME, JOB_NAME), savedNames, "the healthy job was already registered everywhere");
        assertEquals(List.of(DEFAULT_TENANT_ID, "acme"), scheduledForTenants);

        savedNames.clear();
        consumer.reconcile();
        assertTrue(savedNames.isEmpty(), "nothing is outstanding any more, so the next tick must do no work at all");
    }

    /**
     * A tenant that refuses must not abort the fan-out: {@code executeForEachTenant} propagates the
     * first throw, which would leave the tenants behind the failing one unregistered.
     */
    @Test
    void aTenantThatRefusesDoesNotStopTheTenantsBehindIt() {
        provisionedTenants.clear();
        addTenant(DEFAULT_TENANT_ID);
        addTenant("acme");
        addTenant("beta");
        // The middle tenant's database is the one that is unavailable. doAnswer, not when(): re-stubbing
        // with when() would run the answer already in place against a null argument.
        doAnswer(invocation -> {
            Job job = invocation.getArgument(0);
            if ("acme".equals(currentTenantId.get())) {
                throw new IllegalStateException("The tenant's database is not available");
            }
            savedNames.add(job.getName());
            rows().put(job.getName(), job);
            return job;
        }).when(jobService)
          .save(any());

        loadJob();

        assertEquals(List.of(DEFAULT_TENANT_ID, "beta"), scheduledForTenants, "one tenant's refusal is confined to that tenant");
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

    private void loadCleanupJob() {
        consumer.onClassLoaded(new LoadedClass("sample", CleanupJob.class.getName(), CleanupJob.class, CleanupJob.class.getClassLoader()));
    }
}
