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
 * Helpers for the Flowable user-task layer — list outstanding tasks for the calling user, read and
 * write task-local variables, complete a task. Useful inside controllers backing a worklist UI or
 * in script-style steps that finish off a workflow.
 * <p>
 * Task-level variables are scoped to the task and shadow process-level variables of the same name —
 * {@link #complete(String, String)} is what promotes them back to the process scope (and decides
 * which gateway path the engine takes next).
 */
public final class Tasks {

    private Tasks() {}

    /**
     * Returns a JSON array of tasks the calling user (per {@code UserFacade.getName()}) is authorized
     * to act on. The platform pre-filters by candidate user / group; deserialize the JSON in the
     * caller.
     */
    public static String list() {
        return BpmFacadeBridge.invoke("getTasks", new Class<?>[0]);
    }

    public static Object getVariable(String taskId, String name) {
        return BpmFacadeBridge.invoke("getTaskVariable", new Class<?>[] {String.class, String.class}, taskId, name);
    }

    public static Map<String, Object> getVariables(String taskId) {
        return BpmFacadeBridge.invoke("getTaskVariables", new Class<?>[] {String.class}, taskId);
    }

    public static void setVariable(String taskId, String name, Object value) {
        BpmFacadeBridge.invoke("setTaskVariable", new Class<?>[] {String.class, String.class, Object.class}, taskId, name, value);
    }

    public static void setVariables(String taskId, Map<String, Object> variables) {
        BpmFacadeBridge.invoke("setTaskVariables", new Class<?>[] {String.class, Map.class}, taskId, variables);
    }

    /**
     * Marks the task complete with the supplied JSON document promoted onto process scope. Passing
     * {@code null} for {@code variablesJson} leaves the existing scope untouched.
     */
    public static void complete(String taskId, String variablesJson) {
        BpmFacadeBridge.invoke("completeTask", new Class<?>[] {String.class, String.class}, taskId, variablesJson);
    }
}
