/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.dto;

/**
 * The Class StartProcessInstanceData.
 */
public class StartProcessInstanceData {

    /** The process definition key. */
    private String processDefinitionKey;

    /** The business key. */
    private String businessKey;

    /** The parameters. */
    private String parameters;

    /**
     * Gets the process definition key.
     *
     * @return the processDefinitionKey
     */
    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    /**
     * Sets the process definition key.
     *
     * @param processDefinitionKey the processDefinitionKey to set
     */
    public void setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    /**
     * Gets the business key.
     *
     * @return the businessKey
     */
    public String getBusinessKey() {
        return businessKey;
    }

    /**
     * Sets the business key.
     *
     * @param businessKey the businessKey to set
     */
    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    /**
     * Gets the parameters.
     *
     * @return the parameters
     */
    public String getParameters() {
        return parameters;
    }

    /**
     * Sets the parameters.
     *
     * @param parameters the parameters to set
     */
    public void setParameters(String parameters) {
        this.parameters = parameters;
    }
}
