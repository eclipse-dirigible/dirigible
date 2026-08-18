/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.endpoint;

import org.eclipse.dirigible.components.api.security.ActAsFacade;
import org.eclipse.dirigible.components.api.security.UserFacade;
import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.ProcessInstanceData;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.ProcessLabelKeys;
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
        return ResponseEntity.ok(mapToDTOs(bpmService.findTasks(id, extractPrincipalType(type))));
    }

    /**
     * Maps a task list, resolving each process definition's task-label catalog once - a whole inbox is
     * typically a handful of definitions, and every task of one shares its catalog.
     *
     * @param tasks the tasks
     * @return the DTOs
     */
    private List<TaskDTO> mapToDTOs(List<Task> tasks) {
        Map<String, Optional<ProcessLabelKeys>> labelKeys = new HashMap<>();
        return tasks.stream()
                    .map(task -> mapToDTO(task, labelKeys))
                    .collect(Collectors.toList());
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

    private TaskDTO mapToDTO(Task task, Map<String, Optional<ProcessLabelKeys>> labelKeys) {
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
        ProcessInstanceData processInstance = bpmService.getProcessInstanceById(task.getProcessInstanceId());
        dto.setProcessInstanceBusinessKey(processInstance.getBusinessKey());
        dto.setProcessDefinitionId(processInstance.getProcessDefinitionId());
        dto.setProcessDefinitionName(processInstance.getProcessDefinitionName());
        // The names in the language of whoever is looking at them: the process declares the i18n
        // catalog its module's labels live in, and a BPMN id keys them within it. Cross-project shell
        // surfaces (the Inbox, the notification bell, the task-form dialog) have no idea which module
        // a task came from, so the keys have to travel with the task.
        labelKeys.computeIfAbsent(task.getProcessDefinitionId(), bpmService::getProcessLabelKeys)
                 .ifPresent(keys -> {
                     dto.setNameKey(keys.taskNameKey(task.getTaskDefinitionKey()));
                     dto.setProcessDefinitionNameKey(keys.processNameKey());
                 });

        return dto;
    }

    @GetMapping(value = "/tasks")
    public ResponseEntity<List<TaskDTO>> getTasks(@RequestParam(value = "type", required = false) String type) {
        return ResponseEntity.ok(mapToDTOs(bpmService.findTasks(extractPrincipalType(type))));
    }

    @GetMapping(value = "/tasks/{taskId}/variables")
    public ResponseEntity<?> getTaskVariables(@PathVariable("taskId") String taskId) {
        // A task's variables carry its business payload (the record id and the locators a task form
        // resolves), so reading them is gated exactly like acting on the task - the inbox is scoped
        // to the tasks the caller is assigned to or a candidate for, never addressable by bare id.
        verifyCurrentUserHasPermissionForTask(taskId);
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
            bpmService.claimTask(taskId, claimantFor(taskId));
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

    /**
     * Who a claim assigns the task to. Under act-as (delegated entry) that is the ACTING identity, but
     * ONLY for a task addressed to that person - one where they are a candidate user, i.e. their own
     * work the delegate is entering on their behalf. A task the caller reached through their OWN roles
     * (a back-office group task: approve, issue, send) is claimed by the REAL user, whatever is armed:
     * stamping the acted-as person there attributes a decision to someone who never made it, and
     * strands the task on an identity whose group-candidate visibility the claim just removed it from.
     *
     * @param taskId the task about to be claimed
     * @return the username to claim for
     */
    private String claimantFor(String taskId) {
        String realUser = UserFacade.getName();
        String acting = ActAsFacade.actingAs();
        if (acting == null) {
            return realUser;
        }
        boolean addressedToActingIdentity = bpmService.getTaskIdentityLinks(taskId)
                                                      .stream()
                                                      .map(IdentityLinkInfo::getUserId)
                                                      .anyMatch(acting::equals);
        if (addressedToActingIdentity) {
            return acting;
        }
        logger.info("Act-as: task [{}] is not addressed to [{}] - claiming it for the real user [{}]", taskId, acting, realUser);
        return realUser;
    }

    /**
     * What act-as is currently doing to this Inbox: who is armed, and how many of the REAL user's own
     * assigned tasks the armed state is hiding. An armed session's assignee query serves the acting
     * identity's world, so the real user's tasks silently vanish from their own Inbox - this is what
     * lets the Inbox say so instead of rendering an indistinguishable empty state.
     *
     * @return the acting identity (null when none) and the hidden-task count
     */
    @GetMapping(value = "/act-as")
    public ResponseEntity<ActAsInboxState> getActAsState() {
        String acting = ActAsFacade.actingAs();
        long hidden = acting == null ? 0 : bpmService.countTasksByAssignee(UserFacade.getName());
        return ResponseEntity.ok(new ActAsInboxState(acting, hidden));
    }

    /** The Inbox's act-as state: the armed acting identity and the real user's tasks it hides. */
    public record ActAsInboxState(String actingAs, long hiddenTasks) {
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
