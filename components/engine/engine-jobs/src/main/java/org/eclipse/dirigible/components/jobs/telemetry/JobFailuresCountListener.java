/*
 * Copyright (c) 2010-2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.springframework.stereotype.Component;

/**
 * The listener interface for receiving jobFailuresCount events. The class that is interested in
 * processing a jobFailuresCount event implements this interface, and the object created with that
 * class is registered with a component using the component's addJobFailuresCountListener method.
 * When the jobFailuresCount event occurs, that object's appropriate method is invoked.
 *
 * see JobFailuresCountEvent
 */
@Component
public class JobFailuresCountListener extends OpenTelemetryListener {

    /** The counter. */
    private final LongCounter counter;

    /**
     * Instantiates a new job failures count listener.
     *
     * @param openTelemetry the open telemetry
     */
    JobFailuresCountListener(OpenTelemetry openTelemetry) {
        this.counter = openTelemetry.getMeter(QuartzMetricConstants.METER_SCOPE_NAME)
                                    .counterBuilder("quartz_job_failed_count")
                                    .setDescription("Total number of failed jobs")
                                    .build();
    }

    /**
     * Job was executed.
     *
     * @param context the context
     * @param jobException the job exception
     */
    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        JobKey jobKey = context.getJobDetail()
                               .getKey();
        if (jobException == null) {
            counter.add(0, Attributes.of(AttributeKey.stringKey("job_name"), jobKey.getName(), AttributeKey.stringKey("job_group"),
                    jobKey.getGroup()));
        } else {
            String exceptionType = jobException.getClass()
                                               .getName();

            counter.add(1, Attributes.of(AttributeKey.stringKey("job_name"), jobKey.getName(), AttributeKey.stringKey("job_group"),
                    jobKey.getGroup(), AttributeKey.stringKey("exception_type"), exceptionType));
        }
    }
}
