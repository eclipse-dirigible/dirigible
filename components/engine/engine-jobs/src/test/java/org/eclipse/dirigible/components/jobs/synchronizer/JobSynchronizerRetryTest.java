/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.synchronizer;

import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.jobs.domain.Job;
import org.eclipse.dirigible.components.jobs.manager.JobsManager;
import org.eclipse.dirigible.components.jobs.service.JobEmailService;
import org.eclipse.dirigible.components.jobs.service.JobLogService;
import org.eclipse.dirigible.components.jobs.service.JobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A job whose scheduling fails is recorded FAILED and retried on every pass until it schedules -
 * never CREATED while nothing runs, never promoted to FATAL (#7248).
 */
class JobSynchronizerRetryTest {

    /** What the scheduler answers while its store is not reachable yet. */
    private static final String SCHEDULER_REFUSAL = "Scheduler store not available";

    /** The synchronizer under test. */
    private JobSynchronizer synchronizer;

    /** The manager whose scheduling the tests make fail or succeed. */
    private JobsManager jobsManager;

    /**
     * Wires the synchronizer over a manager double and a callback that writes the registered state onto
     * the artefact, as the synchronization processor does.
     */
    @BeforeEach
    void setUp() {
        JobService jobService = mock(JobService.class);
        when(jobService.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        jobsManager = mock(JobsManager.class);
        synchronizer = new JobSynchronizer(jobService, jobsManager, mock(JobEmailService.class), mock(JobLogService.class));

        SynchronizerCallback callback = mock(SynchronizerCallback.class);
        doAnswer(invocation -> {
            synchronizer.setStatus(artefact(invocation.getArgument(1)), invocation.getArgument(2), "");
            return null;
        }).when(callback)
          .registerState(any(), any(TopologyWrapper.class), any(ArtefactLifecycle.class));
        doAnswer(invocation -> {
            Throwable cause = invocation.getArgument(3);
            synchronizer.setStatus(artefact(invocation.getArgument(1)), invocation.getArgument(2), cause.getMessage());
            return null;
        }).when(callback)
          .registerState(any(), any(TopologyWrapper.class), any(ArtefactLifecycle.class), any(Throwable.class));
        doAnswer(invocation -> {
            synchronizer.setStatus(artefact(invocation.getArgument(1)), invocation.getArgument(2), invocation.getArgument(3));
            return null;
        }).when(callback)
          .registerState(any(), any(TopologyWrapper.class), any(ArtefactLifecycle.class), any(String.class));
        synchronizer.setCallback(callback);
    }

    /**
     * The first pass: a failed scheduling is FAILED with its cause and not completed - not CREATED with
     * nothing scheduled.
     *
     * @throws Exception from the manager double
     */
    @Test
    void aFailedSchedulingIsRecordedFailedNotCreated() throws Exception {
        Job job = job(ArtefactLifecycle.NEW);
        TopologyWrapper<Job> wrapper = wrapper(job);
        doThrow(new IllegalStateException(SCHEDULER_REFUSAL)).when(jobsManager)
                                                             .scheduleJob(job);

        assertFalse(synchronizer.completeImpl(wrapper, ArtefactPhase.CREATE), "A failed scheduling is not completed - the pass retries it");
        assertEquals(ArtefactLifecycle.FAILED, job.getLifecycle(), "A job that was never scheduled must read FAILED, not CREATED");
        assertEquals(SCHEDULER_REFUSAL, job.getError(), "The recorded error must be the scheduler's refusal");
        assertNotEquals(Boolean.TRUE, job.getRunning(), "Nothing was scheduled, so nothing may read as running");
    }

    /**
     * The passes after: a FAILED job the scheduler still refuses stays FAILED and keeps being retried,
     * where the second attempt used to register FATAL.
     *
     * @throws Exception from the manager double
     */
    @Test
    void aFailedJobTheSchedulerStillRefusesStaysFailedAndIsRetried() throws Exception {
        Job job = job(ArtefactLifecycle.FAILED);
        TopologyWrapper<Job> wrapper = wrapper(job);
        doThrow(new IllegalStateException(SCHEDULER_REFUSAL)).when(jobsManager)
                                                             .scheduleJob(job);

        assertFalse(synchronizer.completeImpl(wrapper, ArtefactPhase.START));
        assertFalse(synchronizer.completeImpl(wrapper, ArtefactPhase.START));

        assertEquals(ArtefactLifecycle.FAILED, job.getLifecycle(), "A transiently refused job must never read FATAL");
        assertNotEquals(Boolean.TRUE, job.getRunning(), "Nothing was scheduled, so nothing may read as running");
        verify(jobsManager, times(2)).scheduleJob(job);
    }

    /**
     * Once the scheduler accepts, the very same FAILED artefact schedules, reads CREATED and carries no
     * stale error.
     *
     * @throws Exception from the manager double
     */
    @Test
    void aFailedJobHealsOnceTheSchedulerAccepts() throws Exception {
        Job job = job(ArtefactLifecycle.FAILED);
        job.setError(SCHEDULER_REFUSAL);
        TopologyWrapper<Job> wrapper = wrapper(job);
        doNothing().when(jobsManager)
                   .scheduleJob(job);

        assertTrue(synchronizer.completeImpl(wrapper, ArtefactPhase.START), "A scheduling the scheduler accepted completes");
        assertEquals(ArtefactLifecycle.CREATED, job.getLifecycle(), "The healed job must read CREATED");
        assertEquals("", job.getError(), "The healed job must carry no stale error");
        assertEquals(Boolean.TRUE, job.getRunning(), "The healed job is running");
    }

    private static Job job(ArtefactLifecycle lifecycle) {
        Job job = new Job("/retry/probe.job", "probe", "", Set.of(), "retry", "org.eclipse.dirigible.components.jobs.handler.JobHandler",
                "0 0 * * * ?", "retry/handler.js", "javascript", false, true, null, null, null);
        job.setLifecycle(lifecycle);
        job.setRunning(false);
        return job;
    }

    private TopologyWrapper<Job> wrapper(Job job) {
        return new TopologyWrapper<>(job, new HashMap<>(), synchronizer);
    }

    /**
     * Unwraps the artefact of a wrapper handed to the callback.
     *
     * @param wrapper the wrapper
     * @return the job
     */
    private static Job artefact(Object wrapper) {
        return ((TopologyWrapper<?>) wrapper).getArtefact() instanceof Job job ? job : null;
    }
}
