/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.domain;

import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import org.eclipse.dirigible.components.base.artefact.Artefact;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.springframework.data.annotation.Transient;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The Class Job.
 */
@Entity
@Table(name = "DIRIGIBLE_JOBS")
public class Job extends Artefact {

    /** The Constant ARTEFACT_TYPE. */
    public static final String ARTEFACT_TYPE = "job";

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "JOB_ID", nullable = false)
    private Long id;

    /**
     * The group.
     */
    @Column(name = "JOB_GROUP", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    private String group;

    /**
     * The clazz.
     */
    @Column(name = "JOB_CLASS", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    private String clazz = "";

    /**
     * The expression.
     */
    @Column(name = "JOB_EXPRESSION", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    private String expression;

    /**
     * The handler.
     */
    @Column(name = "JOB_HANDLER", columnDefinition = "VARCHAR", nullable = true, length = 255)
    @Expose
    private String handler;

    /**
     * The engine.
     */
    @Column(name = "JOB_ENGINE", columnDefinition = "VARCHAR", nullable = true, length = 100)
    @Expose
    private String engine;

    /**
     * The singleton.
     */
    @Column(name = "JOB_SINGLETON", columnDefinition = "BOOLEAN", nullable = false)
    @Expose
    private Boolean singleton = false;

    /**
     * The enabled.
     */
    @Column(name = "JOB_ENABLED", columnDefinition = "BOOLEAN", nullable = false)
    @Expose
    private Boolean enabled = true;

    /**
     * The parameters.
     */
    @OneToMany(mappedBy = "job", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    @Transient
    private List<JobParameter> parameters = new ArrayList<>();

    /** The status. */
    @Column(name = "JOB_STATUS", nullable = true)
    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.UNKNOWN;

    /**
     * The message.
     */
    @Column(name = "JOB_MESSAGE", columnDefinition = "VARCHAR", nullable = true, length = 2000)
    @Expose
    private String message;

    /**
     * The executed at.
     */
    @Column(name = "JOB_EXECUTED_AT", columnDefinition = "TIMESTAMP", nullable = true)
    @Expose
    private Timestamp executedAt;

    /**
     * Instantiates a new job.
     *
     * @param location the location
     * @param name the name
     * @param description the description
     * @param dependencies the dependencies
     * @param group the group
     * @param clazz the clazz
     * @param expression the expression
     * @param handler the handler
     * @param engine the engine
     * @param singleton the singleton
     * @param enabled the enabled
     * @param status the status
     * @param message the message
     * @param executedAt the executed at
     */
    public Job(String location, String name, String description, Set<String> dependencies, String group, String clazz, String expression,
            String handler, String engine, Boolean singleton, Boolean enabled, JobStatus status, String message, Timestamp executedAt) {
        super(location, name, ARTEFACT_TYPE, description, dependencies);
        this.group = group;
        this.clazz = clazz;
        this.expression = expression;
        this.handler = handler;
        this.engine = engine;
        this.singleton = singleton;
        this.enabled = enabled;
        this.status = status;
        this.message = message;
        this.executedAt = executedAt;
    }

    /**
     * Instantiates a new job.
     *
     * @param name the name
     * @param group the group
     * @param clazz the clazz
     * @param handler the handler
     * @param engine the engine
     * @param description the description
     * @param expression the expression
     * @param singleton the singleton
     * @param parameters the parameters
     * @param location the location
     * @param dependencies the dependencies
     */
    public Job(String name, String group, String clazz, String handler, String engine, String description, String expression,
            Boolean singleton, List<JobParameter> parameters, String location, Set<String> dependencies) {
        super(location, name, ARTEFACT_TYPE, description, dependencies);
        this.group = group;
        this.clazz = clazz;
        this.expression = expression;
        this.handler = handler;
        this.engine = engine;
        this.singleton = singleton;
        this.parameters = parameters;
    }

    /**
     * Instantiates a new job.
     */
    public Job() {
        super();
        this.type = ARTEFACT_TYPE;
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
     * Gets the group.
     *
     * @return the group
     */
    public String getGroup() {
        return group;
    }

    /**
     * Sets the group.
     *
     * @param group the new group
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * Gets the clazz.
     *
     * @return the clazz
     */
    public String getClazz() {
        return clazz;
    }

    /**
     * Sets the clazz.
     *
     * @param clazz the new clazz
     */
    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    /**
     * Gets the expression.
     *
     * @return the expression
     */
    public String getExpression() {
        return expression;
    }

    /**
     * Sets the expression.
     *
     * @param expression the new expression
     */
    public void setExpression(String expression) {
        this.expression = expression;
    }

    /**
     * Gets the handler.
     *
     * @return the handler
     */
    public String getHandler() {
        return handler;
    }

    /**
     * Sets the handler.
     *
     * @param handler the new handler
     */
    public void setHandler(String handler) {
        this.handler = handler;
    }

    /**
     * Gets the engine.
     *
     * @return the engine
     */
    public String getEngine() {
        return engine;
    }

    /**
     * Sets the engine.
     *
     * @param engine the new engine
     */
    public void setEngine(String engine) {
        this.engine = engine;
    }

    /**
     * Checks if is singleton.
     *
     * @return true, if is singleton
     */
    public Boolean isSingleton() {
        return singleton;
    }

    /**
     * Sets the singleton.
     *
     * @param singleton the new singleton
     */
    public void setSingleton(Boolean singleton) {
        this.singleton = singleton;
    }

    /**
     * Checks if is enabled.
     *
     * @return true, if is enabled
     */
    public Boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the enabled.
     *
     * @param enabled the new enabled
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets the status.
     *
     * @return the status
     */
    public JobStatus getStatus() {
        return status;
    }

    /**
     * Sets the status.
     *
     * @param status the new status
     */
    public void setStatus(JobStatus status) {
        this.status = status;
    }

    /**
     * Gets the parameters list.
     *
     * @return the parameters list
     */
    public List<JobParameter> getParameters() {
        return parameters;
    }

    /**
     * Sets the parameters list.
     *
     * @param parameters the new parameters list
     */
    public void setParameters(List<JobParameter> parameters) {
        this.parameters = parameters;
    }

    /**
     * Get the parameter by name.
     *
     * @param name the name
     * @return the parameter
     */
    public JobParameter getParameter(String name) {
        for (JobParameter p : parameters) {
            if (p.getName()
                 .equals(name)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Gets the message.
     *
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message.
     *
     * @param message the new message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets the executed at.
     *
     * @return the executed at
     */
    public Timestamp getExecutedAt() {
        return executedAt;
    }

    /**
     * Sets the executed at.
     *
     * @param executedAt the new executed at
     */
    public void setExecutedAt(Timestamp executedAt) {
        this.executedAt = executedAt;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "Job {" + " id=" + id + ", group=" + group + ", clazz=" + clazz + ", expression=" + expression + ", handler=" + handler
                + ", engine=" + engine + ", singleton=" + singleton + ", enabled=" + enabled + ", parameters=" + parameters + ", status="
                + status + ", message=" + message + ", executedAt=" + executedAt + ", location=" + location + ", name=" + name + ", type="
                + type + ", description=" + description + ", key=" + key + ", dependencies=" + dependencies + ", createdBy=" + createdBy
                + ", createdAt=" + createdAt + ", updatedBy=" + updatedBy + ", updatedAt=" + updatedAt + '}';
    }
}
