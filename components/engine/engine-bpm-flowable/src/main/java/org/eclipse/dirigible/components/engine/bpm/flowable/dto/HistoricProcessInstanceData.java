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

import org.flowable.engine.history.HistoricProcessInstance;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * A finished process instance as the history listing answers it: every property the engine records,
 * unchanged and at the top level (the instance is unwrapped), plus the i18n key of its process'
 * name, so a shell can name the process in the viewer's language as it does a running one.
 */
public final class HistoricProcessInstanceData {

    /** The instance, as the engine recorded it. */
    private final HistoricProcessInstance instance;

    /** The i18n key of the process' name, null when the process declares no catalog. */
    private final String processDefinitionNameKey;

    /**
     * Pairs a finished instance with the translation key of its process' name.
     *
     * @param instance the instance, as the engine recorded it
     * @param processDefinitionNameKey the translation key, or null when the process declares no catalog
     */
    public HistoricProcessInstanceData(HistoricProcessInstance instance, String processDefinitionNameKey) {
        this.instance = instance;
        this.processDefinitionNameKey = processDefinitionNameKey;
    }

    /**
     * The instance, serialized in place - its properties are this object's.
     *
     * @return the instance
     */
    @JsonUnwrapped
    public HistoricProcessInstance getInstance() {
        return instance;
    }

    /**
     * The fully-qualified i18n key of the process' display name.
     *
     * @return the translation key, or null when the process declares no catalog (e.g. a hand-authored
     *         one)
     */
    public String getProcessDefinitionNameKey() {
        return processDefinitionNameKey;
    }

}
