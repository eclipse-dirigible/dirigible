/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.integration.tests.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.eclipse.dirigible.components.jobs.domain.JobLog;
import org.eclipse.dirigible.components.jobs.domain.JobStatus;
import org.eclipse.dirigible.components.jobs.repository.JobLogRepository;
import org.eclipse.dirigible.tests.base.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The scheduled purge deletes the job logs older than
 * {@code DIRIGIBLE_SCHEDULER_LOGS_RETENTION_PERIOD} (a week by default) and keeps the last run of a
 * job that has not run since (#8702). Driven through the system job the platform schedules, fired
 * on demand instead of waiting for its hour.
 */
class JobLogRetentionIT extends IntegrationTest {

    private static final JobKey RETENTION_JOB = JobKey.jobKey("JobLogRetentionJob", "system");
    private static final String TENANT = "job-log-retention-it";
    private static final String ACTIVE_JOB = "retention-it/active.job";
    private static final String IDLE_JOB = "retention-it/idle.job";

    @Autowired
    private JobLogRepository jobLogs;

    @Autowired
    private Scheduler scheduler;

    private long sequence;

    @Test
    void the_scheduled_purge_deletes_the_old_runs_and_keeps_the_last_one() throws SchedulerException {
        run(ACTIVE_JOB, daysAgo(10));
        Timestamp recentRun = daysAgo(1);
        run(ACTIVE_JOB, recentRun);
        run(IDLE_JOB, daysAgo(30));
        Timestamp lastRunOfIdleJob = daysAgo(20);
        run(IDLE_JOB, lastRunOfIdleJob);

        Awaitility.await()
                  .atMost(30, TimeUnit.SECONDS)
                  .until(() -> scheduler.checkExists(RETENTION_JOB));
        scheduler.triggerJob(RETENTION_JOB);

        Awaitility.await()
                  .atMost(30, TimeUnit.SECONDS)
                  .untilAsserted(() -> {
                      assertEquals(List.of(recentRun, recentRun), triggeredAtOf(ACTIVE_JOB));
                      assertEquals(List.of(lastRunOfIdleJob, lastRunOfIdleJob), triggeredAtOf(IDLE_JOB));
                  });
    }

    /** A run as the engine logs it: the TRIGGRED entry and the outcome. */
    private void run(String jobName, Timestamp triggeredAt) {
        log(jobName, JobStatus.TRIGGRED, triggeredAt);
        log(jobName, JobStatus.FINISHED, triggeredAt);
    }

    private void log(String jobName, JobStatus status, Timestamp triggeredAt) {
        String name = jobName + "-" + sequence++;
        jobLogs.save(new JobLog("/" + name, name, null, null, jobName, "handler", triggeredAt, null, null, status, null, TENANT));
    }

    private List<Timestamp> triggeredAtOf(String jobName) {
        return jobLogs.findAll()
                      .stream()
                      .filter(log -> TENANT.equals(log.getTenantId()) && jobName.equals(log.getJobName()))
                      .map(JobLog::getTriggeredAt)
                      .sorted()
                      .toList();
    }

    private static Timestamp daysAgo(int days) {
        return Timestamp.from(Instant.now()
                                     .minus(Duration.ofDays(days))
                                     .truncatedTo(ChronoUnit.MILLIS));
    }
}
