/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.config;

import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.api.security.UserFacade;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.engine.bpm.flowable.TaskService;
import org.eclipse.dirigible.components.engine.bpm.flowable.dto.TaskData;
import org.eclipse.dirigible.components.engine.bpm.flowable.service.PrincipalType;
import org.flowable.engine.runtime.DataObject;
import org.flowable.engine.task.Attachment;
import org.flowable.engine.task.Comment;
import org.flowable.engine.task.Event;
import org.flowable.form.api.FormInfo;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskBuilder;
import org.flowable.task.api.TaskInfoQuery;
import org.flowable.task.api.TaskQuery;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.io.InputStream;
import java.util.*;

/**
 * The Class TaskServiceImpl. NOTE! - all methods in the class should be tenant aware
 */
class TaskServiceImpl implements TaskService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskServiceImpl.class);

    private final org.flowable.engine.TaskService flowableTaskService;
    private final TenantContext tenantContext;
    private final FlowableArtefactsValidator flowableArtefactsValidator;

    TaskServiceImpl(org.flowable.engine.TaskService flowableTaskService, TenantContext tenantContext,
            FlowableArtefactsValidator flowableArtefactsValidator) {
        this.flowableTaskService = flowableTaskService;
        this.tenantContext = tenantContext;
        this.flowableArtefactsValidator = flowableArtefactsValidator;
    }

    @Override
    public long getTasksCount() {
        return flowableTaskService.createTaskQuery()
                                  .count();
    }

    @Override
    public void unclaimTask(String taskId) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.unclaim(taskId);
    }

    @Override
    public String getTasks() {
        List<TaskData> tasksData = new ArrayList<>();
        List<Task> tasks = flowableTaskService.createTaskQuery()
                                              .taskTenantId(getTenantId())
                                              .list();
        for (Task task : tasks) {
            TaskData taskData = new TaskData();
            BeanUtils.copyProperties(task, taskData);
            tasksData.add(taskData);
        }
        return GsonHelper.toJson(tasksData);
    }

    private String getTenantId() {
        Tenant currentTenant = tenantContext.getCurrentTenant();
        LOGGER.debug("Current tenant is [{}]", currentTenant);
        return currentTenant.getId();
    }

    @Override
    public Task newTask() {
        Task task = flowableTaskService.newTask();
        task.setTenantId(getTenantId());
        return task;
    }

    @Override
    public Task newTask(String taskId) {
        Task task = flowableTaskService.newTask(taskId);
        task.setTenantId(getTenantId());
        return task;
    }

    @Override
    public TaskBuilder createTaskBuilder() {
        return flowableTaskService.createTaskBuilder();
    }

    @Override
    public void saveTask(Task task) {
        task.setTenantId(getTenantId());
        flowableTaskService.saveTask(task);
    }

    @Override
    public void bulkSaveTasks(Collection<Task> taskList) {
        taskList.forEach(t -> t.setTenantId(getTenantId()));
        flowableTaskService.bulkSaveTasks(taskList);
    }

    @Override
    public void deleteTask(String taskId) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.deleteTask(taskId);
    }

    @Override
    public void deleteTasks(Collection<String> taskIds) {
        flowableArtefactsValidator.validateTasks(taskIds);

        flowableTaskService.deleteTasks(taskIds);
    }

    @Override
    public void deleteTask(String taskId, boolean cascade) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.deleteTask(taskId, cascade);
    }

    @Override
    public void deleteTasks(Collection<String> taskIds, boolean cascade) {
        flowableArtefactsValidator.validateTasks(taskIds);

        flowableTaskService.deleteTasks(taskIds, cascade);
    }

    @Override
    public void deleteTask(String taskId, String deleteReason) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.deleteTask(taskId, deleteReason);
    }

    @Override
    public void deleteTasks(Collection<String> taskIds, String deleteReason) {
        flowableTaskService.deleteTasks(taskIds, deleteReason);
    }

    @Override
    public void claim(String taskId, String userId) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.claim(taskId, userId);
    }

    @Override
    public Map<String, Object> getTaskVariables(String taskId) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.getVariables(taskId);
    }

    @Override
    public void setTaskVariable(String taskId, String variableName, Object variable) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.setVariable(taskId, variableName, variable);
    }

    @Override
    public void completeTask(String taskId, String variables) {
        Map<String, Object> processVariables = GsonHelper.fromJson(variables, HashMap.class);
        completeTask(taskId, processVariables);
    }

    @Override
    public void completeTask(String taskId, Map<String, Object> variables) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.complete(taskId, variables);
    }

    @Override
    public List<Task> findTasks(String processInstanceId, PrincipalType type) {
        if (UserFacade.getUserRoles()
                      .isEmpty()) {
            return Collections.emptyList();
        }
        TaskInfoQuery<TaskQuery, Task> taskQuery = prepareQuery(type);
        taskQuery.processInstanceId(processInstanceId);
        return taskQuery.list();
    }

    private TaskInfoQuery<TaskQuery, Task> prepareQuery(PrincipalType type) {
        TaskQuery taskQuery = flowableTaskService.createTaskQuery()
                                                 .taskTenantId(getTenantId());
        if (PrincipalType.CANDIDATE_GROUPS.equals(type)) {
            return taskQuery.taskCandidateGroupIn(UserFacade.getUserRoles());
        } else if (PrincipalType.ASSIGNEE.equals(type)) {
            return taskQuery.taskAssignee(UserFacade.getName());
        } else {
            throw new IllegalArgumentException("Unrecognised principal type: " + type);
        }
    }

    @Override
    public List<Task> findTasks(PrincipalType type) {
        if (UserFacade.getUserRoles()
                      .isEmpty()) {
            return Collections.emptyList();
        }
        TaskInfoQuery<TaskQuery, Task> taskQuery = prepareQuery(type);
        return taskQuery.list();
    }

    @Override
    public List<IdentityLink> getTaskIdentityLinks(String taskId) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.getIdentityLinksForTask(taskId);
    }

    @Override
    public void setTaskVariables(String taskId, Map<String, Object> variables) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.setVariables(taskId, variables);
    }

    @Override
    public Object getTaskVariable(String taskId, String variableName) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.getVariable(taskId, variableName);
    }

    @Override
    public void claimTask(String taskId, String userId) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.claim(taskId, userId);
    }

    @Override
    public void unclaim(String taskId) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.unclaim(taskId);
    }

    @Override
    public void startProgress(String taskId, String userId) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.startProgress(taskId, userId);
    }

    @Override
    public void suspendTask(String taskId, String userId) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.suspendTask(taskId, userId);
    }

    @Override
    public void activateTask(String taskId, String userId) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.activateTask(taskId, userId);
    }

    @Override
    public void delegateTask(String taskId, String userId) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.delegateTask(taskId, userId);
    }

    @Override
    public void resolveTask(String taskId) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.resolveTask(taskId);
    }

    @Override
    public void resolveTask(String taskId, Map<String, Object> variables) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.resolveTask(taskId, variables);
    }

    @Override
    public void resolveTask(String taskId, Map<String, Object> variables, Map<String, Object> transientVariables) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.resolveTask(taskId, variables, transientVariables);
    }

    @Override
    public void complete(String taskId) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.complete(taskId);
    }

    @Override
    public void complete(String taskId, String userId) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.complete(taskId, userId);
    }

    @Override
    public void complete(String taskId, Map<String, Object> variables) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.complete(taskId, variables);
    }

    @Override
    public void complete(String taskId, String userId, Map<String, Object> variables) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.complete(taskId, userId, variables);
    }

    @Override
    public void complete(String taskId, Map<String, Object> variables, Map<String, Object> transientVariables) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.complete(taskId, variables, transientVariables);
    }

    @Override
    public void complete(String taskId, String userId, Map<String, Object> variables, Map<String, Object> transientVariables) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.complete(taskId, userId, variables, transientVariables);
    }

    @Override
    public void complete(String taskId, Map<String, Object> variables, boolean localScope) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.complete(taskId, variables, localScope);
    }

    @Override
    public void complete(String taskId, String userId, Map<String, Object> variables, boolean localScope) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.complete(taskId, userId, variables, localScope);
    }

    @Override
    public void completeTaskWithForm(String taskId, String formDefinitionId, String outcome, Map<String, Object> variables) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.completeTaskWithForm(taskId, formDefinitionId, outcome, variables);
    }

    @Override
    public void completeTaskWithForm(String taskId, String formDefinitionId, String outcome, String userId, Map<String, Object> variables) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.completeTaskWithForm(taskId, formDefinitionId, outcome, userId, variables);
    }

    @Override
    public void completeTaskWithForm(String taskId, String formDefinitionId, String outcome, Map<String, Object> variables,
            Map<String, Object> transientVariables) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.completeTaskWithForm(taskId, formDefinitionId, outcome, variables, transientVariables);
    }

    @Override
    public void completeTaskWithForm(String taskId, String formDefinitionId, String outcome, String userId, Map<String, Object> variables,
            Map<String, Object> transientVariables) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.completeTaskWithForm(taskId, formDefinitionId, outcome, userId, variables, transientVariables);
    }

    @Override
    public void completeTaskWithForm(String taskId, String formDefinitionId, String outcome, Map<String, Object> variables,
            boolean localScope) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.completeTaskWithForm(taskId, formDefinitionId, outcome, variables, localScope);
    }

    @Override
    public void completeTaskWithForm(String taskId, String formDefinitionId, String outcome, String userId, Map<String, Object> variables,
            boolean localScope) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.completeTaskWithForm(taskId, formDefinitionId, outcome, userId, variables, localScope);
    }

    @Override
    public FormInfo getTaskFormModel(String taskId) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.getTaskFormModel(taskId);
    }

    @Override
    public FormInfo getTaskFormModel(String taskId, boolean ignoreVariables) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.getTaskFormModel(taskId, ignoreVariables);
    }

    @Override
    public void setAssignee(String taskId, String userId) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.setAssignee(taskId, userId);
    }

    @Override
    public void setOwner(String taskId, String userId) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.setOwner(taskId, userId);
    }

    @Override
    public List<IdentityLink> getIdentityLinksForTask(String taskId) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.getIdentityLinksForTask(taskId);
    }

    @Override
    public void addCandidateUser(String taskId, String userId) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.addCandidateUser(taskId, userId);
    }

    @Override
    public void addCandidateGroup(String taskId, String groupId) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.addCandidateGroup(taskId, groupId);
    }

    @Override
    public void addUserIdentityLink(String taskId, String userId, String identityLinkType) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.addUserIdentityLink(taskId, userId, identityLinkType);
    }

    @Override
    public void addGroupIdentityLink(String taskId, String groupId, String identityLinkType) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.addGroupIdentityLink(taskId, groupId, identityLinkType);
    }

    @Override
    public void deleteCandidateUser(String taskId, String userId) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.deleteCandidateUser(taskId, userId);
    }

    @Override
    public void deleteCandidateGroup(String taskId, String groupId) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.deleteCandidateGroup(taskId, groupId);
    }

    @Override
    public void deleteUserIdentityLink(String taskId, String userId, String identityLinkType) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.deleteUserIdentityLink(taskId, userId, identityLinkType);
    }

    @Override
    public void deleteGroupIdentityLink(String taskId, String groupId, String identityLinkType) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.deleteGroupIdentityLink(taskId, groupId, identityLinkType);
    }

    @Override
    public void setPriority(String taskId, int priority) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.setPriority(taskId, priority);
    }

    @Override
    public void setDueDate(String taskId, Date dueDate) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.setDueDate(taskId, dueDate);
    }

    @Override
    public void setVariable(String taskId, String variableName, Object value) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.setVariable(taskId, variableName, value);
    }

    @Override
    public void setVariables(String taskId, Map<String, ?> variables) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.setVariables(taskId, variables);
    }

    @Override
    public void setVariableLocal(String taskId, String variableName, Object value) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.setVariableLocal(taskId, variableName, value);
    }

    @Override
    public void setVariablesLocal(String taskId, Map<String, ?> variables) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.setVariablesLocal(taskId, variables);
    }

    @Override
    public Object getVariable(String taskId, String variableName) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.getVariable(taskId, variableName);
    }

    @Override
    public <T> T getVariable(String taskId, String variableName, Class<T> variableClass) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.getVariable(taskId, variableName, variableClass);
    }

    @Override
    public VariableInstance getVariableInstance(String taskId, String variableName) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.getVariableInstance(taskId, variableName);
    }

    @Override
    public boolean hasVariable(String taskId, String variableName) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.hasVariable(taskId, variableName);
    }

    @Override
    public Object getVariableLocal(String taskId, String variableName) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.getVariableLocal(taskId, variableName);
    }

    @Override
    public <T> T getVariableLocal(String taskId, String variableName, Class<T> variableClass) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.getVariableLocal(taskId, variableName, variableClass);
    }

    @Override
    public VariableInstance getVariableInstanceLocal(String taskId, String variableName) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.getVariableInstanceLocal(taskId, variableName);
    }

    @Override
    public boolean hasVariableLocal(String taskId, String variableName) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.hasVariableLocal(taskId, variableName);
    }

    @Override
    public Map<String, Object> getVariables(String taskId) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.getVariables(taskId);
    }

    @Override
    public Map<String, VariableInstance> getVariableInstances(String taskId) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.getVariableInstances(taskId);
    }

    @Override
    public Map<String, VariableInstance> getVariableInstances(String taskId, Collection<String> variableNames) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.getVariableInstances(taskId, variableNames);
    }

    @Override
    public Map<String, Object> getVariablesLocal(String taskId) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.getVariablesLocal(taskId);
    }

    @Override
    public Map<String, Object> getVariables(String taskId, Collection<String> variableNames) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.getVariables(taskId, variableNames);
    }

    @Override
    public Map<String, Object> getVariablesLocal(String taskId, Collection<String> variableNames) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.getVariablesLocal(taskId, variableNames);
    }

    @Override
    public List<VariableInstance> getVariableInstancesLocalByTaskIds(Set<String> taskIds) {
        flowableArtefactsValidator.validateTasks(taskIds);

        return flowableTaskService.getVariableInstancesLocalByTaskIds(taskIds);
    }

    @Override
    public Map<String, VariableInstance> getVariableInstancesLocal(String taskId) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.getVariableInstancesLocal(taskId);
    }

    @Override
    public Map<String, VariableInstance> getVariableInstancesLocal(String taskId, Collection<String> variableNames) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.getVariableInstancesLocal(taskId, variableNames);
    }

    @Override
    public void removeVariable(String taskId, String variableName) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.removeVariable(taskId, variableName);
    }

    @Override
    public void removeVariableLocal(String taskId, String variableName) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.removeVariableLocal(taskId, variableName);
    }

    @Override
    public void removeVariables(String taskId, Collection<String> variableNames) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.removeVariables(taskId, variableNames);
    }

    @Override
    public void removeVariablesLocal(String taskId, Collection<String> variableNames) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.removeVariablesLocal(taskId, variableNames);
    }

    @Override
    public Map<String, DataObject> getDataObjects(String taskId) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.getDataObjects(taskId);
    }

    @Override
    public Map<String, DataObject> getDataObjects(String taskId, String locale, boolean withLocalizationFallback) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.getDataObjects(taskId, locale, withLocalizationFallback);
    }

    @Override
    public Map<String, DataObject> getDataObjects(String taskId, Collection<String> dataObjectNames) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.getDataObjects(taskId, dataObjectNames);
    }

    @Override
    public Map<String, DataObject> getDataObjects(String taskId, Collection<String> dataObjectNames, String locale,
            boolean withLocalizationFallback) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.getDataObjects(taskId, dataObjectNames, locale, withLocalizationFallback);
    }

    @Override
    public DataObject getDataObject(String taskId, String dataObject) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.getDataObject(taskId, dataObject);
    }

    @Override
    public DataObject getDataObject(String taskId, String dataObjectName, String locale, boolean withLocalizationFallback) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.getDataObject(taskId, dataObjectName, locale, withLocalizationFallback);
    }

    @Override
    public Comment addComment(String taskId, String processInstanceId, String message) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.addComment(taskId, processInstanceId, message);
    }

    @Override
    public Comment addComment(String taskId, String processInstanceId, String type, String message) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.addComment(taskId, processInstanceId, type, message);
    }

    @Override
    public void saveComment(Comment comment) {
        flowableArtefactsValidator.validateTask(comment.getTaskId());

        flowableTaskService.saveComment(comment);
    }

    @Override
    public void deleteComments(String taskId, String processInstanceId) {
        flowableArtefactsValidator.validateTask(taskId);

        flowableTaskService.deleteComments(taskId, processInstanceId);
    }

    @Override
    public void deleteComment(String commentId) {
        getComment(commentId);// validates the tenant

        flowableTaskService.deleteComment(commentId);
    }

    @Override
    public Comment getComment(String commentId) {
        Comment comment = flowableTaskService.getComment(commentId);
        flowableArtefactsValidator.validateComment(comment);

        return comment;
    }

    @Override
    public List<Comment> getTaskComments(String taskId) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.getTaskComments(taskId);
    }

    @Override
    public List<Comment> getTaskComments(String taskId, String type) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.getTaskComments(taskId, type);
    }

    @Override
    public List<Comment> getCommentsByType(String type) {
        List<Comment> comments = flowableTaskService.getCommentsByType(type);
        flowableArtefactsValidator.validateComments(comments);

        return comments;
    }

    @Override
    public List<Event> getTaskEvents(String taskId) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.getTaskEvents(taskId);
    }

    @Override
    public Event getEvent(String eventId) {
        Event event = flowableTaskService.getEvent(eventId);
        flowableArtefactsValidator.validateEvent(event);

        return event;
    }

    @Override
    public List<Comment> getProcessInstanceComments(String processInstanceId) {
        flowableArtefactsValidator.validateProcessInstanceId(processInstanceId);

        return flowableTaskService.getProcessInstanceComments(processInstanceId);
    }

    @Override
    public List<Comment> getProcessInstanceComments(String processInstanceId, String type) {
        flowableArtefactsValidator.validateProcessInstanceId(processInstanceId);

        return flowableTaskService.getProcessInstanceComments(processInstanceId, type);
    }

    @Override
    public Attachment createAttachment(String attachmentType, String taskId, String processInstanceId, String attachmentName,
            String attachmentDescription, InputStream content) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.createAttachment(attachmentType, taskId, processInstanceId, attachmentName, attachmentDescription,
                content);
    }

    @Override
    public Attachment createAttachment(String attachmentType, String taskId, String processInstanceId, String attachmentName,
            String attachmentDescription, String url) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.createAttachment(attachmentType, taskId, processInstanceId, attachmentName, attachmentDescription, url);
    }

    @Override
    public void saveAttachment(Attachment attachment) {
        flowableArtefactsValidator.validateTask(attachment.getTaskId());

        flowableTaskService.saveAttachment(attachment);
    }

    @Override
    public InputStream getAttachmentContent(String attachmentId) {
        flowableArtefactsValidator.validateAttachment(attachmentId);

        return flowableTaskService.getAttachmentContent(attachmentId);
    }

    @Override
    public Attachment getAttachment(String attachmentId) {
        flowableArtefactsValidator.validateAttachment(attachmentId);

        return flowableTaskService.getAttachment(attachmentId);
    }

    @Override
    public List<Attachment> getTaskAttachments(String taskId) {
        flowableArtefactsValidator.validateTask(taskId);

        return flowableTaskService.getTaskAttachments(taskId);
    }

    @Override
    public List<Attachment> getProcessInstanceAttachments(String processInstanceId) {
        flowableArtefactsValidator.validateProcessInstanceId(processInstanceId);

        return flowableTaskService.getProcessInstanceAttachments(processInstanceId);
    }

    @Override
    public void deleteAttachment(String attachmentId) {
        flowableArtefactsValidator.validateAttachment(attachmentId);

        flowableTaskService.deleteAttachment(attachmentId);
    }

    @Override
    public List<Task> getSubTasks(String parentTaskId) {
        flowableArtefactsValidator.validateTask(parentTaskId);

        return flowableTaskService.getSubTasks(parentTaskId);
    }
}
