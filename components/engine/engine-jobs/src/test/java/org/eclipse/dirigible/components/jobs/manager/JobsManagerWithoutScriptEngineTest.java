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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.eclipse.dirigible.components.base.script.ScriptEngineUnavailableException;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.jobs.domain.Job;
import org.eclipse.dirigible.components.jobs.handler.JavaJobExecutor;
import org.eclipse.dirigible.components.jobs.handler.JobHandlerRunner;
import org.eclipse.dirigible.components.jobs.tenant.JobNameCreator;
import org.junit.jupiter.api.Test;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * An instance without the JavaScript engine (#8006): the job beans start, a script job is refused
 * before it reaches the scheduler - which is what the job synchronizer records as {@code FAILED},
 * with this reason - and a Java job still schedules.
 */
class JobsManagerWithoutScriptEngineTest {

    private final Scheduler scheduler = mock(Scheduler.class);

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withBean(Scheduler.class, () -> scheduler)
                                                                                         .withBean(TenantContext.class,
                                                                                                 JobsManagerWithoutScriptEngineTest::tenantContext)
                                                                                         .withBean(JobNameCreator.class,
                                                                                                 JobsManagerWithoutScriptEngineTest::jobNameCreator)
                                                                                         .withUserConfiguration(JobHandlerRunner.class,
                                                                                                 JobsManager.class);

    @Test
    void aScriptJobIsRefusedBeforeItReachesTheScheduler() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            JobsManager jobsManager = context.getBean(JobsManager.class);

            assertThatThrownBy(() -> jobsManager.scheduleJob(job("project/handler.js", null))).isInstanceOf(
                    ScriptEngineUnavailableException.class)
                                                                                              .hasMessageContaining("project/handler.js")
                                                                                              .hasMessageContaining("JavaScript engine");
            verifyNoInteractions(scheduler);
        });
    }

    @Test
    void aJavaJobIsScheduled() {
        contextRunner.run(context -> {
            context.getBean(JobsManager.class)
                   .scheduleJob(job("app.jobs.CleanupJob", JavaJobExecutor.ENGINE_JAVA));

            verify(scheduler).scheduleJob(any(JobDetail.class), any(Trigger.class));
        });
    }

    private static TenantContext tenantContext() {
        Tenant defaultTenant = mock(Tenant.class);
        when(defaultTenant.isDefault()).thenReturn(true);
        TenantContext tenantContext = mock(TenantContext.class);
        when(tenantContext.getCurrentTenant()).thenReturn(defaultTenant);
        return tenantContext;
    }

    private static JobNameCreator jobNameCreator() {
        JobNameCreator jobNameCreator = mock(JobNameCreator.class);
        when(jobNameCreator.toTenantName(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        return jobNameCreator;
    }

    private static Job job(String handler, String engine) {
        return new Job("/project/probe.job", "probe", "", Set.of(), "defined", "", "0 0 * * * ?", handler, engine, false, true, null, null,
                null);
    }
}
