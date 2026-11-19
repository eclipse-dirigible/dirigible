/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.repository;

import org.eclipse.dirigible.components.base.artefact.ArtefactRepository;
import org.eclipse.dirigible.components.jobs.domain.JobLog;
import org.eclipse.dirigible.components.jobs.domain.JobStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * The Interface JobLogRepository.
 */
@Repository("jobsLogsRepository")
public interface JobLogRepository extends ArtefactRepository<JobLog, Long> {

    /**
     * Sets the running to all.
     *
     * @param running the new running to all
     */
    @Override
    @Modifying
    @Transactional
    @Query(value = "UPDATE JobLog SET running = :running")
    void setRunningToAll(@Param("running") boolean running);

    /**
     * Deletes the job logs of a tenant - its deprovisioning.
     *
     * @param tenantId the tenant id
     * @return how many were deleted
     */
    @Transactional
    long deleteByTenantId(String tenantId);

    /**
     * Finds the jobs, per tenant, that have a log triggered before the given moment.
     *
     * @param before the moment
     * @return the jobs, each with the tenant it ran for
     */
    @Query("SELECT DISTINCT l.tenantId AS tenantId, l.jobName AS jobName FROM JobLog l WHERE l.triggeredAt < :before")
    List<TenantJob> findJobsWithLogsTriggeredBefore(@Param("before") Timestamp before);

    /**
     * Finds when the last run of a tenant's job was triggered. The entries that mark a run - its
     * TRIGGRED entry and its outcome - all carry the time the run was triggered at.
     *
     * @param tenantId the tenant id
     * @param jobName the job name
     * @param runStatuses the statuses of the entries that mark a run
     * @return when the last run was triggered, or empty when the job has no run logged
     */
    @Query("SELECT MAX(l.triggeredAt) FROM JobLog l WHERE l.tenantId = :tenantId AND l.jobName = :jobName AND l.status IN :runStatuses")
    Optional<Timestamp> findLastRunTriggeredAt(@Param("tenantId") String tenantId, @Param("jobName") String jobName,
            @Param("runStatuses") Collection<JobStatus> runStatuses);

    /**
     * Deletes the logs of a tenant's job triggered before the given moment, in one statement.
     *
     * @param tenantId the tenant id
     * @param jobName the job name
     * @param before the moment
     * @return how many were deleted
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query("DELETE FROM JobLog l WHERE l.tenantId = :tenantId AND l.jobName = :jobName AND l.triggeredAt < :before")
    int deleteTriggeredBefore(@Param("tenantId") String tenantId, @Param("jobName") String jobName, @Param("before") Timestamp before);

    /**
     * A job as one tenant runs it - job logs are kept per tenant.
     */
    interface TenantJob {

        /**
         * Gets the tenant id.
         *
         * @return the tenant id
         */
        String getTenantId();

        /**
         * Gets the job name.
         *
         * @return the job name
         */
        String getJobName();
    }
}
