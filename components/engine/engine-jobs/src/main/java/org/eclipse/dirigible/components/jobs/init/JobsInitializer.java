/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.init;

import org.eclipse.dirigible.components.base.ApplicationListenersOrder.ApplicationReadyEventListeners;
import org.eclipse.dirigible.components.jobs.DirigibleJob;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * The Class JobsInitializer.
 */
@Order(ApplicationReadyEventListeners.JOBS_INITIALIZER)
@Component
class JobsInitializer implements ApplicationListener<ApplicationReadyEvent> {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(JobsInitializer.class);

    /** The jobs. */
    private final Set<DirigibleJob> jobs;

    /** The scheduler. */
    private final Scheduler scheduler;

    /**
     * Instantiates a new jobs initializer.
     *
     * @param jobs the jobs
     * @param scheduler the scheduler
     */
    JobsInitializer(Set<DirigibleJob> jobs, Scheduler scheduler) {
        this.jobs = jobs;
        this.scheduler = scheduler;
    }

    /**
     * On application event.
     *
     * @param event the event
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        LOGGER.info("Initializing [{}] jobs: {}}", jobs.size(), jobs);

        jobs.forEach(this::rescheduleJob);

        LOGGER.info("Completed.");
    }

    private void rescheduleJob(DirigibleJob job) {
        try {
            Trigger trigger = job.createTrigger();

            JobKey jobKey = trigger.getJobKey();
            if (scheduler.checkExists(jobKey)) {
                TriggerKey triggerKey = trigger.getKey();
                scheduler.unscheduleJob(triggerKey);

                scheduler.deleteJob(jobKey);
                LOGGER.info("Unscheduled job: [{}]", job);
            }

            JobDetail jobDetail = job.createJob();
            scheduler.scheduleJob(jobDetail, trigger);
            LOGGER.info("Scheduled job [{}] with trigger [{}]", jobDetail, trigger);

        } catch (SchedulerException ex) {
            throw new IllegalStateException("Failed to reschedule job " + job, ex);
        }
    }
}
