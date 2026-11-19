/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.retention;

import org.eclipse.dirigible.commons.config.DirigibleConfig;
import org.eclipse.dirigible.components.jobs.domain.JobStatus;
import org.eclipse.dirigible.components.jobs.repository.JobLogRepository;
import org.eclipse.dirigible.components.jobs.repository.JobLogRepository.TenantJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

/**
 * Purges the job logs older than {@code DIRIGIBLE_SCHEDULER_LOGS_RETENTION_PERIOD} (hours). The
 * last run of every job is kept however old it is, per tenant, so a job that has not run for longer
 * than the period still shows what its last run did. A period of zero or less keeps every log.
 */
@Component
class JobLogRetention {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(JobLogRetention.class);

    /**
     * The statuses of the entries that mark a run: its TRIGGRED entry and its outcome. All of them
     * carry the time the run was triggered at, and whatever the handler logs during the run is stamped
     * later than that.
     */
    private static final Set<JobStatus> RUN_STATUSES = EnumSet.of(JobStatus.TRIGGRED, JobStatus.FINISHED, JobStatus.FAILED);

    /** The job logs. */
    private final JobLogRepository jobLogs;

    /**
     * Instantiates a new job log retention.
     *
     * @param jobLogs the job logs
     */
    JobLogRetention(JobLogRepository jobLogs) {
        this.jobLogs = jobLogs;
    }

    /**
     * Purges the logs older than the configured retention period.
     *
     * @return how many logs were deleted
     */
    long purge() {
        int retentionInHours = DirigibleConfig.SCHEDULER_LOGS_RETENTION_PERIOD.getIntValue();
        if (retentionInHours <= 0) {
            LOGGER.debug("Job logs are not purged: [{}] is [{}].", DirigibleConfig.SCHEDULER_LOGS_RETENTION_PERIOD.getKey(),
                    retentionInHours);
            return 0;
        }
        return purgeTriggeredBefore(Timestamp.from(Instant.now()
                                                          .minus(Duration.ofHours(retentionInHours))));
    }

    /**
     * Purges the logs triggered before the given moment, job by job - each job in a statement and a
     * transaction of its own, so a long backlog is not one transaction.
     *
     * @param cutoff the moment
     * @return how many logs were deleted
     */
    long purgeTriggeredBefore(Timestamp cutoff) {
        long deleted = 0;
        for (TenantJob job : jobLogs.findJobsWithLogsTriggeredBefore(cutoff)) {
            Timestamp before = jobLogs.findLastRunTriggeredAt(job.getTenantId(), job.getJobName(), RUN_STATUSES)
                                      .filter(lastRun -> lastRun.before(cutoff))
                                      .orElse(cutoff);
            deleted += jobLogs.deleteTriggeredBefore(job.getTenantId(), job.getJobName(), before);
        }
        if (deleted > 0) {
            LOGGER.info("Purged [{}] job logs triggered before [{}].", deleted, cutoff);
        }
        return deleted;
    }
}
