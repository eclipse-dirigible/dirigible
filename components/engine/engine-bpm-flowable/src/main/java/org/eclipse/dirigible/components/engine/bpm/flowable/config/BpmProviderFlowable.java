/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.config;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.engine.bpm.BpmProvider;
import org.eclipse.dirigible.components.engine.bpm.flowable.TaskService;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.ActivityStatusData;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The Class BpmProviderFlowable. NOTE! - all methods in the class should be tenant aware
 */
@Component
public class BpmProviderFlowable implements BpmProvider {

    /** The Constant EXTENSION_BPMN20_XML. */
    private static final String EXTENSION_BPMN20_XML = "bpmn20.xml";

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
     * @param processInstanceId the process instance id
     * @param messageName the name of the event
     * @param variables the variables to be passed with the event
     */
    public void correlateMessageEvent(String processInstanceId, String messageName, Map<String, Object> variables) {
        flowableArtefactsValidator.validateProcessInstanceId(processInstanceId);

        RuntimeService runtimeService = processEngine.getRuntimeService();

        Execution execution = runtimeService.createExecutionQuery()
                                            .messageEventSubscriptionName(messageName)
                                            .processInstanceId(processInstanceId)
                                            .executionTenantId(getTenantId())
                                            .singleResult();

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
        RuntimeService runtimeService = processEngine.getRuntimeService();

        ProcessInstance processInstance = getProcessInstance(processInstanceId);

        if (processInstance == null) {
            return Optional.empty();
        }
        ProcessDefinition processDefinition = repositoryService.getProcessDefinition(processInstance.getProcessDefinitionId());

        if (processDefinition != null && processDefinition.hasGraphicalNotation()) {
            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
            ProcessDiagramGenerator diagramGenerator = processEngineConfiguration.getProcessDiagramGenerator();
            InputStream resource = diagramGenerator.generateDiagram(bpmnModel, "png",
                    runtimeService.getActiveActivityIds(processInstance.getId()), Collections.emptyList(),
                    processEngineConfiguration.getActivityFontName(), processEngineConfiguration.getLabelFontName(),
                    processEngineConfiguration.getAnnotationFontName(), processEngineConfiguration.getClassLoader(), 1.0,
                    processEngineConfiguration.isDrawSequenceFlowNameWithNoLabelDI());

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

    public Deployment deployProcess(String deploymentKey, String resourceName, byte[] content) {
        return processEngine.getRepositoryService()
                            .createDeployment()
                            .key(deploymentKey)
                            .tenantId(getTenantId())
                            .addBytes(resourceName, content)
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

    public Map<String, ActivityStatusData> getProcessInstanceActiveActivityIds(String processInstanceId) {
        ProcessInstance processInstance = getProcessInstance(processInstanceId);
        if (null == processInstance) {
            return Collections.emptyMap();
        }
        RuntimeService runtimeService = processEngine.getRuntimeService();
        List<String> positiveActiveActivityIds = runtimeService.getActiveActivityIds(processInstance.getId());

        List<Job> jobs = processEngine.getManagementService()
                                      .createDeadLetterJobQuery()
                                      .processInstanceId(processInstanceId)
                                      .list();

        List<String> negativeActiveActivityIds = jobs.stream()
                                                     .map(Job::getElementId)
                                                     .collect(Collectors.toList());

        Map<String, ActivityStatusData> statuses = new HashMap<>();
        for (String positive : positiveActiveActivityIds) {
            ActivityStatusData data = statuses.get(positive);
            if (data == null) {
                data = new ActivityStatusData();
                data.positive = 1;
                statuses.put(positive, data);
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
        }

        return statuses;
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
