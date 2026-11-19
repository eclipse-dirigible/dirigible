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

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;

import org.eclipse.dirigible.components.jobs.SystemJob;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.SimpleScheduleBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Purges the job logs past their retention period, at start and every hour after it.
 *
 * <p>
 * Quartz instantiates the job through its no-argument constructor and the job factory autowires the
 * instance afterwards, so the collaborator is a field and the class is public.
 */
@Component
@DisallowConcurrentExecution
public class JobLogRetentionJob extends SystemJob {

    @Autowired
    private JobLogRetention retention;

    @Override
    public void execute(JobExecutionContext context) {
        retention.purge();
    }

    @Override
    protected String getJobKey() {
        return "JobLogRetentionJob";
    }

    @Override
    protected String getTriggerKey() {
        return "JobLogRetentionJobTrigger";
    }

    @Override
    protected SimpleScheduleBuilder getSchedule() {
        return simpleSchedule().withIntervalInHours(1)
                               .repeatForever()
                               .withMisfireHandlingInstructionNextWithExistingCount();
    }
}
