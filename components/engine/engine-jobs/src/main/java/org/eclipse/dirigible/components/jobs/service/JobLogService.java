/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
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
import org.eclipse.dirigible.components.jobs.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    /** The logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(JobLogService.class);

    /** The date format. */
    private final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    /** The job email processor. */
    private final JobEmailProcessor jobEmailProcessor;

    /** The job repository - the owning job's last-run fields are stamped through it. */
    private final JobRepository jobRepository;

    /** The tenant context. */
    private final TenantContext tenantContext;

    /** The default tenant. */
    private final Tenant defaultTenant;

    /**
     * Instantiates a new job log service.
     *
     * @param repository the repository
     * @param jobEmailProcessor the job email processor
     * @param jobRepository the job repository
     * @param tenantContext the tenant context
     * @param defaultTenant the default tenant
     */
    public JobLogService(JobLogRepository repository, JobEmailProcessor jobEmailProcessor, JobRepository jobRepository,
            TenantContext tenantContext, @DefaultTenant Tenant defaultTenant) {
        super(repository);
        this.jobEmailProcessor = jobEmailProcessor;
        this.jobRepository = jobRepository;
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
        Job job = findJob(name);
        if (job == null) {
            return jobLog;
        }
        boolean statusChanged = job.getStatus() != JobStatus.FINISHED;
        job.setStatus(JobStatus.FINISHED);
        job.setMessage("");
        job.setExecutedAt(jobLog.getFinishedAt());
        jobRepository.saveAndFlush(job);
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
        Job job = findJob(name);
        if (job == null) {
            return jobLog;
        }
        boolean statusChanged = job.getStatus() != JobStatus.FAILED;
        job.setStatus(JobStatus.FAILED);
        job.setMessage(message);
        job.setExecutedAt(jobLog.getFinishedAt());
        jobRepository.saveAndFlush(job);
        if (statusChanged) {
            String content =
                    jobEmailProcessor.prepareEmail(job, JobEmailProcessor.emailTemplateError, JobEmailProcessor.EMAIL_TEMPLATE_ERROR);
            jobEmailProcessor.sendEmail(job, JobEmailProcessor.emailSubjectError, content);
        }
        return jobLog;
    }

    /**
     * Looks up the job whose run is being recorded. A log may outlive its artefact - a job deleted
     * between the trigger and the finish leaves nothing to stamp, which is not a failure of the run.
     *
     * @param name the job name
     * @return the job, or null when no artefact by that name exists any more
     */
    private Job findJob(String name) {
        String jobName = (name != null && name.startsWith("/")) ? name.substring(1) : name;
        Job job = jobRepository.findByName(jobName)
                               .orElse(null);
        if (job == null) {
            LOGGER.warn("Job [{}] has no artefact - its last run was logged but not stamped on the job.", jobName);
        }
        return job;
    }

    /**
     * Delete all by job name.
     *
     * @param jobName the job name
     */
    public void deleteAllByJobName(String jobName) {
        JobLog filter = createJobLog();
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
        JobLog filter = createJobLog();
        if (name != null && name.startsWith("/")) {
            name = name.substring(1);
        }
        filter.setJobName(name);
        filter.setStatus(null);
        Example<JobLog> example = Example.of(filter);
        return getRepo().findAll(example);
    }
}
