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

import org.eclipse.dirigible.components.api.security.UserFacade;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.TaskActionData;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.TaskDTO;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.BpmService;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.PrincipalType;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkInfo;
import org.flowable.task.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

import static org.eclipse.dirigible.components.engine.bpm.flowable.dto.TaskActionData.TaskAction.*;

/**
 * Front facing REST service serving the BPM Inbox related resources and operations.
 */
@CrossOrigin
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_INBOX)
public class BpmInboxEndpoint extends BaseEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(BpmInboxEndpoint.class);

    private final BpmService bpmService;

    BpmInboxEndpoint(BpmService bpmService) {
        this.bpmService = bpmService;
    }

    @GetMapping(value = "/instance/{id}/tasks")
    public ResponseEntity<List<TaskDTO>> getProcessInstanceTasks(@PathVariable("id") String id,
            @RequestParam(value = "type", required = false) String type) {
        List<TaskDTO> taskDTOS = bpmService.findTasks(id, extractPrincipalType(type))
                                           .stream()
                                           .map(this::mapToDTO)
                                           .collect(Collectors.toList());
        return ResponseEntity.ok(taskDTOS);
    }

    private static PrincipalType extractPrincipalType(String type) {
        PrincipalType principalType;
        try {
            principalType = PrincipalType.fromString(type);
        } catch (IllegalArgumentException e) {
            principalType = PrincipalType.ASSIGNEE;
        }
        return principalType;
    }

    private TaskDTO mapToDTO(Task task) {
        List<IdentityLink> identityLinks = bpmService.getTaskIdentityLinks(task.getId());

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

    @GetMapping(value = "/tasks")
    public ResponseEntity<List<TaskDTO>> getTasks(@RequestParam(value = "type", required = false) String type) {
        List<TaskDTO> taskDTOS = bpmService.findTasks(extractPrincipalType(type))
                                           .stream()
                                           .map(this::mapToDTO)
                                           .collect(Collectors.toList());
        return ResponseEntity.ok(taskDTOS);
    }

    @GetMapping(value = "/tasks/{taskId}/variables")
    public ResponseEntity<?> getTaskVariables(@PathVariable("taskId") String taskId) {
        try {
            Map<String, Object> variables = bpmService.getTaskVariables(taskId);
            TaskVariablesDTO taskVariables = new TaskVariablesDTO(variables);

            return ResponseEntity.ok(taskVariables);
        } catch (FlowableObjectNotFoundException ex) {
            logger.debug("Missing task with id [{}]", taskId, ex);
            return ResponseEntity.notFound()
                                 .build();
        }
    }

    @PostMapping(value = "/tasks/{id}")
    public ResponseEntity<String> executeTaskAction(@PathVariable("id") String taskId, @RequestBody TaskActionData actionData) {
        verifyCurrentUserHasPermissionForTask(taskId);

        if (CLAIM.getActionName()
                 .equals(actionData.getAction())) {
            bpmService.claimTask(taskId, UserFacade.getName());
        } else if (UNCLAIM.getActionName()
                          .equals(actionData.getAction())) {
            bpmService.unclaimTask(taskId);
        } else if (COMPLETE.getActionName()
                           .equals(actionData.getAction())) {
            bpmService.completeTask(taskId, actionData.getData());
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
        Set<String> userRolesTasks = bpmService.findTasks(PrincipalType.CANDIDATE_GROUPS)
                                               .stream()
                                               .map(Task::getId)
                                               .collect(Collectors.toSet());

        Set<String> userAssignedTasks = bpmService.findTasks(PrincipalType.ASSIGNEE)
                                                  .stream()
                                                  .map(Task::getId)
                                                  .collect(Collectors.toSet());

        Set<String> allTasks = new HashSet<>(userRolesTasks);
        allTasks.addAll(userAssignedTasks);
        return Collections.unmodifiableSet(allTasks);
    }

}
