/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.endpoint;

import static org.eclipse.dirigible.components.engine.bpm.flowable.dto.TaskActionData.TaskAction.CLAIM;
import static org.eclipse.dirigible.components.engine.bpm.flowable.dto.TaskActionData.TaskAction.COMPLETE;
import static org.eclipse.dirigible.components.engine.bpm.flowable.dto.TaskActionData.TaskAction.UNCLAIM;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.dirigible.components.api.security.UserFacade;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.TaskActionData;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.TaskDTO;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.BpmService;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.task.TaskQueryExecutor;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.task.TaskQueryExecutor.Type;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.TaskService;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.task.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Front facing REST service serving the BPM Inbox related resources and operations.
 */
@CrossOrigin
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_INBOX)
public class BpmInboxEndpoint extends BaseEndpoint {

    /**
     * The Constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(BpmInboxEndpoint.class);

    /**
     * The bpm service.
     */
    @Autowired
    private BpmService bpmService;

    @Autowired
    private TaskQueryExecutor taskQueryExecutor;

    @GetMapping(value = "/instance/{id}/tasks")
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

    @GetMapping(value = "/tasks")
    public ResponseEntity<List<TaskDTO>> getTasks(@RequestParam(value = "type", required = false) String type) {
        List<TaskDTO> taskDTOS = taskQueryExecutor.findTasks(extractPrincipalType(type))
                                                  .stream()
                                                  .map(this::mapToDTO)
                                                  .collect(Collectors.toList());
        return ResponseEntity.ok(taskDTOS);
    }

    @GetMapping(value = "/tasks/{taskId}/variables")
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

    @PostMapping(value = "/tasks/{id}")
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

}
