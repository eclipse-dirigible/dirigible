/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.engine.bpm.BpmProvider;
import org.eclipse.dirigible.components.engine.bpm.flowable.TaskService;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.ActivityStatusData;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.ProcessLabelKeys;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.job.api.Job;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.flowable.variable.api.runtime.VariableInstanceQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The Class BpmProviderFlowable. NOTE! - all methods in the class should be tenant aware
 */
@Component
public class BpmProviderFlowable implements BpmProvider {

    /** The Constant EXTENSION_BPMN20_XML. */
    private static final String EXTENSION_BPMN20_XML = "bpmn20.xml";

    /** The extension element carrying a process' declarations, and the task-label-catalog one. */
    private static final String TASK_LABEL_CATALOG_ELEMENT = "property";
    private static final String TASK_LABEL_CATALOG_PROPERTY = "taskLabelCatalog";

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BpmProviderFlowable.class);

    /** The repository. */
    private final IRepository repository;
    private final TenantContext tenantContext;
    private final FlowableArtefactsValidator flowableArtefactsValidator;
    /** The process engine. */
    private final ProcessEngine processEngine;

    public BpmProviderFlowable(IRepository repository, TenantContext tenantContext, FlowableArtefactsValidator flowableArtefactsValidator,
            ProcessEngine processEngine) {
        this.repository = repository;
        this.tenantContext = tenantContext;
        this.flowableArtefactsValidator = flowableArtefactsValidator;
        this.processEngine = processEngine;
    }

    /**
     * Deploy process.
     *
     * @param location the location
     * @return the string
     */
    public String deployProcess(String location) {
        LOGGER.debug("Deploying a BPMN process from location: [{}]", location);
        RepositoryService repositoryService = processEngine.getRepositoryService();
        Deployment deployment;
        if (!location.startsWith(IRepositoryStructure.SEPARATOR))
            location = IRepositoryStructure.SEPARATOR + location;
        String repositoryPath = IRepositoryStructure.PATH_REGISTRY_PUBLIC + location;
        if (getRepository().hasResource(repositoryPath)) {
            IResource resource = getRepository().getResource(repositoryPath);
            deployment = repositoryService.createDeployment()
                                          .tenantId(getTenantId())
                                          .addBytes(location + EXTENSION_BPMN20_XML, resource.getContent())
                                          .deploy();
        } else {
            try (InputStream in = BpmProviderFlowable.class.getResourceAsStream("/META-INF/dirigible" + location)) {
                if (in != null) {
                    try {
                        byte[] bytes = IOUtils.toByteArray(in);
                        deployment = repositoryService.createDeployment()
                                                      .addBytes(location + EXTENSION_BPMN20_XML, bytes)
                                                      .deploy();
                    } catch (IOException e) {
                        throw new IllegalArgumentException(e);
                    }
                } else {
                    throw new IllegalArgumentException("No BPMN resource found at location: " + location);
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("Error closing the BPMN resource at location: " + location, e);
            }
        }
        LOGGER.info("Process deployed with deployment id: [{}] and process key: [{}]", deployment.getId(), deployment.getKey());
        LOGGER.debug("Done deploying a BPMN process from location: [{}]", location);
        return deployment.getId();
    }

    /**
     * Gets the repository.
     *
     * @return the repository
     */
    public IRepository getRepository() {
        return repository;
    }

    /**
     * Gets the process engine.
     *
     * @return the process engine
     */

    private String getTenantId() {
        Tenant currentTenant = tenantContext.getCurrentTenant();
        LOGGER.debug("Current tenant is [{}]", currentTenant);
        return currentTenant.getId();
    }

    /**
     * Undeploy process.
     *
     * @param deploymentId the deployment id
     */
    public void undeployProcess(String deploymentId) {
        flowableArtefactsValidator.validateDeployment(deploymentId);

        RepositoryService repositoryService = processEngine.getRepositoryService();
        repositoryService.deleteDeployment(deploymentId, true);
    }

    /**
     * Start process.
     *
     * @param key the key
     * @param businessKey the business key
     * @param parameters the parameters
     * @return the process instance id
     */
    public String startProcess(String key, String businessKey, String parameters) {
        @SuppressWarnings("unchecked")
        Map<String, Object> variables = GsonHelper.fromJson(parameters, HashMap.class);

        return startProcess(key, businessKey, variables);
    }

    public String startProcess(String key, String businessKey, Map<String, Object> variables) {
        LOGGER.info("Starting a BPMN process by key: [{}]", key);
        RuntimeService runtimeService = processEngine.getRuntimeService();
        try {
            ProcessInstance processInstance =
                    runtimeService.startProcessInstanceByKeyAndTenantId(key, businessKey, variables, getTenantId());
            LOGGER.info("Started process instance with id [{}], key [{}] for tenant [{}]", processInstance.getId(), key,
                    processInstance.getTenantId());
            return processInstance.getId();
        } catch (Exception e) {
            LOGGER.error("Failed to start process with key [{}]", key, e);
            return null;
        }
    }

    /**
     * Sets the process instance name.
     *
     * @param processInstanceId the process instance id
     * @param name the name
     */
    public void setProcessInstanceName(String processInstanceId, String name) {
        flowableArtefactsValidator.validateExecutionId(processInstanceId);

        RuntimeService runtimeService = processEngine.getRuntimeService();
        runtimeService.setProcessInstanceName(processInstanceId, name);
    }

    /**
     * Updates the business key.
     *
     * @param processInstanceId the process instance id
     * @param businessKey the business key
     */
    public void updateBusinessKey(String processInstanceId, String businessKey) {
        flowableArtefactsValidator.validateExecutionId(processInstanceId);

        RuntimeService runtimeService = processEngine.getRuntimeService();
        runtimeService.updateBusinessKey(processInstanceId, businessKey);
    }

    /**
     * Updates the business status.
     *
     * @param processInstanceId the process instance id
     * @param businessStatus the business status
     */
    public void updateBusinessStatus(String processInstanceId, String businessStatus) {
        flowableArtefactsValidator.validateExecutionId(processInstanceId);

        RuntimeService runtimeService = processEngine.getRuntimeService();
        runtimeService.updateBusinessStatus(processInstanceId, businessStatus);
    }

    /**
     * Delete process.
     *
     * @param processInstanceId the processInstanceId
     * @param reason the reason
     */
    public void deleteProcess(String processInstanceId, String reason) {
        flowableArtefactsValidator.validateProcessInstanceId(processInstanceId);

        LOGGER.debug("Deleting a BPMN process instance by processInstanceId: [{}]", processInstanceId);
        try {
            processEngine.getRuntimeService()
                         .deleteProcessInstance(processInstanceId, reason);
            LOGGER.info("Done deleting a BPMN process instance by processInstanceId: [{}]", processInstanceId);
        } catch (Exception e) {
            LOGGER.error("Failed to delete process with processInstanceId [{}], reason [{}]", processInstanceId, reason, e);
        }
    }

    public TaskService getTaskService() {
        FlowableArtefactsValidator validator = new FlowableArtefactsValidator(processEngine, tenantContext);

        return new TaskServiceImpl(processEngine.getTaskService(), tenantContext, validator);
    }

    /**
     * Gets the variable.
     *
     * @param executionId the execution id
     * @param variableName the variable name
     * @return the variable
     */
    public Object getVariable(String executionId, String variableName) {
        flowableArtefactsValidator.validateExecutionId(executionId);

        RuntimeService runtimeService = processEngine.getRuntimeService();
        return runtimeService.getVariable(executionId, variableName);
    }

    public Map<String, Object> getVariables(String executionId) {
        flowableArtefactsValidator.validateExecutionId(executionId);

        RuntimeService runtimeService = processEngine.getRuntimeService();
        return runtimeService.getVariables(executionId);
    }

    /**
     * Sets the variable.
     *
     * @param executionId the execution id
     * @param variableName the variable name
     * @param value the value
     */
    public void setVariable(String executionId, String variableName, Object value) {
        flowableArtefactsValidator.validateExecutionId(executionId);

        RuntimeService runtimeService = processEngine.getRuntimeService();
        runtimeService.setVariable(executionId, variableName, value);
    }

    /**
     * Removes the variable.
     *
     * @param executionId the execution id
     * @param variableName the variable name
     */
    public void removeVariable(String executionId, String variableName) {
        flowableArtefactsValidator.validateExecutionId(executionId);

        RuntimeService runtimeService = processEngine.getRuntimeService();
        runtimeService.removeVariable(executionId, variableName);
    }

    /**
     * Correlates a message event to the process instance.
     *
     * An instance that is not subscribed to the message is the expected miss for the fail-soft glue
     * that drives waits and aborts - it is reported by its own type, like an instance that has already
     * ended, so a caller can tell it apart from a real fault (a database or Flowable failure, the wrong
     * tenant scope, a mis-generated message name) instead of meeting a NullPointerException.
     *
     * @param processInstanceId the process instance id
     * @param messageName the name of the event
     * @param variables the variables to be passed with the event
     * @throws IllegalArgumentException if the instance does not exist in the current tenant, or is not
     *         waiting on a message with this name
     */
    public void correlateMessageEvent(String processInstanceId, String messageName, Map<String, Object> variables) {
        flowableArtefactsValidator.validateProcessInstanceId(processInstanceId);

        RuntimeService runtimeService = processEngine.getRuntimeService();

        Execution execution = runtimeService.createExecutionQuery()
                                            .messageEventSubscriptionName(messageName)
                                            .processInstanceId(processInstanceId)
                                            .executionTenantId(getTenantId())
                                            .singleResult();

        if (execution == null) {
            throw new IllegalArgumentException("Process instance with id [" + processInstanceId
                    + "] is not waiting on a message event with name [" + messageName + "]");
        }

        runtimeService.messageEventReceived(messageName, execution.getId(), variables);
    }

    public List<ProcessDefinition> getProcessDefinitions(Optional<String> key) {
        ProcessDefinitionQuery processDefinitionsQuery = processEngine.getRepositoryService()
                                                                      .createProcessDefinitionQuery();
        processDefinitionsQuery.processDefinitionTenantId(getTenantId());
        if (key.isPresent() && !key.get()
                                   .isEmpty()) {
            processDefinitionsQuery.processDefinitionKey(key.get());
        }
        processDefinitionsQuery.processDefinitionTenantId(getTenantId());

        return processDefinitionsQuery.list();
    }

    /**
     * Where a process declares its display names are translated - the {@code taskLabelCatalog}
     * extension property on the {@code <process>} element:
     *
     * <pre>
     * &lt;process id="Approval" ...&gt;
     *   &lt;extensionElements&gt;
     *     &lt;flowable:property name="taskLabelCatalog" value="sales:sales-model.processes"/&gt;
     *   &lt;/extensionElements&gt;
     * </pre>
     *
     * Names are keyed within it by BPMN id, so the whole mapping is declared at generation time and
     * nothing is derived at runtime. Read off the deployment-cached BPMN model, so it costs no query
     * once the definition has been touched.
     *
     * @param processDefinitionId the process definition id
     * @return the declared keys, empty for a process (e.g. a hand-authored one) that declares none
     */
    public Optional<ProcessLabelKeys> getProcessLabelKeys(String processDefinitionId) {
        BpmnModel bpmnModel = processEngine.getRepositoryService()
                                           .getBpmnModel(processDefinitionId);
        Process process = bpmnModel == null ? null : bpmnModel.getMainProcess();
        if (process == null) {
            return Optional.empty();
        }
        return process.getExtensionElements()
                      .getOrDefault(TASK_LABEL_CATALOG_ELEMENT, List.of())
                      .stream()
                      .filter(element -> TASK_LABEL_CATALOG_PROPERTY.equals(element.getAttributeValue(null, "name")))
                      .map(element -> element.getAttributeValue(null, "value"))
                      .filter(value -> value != null && !value.isBlank())
                      .findFirst()
                      .map(catalog -> new ProcessLabelKeys(catalog, catalog + "." + process.getId()));
    }

    public ProcessDefinition getProcessDefinitionByKey(String processDefinitionKey) {
        return processEngine.getRepositoryService()
                            .createProcessDefinitionQuery()
                            .processDefinitionTenantId(getTenantId())
                            .processDefinitionKey(processDefinitionKey)
                            .singleResult();

    }

    public List<HistoricVariableInstance> getProcessHistoricInstanceVariables(String processInstanceId) {
        flowableArtefactsValidator.validateHistoricProcessInstanceByProcessInstanceId(processInstanceId);

        return processEngine.getHistoryService()
                            .createHistoricVariableInstanceQuery()
                            .processInstanceId(processInstanceId)
                            .list();
    }

    public List<VariableInstance> getProcessInstanceVariables(String processInstanceId, Optional<String> variableName) {
        flowableArtefactsValidator.validateProcessInstanceId(processInstanceId);

        VariableInstanceQuery processInstanceQuery = processEngine.getRuntimeService()
                                                                  .createVariableInstanceQuery();
        if (variableName.isPresent() && !variableName.get()
                                                     .isEmpty()) {
            processInstanceQuery.variableNameLike("%" + variableName.get() + "%");
        }
        return processInstanceQuery.processInstanceId(processInstanceId)
                                   .list();
    }

    public List<Job> getDeadLetterJobs(String processInstanceId) {
        return processEngine.getManagementService()
                            .createDeadLetterJobQuery()
                            .processInstanceId(processInstanceId)
                            .jobTenantId(getTenantId())
                            .list();
    }

    public Optional<byte[]> getProcessDefinitionImage(String processDefinitionKey) throws IOException {
        RepositoryService repositoryService = processEngine.getRepositoryService();

        ProcessDefinition process = repositoryService.createProcessDefinitionQuery()
                                                     .processDefinitionKey(processDefinitionKey)
                                                     .processDefinitionTenantId(getTenantId())
                                                     .latestVersion()
                                                     .singleResult();

        if (process == null) {
            return Optional.empty();
        }
        String deploymentId = process.getDeploymentId();
        String diagramResourceName = process.getDiagramResourceName();

        return Optional.of(repositoryService.getResourceAsStream(deploymentId, diagramResourceName)
                                            .readAllBytes());
    }

    public Optional<byte[]> getProcessInstanceImage(String processInstanceId) {
        RepositoryService repositoryService = processEngine.getRepositoryService();

        ProcessEngineConfiguration processEngineConfiguration = processEngine.getProcessEngineConfiguration();

        ProcessInstance processInstance = getProcessInstance(processInstanceId);

        if (processInstance == null) {
            return Optional.empty();
        }
        ProcessDefinition processDefinition = repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId());

        if (processDefinition != null && processDefinition.hasGraphicalNotation()) {
            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
            ProcessDiagramGenerator diagramGenerator = processEngineConfiguration.getProcessDiagramGenerator();
            InputStream resource = diagramGenerator.generateDiagram(bpmnModel, "png", getProcessInstanceActivityIds(processInstance),
                    Collections.emptyList(), processEngineConfiguration.getActivityFontName(),
                    processEngineConfiguration.getLabelFontName(), processEngineConfiguration.getAnnotationFontName(),
                    processEngineConfiguration.getClassLoader(), 1.0, processEngineConfiguration.isDrawSequenceFlowNameWithNoLabelDI());

            try {
                byte[] byteArray = IOUtils.toByteArray(resource);
                return Optional.of(byteArray);
            } catch (Exception e) {
                throw new IllegalArgumentException("Error exporting diagram", e);
            }

        } else {
            throw new IllegalArgumentException("Process instance with id '" + processInstanceId + "' has no graphical notation defined.");
        }
    }

    public ProcessInstance getProcessInstance(String processInstanceId) {
        return processEngine.getRuntimeService()
                            .createProcessInstanceQuery()
                            .processInstanceId(processInstanceId)
                            .processInstanceTenantId(getTenantId())
                            .singleResult();
    }

    public void retryDeadLetterJob(String jobId, int retries) {
        processEngine.getManagementService()
                     .moveDeadLetterJobToExecutableJob(jobId, retries);
    }

    public void addProcessInstanceVariable(String processInstanceId, String key, String value) {
        processEngine.getRuntimeService()
                     .setVariable(processInstanceId, key, value);
    }

    public List<ProcessInstance> getProcessInstances(Optional<String> key, Optional<String> businessKey) {
        ProcessInstanceQuery processInstanceQuery = processEngine.getRuntimeService()
                                                                 .createProcessInstanceQuery();

        if (key.isPresent() && !key.get()
                                   .isEmpty()) {
            processInstanceQuery.processDefinitionKey(key.get());
        }

        if (businessKey.isPresent() && !businessKey.get()
                                                   .isEmpty()) {
            processInstanceQuery.processInstanceBusinessKeyLike("%" + businessKey.get() + "%");
        }
        processInstanceQuery.processInstanceTenantId(getTenantId());

        return processInstanceQuery.list();
    }

    public List<HistoricProcessInstance> getCompletedProcessInstances(Optional<String> definitionKey, Optional<String> businessKey) {
        HistoricProcessInstanceQuery historicProcessInstanceQuery = processEngine.getHistoryService()
                                                                                 .createHistoricProcessInstanceQuery();

        if (definitionKey.isPresent() && !definitionKey.get()
                                                       .isEmpty()) {
            historicProcessInstanceQuery.processDefinitionKey(definitionKey.get());
        }

        if (businessKey.isPresent() && !businessKey.get()
                                                   .isEmpty()) {
            historicProcessInstanceQuery.processInstanceBusinessKeyLike("%" + businessKey.get() + "%");
        }
        historicProcessInstanceQuery.processInstanceTenantId(getTenantId());

        return historicProcessInstanceQuery.finished()
                                           .list();
    }

    public Deployment deployProcess(String deploymentKey, String resourceName, String content) {
        return processEngine.getRepositoryService()
                            .createDeployment()
                            .key(deploymentKey)
                            .tenantId(getTenantId())
                            .addString(resourceName, content)
                            .deploy();
    }

    public ProcessDefinition getProcessDefinitionByDeploymentId(String deploymentId) {
        return processEngine.getRepositoryService()
                            .createProcessDefinitionQuery()
                            .deploymentId(deploymentId)
                            .processDefinitionTenantId(getTenantId())
                            .singleResult();
    }

    public List<Deployment> getDeploymentsByKey(String deploymentKey) {
        return processEngine.getRepositoryService()
                            .createDeploymentQuery()
                            .deploymentKey(deploymentKey)
                            .deploymentTenantId(getTenantId())
                            .list();
    }

    public void deleteDeployment(String deploymentId) {
        flowableArtefactsValidator.validateDeployment(deploymentId);

        processEngine.getRepositoryService()
                     .deleteDeployment(deploymentId, true);
    }

    public long processDefinitionsCount() {
        return processEngine.getRepositoryService()
                            .createProcessDefinitionQuery()
                            .count();
    }

    public long getProcessInstancesCount() {
        return processEngine.getRuntimeService()
                            .createProcessInstanceQuery()
                            .count();
    }

    public long getFinishedHistoricProcessInstancesCount() {
        return processEngine.getHistoryService()
                            .createHistoricProcessInstanceQuery()
                            .finished()
                            .count();
    }

    public long getTotalCompletedTasksCount() {
        return processEngine.getHistoryService()
                            .createHistoricTaskInstanceQuery()
                            .finished()
                            .count();
    }

    public long getCompletedTasksForToday() {
        return processEngine.getHistoryService()
                            .createHistoricTaskInstanceQuery()
                            .finished()
                            .taskCompletedAfter(new Date(System.currentTimeMillis() - secondsForDays(1)))
                            .count();
    }

    private long secondsForDays(int days) {
        int hour = 60 * 60 * 1000;
        int day = 24 * hour;
        return (long) days * day;
    }

    public long getCompletedActivities() {
        return processEngine.getHistoryService()
                            .createHistoricActivityInstanceQuery()
                            .finished()
                            .count();
    }

    public String getProcessDefinitionXmlById(String processDefinitionId) {
        flowableArtefactsValidator.validateProcessDefinitionId(processDefinitionId);

        try {

            InputStream processModel = processEngine.getRepositoryService()
                                                    .getProcessModel(processDefinitionId);
            return IOUtils.toString(processModel, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Error reading BPMN file for: " + processDefinitionId, ex);
        }
    }

    /**
     * Reports where a running process instance currently sits, per activity.
     *
     * <p>
     * An active execution is only half the evidence. Flowable deactivates an execution as soon as an
     * asynchronous attempt fails - its {@code JobRetryCmd} moves the job to the timer-job table and
     * clears the active flag - and {@code RuntimeService#getActiveActivityIds} reports active
     * executions only, so a step waiting between retry attempts is invisible there and only its own
     * pending job still names it. Executable and timer jobs therefore count towards {@code positive}
     * alongside the active executions, by the higher of the two counts rather than by their sum: a
     * token that has not failed yet is active <em>and</em> job-bearing, so adding the two would report
     * every pending asynchronous step twice. Dead-lettered jobs stay {@code negative}.
     *
     * <p>
     * A retrying step is deliberately counted as {@code positive} - it is the step the instance is on,
     * and its failure is already visible as the job's exception message and as {@code negative} once
     * the retries are exhausted.
     *
     * @param processInstanceId the process instance id
     * @return the counters per activity id, empty when no such instance runs in the current tenant
     */
    public Map<String, ActivityStatusData> getProcessInstanceActiveActivityIds(String processInstanceId) {
        ProcessInstance processInstance = getProcessInstance(processInstanceId);
        if (null == processInstance) {
            return Collections.emptyMap();
        }

        Map<String, Integer> activeExecutions = countByValue(processEngine.getRuntimeService()
                                                                          .getActiveActivityIds(processInstance.getId()));
        // Merged by the higher count, never added, because the two sources overlap on every token that
        // has not failed yet. That undercounts only an element hosting both a waiting token and a
        // retrying one - an asynchronous task behind a loop or a fan-in - which would take a further
        // per-activity execution query to tell apart.
        Map<String, Integer> pendingJobs = countByElementId(getPendingJobs(processInstanceId));
        Map<String, Integer> deadLetterJobs = countByElementId(processEngine.getManagementService()
                                                                            .createDeadLetterJobQuery()
                                                                            .processInstanceId(processInstanceId)
                                                                            .list());

        Map<String, ActivityStatusData> statuses = new HashMap<>();
        activeExecutions.forEach((activityId, tokens) -> statusOf(statuses, activityId).positive = tokens);
        pendingJobs.forEach((activityId, jobs) -> {
            ActivityStatusData status = statusOf(statuses, activityId);
            status.positive = Math.max(status.positive, jobs);
        });
        deadLetterJobs.forEach((activityId, jobs) -> statusOf(statuses, activityId).negative = jobs);

        return statuses;
    }

    /**
     * The activity ids a running process instance occupies: its active executions plus the elements of
     * its pending jobs, which are the only evidence left for a step parked between retry attempts.
     *
     * @param processInstance the already resolved process instance
     * @return the occupied activity ids without duplicates, active executions first
     */
    public List<String> getProcessInstanceActivityIds(ProcessInstance processInstance) {
        Set<String> activityIds = new LinkedHashSet<>(processEngine.getRuntimeService()
                                                                   .getActiveActivityIds(processInstance.getId()));
        activityIds.addAll(elementIds(getPendingJobs(processInstance.getId())));

        return List.copyOf(activityIds);
    }

    /**
     * Where each of the given process instances sits, in a fixed number of statements rather than three
     * per instance.
     *
     * <p>
     * The evidence is the same as for a single instance - active executions plus the elements of the
     * pending jobs - gathered in three queries and grouped in memory: one execution query over all the
     * given ids, and the two job queries over the current tenant, whose rows are only the jobs waiting
     * to run and are then matched against the ids asked for. Flowable's job queries take a single
     * process-instance id, so the tenant is the narrowest batch filter available; a listing of a few
     * hundred instances is what this is for, and it is what the Monitoring shell polls every 30 s
     * (<a href="https://github.com/eclipse-dirigible/dirigible/issues/7250">#7250</a>).
     *
     * <p>
     * An execution counts only while it is active, matching {@code RuntimeService#getActiveActivityIds}
     * - the query itself cannot filter on the flag, so an inactive scope execution, the parent of a
     * subprocess or of a multi-instance body, is dropped here instead of being reported as a second
     * activity of the instance.
     *
     * @param processInstances the already resolved process instances
     * @return the occupied activity ids per process instance id, active executions first; an instance
     *         occupying none is absent
     */
    public Map<String, List<String>> getProcessInstanceActivityIds(List<ProcessInstance> processInstances) {
        Set<String> processInstanceIds = processInstances.stream()
                                                         .map(ProcessInstance::getId)
                                                         .collect(Collectors.toCollection(LinkedHashSet::new));
        if (processInstanceIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Set<String>> activityIds = new HashMap<>();
        activeExecutions(processInstanceIds).forEach(
                execution -> record(activityIds, processInstanceIds, execution.getProcessInstanceId(), execution.getActivityId()));
        pendingJobs().forEach(job -> record(activityIds, processInstanceIds, job.getProcessInstanceId(), job.getElementId()));

        return activityIds.entrySet()
                          .stream()
                          .collect(Collectors.toMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
    }

    private static void record(Map<String, Set<String>> activityIds, Set<String> processInstanceIds, String processInstanceId,
            String activityId) {
        if (null == activityId || !processInstanceIds.contains(processInstanceId)) {
            return;
        }
        activityIds.computeIfAbsent(processInstanceId, id -> new LinkedHashSet<>())
                   .add(activityId);
    }

    /**
     * The active child executions of the given process instances. The active flag is not a query
     * criterion in Flowable, so it is read off the returned rows, which are execution entities; an
     * implementation that does not expose the flag is taken at face value rather than dropped.
     *
     * @param processInstanceIds the process instance ids
     * @return the active child executions
     */
    private List<Execution> activeExecutions(Set<String> processInstanceIds) {
        List<Execution> executions = processEngine.getRuntimeService()
                                                  .createExecutionQuery()
                                                  .processInstanceIds(processInstanceIds)
                                                  .onlyChildExecutions()
                                                  .executionTenantId(getTenantId())
                                                  .list();

        return executions.stream()
                         .filter(execution -> !(execution instanceof DelegateExecution delegate) || delegate.isActive())
                         .toList();
    }

    /**
     * The jobs the current tenant is waiting on, executable and timer alike - the batch counterpart of
     * {@link #getPendingJobs(String)}.
     *
     * @return the pending jobs
     */
    private List<Job> pendingJobs() {
        ManagementService managementService = processEngine.getManagementService();

        List<Job> pendingJobs = new ArrayList<>(managementService.createJobQuery()
                                                                 .jobTenantId(getTenantId())
                                                                 .list());
        pendingJobs.addAll(managementService.createTimerJobQuery()
                                            .jobTenantId(getTenantId())
                                            .list());

        return pendingJobs;
    }

    /**
     * The jobs a process instance is waiting on: the executable ones, including a job an executor
     * currently holds, and the timer ones, where a failed job with retries left is parked between
     * attempts.
     *
     * @param processInstanceId the process instance id
     * @return the pending jobs
     */
    private List<Job> getPendingJobs(String processInstanceId) {
        ManagementService managementService = processEngine.getManagementService();

        List<Job> pendingJobs = new ArrayList<>(managementService.createJobQuery()
                                                                 .processInstanceId(processInstanceId)
                                                                 .list());
        pendingJobs.addAll(managementService.createTimerJobQuery()
                                            .processInstanceId(processInstanceId)
                                            .list());

        return pendingJobs;
    }

    private static ActivityStatusData statusOf(Map<String, ActivityStatusData> statuses, String activityId) {
        return statuses.computeIfAbsent(activityId, id -> new ActivityStatusData());
    }

    private static Map<String, Integer> countByElementId(List<? extends Job> jobs) {
        return countByValue(elementIds(jobs));
    }

    /**
     * The flow elements the given jobs belong to. A job bound to no element - an asynchronous variable
     * write, a process-instance migration - contributes nothing.
     *
     * @param jobs the jobs
     * @return the element ids, duplicates kept
     */
    private static List<String> elementIds(List<? extends Job> jobs) {
        return jobs.stream()
                   .map(Job::getElementId)
                   .filter(Objects::nonNull)
                   .toList();
    }

    private static Map<String, Integer> countByValue(List<String> values) {
        Map<String, Integer> counts = new HashMap<>();
        values.forEach(value -> counts.merge(value, 1, Integer::sum));

        return counts;
    }

    public Map<String, ActivityStatusData> getProcessDefinitionActiveActivityIds(String processDefinitionId) {
        ProcessDefinition processDefinition = getProcessDefinitionById(processDefinitionId);
        if (null == processDefinition) {
            return Collections.emptyMap();
        }

        processEngine.getTaskService()
                     .createTaskQuery()
                     .processDefinitionId(processDefinitionId)
                     .suspended()
                     .list();

        RuntimeService runtimeService = processEngine.getRuntimeService();
        List<Execution> executions = runtimeService.createExecutionQuery()
                                                   .onlyChildExecutions()
                                                   .processDefinitionId(processDefinitionId)
                                                   .list();
        List<String> allActiveActivityIds = executions.stream()
                                                      .map(Execution::getActivityId)
                                                      .collect(Collectors.toList());

        List<Job> jobs = processEngine.getManagementService()
                                      .createDeadLetterJobQuery()
                                      .processDefinitionId(processDefinitionId)
                                      .list();

        List<String> negativeActiveActivityIds = jobs.stream()
                                                     .map(Job::getElementId)
                                                     .collect(Collectors.toList());

        Map<String, ActivityStatusData> statuses = new HashMap<>();
        for (String each : allActiveActivityIds) {
            ActivityStatusData data = statuses.get(each);
            if (data == null) {
                data = new ActivityStatusData();
                data.positive = 1;
                statuses.put(each, data);
                continue;
            }
            data.positive += 1;
        }
        for (String negative : negativeActiveActivityIds) {
            ActivityStatusData data = statuses.get(negative);
            if (data == null) {
                data = new ActivityStatusData();
                data.negative = 1;
                statuses.put(negative, data);
                continue;
            }
            data.negative += 1;
            data.positive -= 1;
        }

        return statuses;
    }

    public ProcessDefinition getProcessDefinitionById(String processDefinitionId) {
        return processEngine.getRepositoryService()
                            .createProcessDefinitionQuery()
                            .processDefinitionTenantId(getTenantId())
                            .processDefinitionId(processDefinitionId)
                            .singleResult();
    }
}
