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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
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
     * Verifies that reading and clearing job logs are scoped to the current tenant (regression for
     * #6606). Logs created under one tenant must never be visible to, or clearable by, another tenant.
     */
    @Test
    public void readsAndClearsAreTenantScoped() {
        Tenant defaultTenant = mock(Tenant.class);
        when(defaultTenant.getId()).thenReturn("default-tenant");
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.isNotInitialized()).thenReturn(false);
        Tenant tenantA = mock(Tenant.class);
        when(tenantA.getId()).thenReturn("tenant-a");
        Tenant tenantB = mock(Tenant.class);
        when(tenantB.getId()).thenReturn("tenant-b");
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
}
