/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.handler;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.Date;

import org.eclipse.dirigible.components.data.sources.manager.DataSourcesManager;
import org.eclipse.dirigible.components.jobs.domain.JobLog;
import org.eclipse.dirigible.components.jobs.service.JobLogService;
import org.eclipse.dirigible.components.jobs.tenant.JobNameCreator;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionException;

/**
 * A run leaves an execution log whichever way it was started (dirigible #7075). A manual "Run now"
 * reaches this service through {@code JobService.trigger}, a scheduled fire through the Quartz
 * context - both end up in the same TRIGGRED-then-FINISHED/FAILED trail.
 */
class JobExecutionServiceTest {

    /** The job name as the artefact declares it. */
    private static final String JOB_NAME = "project/report.job";

    /** The job's handler - a repository path, so the default JavaScript engine runs it. */
    private static final String HANDLER = "/project/report.mjs";

    private final JobLogService jobLogService = mock(JobLogService.class);

    private final JobHandlerRunner jobHandlerRunner = mock(JobHandlerRunner.class);

    private final JobExecutionService jobExecutionService =
            new JobExecutionService(jobLogService, mock(JobNameCreator.class), mock(DataSourcesManager.class), jobHandlerRunner);

    @Test
    void aSuccessfulRunIsLoggedAsTriggeredThenFinished() throws Exception {
        JobLog triggered = triggeredLog();

        jobExecutionService.executeJob(JOB_NAME, HANDLER, null);

        verify(jobHandlerRunner).run(HANDLER, null);
        verify(jobLogService).jobFinished(eq(JOB_NAME), eq(HANDLER), eq(triggered.getId()), any(Date.class));
        verify(jobLogService, never()).jobFailed(any(), any(), anyLong(), any(), any());
    }

    @Test
    void aFailedRunIsLoggedAsFailedWithTheCauseAndRethrown() throws Exception {
        JobLog triggered = triggeredLog();
        doThrow(new IllegalStateException("boom")).when(jobHandlerRunner)
                                                  .run(HANDLER, null);

        assertThrows(JobExecutionException.class, () -> jobExecutionService.executeJob(JOB_NAME, HANDLER, null));

        verify(jobLogService).jobFailed(eq(JOB_NAME), eq(HANDLER), eq(triggered.getId()), any(Date.class), eq("boom"));
        verify(jobLogService, never()).jobFinished(any(), any(), anyLong(), any());
    }

    /**
     * The log is a record of the run, not a precondition for it - an unwritable log must not silently
     * stop the schedule.
     */
    @Test
    void theJobStillRunsWhenItsTriggeredEntryCannotBeWritten() throws Exception {
        when(jobLogService.jobTriggered(JOB_NAME, HANDLER)).thenThrow(new IllegalStateException("the log is unavailable"));

        jobExecutionService.executeJob(JOB_NAME, HANDLER, null);

        verify(jobHandlerRunner).run(HANDLER, null);
    }

    /**
     * A run whose TRIGGRED entry could not be written and which then fails must still be recorded
     * (#7148): without a FAILED entry there is no stamp on the job and no failure email, and the Jobs
     * view keeps showing the previous outcome - a failed run indistinguishable from one that never
     * happened.
     */
    @Test
    void aFailedRunIsRecordedEvenWithoutItsTriggeredEntry() throws Exception {
        when(jobLogService.jobTriggered(JOB_NAME, HANDLER)).thenThrow(new IllegalStateException("the log is unavailable"));
        doThrow(new IllegalStateException("boom")).when(jobHandlerRunner)
                                                  .run(HANDLER, null);

        assertThrows(JobExecutionException.class, () -> jobExecutionService.executeJob(JOB_NAME, HANDLER, null));

        verify(jobLogService).jobFailed(eq(JOB_NAME), eq(HANDLER), eq(JobExecutionService.NO_TRIGGERED_ID), any(Date.class), eq("boom"));
    }

    /** The successful branch is unanchored the same way - the recovery email fires too. */
    @Test
    void aSuccessfulRunIsRecordedEvenWithoutItsTriggeredEntry() throws Exception {
        when(jobLogService.jobTriggered(JOB_NAME, HANDLER)).thenThrow(new IllegalStateException("the log is unavailable"));

        jobExecutionService.executeJob(JOB_NAME, HANDLER, null);

        verify(jobLogService).jobFinished(eq(JOB_NAME), eq(HANDLER), eq(JobExecutionService.NO_TRIGGERED_ID), any(Date.class));
    }

    /** An outcome that cannot be written either must not break the run any further. */
    @Test
    void anUnwritableOutcomeIsSwallowed() throws Exception {
        when(jobLogService.jobTriggered(JOB_NAME, HANDLER)).thenThrow(new IllegalStateException("the log is unavailable"));
        when(jobLogService.jobFinished(any(), any(), anyLong(), any())).thenThrow(new IllegalStateException("still unavailable"));

        jobExecutionService.executeJob(JOB_NAME, HANDLER, null);

        verify(jobHandlerRunner).run(HANDLER, null);
    }

    private JobLog triggeredLog() {
        JobLog triggered = new JobLog();
        triggered.setId(42L);
        triggered.setTriggeredAt(new Timestamp(System.currentTimeMillis()));
        when(jobLogService.jobTriggered(JOB_NAME, HANDLER)).thenReturn(triggered);
        return triggered;
    }
}
