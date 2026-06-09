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
import org.eclipse.dirigible.sdk.bpm.internal.BpmFacadeBridge;

/**
 * Start, inspect, and steer Flowable process instances from Java code. The static helpers cover the
 * everyday surface — starting a process by definition key, reading and writing instance variables,
 * correlating message events; anything else (sub-process trees, history queries, advanced delegate
 * state) is one step away through the underlying Flowable {@code ProcessEngine}, which you can
 * reach via the {@code BpmFacade.getEngine()} bridge inside platform code.
 * <p>
 * Variables are exchanged as native Java values — strings, numbers, booleans, and JSON-friendly
 * {@link Map}s land in Flowable's variable store unchanged. Use this together with the
 * {@code .bpmn} synchronizer for steady-state processes; the
 * {@link org.eclipse.dirigible.sdk.bpm.Deployer Deployer} helper covers programmatic deployment
 * when you need it.
 */
public final class Process {

    private Process() {}

    /**
     * Starts a new instance of the process definition with the given key. {@code businessKey} is the
     * application-level correlation id (often a primary key from another table); {@code parametersJson}
     * is a JSON document whose top-level fields become the initial process variables. Returns the new
     * instance id.
     */
    public static String start(String key, String businessKey, String parametersJson) {
        return BpmFacadeBridge.invoke("startProcess", new Class<?>[] {String.class, String.class, String.class}, key, businessKey,
                parametersJson);
    }

    public static void setProcessInstanceName(String processInstanceId, String name) {
        BpmFacadeBridge.invoke("setProcessInstanceName", new Class<?>[] {String.class, String.class}, processInstanceId, name);
    }

    public static void updateBusinessKey(String processInstanceId, String businessKey) {
        BpmFacadeBridge.invoke("updateBusinessKey", new Class<?>[] {String.class, String.class}, processInstanceId, businessKey);
    }

    public static void updateBusinessStatus(String processInstanceId, String businessStatus) {
        BpmFacadeBridge.invoke("updateBusinessStatus", new Class<?>[] {String.class, String.class}, processInstanceId, businessStatus);
    }

    public static Object getVariable(String processInstanceId, String variableName) {
        return BpmFacadeBridge.invoke("getVariable", new Class<?>[] {String.class, String.class}, processInstanceId, variableName);
    }

    public static Map<String, Object> getVariables(String processInstanceId) {
        return BpmFacadeBridge.invoke("getVariables", new Class<?>[] {String.class}, processInstanceId);
    }

    public static void setVariable(String processInstanceId, String variableName, Object value) {
        BpmFacadeBridge.invoke("setVariable", new Class<?>[] {String.class, String.class, Object.class}, processInstanceId, variableName,
                value);
    }

    public static void removeVariable(String processInstanceId, String variableName) {
        BpmFacadeBridge.invoke("removeVariable", new Class<?>[] {String.class, String.class}, processInstanceId, variableName);
    }

    /**
     * Delivers a message event with payload variables to all process instances waiting on a matching
     * intermediate-message catch event. The execution is resumed transactionally.
     */
    public static void correlateMessageEvent(String processInstanceId, String messageName, Map<String, Object> variables) {
        BpmFacadeBridge.invoke("correlateMessageEvent", new Class<?>[] {String.class, String.class, Map.class}, processInstanceId,
                messageName, variables);
    }
}
