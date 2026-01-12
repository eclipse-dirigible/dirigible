/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.service;

import static java.text.MessageFormat.format;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.components.engine.bpm.flowable.config.BpmProviderFlowable;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.ActivityStatusData;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.ProcessDefinitionData;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.ProcessInstanceData;
import org.eclipse.dirigible.components.ide.workspace.domain.File;
import org.eclipse.dirigible.components.ide.workspace.service.WorkspaceService;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.RepositoryNotFoundException;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.common.engine.impl.util.io.InputStreamSource;
import org.flowable.editor.language.json.converter.BpmnJsonConverter;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Processing the BPM UI Service incoming requests.
 */
@Service
public class BpmService {

    /** The Constant DIRIGIBLE_BPM_INTERNAL_SKIP_STEP. */
    public static final String DIRIGIBLE_BPM_INTERNAL_SKIP_STEP = "DIRIGIBLE_BPM_INTERNAL_SKIP_STEP";

    /** The workspace service. */
    private final WorkspaceService workspaceService;

    /** The bpm provider flowable. */
    private final BpmProviderFlowable bpmProviderFlowable;

    /**
     * Instantiates a new bpm service.
     *
     * @param workspaceService the workspace service
     * @param bpmProviderFlowable the bpm provider flowable
     */
    @Autowired
    public BpmService(WorkspaceService workspaceService, BpmProviderFlowable bpmProviderFlowable) {
        this.workspaceService = workspaceService;
        this.bpmProviderFlowable = bpmProviderFlowable;
    }

    /**
     * Gets the process definition XML by id.
     *
     * @param processDefinitionId the id
     * @return the process definition XML by id
     */
    public String getProcessDefinitionXmlById(String processDefinitionId) {
        return getBpmProviderFlowable().getProcessDefinitionXmlById(processDefinitionId);
    }

    /**
     * Gets the bpm provider flowable.
     *
     * @return the bpm provider flowable
     */
    public BpmProviderFlowable getBpmProviderFlowable() {
        return bpmProviderFlowable;
    }

    /**
     * Gets the model.
     *
     * @param workspace the workspace
     * @param project the project
     * @param path the path
     * @return the model
     * @throws JsonProcessingException the json processing exception
     */
    public ObjectNode getModel(String workspace, String project, String path) throws JsonProcessingException {
        BpmnXMLConverter bpmnXMLConverter = new BpmnXMLConverter();
        File file = getWorkspaceService().getWorkspace(workspace)
                                         .getProject(project)
                                         .getFile(path);
        if (file.exists()) {
            BpmnModel bpmnModel =
                    bpmnXMLConverter.convertToBpmnModel(new InputStreamSource(new ByteArrayInputStream(file.getContent())), true, true);
            BpmnJsonConverter bpmnJsonConverter = new BpmnJsonConverter();
            ObjectNode objectNode = bpmnJsonConverter.convertToJson(bpmnModel);
            ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
            rootNode.set("model", objectNode);
            rootNode.set("modelId",
                    JsonNodeFactory.instance.textNode(workspace + IRepository.SEPARATOR + project + IRepository.SEPARATOR + path));
            rootNode.set("name", JsonNodeFactory.instance.textNode(bpmnModel.getProcesses()
                                                                            .get(0)
                                                                            .getName()));
            rootNode.set("key", JsonNodeFactory.instance.textNode(bpmnModel.getProcesses()
                                                                           .get(0)
                                                                           .getId()));
            rootNode.set("description", JsonNodeFactory.instance.textNode(bpmnModel.getProcesses()
                                                                                   .get(0)
                                                                                   .getDocumentation()));
            rootNode.set("lastUpdated", JsonNodeFactory.instance.textNode(file.getInformation()
                                                                              .getModifiedAt()
                    + ""));
            rootNode.set("lastUpdatedBy", JsonNodeFactory.instance.textNode(file.getInformation()
                                                                                .getModifiedBy()));
            return rootNode;
        } else {
            throw new RepositoryNotFoundException(
                    format("The requested BPMN file does not exist in workspace: [{0}], project: [{1}] and path: [{2}]", workspace, project,
                            path));
        }
    }

    /**
     * Gets the workspace service.
     *
     * @return the workspace service
     */
    public WorkspaceService getWorkspaceService() {
        return workspaceService;
    }

    /**
     * Save model.
     *
     * @param workspace the workspace
     * @param project the project
     * @param path the path
     * @param payload the payload
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void saveModel(String workspace, String project, String path, String payload) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode modelNode = objectMapper.readTree(payload);
        BpmnJsonConverter bpmnJsonConverter = new BpmnJsonConverter();
        BpmnModel bpmnModel = bpmnJsonConverter.convertToBpmnModel(modelNode);
        BpmnXMLConverter bpmnXMLConverter = new BpmnXMLConverter();
        byte[] bytes = bpmnXMLConverter.convertToXML(bpmnModel);
        File file = getWorkspaceService().getWorkspace(workspace)
                                         .getProject(project)
                                         .getFile(path);
        if (!file.exists()) {
            file.create();
        }
        file.setContent(bytes);
    }

    /**
     * Gets the stencil set.
     *
     * @return the stencil set
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public JsonNode getStencilSet() throws IOException {
        InputStream in = BpmService.class.getResourceAsStream("/stencilset_bpmn.json");
        try {
            byte[] content = IOUtils.toByteArray(in);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode modelNode = objectMapper.readTree(content);
            // return new String(content, StandardCharsets.UTF_8);
            return modelNode;
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     * Gets the process definitions.
     *
     * @return the process definitions
     */
    public List<ProcessDefinitionData> getProcessDefinitions(Optional<String> key) {
        List<ProcessDefinition> processDefinitions = bpmProviderFlowable.getProcessDefinitions(key);

        return processDefinitions.stream()
                                 .map(this::mapProcessDefinition)
                                 .toList();
    }

    /**
     * Map process definition.
     *
     * @param processDefinition the process definition
     * @return the process definition data
     */
    private ProcessDefinitionData mapProcessDefinition(ProcessDefinition processDefinition) {
        ProcessDefinitionData processDefinitionData = new ProcessDefinitionData();

        processDefinitionData.setCategory(processDefinition.getCategory());
        processDefinitionData.setDeploymentId(processDefinition.getDeploymentId());
        processDefinitionData.setDiagram(processDefinition.getDiagramResourceName());
        processDefinitionData.setId(processDefinition.getId());
        processDefinitionData.setKey(processDefinition.getKey());
        processDefinitionData.setName(processDefinition.getName());
        processDefinitionData.setResourceName(processDefinition.getResourceName());
        processDefinitionData.setTennantId(processDefinition.getTenantId());
        processDefinitionData.setVersion(processDefinition.getVersion());

        return processDefinitionData;

    }

    /**
     * Gets the process definition by key.
     *
     * @param key the key
     * @return the process definition by key
     */
    public ProcessDefinitionData getProcessDefinitionByKey(String key) {
        ProcessDefinition processDefinition = bpmProviderFlowable.getProcessDefinitionByKey(key);
        return mapProcessDefinition(processDefinition);
    }

    /**
     * Gets the process definition by id.
     *
     * @param id the id
     * @return the process definition by id
     */
    public ProcessDefinitionData getProcessDefinitionById(String id) {
        ProcessDefinition processDefinition = bpmProviderFlowable.getProcessDefinitionById(id);
        return mapProcessDefinition(processDefinition);
    }

    /**
     * Gets the process instances.
     *
     * @return the process instances
     */
    public List<ProcessInstanceData> getProcessInstances(Optional<String> key, Optional<String> businessKey) {
        List<ProcessInstance> processInstances = bpmProviderFlowable.getProcessInstances(key, businessKey);
        return processInstances.stream()
                               .map(this::mapProcessInstance)
                               .toList();
    }

    /**
     * Map process instance.
     *
     * @param processInstance the process instance
     * @return the process instance data
     */
    private ProcessInstanceData mapProcessInstance(ProcessInstance processInstance) {
        ProcessInstanceData processInstanceData = new ProcessInstanceData();
        processInstanceData.setBusinessKey(processInstance.getBusinessKey());
        processInstanceData.setBusinessStatus(processInstance.getBusinessStatus());
        processInstanceData.setDeploymentId(processInstance.getDeploymentId());
        processInstanceData.setId(processInstance.getId());
        processInstanceData.setProcessInstanceId(processInstance.getProcessInstanceId());
        processInstanceData.setName(processInstance.getName());
        processInstanceData.setProcessDefinitionId(processInstance.getProcessDefinitionId());
        processInstanceData.setProcessDefinitionKey(processInstance.getProcessDefinitionKey());
        processInstanceData.setProcessDefinitionName(processInstance.getProcessDefinitionName());
        processInstanceData.setProcessDefinitionVersion(processInstance.getProcessDefinitionVersion());
        processInstanceData.setTenantId(processInstance.getTenantId());
        processInstanceData.setStartTime(processInstance.getStartTime());
        processInstanceData.setReferenceId(processInstance.getReferenceId());
        processInstanceData.setCallbackId(processInstance.getCallbackId());
        processInstanceData.setActivityId(processInstance.getActivityId());
        return processInstanceData;
    }

    /**
     * Start process instance.
     *
     * @param processDefinitionKey the process definition key
     * @param businessKey the business key
     * @param parameters the parameters
     * @return the process instance id
     */
    public String startProcess(String processDefinitionKey, String businessKey, String parameters) {
        return bpmProviderFlowable.startProcess(processDefinitionKey, businessKey, parameters);
    }

    public String startProcess(String processDefinitionKey, String businessKey, Map<String, Object> parameters) {
        return bpmProviderFlowable.startProcess(processDefinitionKey, businessKey, parameters);
    }

    /**
     * Gets the process instance by key.
     *
     * @param id the id
     * @return the process instance
     */
    public ProcessInstanceData getProcessInstanceById(String id) {
        ProcessInstance processInstance = bpmProviderFlowable.getProcessInstance(id);
        return mapProcessInstance(processInstance);
    }

    /**
     * Gets the completed historic process instances.
     *
     * @return the process instances
     */
    public List<HistoricProcessInstance> getCompletedProcessInstances(Optional<String> definitionKey, Optional<String> businessKey) {
        return bpmProviderFlowable.getCompletedProcessInstances(definitionKey, businessKey);
    }

    /**
     * Get all jobs that exhausted their retry attempts and are considered "dead".
     *
     * @param processInstanceId the process instance id
     * @return list of jobs
     */
    public List<Job> getDeadLetterJobs(String processInstanceId) {
        return bpmProviderFlowable.getDeadLetterJobs(processInstanceId);
    }

    /**
     * Retry dead-letter job by moving it back to active state.
     *
     * @param job the job instance
     * @param numberOfRetries desired number of retries
     */
    public void retryDeadLetterJob(Job job, int numberOfRetries) {
        bpmProviderFlowable.retryDeadLetterJob(job.getId(), numberOfRetries);
    }

    /**
     * Add or update variable in the process context.
     *
     * @param processInstanceId the process instance id
     * @param key variable key
     * @param value variable value
     */
    public void addProcessInstanceVariable(String processInstanceId, String key, String value) {
        bpmProviderFlowable.addProcessInstanceVariable(processInstanceId, key, value);
    }

    /**
     * Remove variable from the execution context.
     *
     * @param executionId the execution id
     * @param variableName variable name
     */
    public void removeVariable(String executionId, String variableName) {
        bpmProviderFlowable.removeVariable(executionId, variableName);
    }

    public List<IdentityLink> getTaskIdentityLinks(String taskId) {
        return bpmProviderFlowable.getTaskService()
                                  .getTaskIdentityLinks(taskId);
    }

    public Map<String, Object> getTaskVariables(String taskId) {
        return bpmProviderFlowable.getTaskService()
                                  .getTaskVariables(taskId);
    }

    public void claimTask(String taskId, String userId) {
        bpmProviderFlowable.getTaskService()
                           .claimTask(taskId, userId);
    }

    public void unclaimTask(String taskId) {
        bpmProviderFlowable.getTaskService()
                           .unclaimTask(taskId);
    }

    public void completeTask(String taskId, Map<String, Object> variables) {
        bpmProviderFlowable.getTaskService()
                           .completeTask(taskId, variables);
    }

    public List<HistoricVariableInstance> getProcessHistoricInstanceVariables(String processInstanceId) {
        return bpmProviderFlowable.getProcessHistoricInstanceVariables(processInstanceId);
    }

    public List<VariableInstance> getProcessInstanceVariables(String processInstanceId, Optional<String> variableName) {
        return bpmProviderFlowable.getProcessInstanceVariables(processInstanceId, variableName);
    }

    public Optional<byte[]> getProcessDefinitionImage(String processDefinitionKey) throws IOException {
        return bpmProviderFlowable.getProcessDefinitionImage(processDefinitionKey);
    }

    public Optional<byte[]> getProcessInstanceImage(String processInstanceId) {
        return bpmProviderFlowable.getProcessInstanceImage(processInstanceId);
    }

    public Deployment deployProcess(String deploymentKey, String resourceName, String content) {
        return bpmProviderFlowable.deployProcess(deploymentKey, resourceName, content);
    }

    public ProcessDefinition getProcessDefinitionByDeploymentId(String deploymentId) {
        return bpmProviderFlowable.getProcessDefinitionByDeploymentId(deploymentId);
    }

    public List<Deployment> getDeploymentsByKey(String deploymentKey) {
        return bpmProviderFlowable.getDeploymentsByKey(deploymentKey);
    }

    public void deleteDeployment(String deploymentId) {
        bpmProviderFlowable.deleteDeployment(deploymentId);
    }

    public List<Task> findTasks(String processInstanceId, PrincipalType type) {
        return bpmProviderFlowable.getTaskService()
                                  .findTasks(processInstanceId, type);
    }

    public List<Task> findTasks(PrincipalType type) {
        return bpmProviderFlowable.getTaskService()
                                  .findTasks(type);
    }

    public long processDefinitionsCount() {
        return bpmProviderFlowable.processDefinitionsCount();
    }

    public long getProcessInstancesCount() {
        return bpmProviderFlowable.getProcessInstancesCount();
    }

    public long getFinishedHistoricProcessInstancesCount() {
        return bpmProviderFlowable.getFinishedHistoricProcessInstancesCount();
    }

    public long getTasksCount() {
        return bpmProviderFlowable.getTaskService()
                                  .getTasksCount();
    }

    public long getTotalCompletedTasksCount() {
        return bpmProviderFlowable.getTotalCompletedTasksCount();
    }

    public long getCompletedTasksForToday() {
        return bpmProviderFlowable.getCompletedTasksForToday();
    }

    public long getCompletedActivities() {
        return bpmProviderFlowable.getCompletedActivities();
    }

    /**
     * Gets the process instance active activity ids by instance id.
     *
     * @param processInstanceId the instance id
     * @return the process instance active activity ids
     */
    public Map<String, ActivityStatusData> getProcessInstanceActiveActivityIds(String processInstanceId) {
        return bpmProviderFlowable.getProcessInstanceActiveActivityIds(processInstanceId);
    }

    /**
     * Gets the process definition active activity ids by definition id.
     *
     * @param processDefinitionId the definition id
     * @return the process definition active activity ids
     */
    public Map<String, ActivityStatusData> getProcessDefinitionActiveActivityIds(String processDefinitionId) {
        return bpmProviderFlowable.getProcessDefinitionActiveActivityIds(processDefinitionId);
    }
}
