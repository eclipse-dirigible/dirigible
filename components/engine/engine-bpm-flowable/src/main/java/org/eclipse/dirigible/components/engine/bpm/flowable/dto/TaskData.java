/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.dto;

import java.util.Date;

import org.flowable.task.api.DelegationState;

/**
 * The Class TaskData.
 */
public class TaskData {

    /** The id. */
    protected String id;

    /** The revision. */
    protected int revision;

    /** The owner. */
    protected String owner;

    /** The assignee updated count. */
    protected int assigneeUpdatedCount;

    /** The original assignee. */
    protected String originalAssignee;

    /** The assignee. */
    protected String assignee;

    /** The delegation state. */
    protected DelegationState delegationState;

    /** The parent task id. */
    protected String parentTaskId;

    /** The name. */
    protected String name;

    /** The localized name. */
    protected String localizedName;

    /** The description. */
    protected String description;

    /** The localized description. */
    protected String localizedDescription;

    /** The priority. */
    protected int priority = 50;

    /** The create time. */
    protected Date createTime;

    /** The due date. */
    protected Date dueDate;

    /** The suspension state. */
    protected int suspensionState = 1;

    /** The category. */
    protected String category;

    /** The is identity links initialized. */
    protected boolean isIdentityLinksInitialized;

    /** The execution id. */
    protected String executionId;

    /** The process instance id. */
    protected String processInstanceId;

    /** The process definition id. */
    protected String processDefinitionId;

    /** The scope id. */
    protected String scopeId;

    /** The sub scope id. */
    protected String subScopeId;

    /** The scope type. */
    protected String scopeType;

    /** The scope definition id. */
    protected String scopeDefinitionId;

    /** The task definition key. */
    protected String taskDefinitionKey;

    /** The form key. */
    protected String formKey;

    /** The is deleted. */
    protected boolean isDeleted;

    /** The is canceled. */
    protected boolean isCanceled;

    /** The is count enabled. */
    private boolean isCountEnabled;

    /** The variable count. */
    private int variableCount;

    /** The identity link count. */
    private int identityLinkCount;

    /** The claim time. */
    protected Date claimTime;

    /** The tenant id. */
    protected String tenantId = "";

    /** The event name. */
    protected String eventName;

    /** The event handler id. */
    protected String eventHandlerId;

    /** The forced update. */
    protected boolean forcedUpdate;

    /**
     * Gets the id.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param id the new id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the revision.
     *
     * @return the revision
     */
    public int getRevision() {
        return revision;
    }

    /**
     * Sets the revision.
     *
     * @param revision the new revision
     */
    public void setRevision(int revision) {
        this.revision = revision;
    }

    /**
     * Gets the owner.
     *
     * @return the owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Sets the owner.
     *
     * @param owner the new owner
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * Gets the assignee updated count.
     *
     * @return the assignee updated count
     */
    public int getAssigneeUpdatedCount() {
        return assigneeUpdatedCount;
    }

    /**
     * Sets the assignee updated count.
     *
     * @param assigneeUpdatedCount the new assignee updated count
     */
    public void setAssigneeUpdatedCount(int assigneeUpdatedCount) {
        this.assigneeUpdatedCount = assigneeUpdatedCount;
    }

    /**
     * Gets the original assignee.
     *
     * @return the original assignee
     */
    public String getOriginalAssignee() {
        return originalAssignee;
    }

    /**
     * Sets the original assignee.
     *
     * @param originalAssignee the new original assignee
     */
    public void setOriginalAssignee(String originalAssignee) {
        this.originalAssignee = originalAssignee;
    }

    /**
     * Gets the assignee.
     *
     * @return the assignee
     */
    public String getAssignee() {
        return assignee;
    }

    /**
     * Sets the assignee.
     *
     * @param assignee the new assignee
     */
    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    /**
     * Gets the delegation state.
     *
     * @return the delegation state
     */
    public DelegationState getDelegationState() {
        return delegationState;
    }

    /**
     * Sets the delegation state.
     *
     * @param delegationState the new delegation state
     */
    public void setDelegationState(DelegationState delegationState) {
        this.delegationState = delegationState;
    }

    /**
     * Gets the parent task id.
     *
     * @return the parent task id
     */
    public String getParentTaskId() {
        return parentTaskId;
    }

    /**
     * Sets the parent task id.
     *
     * @param parentTaskId the new parent task id
     */
    public void setParentTaskId(String parentTaskId) {
        this.parentTaskId = parentTaskId;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the localized name.
     *
     * @return the localized name
     */
    public String getLocalizedName() {
        return localizedName;
    }

    /**
     * Sets the localized name.
     *
     * @param localizedName the new localized name
     */
    public void setLocalizedName(String localizedName) {
        this.localizedName = localizedName;
    }

    /**
     * Gets the description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     *
     * @param description the new description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the localized description.
     *
     * @return the localized description
     */
    public String getLocalizedDescription() {
        return localizedDescription;
    }

    /**
     * Sets the localized description.
     *
     * @param localizedDescription the new localized description
     */
    public void setLocalizedDescription(String localizedDescription) {
        this.localizedDescription = localizedDescription;
    }

    /**
     * Gets the priority.
     *
     * @return the priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Sets the priority.
     *
     * @param priority the new priority
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * Gets the creates the time.
     *
     * @return the creates the time
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * Sets the creates the time.
     *
     * @param createTime the new creates the time
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    /**
     * Gets the due date.
     *
     * @return the due date
     */
    public Date getDueDate() {
        return dueDate;
    }

    /**
     * Sets the due date.
     *
     * @param dueDate the new due date
     */
    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    /**
     * Gets the suspension state.
     *
     * @return the suspension state
     */
    public int getSuspensionState() {
        return suspensionState;
    }

    /**
     * Sets the suspension state.
     *
     * @param suspensionState the new suspension state
     */
    public void setSuspensionState(int suspensionState) {
        this.suspensionState = suspensionState;
    }

    /**
     * Gets the category.
     *
     * @return the category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the category.
     *
     * @param category the new category
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Checks if is identity links initialized.
     *
     * @return true, if is identity links initialized
     */
    public boolean isIdentityLinksInitialized() {
        return isIdentityLinksInitialized;
    }

    /**
     * Sets the identity links initialized.
     *
     * @param isIdentityLinksInitialized the new identity links initialized
     */
    public void setIdentityLinksInitialized(boolean isIdentityLinksInitialized) {
        this.isIdentityLinksInitialized = isIdentityLinksInitialized;
    }

    /**
     * Gets the execution id.
     *
     * @return the execution id
     */
    public String getExecutionId() {
        return executionId;
    }

    /**
     * Sets the execution id.
     *
     * @param executionId the new execution id
     */
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    /**
     * Gets the process instance id.
     *
     * @return the process instance id
     */
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    /**
     * Sets the process instance id.
     *
     * @param processInstanceId the new process instance id
     */
    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    /**
     * Gets the process definition id.
     *
     * @return the process definition id
     */
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    /**
     * Sets the process definition id.
     *
     * @param processDefinitionId the new process definition id
     */
    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    /**
     * Gets the scope id.
     *
     * @return the scope id
     */
    public String getScopeId() {
        return scopeId;
    }

    /**
     * Sets the scope id.
     *
     * @param scopeId the new scope id
     */
    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }

    /**
     * Gets the sub scope id.
     *
     * @return the sub scope id
     */
    public String getSubScopeId() {
        return subScopeId;
    }

    /**
     * Sets the sub scope id.
     *
     * @param subScopeId the new sub scope id
     */
    public void setSubScopeId(String subScopeId) {
        this.subScopeId = subScopeId;
    }

    /**
     * Gets the scope type.
     *
     * @return the scope type
     */
    public String getScopeType() {
        return scopeType;
    }

    /**
     * Sets the scope type.
     *
     * @param scopeType the new scope type
     */
    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    /**
     * Gets the scope definition id.
     *
     * @return the scope definition id
     */
    public String getScopeDefinitionId() {
        return scopeDefinitionId;
    }

    /**
     * Sets the scope definition id.
     *
     * @param scopeDefinitionId the new scope definition id
     */
    public void setScopeDefinitionId(String scopeDefinitionId) {
        this.scopeDefinitionId = scopeDefinitionId;
    }

    /**
     * Gets the task definition key.
     *
     * @return the task definition key
     */
    public String getTaskDefinitionKey() {
        return taskDefinitionKey;
    }

    /**
     * Sets the task definition key.
     *
     * @param taskDefinitionKey the new task definition key
     */
    public void setTaskDefinitionKey(String taskDefinitionKey) {
        this.taskDefinitionKey = taskDefinitionKey;
    }

    /**
     * Gets the form key.
     *
     * @return the form key
     */
    public String getFormKey() {
        return formKey;
    }

    /**
     * Sets the form key.
     *
     * @param formKey the new form key
     */
    public void setFormKey(String formKey) {
        this.formKey = formKey;
    }

    /**
     * Checks if is deleted.
     *
     * @return true, if is deleted
     */
    public boolean isDeleted() {
        return isDeleted;
    }

    /**
     * Sets the deleted.
     *
     * @param isDeleted the new deleted
     */
    public void setDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    /**
     * Checks if is canceled.
     *
     * @return true, if is canceled
     */
    public boolean isCanceled() {
        return isCanceled;
    }

    /**
     * Sets the canceled.
     *
     * @param isCanceled the new canceled
     */
    public void setCanceled(boolean isCanceled) {
        this.isCanceled = isCanceled;
    }

    /**
     * Checks if is count enabled.
     *
     * @return true, if is count enabled
     */
    public boolean isCountEnabled() {
        return isCountEnabled;
    }

    /**
     * Sets the count enabled.
     *
     * @param isCountEnabled the new count enabled
     */
    public void setCountEnabled(boolean isCountEnabled) {
        this.isCountEnabled = isCountEnabled;
    }

    /**
     * Gets the variable count.
     *
     * @return the variable count
     */
    public int getVariableCount() {
        return variableCount;
    }

    /**
     * Sets the variable count.
     *
     * @param variableCount the new variable count
     */
    public void setVariableCount(int variableCount) {
        this.variableCount = variableCount;
    }

    /**
     * Gets the claim time.
     *
     * @return the claim time
     */
    public Date getClaimTime() {
        return claimTime;
    }

    /**
     * Sets the claim time.
     *
     * @param claimTime the new claim time
     */
    public void setClaimTime(Date claimTime) {
        this.claimTime = claimTime;
    }

    /**
     * Gets the tenant id.
     *
     * @return the tenant id
     */
    public String getTenantId() {
        return tenantId;
    }

    /**
     * Sets the tenant id.
     *
     * @param tenantId the new tenant id
     */
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * Gets the event name.
     *
     * @return the event name
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * Sets the event name.
     *
     * @param eventName the new event name
     */
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    /**
     * Gets the event handler id.
     *
     * @return the event handler id
     */
    public String getEventHandlerId() {
        return eventHandlerId;
    }

    /**
     * Sets the event handler id.
     *
     * @param eventHandlerId the new event handler id
     */
    public void setEventHandlerId(String eventHandlerId) {
        this.eventHandlerId = eventHandlerId;
    }

    /**
     * Checks if is forced update.
     *
     * @return true, if is forced update
     */
    public boolean isForcedUpdate() {
        return forcedUpdate;
    }

    /**
     * Sets the forced update.
     *
     * @param forcedUpdate the new forced update
     */
    public void setForcedUpdate(boolean forcedUpdate) {
        this.forcedUpdate = forcedUpdate;
    }

    /**
     * Gets the identity link count.
     *
     * @return the identity link count
     */
    public int getIdentityLinkCount() {
        return identityLinkCount;
    }

    /**
     * Sets the identity link count.
     *
     * @param identityLinkCount the new identity link count
     */
    public void setIdentityLinkCount(int identityLinkCount) {
        this.identityLinkCount = identityLinkCount;
    }

}
