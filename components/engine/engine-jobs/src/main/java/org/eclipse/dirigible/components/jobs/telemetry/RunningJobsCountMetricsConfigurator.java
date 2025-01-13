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
import io.opentelemetry.api.metrics.Meter;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * The Class RunningJobsCountMetricsConfigurator.
 */
@Component
class RunningJobsCountMetricsConfigurator implements ApplicationListener<ApplicationReadyEvent> {

    /** The open telemetry. */
    private final OpenTelemetry openTelemetry;

    /** The scheduler. */
    private final Scheduler scheduler;

    /**
     * Instantiates a new running jobs count metrics configurator.
     *
     * @param openTelemetry the open telemetry
     * @param scheduler the scheduler
     */
    RunningJobsCountMetricsConfigurator(OpenTelemetry openTelemetry, Scheduler scheduler) {
        this.openTelemetry = openTelemetry;
        this.scheduler = scheduler;
    }

    /**
     * On application event.
     *
     * @param event the event
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Meter meter = openTelemetry.getMeter(QuartzMetricConstants.METER_SCOPE_NAME);

        meter.gaugeBuilder("quartz_scheduler_running_jobs")
             .setDescription("Current number of running jobs")
             .ofLongs()
             .buildWithCallback(observation -> {
                 try {
                     observation.record(scheduler.getCurrentlyExecutingJobs()
                                                 .size());
                 } catch (SchedulerException e) {
                     observation.record(0);
                 }
             });
    }

}
