/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.jobs.domain.Job;
import org.eclipse.dirigible.components.jobs.domain.JobLog;
import org.eclipse.dirigible.components.jobs.domain.JobStatus;
import org.eclipse.dirigible.components.jobs.email.JobEmailProcessor;
import org.eclipse.dirigible.components.jobs.repository.JobLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Class JobLogServiceTest.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ComponentScan(basePackages = {"org.eclipse.dirigible.components"})
@EntityScan("org.eclipse.dirigible.components")
@Transactional
public class JobLogServiceTest {

    /** The default tenant id used across the tests. */
    private static final String DEFAULT_TENANT_ID = "default-tenant";

    /** The job log repository. */
    @Autowired
    private JobLogRepository jobLogRepository;

    /**
     * The Class TestConfiguration.
     */
    @SpringBootApplication
    static class TestConfiguration {
    }

    /**
     * Setup.
     */
    @BeforeEach
    public void setup() {
        cleanup();
    }

    /**
     * Cleanup.
     */
    @AfterEach
    public void cleanup() {
        jobLogRepository.deleteAll();
    }

    /**
     * Verifies that jobTriggered persists a TRIGGRED log stamped with the current tenant and populates
     * its core fields.
     */
    @Test
    public void jobTriggeredPersistsTriggeredLog() {
        JobLogService service = serviceForTenant("tenant-a");

        JobLog log = service.jobTriggered("nightly", "handler.js");

        assertEquals(JobStatus.TRIGGRED, log.getStatus());
        assertEquals("nightly", log.getJobName());
        assertEquals("handler.js", log.getHandler());
        assertEquals("tenant-a", log.getTenantId());
        assertNotNull(log.getTriggeredAt());

        assertEquals(1, service.findByJob("nightly")
                               .size());
    }

    /**
     * Verifies each logging method persists a log carrying its corresponding status.
     */
    @Test
    public void logMethodsSetTheirStatus() {
        JobLogService service = serviceForTenant("tenant-a");

        assertEquals(JobStatus.LOGGED, service.jobLogged("job", "handler.js", "msg")
                                              .getStatus());
        assertEquals(JobStatus.ERROR, service.jobLoggedError("job", "handler.js", "msg")
                                             .getStatus());
        assertEquals(JobStatus.WARN, service.jobLoggedWarning("job", "handler.js", "msg")
                                            .getStatus());
        assertEquals(JobStatus.INFO, service.jobLoggedInfo("job", "handler.js", "msg")
                                            .getStatus());

        assertEquals(4, service.findByJob("job")
                               .size());
    }

    /**
     * Verifies findByJob strips a leading slash from the job name before matching.
     */
    @Test
    public void findByJobNormalizesLeadingSlash() {
        JobLogService service = serviceForTenant("tenant-a");
        service.jobLogged("myJob", "handler.js", "msg");

        assertEquals(1, service.findByJob("/myJob")
                               .size());
    }

    /**
     * Verifies deleteAllByJobName strips a leading slash from the job name before matching.
     */
    @Test
    public void deleteAllByJobNameNormalizesLeadingSlash() {
        JobLogService service = serviceForTenant("tenant-a");
        service.jobLogged("myJob", "handler.js", "msg1");
        service.jobLogged("myJob", "handler.js", "msg2");

        service.deleteAllByJobName("/myJob");

        assertEquals(0, service.findByJob("myJob")
                               .size());
    }

    /**
     * Verifies that logs created while the tenant context is not yet initialized fall back to the
     * default tenant.
     */
    @Test
    public void logsFallBackToDefaultTenantWhenContextUninitialized() {
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.isNotInitialized()).thenReturn(true);
        JobLogService service = new JobLogService(jobLogRepository, mock(JobEmailProcessor.class), mock(JobService.class), tenantContext,
                mockTenant(DEFAULT_TENANT_ID));

        JobLog log = service.jobLogged("job", "handler.js", "msg");

        assertEquals(DEFAULT_TENANT_ID, log.getTenantId());
        assertEquals(1, service.findByJob("job")
                               .size());
    }

    /**
     * Verifies jobFinished persists a FINISHED log and flips the owning job to FINISHED.
     */
    @Test
    public void jobFinishedPersistsFinishedLogAndUpdatesJob() {
        Job job = mock(Job.class);
        when(job.getStatus()).thenReturn(JobStatus.TRIGGRED);
        JobService jobService = mock(JobService.class);
        when(jobService.findByName("job")).thenReturn(job);
        JobLogService service = new JobLogService(jobLogRepository, mock(JobEmailProcessor.class), jobService,
                initializedContext("tenant-a"), mockTenant(DEFAULT_TENANT_ID));

        JobLog log = service.jobFinished("job", "handler.js", 5L, new Date());

        assertEquals(JobStatus.FINISHED, log.getStatus());
        assertEquals(5L, log.getTriggeredId());
        assertNotNull(log.getFinishedAt());
        verify(job).setStatus(JobStatus.FINISHED);
        assertEquals(1, service.findByJob("job")
                               .size());
    }

    /**
     * Verifies jobFailed persists a FAILED log carrying the failure message and flips the owning job to
     * FAILED.
     */
    @Test
    public void jobFailedPersistsFailedLogAndUpdatesJob() {
        Job job = mock(Job.class);
        when(job.getStatus()).thenReturn(JobStatus.TRIGGRED);
        JobService jobService = mock(JobService.class);
        when(jobService.findByName("job")).thenReturn(job);
        JobLogService service = new JobLogService(jobLogRepository, mock(JobEmailProcessor.class), jobService,
                initializedContext("tenant-a"), mockTenant(DEFAULT_TENANT_ID));

        JobLog log = service.jobFailed("job", "handler.js", 7L, new Date(), "boom");

        assertEquals(JobStatus.FAILED, log.getStatus());
        assertEquals("boom", log.getMessage());
        assertEquals(7L, log.getTriggeredId());
        assertNotNull(log.getFinishedAt());
        verify(job).setStatus(JobStatus.FAILED);
        assertEquals(1, service.findByJob("job")
                               .size());
    }

    /**
     * Verifies that reading and clearing job logs are scoped to the current tenant (regression for
     * #6606). Logs created under one tenant must never be visible to, or clearable by, another tenant.
     */
    @Test
    public void readsAndClearsAreTenantScoped() {
        Tenant defaultTenant = mockTenant(DEFAULT_TENANT_ID);
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.isNotInitialized()).thenReturn(false);
        Tenant tenantA = mockTenant("tenant-a");
        Tenant tenantB = mockTenant("tenant-b");
        JobLogService service =
                new JobLogService(jobLogRepository, mock(JobEmailProcessor.class), mock(JobService.class), tenantContext, defaultTenant);

        // two logs for the same job under tenant A
        when(tenantContext.getCurrentTenant()).thenReturn(tenantA);
        service.jobLogged("sharedJob", "handler.js", "message A1");
        service.jobLogged("sharedJob", "handler.js", "message A2");

        // one log for the same job under tenant B
        when(tenantContext.getCurrentTenant()).thenReturn(tenantB);
        service.jobLogged("sharedJob", "handler.js", "message B1");

        // each tenant sees only its own logs for the shared job
        assertEquals(1, service.findByJob("sharedJob")
                               .size());
        when(tenantContext.getCurrentTenant()).thenReturn(tenantA);
        assertEquals(2, service.findByJob("sharedJob")
                               .size());

        // clearing under tenant B removes only tenant B's log
        when(tenantContext.getCurrentTenant()).thenReturn(tenantB);
        service.deleteAllByJobName("sharedJob");
        assertEquals(0, service.findByJob("sharedJob")
                               .size());
        when(tenantContext.getCurrentTenant()).thenReturn(tenantA);
        assertEquals(2, service.findByJob("sharedJob")
                               .size());
    }

    /**
     * Builds a job log service whose tenant context is initialized and resolves to the given tenant.
     *
     * @param tenantId the current tenant id
     * @return the job log service
     */
    private JobLogService serviceForTenant(String tenantId) {
        return new JobLogService(jobLogRepository, mock(JobEmailProcessor.class), mock(JobService.class), initializedContext(tenantId),
                mockTenant(DEFAULT_TENANT_ID));
    }

    /**
     * Mocks an initialized tenant context resolving to the given tenant.
     *
     * @param tenantId the current tenant id
     * @return the tenant context
     */
    private static TenantContext initializedContext(String tenantId) {
        Tenant tenant = mockTenant(tenantId);
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.isNotInitialized()).thenReturn(false);
        when(tenantContext.getCurrentTenant()).thenReturn(tenant);
        return tenantContext;
    }

    /**
     * Mocks a tenant with the given id.
     *
     * @param id the tenant id
     * @return the tenant
     */
    private static Tenant mockTenant(String id) {
        Tenant tenant = mock(Tenant.class);
        when(tenant.getId()).thenReturn(id);
        return tenant;
    }
}
