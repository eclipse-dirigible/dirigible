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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.jobs.domain.Job;
import org.eclipse.dirigible.components.jobs.domain.JobParameter;
import org.eclipse.dirigible.components.jobs.email.JobEmailProcessor;
import org.eclipse.dirigible.components.jobs.handler.JobHandlerRunner;
import org.eclipse.dirigible.components.jobs.manager.JobsManager;
import org.eclipse.dirigible.components.jobs.repository.JobRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * What a trigger-now may do to the platform configuration (dirigible #6729). The parameters reach
 * the job body as configuration values, but only the ones the artefact declares, and only for the
 * thread that runs the handler.
 */
class JobTriggerParametersTest {

    /** The job's name as the repository holds it - the trigger path carries a leading separator. */
    private static final String JOB_NAME = "project/report.job";

    /** The path the Jobs perspective posts to. */
    private static final String TRIGGER_PATH = "/" + JOB_NAME;

    /** A key the caller must never be able to redefine through a trigger. */
    private static final String INFRASTRUCTURE_KEY = "DIRIGIBLE_TEST_INFRASTRUCTURE_KEY";

    private final JobRepository jobRepository = mock(JobRepository.class);

    private final JobHandlerRunner jobHandlerRunner = mock(JobHandlerRunner.class);

    private final JobService jobService =
            new JobService(jobRepository, mock(JobEmailProcessor.class), mock(JobsManager.class), jobHandlerRunner);

    @AfterEach
    void cleanUpConfiguration() {
        Configuration.removeThreadConfiguration();
        Configuration.remove(INFRASTRUCTURE_KEY);
    }

    @Test
    void aDeclaredParameterIsVisibleToTheHandlerAndGoneAfterwards() throws Exception {
        registerJob(job(JOB_NAME, "REPORT_DATE"));
        doAnswer(invocation -> {
            assertEquals("2026-08-14", Configuration.get("REPORT_DATE"), "the job body reads its parameter as a configuration value");
            return null;
        }).when(jobHandlerRunner)
          .run("/project/report.mjs", null);

        jobService.trigger(TRIGGER_PATH, Map.of("REPORT_DATE", "2026-08-14"));

        verify(jobHandlerRunner).run("/project/report.mjs", null);
        assertNull(Configuration.get("REPORT_DATE"), "the parameter must not outlive the run");
    }

    @Test
    void anUndeclaredParameterIsRejectedAndTheJobNeverRuns() {
        registerJob(job(JOB_NAME, "REPORT_DATE"));

        assertThrows(IllegalArgumentException.class,
                () -> jobService.trigger(TRIGGER_PATH, Map.of(INFRASTRUCTURE_KEY, "http://attacker.example.com")));

        assertNull(Configuration.get(INFRASTRUCTURE_KEY), "an undeclared key must never reach the configuration");
        verifyNoInteractions(jobHandlerRunner);
    }

    /**
     * The values used to go into the RUNTIME layer, which is process-global and outranks everything
     * else - so a trigger changed what every concurrent request read. They are thread-scoped now.
     */
    @Test
    void theParametersDoNotLeakOutOfTheTriggeringThread() throws Exception {
        registerJob(job(JOB_NAME, "REPORT_DATE"));
        AtomicReference<String> seenByAnotherThread = new AtomicReference<>("not read");
        doAnswer(invocation -> {
            Thread reader = new Thread(() -> seenByAnotherThread.set(Configuration.get("REPORT_DATE")));
            reader.start();
            reader.join();
            return null;
        }).when(jobHandlerRunner)
          .run("/project/report.mjs", null);

        jobService.trigger(TRIGGER_PATH, Map.of("REPORT_DATE", "2026-08-14"));

        assertNull(seenByAnotherThread.get(), "a concurrent request must not see the parameter");
    }

    /**
     * The thread configuration also carries the tenant's own overrides for the duration of the request,
     * so the trigger has to add to it rather than replace it.
     */
    @Test
    void theSurroundingThreadConfigurationSurvivesTheRun() throws Exception {
        registerJob(job(JOB_NAME, "REPORT_DATE"));
        Configuration.setThreadConfiguration(Map.of("DIRIGIBLE_BRANDING_NAME", "Tenant One"));
        doAnswer(invocation -> {
            assertEquals("Tenant One", Configuration.get("DIRIGIBLE_BRANDING_NAME"));
            return null;
        }).when(jobHandlerRunner)
          .run("/project/report.mjs", null);

        jobService.trigger(TRIGGER_PATH, Map.of("REPORT_DATE", "2026-08-14"));

        assertEquals(Map.of("DIRIGIBLE_BRANDING_NAME", "Tenant One"), Configuration.getThreadConfiguration());
    }

    private void registerJob(Job job) {
        when(jobRepository.findByName(job.getName())).thenReturn(Optional.of(job));
    }

    private static Job job(String name, String... declaredParameters) {
        Job job = new Job();
        job.setName(name);
        job.setHandler("/project/report.mjs");
        job.setParameters(Arrays.stream(declaredParameters)
                                .map(parameterName -> new JobParameter(parameterName, "string", "", "", null, job))
                                .toList());
        return job;
    }
}
