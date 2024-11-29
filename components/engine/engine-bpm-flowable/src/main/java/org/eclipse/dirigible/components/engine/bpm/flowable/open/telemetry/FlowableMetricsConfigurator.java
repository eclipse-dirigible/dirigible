package org.eclipse.dirigible.components.engine.bpm.flowable.open.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import org.eclipse.dirigible.components.engine.bpm.flowable.provider.BpmProviderFlowable;
import org.flowable.spring.boot.actuate.endpoint.ProcessEngineEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
class FlowableMetricsConfigurator implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableMetricsConfigurator.class);

    private static final String METRIC_PREFIX = "flowable_";
    private static final String METER_SCOPE_NAME = "dirigible_flowable";

    private final BpmProviderFlowable bpmProviderFlowable;
    private final OpenTelemetry openTelemetry;

    FlowableMetricsConfigurator(BpmProviderFlowable bpmProviderFlowable, OpenTelemetry openTelemetry) {
        this.bpmProviderFlowable = bpmProviderFlowable;
        this.openTelemetry = openTelemetry;
    }

    /**
     * Create meters based on the logic in the Flowable actuator endpoint in
     * {@link ProcessEngineEndpoint} class
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
             .buildWithCallback(measurement -> measurement.record(bpmProviderFlowable.getProcessEngine()
                                                                                     .getRepositoryService()
                                                                                     .createProcessDefinitionQuery()
                                                                                     .count()));

        meter.gaugeBuilder(METRIC_PREFIX + "runningProcessInstanceCount")
             .setDescription("Total number of flowable running process instances")
             .ofLongs()
             .buildWithCallback(measurement -> measurement.record(bpmProviderFlowable.getProcessEngine()
                                                                                     .getRuntimeService()
                                                                                     .createProcessInstanceQuery()
                                                                                     .count()));

        meter.gaugeBuilder(METRIC_PREFIX + "completedProcessInstanceCount")
             .setDescription("Total number of completed flowable process instances")
             .ofLongs()
             .buildWithCallback(measurement -> measurement.record(bpmProviderFlowable.getProcessEngine()
                                                                                     .getHistoryService()
                                                                                     .createHistoricProcessInstanceQuery()
                                                                                     .finished()
                                                                                     .count()));

        meter.gaugeBuilder(METRIC_PREFIX + "openTaskCount")
             .setDescription("Total number of flowable open tasks")
             .ofLongs()
             .buildWithCallback(measurement -> measurement.record(bpmProviderFlowable.getProcessEngine()
                                                                                     .getTaskService()
                                                                                     .createTaskQuery()
                                                                                     .count()));

        meter.gaugeBuilder(METRIC_PREFIX + "completedTaskCount")
             .setDescription("Total number of flowable completed tasks")
             .ofLongs()
             .buildWithCallback(measurement -> measurement.record(bpmProviderFlowable.getProcessEngine()
                                                                                     .getHistoryService()
                                                                                     .createHistoricTaskInstanceQuery()
                                                                                     .finished()
                                                                                     .count()));
        meter.gaugeBuilder(METRIC_PREFIX + "completedTaskCountToday")
             .setDescription("Total number of flowable completed tasks for today")
             .ofLongs()
             .buildWithCallback(measurement -> measurement.record(bpmProviderFlowable.getProcessEngine()
                                                                                     .getHistoryService()
                                                                                     .createHistoricTaskInstanceQuery()
                                                                                     .finished()
                                                                                     .taskCompletedAfter(new Date(System.currentTimeMillis()
                                                                                             - secondsForDays(1)))
                                                                                     .count()));

        meter.gaugeBuilder(METRIC_PREFIX + "completedActivities")
             .setDescription("Total number of flowable completed activities")
             .ofLongs()
             .buildWithCallback(measurement -> measurement.record(bpmProviderFlowable.getProcessEngine()
                                                                                     .getHistoryService()
                                                                                     .createHistoricActivityInstanceQuery()
                                                                                     .finished()
                                                                                     .count()));
    }

    private long secondsForDays(int days) {
        int hour = 60 * 60 * 1000;
        int day = 24 * hour;
        return (long) days * day;
    }

}
