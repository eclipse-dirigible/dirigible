/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.dto;

/**
 * Where a process' display names are translated. A generated process declares the i18n catalog of
 * its module ({@code <project>:<model>-model.processes}) on its {@code <process>} element; within
 * that catalog a name is keyed by its BPMN id - the process' own id for the process name, a task's
 * id (the authored step name) for a task name.
 *
 * <p>
 * This is what lets a shell surface shared by every deployed application - the Inbox, the
 * notification bell, the task-form dialog - name a task in the user's language without knowing
 * which module raised it.
 *
 * @param catalog the declared catalog, never blank
 * @param processNameKey the key of the process' own name within it
 */
public record ProcessLabelKeys(String catalog, String processNameKey) {

    /**
     * The key of a task's name within this catalog.
     *
     * @param taskDefinitionKey the task's BPMN id, i.e. the authored step name
     * @return the translation key, or null when the task carries no definition key
     */
    public String taskNameKey(String taskDefinitionKey) {
        return taskDefinitionKey == null ? null : catalog + "." + taskDefinitionKey;
    }

}
