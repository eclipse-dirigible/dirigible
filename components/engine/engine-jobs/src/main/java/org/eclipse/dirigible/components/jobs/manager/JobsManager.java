/*
 * Copyright (c) 2024 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.jobs.manager;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.dirigible.components.base.tenant.TenantContext;
import org.eclipse.dirigible.components.jobs.handler.JobHandler;
import org.eclipse.dirigible.components.jobs.tenant.JobNameCreator;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static org.quartz.CronScheduleBuilder.cronSchedule;

/**
 * The Scheduler Manager.
 */
@Component
public class JobsManager {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(JobsManager.class);

    /** The user defined jobs. */
    private static final String JOB_GROUP_DEFINED = "defined";

    /** The scheduler. */
    private final Scheduler scheduler;

    /** The tenant context. */
    private final TenantContext tenantContext;

    /** The job name creator. */
    private final JobNameCreator jobNameCreator;

    /**
     * Instantiates a new jobs manager.
     *
     * @param scheduler the scheduler
     * @param tenantContext the tenant context
     * @param jobNameCreator the job name creator
     */
    JobsManager(Scheduler scheduler, TenantContext tenantContext, JobNameCreator jobNameCreator) {
        this.scheduler = scheduler;
        this.tenantContext = tenantContext;
        this.jobNameCreator = jobNameCreator;
    }

    /**
     * Schedule a job.
     *
     * @param jobDefinition the job definition
     * @throws Exception the exception
     */
    public void scheduleJob(org.eclipse.dirigible.components.jobs.domain.Job jobDefinition) throws Exception {
        JobKey jobKey = createJobKey(jobDefinition);
        if (!jobDefinition.isEnabled()) {
            if (scheduler.checkExists(jobKey)) {
                LOGGER.debug("Deleting job with key [{}]...", jobKey);
                scheduler.deleteJob(jobKey);
            }
            LOGGER.debug("Job [{}] is NOT enabled and will not be scheduled.", jobDefinition);
            return;
        }
        try {
            TriggerKey triggerKey = createTriggerKey(jobDefinition);
            if (scheduler.checkExists(jobKey) && scheduler.checkExists(triggerKey)) {
                LOGGER.debug("Job [{}] already exists and will not be scheduled.", jobDefinition);
                return;
            }
            JobDetail job = isInternalJob(jobDefinition) ? createInternalJob(jobDefinition, jobKey) : createUserJob(jobDefinition, jobKey);
            Trigger trigger = createTrigger(jobDefinition, triggerKey);
            scheduler.scheduleJob(job, trigger);

            LOGGER.info("Scheduled Job: [{}] of group: [{}] at: [{}]", jobKey.getName(), jobKey.getGroup(), jobDefinition.getExpression());
        } catch (ObjectAlreadyExistsException e) {
            LOGGER.warn(e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new Exception("Invalid class name [" + jobDefinition.getClazz() + "] for the job", e);
        } catch (org.quartz.SchedulerException e) {
            throw new Exception("Failed to schedule a job for " + jobDefinition, e);
        }
    }

    /**
     * Unschedule a job.
     *
     * @param name the name
     * @param group the group
     * @throws Exception the exception
     */
    public void unscheduleJob(String name, String group) throws Exception {
        if (isInternalJob(group)) {
            LOGGER.debug("Internal job with name [{}] will NOT be unscheduled", name);
            return;
        }
        try {
            JobKey jobKey = createJobKey(name, group);
            TriggerKey triggerKey = createTriggerKey(name, group);
            if (scheduler.checkExists(triggerKey)) {
                scheduler.unscheduleJob(triggerKey);
                scheduler.deleteJob(jobKey);
                LOGGER.info("Unscheduled Job: [{}] of group: [{}]", name, group);
            }
        } catch (ObjectAlreadyExistsException ex) {
            LOGGER.warn(ex.getMessage());
        } catch (org.quartz.SchedulerException ex) {
            String msg = "Failed to unschedule job with name [" + name + "] from group [" + group + "] ";
            throw new Exception(msg, ex);
        }
    }

    /**
     * Creates the trigger key.
     *
     * @param jobDefinition the job definition
     * @return the trigger key
     */
    private TriggerKey createTriggerKey(org.eclipse.dirigible.components.jobs.domain.Job jobDefinition) {
        return createTriggerKey(jobDefinition.getName(), jobDefinition.getGroup());
    }

    /**
     * Creates the trigger key.
     *
     * @param name the name
     * @param group the group
     * @return the trigger key
     */
    private TriggerKey createTriggerKey(String name, String group) {
        String jobName = createJobName(name, group);
        return new TriggerKey(jobName, group);
    }

    /**
     * Creates the job name.
     *
     * @param jobName the job name
     * @param group the group
     * @return the string
     */
    private String createJobName(String jobName, String group) {
        return isInternalJob(group) ? jobName : jobNameCreator.toTenantName(jobName);
    }

    /**
     * Checks if is internal job.
     *
     * @param jobDefinition the job definition
     * @return true, if is internal job
     */
    private boolean isInternalJob(org.eclipse.dirigible.components.jobs.domain.Job jobDefinition) {
        return isInternalJob(jobDefinition.getGroup());
    }

    /**
     * Checks if is internal job.
     *
     * @param group the group
     * @return true, if is internal job
     */
    private boolean isInternalJob(String group) {
        return !JOB_GROUP_DEFINED.equals(group);
    }

    /**
     * Creates the job key.
     *
     * @param jobDefinition the job definition
     * @return the job key
     */
    private JobKey createJobKey(org.eclipse.dirigible.components.jobs.domain.Job jobDefinition) {
        return createJobKey(jobDefinition.getName(), jobDefinition.getGroup());
    }

    /**
     * Creates the job key.
     *
     * @param name the name
     * @param group the group
     * @return the job key
     */
    private JobKey createJobKey(String name, String group) {
        String jobName = createJobName(name, group);
        return new JobKey(jobName, group);
    }

    /**
     * Creates the internal job.
     *
     * @param jobDefinition the job definition
     * @param jobKey the job key
     * @return the job detail
     * @throws ClassNotFoundException the class not found exception
     */
    private JobDetail createInternalJob(org.eclipse.dirigible.components.jobs.domain.Job jobDefinition, JobKey jobKey)
            throws ClassNotFoundException {
        Class<Job> jobClass = (Class<Job>) Class.forName(jobDefinition.getClazz());
        return JobBuilder.newJob(jobClass)
                         .withIdentity(jobKey)
                         .withDescription(jobDefinition.getDescription())
                         .build();
    }

    /**
     * Creates the user job.
     *
     * @param jobDefinition the job definition
     * @param jobKey the job key
     * @return the job detail
     */
    private JobDetail createUserJob(org.eclipse.dirigible.components.jobs.domain.Job jobDefinition, JobKey jobKey) {
        JobDetail job = JobBuilder.newJob(JobHandler.class)
                                  .withIdentity(jobKey)
                                  .withDescription(jobDefinition.getDescription())
                                  .usingJobData(JobHandler.JOB_PARAMETER_HANDLER, jobDefinition.getHandler())
                                  .usingJobData(JobHandler.JOB_PARAMETER_ENGINE, jobDefinition.getEngine())
                                  .build();
        job.getJobDataMap()
           .put(JobHandler.TENANT_PARAMETER, tenantContext.getCurrentTenant());

        return job;
    }

    /**
     * Creates the trigger.
     *
     * @param jobDefinition the job definition
     * @param triggerKey the trigger key
     * @return the trigger
     */
    private Trigger createTrigger(org.eclipse.dirigible.components.jobs.domain.Job jobDefinition, TriggerKey triggerKey) {
        TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger()
                                                               .withIdentity(triggerKey);
        if (StringUtils.isEmpty(jobDefinition.getExpression())) {
            triggerBuilder.startNow();
        } else {
            triggerBuilder.withSchedule(cronSchedule(jobDefinition.getExpression()));
        }
        return triggerBuilder.build();
    }

}
