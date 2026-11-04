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

import java.util.List;

import org.eclipse.dirigible.commons.api.helpers.LogSanitizer;
import org.eclipse.dirigible.components.base.tenant.TenantSuspensionParticipant;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Suspension: pauses the tenant's scheduled jobs - every Quartz job named
 * {@code <tenantId>###<name>} in the user-defined group, the {@code .job} artefacts and the
 * client-Java {@code @Scheduled} ones alike - and resumes them. A paused trigger survives a restart
 * in the JDBC job store, and a job scheduled while the tenant is suspended is paused by
 * {@link JobsManager} at once. Nothing is deleted; the job logs stay. Internal platform jobs are
 * never touched.
 */
@Component
class JobsTenantSuspension implements TenantSuspensionParticipant {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(JobsTenantSuspension.class);

    /** The order of this participant: what runs for the tenant. */
    static final int ORDER = 100;

    /** The group the tenant-scoped user jobs are scheduled in. */
    private static final String JOB_GROUP_DEFINED = "defined";

    /** The scheduler. */
    private final Scheduler scheduler;

    /**
     * Instantiates a new jobs suspension.
     *
     * @param scheduler the scheduler
     */
    JobsTenantSuspension(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public void suspend(String tenantId) {
        try {
            List<JobKey> keys = tenantJobs(tenantId);
            for (JobKey key : keys) {
                scheduler.pauseJob(key);
            }
            LOGGER.info("Paused [{}] scheduled job(s) of tenant [{}].", keys.size(), LogSanitizer.sanitize(tenantId));
        } catch (SchedulerException ex) {
            throw new IllegalStateException("Could not pause the scheduled jobs of tenant [" + tenantId + "]: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void resume(String tenantId) {
        try {
            List<JobKey> keys = tenantJobs(tenantId);
            for (JobKey key : keys) {
                scheduler.resumeJob(key);
            }
            LOGGER.info("Resumed [{}] scheduled job(s) of tenant [{}].", keys.size(), LogSanitizer.sanitize(tenantId));
        } catch (SchedulerException ex) {
            throw new IllegalStateException("Could not resume the scheduled jobs of tenant [" + tenantId + "]: " + ex.getMessage(), ex);
        }
    }

    private List<JobKey> tenantJobs(String tenantId) throws SchedulerException {
        String prefix = tenantId + "###";
        return scheduler.getJobKeys(GroupMatcher.jobGroupEquals(JOB_GROUP_DEFINED))
                        .stream()
                        .filter(key -> key.getName()
                                          .startsWith(prefix))
                        .toList();
    }
}
