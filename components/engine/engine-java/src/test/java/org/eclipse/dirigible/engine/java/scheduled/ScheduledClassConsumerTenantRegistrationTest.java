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
 * A client-Java job has to exist in EVERY tenant, including one provisioned after the class was
 * loaded. The fan-out runs at class-load time only, and a client-Java generation is JVM-wide and
 * rebuilt solely when the Java synchronizer goes dirty on a publish - so without the
 * post-provisioning top-up a tenant created later has no {@code Job} row and no Quartz trigger for
 * any client-Java job, and the Jobs perspective shows nothing to switch on.
 */
class ScheduledClassConsumerTenantRegistrationTest {

    /** A method-level job, so the assertions also cover the {@code <fqn>#<method>} handler shape. */
    static class ReportsJob {

        @Scheduled(expression = "0 0 6 * * ?")
        public void sendDaily() {
            // The registration, not the run, is what this test is about.
        }
    }

    private static final String DEFAULT_TENANT_ID = "default-tenant";
    private static final String JOB_NAME = ReportsJob.class.getName() + ".sendDaily";
    private static final String JOB_HANDLER = ReportsJob.class.getName() + "#sendDaily";

    /** The tenants {@code executeForEachTenant} fans out over; mutable, so a test can add one. */
    private final Map<String, Tenant> provisionedTenants = new LinkedHashMap<>();

    /** The tenant whose context the fan-out is currently simulating. */
    private final AtomicReference<String> currentTenantId = new AtomicReference<>();

    /** The tenants a job was actually scheduled for, in order. */
    private final List<String> scheduledForTenants = new ArrayList<>();

    /** The rows a tenant already has, keyed by tenant id then job name - the "DB" of this test. */
    private final Map<String, Map<String, Job>> rowsByTenant = new LinkedHashMap<>();

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
        // The Quartz job name is tenant-prefixed by JobsManager itself, so the tenant in scope at the
        // moment of scheduling IS the observable for "which tenants got this job".
        doAnswer(invocation -> {
            scheduledForTenants.add(currentTenantId.get());
            return null;
        }).when(jobsManager)
          .scheduleJob(any());

        jobService = mock(JobService.class);
        // Each tenant sees only its own rows, the way the tenant-routed datasource has it: findByName
        // THROWS when absent (it does not return null), which is what "brand new" looks like here.
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
        // Run the callable once per provisioned tenant, inline, with that tenant current.
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
    void aLoadedJobIsRegisteredInEveryProvisionedTenant() {
        loadJob();

        assertEquals(List.of(DEFAULT_TENANT_ID, "acme"), scheduledForTenants);
    }

    @Test
    void aTenantProvisionedAfterTheClassWasLoadedIsRegisteredByThePostProvisioningStep() {
        loadJob();
        scheduledForTenants.clear();

        addTenant("beta");
        consumer.execute();

        row("beta");
        assertEquals(List.of(DEFAULT_TENANT_ID, "acme", "beta"), scheduledForTenants,
                "the top-up re-registers every tenant; scheduling an already-scheduled job is a no-op");
    }

    @Test
    void theTopUpRegistersTheSameCronAndHandlerTheClassDeclared() {
        loadJob();

        addTenant("beta");
        consumer.execute();

        // Tracking only the job NAME would be enough to know WHAT to re-register but not HOW: the
        // cron expression and the handler have to be kept with it, or the late tenant gets a row
        // that never fires.
        Job lateRow = row("beta");
        assertEquals("0 0 6 * * ?", lateRow.getExpression());
        assertEquals(JOB_HANDLER, lateRow.getHandler());
    }

    @Test
    void theTopUpKeepsTheOperatorsDisableInTheTenantsThatAlreadyHadTheJob() throws Exception {
        loadJob();
        // The operator switches the job off in one tenant through the Jobs perspective.
        row("acme").setEnabled(false);

        addTenant("beta");
        consumer.execute();

        assertFalse(row("acme").isEnabled(), "topping up a late tenant must not switch the job back on in the others (#6626)");
        verify(jobService, never()).delete(any());
        verify(jobsManager, never()).unscheduleJob(anyString(), anyString());
    }

    /** The job row a tenant holds — the assertion subject, so its absence reads as the failure. */
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
