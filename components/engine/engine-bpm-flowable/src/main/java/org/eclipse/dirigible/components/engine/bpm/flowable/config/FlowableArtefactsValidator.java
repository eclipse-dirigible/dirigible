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

import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Attachment;
import org.flowable.engine.task.Comment;
import org.flowable.engine.task.Event;
import org.flowable.task.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
class FlowableArtefactsValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableArtefactsValidator.class);

    private final TenantContext tenantContext;
    private final ProcessEngine processEngine;

    FlowableArtefactsValidator(ProcessEngine processEngine, TenantContext tenantContext) {
        this.processEngine = processEngine;
        this.tenantContext = tenantContext;
    }

    void validateAttachment(String attachmentId) {
        Attachment attachment = processEngine.getTaskService()
                                             .getAttachment(attachmentId);
        validateTask(attachment.getTaskId());
    }

    void validateTask(String taskId) {
        Task task = processEngine.getTaskService()
                                 .createTaskQuery()
                                 .taskId(taskId)
                                 .taskTenantId(getTenantId())
                                 .singleResult();
        if (task == null) {
            throw new IllegalArgumentException("Task with id [" + taskId + "] not found or does not belong to current tenant");
        }
    }

    private String getTenantId() {
        Tenant currentTenant = tenantContext.getCurrentTenant();
        LOGGER.debug("Current tenant is [{}]", currentTenant);
        return currentTenant.getId();
    }

    void validateProcessDefinitionId(String processDefinitionId) {
        ProcessDefinition processDefinition = processEngine.getRepositoryService()
                                                           .createProcessDefinitionQuery()
                                                           .processDefinitionId(processDefinitionId)
                                                           .processDefinitionTenantId(getTenantId())
                                                           .singleResult();

        if (processDefinition == null) {
            throw new IllegalArgumentException(
                    "Process definition with id [" + processDefinitionId + "] not found or does not belong to current tenant.");
        }

    }

    void validateHistoricProcessInstanceByProcessInstanceId(String processInstanceId) {
        HistoricProcessInstance historicProcessInstance = processEngine.getHistoryService()
                                                                       .createHistoricProcessInstanceQuery()
                                                                       .processInstanceId(processInstanceId)
                                                                       .processInstanceTenantId(getTenantId())
                                                                       .singleResult();

        if (historicProcessInstance == null) {
            throw new IllegalArgumentException("Historic process instance for process instance id [" + processInstanceId
                    + "] not found or does not belong to current tenant.");
        }
    }

    void validateExecutionId(String executionId) {
        RuntimeService runtimeService = processEngine.getRuntimeService();

        Execution execution = runtimeService.createExecutionQuery()
                                            .executionId(executionId)
                                            .executionTenantId(getTenantId())
                                            .singleResult();

        if (execution == null) {
            throw new IllegalArgumentException("Execution with id [" + executionId + "] not found or does not belong to current tenant.");
        }
    }

    void validateDeployment(String deploymentId) {
        RepositoryService repositoryService = processEngine.getRepositoryService();

        Deployment deployment = repositoryService.createDeploymentQuery()
                                                 .deploymentId(deploymentId)
                                                 .deploymentTenantId(getTenantId())
                                                 .singleResult();

        if (deployment == null) {
            throw new IllegalArgumentException("Deployment with id [" + deploymentId + "] not found or does not belong to current tenant.");
        }

    }

    void validateComments(List<Comment> comments) {
        comments.forEach(this::validateComment);
    }

    void validateComment(Comment comment) {
        validateTask(comment.getTaskId());
    }

    void validateEvent(Event event) {
        validateTask(event.getTaskId());
    }

    void validateProcessInstanceId(String processInstanceId) {
        RuntimeService runtimeService = processEngine.getRuntimeService();

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                                                        .processInstanceId(processInstanceId)
                                                        .processInstanceTenantId(getTenantId())
                                                        .singleResult();

        if (processInstance == null) {
            throw new IllegalArgumentException(
                    "Process instance with id [" + processInstanceId + "] not found or does not belong to current tenant.");
        }

    }

    void validateTasks(Collection<String> taskIds) {
        taskIds.forEach(this::validateTask);
    }

}
