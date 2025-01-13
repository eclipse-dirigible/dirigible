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
import io.opentelemetry.api.metrics.LongHistogram;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * The listener interface for receiving jobExecutionsDuration events. The class that is interested
 * in processing a jobExecutionsDuration event implements this interface, and the object created
 * with that class is registered with a component using the component's
 * addJobExecutionsDurationListener method. When the jobExecutionsDuration event occurs, that
 * object's appropriate method is invoked.
 *
 * see JobExecutionsDurationEvent
 */
@Component
public class JobExecutionsDurationListener extends OpenTelemetryListener {

    /** The Constant START_TIME_KEY. */
    private static final String START_TIME_KEY = "startTime";

    /** The histogram. */
    private final LongHistogram histogram;

    /**
     * Instantiates a new job executions duration listener.
     *
     * @param openTelemetry the open telemetry
     */
    JobExecutionsDurationListener(OpenTelemetry openTelemetry) {
        this.histogram = openTelemetry.getMeter(QuartzMetricConstants.METER_SCOPE_NAME)
                                      .histogramBuilder("quartz_job_execution_time")
                                      .setDescription("Job execution duration in milliseconds")
                                      .ofLongs()
                                      .build();
    }

    /**
     * Job to be executed.
     *
     * @param context the context
     */
    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        context.put(START_TIME_KEY, Instant.now());
    }

    /**
     * Job was executed.
     *
     * @param context the context
     * @param jobException the job exception
     */
    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        Instant startTime = (Instant) context.get(START_TIME_KEY);
        Instant endTime = Instant.now();
        Duration duration = Duration.between(startTime, endTime);

        JobKey jobKey = context.getJobDetail()
                               .getKey();

        histogram.record(duration.toMillis(), Attributes.of(AttributeKey.stringKey("job_name"), jobKey.getName(),
                AttributeKey.stringKey("job_group"), jobKey.getGroup()));
    }
}
