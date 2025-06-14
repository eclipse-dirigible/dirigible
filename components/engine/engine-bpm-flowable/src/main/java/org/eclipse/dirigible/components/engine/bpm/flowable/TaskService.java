/*
 * Copyright (c) 2010-2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable;

import org.eclipse.dirigible.components.engine.bpm.flowable.service.PrincipalType;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.runtime.DataObject;
import org.flowable.engine.task.Attachment;
import org.flowable.engine.task.Comment;
import org.flowable.engine.task.Event;
import org.flowable.form.api.FormInfo;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskBuilder;
import org.flowable.variable.api.persistence.entity.VariableInstance;

import java.io.InputStream;
import java.util.*;

/**
 * Service which provides access to {@link Task} and form related operations.
 *
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public interface TaskService {

    long getTasksCount();

    void unclaimTask(String taskId);

    String getTasks();

    /**
     * Creates a new task that is not related to any process instance.
     *
     * The returned task is transient and must be saved with {@link #saveTask(Task)} 'manually'.
     */
    Task newTask();

    /** create a new task with a user defined task id */
    Task newTask(String taskId);

    /**
     * Create a builder for the task
     *
     * @return task builder
     */
    TaskBuilder createTaskBuilder();

    /**
     * Saves the given task to the persistent data store. If the task is already present in the
     * persistent store, it is updated. After a new task has been saved, the task instance passed into
     * this method is updated with the id of the newly created task.
     *
     * @param task the task, cannot be null.
     */
    void saveTask(Task task);

    /**
     * Saves the given tasks to the persistent data store. If the tasks are already present in the
     * persistent store, it is updated. After a new task has been saved, the task instance passed into
     * this method is updated with the id of the newly created task.
     *
     * @param taskList the list of task instances, cannot be null.
     */
    void bulkSaveTasks(Collection<Task> taskList);

    /**
     * Deletes the given task, not deleting historic information that is related to this task.
     *
     * @param taskId The id of the task that will be deleted, cannot be null. If no task exists with the
     *        given taskId, the operation is ignored.
     * @throws FlowableObjectNotFoundException when the task with given id does not exist.
     * @throws FlowableException when an error occurs while deleting the task or in case the task is
     *         part of a running process.
     */
    void deleteTask(String taskId);

    /**
     * Deletes all tasks of the given collection, not deleting historic information that is related to
     * these tasks.
     *
     * @param taskIds The id's of the tasks that will be deleted, cannot be null. All id's in the list
     *        that don't have an existing task will be ignored.
     * @throws FlowableObjectNotFoundException when one of the task does not exist.
     * @throws FlowableException when an error occurs while deleting the tasks or in case one of the
     *         tasks is part of a running process.
     */
    void deleteTasks(Collection<String> taskIds);

    /**
     * Deletes the given task.
     *
     * @param taskId The id of the task that will be deleted, cannot be null. If no task exists with the
     *        given taskId, the operation is ignored.
     * @param cascade If cascade is true, also the historic information related to this task is deleted.
     * @throws FlowableObjectNotFoundException when the task with given id does not exist.
     * @throws FlowableException when an error occurs while deleting the task or in case the task is
     *         part of a running process.
     */
    void deleteTask(String taskId, boolean cascade);

    /**
     * Deletes all tasks of the given collection.
     *
     * @param taskIds The id's of the tasks that will be deleted, cannot be null. All id's in the list
     *        that don't have an existing task will be ignored.
     * @param cascade If cascade is true, also the historic information related to this task is deleted.
     * @throws FlowableObjectNotFoundException when one of the tasks does not exist.
     * @throws FlowableException when an error occurs while deleting the tasks or in case one of the
     *         tasks is part of a running process.
     */
    void deleteTasks(Collection<String> taskIds, boolean cascade);

    /**
     * Deletes the given task, not deleting historic information that is related to this task..
     *
     * @param taskId The id of the task that will be deleted, cannot be null. If no task exists with the
     *        given taskId, the operation is ignored.
     * @param deleteReason reason the task is deleted. Is recorded in history, if enabled.
     * @throws FlowableObjectNotFoundException when the task with given id does not exist.
     * @throws FlowableException when an error occurs while deleting the task or in case the task is
     *         part of a running process
     */
    void deleteTask(String taskId, String deleteReason);

    /**
     * Deletes all tasks of the given collection, not deleting historic information that is related to
     * these tasks.
     *
     * @param taskIds The id's of the tasks that will be deleted, cannot be null. All id's in the list
     *        that don't have an existing task will be ignored.
     * @param deleteReason reason the task is deleted. Is recorded in history, if enabled.
     * @throws FlowableObjectNotFoundException when one of the tasks does not exist.
     * @throws FlowableException when an error occurs while deleting the tasks or in case one of the
     *         tasks is part of a running process.
     */
    void deleteTasks(Collection<String> taskIds, String deleteReason);

    /**
     * Claim responsibility for a task: the given user is made assignee for the task. The difference
     * with {@link #setAssignee(String, String)} is that here a check is done if the task already has a
     * user assigned to it. No check is done whether the user is known by the identity component.
     *
     * @param taskId task to claim, cannot be null.
     * @param userId user that claims the task. When userId is null the task is unclaimed, assigned to
     *        no one.
     * @throws FlowableObjectNotFoundException when the task doesn't exist.
     * @throws org.flowable.common.engine.api.FlowableTaskAlreadyClaimedException when the task is
     *         already claimed by another user
     */
    void claim(String taskId, String userId);

    Map<String, Object> getTaskVariables(String taskId);

    void setTaskVariable(String taskId, String variableName, Object variable);

    void completeTask(String taskId, String variables);

    List<Task> findTasks(String processInstanceId, PrincipalType type);

    List<Task> findTasks(PrincipalType type);

    void completeTask(String taskId, Map<String, Object> variables);

    List<IdentityLink> getTaskIdentityLinks(String taskId);

    void setTaskVariables(String taskId, Map<String, Object> variables);

    Object getTaskVariable(String taskId, String variableName);

    void claimTask(String taskId, String userId);

    /**
     * A shortcut to {@link #claim} with null user in order to unclaim the task
     *
     * @param taskId task to unclaim, cannot be null.
     * @throws FlowableObjectNotFoundException when the task doesn't exist.
     */
    void unclaim(String taskId);

    /**
     * Set the task state to in progress. No check is done whether the user is known by the identity
     * component.
     *
     * @param taskId task to change the state, cannot be null.
     * @param userId user that puts the task in progress.
     * @throws FlowableObjectNotFoundException when the task doesn't exist.
     */
    void startProgress(String taskId, String userId);

    /**
     * Suspends the task. No check is done whether the user is known by the identity component.
     *
     * @param taskId task to suspend, cannot be null.
     * @param userId user that suspends the task.
     * @throws FlowableObjectNotFoundException when the task doesn't exist.
     */
    void suspendTask(String taskId, String userId);

    /**
     * Activates the task. No check is done whether the user is known by the identity component.
     *
     * @param taskId task to activate, cannot be null.
     * @param userId user that activates the task.
     * @throws FlowableObjectNotFoundException when the task doesn't exist.
     */
    void activateTask(String taskId, String userId);

    /**
     * Delegates the task to another user. This means that the assignee is set and the delegation state
     * is set to {@link DelegationState#PENDING}. If no owner is set on the task, the owner is set to
     * the current assignee of the task.
     *
     * @param taskId The id of the task that will be delegated.
     * @param userId The id of the user that will be set as assignee.
     * @throws FlowableObjectNotFoundException when no task exists with the given id.
     */
    void delegateTask(String taskId, String userId);

    /**
     * Marks that the assignee is done with this task and that it can be send back to the owner. Can
     * only be called when this task is {@link DelegationState#PENDING} delegation. After this method
     * returns, the {@link Task#getDelegationState() delegationState} is set to
     * {@link DelegationState#RESOLVED}.
     *
     * @param taskId the id of the task to resolve, cannot be null.
     * @throws FlowableObjectNotFoundException when no task exists with the given id.
     */
    void resolveTask(String taskId);

    /**
     * Marks that the assignee is done with this task providing the required variables and that it can
     * be sent back to the owner. Can only be called when this task is {@link DelegationState#PENDING}
     * delegation. After this method returns, the {@link Task#getDelegationState() delegationState} is
     * set to {@link DelegationState#RESOLVED}.
     *
     * @param taskId
     * @param variables
     * @throws FlowableObjectNotFoundException When no task exists with the given id.
     */
    void resolveTask(String taskId, Map<String, Object> variables);

    /**
     * Similar to {@link #resolveTask(String, Map)}, but allows to set transient variables too.
     */
    void resolveTask(String taskId, Map<String, Object> variables, Map<String, Object> transientVariables);

    /**
     * Called when the task is successfully executed.
     *
     * @param taskId the id of the task to complete, cannot be null.
     * @throws FlowableObjectNotFoundException when no task exists with the given id.
     * @throws FlowableException when this task is {@link DelegationState#PENDING} delegation.
     */
    void complete(String taskId);

    /**
     * Called when the task is successfully executed.
     *
     * @param taskId the id of the task to complete, cannot be null.
     * @param userId user that completes the task.
     * @throws FlowableObjectNotFoundException when no task exists with the given id.
     * @throws FlowableException when this task is {@link DelegationState#PENDING} delegation.
     */
    void complete(String taskId, String userId);

    /**
     * Called when the task is successfully executed, and the required task parameters are given by the
     * end-user.
     *
     * @param taskId the id of the task to complete, cannot be null.
     * @param variables task parameters. May be null or empty.
     * @throws FlowableObjectNotFoundException when no task exists with the given id.
     */
    void complete(String taskId, Map<String, Object> variables);

    /**
     * Called when the task is successfully executed, and the required task parameters are given by the
     * end-user.
     *
     * @param taskId the id of the task to complete, cannot be null.
     * @param userId user that completes the task.
     * @param variables task parameters. May be null or empty.
     * @throws FlowableObjectNotFoundException when no task exists with the given id.
     */
    void complete(String taskId, String userId, Map<String, Object> variables);

    /**
     * Similar to {@link #complete(String, Map)}, but allows to set transient variables too.
     */
    void complete(String taskId, Map<String, Object> variables, Map<String, Object> transientVariables);

    /**
     * Similar to {@link #complete(String, String, Map)}, but allows to set transient variables too.
     */
    void complete(String taskId, String userId, Map<String, Object> variables, Map<String, Object> transientVariables);

    /**
     * Called when the task is successfully executed, and the required task parameters are given by the
     * end-user.
     *
     * @param taskId the id of the task to complete, cannot be null.
     * @param variables task parameters. May be null or empty.
     * @param localScope If true, the provided variables will be stored task-local, instead of process
     *        instance wide (which is the default for {@link #complete(String, Map)}).
     * @throws FlowableObjectNotFoundException when no task exists with the given id.
     */
    void complete(String taskId, Map<String, Object> variables, boolean localScope);

    /**
     * Called when the task is successfully executed, and the required task parameters are given by the
     * end-user.
     *
     * @param taskId the id of the task to complete, cannot be null.
     * @param userId user that completes the task.
     * @param variables task parameters. May be null or empty.
     * @param localScope If true, the provided variables will be stored task-local, instead of process
     *        instance wide (which is the default for {@link #complete(String, Map)}).
     * @throws FlowableObjectNotFoundException when no task exists with the given id.
     */
    void complete(String taskId, String userId, Map<String, Object> variables, boolean localScope);

    /**
     * Called when the task is successfully executed, and the task form has been submitted.
     *
     * @param taskId the id of the task to complete, cannot be null.
     * @param formDefinitionId the id of the form definition that is filled-in to complete the task,
     *        cannot be null.
     * @param outcome the outcome of the completed form, can be null.
     * @param variables values of the completed form. May be null or empty.
     * @throws FlowableObjectNotFoundException when no task exists with the given id.
     */
    void completeTaskWithForm(String taskId, String formDefinitionId, String outcome, Map<String, Object> variables);

    /**
     * Called when the task is successfully executed, and the task form has been submitted.
     *
     * @param taskId the id of the task to complete, cannot be null.
     * @param formDefinitionId the id of the form definition that is filled-in to complete the task,
     *        cannot be null.
     * @param outcome the outcome of the completed form, can be null.
     * @param userId user that completes the task.
     * @param variables values of the completed form. May be null or empty.
     * @throws FlowableObjectNotFoundException when no task exists with the given id.
     */
    void completeTaskWithForm(String taskId, String formDefinitionId, String outcome, String userId, Map<String, Object> variables);

    /**
     * Called when the task is successfully executed, and the task form has been submitted.
     *
     * @param taskId the id of the task to complete, cannot be null.
     * @param formDefinitionId the id of the form definition that is filled-in to complete the task,
     *        cannot be null.
     * @param outcome the outcome of the completed form, can be null.
     * @param variables values of the completed form. May be null or empty.
     * @param transientVariables additional transient values that need to added to the process instance
     *        transient variables. May be null or empty.
     * @throws FlowableObjectNotFoundException when no task exists with the given id.
     */
    void completeTaskWithForm(String taskId, String formDefinitionId, String outcome, Map<String, Object> variables,
            Map<String, Object> transientVariables);

    /**
     * Called when the task is successfully executed, and the task form has been submitted.
     *
     * @param taskId the id of the task to complete, cannot be null.
     * @param formDefinitionId the id of the form definition that is filled-in to complete the task,
     *        cannot be null.
     * @param outcome the outcome of the completed form, can be null.
     * @param userId user that completes the task.
     * @param variables values of the completed form. May be null or empty.
     * @param transientVariables additional transient values that need to added to the process instance
     *        transient variables. May be null or empty.
     * @throws FlowableObjectNotFoundException when no task exists with the given id.
     */
    void completeTaskWithForm(String taskId, String formDefinitionId, String outcome, String userId, Map<String, Object> variables,
            Map<String, Object> transientVariables);

    /**
     * Called when the task is successfully executed, and the task form has been submitted.
     *
     * @param taskId the id of the task to complete, cannot be null.
     * @param formDefinitionId the id of the form definition that is filled-in to complete the task,
     *        cannot be null.
     * @param outcome the outcome of the completed form, can be null.
     * @param variables values of the completed form. May be null or empty.
     * @param localScope If true, the provided variables will be stored task-local, instead of process
     *        instance wide (which is the default for {@link #complete(String, Map)}).
     * @throws FlowableObjectNotFoundException when no task exists with the given id.
     */
    void completeTaskWithForm(String taskId, String formDefinitionId, String outcome, Map<String, Object> variables, boolean localScope);

    /**
     * Called when the task is successfully executed, and the task form has been submitted.
     *
     * @param taskId the id of the task to complete, cannot be null.
     * @param formDefinitionId the id of the form definition that is filled-in to complete the task,
     *        cannot be null.
     * @param outcome the outcome of the completed form, can be null.
     * @param userId user that completes the task.
     * @param variables values of the completed form. May be null or empty.
     * @param localScope If true, the provided variables will be stored task-local, instead of process
     *        instance wide (which is the default for {@link #complete(String, Map)}).
     * @throws FlowableObjectNotFoundException when no task exists with the given id.
     */
    void completeTaskWithForm(String taskId, String formDefinitionId, String outcome, String userId, Map<String, Object> variables,
            boolean localScope);

    /**
     * Gets a Form model instance of the task form of a specific task
     *
     * @param taskId id of the task, cannot be null.
     * @throws FlowableObjectNotFoundException when the task or form definition doesn't exist.
     */
    FormInfo getTaskFormModel(String taskId);

    /**
     * Gets a Form model instance of the task form of a specific task without any variable handling
     *
     * @param taskId id of the task, cannot be null.
     * @param ignoreVariables should the variables be ignored when fetching the form model?
     * @throws FlowableObjectNotFoundException when the task or form definition doesn't exist.
     */
    FormInfo getTaskFormModel(String taskId, boolean ignoreVariables);

    /**
     * Changes the assignee of the given task to the given userId. No check is done whether the user is
     * known by the identity component.
     *
     * @param taskId id of the task, cannot be null.
     * @param userId id of the user to use as assignee.
     * @throws FlowableObjectNotFoundException when the task or user doesn't exist.
     */
    void setAssignee(String taskId, String userId);

    /**
     * Transfers ownership of this task to another user. No check is done whether the user is known by
     * the identity component.
     *
     * @param taskId id of the task, cannot be null.
     * @param userId of the person that is receiving ownership.
     * @throws FlowableObjectNotFoundException when the task or user doesn't exist.
     */
    void setOwner(String taskId, String userId);

    /**
     * Retrieves the {@link IdentityLink}s associated with the given task. Such an {@link IdentityLink}
     * informs how a certain identity (eg. group or user) is associated with a certain task (eg. as
     * candidate, assignee, etc.)
     */
    List<IdentityLink> getIdentityLinksForTask(String taskId);

    /**
     * Convenience shorthand for {@link #addUserIdentityLink(String, String, String)}; with type
     * {@link IdentityLinkType#CANDIDATE}
     *
     * @param taskId id of the task, cannot be null.
     * @param userId id of the user to use as candidate, cannot be null.
     * @throws FlowableObjectNotFoundException when the task or user doesn't exist.
     */
    void addCandidateUser(String taskId, String userId);

    /**
     * Convenience shorthand for {@link #addGroupIdentityLink(String, String, String)}; with type
     * {@link IdentityLinkType#CANDIDATE}
     *
     * @param taskId id of the task, cannot be null.
     * @param groupId id of the group to use as candidate, cannot be null.
     * @throws FlowableObjectNotFoundException when the task or group doesn't exist.
     */
    void addCandidateGroup(String taskId, String groupId);

    /**
     * Involves a user with a task. The type of identity link is defined by the given identityLinkType.
     *
     * @param taskId id of the task, cannot be null.
     * @param userId id of the user involve, cannot be null.
     * @param identityLinkType type of identityLink, cannot be null (@see {@link IdentityLinkType}).
     * @throws FlowableObjectNotFoundException when the task or user doesn't exist.
     */
    void addUserIdentityLink(String taskId, String userId, String identityLinkType);

    /**
     * Involves a group with a task. The type of identityLink is defined by the given identityLink.
     *
     * @param taskId id of the task, cannot be null.
     * @param groupId id of the group to involve, cannot be null.
     * @param identityLinkType type of identity, cannot be null (@see {@link IdentityLinkType}).
     * @throws FlowableObjectNotFoundException when the task or group doesn't exist.
     */
    void addGroupIdentityLink(String taskId, String groupId, String identityLinkType);

    /**
     * Convenience shorthand for {@link #deleteUserIdentityLink(String, String, String)}; with type
     * {@link IdentityLinkType#CANDIDATE}
     *
     * @param taskId id of the task, cannot be null.
     * @param userId id of the user to use as candidate, cannot be null.
     * @throws FlowableObjectNotFoundException when the task or user doesn't exist.
     */
    void deleteCandidateUser(String taskId, String userId);

    /**
     * Convenience shorthand for {@link #deleteGroupIdentityLink(String, String, String)}; with type
     * {@link IdentityLinkType#CANDIDATE}
     *
     * @param taskId id of the task, cannot be null.
     * @param groupId id of the group to use as candidate, cannot be null.
     * @throws FlowableObjectNotFoundException when the task or group doesn't exist.
     */
    void deleteCandidateGroup(String taskId, String groupId);

    /**
     * Removes the association between a user and a task for the given identityLinkType.
     *
     * @param taskId id of the task, cannot be null.
     * @param userId id of the user involve, cannot be null.
     * @param identityLinkType type of identityLink, cannot be null (@see {@link IdentityLinkType}).
     * @throws FlowableObjectNotFoundException when the task or user doesn't exist.
     */
    void deleteUserIdentityLink(String taskId, String userId, String identityLinkType);

    /**
     * Removes the association between a group and a task for the given identityLinkType.
     *
     * @param taskId id of the task, cannot be null.
     * @param groupId id of the group to involve, cannot be null.
     * @param identityLinkType type of identity, cannot be null (@see {@link IdentityLinkType}).
     * @throws FlowableObjectNotFoundException when the task or group doesn't exist.
     */
    void deleteGroupIdentityLink(String taskId, String groupId, String identityLinkType);

    /**
     * Changes the priority of the task.
     *
     * Authorization: actual owner / business admin
     *
     * @param taskId id of the task, cannot be null.
     * @param priority the new priority for the task.
     * @throws FlowableObjectNotFoundException when the task doesn't exist.
     */
    void setPriority(String taskId, int priority);

    /**
     * Changes the due date of the task
     *
     * @param taskId id of the task, cannot be null.
     * @param dueDate the new due date for the task
     * @throws FlowableException when the task doesn't exist.
     */
    void setDueDate(String taskId, Date dueDate);

    /**
     * set variable on a task. If the variable is not already existing, it will be created in the most
     * outer scope. This means the process instance in case this task is related to an execution.
     */
    void setVariable(String taskId, String variableName, Object value);

    /**
     * set variables on a task. If the variable is not already existing, it will be created in the most
     * outer scope. This means the process instance in case this task is related to an execution.
     */
    void setVariables(String taskId, Map<String, ? extends Object> variables);

    /**
     * set variable on a task. If the variable is not already existing, it will be created in the task.
     */
    void setVariableLocal(String taskId, String variableName, Object value);

    /**
     * set variables on a task. If the variable is not already existing, it will be created in the task.
     */
    void setVariablesLocal(String taskId, Map<String, ? extends Object> variables);

    /**
     * get a variables and search in the task scope and if available also the execution scopes.
     */
    Object getVariable(String taskId, String variableName);

    /**
     * get a variables and search in the task scope and if available also the execution scopes.
     */
    <T> T getVariable(String taskId, String variableName, Class<T> variableClass);

    /**
     * The variable. Searching for the variable is done in all scopes that are visible to the given task
     * (including parent scopes). Returns null when no variable value is found with the given name.
     *
     * @param taskId id of task, cannot be null.
     * @param variableName name of variable, cannot be null.
     * @return the variable or null if the variable is undefined.
     * @throws FlowableObjectNotFoundException when no execution is found for the given taskId.
     */
    VariableInstance getVariableInstance(String taskId, String variableName);

    /**
     * checks whether or not the task has a variable defined with the given name, in the task scope and
     * if available also the execution scopes.
     */
    boolean hasVariable(String taskId, String variableName);

    /**
     * checks whether or not the task has a variable defined with the given name.
     */
    Object getVariableLocal(String taskId, String variableName);

    /**
     * checks whether or not the task has a variable defined with the given name.
     */
    <T> T getVariableLocal(String taskId, String variableName, Class<T> variableClass);

    /**
     * The variable for a task. Returns the variable when it is set for the task (and not searching
     * parent scopes). Returns null when no variable is found with the given name.
     *
     * @param taskId id of task, cannot be null.
     * @param variableName name of variable, cannot be null.
     * @return the variable or null if the variable is undefined.
     * @throws FlowableObjectNotFoundException when no task is found for the given taskId.
     */
    VariableInstance getVariableInstanceLocal(String taskId, String variableName);

    /**
     * checks whether or not the task has a variable defined with the given name, local task scope only.
     */
    boolean hasVariableLocal(String taskId, String variableName);

    /**
     * get all variables and search in the task scope and if available also the execution scopes. If you
     * have many variables and you only need a few, consider using
     * {@link #getVariables(String, Collection)} for better performance.
     */
    Map<String, Object> getVariables(String taskId);

    /**
     * All variables visible from the given task scope (including parent scopes).
     *
     * @param taskId id of task, cannot be null.
     * @return the variable instances or an empty map if no such variables are found.
     * @throws FlowableObjectNotFoundException when no task is found for the given taskId.
     */
    Map<String, VariableInstance> getVariableInstances(String taskId);

    /**
     * The variable values for all given variableNames, takes all variables into account which are
     * visible from the given task scope (including parent scopes).
     *
     * @param taskId id of taskId, cannot be null.
     * @param variableNames the collection of variable names that should be retrieved.
     * @return the variables or an empty map if no such variables are found.
     * @throws FlowableObjectNotFoundException when no taskId is found for the given taskId.
     */
    Map<String, VariableInstance> getVariableInstances(String taskId, Collection<String> variableNames);

    /**
     * get all variables and search only in the task scope. If you have many task local variables and
     * you only need a few, consider using {@link #getVariablesLocal(String, Collection)} for better
     * performance.
     */
    Map<String, Object> getVariablesLocal(String taskId);

    /**
     * get values for all given variableNames and search only in the task scope.
     */
    Map<String, Object> getVariables(String taskId, Collection<String> variableNames);

    /** get a variable on a task */
    Map<String, Object> getVariablesLocal(String taskId, Collection<String> variableNames);

    /** get all variables and search only in the task scope. */
    List<VariableInstance> getVariableInstancesLocalByTaskIds(Set<String> taskIds);

    /**
     * All variable values that are defined in the task scope, without taking outer scopes into account.
     * If you have many task local variables and you only need a few, consider using
     * {@link #getVariableInstancesLocal(String, Collection)} for better performance.
     *
     * @param taskId id of task, cannot be null.
     * @return the variables or an empty map if no such variables are found.
     * @throws FlowableObjectNotFoundException when no task is found for the given taskId.
     */
    Map<String, VariableInstance> getVariableInstancesLocal(String taskId);

    /**
     * The variable values for all given variableNames that are defined in the given task's scope. (Does
     * not searching parent scopes).
     *
     * @param taskId id of taskId, cannot be null.
     * @param variableNames the collection of variable names that should be retrieved.
     * @return the variables or an empty map if no such variables are found.
     * @throws FlowableObjectNotFoundException when no taskId is found for the given taskId.
     */
    Map<String, VariableInstance> getVariableInstancesLocal(String taskId, Collection<String> variableNames);

    /**
     * Removes the variable from the task. When the variable does not exist, nothing happens.
     */
    void removeVariable(String taskId, String variableName);

    /**
     * Removes the variable from the task (not considering parent scopes). When the variable does not
     * exist, nothing happens.
     */
    void removeVariableLocal(String taskId, String variableName);

    /**
     * Removes all variables in the given collection from the task. Non existing variable names are
     * simply ignored.
     */
    void removeVariables(String taskId, Collection<String> variableNames);

    /**
     * Removes all variables in the given collection from the task (not considering parent scopes). Non
     * existing variable names are simply ignored.
     */
    void removeVariablesLocal(String taskId, Collection<String> variableNames);

    /**
     * All DataObjects visible from the given execution scope (including parent scopes).
     *
     * @param taskId id of task, cannot be null.
     * @return the DataObjects or an empty map if no such variables are found.
     * @throws FlowableObjectNotFoundException when no task is found for the given taskId.
     */
    Map<String, DataObject> getDataObjects(String taskId);

    /**
     * All DataObjects visible from the given task scope (including parent scopes).
     *
     * @param taskId id of task, cannot be null.
     * @param locale locale the DataObject name and description should be returned in (if available).
     * @param withLocalizationFallback When true localization will fallback to more general locales if
     *        the specified locale is not found.
     * @return the DataObjects or an empty map if no DataObjects are found.
     * @throws FlowableObjectNotFoundException when no task is found for the given task.
     */
    Map<String, DataObject> getDataObjects(String taskId, String locale, boolean withLocalizationFallback);

    /**
     * The DataObjects for all given dataObjectNames, takes all dataObjects into account which are
     * visible from the given task scope (including parent scopes).
     *
     * @param taskId id of task, cannot be null.
     * @param dataObjectNames the collection of DataObject names that should be retrieved.
     * @return the DataObject or an empty map if no DataObjects are found.
     * @throws FlowableObjectNotFoundException when no task is found for the given taskId.
     */
    Map<String, DataObject> getDataObjects(String taskId, Collection<String> dataObjectNames);

    /**
     * The DataObjects for all given dataObjectNames, takes all dataObjects into account which are
     * visible from the given task scope (including parent scopes).
     *
     * @param taskId id of task, cannot be null.
     * @param dataObjectNames the collection of DataObject names that should be retrieved.
     * @param locale locale the DataObject name and description should be returned in (if available).
     * @param withLocalizationFallback When true localization will fallback to more general locales if
     *        the specified locale is not found.
     * @return the DataObjects or an empty map if no such dataObjects are found.
     * @throws FlowableObjectNotFoundException when no task is found for the given task.
     */
    Map<String, DataObject> getDataObjects(String taskId, Collection<String> dataObjectNames, String locale,
            boolean withLocalizationFallback);

    /**
     * The DataObject. Searching for the DataObject is done in all scopes that are visible to the given
     * task (including parent scopes). Returns null when no DataObject value is found with the given
     * name.
     *
     * @param taskId id of task, cannot be null.
     * @param dataObject name of DataObject, cannot be null.
     * @return the DataObject or null if the variable is undefined.
     * @throws FlowableObjectNotFoundException when no task is found for the given taskId.
     */
    DataObject getDataObject(String taskId, String dataObject);

    /**
     * The DataObject. Searching for the DataObject is done in all scopes that are visible to the given
     * task (including parent scopes). Returns null when no DataObject value is found with the given
     * name.
     *
     * @param taskId id of task, cannot be null.
     * @param dataObjectName name of DataObject, cannot be null.
     * @param locale locale the DataObject name and description should be returned in (if available).
     * @param withLocalizationFallback When true localization will fallback to more general locales
     *        including the default locale of the JVM if the specified locale is not found.
     * @return the DataObject or null if the DataObject is undefined.
     * @throws FlowableObjectNotFoundException when no task is found for the given taskId.
     */
    DataObject getDataObject(String taskId, String dataObjectName, String locale, boolean withLocalizationFallback);

    /** Add a comment to a task and/or process instance. */
    Comment addComment(String taskId, String processInstanceId, String message);

    /** Add a comment to a task and/or process instance with a custom type. */
    Comment addComment(String taskId, String processInstanceId, String type, String message);

    /** Update a comment to a task and/or process instance. */
    void saveComment(Comment comment);

    /**
     * Returns an individual comment with the given id. Returns null if no comment exists with the given
     * id.
     */
    Comment getComment(String commentId);

    /** Removes all comments from the provided task and/or process instance */
    void deleteComments(String taskId, String processInstanceId);

    /**
     * Removes an individual comment with the given id.
     *
     * @throws FlowableObjectNotFoundException when no comment exists with the given id.
     */
    void deleteComment(String commentId);

    /** The comments related to the given task. */
    List<Comment> getTaskComments(String taskId);

    /** The comments related to the given task of the given type. */
    List<Comment> getTaskComments(String taskId, String type);

    /** All comments of a given type. */
    List<Comment> getCommentsByType(String type);

    /** The all events related to the given task. */
    List<Event> getTaskEvents(String taskId);

    /**
     * Returns an individual event with the given id. Returns null if no event exists with the given id.
     */
    Event getEvent(String eventId);

    /** The comments related to the given process instance. */
    List<Comment> getProcessInstanceComments(String processInstanceId);

    /** The comments related to the given process instance. */
    List<Comment> getProcessInstanceComments(String processInstanceId, String type);

    /**
     * Add a new attachment to a task and/or a process instance and use an input stream to provide the
     * content
     */
    Attachment createAttachment(String attachmentType, String taskId, String processInstanceId, String attachmentName,
            String attachmentDescription, InputStream content);

    /**
     * Add a new attachment to a task and/or a process instance and use an url as the content
     */
    Attachment createAttachment(String attachmentType, String taskId, String processInstanceId, String attachmentName,
            String attachmentDescription, String url);

    /** Update the name and description of an attachment */
    void saveAttachment(Attachment attachment);

    /** Retrieve a particular attachment */
    Attachment getAttachment(String attachmentId);

    /** Retrieve stream content of a particular attachment */
    InputStream getAttachmentContent(String attachmentId);

    /** The list of attachments associated to a task */
    List<Attachment> getTaskAttachments(String taskId);

    /** The list of attachments associated to a process instance */
    List<Attachment> getProcessInstanceAttachments(String processInstanceId);

    /** Delete an attachment */
    void deleteAttachment(String attachmentId);

    /** The list of subtasks for this parent task */
    List<Task> getSubTasks(String parentTaskId);
}
