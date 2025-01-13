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

/**
 * The Class ProcessInstanceData.
 */
public class ProcessInstanceData {

    /** The business key. */
    private String businessKey;

    /** The business status. */
    private String businessStatus;

    /** The deployment id. */
    private String deploymentId;

    /** The id. */
    private String id;

    /** The name. */
    private String name;

    /** The process definition id. */
    private String processDefinitionId;

    /** The process definition key. */
    private String processDefinitionKey;

    /** The process definition name. */
    private String processDefinitionName;

    /** The process definition version. */
    private int processDefinitionVersion;

    /** The tenant id. */
    private String tenantId;

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
     * Gets the business status.
     *
     * @return the businessStatus
     */
    public String getBusinessStatus() {
        return businessStatus;
    }

    /**
     * Sets the business status.
     *
     * @param businessStatus the businessStatus to set
     */
    public void setBusinessStatus(String businessStatus) {
        this.businessStatus = businessStatus;
    }

    /**
     * Gets the deployment id.
     *
     * @return the deploymentId
     */
    public String getDeploymentId() {
        return deploymentId;
    }

    /**
     * Sets the deployment id.
     *
     * @param deploymentId the deploymentId to set
     */
    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

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
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
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
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the process definition id.
     *
     * @return the processDefinitionId
     */
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    /**
     * Sets the process definition id.
     *
     * @param processDefinitionId the processDefinitionId to set
     */
    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

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
     * Gets the process definition name.
     *
     * @return the processDefinitionName
     */
    public String getProcessDefinitionName() {
        return processDefinitionName;
    }

    /**
     * Sets the process definition name.
     *
     * @param processDefinitionName the processDefinitionName to set
     */
    public void setProcessDefinitionName(String processDefinitionName) {
        this.processDefinitionName = processDefinitionName;
    }

    /**
     * Gets the process definition version.
     *
     * @return the processDefinitionVersion
     */
    public int getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    /**
     * Sets the process definition version.
     *
     * @param processDefinitionVersion the processDefinitionVersion to set
     */
    public void setProcessDefinitionVersion(int processDefinitionVersion) {
        this.processDefinitionVersion = processDefinitionVersion;
    }

    /**
     * Gets the tenant id.
     *
     * @return the tenantId
     */
    public String getTenantId() {
        return tenantId;
    }

    /**
     * Sets the tenant id.
     *
     * @param tenantId the tenantId to set
     */
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }



}
