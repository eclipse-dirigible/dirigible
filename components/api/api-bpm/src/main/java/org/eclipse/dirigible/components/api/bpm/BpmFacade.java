/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.bpm;

import java.util.Map;
import org.eclipse.dirigible.components.engine.bpm.flowable.config.BpmProviderFlowable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The Class BpmFacade.
 */
@Component
public class BpmFacade implements InitializingBean {

    /** The bpm facade. */
    private static BpmFacade INSTANCE;

    private final BpmProviderFlowable bpmProviderFlowable;

    @Autowired
    private BpmFacade(BpmProviderFlowable bpmProviderFlowable) {
        this.bpmProviderFlowable = bpmProviderFlowable;
    }

    /**
     * After properties set.
     */
    @Override
    public void afterPropertiesSet() {
        INSTANCE = this;
    }

    /**
     * BPM Engine.
     *
     * @return the BPM engine object
     */
    public static BpmProviderFlowable getEngine() {
        return BpmFacade.get()
                        .getBpmProviderFlowable();
    }

    /**
     * Gets the instance.
     *
     * @return the database facade
     */
    public static BpmFacade get() {
        return INSTANCE;
    }

    public BpmProviderFlowable getBpmProviderFlowable() {
        return bpmProviderFlowable;
    }

    /**
     * Deploy a BPMN process available in the registry or in the class-path.
     *
     * @param location the BPMN resource location
     * @return the deployment id
     */
    public static String deployProcess(String location) {
        return BpmFacade.get()
                        .getBpmProviderFlowable()
                        .deployProcess(location);
    }

    /**
     * Undeploy a BPMN process and all its dependencies.
     *
     * @param deploymentId the BPMN process definition deployment id
     */
    public static void undeployProcess(String deploymentId) {
        BpmFacade.get()
                 .getBpmProviderFlowable()
                 .undeployProcess(deploymentId);
    }

    /**
     * Starts a BPMN process by its key and initial parameters.
     *
     * @param key the BPMN id of the process
     * @param businessKey the business key of the process
     * @param parameters the serialized in JSON process initial parameters
     * @return the process instance id
     */
    public static String startProcess(String key, String businessKey, String parameters) {
        return BpmFacade.get()
                        .getBpmProviderFlowable()
                        .startProcess(key, businessKey, parameters);
    }

    /**
     * Sets the process instance name.
     *
     * @param processInstanceId the process instance id
     * @param name the name
     */
    public static void setProcessInstanceName(String processInstanceId, String name) {
        BpmFacade.get()
                 .getBpmProviderFlowable()
                 .setProcessInstanceName(processInstanceId, name);
    }

    /**
     * Updates the business key.
     *
     * @param processInstanceId the process instance id
     * @param businessKey the business key
     */
    public static void updateBusinessKey(String processInstanceId, String businessKey) {
        BpmFacade.get()
                 .getBpmProviderFlowable()
                 .updateBusinessKey(processInstanceId, businessKey);
    }

    /**
     * Updates the business status.
     *
     * @param processInstanceId the process instance id
     * @param businessStatus the business status
     */
    public static void updateBusinessStatus(String processInstanceId, String businessStatus) {
        BpmFacade.get()
                 .getBpmProviderFlowable()
                 .updateBusinessStatus(processInstanceId, businessStatus);
    }

    /**
     * Delete a BPMN process by its id.
     *
     * @param id the id
     * @param reason the reason for deletion
     */
    public static void deleteProcess(String id, String reason) {
        BpmFacade.get()
                 .getBpmProviderFlowable()
                 .deleteProcess(id, reason);
    }

    /**
     * Get a variable in the process execution context.
     *
     * @param processInstanceId the process instance id
     * @param variableName the variable name
     * @return the value
     */
    public static Object getVariable(String processInstanceId, String variableName) {
        return BpmFacade.get()
                        .getBpmProviderFlowable()
                        .getVariable(processInstanceId, variableName);
    }

    public static Map<String, Object> getVariables(String processInstanceId) {
        return BpmFacade.get()
                        .getBpmProviderFlowable()
                        .getVariables(processInstanceId);
    }

    /**
     * Set a variable in the process execution context.
     *
     * @param processInstanceId the process instance id
     * @param variableName the variable name
     * @param value the value object
     */
    public static void setVariable(String processInstanceId, String variableName, Object value) {
        BpmFacade.get()
                 .getBpmProviderFlowable()
                 .setVariable(processInstanceId, variableName, value);
    }

    /**
     * Remove a variable from the process execution context.
     *
     * @param processInstanceId the process instance id
     * @param variableName the variable name
     */
    public static void removeVariable(String processInstanceId, String variableName) {
        BpmFacade.get()
                 .getBpmProviderFlowable()
                 .removeVariable(processInstanceId, variableName);
    }

    /**
     * Get all the tasks.
     *
     * @return the list of tasks
     */
    public static String getTasks() {
        return BpmFacade.get()
                        .getBpmProviderFlowable()
                        .getTaskService()
                        .getTasks();
    }

    /**
     * Get task's variable.
     *
     * @param taskId the task id
     * @param variableName the variable name
     * @return the task's variables
     */
    public static Object getTaskVariable(String taskId, String variableName) {
        return BpmFacade.get()
                        .getBpmProviderFlowable()
                        .getTaskService()
                        .getTaskVariable(taskId, variableName);
    }

    /**
     * Get all the task's variables.
     *
     * @param taskId the task id
     * @return the task's variables
     */
    public static Map<String, Object> getTaskVariables(String taskId) {
        return BpmFacade.get()
                        .getBpmProviderFlowable()
                        .getTaskService()
                        .getTaskVariables(taskId);
    }

    /**
     * Set task's variable.
     *
     * @param taskId the task id
     * @param variableName the variable name
     * @param variable the variable
     */
    public static void setTaskVariable(String taskId, String variableName, Object variable) {
        BpmFacade.get()
                 .getBpmProviderFlowable()
                 .getTaskService()
                 .setTaskVariable(taskId, variableName, variable);
    }

    /**
     * Set the task's variables.
     *
     * @param taskId the task id
     * @param variables the variables
     */
    public static void setTaskVariables(String taskId, Map<String, Object> variables) {
        BpmFacade.get()
                 .getBpmProviderFlowable()
                 .getTaskService()
                 .setTaskVariables(taskId, variables);
    }

    /**
     * Complete the task with variables.
     *
     * @param taskId the task id
     * @param variables serialized as JSON string
     */
    public static void completeTask(String taskId, String variables) {
        BpmFacade.get()
                 .getBpmProviderFlowable()
                 .getTaskService()
                 .completeTask(taskId, variables);
    }

    /**
     * Set the task's variables.
     *
     * @param processInstanceId the process instance id
     * @param messageName the name of the message event
     * @param variables the variables to be passed with the message vent
     */
    public static void correlateMessageEvent(String processInstanceId, String messageName, Map<String, Object> variables) {
        BpmFacade.get()
                 .getBpmProviderFlowable()
                 .correlateMessageEvent(processInstanceId, messageName, variables);
    }
}
