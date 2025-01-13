/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.bpm;

import org.eclipse.dirigible.components.engine.bpm.flowable.provider.BpmProviderFlowable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * The Class BpmFacade.
 */
@Component
public class BpmFacade implements InitializingBean {

    /** The bpm facade. */
    private static BpmFacade INSTANCE;

    private final BpmProviderFlowable bpmProviderFlowable;

    /**
     * Instantiates a new database facade.
     *
     * @param databaseDefinitionService the database definition service
     * @param dataSourcesManager the data sources manager
     */
    @Autowired
    private BpmFacade(BpmProviderFlowable bpmProviderFlowable) {
        this.bpmProviderFlowable = bpmProviderFlowable;
    }

    /**
     * After properties set.
     *
     * @throws Exception the exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        INSTANCE = this;
    }

    /**
     * BPM Engine.
     *
     * @return the BPM engine object
     */
    public static final BpmProviderFlowable getEngine() {
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
     * @param parameters the serialized in JSON process initial parameters
     * @return the process instance id
     */
    public static String startProcess(String key, String parameters) {
        return BpmFacade.get()
                        .getBpmProviderFlowable()
                        .startProcess(key, parameters);
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
                        .getTasks();
    }

    /**
     * Get all the task's variables.
     *
     * @param taskId the task id
     * @return the task's variables
     */
    public static String getTaskVariables(String taskId) {
        return BpmFacade.get()
                        .getBpmProviderFlowable()
                        .getTaskVariables(taskId);
    }

    /**
     * Set the task's variables.
     *
     * @param taskId the task id
     * @param variables serialized as JSON string
     */
    public static void setTaskVariables(String taskId, String variables) {
        BpmFacade.get()
                 .getBpmProviderFlowable()
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
                 .completeTask(taskId, variables);
    }

}
