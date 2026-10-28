/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.api.job;

import static java.text.MessageFormat.format;

import java.util.Map;

import org.eclipse.dirigible.commons.api.helpers.GsonHelper;
import org.eclipse.dirigible.components.jobs.domain.Job;
import org.eclipse.dirigible.components.jobs.domain.JobParameter;
import org.eclipse.dirigible.components.jobs.service.JobLogService;
import org.eclipse.dirigible.components.jobs.service.JobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The Class JobFacade.
 */
@Component
public class JobFacade implements InitializingBean {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(JobFacade.class);

    /**
     * Serialises jobs as {@link GsonHelper} does, minus the parameter's back-reference to its job: it
     * is hidden from Jackson only, so Gson would follow job, parameters, job, ... until the stack
     * overflows.
     */
    private static final Gson JOB_GSON = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
                                                          .setPrettyPrinting()
                                                          .setExclusionStrategies(new ParameterJobExclusion())
                                                          .create();

    /** The job facade. */
    private static JobFacade INSTANCE;

    /** The job service. */
    private JobService jobService;

    /** The job log service. */
    private JobLogService jobLogService;

    /**
     * Instantiates a new database facade.
     *
     * @param jobService the job service
     * @param jobLogService the job log service
     */
    @Autowired
    private JobFacade(JobService jobService, JobLogService jobLogService) {
        this.jobService = jobService;
        this.jobLogService = jobLogService;
    }

    /**
     * After properties set.
     *
     * @throws Exception the exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        INSTANCE = this;
    }

    /**
     * Gets the instance.
     *
     * @return the job facade
     */
    public static JobFacade get() {
        return INSTANCE;
    }

    /**
     * Gets the job service.
     *
     * @return the job service
     */
    public JobService getJobService() {
        return jobService;
    }

    /**
     * Gets the job log service.
     *
     * @return the job log service
     */
    public JobLogService getJobLogService() {
        return jobLogService;
    }

    /**
     * Gets the jobs.
     *
     * @return the jobs
     */
    public static String getJobs() {
        return toJson(JobFacade.get()
                               .getJobService()
                               .getAll());
    }

    /**
     * Gets the job.
     *
     * @param name the name
     * @return the job
     */
    public static String getJob(String name) {
        return toJson(JobFacade.get()
                               .getJobService()
                               .findByName(name));
    }

    /**
     * Enable.
     *
     * @param name the name
     * @return the string
     * @throws Exception the exception
     */
    public static String enable(String name) throws Exception {
        return toJson(JobFacade.get()
                               .getJobService()
                               .enable(name));
    }

    /**
     * Disable.
     *
     * @param name the name
     * @return the string
     * @throws Exception the exception
     */
    public static String disable(String name) throws Exception {
        return toJson(JobFacade.get()
                               .getJobService()
                               .disable(name));
    }

    /**
     * Trigger.
     *
     * @param name the name
     * @param parameters the parameters
     * @return true, if successful
     * @throws Exception the job execution exception
     */
    public static boolean trigger(String name, String parameters) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, String> parametersMap = GsonHelper.fromJson(parameters, Map.class);
        return JobFacade.get()
                        .getJobService()
                        .trigger(name, parametersMap);
    }

    /**
     * Log.
     *
     * @param name the name
     * @param message the message
     * @throws Exception the scheduler exception
     */
    public static void log(String name, String message) throws Exception {
        Job job = JobFacade.get()
                           .getJobService()
                           .findByName(name);
        if (job != null) {
            String handler = job.getHandler();
            JobFacade.get()
                     .getJobLogService()
                     .jobLogged(name, handler, message);
        } else {
            String error = format("Job with name {0} does not exist, hence cannot be used to log messages", name);
            throw new Exception(error);
        }
    }

    /**
     * Error.
     *
     * @param name the name
     * @param message the message
     * @throws Exception the scheduler exception
     */
    public static void error(String name, String message) throws Exception {
        Job job = JobFacade.get()
                           .getJobService()
                           .findByName(name);
        if (job != null) {
            String handler = job.getHandler();
            JobFacade.get()
                     .getJobLogService()
                     .jobLoggedError(name, handler, message);
        } else {
            String error = format("Job with name {0} does not exist, hence cannot be used to log messages", name);
            throw new Exception(error);
        }
    }

    /**
     * Warn.
     *
     * @param name the name
     * @param message the message
     * @throws Exception the scheduler exception
     */
    public static void warn(String name, String message) throws Exception {
        Job job = JobFacade.get()
                           .getJobService()
                           .findByName(name);
        if (job != null) {
            String handler = job.getHandler();
            JobFacade.get()
                     .getJobLogService()
                     .jobLoggedWarning(name, handler, message);
        } else {
            String error = format("Job with name {0} does not exist, hence cannot be used to log messages", name);
            throw new Exception(error);
        }
    }

    /**
     * Info.
     *
     * @param name the name
     * @param message the message
     * @throws Exception the scheduler exception
     */
    public static void info(String name, String message) throws Exception {
        Job job = JobFacade.get()
                           .getJobService()
                           .findByName(name);
        if (job != null) {
            String handler = job.getHandler();
            JobFacade.get()
                     .getJobLogService()
                     .jobLoggedInfo(name, handler, message);
        } else {
            String error = format("Job with name {0} does not exist, hence cannot be used to log messages", name);
            throw new Exception(error);
        }
    }

    /**
     * Serialises a job, or a list of jobs, with their declared parameters.
     *
     * @param jobs the job or jobs
     * @return the JSON
     */
    static String toJson(Object jobs) {
        return JOB_GSON.toJson(jobs);
    }

    /**
     * Skips {@link JobParameter}'s reference back to the job that declares it.
     */
    private static final class ParameterJobExclusion implements ExclusionStrategy {

        @Override
        public boolean shouldSkipField(FieldAttributes field) {
            return field.getDeclaringClass() == JobParameter.class && "job".equals(field.getName());
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }
    }

}
