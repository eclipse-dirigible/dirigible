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

    public static final String ARTEFACT_TYPE = "job";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "JOB_ID", nullable = false)
    private Long id;

    @Column(name = "JOB_GROUP", nullable = false, length = 255)
    @Expose
    private String group;

    @Column(name = "JOB_CLASS", nullable = false, length = 255)
    @Expose
    private String clazz = "";

    @Column(name = "JOB_EXPRESSION", nullable = false, length = 255)
    @Expose
    private String expression;

    @Column(name = "JOB_HANDLER", length = 255)
    @Expose
    private String handler;

    @Column(name = "JOB_ENGINE", length = 100)
    @Expose
    private String engine;

    @Column(name = "JOB_SINGLETON", nullable = false)
    @Expose
    private Boolean singleton = false;

    @Column(name = "JOB_ENABLED", nullable = false)
    @Expose
    private Boolean enabled = true;

    /**
     * NOT persisted. Populated externally.
     */
    @jakarta.persistence.Transient
    private List<JobParameter> parameters = new ArrayList<>();

    @Column(name = "JOB_STATUS")
    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.UNKNOWN;

    @Column(name = "JOB_MESSAGE", length = 2000)
    @Expose
    private String message;

    @Column(name = "JOB_EXECUTED_AT")
    @Expose
    private Timestamp executedAt;

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

    public Job() {
        super();
        this.type = ARTEFACT_TYPE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getClazz() {
        return clazz;
    }

    public void setClazz(String clazz) {
        this.clazz = clazz;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public Boolean isSingleton() {
        return singleton;
    }

    public void setSingleton(Boolean singleton) {
        this.singleton = singleton;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public List<JobParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<JobParameter> parameters) {
        this.parameters = parameters;
    }

    public JobParameter getParameter(String name) {
        for (JobParameter p : parameters) {
            if (p.getName()
                 .equals(name)) {
                return p;
            }
        }
        return null;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Timestamp getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Timestamp executedAt) {
        this.executedAt = executedAt;
    }

    @Override
    public String toString() {
        return "Job {" + "id=" + id + ", group=" + group + ", clazz=" + clazz + ", expression=" + expression + ", handler=" + handler
                + ", engine=" + engine + ", singleton=" + singleton + ", enabled=" + enabled + ", parameters=" + parameters + ", status="
                + status + ", message=" + message + ", executedAt=" + executedAt + '}';
    }
}
