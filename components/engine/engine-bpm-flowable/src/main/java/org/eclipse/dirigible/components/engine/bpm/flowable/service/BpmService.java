/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.ProcessDefinitionData;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.ProcessInstanceData;
import org.eclipse.dirigible.components.engine.bpm.flowable.provider.BpmProviderFlowable;
import org.eclipse.dirigible.components.ide.workspace.domain.File;
import org.eclipse.dirigible.components.ide.workspace.service.WorkspaceService;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.RepositoryNotFoundException;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.common.engine.impl.util.io.InputStreamSource;
import org.flowable.editor.language.json.converter.BpmnJsonConverter;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.job.api.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(BpmService.class);

    /** The Constant DIRIGIBLE_BPM_INTERNAL_SKIP_STEP. */
    public static final String DIRIGIBLE_BPM_INTERNAL_SKIP_STEP = "DIRIGIBLE_BPM_INTERNAL_SKIP_STEP";

    /** The workspace service. */
    private WorkspaceService workspaceService;

    /** The bpm provider flowable. */
    private BpmProviderFlowable bpmProviderFlowable;

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
     * Gets the workspace service.
     *
     * @return the workspace service
     */
    public WorkspaceService getWorkspaceService() {
        return workspaceService;
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
            // String json = objectMapper.writeValueAsString(rootNode);
            // return json;
            return rootNode;
        } else {
            throw new RepositoryNotFoundException(
                    format("The requested BPMN file does not exist in workspace: [{0}], project: [{1}] and path: [{2}]", workspace, project,
                            path));
        }
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
    public List<ProcessDefinitionData> getProcessDefinitions() {
        ProcessEngine processEngine = ((ProcessEngine) getBpmProviderFlowable().getProcessEngine());

        List<ProcessDefinition> processDefinitions = processEngine.getRepositoryService()
                                                                  .createProcessDefinitionQuery()
                                                                  .list();

        List<ProcessDefinitionData> results = new ArrayList<ProcessDefinitionData>();
        for (ProcessDefinition processDefinition : processDefinitions) {
            ProcessDefinitionData processDefinitionData = mapProcessDefinition(processDefinition);
            results.add(processDefinitionData);
        }
        return results;
    }

    /**
     * Gets the process definition by key.
     *
     * @param key the key
     * @return the process definition by key
     */
    public ProcessDefinitionData getProcessDefinitionByKey(String key) {
        ProcessEngine processEngine = ((ProcessEngine) getBpmProviderFlowable().getProcessEngine());

        List<ProcessDefinition> processDefinitions = processEngine.getRepositoryService()
                                                                  .createProcessDefinitionQuery()
                                                                  .processDefinitionKey(key)
                                                                  .list();

        for (ProcessDefinition processDefinition : processDefinitions) {
            ProcessDefinitionData processDefinitionData = mapProcessDefinition(processDefinition);
            return processDefinitionData;
        }
        return null;
    }

    /**
     * Gets the process definition by id.
     *
     * @param id the id
     * @return the process definition by id
     */
    public ProcessDefinitionData getProcessDefinitionById(String id) {
        ProcessEngine processEngine = ((ProcessEngine) getBpmProviderFlowable().getProcessEngine());

        List<ProcessDefinition> processDefinitions = processEngine.getRepositoryService()
                                                                  .createProcessDefinitionQuery()
                                                                  .processDefinitionId(id)
                                                                  .list();

        for (ProcessDefinition processDefinition : processDefinitions) {
            ProcessDefinitionData processDefinitionData = mapProcessDefinition(processDefinition);
            return processDefinitionData;
        }
        return null;
    }

    /**
     * Gets the process instances.
     *
     * @return the process instances
     */
    public List<ProcessInstanceData> getProcessInstances(Optional<String> key, Optional<String> businessKey) {
        ProcessEngine processEngine = ((ProcessEngine) getBpmProviderFlowable().getProcessEngine());
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

        List<ProcessInstance> processInstances = processInstanceQuery.list();

        List<ProcessInstanceData> results = new ArrayList<ProcessInstanceData>();
        for (ProcessInstance processInstance : processInstances) {
            ProcessInstanceData processInstanceData = mapProcessInstance(processInstance);
            results.add(processInstanceData);
        }
        return results;
    }

    /**
     * Gets the process instance by key.
     *
     * @param key the key
     * @return the process instance
     */
    public List<ProcessInstanceData> getProcessInstanceByKey(String key) {
        ProcessEngine processEngine = ((ProcessEngine) getBpmProviderFlowable().getProcessEngine());

        List<ProcessInstance> processInstances = processEngine.getRuntimeService()
                                                              .createProcessInstanceQuery()
                                                              .processDefinitionKey(key)
                                                              .list();

        List<ProcessInstanceData> results = new ArrayList<ProcessInstanceData>();
        for (ProcessInstance processInstance : processInstances) {
            ProcessInstanceData processInstanceData = mapProcessInstance(processInstance);
            results.add(processInstanceData);
        }
        return results;
    }

    /**
     * Gets the process instance by key.
     *
     * @param id the id
     * @return the process instance
     */
    public ProcessInstanceData getProcessInstanceById(String id) {
        ProcessEngine processEngine = ((ProcessEngine) getBpmProviderFlowable().getProcessEngine());

        List<ProcessInstance> processInstances = processEngine.getRuntimeService()
                                                              .createProcessInstanceQuery()
                                                              .processInstanceId(id)
                                                              .list();

        for (ProcessInstance processInstance : processInstances) {
            ProcessInstanceData processInstanceData = mapProcessInstance(processInstance);
            return processInstanceData;
        }
        return null;
    }

    /**
     * Gets the process instance by key.
     *
     * @param businessKey the business key
     * @return the process instance
     */
    public List<ProcessInstanceData> getProcessInstanceByBusinessKey(String businessKey) {
        ProcessEngine processEngine = ((ProcessEngine) getBpmProviderFlowable().getProcessEngine());

        List<ProcessInstance> processInstances = processEngine.getRuntimeService()
                                                              .createProcessInstanceQuery()
                                                              .processInstanceBusinessKeyLike("%" + businessKey + "%")
                                                              .list();

        List<ProcessInstanceData> results = new ArrayList<ProcessInstanceData>();
        for (ProcessInstance processInstance : processInstances) {
            ProcessInstanceData processInstanceData = mapProcessInstance(processInstance);
            results.add(processInstanceData);
        }
        return results;
    }

    /**
     * Gets the completed historic process instances.
     *
     * @return the process instances
     */
    public List<HistoricProcessInstance> getCompletedProcessInstances(Optional<String> definitionKey, Optional<String> businessKey) {
        HistoricProcessInstanceQuery historicProcessInstanceQuery = getBpmProviderFlowable().getProcessEngine()
                                                                                            .getHistoryService()
                                                                                            .createHistoricProcessInstanceQuery();

        if (definitionKey.isPresent() && !definitionKey.get()
                                                       .isEmpty()) {
            historicProcessInstanceQuery.processDefinitionKey(definitionKey.get());
        }

        if (businessKey.isPresent() && !businessKey.get()
                                                   .isEmpty()) {
            historicProcessInstanceQuery.processInstanceBusinessKeyLike("%" + businessKey.get() + "%");
        }
        return historicProcessInstanceQuery.finished()
                                           .list();
    }

    /**
     * Gets the completed historic process instances by business key pattern.
     *
     * @param businessKey the business key
     * @return the process instances
     */
    public List<HistoricProcessInstance> getCompletedProcessInstances(String businessKey) {
        return getBpmProviderFlowable().getProcessEngine()
                                       .getHistoryService()
                                       .createHistoricProcessInstanceQuery()
                                       .processInstanceBusinessKeyLike("%" + businessKey + "%")
                                       .finished()
                                       .list();
    }

    /**
     * Get all jobs that exhausted their retry attempts and are considered "dead".
     *
     * @param processInstanceId the process instance id
     * @return list of jobs
     */
    public List<Job> getDeadLetterJobs(String processInstanceId) {
        return getBpmProviderFlowable().getProcessEngine()
                                       .getManagementService()
                                       .createDeadLetterJobQuery()
                                       .processInstanceId(processInstanceId)
                                       .list();
    }

    /**
     * Retry dead-letter job by moving it back to active state.
     *
     * @param job the job instance
     * @param numberOfRetries desired number of retries
     */
    public void retryDeadLetterJob(Job job, int numberOfRetries) {
        getBpmProviderFlowable().getProcessEngine()
                                .getManagementService()
                                .moveDeadLetterJobToExecutableJob(job.getId(), numberOfRetries);
    }

    /**
     * Add or update variable in the process context.
     *
     * @param processInstanceId the process instance id
     * @param key variable key
     * @param value variable value
     */
    public void addProcessInstanceVariable(String processInstanceId, String key, String value) {
        getBpmProviderFlowable().getProcessEngine()
                                .getRuntimeService()
                                .setVariable(processInstanceId, key, value);
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
        processInstanceData.setName(processInstance.getName());
        processInstanceData.setProcessDefinitionId(processInstance.getProcessDefinitionId());
        processInstanceData.setProcessDefinitionKey(processInstance.getProcessDefinitionKey());
        processInstanceData.setProcessDefinitionName(processInstance.getProcessDefinitionName());
        processInstanceData.setProcessDefinitionVersion(processInstance.getProcessDefinitionVersion());
        processInstanceData.setTenantId(processInstance.getTenantId());
        return processInstanceData;
    }

}
