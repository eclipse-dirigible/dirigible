/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.open.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.BpmService;
import org.flowable.spring.boot.actuate.endpoint.ProcessEngineEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * The Class FlowableMetricsConfigurator.
 */
@Component
class FlowableMetricsConfigurator implements ApplicationListener<ApplicationReadyEvent> {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableMetricsConfigurator.class);

    /** The Constant METRIC_PREFIX. */
    private static final String METRIC_PREFIX = "flowable_";

    /** The Constant METER_SCOPE_NAME. */
    private static final String METER_SCOPE_NAME = "dirigible_flowable";

    private final BpmService bpmService;
    /** The open telemetry. */
    private final OpenTelemetry openTelemetry;

    FlowableMetricsConfigurator(BpmService bpmService, OpenTelemetry openTelemetry) {
        this.bpmService = bpmService;
        this.openTelemetry = openTelemetry;
    }

    /**
     * Create meters based on the logic in the Flowable actuator endpoint in
     * {@link ProcessEngineEndpoint} class.
     *
     * @param event app ready event
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        LOGGER.debug("Configuring flowable metrics...");

        Meter meter = openTelemetry.getMeter(METER_SCOPE_NAME);

        meter.gaugeBuilder(METRIC_PREFIX + "processDefinitionCount")
             .setDescription("Total number of flowable process definitions")
             .ofLongs()
             .buildWithCallback(measurement -> measurement.record(bpmService.processDefinitionsCount()));

        meter.gaugeBuilder(METRIC_PREFIX + "runningProcessInstanceCount")
             .setDescription("Total number of flowable running process instances")
             .ofLongs()
             .buildWithCallback(measurement -> measurement.record(bpmService.getProcessInstancesCount()));

        meter.gaugeBuilder(METRIC_PREFIX + "completedProcessInstanceCount")
             .setDescription("Total number of completed flowable process instances")
             .ofLongs()
             .buildWithCallback(measurement -> measurement.record(bpmService.getFinishedHistoricProcessInstancesCount()));

        meter.gaugeBuilder(METRIC_PREFIX + "openTaskCount")
             .setDescription("Total number of flowable open tasks")
             .ofLongs()
             .buildWithCallback(measurement -> measurement.record(bpmService.getTasksCount()));

        meter.gaugeBuilder(METRIC_PREFIX + "completedTaskCount")
             .setDescription("Total number of flowable completed tasks")
             .ofLongs()
             .buildWithCallback(measurement -> measurement.record(bpmService.getTotalCompletedTasksCount()));
        meter.gaugeBuilder(METRIC_PREFIX + "completedTaskCountToday")
             .setDescription("Total number of flowable completed tasks for today")
             .ofLongs()
             .buildWithCallback(measurement -> measurement.record(bpmService.getCompletedTasksForToday()));

        meter.gaugeBuilder(METRIC_PREFIX + "completedActivities")
             .setDescription("Total number of flowable completed activities")
             .ofLongs()
             .buildWithCallback(measurement -> measurement.record(bpmService.getCompletedActivities()));
    }

}
