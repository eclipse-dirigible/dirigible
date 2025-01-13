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
 * The listener interface for receiving jobExecutionsCount events. The class that is interested in
 * processing a jobExecutionsCount event implements this interface, and the object created with that
 * class is registered with a component using the component's addJobExecutionsCountListener method.
 * When the jobExecutionsCount event occurs, that object's appropriate method is invoked.
 *
 * see JobExecutionsCountEvent
 */
@Component
public class JobExecutionsCountListener extends OpenTelemetryListener {

    /** The counter. */
    private final LongCounter counter;

    /**
     * Instantiates a new job executions count listener.
     *
     * @param openTelemetry the open telemetry
     */
    JobExecutionsCountListener(OpenTelemetry openTelemetry) {
        this.counter = openTelemetry.getMeter(QuartzMetricConstants.METER_SCOPE_NAME)
                                    .counterBuilder("quartz_job_executed_count")
                                    .setDescription("Total number of executed jobs")
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

        counter.add(1, Attributes.of(AttributeKey.stringKey("job_name"), jobKey.getName(), AttributeKey.stringKey("job_group"),
                jobKey.getGroup()));
    }
}
