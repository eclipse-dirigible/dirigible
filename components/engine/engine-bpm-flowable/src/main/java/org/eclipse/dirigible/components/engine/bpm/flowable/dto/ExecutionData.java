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

/**
 * The Class ExecutionData.
 */
public class ExecutionData {

    /** The id. */
    protected String id;

    /** The revision. */
    protected int revision;

    /** The is inserted. */
    protected boolean isInserted;

    /** The is updated. */
    protected boolean isUpdated;

    /** The is deleted. */
    protected boolean isDeleted;

    /** The tenant id. */
    protected String tenantId = "";

    /** The name. */
    protected String name;

    /** The description. */
    protected String description;

    /** The localized name. */
    protected String localizedName;

    /** The localized description. */
    protected String localizedDescription;

    /** The lock time. */
    protected Date lockTime;

    /** The is active. */
    protected boolean isActive = true;

    /** The is scope. */
    protected boolean isScope = true;

    /** The is concurrent. */
    protected boolean isConcurrent;

    /** The is ended. */
    protected boolean isEnded;

    /** The is event scope. */
    protected boolean isEventScope;

    /** The is multi instance root. */
    protected boolean isMultiInstanceRoot;

    /** The is count enabled. */
    protected boolean isCountEnabled;

    /** The event name. */
    protected String eventName;

    /** The delete reason. */
    protected String deleteReason;

    /** The suspension state. */
    protected int suspensionState = 1;

    /** The start activity id. */
    protected String startActivityId;

    /** The start user id. */
    protected String startUserId;

    /** The start time. */
    protected Date startTime;

    /** The event subscription count. */
    protected int eventSubscriptionCount;

    /** The task count. */
    protected int taskCount;

    /** The job count. */
    protected int jobCount;

    /** The timer job count. */
    protected int timerJobCount;

    /** The suspended job count. */
    protected int suspendedJobCount;

    /** The dead letter job count. */
    protected int deadLetterJobCount;

    /** The variable count. */
    protected int variableCount;

    /** The identity link count. */
    protected int identityLinkCount;

    /** The process definition id. */
    protected String processDefinitionId;

    /** The process definition key. */
    protected String processDefinitionKey;

    /** The process definition name. */
    protected String processDefinitionName;

    /** The process definition version. */
    protected Integer processDefinitionVersion;

    /** The deployment id. */
    protected String deploymentId;

    /** The activity id. */
    protected String activityId;

    /** The activity name. */
    protected String activityName;

    /** The process instance id. */
    protected String processInstanceId;

    /** The business key. */
    protected String businessKey;

    /** The parent id. */
    protected String parentId;

    /** The super execution id. */
    protected String superExecutionId;

    /** The root process instance id. */
    protected String rootProcessInstanceId;

    /** The forced update. */
    protected boolean forcedUpdate;

    /** The callback id. */
    protected String callbackId;

    /** The callback type. */
    protected String callbackType;

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
     * Checks if is inserted.
     *
     * @return true, if is inserted
     */
    public boolean isInserted() {
        return isInserted;
    }

    /**
     * Sets the inserted.
     *
     * @param isInserted the new inserted
     */
    public void setInserted(boolean isInserted) {
        this.isInserted = isInserted;
    }

    /**
     * Checks if is updated.
     *
     * @return true, if is updated
     */
    public boolean isUpdated() {
        return isUpdated;
    }

    /**
     * Sets the updated.
     *
     * @param isUpdated the new updated
     */
    public void setUpdated(boolean isUpdated) {
        this.isUpdated = isUpdated;
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
     * Gets the lock time.
     *
     * @return the lock time
     */
    public Date getLockTime() {
        return lockTime;
    }

    /**
     * Sets the lock time.
     *
     * @param lockTime the new lock time
     */
    public void setLockTime(Date lockTime) {
        this.lockTime = lockTime;
    }

    /**
     * Checks if is active.
     *
     * @return true, if is active
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Sets the active.
     *
     * @param isActive the new active
     */
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * Checks if is scope.
     *
     * @return true, if is scope
     */
    public boolean isScope() {
        return isScope;
    }

    /**
     * Sets the scope.
     *
     * @param isScope the new scope
     */
    public void setScope(boolean isScope) {
        this.isScope = isScope;
    }

    /**
     * Checks if is concurrent.
     *
     * @return true, if is concurrent
     */
    public boolean isConcurrent() {
        return isConcurrent;
    }

    /**
     * Sets the concurrent.
     *
     * @param isConcurrent the new concurrent
     */
    public void setConcurrent(boolean isConcurrent) {
        this.isConcurrent = isConcurrent;
    }

    /**
     * Checks if is ended.
     *
     * @return true, if is ended
     */
    public boolean isEnded() {
        return isEnded;
    }

    /**
     * Sets the ended.
     *
     * @param isEnded the new ended
     */
    public void setEnded(boolean isEnded) {
        this.isEnded = isEnded;
    }

    /**
     * Checks if is event scope.
     *
     * @return true, if is event scope
     */
    public boolean isEventScope() {
        return isEventScope;
    }

    /**
     * Sets the event scope.
     *
     * @param isEventScope the new event scope
     */
    public void setEventScope(boolean isEventScope) {
        this.isEventScope = isEventScope;
    }

    /**
     * Checks if is multi instance root.
     *
     * @return true, if is multi instance root
     */
    public boolean isMultiInstanceRoot() {
        return isMultiInstanceRoot;
    }

    /**
     * Sets the multi instance root.
     *
     * @param isMultiInstanceRoot the new multi instance root
     */
    public void setMultiInstanceRoot(boolean isMultiInstanceRoot) {
        this.isMultiInstanceRoot = isMultiInstanceRoot;
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
     * Gets the delete reason.
     *
     * @return the delete reason
     */
    public String getDeleteReason() {
        return deleteReason;
    }

    /**
     * Sets the delete reason.
     *
     * @param deleteReason the new delete reason
     */
    public void setDeleteReason(String deleteReason) {
        this.deleteReason = deleteReason;
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
     * Gets the start activity id.
     *
     * @return the start activity id
     */
    public String getStartActivityId() {
        return startActivityId;
    }

    /**
     * Sets the start activity id.
     *
     * @param startActivityId the new start activity id
     */
    public void setStartActivityId(String startActivityId) {
        this.startActivityId = startActivityId;
    }

    /**
     * Gets the start user id.
     *
     * @return the start user id
     */
    public String getStartUserId() {
        return startUserId;
    }

    /**
     * Sets the start user id.
     *
     * @param startUserId the new start user id
     */
    public void setStartUserId(String startUserId) {
        this.startUserId = startUserId;
    }

    /**
     * Gets the start time.
     *
     * @return the start time
     */
    public Date getStartTime() {
        return startTime;
    }

    /**
     * Sets the start time.
     *
     * @param startTime the new start time
     */
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    /**
     * Gets the event subscription count.
     *
     * @return the event subscription count
     */
    public int getEventSubscriptionCount() {
        return eventSubscriptionCount;
    }

    /**
     * Sets the event subscription count.
     *
     * @param eventSubscriptionCount the new event subscription count
     */
    public void setEventSubscriptionCount(int eventSubscriptionCount) {
        this.eventSubscriptionCount = eventSubscriptionCount;
    }

    /**
     * Gets the task count.
     *
     * @return the task count
     */
    public int getTaskCount() {
        return taskCount;
    }

    /**
     * Sets the task count.
     *
     * @param taskCount the new task count
     */
    public void setTaskCount(int taskCount) {
        this.taskCount = taskCount;
    }

    /**
     * Gets the job count.
     *
     * @return the job count
     */
    public int getJobCount() {
        return jobCount;
    }

    /**
     * Sets the job count.
     *
     * @param jobCount the new job count
     */
    public void setJobCount(int jobCount) {
        this.jobCount = jobCount;
    }

    /**
     * Gets the timer job count.
     *
     * @return the timer job count
     */
    public int getTimerJobCount() {
        return timerJobCount;
    }

    /**
     * Sets the timer job count.
     *
     * @param timerJobCount the new timer job count
     */
    public void setTimerJobCount(int timerJobCount) {
        this.timerJobCount = timerJobCount;
    }

    /**
     * Gets the suspended job count.
     *
     * @return the suspended job count
     */
    public int getSuspendedJobCount() {
        return suspendedJobCount;
    }

    /**
     * Sets the suspended job count.
     *
     * @param suspendedJobCount the new suspended job count
     */
    public void setSuspendedJobCount(int suspendedJobCount) {
        this.suspendedJobCount = suspendedJobCount;
    }

    /**
     * Gets the dead letter job count.
     *
     * @return the dead letter job count
     */
    public int getDeadLetterJobCount() {
        return deadLetterJobCount;
    }

    /**
     * Sets the dead letter job count.
     *
     * @param deadLetterJobCount the new dead letter job count
     */
    public void setDeadLetterJobCount(int deadLetterJobCount) {
        this.deadLetterJobCount = deadLetterJobCount;
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
     * Gets the process definition key.
     *
     * @return the process definition key
     */
    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    /**
     * Sets the process definition key.
     *
     * @param processDefinitionKey the new process definition key
     */
    public void setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    /**
     * Gets the process definition name.
     *
     * @return the process definition name
     */
    public String getProcessDefinitionName() {
        return processDefinitionName;
    }

    /**
     * Sets the process definition name.
     *
     * @param processDefinitionName the new process definition name
     */
    public void setProcessDefinitionName(String processDefinitionName) {
        this.processDefinitionName = processDefinitionName;
    }

    /**
     * Gets the process definition version.
     *
     * @return the process definition version
     */
    public Integer getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    /**
     * Sets the process definition version.
     *
     * @param processDefinitionVersion the new process definition version
     */
    public void setProcessDefinitionVersion(Integer processDefinitionVersion) {
        this.processDefinitionVersion = processDefinitionVersion;
    }

    /**
     * Gets the deployment id.
     *
     * @return the deployment id
     */
    public String getDeploymentId() {
        return deploymentId;
    }

    /**
     * Sets the deployment id.
     *
     * @param deploymentId the new deployment id
     */
    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    /**
     * Gets the activity id.
     *
     * @return the activity id
     */
    public String getActivityId() {
        return activityId;
    }

    /**
     * Sets the activity id.
     *
     * @param activityId the new activity id
     */
    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    /**
     * Gets the activity name.
     *
     * @return the activity name
     */
    public String getActivityName() {
        return activityName;
    }

    /**
     * Sets the activity name.
     *
     * @param activityName the new activity name
     */
    public void setActivityName(String activityName) {
        this.activityName = activityName;
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
     * Gets the business key.
     *
     * @return the business key
     */
    public String getBusinessKey() {
        return businessKey;
    }

    /**
     * Sets the business key.
     *
     * @param businessKey the new business key
     */
    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    /**
     * Gets the parent id.
     *
     * @return the parent id
     */
    public String getParentId() {
        return parentId;
    }

    /**
     * Sets the parent id.
     *
     * @param parentId the new parent id
     */
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    /**
     * Gets the super execution id.
     *
     * @return the super execution id
     */
    public String getSuperExecutionId() {
        return superExecutionId;
    }

    /**
     * Sets the super execution id.
     *
     * @param superExecutionId the new super execution id
     */
    public void setSuperExecutionId(String superExecutionId) {
        this.superExecutionId = superExecutionId;
    }

    /**
     * Gets the root process instance id.
     *
     * @return the root process instance id
     */
    public String getRootProcessInstanceId() {
        return rootProcessInstanceId;
    }

    /**
     * Sets the root process instance id.
     *
     * @param rootProcessInstanceId the new root process instance id
     */
    public void setRootProcessInstanceId(String rootProcessInstanceId) {
        this.rootProcessInstanceId = rootProcessInstanceId;
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
     * Gets the callback id.
     *
     * @return the callback id
     */
    public String getCallbackId() {
        return callbackId;
    }

    /**
     * Sets the callback id.
     *
     * @param callbackId the new callback id
     */
    public void setCallbackId(String callbackId) {
        this.callbackId = callbackId;
    }

    /**
     * Gets the callback type.
     *
     * @return the callback type
     */
    public String getCallbackType() {
        return callbackType;
    }

    /**
     * Sets the callback type.
     *
     * @param callbackType the new callback type
     */
    public void setCallbackType(String callbackType) {
        this.callbackType = callbackType;
    }

}
