/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.endpoint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import org.eclipse.dirigible.components.api.security.UserFacade;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.*;
import org.eclipse.dirigible.components.engine.bpm.flowable.provider.BpmProviderFlowable;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.BpmService;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.task.TaskQueryExecutor;
import org.eclipse.dirigible.components.ide.workspace.service.WorkspaceService;
import org.eclipse.dirigible.repository.api.RepositoryNotFoundException;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import static java.text.MessageFormat.format;
import static org.eclipse.dirigible.components.engine.bpm.flowable.dto.ActionData.Action.RETRY;
import static org.eclipse.dirigible.components.engine.bpm.flowable.dto.ActionData.Action.SKIP;
import static org.eclipse.dirigible.components.engine.bpm.flowable.dto.TaskActionData.TaskAction.*;
import static org.eclipse.dirigible.components.engine.bpm.flowable.service.BpmService.DIRIGIBLE_BPM_INTERNAL_SKIP_STEP;
import static org.eclipse.dirigible.components.engine.bpm.flowable.service.task.TaskQueryExecutor.Type;

/**
 * Front facing REST service serving the BPM related resources and operations.
 */
@CrossOrigin
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_BPM)
public class BpmFlowableEndpoint extends BaseEndpoint {

    /**
     * The Constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(BpmFlowableEndpoint.class);

    /**
     * The bpm provider flowable.
     */
    @Autowired
    private BpmProviderFlowable bpmProviderFlowable;

    /**
     * The bpm service.
     */
    @Autowired
    private BpmService bpmService;

    /**
     * The workspace service.
     */
    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private TaskQueryExecutor taskQueryExecutor;

    /**
     * Get the BPM model source.
     *
     * @param workspace the workspace
     * @param project the project
     * @param path the path
     * @return the response
     * @throws JsonProcessingException exception
     */
    @GetMapping(value = "/models/{workspace}/{project}/{*path}", produces = "application/json")
    public ResponseEntity<ObjectNode> getModel(@PathVariable("workspace") String workspace, @PathVariable("project") String project,
            @PathVariable("path") String path) throws JsonProcessingException {

        path = sanitizePath(path);

        ObjectNode model = getBpmService().getModel(workspace, project, path);

        if (model == null) {
            String error = format("Model in workspace: {0} and project {1} with path {2} does not exist.", workspace, project, path);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, error);
        }
        return ResponseEntity.ok(model);
    }

    /**
     * Gets the bpm service.
     *
     * @return the bpm service
     */
    public BpmService getBpmService() {
        return bpmService;
    }

    /**
     * Sanitize path.
     *
     * @param path the path
     * @return the string
     */
    private String sanitizePath(String path) {
        if (path.indexOf("?") > 0) {
            path = path.substring(0, path.indexOf("?"));
        } else if (path.indexOf("&") > 0) {
            path = path.substring(0, path.indexOf("&"));
        } else if (path.indexOf("/") == 0) {
            path = path.substring(1);
        }
        return path;
    }

    /**
     * Save the BPM model source.
     *
     * @param workspace the workspace
     * @param project the project
     * @param path the path
     * @param payload the payload
     * @return the response
     * @throws URISyntaxException in case of an error
     * @throws IOException exception
     */
    @PostMapping(value = "/models/{workspace}/{project}/{*path}", produces = "application/json")
    public ResponseEntity<URI> saveModel(@PathVariable("workspace") String workspace, @PathVariable("project") String project,
            @PathVariable("path") String path, @RequestParam("json_xml") String payload) throws URISyntaxException, IOException {

        path = sanitizePath(path);

        getBpmService().saveModel(workspace, project, path, payload);

        return ResponseEntity.ok(getWorkspaceService().getURI(workspace, project, path));
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
     * Get the Stencil-Set.
     *
     * @return the response
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @GetMapping(value = "/stencil-sets", produces = "application/json")
    public ResponseEntity<JsonNode> getStencilSet() throws IOException {

        JsonNode stencilSets = getBpmService().getStencilSet();

        if (stencilSets == null) {
            String error = "Stencil Sets definition does not exist.";
            throw new RepositoryNotFoundException(error);
        }
        return ResponseEntity.ok(stencilSets);
    }

    /**
     * Gets the process definitions.
     *
     * @return the process definitions
     */
    @GetMapping(value = "/bpm-processes/definitions")
    public ResponseEntity<List<ProcessDefinitionData>> getProcessDefinitions() {
        return ResponseEntity.ok(getBpmService().getProcessDefinitions());
    }

    /**
     * Gets the process definitions.
     *
     * @param id the id
     * @param key the key
     * @return the process definitions
     */
    @GetMapping(value = "/bpm-processes/definition")
    public ResponseEntity<ProcessDefinitionData> getProcessDefinition(@Nullable @RequestParam("id") Optional<String> id,
            @Nullable @RequestParam("key") Optional<String> key) {
        if (key.isPresent()) {
            return ResponseEntity.ok(getBpmService().getProcessDefinitionByKey(key.get()));
        } else if (id.isPresent()) {
            return ResponseEntity.ok(getBpmService().getProcessDefinitionById(id.get()));
        }
        return null;
    }

    /**
     * Gets the processes keys.
     *
     * @param businessKey the business key
     * @param key the key
     * @return the processes keys
     */
    @GetMapping(value = "/bpm-processes/instances")
    public ResponseEntity<List<ProcessInstanceData>> getProcessesInstances(@Nullable @RequestParam("id") Optional<String> businessKey,
            @Nullable @RequestParam("key") Optional<String> key) {
        return ResponseEntity.ok(getBpmService().getProcessInstances(key, businessKey));
    }

    /**
     * Gets the completed historic process instances.
     *
     * @return the process instances
     */
    @GetMapping(value = "/bpm-processes/historic-instances")
    public ResponseEntity<List<HistoricProcessInstance>> getHistoricProcessesInstances(
            @Nullable @RequestParam("definitionKey") Optional<String> definitionKey,
            @Nullable @RequestParam("businessKey") Optional<String> businessKey) {

        return ResponseEntity.ok(getBpmService().getCompletedProcessInstances(definitionKey, businessKey));
    }

    /**
     * List historic process instance variables.
     *
     * @param id the process instance id
     * @return process variables list
     */
    @GetMapping(value = "/bpm-processes/historic-instances/{id}/variables")
    public ResponseEntity<List<HistoricVariableInstance>> getProcessHistoricInstanceVariables(@PathVariable("id") String id) {
        BpmService bpmService = getBpmService();
        List<HistoricVariableInstance> variables = bpmService.getBpmProviderFlowable()
                                                             .getProcessEngine()
                                                             .getHistoryService()
                                                             .createHistoricVariableInstanceQuery()
                                                             .processInstanceId(id)
                                                             .list();

        return ResponseEntity.ok(variables);
    }

    @GetMapping(value = "/bpm-processes/instance/{id}")
    public ResponseEntity<ProcessInstanceData> getProcessInstance(@PathVariable("id") String id) {
        return ResponseEntity.ok(getBpmService().getProcessInstanceById(id));
    }

    /**
     * List active process instance variables.
     *
     * @param id the process instance id
     * @return process variables list
     */
    @GetMapping(value = "/bpm-processes/instance/{id}/variables")
    public ResponseEntity<List<VariableInstance>> getProcessInstanceVariables(@PathVariable("id") String id) {
        BpmService bpmService = getBpmService();
        List<VariableInstance> variables = bpmService.getBpmProviderFlowable()
                                                     .getProcessEngine()
                                                     .getRuntimeService()
                                                     .createVariableInstanceQuery()
                                                     .processInstanceId(id)
                                                     .list();

        return ResponseEntity.ok(variables);
    }

    @GetMapping(value = "/bpm-processes/instance/{id}/tasks")
    public ResponseEntity<List<TaskDTO>> getProcessInstanceTasks(@PathVariable("id") String id,
            @RequestParam(value = "type", required = false) String type) {
        List<TaskDTO> taskDTOS = taskQueryExecutor.findTasks(id, extractPrincipalType(type))
                                                  .stream()
                                                  .map(this::mapToDTO)
                                                  .collect(Collectors.toList());
        return ResponseEntity.ok(taskDTOS);
    }

    private static Type extractPrincipalType(String type) {
        Type principalType;
        try {
            principalType = Type.fromString(type);
        } catch (IllegalArgumentException e) {
            principalType = Type.ASSIGNEE;
        }
        return principalType;
    }

    private TaskDTO mapToDTO(Task task) {
        List<IdentityLink> identityLinks = getTaskService().getIdentityLinksForTask(task.getId());

        TaskDTO dto = new TaskDTO();
        dto.setId(task.getId());
        dto.setName(task.getName());
        dto.setAssignee(task.getAssignee());
        dto.setFormKey(task.getFormKey());
        dto.setCreateTime(task.getCreateTime());
        dto.setProcessInstanceId(task.getProcessInstanceId());
        dto.setCandidateUsers(identityLinks.stream()
                                           .map(IdentityLinkInfo::getUserId)
                                           .filter(Objects::nonNull)
                                           .collect(Collectors.joining(",")));
        dto.setCandidateGroups(identityLinks.stream()
                                            .map(IdentityLinkInfo::getGroupId)
                                            .filter(Objects::nonNull)
                                            .collect(Collectors.joining(",")));
        return dto;
    }

    private TaskService getTaskService() {
        return bpmService.getBpmProviderFlowable()
                         .getProcessEngine()
                         .getTaskService();
    }

    @GetMapping(value = "/bpm-processes/tasks")
    public ResponseEntity<List<TaskDTO>> getTasks(@RequestParam(value = "type", required = false) String type) {
        List<TaskDTO> taskDTOS = taskQueryExecutor.findTasks(extractPrincipalType(type))
                                                  .stream()
                                                  .map(this::mapToDTO)
                                                  .collect(Collectors.toList());
        return ResponseEntity.ok(taskDTOS);
    }

    @GetMapping(value = "/bpm-processes/tasks/{taskId}/variables")
    public ResponseEntity<?> getTaskVariables(@PathVariable("taskId") String taskId) {
        TaskService taskService = getTaskService();

        try {
            Map<String, Object> variables = taskService.getVariables(taskId);
            TaskVariablesDTO taskVariables = new TaskVariablesDTO(variables);

            return ResponseEntity.ok(taskVariables);
        } catch (FlowableObjectNotFoundException ex) {
            logger.debug("Missing task with id [{}]", taskId, ex);
            return ResponseEntity.notFound()
                                 .build();
        }
    }

    @PostMapping(value = "/bpm-processes/tasks/{id}")
    public ResponseEntity<String> executeTaskAction(@PathVariable("id") String id, @RequestBody TaskActionData actionData) {
        verifyCurrentUserHasPermissionForTask(id);

        final TaskService taskService = getTaskService();

        if (CLAIM.getActionName()
                 .equals(actionData.getAction())) {
            taskService.claim(id, UserFacade.getName());
        } else if (UNCLAIM.getActionName()
                          .equals(actionData.getAction())) {
            taskService.unclaim(id);
        } else if (COMPLETE.getActionName()
                           .equals(actionData.getAction())) {
            taskService.complete(id, actionData.getData());
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body("Invalid action id provided [" + actionData.getAction() + "]");
        }
        return ResponseEntity.ok()
                             .build();
    }

    private void verifyCurrentUserHasPermissionForTask(String id) {
        Set<String> userTaskIds = getUserTaskIds();
        if (!userTaskIds.contains(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Current user [" + UserFacade.getName() + "] doesn't have permissions for task with id " + id);
        }
    }

    private Set<String> getUserTaskIds() {
        Set<String> userRolesTasks = taskQueryExecutor.findTasks(Type.CANDIDATE_GROUPS)
                                                      .stream()
                                                      .map(Task::getId)
                                                      .collect(Collectors.toSet());

        Set<String> userAssignedTasks = taskQueryExecutor.findTasks(Type.ASSIGNEE)
                                                         .stream()
                                                         .map(Task::getId)
                                                         .collect(Collectors.toSet());

        Set<String> allTasks = new HashSet<>(userRolesTasks);
        allTasks.addAll(userAssignedTasks);
        return Collections.unmodifiableSet(allTasks);
    }

    /**
     * Add or update active process instance variable.
     *
     * @param id the process instance id
     * @param variableData the variable data
     * @return the response entity
     */
    @PostMapping(value = "/bpm-processes/instance/{id}/variables")
    public ResponseEntity<Void> addProcessInstanceVariables(@PathVariable("id") String id, @RequestBody VariableData variableData) {
        getBpmService().addProcessInstanceVariable(id, variableData.getName(), variableData.getValue());
        return ResponseEntity.ok()
                             .build();
    }

    /**
     * Execute action on active process instance variable.
     *
     * @param id the process instance id
     * @param actionData the action to be executed, possible values: RETRY
     * @return the response entity
     */
    @PostMapping(value = "/bpm-processes/instance/{id}")
    public ResponseEntity<String> executeProcessInstanceAction(@PathVariable("id") String id, @RequestBody ActionData actionData) {

        BpmService bpmService = getBpmService();
        if (RETRY.getActionName()
                 .equals(actionData.getAction())) {
            return retryJob(id);
        } else if (SKIP.getActionName()
                       .equals(actionData.getAction())) {
            bpmService.addProcessInstanceVariable(id, DIRIGIBLE_BPM_INTERNAL_SKIP_STEP, SKIP.getActionName());
            return retryJob(id);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body("Invalid action id provided [" + actionData.getAction() + "]");
        }
    }

    /**
     * Retry job.
     *
     * @param processInstanceId the process instance id
     * @return the response entity
     */
    private ResponseEntity<String> retryJob(String processInstanceId) {
        List<Job> jobs = bpmService.getDeadLetterJobs(processInstanceId);

        if (jobs.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                 .body("No dead letter jobs found for process instance id [" + processInstanceId + "]");
        }
        bpmService.retryDeadLetterJob(jobs.get(0), 1);
        return ResponseEntity.ok()
                             .build();
    }

    /**
     * List dead-letter jobs for an active process instance variables.
     *
     * @param id the process instance id
     * @return list of dead-letter jobs
     */
    @GetMapping(value = "/bpm-processes/instance/{id}/jobs")
    public ResponseEntity<List<Job>> getDeadLetterJobs(@PathVariable("id") String id) {

        BpmService bpmService = getBpmService();
        List<Job> jobs = bpmService.getBpmProviderFlowable()
                                   .getProcessEngine()
                                   .getManagementService()
                                   .createDeadLetterJobQuery()
                                   .processInstanceId(id)
                                   .list();

        return ResponseEntity.ok(jobs);
    }

    /**
     * Gets the process image.
     *
     * @param processDefinitionKey the process definition key
     * @return the process image
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @GetMapping(value = "/bpm-processes/diagram/definition/{processDefinitionKey}", produces = "image/png")
    public ResponseEntity<byte[]> getProcessDefinitionImage(@PathVariable("processDefinitionKey") String processDefinitionKey)
            throws IOException {
        ProcessEngine processEngine = getBpmProviderFlowable().getProcessEngine();
        RepositoryService repositoryService = processEngine.getRepositoryService();

        ProcessDefinition process = repositoryService.createProcessDefinitionQuery()
                                                     .processDefinitionKey(processDefinitionKey)
                                                     .latestVersion()
                                                     .singleResult();

        if (process != null) {
            String deploymentId = process.getDeploymentId();
            String diagramResourceName = process.getDiagramResourceName();

            byte[] imageBytes = repositoryService.getResourceAsStream(deploymentId, diagramResourceName)
                                                 .readAllBytes();

            return ResponseEntity.ok(imageBytes);
        }
        return ResponseEntity.ok(new byte[] {});
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
     * Gets the process image.
     *
     * @param processInstanceId the process instance id
     * @return the process image
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @GetMapping(value = "/bpm-processes/diagram/instance/{processInstanceId}", produces = "image/png")
    public ResponseEntity<byte[]> getProcessInstanceImage(@PathVariable("processInstanceId") String processInstanceId) throws IOException {
        ProcessEngine processEngine = getBpmProviderFlowable().getProcessEngine();
        RepositoryService repositoryService = processEngine.getRepositoryService();
        ProcessEngineConfiguration processEngineConfiguration = processEngine.getProcessEngineConfiguration();
        RuntimeService runtimeService = processEngine.getRuntimeService();

        ProcessInstanceData processInstanceData = getBpmService().getProcessInstanceById(processInstanceId);

        if (processInstanceData != null) {
            ProcessDefinition processDefinition = repositoryService.getProcessDefinition(processInstanceData.getProcessDefinitionId());

            if (processDefinition != null && processDefinition.hasGraphicalNotation()) {
                BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
                ProcessDiagramGenerator diagramGenerator = processEngineConfiguration.getProcessDiagramGenerator();
                InputStream resource = diagramGenerator.generateDiagram(bpmnModel, "png",
                        runtimeService.getActiveActivityIds(processInstanceData.getId()), Collections.emptyList(),
                        processEngineConfiguration.getActivityFontName(), processEngineConfiguration.getLabelFontName(),
                        processEngineConfiguration.getAnnotationFontName(), processEngineConfiguration.getClassLoader(), 1.0,
                        processEngineConfiguration.isDrawSequenceFlowNameWithNoLabelDI());

                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.set("Content-Type", "image/png");
                try {
                    return new ResponseEntity<>(IOUtils.toByteArray(resource), responseHeaders, HttpStatus.OK);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Error exporting diagram", e);
                }

            } else {
                throw new IllegalArgumentException(
                        "Process instance with id '" + processInstanceData.getId() + "' has no graphical notation defined.");
            }
        }
        return ResponseEntity.ok(new byte[] {});
    }

}
