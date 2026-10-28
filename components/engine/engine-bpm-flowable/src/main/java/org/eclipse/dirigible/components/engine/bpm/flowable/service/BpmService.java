/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.commons.api.helpers.LogSanitizer;
import org.eclipse.dirigible.components.engine.bpm.flowable.config.BpmProviderFlowable;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.ActivityStatusData;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.ProcessDefinitionData;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.ProcessInstanceData;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.ProcessLabelKeys;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.ValidationErrorDTO;
import org.eclipse.dirigible.components.ide.workspace.domain.File;
import org.eclipse.dirigible.components.ide.workspace.service.WorkspaceService;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.RepositoryNotFoundException;
import org.flowable.common.engine.impl.history.HistoryLevel;
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
import org.flowable.validation.ProcessValidator;
import org.flowable.validation.ProcessValidatorFactory;
import org.flowable.validation.ValidationError;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
// Jackson 2 ON PURPOSE (#7790): Flowable's BpmnJsonConverter takes and returns
// com.fasterxml.jackson.databind nodes, so the model this class exchanges with it must be 2.x. The
// rest of the platform is on tools.jackson; this file and BpmFlowableEndpoint are the Flowable
// boundary and move when Flowable does.
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

    private static final Logger LOGGER = LoggerFactory.getLogger(BpmService.class);

    /** The variable a completion names its action by, which every task may write. */
    private static final String ACTION_VARIABLE = "action";

    /** The prefix of the platform's own process variables, which no completion may write. */
    private static final String SYSTEM_VARIABLE_PREFIX = "__";

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
    public String getModel(String workspace, String project, String path) throws JsonProcessingException {
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
            return new ObjectMapper().writeValueAsString(rootNode);
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
     * Validates a BPMN model posted in the modeler's JSON form, running Flowable's default process
     * validator over it so the editor can report problems - an unresolved service-task implementation,
     * a missing sequence-flow condition - before publish, instead of at the synchronizer's FAILED.
     *
     * @param payload the modeler's JSON model (the same shape {@link #saveModel} receives)
     * @return the validation problems, empty when the model is valid
     * @throws IOException when the payload is not readable JSON
     */
    public List<ValidationErrorDTO> validateModel(String payload) throws IOException {
        JsonNode modelNode = new ObjectMapper().readTree(payload);
        BpmnModel bpmnModel = new BpmnJsonConverter().convertToBpmnModel(modelNode);
        ProcessValidator processValidator = new ProcessValidatorFactory().createDefaultProcessValidator();
        return processValidator.validate(bpmnModel)
                               .stream()
                               .map(BpmService::toValidationErrorDTO)
                               .toList();
    }

    private static ValidationErrorDTO toValidationErrorDTO(ValidationError error) {
        ValidationErrorDTO dto = new ValidationErrorDTO();
        dto.setActivityId(error.getActivityId());
        dto.setActivityName(error.getActivityName());
        dto.setProblem(error.getProblem());
        dto.setDefaultDescription(error.getDefaultDescription());
        dto.setValidatorSetName(error.getValidatorSetName());
        dto.setWarning(error.isWarning());
        return dto;
    }

    /**
     * Gets the stencil set.
     *
     * @return the stencil set
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public String getStencilSet() throws IOException {
        InputStream in = BpmService.class.getResourceAsStream("/stencilset_bpmn.json");
        try {
            return IOUtils.toString(in, java.nio.charset.StandardCharsets.UTF_8);
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
        processDefinitionData.setSuspended(processDefinition.isSuspended());

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
     * Where a process declares its display names are translated, empty when it declares nothing.
     *
     * @param processDefinitionId the process definition id
     * @return the declared keys
     */
    public Optional<ProcessLabelKeys> getProcessLabelKeys(String processDefinitionId) {
        return bpmProviderFlowable.getProcessLabelKeys(processDefinitionId);
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
        return getProcessInstances(key, businessKey, null);
    }

    /**
     * The running instances, newest first and at most {@code limit} of them when a limit is given
     * (dirigible #7982).
     *
     * @param key the process definition key, if any
     * @param businessKey a business key fragment, if any
     * @param limit the most instances to return; null = all
     * @return the instances
     */
    public List<ProcessInstanceData> getProcessInstances(Optional<String> key, Optional<String> businessKey, Integer limit) {
        List<ProcessInstance> processInstances = bpmProviderFlowable.getProcessInstances(key, businessKey, limit);
        // Resolved for the whole listing at once: per instance it is three statements, and the listing
        // is unbounded - the Monitoring shell polls it every 30 s with no key (#7250).
        Map<String, List<String>> activityIds = bpmProviderFlowable.getProcessInstanceActivityIds(processInstances);

        return processInstances.stream()
                               .map(processInstance -> mapProcessInstance(processInstance,
                                       activityIds.getOrDefault(processInstance.getId(), List.of())))
                               .toList();
    }

    /**
     * Map process instance.
     *
     * <p>
     * The activity id is resolved rather than read off the instance: Flowable leaves it unset on a root
     * process-instance execution, which is what every running instance is, so the field was always
     * null. It stays null while an instance is forked across several activities, the field being
     * singular; {@code getProcessInstanceActiveActivityIds} answers for a fan-out.
     *
     * @param processInstance the process instance
     * @param activityIds the activity ids the instance occupies, already resolved
     * @return the process instance data
     */
    private ProcessInstanceData mapProcessInstance(ProcessInstance processInstance, List<String> activityIds) {
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
        processInstanceData.setActivityId(singleActivityId(activityIds));
        return processInstanceData;
    }

    /**
     * The single activity a process instance sits on, or null when it sits on none or on several.
     *
     * @param activityIds the activity ids the instance occupies
     * @return the activity id, or null
     */
    private static String singleActivityId(List<String> activityIds) {
        return activityIds.size() == 1 ? activityIds.get(0) : null;
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
        return mapProcessInstance(processInstance, bpmProviderFlowable.getProcessInstanceActivityIds(processInstance));
    }

    /**
     * Gets the completed historic process instances.
     *
     * @return the process instances
     */
    public List<HistoricProcessInstance> getCompletedProcessInstances(Optional<String> definitionKey, Optional<String> businessKey) {
        return getCompletedProcessInstances(definitionKey, businessKey, null);
    }

    /**
     * The finished instances, most recently ended first and at most {@code limit} of them (dirigible
     * #7982).
     *
     * @param definitionKey the process definition key, if any
     * @param businessKey a business key fragment, if any
     * @param limit the most instances to return; null = all
     * @return the instances
     */
    public List<HistoricProcessInstance> getCompletedProcessInstances(Optional<String> definitionKey, Optional<String> businessKey,
            Integer limit) {
        return bpmProviderFlowable.getCompletedProcessInstances(definitionKey, businessKey, limit);
    }

    /**
     * Get all jobs that exhausted their retry attempts and are considered "dead".
     *
     * @param processInstanceId the process instance id
     * @return list of jobs
     */
    public List<Job> getDeadLetterJobs(String processInstanceId) {
        return getDeadLetterJobs(processInstanceId, null);
    }

    /**
     * A process instance's dead-letter jobs, at most {@code limit} of them (dirigible #7982).
     *
     * @param processInstanceId the process instance
     * @param limit the most jobs to return; null = all
     * @return the jobs
     */
    public List<Job> getDeadLetterJobs(String processInstanceId, Integer limit) {
        return bpmProviderFlowable.getDeadLetterJobs(processInstanceId, limit);
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

    /**
     * Whether the task is still active in the current tenant - i.e. neither completed nor deleted.
     *
     * @param taskId the task id
     * @return true while the task exists
     */
    public boolean isTaskActive(String taskId) {
        return bpmProviderFlowable.isTaskActive(taskId);
    }

    public List<IdentityLink> getTaskIdentityLinks(String taskId) {
        return bpmProviderFlowable.getTaskService()
                                  .getTaskIdentityLinks(taskId);
    }

    /**
     * The identity links of a task a listing just returned - the same read without the existence check
     * that listing already made (issue #7232).
     *
     * @param taskId the id of a task the caller just listed
     * @return its identity links
     */
    public List<IdentityLink> getListedTaskIdentityLinks(String taskId) {
        return bpmProviderFlowable.getTaskService()
                                  .getListedTaskIdentityLinks(taskId);
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

    /**
     * The actions a task's form completes it with, as its user task declares them.
     *
     * @param taskId the task id
     * @return the declared actions, empty when the task declares none
     */
    public List<String> getTaskActions(String taskId) {
        return bpmProviderFlowable.getTaskActions(taskId);
    }

    /**
     * Completes the task with the variables its completion may write. A {@code __}-prefixed variable is
     * the platform's own (the locators the Inbox presents the task's record by) and is never taken. A
     * task that declares what it writes ({@link BpmProviderFlowable#getTaskWrites}) takes only those
     * and its {@code action}: an intent-generated task, whose delegates find the record by its key
     * variable, so a completion carrying another record's key wrote the process's changes to that
     * record, and a field its form does not offer was planted for a later step (issue #8094).
     *
     * @param taskId the task id
     * @param variables the completion's variables, as the caller sent them
     */
    public void completeTask(String taskId, Map<String, Object> variables) {
        bpmProviderFlowable.getTaskService()
                           .completeTask(taskId, writableVariables(taskId, variables));
    }

    private Map<String, Object> writableVariables(String taskId, Map<String, Object> variables) {
        if (variables == null) {
            return null;
        }
        Optional<List<String>> writes = bpmProviderFlowable.getTaskWrites(taskId);
        Map<String, Object> writable = new HashMap<>();
        variables.forEach((name, value) -> {
            boolean declared = writes.map(names -> ACTION_VARIABLE.equals(name) || names.contains(name))
                                     .orElse(true);
            if (declared && !name.startsWith(SYSTEM_VARIABLE_PREFIX)) {
                writable.put(name, value);
            }
        });
        List<String> ignored = variables.keySet()
                                        .stream()
                                        .filter(name -> !writable.containsKey(name))
                                        .toList();
        if (!ignored.isEmpty()) {
            LOGGER.debug("Completion of task [{}] ignored the variables it may not write: {}", taskId, LogSanitizer.sanitize(ignored));
        }
        return writable;
    }

    public List<HistoricVariableInstance> getProcessHistoricInstanceVariables(String processInstanceId) {
        return bpmProviderFlowable.getProcessHistoricInstanceVariables(processInstanceId);
    }

    public List<VariableInstance> getProcessInstanceVariables(String processInstanceId, Optional<String> variableName) {
        return getProcessInstanceVariables(processInstanceId, variableName, null);
    }

    /**
     * A process instance's variables, by name and at most {@code limit} of them (dirigible #7982).
     *
     * @param processInstanceId the process instance
     * @param variableName a variable name fragment, if any
     * @param limit the most variables to return; null = all
     * @return the variables
     */
    public List<VariableInstance> getProcessInstanceVariables(String processInstanceId, Optional<String> variableName, Integer limit) {
        return bpmProviderFlowable.getProcessInstanceVariables(processInstanceId, variableName, limit);
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

    public boolean retireDeployment(String deploymentId) {
        return bpmProviderFlowable.retireDeployment(deploymentId);
    }

    public boolean isDeploymentRetired(String deploymentId) {
        return bpmProviderFlowable.isDeploymentRetired(deploymentId);
    }

    public boolean isProcessInstanceKnown(String processInstanceId) {
        return bpmProviderFlowable.isProcessInstanceKnown(processInstanceId);
    }

    public HistoryLevel getHistoryLevel() {
        return bpmProviderFlowable.getHistoryLevel();
    }

    public List<Task> findTasks(String processInstanceId, PrincipalType type) {
        return bpmProviderFlowable.getTaskService()
                                  .findTasks(processInstanceId, type);
    }

    public List<Task> findTasks(PrincipalType type) {
        return bpmProviderFlowable.getTaskService()
                                  .findTasks(type);
    }

    /**
     * The same tasks as {@link #findTasks(String, PrincipalType)}, each carrying its process variables.
     *
     * @param processInstanceId the process instance to list the tasks of
     * @param type the principal the tasks are addressed to
     * @return the tasks, with their process variables loaded
     */
    public List<Task> findTasksWithProcessVariables(String processInstanceId, PrincipalType type) {
        return bpmProviderFlowable.getTaskService()
                                  .findTasksWithProcessVariables(processInstanceId, type);
    }

    /**
     * The same tasks as {@link #findTasks(PrincipalType)}, each carrying its process variables - for a
     * listing that reads them, which would otherwise cost one variable query per task (issue #7141).
     *
     * @param type the principal the tasks are addressed to
     * @return the tasks, with their process variables loaded
     */
    public List<Task> findTasksWithProcessVariables(PrincipalType type) {
        return bpmProviderFlowable.getTaskService()
                                  .findTasksWithProcessVariables(type);
    }

    /**
     * One page of the tasks addressed to the principal, newest first, each carrying its process
     * variables and identity links (issue #7977).
     *
     * @param type the principal the tasks are addressed to
     * @param offset the number of tasks to skip
     * @param limit the page size
     * @return the page
     */
    public List<Task> findTasksWithProcessVariables(PrincipalType type, int offset, int limit) {
        return bpmProviderFlowable.getTaskService()
                                  .findTasksWithProcessVariables(type, offset, limit);
    }

    /**
     * How many tasks are addressed to the principal, without listing them (issue #7977).
     *
     * @param type the principal the tasks are addressed to
     * @return the number of tasks
     */
    public long countTasks(PrincipalType type) {
        return bpmProviderFlowable.getTaskService()
                                  .countTasks(type);
    }

    /**
     * The process instances a task listing names, read together (issue #7977) - keyed by instance id.
     * Only what a listing row shows is mapped: the activity id, which costs statements of its own, is
     * left unset. An instance that has ended since the listing was taken is absent from the map.
     *
     * @param processInstanceIds the ids the listing's tasks belong to
     * @return the running instances by id
     */
    public Map<String, ProcessInstanceData> getListedProcessInstances(Collection<String> processInstanceIds) {
        if (processInstanceIds.isEmpty()) {
            return Map.of();
        }
        Map<String, ProcessInstanceData> instances = new HashMap<>();
        for (ProcessInstance processInstance : bpmProviderFlowable.getProcessInstances(processInstanceIds)) {
            instances.put(processInstance.getId(), mapProcessInstance(processInstance, List.of()));
        }
        return instances;
    }

    public long countTasksByAssignee(String assignee) {
        return bpmProviderFlowable.getTaskService()
                                  .countTasksByAssignee(assignee);
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
