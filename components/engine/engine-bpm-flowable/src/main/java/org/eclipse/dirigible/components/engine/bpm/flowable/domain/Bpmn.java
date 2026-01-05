/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.bpm.flowable.domain;

import org.eclipse.dirigible.components.base.artefact.Artefact;
import com.google.gson.annotations.Expose;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The Class Bpmn.
 */
@Entity
@Table(name = "DIRIGIBLE_BPMN")
public class Bpmn extends Artefact {

    /** The Constant ARTEFACT_TYPE. */
    public static final String ARTEFACT_TYPE = "bpmn";

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "BPMN_ID", nullable = false)
    private Long id;

    /** The deployment id. */
    @Column(name = "BPMN_DEPLOYMENT_ID", columnDefinition = "VARCHAR", nullable = true, length = 255)
    @Expose
    private String deploymentId;

    /** The process definition id. */
    @Column(name = "BPMN_PROCESS_DEFINITION_ID", columnDefinition = "VARCHAR", nullable = true, length = 255)
    @Expose
    private String processDefinitionId;

    /** The process definition key. */
    @Column(name = "BPMN_PROCESS_DEFINITION_KEY", columnDefinition = "VARCHAR", nullable = true, length = 255)
    @Expose
    private String processDefinitionKey;

    /** The process definition name. */
    @Column(name = "BPMN_PROCESS_DEFINITION_NAME", columnDefinition = "VARCHAR", nullable = true, length = 255)
    @Expose
    private String processDefinitionName;

    /** The process definition version. */
    @Column(name = "BPMN_PROCESS_DEFINITION_VERSION", columnDefinition = "INT", nullable = true)
    @Expose
    private Integer processDefinitionVersion;

    /** The process definition tenant id. */
    @Column(name = "BPMN_PROCESS_DEFINITION_TENANT_ID", columnDefinition = "VARCHAR", nullable = true, length = 255)
    @Expose
    private String processDefinitionTenantId;

    /** The process definition category. */
    @Column(name = "BPMN_PROCESS_DEFINITION_CATEGORY", columnDefinition = "VARCHAR", nullable = true, length = 255)
    @Expose
    private String processDefinitionCategory;

    /** The process definition description. */
    @Column(name = "BPMN_PROCESS_DEFINITION_DESCRIPTION", columnDefinition = "TEXT", nullable = true)
    @Expose
    private String processDefinitionDescription;

    /** The content. */
    @Column(name = "BPMN_CONTENT", columnDefinition = "TEXT", nullable = true)
    @Expose
    private String content;



    /**
     * Gets the id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
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
    public Integer getProcessDefinitionVersion() {
        return processDefinitionVersion;
    }

    /**
     * Sets the process definition version.
     *
     * @param processDefinitionVersion the processDefinitionVersion to set
     */
    public void setProcessDefinitionVersion(Integer processDefinitionVersion) {
        this.processDefinitionVersion = processDefinitionVersion;
    }

    /**
     * Gets the process definition tenant id.
     *
     * @return the processDefinitionTenantId
     */
    public String getProcessDefinitionTenantId() {
        return processDefinitionTenantId;
    }

    /**
     * Sets the process definition tenant id.
     *
     * @param processDefinitionTenantId the processDefinitionTenantId to set
     */
    public void setProcessDefinitionTenantId(String processDefinitionTenantId) {
        this.processDefinitionTenantId = processDefinitionTenantId;
    }

    /**
     * Gets the process definition category.
     *
     * @return the processDefinitionCategory
     */
    public String getProcessDefinitionCategory() {
        return processDefinitionCategory;
    }

    /**
     * Sets the process definition category.
     *
     * @param processDefinitionCategory the processDefinitionCategory to set
     */
    public void setProcessDefinitionCategory(String processDefinitionCategory) {
        this.processDefinitionCategory = processDefinitionCategory;
    }

    /**
     * Gets the process definition description.
     *
     * @return the processDefinitionDescription
     */
    public String getProcessDefinitionDescription() {
        return processDefinitionDescription;
    }

    /**
     * Sets the process definition description.
     *
     * @param processDefinitionDescription the processDefinitionDescription to set
     */
    public void setProcessDefinitionDescription(String processDefinitionDescription) {
        this.processDefinitionDescription = processDefinitionDescription;
    }

    /**
     * Gets the content.
     *
     * @return the content
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the content.
     *
     * @param content the content to set
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "Bpmn [id=" + id + ", location=" + location + ", name=" + name + ", type=" + type + ", description=" + description + ", key="
                + key + ", dependencies=" + dependencies + ", lifecycle=" + lifecycle + ", createdBy=" + createdBy + ", createdAt="
                + createdAt + ", updatedBy=" + updatedBy + ", updatedAt=" + updatedAt + "]";
    }

}
