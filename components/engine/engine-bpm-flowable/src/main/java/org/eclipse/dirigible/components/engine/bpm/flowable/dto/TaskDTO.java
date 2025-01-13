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

import java.util.Date;

/**
 * The Class TaskDTO.
 */
public class TaskDTO {

    /** The id. */
    private String id;

    /** The name. */
    private String name;

    /** The form key. */
    private String formKey;

    /** The assignee. */
    private String assignee;

    /** The candidate users. */
    private String candidateUsers;

    /** The candidate groups. */
    private String candidateGroups;

    /** The create time. */
    private Date createTime;

    /** The process instance id. */
    private String processInstanceId;

    /**
     * Gets the candidate users.
     *
     * @return the candidate users
     */
    public String getCandidateUsers() {
        return candidateUsers;
    }

    /**
     * Sets the candidate users.
     *
     * @param candidateUsers the new candidate users
     */
    public void setCandidateUsers(String candidateUsers) {
        this.candidateUsers = candidateUsers;
    }

    /**
     * Gets the candidate groups.
     *
     * @return the candidate groups
     */
    public String getCandidateGroups() {
        return candidateGroups;
    }

    /**
     * Sets the candidate groups.
     *
     * @param candidateGroups the new candidate groups
     */
    public void setCandidateGroups(String candidateGroups) {
        this.candidateGroups = candidateGroups;
    }


    /**
     * Gets the process instance id.
     *
     * @return the process instance id
     */
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    /**
     * Sets the process instance id.
     *
     * @param processInstanceId the new process instance id
     */
    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
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
     * @param id the new id
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
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the form key.
     *
     * @return the form key
     */
    public String getFormKey() {
        return formKey;
    }

    /**
     * Sets the form key.
     *
     * @param formKey the new form key
     */
    public void setFormKey(String formKey) {
        this.formKey = formKey;
    }

    /**
     * Gets the assignee.
     *
     * @return the assignee
     */
    public String getAssignee() {
        return assignee;
    }

    /**
     * Sets the assignee.
     *
     * @param assignee the new assignee
     */
    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    /**
     * Gets the creates the time.
     *
     * @return the creates the time
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * Sets the creates the time.
     *
     * @param createTime the new creates the time
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
