/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.base.healthcheck.status;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.dirigible.commons.timeout.TimeLimited;
import org.eclipse.dirigible.components.base.healthcheck.status.HealthCheckStatus.Jobs.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class HealthStatus.
 */
public class HealthCheckStatus {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(HealthCheckStatus.class);

    /** The Constant INSTANCE. */
    private static final HealthCheckStatus INSTANCE = new HealthCheckStatus();

    /** The started. */
    private long started = System.currentTimeMillis();

    /**
     * Gets the single instance of HealthStatus.
     *
     * @return single instance of HealthStatus
     */
    public static HealthCheckStatus getInstance() {
        if (!INSTANCE.status.equals(Status.Ready) && (System.currentTimeMillis() - INSTANCE.started) > TimeLimited.getTimeoutInMillis()) {
            INSTANCE.status = Status.Ready;
            if (logger.isWarnEnabled()) {
                logger.warn("Health status: one or more synchronizers still in progress...");
            }
        }
        return INSTANCE;
    }

    /**
     * Sets the current status.
     */
    private static void setCurrentStatus() {
        HealthCheckStatus healthCheckStatus = getInstance();
        healthCheckStatus.currentStatus = Status.NotReady;
        for (JobStatus next : healthCheckStatus.getJobs()
                                               .getJobsStatuses()) {
            if (next.equals(JobStatus.Running)) {
                healthCheckStatus.currentStatus = Status.Running;
                break;
            } else if (next.equals(JobStatus.Failed)) {
                healthCheckStatus.currentStatus = Status.NotReady;
                break;
            } else if (next.equals(JobStatus.Succeeded)) {
                healthCheckStatus.currentStatus = Status.Ready;
            }
        }
        if (!healthCheckStatus.status.equals(Status.Ready) && healthCheckStatus.currentStatus.equals(Status.Ready)) {
            healthCheckStatus.status = Status.Ready;
        }
    }

    /**
     * The Enum Status.
     */
    public enum Status {

        /** The Ready. */
        Ready,
        /** The Not ready. */
        NotReady,
        /** The Running. */
        Running
    }

    /** The status. */
    private Status status = Status.NotReady;

    /** The current status. */
    private Status currentStatus = Status.NotReady;

    /** The jobs. */
    private Jobs jobs = new Jobs();

    /**
     * Instantiates a new health status.
     */
    private HealthCheckStatus() {

    }

    /**
     * Gets the status.
     *
     * @return the status
     */
    public Status getStatus() {
        setCurrentStatus();
        return status;
    }

    /**
     * Gets the current status.
     *
     * @return the current status
     */
    public Status getCurrentStatus() {
        return currentStatus;
    }

    /**
     * Gets the jobs.
     *
     * @return the jobs
     */
    public Jobs getJobs() {
        return jobs;
    }

    /**
     * The Class Jobs.
     */
    public static class Jobs {

        /**
         * The Enum JobStatus.
         */
        public enum JobStatus {

            /** The Running. */
            Running,
            /** The Succeeded. */
            Succeeded,
            /** The Failed. */
            Failed
        }

        /** The statuses. */
        private Map<String, JobStatus> statuses = new HashMap<String, JobStatus>();

        /**
         * Gets the statuses.
         *
         * @return the statuses
         */
        public Map<String, JobStatus> getStatuses() {
            return statuses;
        }

        /**
         * Sets the status.
         *
         * @param name the name
         * @param status the status
         */
        public void setStatus(String name, JobStatus status) {
            statuses.put(name, status);
            setCurrentStatus();
        }

        /**
         * Gets the jobs statuses.
         *
         * @return the jobs statuses
         */
        private Collection<JobStatus> getJobsStatuses() {
            return statuses.values();
        }

    }
}
