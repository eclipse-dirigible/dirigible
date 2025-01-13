/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.Transient;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * The JobParameterDefinition serialization object.
 */
@Entity
@Table(name = "DIRIGIBLE_JOB_PARAMETERS")
public class JobParameter {

    /** The Constant ARTEFACT_TYPE. */
    public static final String ARTEFACT_TYPE = "job-parameter";

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "JOBPARAM_ID", nullable = false)
    private Long id;

    /** The name. */
    @Column(name = "JOBPARAM_NAME", columnDefinition = "VARCHAR", nullable = false, length = 255)
    private String name;

    /** The type. */
    @Column(name = "JOBPARAM_TYPE", columnDefinition = "VARCHAR", nullable = false, length = 255)
    private String type; // string, number, boolean

    /** The default value. */
    @Column(name = "JOBPARAM_DEFAULT_VALUE", columnDefinition = "VARCHAR", nullable = true, length = 2000)
    private String defaultValue = "";

    /** The choices. */
    @Column(name = "JOBPARAM_CHOICES", columnDefinition = "VARCHAR", nullable = true, length = 2000)
    private String choices = "";

    /** The value. */
    @Transient
    @Column(name = "JOBPARAM_VALUE", columnDefinition = "VARCHAR", nullable = true, length = 2000)
    private String value;

    /** The job. */
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "JOB_ID", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private Job job;

    /**
     * Instantiates a new job parameter.
     */
    public JobParameter() {
        super();
        this.type = ARTEFACT_TYPE;
    }

    /**
     * Instantiates a new job parameter.
     *
     * @param name the name
     * @param type the param type
     * @param defaultValue the default value
     * @param choices the choices
     * @param value the value
     * @param job the job
     */
    public JobParameter(String name, String type, String defaultValue, String choices, String value, Job job) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
        this.choices = choices;
        this.value = value;
        this.job = job;
    }

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
     * @param id the new id
     */
    public void setId(Long id) {
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
     * Gets the parameter type.
     *
     * @return the param type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the param type.
     *
     * @param type the new param type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets the default value.
     *
     * @return the default value
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * Sets the default value.
     *
     * @param defaultValue the new default value
     */
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * Gets the choices.
     *
     * @return the choices
     */
    public String getChoices() {
        return choices;
    }

    /**
     * Sets the choices.
     *
     * @param choices the new choices
     */
    public void setChoices(String choices) {
        this.choices = choices;
    }

    /**
     * Gets the value.
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value.
     *
     * @param value the new value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the job.
     *
     * @return the job
     */
    public Job getJob() {
        return job;
    }

    /**
     * Sets the job.
     *
     * @param job the new job
     */
    public void setJob(Job job) {
        this.job = job;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "JobParameter{" + "id=" + id + ", name=" + name + ", type=" + type + ", defaultValue=" + defaultValue + ", choices="
                + choices + ", value=" + value + ", job=" + job.getName() + ", type=" + type + '}';
    }
}
