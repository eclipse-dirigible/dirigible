/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.service;

import org.eclipse.dirigible.components.base.artefact.BaseArtefactService;
import org.eclipse.dirigible.components.base.tenant.DefaultTenant;
import org.eclipse.dirigible.components.base.tenant.Tenant;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.jobs.domain.Job;
import org.eclipse.dirigible.components.jobs.domain.JobLog;
import org.eclipse.dirigible.components.jobs.domain.JobStatus;
import org.eclipse.dirigible.components.jobs.email.JobEmailProcessor;
import org.eclipse.dirigible.components.jobs.repository.JobLogRepository;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * The Class JobLogService.
 */
@Service
@Transactional
public class JobLogService extends BaseArtefactService<JobLog, Long> {

    /** The date format. */
    private final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    /** The job email processor. */
    private final JobEmailProcessor jobEmailProcessor;

    /** The job service. */
    private final JobService jobService;

    /** The tenant context. */
    private final TenantContext tenantContext;

    /** The default tenant. */
    private final Tenant defaultTenant;

    /**
     * Instantiates a new job log service.
     *
     * @param repository the repository
     * @param jobEmailProcessor the job email processor
     * @param jobService the job service
     * @param tenantContext the tenant context
     * @param defaultTenant the default tenant
     */
    public JobLogService(JobLogRepository repository, JobEmailProcessor jobEmailProcessor, JobService jobService,
            TenantContext tenantContext, @DefaultTenant Tenant defaultTenant) {
        super(repository);
        this.jobEmailProcessor = jobEmailProcessor;
        this.jobService = jobService;
        this.tenantContext = tenantContext;
        this.defaultTenant = defaultTenant;
    }

    /**
     * Job triggered.
     *
     * @param name the name
     * @param handler the handler
     * @return the job log definition
     */
    public JobLog jobTriggered(String name, String handler) {
        JobLog jobLog = createJobLog();
        jobLog.setName(name);
        jobLog.setJobName(name);
        jobLog.setHandler(handler);
        jobLog.setStatus(JobStatus.TRIGGRED);
        jobLog.setTriggeredAt(new Timestamp(new Date().getTime()));
        jobLog.setLocation(new SimpleDateFormat(DATE_FORMAT).format(new Date()));
        jobLog.updateKey();
        save(jobLog);
        return jobLog;
    }

    /**
     * Creates the job log.
     *
     * @return the job log
     */
    private JobLog createJobLog() {
        JobLog jobLog = new JobLog();
        String tenantId = tenantContext.isNotInitialized() ? defaultTenant.getId()
                : tenantContext.getCurrentTenant()
                               .getId();
        jobLog.setTenantId(tenantId);
        return jobLog;
    }

    /**
     * Job logged.
     *
     * @param name the name
     * @param handler the handler
     * @param message the message
     * @return the job log definition
     */
    public JobLog jobLogged(String name, String handler, String message) {
        return jobLogged(name, handler, message, JobStatus.LOGGED);
    }

    /**
     * Job logged.
     *
     * @param name the name
     * @param handler the handler
     * @param message the message
     * @param status the status
     * @return the job log definition
     */
    private JobLog jobLogged(String name, String handler, String message, JobStatus status) {
        JobLog jobLog = createJobLog();
        jobLog.setName(name);
        jobLog.setJobName(name);
        jobLog.setHandler(handler);
        jobLog.setMessage(message);
        jobLog.setStatus(status);
        jobLog.setTriggeredAt(new Timestamp(new Date().getTime()));
        jobLog.setLocation(new SimpleDateFormat(DATE_FORMAT).format(new Date()));
        jobLog.updateKey();
        save(jobLog);
        return jobLog;
    }

    /**
     * Job logged error.
     *
     * @param name the name
     * @param handler the handler
     * @param message the message
     * @return the job log definition
     */
    public JobLog jobLoggedError(String name, String handler, String message) {
        return jobLogged(name, handler, message, JobStatus.ERROR);
    }

    /**
     * Job logged warning.
     *
     * @param name the name
     * @param handler the handler
     * @param message the message
     * @return the job log definition
     */
    public JobLog jobLoggedWarning(String name, String handler, String message) {
        return jobLogged(name, handler, message, JobStatus.WARN);
    }

    /**
     * Job logged info.
     *
     * @param name the name
     * @param handler the handler
     * @param message the message
     * @return the job log definition
     */
    public JobLog jobLoggedInfo(String name, String handler, String message) {
        return jobLogged(name, handler, message, JobStatus.INFO);
    }

    /**
     * Job finished.
     *
     * @param name the name
     * @param handler the handler
     * @param triggeredId the triggered id
     * @param triggeredAt the triggered at
     * @return the job log definition
     */
    public JobLog jobFinished(String name, String handler, long triggeredId, Date triggeredAt) {
        JobLog jobLog = createJobLog();
        jobLog.setName(name);
        jobLog.setJobName(name);
        jobLog.setHandler(handler);
        jobLog.setStatus(JobStatus.FINISHED);
        jobLog.setTriggeredId(triggeredId);
        jobLog.setTriggeredAt(new Timestamp(triggeredAt.getTime()));
        jobLog.setFinishedAt(new Timestamp(new Date().getTime()));
        jobLog.setLocation(new SimpleDateFormat(DATE_FORMAT).format(new Date()));
        jobLog.updateKey();
        save(jobLog);
        Job job = jobService.findByName(name);
        boolean statusChanged = job.getStatus() != JobStatus.FINISHED;
        job.setStatus(JobStatus.FINISHED);
        job.setMessage("");
        job.setExecutedAt(jobLog.getFinishedAt());
        if (statusChanged) {
            String content =
                    jobEmailProcessor.prepareEmail(job, JobEmailProcessor.emailTemplateNormal, JobEmailProcessor.EMAIL_TEMPLATE_NORMAL);
            jobEmailProcessor.sendEmail(job, JobEmailProcessor.emailSubjectNormal, content);
        }
        return jobLog;
    }

    /**
     * Job failed.
     *
     * @param name the name
     * @param handler the handler
     * @param triggeredId the triggered id
     * @param triggeredAt the triggered at
     * @param message the message
     * @return the job log definition
     */
    public JobLog jobFailed(String name, String handler, long triggeredId, Date triggeredAt, String message) {
        JobLog jobLog = createJobLog();
        jobLog.setName(name);
        jobLog.setJobName(name);
        jobLog.setHandler(handler);
        jobLog.setStatus(JobStatus.FAILED);
        jobLog.setTriggeredId(triggeredId);
        jobLog.setTriggeredAt(new Timestamp(triggeredAt.getTime()));
        jobLog.setFinishedAt(new Timestamp(new Date().getTime()));
        jobLog.setMessage(message);
        jobLog.setLocation(new SimpleDateFormat(DATE_FORMAT).format(new Date()));
        jobLog.updateKey();
        save(jobLog);
        Job job = jobService.findByName(name);
        boolean statusChanged = job.getStatus() != JobStatus.FAILED;
        job.setStatus(JobStatus.FAILED);
        job.setMessage(message);
        job.setExecutedAt(jobLog.getFinishedAt());
        if (statusChanged) {
            String content =
                    jobEmailProcessor.prepareEmail(job, JobEmailProcessor.emailTemplateError, JobEmailProcessor.EMAIL_TEMPLATE_ERROR);
            jobEmailProcessor.sendEmail(job, JobEmailProcessor.emailSubjectError, content);
        }
        return jobLog;
    }

    /**
     * Delete all by job name.
     *
     * @param jobName the job name
     */
    public void deleteAllByJobName(String jobName) {
        // createJobLog() if we want only logs for the current tenant otherwise new JobLog()
        JobLog filter = new JobLog();
        if (jobName != null && jobName.startsWith("/")) {
            jobName = jobName.substring(1);
        }
        filter.setJobName(jobName);
        Example<JobLog> example = Example.of(filter);
        List<JobLog> jobLogs = getRepo().findAll(example);
        getRepo().deleteAll(jobLogs);

    }

    /**
     * Find by name.
     *
     * @param name the name
     * @return the job log
     */
    @Transactional(readOnly = true)
    public List<JobLog> findByJob(String name) {
        // createJobLog() if we want only logs for the current tenant otherwise new JobLog()
        JobLog filter = new JobLog();
        if (name != null && name.startsWith("/")) {
            name = name.substring(1);
        }
        filter.setJobName(name);
        filter.setStatus(null);
        Example<JobLog> example = Example.of(filter);
        return getRepo().findAll(example);
    }
}
