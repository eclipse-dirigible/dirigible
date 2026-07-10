/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.sdk.bpm;

import java.util.Map;
import org.eclipse.dirigible.components.api.bpm.BpmFacade;
import org.eclipse.dirigible.components.engine.bpm.flowable.config.BpmProviderFlowable;

/**
 * Start, inspect, and steer Flowable process instances from Java code. The static helpers cover the
 * everyday surface — starting a process by definition key, reading and writing instance variables,
 * correlating message events; anything else (sub-process trees, history queries, advanced delegate
 * state) is one step away through the underlying Flowable {@link BpmProviderFlowable} returned by
 * {@link #getEngine()}.
 * <p>
 * Variables are exchanged as native Java values — strings, numbers, booleans, and JSON-friendly
 * {@link Map}s land in Flowable's variable store unchanged. Use this together with the
 * {@code .bpmn} synchronizer for steady-state processes; the {@link Deployer} helper covers
 * programmatic deployment when you need it.
 */
public final class Process {

    private Process() {}

    public static BpmProviderFlowable getEngine() {
        return BpmFacade.getEngine();
    }

    /**
     * Starts a new instance of the process definition with the given key. {@code businessKey} is the
     * application-level correlation id (often a primary key from another table); {@code parametersJson}
     * is a JSON document whose top-level fields become the initial process variables. Returns the new
     * instance id.
     */
    public static String start(String key, String businessKey, String parametersJson) {
        return BpmFacade.startProcess(key, businessKey, parametersJson);
    }

    public static void setProcessInstanceName(String processInstanceId, String name) {
        BpmFacade.setProcessInstanceName(processInstanceId, name);
    }

    public static void updateBusinessKey(String processInstanceId, String businessKey) {
        BpmFacade.updateBusinessKey(processInstanceId, businessKey);
    }

    public static void updateBusinessStatus(String processInstanceId, String businessStatus) {
        BpmFacade.updateBusinessStatus(processInstanceId, businessStatus);
    }

    public static Object getVariable(String processInstanceId, String variableName) {
        return BpmFacade.getVariable(processInstanceId, variableName);
    }

    public static Map<String, Object> getVariables(String processInstanceId) {
        return BpmFacade.getVariables(processInstanceId);
    }

    public static void setVariable(String processInstanceId, String variableName, Object value) {
        BpmFacade.setVariable(processInstanceId, variableName, value);
    }

    public static void removeVariable(String processInstanceId, String variableName) {
        BpmFacade.removeVariable(processInstanceId, variableName);
    }

    /**
     * Delivers a message event with payload variables to all process instances waiting on a matching
     * intermediate-message catch event. The execution is resumed transactionally.
     */
    public static void correlateMessageEvent(String processInstanceId, String messageName, Map<String, Object> variables) {
        BpmFacade.correlateMessageEvent(processInstanceId, messageName, variables);
    }

    /**
     * Run an action after the current BPMN execution chain COMMITS - i.e. after the current service
     * task and every service task that follows it up to the next wait state have completed. Use it from
     * a delegate to publish an event about a workflow transition: an asynchronous consumer that
     * re-loads the entity on receive is then guaranteed to observe every write the chain performed (a
     * number stamped by a later step), instead of racing it. Outside a BPMN execution the action runs
     * immediately.
     *
     * @param action the action to run on commit
     */
    public static void executeAfterCommit(Runnable action) {
        BpmFacade.executeAfterCommit(action);
    }
}
