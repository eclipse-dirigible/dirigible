/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs;

import org.quartz.*;

import java.util.Optional;

/**
 * The Class DirigibleJob.
 */
public abstract class DirigibleJob implements Job {

    /**
     * Creates the trigger.
     *
     * @return the trigger
     */
    public Trigger createTrigger() {
        JobDetail job = createJob();

        Optional<String> group = getTriggerGroup();
        TriggerKey key = group.isPresent() ? TriggerKey.triggerKey(getTriggerKey(), group.get()) : TriggerKey.triggerKey(getTriggerKey());

        return TriggerBuilder.newTrigger()
                             .forJob(job)
                             .withIdentity(key)
                             .withDescription(getTriggerDescription())
                             .withSchedule(getSchedule())
                             .build();
    }

    /**
     * Creates the job.
     *
     * @return the job detail
     */
    public JobDetail createJob() {
        Optional<String> jobGroup = getJobGroup();
        JobKey key = jobGroup.isPresent() ? JobKey.jobKey(getJobKey(), jobGroup.get()) : JobKey.jobKey(getJobKey());

        return JobBuilder.newJob()
                         .ofType(this.getClass())
                         .storeDurably()
                         .requestRecovery()
                         .withIdentity(key)
                         .withDescription(getJobDescription())
                         .build();
    }

    /**
     * Gets the job group.
     *
     * @return the job group
     */
    protected abstract Optional<String> getJobGroup();

    /**
     * Gets the job key.
     *
     * @return the job key
     */
    protected abstract String getJobKey();

    /**
     * Gets the job description.
     *
     * @return the job description
     */
    protected abstract String getJobDescription();

    /**
     * Gets the trigger group.
     *
     * @return the trigger group
     */
    protected abstract Optional<String> getTriggerGroup();

    /**
     * Gets the trigger key.
     *
     * @return the trigger key
     */
    protected abstract String getTriggerKey();

    /**
     * Gets the trigger description.
     *
     * @return the trigger description
     */
    protected abstract String getTriggerDescription();

    /**
     * Gets the schedule.
     *
     * @return the schedule
     */
    protected abstract SimpleScheduleBuilder getSchedule();
}
